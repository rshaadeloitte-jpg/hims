package com.hims.diet.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.hims.diet.application.DietMasterService;
import com.hims.diet.domain.model.DietItemMaster;
import com.hims.diet.domain.model.DietPlanDetails;
import com.hims.diet.domain.model.DietPlanMaster;
import com.hims.diet.domain.model.DietSessionMaster;
import com.hims.diet.domain.model.TodayDietMenu;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Diet masters + today-menu API (MIG-023 sub-slice, S18) — clean target of the
 * legacy diet-domain masters (M_DIETITEMMASTER, M_DIETSESSIONMASTER,
 * M_DIETPLANMASTER, M_DIETPLANDETAILS) and the T_TODAYDIETMENU menu. REST-only:
 * no diet-produced events in KAFKA-EVENT-CATALOG.
 */
@RestController
@RequestMapping("/api/v1/diet")
@Tag(name = "Diet Masters (MIG-023)",
        description = "Diet item / session / plan masters, plan detail lines, and the today-menu "
                + "(add / upsert / remove / list)")
public class DietMasterController {

    private final DietMasterService service;

    public DietMasterController(DietMasterService service) {
        this.service = service;
    }

    // ---------------------------------------------------------- diet items

    @PostMapping("/items")
    @Operation(summary = "Create a diet item",
            description = "Payload: {dietItemType, dietItemIen, description, energy, dietTypeIen, "
                    + "dietSessionId, localUnit}. Legacy addDietItem joins dietSessionIds / "
                    + "dietItemTypes arrays into the comma-string columns before save.")
    public ResponseEntity<DietItemMaster> createItem(@RequestBody(required = false) JsonNode body,
                                                     @RequestParam(required = false) Long unitId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createDietItem(body, unitId));
    }

    @GetMapping("/items")
    @Operation(summary = "List diet items", description = "Unit-scoped active diet items, newest first.")
    public List<DietItemMaster> listItems(@RequestParam(required = false) Long unitId) {
        return service.listDietItems(unitId);
    }

    @GetMapping("/items/{id}")
    public DietItemMaster getItem(@PathVariable long id) {
        return service.getDietItem(id);
    }

    // -------------------------------------------------------- diet sessions

    @PostMapping("/sessions")
    @Operation(summary = "Create a diet session",
            description = "Payload: {description, fromTime, toTime, dietSequenceNumber}. A session is a "
                    + "feeding window (breakfast / lunch / dinner) ordered by dietSequenceNumber.")
    public ResponseEntity<DietSessionMaster> createSession(@RequestBody(required = false) JsonNode body,
                                                           @RequestParam(required = false) Long unitId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createDietSession(body, unitId));
    }

    @GetMapping("/sessions")
    @Operation(summary = "List diet sessions",
            description = "Unit-scoped active sessions ordered by dietSequenceNumber.")
    public List<DietSessionMaster> listSessions(@RequestParam(required = false) Long unitId) {
        return service.listDietSessions(unitId);
    }

    @GetMapping("/sessions/{id}")
    public DietSessionMaster getSession(@PathVariable long id) {
        return service.getDietSession(id);
    }

    // ------------------------------------------------------------ diet plans

    @PostMapping("/plans")
    @Operation(summary = "Create a diet plan",
            description = "Payload: {code, description, planInformation, serviceId}.")
    public ResponseEntity<DietPlanMaster> createPlan(@RequestBody(required = false) JsonNode body,
                                                     @RequestParam(required = false) Long unitId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createDietPlan(body, unitId));
    }

    @GetMapping("/plans")
    @Operation(summary = "List diet plans", description = "Unit-scoped active plans, newest first.")
    public List<DietPlanMaster> listPlans(@RequestParam(required = false) Long unitId) {
        return service.listDietPlans(unitId);
    }

    @GetMapping("/plans/{id}")
    public DietPlanMaster getPlan(@PathVariable long id) {
        return service.getDietPlan(id);
    }

    @PostMapping("/plans/{planId}/details")
    @Operation(summary = "Add a diet plan detail line",
            description = "Payload: {itemId, timing, itemQty, itemUnit, itemCal, itemCh, itemFat, "
                    + "itemExpectedCal, itemInstruction, itemProtein}. planId is taken from the path.")
    public ResponseEntity<DietPlanDetails> addPlanDetail(@PathVariable long planId,
                                                         @RequestBody(required = false) JsonNode body,
                                                         @RequestParam(required = false) Long unitId) {
        if (body != null && body.isObject()) {
            ((com.fasterxml.jackson.databind.node.ObjectNode) body).put("planId", planId);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addDietPlanDetail(body, unitId));
    }

    @GetMapping("/plans/{planId}/details")
    @Operation(summary = "List diet plan detail lines",
            description = "Active detail lines of one plan, ordered by timing (session).")
    public List<DietPlanDetails> listPlanDetails(@PathVariable long planId) {
        return service.listDietPlanDetails(planId);
    }

    // ---------------------------------------------------------- today menu

    @PostMapping("/today-menu")
    @Operation(summary = "Add / upsert a today-menu item",
            description = "Payload: {dietTypeId, dietSessionId, itemId, description}. Upsert on the "
                    + "(unitId, dietTypeId, dietSessionId, itemId) key. A missing row is inserted only "
                    + "when the description is present and not the literal 'Add New Item'; an existing "
                    + "row is re-activated and its description replaced.")
    public ResponseEntity<TodayDietMenu> upsertTodayMenu(@RequestBody(required = false) JsonNode body,
                                                         @RequestParam(required = false) Long unitId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.upsertTodayMenuItem(body, unitId));
    }

    @PostMapping("/today-menu/remove")
    @Operation(summary = "Remove a today-menu item",
            description = "Soft-delete (status=false) the row keyed on (unitId, dietTypeId, "
                    + "dietSessionId, itemId).")
    public TodayDietMenu removeTodayMenu(@RequestParam Long unitId,
                                         @RequestParam Long dietTypeId,
                                         @RequestParam Long dietSessionId,
                                         @RequestParam Long itemId) {
        return service.removeTodayMenuItem(unitId, dietTypeId, dietSessionId, itemId);
    }

    @GetMapping("/today-menu")
    @Operation(summary = "List today-menu items",
            description = "Active menu rows for a unit, optionally narrowed to one diet type.")
    public List<TodayDietMenu> listTodayMenu(@RequestParam Long unitId,
                                             @RequestParam(required = false) Long dietTypeId) {
        return dietTypeId == null
                ? service.listTodayMenu(unitId)
                : service.listTodayMenuByDietType(unitId, dietTypeId);
    }
}
