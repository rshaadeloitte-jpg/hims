package com.hims.transport.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.transport.application.exceptions.InvalidTransportRequestException;
import com.hims.transport.application.exceptions.TransportNotFoundException;
import com.hims.transport.domain.model.Driver;
import com.hims.transport.domain.model.TransportBase;
import com.hims.transport.domain.model.Vehicle;
import com.hims.transport.domain.model.VehicleRequest;
import com.hims.transport.domain.model.VehicleRequestDetails;
import com.hims.transport.domain.model.VehicleRequestType;
import com.hims.transport.domain.repository.DriverRepository;
import com.hims.transport.domain.repository.VehicleRepository;
import com.hims.transport.domain.repository.VehicleRequestDetailsRepository;
import com.hims.transport.domain.repository.VehicleRequestRepository;
import com.hims.transport.domain.repository.VehicleRequestTypeRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Transport / vehicle request lifecycle (MIG-023 sub-slice, S17). Clean target
 * of legacy TransportServiceImpl (t_vehiclerequest + t_vehiclerequestdetails).
 * REST-only: TransportRequested is rejected phase 1 in KAFKA-EVENT-CATALOG —
 * no outbox until a second consumer appears.
 *
 * <p>Preserved legacy behavior:
 * <ul>
 *   <li>raise (addPatientTansportRequest): patient (non-doctor) requests are
 *       auto-approved (isApproved=true); requestMode list is joined into
 *       staffInstrumentReq; tripType 0=drop / 1=pickup (else-if) / 3=deadbody;
 *       requestDateTime defaults to now; expiryDateTime defaults to
 *       (requestToDateTime or now) truncated to midnight + 1 day;</li>
 *   <li>allocate (allocateVehicle): stamps vehicleId/driverId/location/etc.,
 *       doctor requests flip isCharged=true, and the vehicle + driver both flip
 *       isAvailable=false;</li>
 *   <li>vehicle-out (saveVehicleOutDetails): inserts (or updates) a detail row
 *       and flips the request isVehicleOut=true;</li>
 *   <li>vehicle-in (saveVehicleInDetails): stamps endOdmReading/tripDistance/
 *       actualFare/inTime on the request's detail rows, flips completed=true,
 *       and releases the vehicle + driver (isAvailable=true);</li>
 *   <li>cancel (cancelTransportRequest): flips isCancelled=true and releases any
 *       allocated vehicle + driver;</li>
 *   <li>counts (getTripRequestCount): day-window total + emergency/normal/
 *       scheduled split resolved from the vehicle_request_type dictionary
 *       (description matched case-insensitively);</li>
 *   <li>list (getTripRequestDetails): unit-scoped day-window search with
 *       requestType / requisitionBy / mobileNo / isDoctorRequest filters.</li>
 * </ul>
 *
 * <p>Documented improvements (IMP-DEC-035): lifecycle guards the legacy raw
 * UPDATEs lacked — allocate/vehicle-out require an active, not-cancelled,
 * not-completed request; vehicle-in requires the request to be out and not yet
 * completed; cancel/approve reject already-cancelled/completed requests;
 * allocate validates the vehicle + driver exist.
 *
 * <p>Deferred cross-domain integrations (dependency-neutral): notification mail
 * (sendTransportRequestMail / allocation / approval), billing charges
 * (addTransportCharge / getChargesForTransport / vehicle-in billing via
 * ConfigApplication.smsTransport), and patient/doctor/staff search + mrno/
 * designation lookups (patient + IAM domains).
 */
@Service
public class VehicleRequestService {

    private static final Logger log = LoggerFactory.getLogger(VehicleRequestService.class);

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id

    private final VehicleRequestRepository requestRepository;
    private final VehicleRequestDetailsRepository detailsRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final VehicleRequestTypeRepository requestTypeRepository;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager em;

    public VehicleRequestService(VehicleRequestRepository requestRepository,
                                 VehicleRequestDetailsRepository detailsRepository,
                                 VehicleRepository vehicleRepository,
                                 DriverRepository driverRepository,
                                 VehicleRequestTypeRepository requestTypeRepository,
                                 ObjectMapper objectMapper) {
        this.requestRepository = requestRepository;
        this.detailsRepository = detailsRepository;
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
        this.requestTypeRepository = requestTypeRepository;
        this.objectMapper = objectMapper;
    }

    // ----------------------------------------------------------------- raise

    /**
     * Raise a transport request (legacy addPatientTansportRequest). Payload is
     * the request body itself (no wrapper): {@code {requisitionBy, mobileNo,
     * vehicleTypeId, requestType, tripType:[...], requestMode:[...],
     * isDoctorRequest, patientId, doctorId, staffId, requestDateTime,
     * requestToDateTime, ...}}.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public VehicleRequest raiseRequest(JsonNode payload, Long unitId) {
        if (payload == null || payload.isNull() || payload.isMissingNode() || !payload.isObject()) {
            throw new InvalidTransportRequestException("BEXA003", "Invalid input data !");
        }
        VehicleRequest request = bind(payload, VehicleRequest.class);
        request.setId(nextId("vehicle_request_seq"));

        boolean doctorRequest = Boolean.TRUE.equals(request.getIsDoctorRequest());
        if (!doctorRequest) {
            request.setStaffInstrumentReq(joinRequestMode(payload));
            request.setIsApproved(true); // legacy: auto-approve patient request
        }
        applyTripType(request, payload);

        if (request.getRequestDateTime() == null) {
            request.setRequestDateTime(Timestamp.from(Instant.now()));
        }
        if (request.getExpiryDateTime() == null) {
            request.setExpiryDateTime(defaultExpiry(request.getRequestToDateTime()));
        }
        request.setStatus(request.getStatus() == null ? true : request.getStatus());
        stampCreate(request, unitId);
        VehicleRequest saved = requestRepository.save(request);
        log.info("Transport request raised id={} doctorRequest={} autoApproved={}",
                saved.getId(), doctorRequest, saved.getIsApproved());
        return saved;
    }

    // ------------------------------------------------------------- lifecycle

    /**
     * Allocate a vehicle + driver (legacy allocateVehicle). Stamps the request
     * and flips the vehicle + driver isAvailable=false; doctor requests flip
     * isCharged=true.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public VehicleRequest allocateVehicle(JsonNode payload, Long unitId) {
        if (payload == null || payload.isNull() || payload.isMissingNode() || !payload.isObject()) {
            throw new InvalidTransportRequestException("BEXA003", "Invalid input data !");
        }
        Long requestId = longField(payload, "id");
        if (requestId == null) {
            throw new InvalidTransportRequestException("BEXA003", "id is required");
        }
        VehicleRequest request = getRequest(requestId);
        requireActive(request, "allocate");

        Long vehicleId = longField(payload, "vehicleId");
        Long driverId = longField(payload, "driverId");
        if (vehicleId == null || driverId == null) {
            throw new InvalidTransportRequestException("BEXA003", "vehicleId and driverId are required");
        }
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new TransportNotFoundException("NOT_FOUND", "Vehicle not found"));
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new TransportNotFoundException("NOT_FOUND", "Driver not found"));

        request.setVehicleId(vehicleId);
        request.setDriverId(driverId);
        if (payload.hasNonNull("location")) request.setLocation(payload.get("location").asText());
        if (payload.hasNonNull("requestDateTime")) {
            request.setRequestDateTime(Timestamp.from(Instant.parse(payload.get("requestDateTime").asText())));
        }
        if (payload.hasNonNull("isChargeExempted")) request.setIsChargeExempted(payload.get("isChargeExempted").asBoolean());
        if (payload.hasNonNull("exemptionCatgId")) request.setExemptionCatgId(payload.get("exemptionCatgId").asLong());
        request.setStaffInstrumentReq(joinRequestMode(payload));
        if (payload.hasNonNull("allocatedDoctors")) request.setAllocatedDoctors(payload.get("allocatedDoctors").asText());
        if (payload.hasNonNull("allocatedNurses")) request.setAllocatedNurses(payload.get("allocatedNurses").asText());
        if (Boolean.TRUE.equals(request.getIsDoctorRequest())) {
            request.setIsCharged(true);
        }
        stampUpdate(request);

        vehicle.setIsAvailable(false);
        stampUpdate(vehicle);
        vehicleRepository.save(vehicle);
        driver.setIsAvailable(false);
        stampUpdate(driver);
        driverRepository.save(driver);

        log.info("Transport request id={} allocated vehicle={} driver={}", request.getId(), vehicleId, driverId);
        return requestRepository.save(request);
    }

    /** Approve (legacy approveTransportRequest): stamps approvedBy + isApproved=true. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public VehicleRequest approve(Long id, String approvedBy) {
        VehicleRequest request = getRequest(id);
        requireActive(request, "approve");
        if (Boolean.TRUE.equals(request.getIsApproved())) {
            throw new InvalidTransportRequestException("BEXA003",
                    "Cannot approve: request is already approved");
        }
        request.setIsApproved(true);
        request.setApprovedBy(approvedBy);
        stampUpdate(request);
        return requestRepository.save(request);
    }

    /**
     * Vehicle out (legacy saveVehicleOutDetails): insert (or update) a detail
     * row and flip the request isVehicleOut=true.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public VehicleRequest vehicleOut(JsonNode payload, Long unitId) {
        if (payload == null || payload.isNull() || payload.isMissingNode() || !payload.isObject()) {
            throw new InvalidTransportRequestException("BEXA003", "Invalid input data !");
        }
        Long requestId = longField(payload, "vehicleRequestId");
        if (requestId == null) {
            throw new InvalidTransportRequestException("BEXA003", "vehicleRequestId is required");
        }
        VehicleRequest request = getRequest(requestId);
        requireActive(request, "vehicle-out");
        if (Boolean.TRUE.equals(request.getIsVehicleOut())) {
            throw new InvalidTransportRequestException("BEXA003",
                    "Cannot mark vehicle out: request is already out");
        }

        VehicleRequestDetails details;
        Long detailId = longField(payload, "id");
        if (detailId != null) {
            details = detailsRepository.findById(detailId)
                    .orElseThrow(() -> new TransportNotFoundException("NOT_FOUND", "Vehicle request details not found"));
        } else {
            details = new VehicleRequestDetails();
            details.setId(nextId("vehicle_request_details_seq"));
            details.setStatus(true);
            stampCreate(details, unitId);
        }
        details.setVehicleRequestId(requestId);
        if (payload.hasNonNull("startOdmReading")) details.setStartOdmReading(payload.get("startOdmReading").asLong());
        if (payload.hasNonNull("calculatedTripFare")) details.setCalculatedTripFare(payload.get("calculatedTripFare").asLong());
        if (payload.hasNonNull("tripDistanceEstimated")) details.setTripDistanceEstimated(payload.get("tripDistanceEstimated").asLong());
        if (payload.hasNonNull("outTime")) details.setOutTime(Timestamp.from(Instant.parse(payload.get("outTime").asText())));
        if (detailId != null) {
            stampUpdate(details);
        }
        detailsRepository.save(details);

        request.setIsVehicleOut(true);
        stampUpdate(request);
        log.info("Transport request id={} vehicle out (detail id={})", requestId, details.getId());
        return requestRepository.save(request);
    }

    /**
     * Vehicle in (legacy saveVehicleInDetails): stamp endOdmReading/tripDistance/
     * actualFare/inTime on the request's detail rows, flip completed=true, and
     * release the vehicle + driver.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public VehicleRequest vehicleIn(JsonNode payload, Long unitId) {
        if (payload == null || payload.isNull() || payload.isMissingNode() || !payload.isObject()) {
            throw new InvalidTransportRequestException("BEXA003", "Invalid input data !");
        }
        Long requestId = longField(payload, "vehicleRequestId");
        if (requestId == null) {
            throw new InvalidTransportRequestException("BEXA003", "vehicleRequestId is required");
        }
        VehicleRequest request = getRequest(requestId);
        requireActive(request, "vehicle-in");
        if (!Boolean.TRUE.equals(request.getIsVehicleOut())) {
            throw new InvalidTransportRequestException("BEXA003",
                    "Cannot mark vehicle in: request is not out");
        }
        if (Boolean.TRUE.equals(request.getCompleted())) {
            throw new InvalidTransportRequestException("BEXA003",
                    "Cannot mark vehicle in: request is already completed");
        }

        // Legacy updates every detail row of the request keyed by vehicleRequestId.
        for (VehicleRequestDetails details : detailsRepository.findByVehicleRequestIdOrderByIdAsc(requestId)) {
            if (payload.hasNonNull("endOdmReading")) details.setEndOdmReading(payload.get("endOdmReading").asLong());
            if (payload.hasNonNull("tripDistance")) details.setTripDistance(payload.get("tripDistance").asLong());
            if (payload.hasNonNull("actualFare")) details.setActualFare(payload.get("actualFare").asLong());
            if (payload.hasNonNull("inTime")) details.setInTime(Timestamp.from(Instant.parse(payload.get("inTime").asText())));
            stampUpdate(details);
            detailsRepository.save(details);
        }

        request.setCompleted(true);
        stampUpdate(request);
        releaseVehicleAndDriver(request);
        log.info("Transport request id={} vehicle in (completed)", requestId);
        return requestRepository.save(request);
    }

    /** Cancel (legacy cancelTransportRequest): isCancelled=true + release vehicle/driver. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public VehicleRequest cancel(Long id) {
        VehicleRequest request = getRequest(id);
        if (Boolean.TRUE.equals(request.getIsCancelled())) {
            throw new InvalidTransportRequestException("BEXA003",
                    "Cannot cancel: request is already cancelled");
        }
        if (Boolean.TRUE.equals(request.getCompleted())) {
            throw new InvalidTransportRequestException("BEXA003",
                    "Cannot cancel: request is already completed");
        }
        request.setIsCancelled(true);
        stampUpdate(request);
        releaseVehicleAndDriver(request);
        log.info("Transport request id={} cancelled", id);
        return requestRepository.save(request);
    }

    // ------------------------------------------------------------------ reads

    /** Single read; 404 when absent. */
    public VehicleRequest getRequest(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new TransportNotFoundException("NOT_FOUND", "Transport request not found"));
    }

    /** Detail rows for a request (oldest first). */
    public List<VehicleRequestDetails> getDetails(Long requestId) {
        getRequest(requestId); // 404 guard
        return detailsRepository.findByVehicleRequestIdOrderByIdAsc(requestId);
    }

    /**
     * Fare preview (legacy getDetailsForVehicleFare): the newest detail row for
     * the request, decorated with the vehicle's farePerKm.
     */
    public VehicleRequestDetails getFareDetails(Long requestId, Long vehicleId) {
        getRequest(requestId); // 404 guard
        List<VehicleRequestDetails> rows = detailsRepository.findByVehicleRequestIdOrderByIdDesc(requestId);
        VehicleRequestDetails details = rows.isEmpty() ? new VehicleRequestDetails() : rows.get(0);
        if (vehicleId != null) {
            vehicleRepository.findById(vehicleId).ifPresent(v -> {
                if (v.getFarePerKm() != null) {
                    details.setFarePerKm(v.getFarePerKm());
                }
            });
        }
        return details;
    }

    /**
     * List (legacy getTripRequestDetails): unit-scoped day-window search.
     * fromDate is truncated to midnight; toDateExclusive = fromDate + 1 day.
     * Cross-domain mrno / designation decoration is deferred.
     */
    public List<VehicleRequest> listRequests(Long unitId, Timestamp fromDate, Long requestType,
                                             String search, Long mobileNo, Boolean isDoctorRequest) {
        Timestamp from = fromDate == null ? null : startOfDay(fromDate);
        Timestamp toExcl = from == null ? null : Timestamp.from(
                from.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().plusDays(1)
                        .atStartOfDay(ZoneId.systemDefault()).toInstant());
        return requestRepository.search(
                unitId, unitId != null,
                from, from != null,
                toExcl, toExcl != null,
                requestType, requestType != null,
                search == null ? "" : search, search != null && !search.isBlank(),
                mobileNo, mobileNo != null,
                isDoctorRequest, isDoctorRequest != null);
    }

    /**
     * Counts (legacy getTripRequestCount): day-window total + emergency/normal/
     * scheduled split. The type ids are resolved from the vehicle_request_type
     * dictionary by description (case-insensitive), matching the legacy cache
     * lookup.
     */
    public Map<String, Long> getCounts(Long unitId, Timestamp fromDate) {
        Timestamp from = fromDate == null ? Timestamp.from(Instant.now()) : fromDate;
        Timestamp fromDay = startOfDay(from);
        Timestamp toExcl = Timestamp.from(fromDay.toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate().plusDays(1)
                .atStartOfDay(ZoneId.systemDefault()).toInstant());

        long emergencyId = 0, normalId = 0, scheduledId = 0;
        for (VehicleRequestType type : requestTypeRepository.findByStatusTrue()) {
            if (type.getDescription() == null) continue;
            String d = type.getDescription().trim();
            if (d.equalsIgnoreCase("emergency")) emergencyId = type.getId();
            else if (d.equalsIgnoreCase("normal")) normalId = type.getId();
            else if (d.equalsIgnoreCase("scheduled")) scheduledId = type.getId();
        }

        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("TOTAL", requestRepository.countScoped(unitId, unitId != null, fromDay, true, toExcl, true, 0L, false));
        counts.put("EMERGENCY", countType(unitId, fromDay, toExcl, emergencyId));
        counts.put("NORMAL", countType(unitId, fromDay, toExcl, normalId));
        counts.put("SCHEDULED", countType(unitId, fromDay, toExcl, scheduledId));
        return counts;
    }

    private long countType(Long unitId, Timestamp fromDay, Timestamp toExcl, long typeId) {
        if (typeId == 0) return 0L;
        return requestRepository.countScoped(unitId, unitId != null, fromDay, true, toExcl, true, typeId, true);
    }

    // --------------------------------------------------------------- helpers

    private void releaseVehicleAndDriver(VehicleRequest request) {
        if (request.getVehicleId() != null) {
            vehicleRepository.findById(request.getVehicleId()).ifPresent(v -> {
                v.setIsAvailable(true);
                stampUpdate(v);
                vehicleRepository.save(v);
            });
        }
        if (request.getDriverId() != null) {
            driverRepository.findById(request.getDriverId()).ifPresent(d -> {
                d.setIsAvailable(true);
                stampUpdate(d);
                driverRepository.save(d);
            });
        }
    }

    private void requireActive(VehicleRequest request, String action) {
        if (Boolean.TRUE.equals(request.getIsCancelled())) {
            throw new InvalidTransportRequestException("BEXA003",
                    "Cannot " + action + ": request is cancelled");
        }
        if (Boolean.TRUE.equals(request.getCompleted())) {
            throw new InvalidTransportRequestException("BEXA003",
                    "Cannot " + action + ": request is already completed");
        }
    }

    /** Legacy tripType: 0=drop, else-if 1=pickup, 3=deadbody (independent). */
    private void applyTripType(VehicleRequest request, JsonNode payload) {
        JsonNode tripType = payload.path("tripType");
        if (!tripType.isArray() || tripType.isEmpty()) {
            return;
        }
        List<Integer> types = new ArrayList<>();
        tripType.forEach(n -> types.add(n.asInt()));
        if (types.contains(0)) {
            request.setIsDropTrip(true);
        } else if (types.contains(1)) {
            request.setIsPickupTrip(true);
        }
        if (types.contains(3)) {
            request.setIsDeadbodyTrip(true);
        }
    }

    /** Legacy StringUtils.join(requestMode): concatenated with no separator. */
    private String joinRequestMode(JsonNode payload) {
        JsonNode requestMode = payload.path("requestMode");
        if (!requestMode.isArray() || requestMode.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        requestMode.forEach(n -> sb.append(n.asText()));
        return sb.toString();
    }

    /** Legacy expiry default: (requestToDateTime or now) truncated to midnight + 1 day. */
    private Timestamp defaultExpiry(Timestamp requestToDateTime) {
        Instant base = requestToDateTime == null ? Instant.now() : requestToDateTime.toInstant();
        LocalDate day = base.atZone(ZoneId.systemDefault()).toLocalDate();
        return Timestamp.from(day.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private Timestamp startOfDay(Timestamp ts) {
        LocalDate day = ts.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return Timestamp.from(day.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private Long longField(JsonNode payload, String field) {
        JsonNode node = payload.path(field);
        return (node.isNull() || node.isMissingNode()) ? null : node.asLong();
    }

    private <T> T bind(JsonNode payload, Class<T> type) {
        try {
            return objectMapper.treeToValue(payload, type);
        } catch (Exception e) {
            throw new InvalidTransportRequestException("BEXA003", "Invalid input data !");
        }
    }

    private void stampCreate(TransportBase entity, Long unitId) {
        entity.setAddedBy(AUDIT_ACTOR_ID);
        entity.setAddedDatetime(Timestamp.from(Instant.now()));
        entity.setUpdatedBy(null);
        entity.setUpdatedDatetime(null);
        entity.setSynchronizedd(false);
        if (unitId != null) {
            entity.setUnitId(unitId);
        }
    }

    private void stampUpdate(TransportBase entity) {
        entity.setUpdatedBy(AUDIT_ACTOR_ID);
        entity.setUpdatedDatetime(Timestamp.from(Instant.now()));
    }

    private long nextId(String sequenceName) {
        return ((Number) em.createNativeQuery("SELECT nextval('ihms_transport.\"" + sequenceName + "\"')")
                .getSingleResult()).longValue();
    }
}
