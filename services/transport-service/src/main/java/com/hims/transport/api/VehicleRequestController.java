package com.hims.transport.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.hims.transport.application.VehicleRequestService;
import com.hims.transport.domain.model.VehicleRequest;
import com.hims.transport.domain.model.VehicleRequestDetails;
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
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

/**
 * Transport / vehicle request lifecycle API (MIG-023 sub-slice, S17) — clean
 * target of legacy TransportServiceImpl (raise / allocate / approve /
 * vehicle-out / vehicle-in / cancel, counts, list, fare preview). REST-only:
 * TransportRequested is rejected phase 1 in KAFKA-EVENT-CATALOG.
 */
@RestController
@RequestMapping("/api/v1/transport-requests")
@Tag(name = "Transport Requests (MIG-023)",
        description = "Vehicle / ambulance transport request lifecycle: raise, allocate vehicle+driver, "
                + "approve, vehicle-out, vehicle-in, cancel, counts, list, fare preview")
public class VehicleRequestController {

    private final VehicleRequestService service;

    public VehicleRequestController(VehicleRequestService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Raise a transport request",
            description = "Payload is the request body itself: {requisitionBy, mobileNo, vehicleTypeId, requestType, "
                    + "tripType:[0=drop,1=pickup,3=deadbody], requestMode:[...], isDoctorRequest, patientId, doctorId, "
                    + "staffId, requestDateTime, requestToDateTime, ...}. Patient (non-doctor) requests are auto-approved; "
                    + "requestDateTime defaults to now; expiryDateTime defaults to (requestToDateTime or now) midnight + 1 day.")
    public ResponseEntity<VehicleRequest> raise(@RequestBody(required = false) JsonNode body,
                                                @RequestParam(required = false) Long unitId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.raiseRequest(body, unitId));
    }

    @GetMapping
    @Operation(summary = "List transport requests",
            description = "Unit-scoped day-window search (fromDate truncated to midnight, window = [fromDate, fromDate+1d)). "
                    + "Optional filters: unitId, fromDate, requestType, search (requisitionBy LIKE), mobileNo, isDoctorRequest. "
                    + "Newest first. Cross-domain mrno/designation decoration is deferred.")
    public List<VehicleRequest> list(@RequestParam(required = false) Long unitId,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                     @RequestParam(required = false) Long requestType,
                                     @RequestParam(required = false) String search,
                                     @RequestParam(required = false) Long mobileNo,
                                     @RequestParam(required = false) Boolean isDoctorRequest) {
        return service.listRequests(unitId, toTimestamp(fromDate), requestType, search, mobileNo, isDoctorRequest);
    }

    @GetMapping("/counts")
    @Operation(summary = "Dashboard request counts",
            description = "TOTAL / EMERGENCY / NORMAL / SCHEDULED for the day window starting at fromDate (default today). "
                    + "Type ids resolved from the vehicle_request_type dictionary by description (legacy getTripRequestCount).")
    public Map<String, Long> counts(@RequestParam(required = false) Long unitId,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate) {
        return service.getCounts(unitId, toTimestamp(fromDate));
    }

    @GetMapping("/{id}")
    public VehicleRequest get(@PathVariable long id) {
        return service.getRequest(id);
    }

    @GetMapping("/{id}/details")
    @Operation(summary = "List trip detail rows of a request",
            description = "Vehicle-out / vehicle-in readings + fare (startOdmReading/endOdmReading/tripDistance/"
                    + "calculatedTripFare/actualFare/outTime/inTime). Oldest first.")
    public List<VehicleRequestDetails> details(@PathVariable long id) {
        return service.getDetails(id);
    }

    @GetMapping("/{id}/fare")
    @Operation(summary = "Fare preview for a request",
            description = "Newest detail row decorated with the vehicle's farePerKm (legacy getDetailsForVehicleFare).")
    public VehicleRequestDetails fare(@PathVariable long id,
                                      @RequestParam(required = false) Long vehicleId) {
        return service.getFareDetails(id, vehicleId);
    }

    @PostMapping("/{id}/allocate")
    @Operation(summary = "Allocate a vehicle + driver",
            description = "Body {vehicleId, driverId, location?, requestDateTime?, isChargeExempted?, exemptionCatgId?, "
                    + "requestMode?, allocatedDoctors?, allocatedNurses?}. Stamps the request, flips vehicle+driver "
                    + "isAvailable=false; doctor requests flip isCharged=true (legacy allocateVehicle).")
    public VehicleRequest allocate(@PathVariable long id,
                                   @RequestBody(required = false) JsonNode body,
                                   @RequestParam(required = false) Long unitId) {
        JsonNode withId = injectId(body, id);
        return service.allocateVehicle(withId, unitId);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a request",
            description = "Stamps approvedBy + isApproved=true (legacy approveTransportRequest).")
    public VehicleRequest approve(@PathVariable long id,
                                  @RequestParam(required = false) String approvedBy) {
        return service.approve(id, approvedBy);
    }

    @PostMapping("/{id}/vehicle-out")
    @Operation(summary = "Mark vehicle out",
            description = "Body {startOdmReading?, calculatedTripFare?, tripDistanceEstimated?, outTime?}. Inserts (or "
                    + "updates) a detail row and flips the request isVehicleOut=true (legacy saveVehicleOutDetails).")
    public VehicleRequest vehicleOut(@PathVariable long id,
                                     @RequestBody(required = false) JsonNode body,
                                     @RequestParam(required = false) Long unitId) {
        JsonNode withId = injectRequestId(body, id);
        return service.vehicleOut(withId, unitId);
    }

    @PostMapping("/{id}/vehicle-in")
    @Operation(summary = "Mark vehicle in (complete trip)",
            description = "Body {endOdmReading?, tripDistance?, actualFare?, inTime?}. Stamps the request's detail rows, "
                    + "flips completed=true, and releases the vehicle + driver (legacy saveVehicleInDetails). Billing "
                    + "charges are deferred until the billing integration slice.")
    public VehicleRequest vehicleIn(@PathVariable long id,
                                    @RequestBody(required = false) JsonNode body,
                                    @RequestParam(required = false) Long unitId) {
        JsonNode withId = injectRequestId(body, id);
        return service.vehicleIn(withId, unitId);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a request",
            description = "Flips isCancelled=true and releases any allocated vehicle + driver (legacy cancelTransportRequest).")
    public VehicleRequest cancel(@PathVariable long id) {
        return service.cancel(id);
    }

    // --------------------------------------------------------------- helpers

    private Timestamp toTimestamp(LocalDate date) {
        return date == null ? null
                : Timestamp.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    /** Inject the path id as the body's "id" field (allocate). */
    private JsonNode injectId(JsonNode body, long id) {
        com.fasterxml.jackson.databind.node.ObjectNode node =
                (body != null && body.isObject())
                        ? (com.fasterxml.jackson.databind.node.ObjectNode) body
                        : com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
        node.put("id", id);
        return node;
    }

    /** Inject the path id as the body's "vehicleRequestId" field (vehicle-out / vehicle-in). */
    private JsonNode injectRequestId(JsonNode body, long id) {
        com.fasterxml.jackson.databind.node.ObjectNode node =
                (body != null && body.isObject())
                        ? (com.fasterxml.jackson.databind.node.ObjectNode) body
                        : com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
        node.put("vehicleRequestId", id);
        return node;
    }
}
