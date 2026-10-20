package com.hims.transport.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.transport.TransportServiceApplication;
import com.hims.transport.application.FleetService;
import com.hims.transport.application.VehicleRequestService;
import com.hims.transport.domain.model.Driver;
import com.hims.transport.domain.model.Vehicle;
import com.hims.transport.domain.model.VehicleMaintenance;
import com.hims.transport.domain.model.VehicleRequest;
import com.hims.transport.domain.model.VehicleRequestDetails;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * L2 — MIG-023 transport service on real PG16: V1 schema applies (ihms_transport),
 * and the full request lifecycle (raise -> allocate -> vehicle-out -> vehicle-in)
 * with vehicle/driver availability flips, plus the maintenance lifecycle.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = TransportServiceApplication.class)
@DirtiesContext
@Transactional
class TransportServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private VehicleRequestService requestService;

    @Autowired
    private FleetService fleetService;

    @Autowired
    private ObjectMapper mapper;

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private org.springframework.transaction.PlatformTransactionManager txManager;

    @BeforeEach
    void resetSchema() {
        em.createNativeQuery("SELECT setval('ihms_transport.\"vehicle_request_seq\"', 1000)").getSingleResult();
        em.createNativeQuery("SELECT setval('ihms_transport.\"vehicle_request_details_seq\"', 2000)").getSingleResult();
        em.createNativeQuery("SELECT setval('ihms_transport.\"vehicle_seq\"', 3000)").getSingleResult();
        em.createNativeQuery("SELECT setval('ihms_transport.\"driver_seq\"', 4000)").getSingleResult();
        em.createNativeQuery("SELECT setval('ihms_transport.\"vehicle_request_type_seq\"', 5000)").getSingleResult();
        em.createNativeQuery("SELECT setval('ihms_transport.\"ambulance_type_seq\"', 6000)").getSingleResult();
        em.createNativeQuery("SELECT setval('ihms_transport.\"fuel_station_details_seq\"', 7000)").getSingleResult();
        em.createNativeQuery("SELECT setval('ihms_transport.\"vehicle_maintenance_seq\"', 8000)").getSingleResult();
        // Service methods run REQUIRES_NEW (commit independently of the @Transactional
        // test, which rolls back), so rows persist across tests in the shared container.
        // Truncate for isolation in a committed tx.
        org.springframework.transaction.support.TransactionTemplate tt =
                new org.springframework.transaction.support.TransactionTemplate(txManager);
        tt.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tt.executeWithoutResult(status -> {
            em.createNativeQuery("""
                    TRUNCATE ihms_transport.vehicle_request_details, ihms_transport.vehicle_request,
                             ihms_transport.vehicle, ihms_transport.driver,
                             ihms_transport.vehicle_request_type, ihms_transport.ambulance_type,
                             ihms_transport.fuel_station_details, ihms_transport.vehicle_maintenance CASCADE""")
                    .executeUpdate();
        });
    }

    @Test
    void v1Schema_applies() {
        List<String> tables = em.createNativeQuery("""
                        SELECT table_name FROM information_schema.tables
                        WHERE table_schema = 'ihms_transport' AND table_name IN
                          ('vehicle_request','vehicle_request_details','vehicle','driver',
                           'vehicle_request_type','ambulance_type','fuel_station_details','vehicle_maintenance')
                        ORDER BY table_name""").getResultList();
        assertThat(tables).containsExactly("ambulance_type", "driver", "fuel_station_details", "vehicle",
                "vehicle_maintenance", "vehicle_request", "vehicle_request_details", "vehicle_request_type");
    }

    @Test
    void fullLifecycle_raiseAllocateOutIn() throws Exception {
        // seed vehicle + driver
        Vehicle v = fleetService.createVehicle(mapper.readTree(
                "{\"vehicleName\":\"Amb1\",\"vehicleNo\":\"RJ14-AB-1234\",\"farePerKm\":10}"), 7L);
        Driver d = fleetService.createDriver(mapper.readTree(
                "{\"firstName\":\"Ravi\",\"drivingLicNo\":\"DL123\"}"), 7L);
        em.clear();

        // raise (patient request -> auto-approved)
        VehicleRequest r = requestService.raiseRequest(mapper.readTree(
                "{\"requisitionBy\":\"Asha\",\"isDoctorRequest\":false,\"tripType\":[0],\"mobileNo\":9999}"), 7L);
        assertThat(r.getIsApproved()).isTrue();
        assertThat(r.getIsDropTrip()).isTrue();

        // allocate
        VehicleRequest allocated = requestService.allocateVehicle(mapper.readTree(
                "{\"id\":" + r.getId() + ",\"vehicleId\":" + v.getId() + ",\"driverId\":" + d.getId() + ",\"location\":\"Ward A\"}"), 7L);
        assertThat(allocated.getVehicleId()).isEqualTo(v.getId());
        em.clear();
        assertThat(fleetService.getVehicle(v.getId()).getIsAvailable()).isFalse();
        assertThat(fleetService.getDriver(d.getId()).getIsAvailable()).isFalse();

        // vehicle-out
        VehicleRequest out = requestService.vehicleOut(mapper.readTree(
                "{\"vehicleRequestId\":" + r.getId() + ",\"startOdmReading\":1000,\"calculatedTripFare\":200}"), 7L);
        assertThat(out.getIsVehicleOut()).isTrue();
        em.clear();
        List<VehicleRequestDetails> details = requestService.getDetails(r.getId());
        assertThat(details).hasSize(1);
        assertThat(details.get(0).getStartOdmReading()).isEqualTo(1000L);

        // vehicle-in (complete + release)
        VehicleRequest in = requestService.vehicleIn(mapper.readTree(
                "{\"vehicleRequestId\":" + r.getId() + ",\"endOdmReading\":1050,\"tripDistance\":50,\"actualFare\":500}"), 7L);
        assertThat(in.getCompleted()).isTrue();
        em.clear();
        List<VehicleRequestDetails> after = requestService.getDetails(r.getId());
        assertThat(after.get(0).getEndOdmReading()).isEqualTo(1050L);
        assertThat(after.get(0).getActualFare()).isEqualTo(500L);
        assertThat(fleetService.getVehicle(v.getId()).getIsAvailable()).isTrue();
        assertThat(fleetService.getDriver(d.getId()).getIsAvailable()).isTrue();

        // counts (no request-type dictionary seeded -> only TOTAL)
        Map<String, Long> counts = requestService.getCounts(7L, null);
        assertThat(counts.get("TOTAL")).isEqualTo(1L);
    }

    @Test
    void counts_dictionarySplit() throws Exception {
        // seed dictionary
        fleetService.createRequestType(mapper.readTree("{\"description\":\"Emergency\"}"), 7L);
        fleetService.createRequestType(mapper.readTree("{\"description\":\"Normal\"}"), 7L);
        em.clear();
        // raise one emergency request
        List<com.hims.transport.domain.model.VehicleRequestType> types = fleetService.listRequestTypes();
        long emergencyId = types.stream().filter(t -> t.getDescription().equalsIgnoreCase("Emergency"))
                .findFirst().orElseThrow().getId();
        requestService.raiseRequest(mapper.readTree(
                "{\"requisitionBy\":\"Asha\",\"requestType\":" + emergencyId + "}"), 7L);

        Map<String, Long> counts = requestService.getCounts(7L, null);
        assertThat(counts.get("TOTAL")).isEqualTo(1L);
        assertThat(counts.get("EMERGENCY")).isEqualTo(1L);
        assertThat(counts.get("NORMAL")).isEqualTo(0L);
    }

    @Test
    void cancel_releasesVehicleAndDriver() throws Exception {
        Vehicle v = fleetService.createVehicle(mapper.readTree("{\"vehicleName\":\"Amb1\"}"), 7L);
        Driver d = fleetService.createDriver(mapper.readTree("{\"firstName\":\"Ravi\"}"), 7L);
        em.clear();
        VehicleRequest r = requestService.raiseRequest(mapper.readTree(
                "{\"requisitionBy\":\"Asha\"}"), 7L);
        requestService.allocateVehicle(mapper.readTree(
                "{\"id\":" + r.getId() + ",\"vehicleId\":" + v.getId() + ",\"driverId\":" + d.getId() + "}"), 7L);
        em.clear();
        assertThat(fleetService.getVehicle(v.getId()).getIsAvailable()).isFalse();

        VehicleRequest cancelled = requestService.cancel(r.getId());
        assertThat(cancelled.getIsCancelled()).isTrue();
        em.clear();
        assertThat(fleetService.getVehicle(v.getId()).getIsAvailable()).isTrue();
        assertThat(fleetService.getDriver(d.getId()).getIsAvailable()).isTrue();
    }

    @Test
    void maintenanceLifecycle_flipsVehicleAvailability() throws Exception {
        Vehicle v = fleetService.createVehicle(mapper.readTree("{\"vehicleName\":\"Amb1\"}"), 7L);
        em.clear();

        VehicleMaintenance m = fleetService.addMaintenance(mapper.readTree(
                "{\"workshopName\":\"WS1\",\"vehicleId\":" + v.getId() + ",\"estimatedCost\":2000}"), 7L);
        em.clear();
        assertThat(fleetService.getVehicle(v.getId()).getIsAvailable()).isFalse();

        fleetService.approveMaintenance(m.getId());
        em.clear();
        VehicleMaintenance completed = fleetService.completeMaintenance(m.getId(), mapper.readTree(
                "{\"actualCost\":1800}"));
        assertThat(completed.getIsCompleted()).isTrue();
        assertThat(completed.getActualCost()).isEqualByComparingTo("1800");
        em.clear();
        assertThat(fleetService.getVehicle(v.getId()).getIsAvailable()).isTrue();
    }

    @Test
    void forwardOnlyGuard_rejectsVehicleInWhenNotOut() throws Exception {
        VehicleRequest r = requestService.raiseRequest(mapper.readTree(
                "{\"requisitionBy\":\"Asha\"}"), 7L);
        em.clear();
        assertThatThrownBy(() -> requestService.vehicleIn(mapper.readTree(
                "{\"vehicleRequestId\":" + r.getId() + "}"), 7L))
                .isInstanceOf(com.hims.transport.application.exceptions.InvalidTransportRequestException.class);
    }

    @Test
    void listRequests_dayWindowAndSearch() throws Exception {
        requestService.raiseRequest(mapper.readTree(
                "{\"requisitionBy\":\"Asha\",\"mobileNo\":9999}"), 7L);
        requestService.raiseRequest(mapper.readTree(
                "{\"requisitionBy\":\"Bhaskar\"}"), 7L);
        em.clear();
        // both today
        assertThat(requestService.listRequests(7L, new java.sql.Timestamp(System.currentTimeMillis()),
                null, null, null, null)).hasSize(2);
        // search by requisitionBy
        assertThat(requestService.listRequests(7L, new java.sql.Timestamp(System.currentTimeMillis()),
                null, "Asha", null, null)).hasSize(1);
        // mobile filter
        assertThat(requestService.listRequests(7L, new java.sql.Timestamp(System.currentTimeMillis()),
                null, null, 9999L, null)).hasSize(1);
    }
}
