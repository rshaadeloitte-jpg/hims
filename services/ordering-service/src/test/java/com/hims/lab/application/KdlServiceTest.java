package com.hims.lab.application;

import com.hims.lab.application.exceptions.InvalidLabRequestException;
import com.hims.lab.application.exceptions.LabEntityNotFoundException;
import com.hims.lab.domain.model.KdlResult;
import com.hims.lab.domain.model.MachineConfiguration;
import com.hims.lab.domain.repository.KdlResultRepository;
import com.hims.lab.domain.repository.MachineConfigurationRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KdlServiceTest {

    private KdlResultRepository kdlResultRepository;
    private MachineConfigurationRepository machineConfigurationRepository;
    private EntityManager em;
    private KdlService service;

    @BeforeEach
    void setUp() {
        kdlResultRepository = mock(KdlResultRepository.class);
        machineConfigurationRepository = mock(MachineConfigurationRepository.class);
        em = mock(EntityManager.class);
        service = new KdlService(kdlResultRepository, machineConfigurationRepository);
        ReflectionTestUtils.setField(service, "em", em);

        Query seqQuery = mock(Query.class);
        when(seqQuery.getSingleResult()).thenAnswer(inv -> 100L);
        when(em.createNativeQuery(contains("nextval"))).thenReturn(seqQuery);
        when(kdlResultRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void parsePayload_hl7_extractsSegments() {
        String hl7 = "MSH|^~\\&|SENDER|FAC|RECV|APP|20260101||ORU^R01|12345|P|2.5\rPID|1||MRN123||DOE^JOHN\rOBR|1||ORDER1^CBC^LAB\rOBX|1||HB^Hemoglobin||13.5|g/dL|12-16|N";
        String parsed = service.parsePayload("HL7", hl7);
        assertThat(parsed).contains("OBX").contains("observations").contains("Hemoglobin");
    }

    @Test
    void parsePayload_astm_extractsResults() {
        String astm = "H|^&|||HOST|||||||P|1|20260101\rP|1||MRN456||DOE^JANE\rO|1||ORDER2||^^^CBC\rR|1|^^^HB|13.5|g/dL|||N\rL|1|N";
        String parsed = service.parsePayload("ASTM", astm);
        assertThat(parsed).contains("results");
    }

    @Test
    void parsePayload_json_passthrough() {
        String json = "{\"test\":\"CBC\",\"value\":\"13.5\"}";
        String parsed = service.parsePayload("JSON", json);
        assertThat(parsed).contains("CBC").contains("13.5");
    }

    @Test
    void parsePayload_unsupportedProtocol_throws() {
        assertThatThrownBy(() -> service.parsePayload("XML", "<x/>"))
                .isInstanceOf(InvalidLabRequestException.class)
                .hasMessageContaining("Unsupported protocol");
    }

    @Test
    void parseAndStore_requiresProtocol() {
        assertThatThrownBy(() -> service.parseAndStore(1L, null, "raw", null, null, null, null, null))
                .isInstanceOf(InvalidLabRequestException.class)
                .extracting(e -> ((InvalidLabRequestException) e).getErrorCode())
                .isEqualTo("LAB_REQ_INVALID");
    }

    @Test
    void parseAndStore_requiresRawPayload() {
        assertThatThrownBy(() -> service.parseAndStore(1L, "HL7", null, null, null, null, null, null))
                .isInstanceOf(InvalidLabRequestException.class)
                .hasMessageContaining("rawPayload");
    }

    @Test
    void parseAndStore_inactiveMachine_rejected() {
        MachineConfiguration m = new MachineConfiguration();
        m.setMachineId(1L);
        m.setActive(false);
        when(machineConfigurationRepository.findById(1L)).thenReturn(Optional.of(m));

        assertThatThrownBy(() -> service.parseAndStore(1L, "HL7", "raw", null, null, null, null, null))
                .isInstanceOf(InvalidLabRequestException.class)
                .hasMessageContaining("not active");
    }

    @Test
    void parseAndStore_valid_storesResult() {
        MachineConfiguration m = new MachineConfiguration();
        m.setMachineId(1L);
        m.setActive(true);
        when(machineConfigurationRepository.findById(1L)).thenReturn(Optional.of(m));

        KdlResult result = service.parseAndStore(1L, "HL7", "MSH|^~\\&|SEND|FAC", "LAB-001", "MRN123", "John", "CBC", "COLLECTED");
        assertThat(result.getKdlResultId()).isEqualTo(100L);
        assertThat(result.getProtocol()).isEqualTo("HL7");
        assertThat(result.getLabNumber()).isEqualTo("LAB-001");
        assertThat(result.getIsMapped()).isFalse();
    }

    @Test
    void mapToDetail_alreadyMapped_rejected() {
        KdlResult r = new KdlResult();
        r.setKdlResultId(5L);
        r.setIsMapped(true);
        r.setMappedDetailId(99L);
        when(kdlResultRepository.findById(5L)).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> service.mapToDetail(5L, 21L))
                .isInstanceOf(InvalidLabRequestException.class)
                .extracting(e -> ((InvalidLabRequestException) e).getErrorCode())
                .isEqualTo("LAB_ILLEGAL_TRANSITION");
    }

    @Test
    void mapToDetail_valid_marksMapped() {
        KdlResult r = new KdlResult();
        r.setKdlResultId(5L);
        r.setIsMapped(false);
        when(kdlResultRepository.findById(5L)).thenReturn(Optional.of(r));

        KdlResult mapped = service.mapToDetail(5L, 21L);
        assertThat(mapped.getIsMapped()).isTrue();
        assertThat(mapped.getMappedDetailId()).isEqualTo(21L);
    }

    @Test
    void get_unknownId_throws404() {
        when(kdlResultRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(999L))
                .isInstanceOf(LabEntityNotFoundException.class)
                .extracting(e -> ((LabEntityNotFoundException) e).getErrorCode())
                .isEqualTo("NOT_FOUND");
    }
}
