package com.hims.lab.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hims.lab.application.exceptions.InvalidLabRequestException;
import com.hims.lab.application.exceptions.LabEntityNotFoundException;
import com.hims.lab.domain.model.KdlResult;
import com.hims.lab.domain.model.MachineConfiguration;
import com.hims.lab.domain.repository.KdlResultRepository;
import com.hims.lab.domain.repository.MachineConfigurationRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * KDL/machine interface integration service (GAP-REGISTER WP-7 HIGH).
 * Parses raw instrument payloads (HL7/ASTM/file-drop) and maps them to
 * patho parameter rows. Ported from legacy LabKDLResultListDto / LabLoadListForKDLDto.
 */
@Service
public class KdlService {

    private static final Logger log = LoggerFactory.getLogger(KdlService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static final long AUDIT_ACTOR_ID = 1L;

    private final KdlResultRepository kdlResultRepository;
    private final MachineConfigurationRepository machineConfigurationRepository;

    @PersistenceContext
    private EntityManager em;

    public KdlService(KdlResultRepository kdlResultRepository,
                      MachineConfigurationRepository machineConfigurationRepository) {
        this.kdlResultRepository = kdlResultRepository;
        this.machineConfigurationRepository = machineConfigurationRepository;
    }

    /**
     * Parse raw KDL/machine protocol payload and store it.
     * Supports HL7 v2.x (pipe-delimited), ASTM (frame-delimited), and JSON.
     */
    @Transactional
    public KdlResult parseAndStore(Long machineId, String protocol, String rawPayload,
                                   String labNumber, String mrnNo, String patientName,
                                   String testName, String sampleStatus) {
        if (protocol == null || protocol.isBlank()) {
            throw new InvalidLabRequestException("LAB_REQ_INVALID", "protocol is required");
        }
        if (rawPayload == null || rawPayload.isBlank()) {
            throw new InvalidLabRequestException("LAB_REQ_INVALID", "rawPayload is required");
        }
        MachineConfiguration machine = null;
        if (machineId != null) {
            machine = machineConfigurationRepository.findById(machineId)
                    .orElseThrow(() -> new LabEntityNotFoundException("NOT_FOUND",
                            "Machine not found: " + machineId));
            if (!Boolean.TRUE.equals(machine.getActive())) {
                throw new InvalidLabRequestException("LAB_REQ_INVALID",
                        "Machine " + machineId + " is not active");
            }
        }

        KdlResult result = new KdlResult();
        result.setKdlResultId(nextId("t_kdl_result_seq"));
        result.setMachineId(machineId);
        result.setProtocol(protocol.trim().toUpperCase());
        result.setRawPayload(rawPayload);
        result.setLabNumber(labNumber);
        result.setMrnNo(mrnNo);
        result.setPatientName(patientName);
        result.setTestName(testName);
        result.setSampleStatus(sampleStatus);
        result.setIsMapped(false);
        result.setReceivedAt(now());
        result.setStatus(true);
        result.setSynchronizedd(false);
        result.setAddedBy(AUDIT_ACTOR_ID);
        result.setAddedDatetime(now());

        // attempt parse based on protocol
        try {
            String parsed = parsePayload(protocol, rawPayload);
            result.setParsedPayload(parsed);
        } catch (Exception e) {
            log.warn("KDL parse failed for protocol {}: {}", protocol, e.getMessage());
            // still store raw; parsed stays null
        }

        KdlResult saved = kdlResultRepository.save(result);
        log.info("Stored KDL result {} (protocol={}, machine={})", saved.getKdlResultId(), protocol, machineId);
        return saved;
    }

    /**
     * Map an unmapped KDL result to a patho detail.
     * Returns the updated KdlResult with isMapped=true.
     */
    @Transactional
    public KdlResult mapToDetail(Long kdlResultId, Long detailId) {
        KdlResult result = load(kdlResultId);
        if (Boolean.TRUE.equals(result.getIsMapped())) {
            throw new InvalidLabRequestException("LAB_ILLEGAL_TRANSITION",
                    "KDL result " + kdlResultId + " is already mapped to detail " + result.getMappedDetailId());
        }
        result.setIsMapped(true);
        result.setMappedDetailId(detailId);
        result.setUpdatedBy(AUDIT_ACTOR_ID);
        result.setUpdatedDatetime(now());
        KdlResult saved = kdlResultRepository.save(result);
        log.info("Mapped KDL result {} to detail {}", kdlResultId, detailId);
        return saved;
    }

    @Transactional(readOnly = true)
    public KdlResult get(Long id) {
        return load(id);
    }

    @Transactional(readOnly = true)
    public List<KdlResult> listUnmapped() {
        return kdlResultRepository.findByIsMappedFalseOrderByReceivedAtAsc();
    }

    @Transactional(readOnly = true)
    public List<KdlResult> listByMachine(Long machineId) {
        return kdlResultRepository.findByMachineIdOrderByReceivedAtAsc(machineId);
    }

    @Transactional(readOnly = true)
    public List<KdlResult> listByMrn(String mrnNo) {
        return kdlResultRepository.findByMrnNoOrderByReceivedAtAsc(mrnNo);
    }

    /**
     * Parse payload based on protocol type.
     * HL7 v2.x: pipe-delimited segments (MSH, PID, OBR, OBX).
     * ASTM: frame-delimited with header/record terminators.
     * JSON: passthrough.
     */
    String parsePayload(String protocol, String rawPayload) {
        return switch (protocol.trim().toUpperCase()) {
            case "HL7" -> parseHl7(rawPayload);
            case "ASTM" -> parseAstm(rawPayload);
            case "JSON", "FILE" -> parseJsonPassthrough(rawPayload);
            default -> throw new InvalidLabRequestException("LAB_REQ_INVALID",
                    "Unsupported protocol: " + protocol);
        };
    }

    private String parseHl7(String raw) {
        ObjectNode root = MAPPER.createObjectNode();
        String[] segments = raw.split("[\r\n]+");
        List<ObjectNode> obxList = new ArrayList<>();
        for (String seg : segments) {
            if (seg.isBlank()) continue;
            String[] fields = seg.split("\\|", -1);
            if (fields.length == 0) continue;
            String type = fields[0].trim();
            ObjectNode node = MAPPER.createObjectNode();
            node.put("segmentType", type);
            for (int i = 1; i < fields.length; i++) {
                node.put("f" + i, fields[i].trim());
            }
            switch (type) {
                case "MSH" -> {
                    node.put("sendingApplication", fields.length > 2 ? fields[2].trim() : "");
                    node.put("sendingFacility", fields.length > 3 ? fields[3].trim() : "");
                }
                case "PID" -> {
                    node.put("patientId", fields.length > 2 ? fields[2].trim() : "");
                    node.put("patientName", fields.length > 5 ? fields[5].trim() : "");
                }
                case "OBR" -> {
                    node.put("testCode", fields.length > 4 ? fields[4].trim() : "");
                    node.put("testName", fields.length > 5 ? fields[5].trim() : "");
                }
                case "OBX" -> obxList.add(node);
                default -> { /* generic segment */ }
            }
        }
        if (!obxList.isEmpty()) {
            var arr = root.putArray("observations");
            obxList.forEach(arr::add);
        }
        return root.toString();
    }

    private String parseAstm(String raw) {
        ObjectNode root = MAPPER.createObjectNode();
        String[] records = raw.split("[\r\n]+");
        List<ObjectNode> results = new ArrayList<>();
        for (String rec : records) {
            if (rec.isBlank()) continue;
            // ASTM records start with 'H', 'P', 'O', 'R', 'L'
            char type = rec.charAt(0);
            ObjectNode node = MAPPER.createObjectNode();
            node.put("recordType", String.valueOf(type));
            node.put("raw", rec);
            if (type == 'R') {
                // result record: R|1|^^^TEST_CODE|RESULT_VALUE|UNIT|...
                String[] fields = rec.split("\\|", -1);
                if (fields.length > 3) node.put("testCode", fields[2].trim());
                if (fields.length > 4) node.put("resultValue", fields[3].trim());
                if (fields.length > 5) node.put("unit", fields[4].trim());
                results.add(node);
            }
        }
        if (!results.isEmpty()) {
            var arr = root.putArray("results");
            results.forEach(arr::add);
        }
        return root.toString();
    }

    private String parseJsonPassthrough(String raw) {
        try {
            JsonNode node = MAPPER.readTree(raw);
            return node.toString();
        } catch (Exception e) {
            throw new InvalidLabRequestException("LAB_REQ_INVALID", "Invalid JSON payload: " + e.getMessage());
        }
    }

    private KdlResult load(Long id) {
        return kdlResultRepository.findById(id)
                .orElseThrow(() -> new LabEntityNotFoundException("NOT_FOUND",
                        "KDL result not found: " + id));
    }

    private Long nextId(String sequenceName) {
        Number n = (Number) em.createNativeQuery("SELECT nextval('ihms_lab." + sequenceName + "')")
                .getSingleResult();
        return n.longValue();
    }

    private static Timestamp now() {
        return Timestamp.from(Instant.now());
    }
}
