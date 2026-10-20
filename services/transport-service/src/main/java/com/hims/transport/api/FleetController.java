package com.hims.transport.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.hims.transport.application.FleetService;
import com.hims.transport.domain.model.AmbulanceType;
import com.hims.transport.domain.model.Driver;
import com.hims.transport.domain.model.FuelStationDetails;
import com.hims.transport.domain.model.Vehicle;
import com.hims.transport.domain.model.VehicleMaintenance;
import com.hims.transport.domain.model.VehicleRequestType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Transport fleet masters + fuel + maintenance API (MIG-023 sub-slice, S17) —
 * clean target of the legacy transport-domain masters (vehicle / driver /
 * request-type / ambulance-type dictionaries) and transactionals (fuel station
 * details, vehicle maintenance). REST-only.
 */
@RestController
@RequestMapping("/api/v1/transport")
@Tag(name = "Transport Fleet (MIG-023)",
        description = "Vehicle + driver masters, request-type + ambulance-type dictionaries, fuel station "
                + "details, and vehicle maintenance lifecycle")
public class FleetController {

    private final FleetService service;

    public FleetController(FleetService service) {
        this.service = service;
    }

    // --------------------------------------------------------------- vehicles

    @PostMapping("/vehicles")
    @Operation(summary = "Create a vehicle", description = "Vehicle / ambulance master (legacy M_AMBULANCEMASTER).")
    public ResponseEntity<Vehicle> createVehicle(@RequestBody(required = false) JsonNode body,
                                                 @RequestParam(required = false) Long unitId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createVehicle(body, unitId));
    }

    @GetMapping("/vehicles")
    @Operation(summary = "List vehicles",
            description = "Unit-scoped active vehicles, optional vehicleType filter (legacy getAllVehicleDetails).")
    public List<Vehicle> listVehicles(@RequestParam(required = false) Long unitId,
                                      @RequestParam(required = false) String vehicleType) {
        return service.listVehicles(unitId, vehicleType);
    }

    @GetMapping("/vehicles/{id}")
    public Vehicle getVehicle(@PathVariable long id) {
        return service.getVehicle(id);
    }

    // ---------------------------------------------------------------- drivers

    @PostMapping("/drivers")
    @Operation(summary = "Create a driver", description = "Driver master (legacy M_DRIVERMASTER).")
    public ResponseEntity<Driver> createDriver(@RequestBody(required = false) JsonNode body,
                                               @RequestParam(required = false) Long unitId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createDriver(body, unitId));
    }

    @GetMapping("/drivers")
    @Operation(summary = "List drivers", description = "Unit-scoped active drivers (legacy getDriverDetails).")
    public List<Driver> listDrivers(@RequestParam(required = false) Long unitId) {
        return service.listDrivers(unitId);
    }

    @GetMapping("/drivers/{id}")
    public Driver getDriver(@PathVariable long id) {
        return service.getDriver(id);
    }

    // ----------------------------------------------------------- dictionaries

    @PostMapping("/request-types")
    @Operation(summary = "Create a request type",
            description = "Request-type dictionary (legacy M_VEHICLEREQUESTTYPEMASTER; emergency/normal/scheduled).")
    public ResponseEntity<VehicleRequestType> createRequestType(@RequestBody(required = false) JsonNode body,
                                                                @RequestParam(required = false) Long unitId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createRequestType(body, unitId));
    }

    @GetMapping("/request-types")
    public List<VehicleRequestType> listRequestTypes() {
        return service.listRequestTypes();
    }

    @PostMapping("/ambulance-types")
    @Operation(summary = "Create an ambulance type",
            description = "Ambulance-type dictionary (legacy M_AMBULANCETYPEMASTER).")
    public ResponseEntity<AmbulanceType> createAmbulanceType(@RequestBody(required = false) JsonNode body,
                                                             @RequestParam(required = false) Long unitId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createAmbulanceType(body, unitId));
    }

    @GetMapping("/ambulance-types")
    public List<AmbulanceType> listAmbulanceTypes() {
        return service.listAmbulanceTypes();
    }

    // ------------------------------------------------------------------- fuel

    @PostMapping("/fuel")
    @Operation(summary = "Add a fuel record",
            description = "Fuel fill-up for a vehicle (legacy addFuelStationDetails); dateTime defaults to now.")
    public ResponseEntity<FuelStationDetails> addFuel(@RequestBody(required = false) JsonNode body,
                                                      @RequestParam(required = false) Long unitId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addFuel(body, unitId));
    }

    @GetMapping("/fuel")
    @Operation(summary = "List fuel records",
            description = "Unit-scoped records in [fromDate, toDate] (toDate inclusive to end of day), optional "
                    + "vehicleTypeId / vehicleId filters (legacy getFuelStationDetails). Newest first.")
    public List<FuelStationDetails> listFuel(@RequestParam(required = false) Long unitId,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                                             @RequestParam(required = false) Long vehicleTypeId,
                                             @RequestParam(required = false) Long vehicleId) {
        return service.listFuel(unitId, toStart(fromDate), toEnd(toDate), vehicleTypeId, vehicleId);
    }

    // ------------------------------------------------------------ maintenance

    @PostMapping("/maintenance")
    @Operation(summary = "Raise a maintenance request",
            description = "Vehicle maintenance (legacy addVehicleMaintenanceRequest); requestDate defaults to now and "
                    + "the vehicle flips isAvailable=false.")
    public ResponseEntity<VehicleMaintenance> addMaintenance(@RequestBody(required = false) JsonNode body,
                                                             @RequestParam(required = false) Long unitId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addMaintenance(body, unitId));
    }

    @GetMapping("/maintenance")
    @Operation(summary = "List maintenance requests",
            description = "Unit-scoped records in [fromDate, toDate] (toDate inclusive to end of day), optional "
                    + "vehicleTypeId / vehicleId filters (legacy getVehicleMaintenanceDetails). Newest first.")
    public List<VehicleMaintenance> listMaintenance(@RequestParam(required = false) Long unitId,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                                                    @RequestParam(required = false) Long vehicleTypeId,
                                                    @RequestParam(required = false) Long vehicleId) {
        return service.listMaintenance(unitId, toStart(fromDate), toEnd(toDate), vehicleTypeId, vehicleId);
    }

    @GetMapping("/maintenance/{id}")
    @Operation(summary = "Get a maintenance request",
            description = "Single read by id (legacy getVehicleMaintenanceRequest; id-only like the request lifecycle).")
    public VehicleMaintenance getMaintenance(@PathVariable long id) {
        return service.getMaintenance(id);
    }

    @PostMapping("/maintenance/{id}/approve")
    @Operation(summary = "Approve a maintenance request",
            description = "isApproved=true (legacy approveVehicleMaintenanceRequest).")
    public VehicleMaintenance approveMaintenance(@PathVariable long id) {
        return service.approveMaintenance(id);
    }

    @PostMapping("/maintenance/{id}/complete")
    @Operation(summary = "Complete a maintenance request",
            description = "Body {actualCost?, maintenanceDate?}; isCompleted=true and the vehicle flips isAvailable=true "
                    + "(legacy completeVehicleMaintenanceRequest).")
    public VehicleMaintenance completeMaintenance(@PathVariable long id,
                                                  @RequestBody(required = false) JsonNode body) {
        return service.completeMaintenance(id, body);
    }

    // --------------------------------------------------------------- helpers

    private Timestamp toStart(LocalDate date) {
        return date == null ? null
                : Timestamp.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    /** Legacy end-of-day inclusive bound (23:59:59). */
    private Timestamp toEnd(LocalDate date) {
        return date == null ? null
                : Timestamp.from(date.atTime(LocalTime.of(23, 59, 59))
                        .atZone(ZoneId.systemDefault()).toInstant());
    }
}
