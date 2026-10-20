package com.hims.diet.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.hims.diet.application.DietOrderService;
import com.hims.diet.domain.model.DietItemScheduleDetails;
import com.hims.diet.domain.model.DietOrderDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
 * Diet order + food item schedule lifecycle API (MIG-023 sub-slice, S18) —
 * clean target of legacy DietServiceImpl (raise / sign / cancel diet orders,
 * order / advance / uncheck / hold / remove food item schedules). REST-only:
 * no diet-produced events in KAFKA-EVENT-CATALOG.
 */
@RestController
@RequestMapping("/api/v1/diet-orders")
@Tag(name = "Diet Orders (MIG-023)",
        description = "Diet order lifecycle (raise / sign / cancel) and food item schedule lifecycle "
                + "(order / advance / uncheck / hold / remove). Patient / admission / ward / bed / "
                + "doctor / service ids are caller-supplied; cross-domain decoration is deferred.")
public class DietOrderController {

    private final DietOrderService service;

    public DietOrderController(DietOrderService service) {
        this.service = service;
    }

    // ------------------------------------------------------- diet orders

    @PostMapping
    @Operation(summary = "Raise a diet order",
            description = "Payload: {patientId, admissionId, wardId, bedId, doctorId, serviceId (diet type), "
                    + "fromDate, toDate, remarks, orderId, orderStatus (2=signed/active, else 1=unreleased)}. "
                    + "Pre-checks that every active session in the unit has a today-menu item for the diet type; "
                    + "replaces the patient's existing ACTIVE order (->3) and its still-raised schedule rows; "
                    + "assigns an autocode order number. Returns 400 BERA035 when serviceId is missing.")
    public ResponseEntity<DietOrderDetails> raise(@RequestBody(required = false) JsonNode body,
                                                  @RequestParam(required = false) Long unitId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.raiseDietOrder(body, unitId));
    }

    @GetMapping
    @Operation(summary = "List diet orders for an admission",
            description = "All orders for an admission in a unit, newest first (legacy getPatientDietOrderList).")
    public List<DietOrderDetails> listByAdmission(@RequestParam Long admissionId,
                                                  @RequestParam Long unitId) {
        return service.listOrdersByAdmission(admissionId, unitId);
    }

    @PostMapping("/filter")
    @Operation(summary = "Filter diet orders",
            description = "Admission-scoped orders with an optional strict fromDate window "
                    + "(fromDate > :fromDate AND fromDate < :toDate). Payload: {admissionId, fromDate, toDate}.")
    public List<DietOrderDetails> filter(@RequestBody(required = false) JsonNode body,
                                         @RequestParam Long unitId) {
        Long admissionId = body != null && body.hasNonNull("admissionId") ? body.get("admissionId").asLong() : null;
        Timestamp from = parseDate(body, "fromDate");
        Timestamp to = parseDate(body, "toDate");
        return service.filterOrders(admissionId, unitId, from, to);
    }

    @GetMapping("/{id}")
    public DietOrderDetails get(@PathVariable long id) {
        return service.getOrder(id);
    }

    @GetMapping("/active")
    @Operation(summary = "Latest ACTIVE diet order for an admission",
            description = "Newest order with orderStatus=2 (legacy getDietOrderDetailByAdmissionId).")
    public DietOrderDetails getLatestActive(@RequestParam Long admissionId,
                                            @RequestParam Long unitId) {
        return service.getLatestActiveOrder(admissionId, unitId);
    }
    @GetMapping("/report")
    public java.util.Map<String, Long> report() { return service.dietReport(); }


    @PutMapping("/sign")
    @Operation(summary = "Sign diet orders",
            description = "Payload: a JSON array of diet order ids. The first becomes ACTIVE (2); the rest "
                    + "become REPLACED (3) (legacy signPatientDietOrders).")
    public Map<String, Boolean> sign(@RequestBody(required = false) JsonNode body,
                                     @RequestParam Long unitId) {
        List<Long> ids = new java.util.ArrayList<>();
        if (body != null && body.isArray()) {
            for (JsonNode n : body) {
                if (n.canConvertToLong()) {
                    ids.add(n.asLong());
                }
            }
        }
        return Map.of("signed", service.signDietOrders(ids, unitId));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Set order status by id",
            description = "Set orderStatus on one order (legacy updateDietOrderDetailsById).")
    public Map<String, Boolean> updateStatusById(@PathVariable long id,
                                                 @RequestParam Long orderStatus,
                                                 @RequestParam Long unitId) {
        return Map.of("updated", service.updateOrderStatusById(id, orderStatus, unitId));
    }

    @PutMapping("/status-by-order-id")
    @Operation(summary = "Set order status by business orderId",
            description = "Set orderStatus on all rows sharing the business orderId field "
                    + "(legacy updateDietOrderDetails). Returns the number of rows updated.")
    public Map<String, Integer> updateStatusByOrderId(@RequestParam Long orderId,
                                                      @RequestParam Long orderStatus) {
        return Map.of("updated", service.updateOrderStatusByOrderId(orderId, orderStatus));
    }

    // ----------------------------------------------- food item schedules

    @PostMapping("/{orderId}/food-items")
    @Operation(summary = "Order food items for today",
            description = "Payload: {admissionId, orderId, sessions:[{sessionId, itemIds:[...], caloriesConsume}]}. "
                    + "Per session: invalidates the session's replaceable (<6) rows for today, skips when a "
                    + "COMPLETED (6) row already exists for today, else inserts a new row with "
                    + "itemScheduleStatus=2 and foodOrderedDate=today (legacy addPatFooddItemOrder same-day core).")
    public ResponseEntity<List<DietItemScheduleDetails>> orderFoodItems(@PathVariable long orderId,
                                                                        @RequestBody(required = false) JsonNode body,
                                                                        @RequestParam Long unitId) {
        if (body != null && body.isObject()) {
            ((com.fasterxml.jackson.databind.node.ObjectNode) body).put("orderId", orderId);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(service.orderFoodItems(body, unitId));
    }

    @GetMapping("/{orderId}/food-items")
    @Operation(summary = "List food item schedule rows of an order",
            description = "Active schedule rows of one diet order, ordered by session then id.")
    public List<DietItemScheduleDetails> listFoodItems(@PathVariable long orderId) {
        return service.listSchedulesByOrder(orderId);
    }

    @PutMapping("/food-items/advance")
    @Operation(summary = "Advance a food item schedule",
            description = "Increment itemScheduleStatus by 1 and stamp dietStatusTime, keyed on "
                    + "(admissionId, dietOrderId, dietSessionId, foodOrderId) for today "
                    + "(legacy updateDietFoodOrder).")
    public DietItemScheduleDetails advance(@RequestParam Long admissionId,
                                           @RequestParam Long dietOrderId,
                                           @RequestParam Long dietSessionId,
                                           @RequestParam Long foodOrderId,
                                           @RequestParam Long unitId) {
        return service.advanceFoodOrder(admissionId, dietOrderId, dietSessionId, foodOrderId, unitId);
    }

    @PutMapping("/food-items/uncheck")
    @Operation(summary = "Uncheck a food item schedule",
            description = "Decrement itemScheduleStatus by 1 and stamp dietStatusTime, keyed on "
                    + "(admissionId, dietOrderId, dietSessionId, foodOrderId) for today "
                    + "(legacy updateUncheckFoodOrder).")
    public DietItemScheduleDetails uncheck(@RequestParam Long admissionId,
                                           @RequestParam Long dietOrderId,
                                           @RequestParam Long dietSessionId,
                                           @RequestParam Long foodOrderId,
                                           @RequestParam Long unitId) {
        return service.uncheckFoodOrder(admissionId, dietOrderId, dietSessionId, foodOrderId, unitId);
    }

    @PutMapping("/food-items/action-status")
    @Operation(summary = "Set food action status",
            description = "Stamp reasonForHold + dietActionStatus (HOLD/CANCEL/REJECT/RELEASE), keyed on "
                    + "(admissionId, dietOrderId, dietSessionId, foodOrderId) for today "
                    + "(legacy updateDietFoodStatusForCancel).")
    public DietItemScheduleDetails setActionStatus(@RequestParam Long admissionId,
                                                   @RequestParam Long dietOrderId,
                                                   @RequestParam Long dietSessionId,
                                                   @RequestParam Long foodOrderId,
                                                   @RequestParam String dietActionStatus,
                                                   @RequestParam(required = false) String reasonForHold,
                                                   @RequestParam Long unitId) {
        return service.setFoodActionStatus(admissionId, dietOrderId, dietSessionId, foodOrderId,
                dietActionStatus, reasonForHold, unitId);
    }

    @PostMapping("/food-items/remove-item")
    @Operation(summary = "Remove one item from a food schedule row",
            description = "Remove one itemId from the itemIds CSV; delete the row when the list becomes "
                    + "empty, else update it (legacy removeDietItem).")
    public DietItemScheduleDetails removeItem(@RequestParam Long unitId,
                                              @RequestParam Long admissionId,
                                              @RequestParam Long dietSessionId,
                                              @RequestParam Long scheduleId,
                                              @RequestParam String itemId) {
        return service.removeDietItem(unitId, admissionId, dietSessionId, scheduleId, itemId);
    }

    @GetMapping("/kitchen")
    @Operation(summary = "Kitchen list",
            description = "Active rows at or past the kitchen stage (itemScheduleStatus >= 3) for a unit "
                    + "today, optionally one session (legacy getDietKichenDetails diet-owned half). "
                    + "Ward / service decoration JOINs are deferred.")
    public List<DietItemScheduleDetails> kitchen(@RequestParam Long unitId,
                                                 @RequestParam(required = false) Long dietSessionId) {
        return service.listKitchen(unitId, dietSessionId);
    }

    // ------------------------------------------------------------- helpers

    private Timestamp parseDate(JsonNode body, String field) {
        if (body == null || !body.hasNonNull(field)) {
            return null;
        }
        try {
            LocalDate date = LocalDate.parse(body.get(field).asText());
            return Timestamp.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
        } catch (Exception e) {
            return null;
        }
    }
}
