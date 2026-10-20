package com.hims.transport.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.transport.application.exceptions.InvalidTransportRequestException;
import com.hims.transport.application.exceptions.TransportNotFoundException;
import com.hims.transport.domain.model.Driver;
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
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/** L1 — transport request lifecycle business rules (raise/allocate/approve/vehicle-out/vehicle-in/cancel, guards, counts). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VehicleRequestServiceTest {

    // Spring Boot's auto-configured mapper ignores unknown properties; mirror that
    // so payload-only fields (requestMode/tripType arrays) don't fail binding.
    private final ObjectMapper mapper = new ObjectMapper()
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Mock private VehicleRequestRepository requestRepository;
    @Mock private VehicleRequestDetailsRepository detailsRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private DriverRepository driverRepository;
    @Mock private VehicleRequestTypeRepository requestTypeRepository;
    @Mock private EntityManager em;
    @Mock private Query query;

    private VehicleRequestService service;

    @BeforeEach
    void setUp() {
        service = new VehicleRequestService(requestRepository, detailsRepository, vehicleRepository,
                driverRepository, requestTypeRepository, mapper);
        ReflectionTestUtils.setField(service, "em", em);
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(1L);
        when(requestRepository.save(any(VehicleRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(detailsRepository.save(any(VehicleRequestDetails.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(inv -> inv.getArgument(0));
        when(driverRepository.save(any(Driver.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private VehicleRequest persisted(VehicleRequest r) {
        when(requestRepository.findById(r.getId())).thenReturn(Optional.of(r));
        return r;
    }

    @Test
    void raiseRequest_patientRequest_autoApproved() throws Exception {
        VehicleRequest r = service.raiseRequest(mapper.readTree(
                "{\"requisitionBy\":\"Asha\",\"isDoctorRequest\":false,\"requestMode\":[\"1\",\"2\"]}"), 7L);
        assertThat(r.getIsApproved()).isTrue();
        assertThat(r.getStaffInstrumentReq()).isEqualTo("12"); // joined, no separator
        assertThat(r.getRequestDateTime()).isNotNull();
        assertThat(r.getExpiryDateTime()).isNotNull();
        assertThat(r.getUnitId()).isEqualTo(7L);
    }

    @Test
    void raiseRequest_doctorRequest_notAutoApproved() throws Exception {
        VehicleRequest r = service.raiseRequest(mapper.readTree(
                "{\"requisitionBy\":\"Dr Rao\",\"isDoctorRequest\":true}"), 7L);
        assertThat(r.getIsApproved()).isFalse();
    }

    @Test
    void raiseRequest_tripType_dropPickupDeadbody() throws Exception {
        VehicleRequest drop = service.raiseRequest(mapper.readTree(
                "{\"requisitionBy\":\"A\",\"tripType\":[0]}"), 7L);
        assertThat(drop.getIsDropTrip()).isTrue();
        assertThat(drop.getIsPickupTrip()).isFalse();

        VehicleRequest pickup = service.raiseRequest(mapper.readTree(
                "{\"requisitionBy\":\"B\",\"tripType\":[1]}"), 7L);
        assertThat(pickup.getIsPickupTrip()).isTrue();

        VehicleRequest deadbody = service.raiseRequest(mapper.readTree(
                "{\"requisitionBy\":\"C\",\"tripType\":[3]}"), 7L);
        assertThat(deadbody.getIsDeadbodyTrip()).isTrue();
    }

    @Test
    void raiseRequest_invalidPayload_throws() {
        assertThatThrownBy(() -> service.raiseRequest(null, 7L))
                .isInstanceOf(InvalidTransportRequestException.class);
    }

    @Test
    void allocateVehicle_flipsAvailabilityAndChargesDoctor() throws Exception {
        VehicleRequest r = new VehicleRequest();
        r.setId(1L);
        r.setIsDoctorRequest(true);
        persisted(r);
        Vehicle v = new Vehicle(); v.setId(10L); v.setIsAvailable(true);
        Driver d = new Driver(); d.setId(20L); d.setIsAvailable(true);
        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(v));
        when(driverRepository.findById(20L)).thenReturn(Optional.of(d));

        VehicleRequest out = service.allocateVehicle(mapper.readTree(
                "{\"id\":1,\"vehicleId\":10,\"driverId\":20,\"location\":\"Ward A\"}"), 7L);
        assertThat(out.getVehicleId()).isEqualTo(10L);
        assertThat(out.getDriverId()).isEqualTo(20L);
        assertThat(out.getIsCharged()).isTrue();
        assertThat(v.getIsAvailable()).isFalse();
        assertThat(d.getIsAvailable()).isFalse();
    }

    @Test
    void allocateVehicle_missingVehicle_throws404() throws Exception {
        VehicleRequest r = new VehicleRequest(); r.setId(1L); persisted(r);
        when(vehicleRepository.findById(10L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.allocateVehicle(mapper.readTree(
                "{\"id\":1,\"vehicleId\":10,\"driverId\":20}"), 7L))
                .isInstanceOf(TransportNotFoundException.class);
    }

    @Test
    void allocateVehicle_cancelledRequest_rejected() throws Exception {
        VehicleRequest r = new VehicleRequest(); r.setId(1L); r.setIsCancelled(true); persisted(r);
        assertThatThrownBy(() -> service.allocateVehicle(mapper.readTree(
                "{\"id\":1,\"vehicleId\":10,\"driverId\":20}"), 7L))
                .isInstanceOf(InvalidTransportRequestException.class);
    }

    @Test
    void approve_stampsApprovedBy_andRejectsReapprove() throws Exception {
        VehicleRequest r = new VehicleRequest(); r.setId(1L); r.setIsApproved(false); persisted(r);
        VehicleRequest out = service.approve(1L, "Manager");
        assertThat(out.getIsApproved()).isTrue();
        assertThat(out.getApprovedBy()).isEqualTo("Manager");
        // re-approve now rejected
        assertThatThrownBy(() -> service.approve(1L, "Manager"))
                .isInstanceOf(InvalidTransportRequestException.class);
    }

    @Test
    void vehicleOut_insertsDetailAndFlipsFlag() throws Exception {
        VehicleRequest r = new VehicleRequest(); r.setId(1L); r.setIsVehicleOut(false); persisted(r);
        when(detailsRepository.findByVehicleRequestIdOrderByIdAsc(1L)).thenReturn(List.of());
        VehicleRequest out = service.vehicleOut(mapper.readTree(
                "{\"vehicleRequestId\":1,\"startOdmReading\":1000,\"outTime\":\"2026-08-20T10:00:00Z\"}"), 7L);
        assertThat(out.getIsVehicleOut()).isTrue();
    }

    @Test
    void vehicleOut_alreadyOut_rejected() throws Exception {
        VehicleRequest r = new VehicleRequest(); r.setId(1L); r.setIsVehicleOut(true); persisted(r);
        assertThatThrownBy(() -> service.vehicleOut(mapper.readTree(
                "{\"vehicleRequestId\":1}"), 7L))
                .isInstanceOf(InvalidTransportRequestException.class);
    }

    @Test
    void vehicleIn_completesAndReleases() throws Exception {
        VehicleRequest r = new VehicleRequest(); r.setId(1L); r.setIsVehicleOut(true); r.setCompleted(false);
        r.setVehicleId(10L); r.setDriverId(20L); persisted(r);
        Vehicle v = new Vehicle(); v.setId(10L); v.setIsAvailable(false);
        Driver d = new Driver(); d.setId(20L); d.setIsAvailable(false);
        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(v));
        when(driverRepository.findById(20L)).thenReturn(Optional.of(d));
        VehicleRequestDetails det = new VehicleRequestDetails(); det.setId(5L); det.setVehicleRequestId(1L);
        when(detailsRepository.findByVehicleRequestIdOrderByIdAsc(1L)).thenReturn(List.of(det));

        VehicleRequest out = service.vehicleIn(mapper.readTree(
                "{\"vehicleRequestId\":1,\"endOdmReading\":1050,\"actualFare\":500}"), 7L);
        assertThat(out.getCompleted()).isTrue();
        assertThat(det.getEndOdmReading()).isEqualTo(1050L);
        assertThat(det.getActualFare()).isEqualTo(500L);
        assertThat(v.getIsAvailable()).isTrue();
        assertThat(d.getIsAvailable()).isTrue();
    }

    @Test
    void vehicleIn_notOut_rejected() throws Exception {
        VehicleRequest r = new VehicleRequest(); r.setId(1L); r.setIsVehicleOut(false); persisted(r);
        assertThatThrownBy(() -> service.vehicleIn(mapper.readTree(
                "{\"vehicleRequestId\":1}"), 7L))
                .isInstanceOf(InvalidTransportRequestException.class);
    }

    @Test
    void cancel_flipsFlagAndReleases() throws Exception {
        VehicleRequest r = new VehicleRequest(); r.setId(1L); r.setIsCancelled(false); r.setCompleted(false);
        r.setVehicleId(10L); r.setDriverId(20L); persisted(r);
        Vehicle v = new Vehicle(); v.setId(10L); v.setIsAvailable(false);
        Driver d = new Driver(); d.setId(20L); d.setIsAvailable(false);
        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(v));
        when(driverRepository.findById(20L)).thenReturn(Optional.of(d));

        VehicleRequest out = service.cancel(1L);
        assertThat(out.getIsCancelled()).isTrue();
        assertThat(v.getIsAvailable()).isTrue();
        assertThat(d.getIsAvailable()).isTrue();
    }

    @Test
    void cancel_alreadyCancelled_rejected() throws Exception {
        VehicleRequest r = new VehicleRequest(); r.setId(1L); r.setIsCancelled(true); persisted(r);
        assertThatThrownBy(() -> service.cancel(1L))
                .isInstanceOf(InvalidTransportRequestException.class);
    }

    @Test
    void getCounts_resolvesDictionarySplit() {
        VehicleRequestType emergency = new VehicleRequestType(); emergency.setId(1L); emergency.setDescription("Emergency");
        VehicleRequestType normal = new VehicleRequestType(); normal.setId(2L); normal.setDescription("normal");
        VehicleRequestType scheduled = new VehicleRequestType(); scheduled.setId(3L); scheduled.setDescription("SCHEDULED");
        when(requestTypeRepository.findByStatusTrue()).thenReturn(List.of(emergency, normal, scheduled));
        when(requestRepository.countScoped(any(), org.mockito.ArgumentMatchers.anyBoolean(), any(),
                org.mockito.ArgumentMatchers.anyBoolean(), any(), org.mockito.ArgumentMatchers.anyBoolean(),
                any(), org.mockito.ArgumentMatchers.anyBoolean())).thenAnswer(inv -> {
            boolean hasType = inv.getArgument(7);
            return hasType ? 1L : 4L;
        });

        Map<String, Long> counts = service.getCounts(7L, null);
        assertThat(counts.get("TOTAL")).isEqualTo(4L);
        assertThat(counts.get("EMERGENCY")).isEqualTo(1L);
        assertThat(counts.get("NORMAL")).isEqualTo(1L);
        assertThat(counts.get("SCHEDULED")).isEqualTo(1L);
    }

    @Test
    void getRequest_unknown_throws404() {
        when(requestRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getRequest(999L))
                .isInstanceOf(TransportNotFoundException.class);
    }
}
