package com.hims.transport.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.transport.application.exceptions.InvalidTransportRequestException;
import com.hims.transport.application.exceptions.TransportNotFoundException;
import com.hims.transport.domain.model.AmbulanceType;
import com.hims.transport.domain.model.Driver;
import com.hims.transport.domain.model.FuelStationDetails;
import com.hims.transport.domain.model.TransportBase;
import com.hims.transport.domain.model.Vehicle;
import com.hims.transport.domain.model.VehicleMaintenance;
import com.hims.transport.domain.model.VehicleRequestType;
import com.hims.transport.domain.repository.AmbulanceTypeRepository;
import com.hims.transport.domain.repository.DriverRepository;
import com.hims.transport.domain.repository.FuelStationDetailsRepository;
import com.hims.transport.domain.repository.VehicleMaintenanceRepository;
import com.hims.transport.domain.repository.VehicleRepository;
import com.hims.transport.domain.repository.VehicleRequestTypeRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/**
 * Transport fleet masters + fuel + maintenance (MIG-023 sub-slice, S17). Clean
 * target of the legacy transport-domain masters and transactionals:
 * M_AMBULANCEMASTER (vehicle), M_DRIVERMASTER (driver),
 * M_VEHICLEREQUESTTYPEMASTER, M_AMBULANCETYPEMASTER, t_fuelstationdetails, and
 * t_vehiclemaintenance.
 *
 * <p>Preserved legacy behavior:
 * <ul>
 *   <li>vehicle / driver / dictionary masters are unit-scoped CRUD with explicit
 *       sequence ids;</li>
 *   <li>fuel records (addFuelStationDetails) stamp unitId + audit and are listed
 *       by date range + optional vehicle filters;</li>
 *   <li>maintenance (addVehicleMaintenanceRequest) flips the vehicle
 *       isAvailable=false; approve flips isApproved=true; complete stamps
 *       actualCost + maintenanceDate, flips isCompleted=true, and flips the
 *       vehicle isAvailable=true.</li>
 * </ul>
 *
 * <p>Documented improvements (IMP-DEC-035): maintenance approve/complete reject
 * already-approved / already-completed records (legacy raw UPDATEs had no
 * guard), and complete requires the vehicle to exist.
 */
@Service
public class FleetService {

    private static final Logger log = LoggerFactory.getLogger(FleetService.class);

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id

    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final VehicleRequestTypeRepository requestTypeRepository;
    private final AmbulanceTypeRepository ambulanceTypeRepository;
    private final FuelStationDetailsRepository fuelRepository;
    private final VehicleMaintenanceRepository maintenanceRepository;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager em;

    public FleetService(VehicleRepository vehicleRepository,
                        DriverRepository driverRepository,
                        VehicleRequestTypeRepository requestTypeRepository,
                        AmbulanceTypeRepository ambulanceTypeRepository,
                        FuelStationDetailsRepository fuelRepository,
                        VehicleMaintenanceRepository maintenanceRepository,
                        ObjectMapper objectMapper) {
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
        this.requestTypeRepository = requestTypeRepository;
        this.ambulanceTypeRepository = ambulanceTypeRepository;
        this.fuelRepository = fuelRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.objectMapper = objectMapper;
    }

    // --------------------------------------------------------------- vehicles

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Vehicle createVehicle(JsonNode payload, Long unitId) {
        Vehicle vehicle = bind(payload, Vehicle.class);
        vehicle.setId(nextId("vehicle_seq"));
        vehicle.setIsAvailable(vehicle.getIsAvailable() == null ? true : vehicle.getIsAvailable());
        vehicle.setStatus(vehicle.getStatus() == null ? true : vehicle.getStatus());
        stampCreate(vehicle, unitId);
        return vehicleRepository.save(vehicle);
    }

    public List<Vehicle> listVehicles(Long unitId, String vehicleType) {
        return vehicleRepository.findScoped(unitId, unitId != null,
                vehicleType == null ? "" : vehicleType, vehicleType != null && !vehicleType.isBlank());
    }

    public Vehicle getVehicle(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new TransportNotFoundException("NOT_FOUND", "Vehicle not found"));
    }

    // ---------------------------------------------------------------- drivers

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Driver createDriver(JsonNode payload, Long unitId) {
        Driver driver = bind(payload, Driver.class);
        driver.setId(nextId("driver_seq"));
        driver.setIsAvailable(driver.getIsAvailable() == null ? true : driver.getIsAvailable());
        driver.setStatus(driver.getStatus() == null ? true : driver.getStatus());
        stampCreate(driver, unitId);
        return driverRepository.save(driver);
    }

    public List<Driver> listDrivers(Long unitId) {
        return driverRepository.findByUnitIdAndStatusTrueOrderByIdAsc(unitId);
    }

    public Driver getDriver(Long id) {
        return driverRepository.findById(id)
                .orElseThrow(() -> new TransportNotFoundException("NOT_FOUND", "Driver not found"));
    }

    // ------------------------------------------------------------- dictionaries

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public VehicleRequestType createRequestType(JsonNode payload, Long unitId) {
        VehicleRequestType type = bind(payload, VehicleRequestType.class);
        type.setId(nextId("vehicle_request_type_seq"));
        type.setStatus(type.getStatus() == null ? true : type.getStatus());
        stampCreate(type, unitId);
        return requestTypeRepository.save(type);
    }

    public List<VehicleRequestType> listRequestTypes() {
        return requestTypeRepository.findByStatusTrue();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AmbulanceType createAmbulanceType(JsonNode payload, Long unitId) {
        AmbulanceType type = bind(payload, AmbulanceType.class);
        type.setId(nextId("ambulance_type_seq"));
        type.setStatus(type.getStatus() == null ? true : type.getStatus());
        stampCreate(type, unitId);
        return ambulanceTypeRepository.save(type);
    }

    public List<AmbulanceType> listAmbulanceTypes() {
        return ambulanceTypeRepository.findByStatusTrueOrderByIdAsc();
    }

    // ------------------------------------------------------------------- fuel

    /** Legacy addFuelStationDetails: stamp unitId + audit, save. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public FuelStationDetails addFuel(JsonNode payload, Long unitId) {
        FuelStationDetails fuel = bind(payload, FuelStationDetails.class);
        fuel.setId(nextId("fuel_station_details_seq"));
        if (fuel.getDateTime() == null) {
            fuel.setDateTime(Timestamp.from(Instant.now()));
        }
        fuel.setStatus(fuel.getStatus() == null ? true : fuel.getStatus());
        stampCreate(fuel, unitId);
        return fuelRepository.save(fuel);
    }

    /** Legacy getFuelStationDetails: date range + optional vehicle filters. */
    public List<FuelStationDetails> listFuel(Long unitId, Timestamp fromDate, Timestamp toDate,
                                             Long vehicleTypeId, Long vehicleId) {
        return fuelRepository.search(unitId, unitId != null,
                fromDate, fromDate != null,
                toDate, toDate != null,
                vehicleTypeId, vehicleTypeId != null,
                vehicleId, vehicleId != null);
    }

    // ------------------------------------------------------------ maintenance

    /** Legacy addVehicleMaintenanceRequest: save + flip vehicle isAvailable=false. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public VehicleMaintenance addMaintenance(JsonNode payload, Long unitId) {
        VehicleMaintenance maintenance = bind(payload, VehicleMaintenance.class);
        maintenance.setId(nextId("vehicle_maintenance_seq"));
        if (maintenance.getRequestDate() == null) {
            maintenance.setRequestDate(Timestamp.from(Instant.now()));
        }
        maintenance.setStatus(maintenance.getStatus() == null ? true : maintenance.getStatus());
        stampCreate(maintenance, unitId);
        VehicleMaintenance saved = maintenanceRepository.save(maintenance);

        if (saved.getVehicleId() != null) {
            vehicleRepository.findById(saved.getVehicleId()).ifPresent(v -> {
                v.setIsAvailable(false);
                stampUpdate(v);
                vehicleRepository.save(v);
            });
        }
        log.info("Vehicle maintenance raised id={} vehicle={}", saved.getId(), saved.getVehicleId());
        return saved;
    }

    /** Legacy approveVehicleMaintenanceRequest: isApproved=true. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public VehicleMaintenance approveMaintenance(Long id) {
        VehicleMaintenance maintenance = getMaintenance(id);
        if (Boolean.TRUE.equals(maintenance.getIsApproved())) {
            throw new InvalidTransportRequestException("BEXA003",
                    "Cannot approve: maintenance request is already approved");
        }
        maintenance.setIsApproved(true);
        stampUpdate(maintenance);
        return maintenanceRepository.save(maintenance);
    }

    /** Legacy completeVehicleMaintenanceRequest: actualCost + maintenanceDate + isCompleted=true + release vehicle. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public VehicleMaintenance completeMaintenance(Long id, JsonNode payload) {
        VehicleMaintenance maintenance = getMaintenance(id);
        if (Boolean.TRUE.equals(maintenance.getIsCompleted())) {
            throw new InvalidTransportRequestException("BEXA003",
                    "Cannot complete: maintenance request is already completed");
        }
        if (payload != null && !payload.isNull() && !payload.isMissingNode()) {
            if (payload.hasNonNull("actualCost")) {
                maintenance.setActualCost(new java.math.BigDecimal(payload.get("actualCost").asText()));
            }
            if (payload.hasNonNull("maintenanceDate")) {
                maintenance.setMaintenanceDate(Timestamp.from(Instant.parse(payload.get("maintenanceDate").asText())));
            }
        }
        maintenance.setIsCompleted(true);
        stampUpdate(maintenance);

        if (maintenance.getVehicleId() != null) {
            vehicleRepository.findById(maintenance.getVehicleId()).ifPresent(v -> {
                v.setIsAvailable(true);
                stampUpdate(v);
                vehicleRepository.save(v);
            });
        }
        log.info("Vehicle maintenance id={} completed", id);
        return maintenanceRepository.save(maintenance);
    }

    /** Legacy getVehicleMaintenanceDetails: date range + optional vehicle filters. */
    public List<VehicleMaintenance> listMaintenance(Long unitId, Timestamp fromDate, Timestamp toDate,
                                                    Long vehicleTypeId, Long vehicleId) {
        return maintenanceRepository.search(unitId, unitId != null,
                fromDate, fromDate != null,
                toDate, toDate != null,
                vehicleTypeId, vehicleTypeId != null,
                vehicleId, vehicleId != null);
    }

    /** Legacy getVehicleMaintenanceRequest: single read by id (id-only, like the request lifecycle; avoids the NULL-unit trap). */
    public VehicleMaintenance getMaintenance(Long id) {
        return maintenanceRepository.findById(id)
                .orElseThrow(() -> new TransportNotFoundException("NOT_FOUND", "Vehicle maintenance not found"));
    }

    // --------------------------------------------------------------- helpers

    private <T> T bind(JsonNode payload, Class<T> type) {
        if (payload == null || payload.isNull() || payload.isMissingNode() || !payload.isObject()) {
            throw new InvalidTransportRequestException("BEXA003", "Invalid input data !");
        }
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
