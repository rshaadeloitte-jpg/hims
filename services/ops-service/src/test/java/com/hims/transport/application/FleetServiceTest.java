package com.hims.transport.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.transport.application.exceptions.InvalidTransportRequestException;
import com.hims.transport.application.exceptions.TransportNotFoundException;
import com.hims.transport.domain.model.FuelStationDetails;
import com.hims.transport.domain.model.Vehicle;
import com.hims.transport.domain.model.VehicleMaintenance;
import com.hims.transport.domain.repository.AmbulanceTypeRepository;
import com.hims.transport.domain.repository.DriverRepository;
import com.hims.transport.domain.repository.FuelStationDetailsRepository;
import com.hims.transport.domain.repository.VehicleMaintenanceRepository;
import com.hims.transport.domain.repository.VehicleRepository;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/** L1 — fleet masters + fuel + maintenance business rules (availability flips, guards). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FleetServiceTest {

    // Spring Boot's auto-configured mapper ignores unknown properties; mirror that
    // so payload-only fields (requestMode/tripType arrays) don't fail binding.
    private final ObjectMapper mapper = new ObjectMapper()
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Mock private VehicleRepository vehicleRepository;
    @Mock private DriverRepository driverRepository;
    @Mock private VehicleRequestTypeRepository requestTypeRepository;
    @Mock private AmbulanceTypeRepository ambulanceTypeRepository;
    @Mock private FuelStationDetailsRepository fuelRepository;
    @Mock private VehicleMaintenanceRepository maintenanceRepository;
    @Mock private EntityManager em;
    @Mock private Query query;

    private FleetService service;

    @BeforeEach
    void setUp() {
        service = new FleetService(vehicleRepository, driverRepository, requestTypeRepository,
                ambulanceTypeRepository, fuelRepository, maintenanceRepository, mapper);
        ReflectionTestUtils.setField(service, "em", em);
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(1L);
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(inv -> inv.getArgument(0));
        when(fuelRepository.save(any(FuelStationDetails.class))).thenAnswer(inv -> inv.getArgument(0));
        when(maintenanceRepository.save(any(VehicleMaintenance.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createVehicle_defaultsAvailableAndStampsAudit() throws Exception {
        Vehicle v = service.createVehicle(mapper.readTree("{\"vehicleName\":\"Amb1\"}"), 7L);
        assertThat(v.getIsAvailable()).isTrue();
        assertThat(v.getStatus()).isTrue();
        assertThat(v.getUnitId()).isEqualTo(7L);
        assertThat(v.getAddedBy()).isEqualTo(FleetService.AUDIT_ACTOR_ID);
    }

    @Test
    void addFuel_defaultsDateTime() throws Exception {
        FuelStationDetails f = service.addFuel(mapper.readTree("{\"receiptNo\":\"R1\",\"vehicleId\":1}"), 7L);
        assertThat(f.getDateTime()).isNotNull();
        assertThat(f.getUnitId()).isEqualTo(7L);
    }

    @Test
    void addMaintenance_flipsVehicleUnavailable() throws Exception {
        Vehicle v = new Vehicle(); v.setId(10L); v.setIsAvailable(true);
        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(v));

        VehicleMaintenance m = service.addMaintenance(mapper.readTree(
                "{\"workshopName\":\"WS1\",\"vehicleId\":10}"), 7L);
        assertThat(m.getRequestDate()).isNotNull();
        assertThat(m.getIsApproved()).isFalse();
        assertThat(m.getIsCompleted()).isFalse();
        assertThat(v.getIsAvailable()).isFalse();
    }

    @Test
    void approveMaintenance_flipsFlag_andRejectsReapprove() throws Exception {
        VehicleMaintenance m = new VehicleMaintenance(); m.setId(1L); m.setUnitId(7L); m.setIsApproved(false);
        when(maintenanceRepository.findById(1L)).thenReturn(Optional.of(m));

        VehicleMaintenance out = service.approveMaintenance(1L);
        assertThat(out.getIsApproved()).isTrue();
        assertThatThrownBy(() -> service.approveMaintenance(1L))
                .isInstanceOf(InvalidTransportRequestException.class);
    }

    @Test
    void completeMaintenance_stampsCostAndReleasesVehicle() throws Exception {
        VehicleMaintenance m = new VehicleMaintenance(); m.setId(1L); m.setUnitId(7L);
        m.setIsCompleted(false); m.setVehicleId(10L);
        when(maintenanceRepository.findById(1L)).thenReturn(Optional.of(m));
        Vehicle v = new Vehicle(); v.setId(10L); v.setIsAvailable(false);
        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(v));

        VehicleMaintenance out = service.completeMaintenance(1L, mapper.readTree(
                "{\"actualCost\":1500,\"maintenanceDate\":\"2026-08-20T10:00:00Z\"}"));
        assertThat(out.getIsCompleted()).isTrue();
        assertThat(out.getActualCost()).isEqualByComparingTo("1500");
        assertThat(out.getMaintenanceDate()).isNotNull();
        assertThat(v.getIsAvailable()).isTrue();
    }

    @Test
    void completeMaintenance_alreadyCompleted_rejected() throws Exception {
        VehicleMaintenance m = new VehicleMaintenance(); m.setId(1L); m.setUnitId(7L); m.setIsCompleted(true);
        when(maintenanceRepository.findById(1L)).thenReturn(Optional.of(m));
        assertThatThrownBy(() -> service.completeMaintenance(1L, null))
                .isInstanceOf(InvalidTransportRequestException.class);
    }

    @Test
    void getMaintenance_unknown_throws404() {
        when(maintenanceRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getMaintenance(999L))
                .isInstanceOf(TransportNotFoundException.class);
    }
}
