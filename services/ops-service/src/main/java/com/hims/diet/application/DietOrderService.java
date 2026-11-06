package com.hims.diet.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.diet.application.exceptions.DietNotFoundException;
import com.hims.diet.application.exceptions.InvalidDietRequestException;
import com.hims.diet.domain.model.DietActionStatus;
import com.hims.diet.domain.model.DietBase;
import com.hims.diet.domain.model.DietItemScheduleDetails;
import com.hims.diet.domain.model.DietOrderDetails;
import com.hims.diet.domain.model.DietSessionMaster;
import com.hims.diet.domain.model.TodayDietMenu;
import com.hims.diet.domain.repository.DietItemScheduleDetailsRepository;
import com.hims.diet.domain.repository.DietOrderDetailsRepository;
import com.hims.diet.domain.repository.DietSessionMasterRepository;
import com.hims.diet.domain.repository.TodayDietMenuRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Diet order + food item schedule lifecycle (MIG-023 sub-slice, S18). Clean
 * target of the legacy T_DIETORDERDETAILS + T_DIETITEMSCHEDULEDETAILS write
 * paths in DietServiceImpl.
 *
 * <p>Status dictionaries (legacy):
 * <ul>
 *   <li>diet_order_details.order_status: 1=UNRELEASED 2=ACTIVE/SIGNED
 *       3=CANCELLED/REPLACED;</li>
 *   <li>diet_item_schedule_details.item_schedule_status: 2=REQUEST_RAISED
 *       (default), increments toward 6=COMPLETED/LOCKED; rows &lt; 6 are
 *       replaceable;</li>
 *   <li>diet_action_status enum: HOLD / CANCEL / REJECT / RELEASE.</li>
 * </ul>
 *
 * <p>Preserved legacy behavior:
 * <ul>
 *   <li>raiseDietOrder (addPatDietOrder): pre-checks that every active session
 *       in the unit has at least one today-menu item for the diet type
 *       (serviceId) — otherwise no order is created; replaces the patient's
 *       existing ACTIVE order (orderStatus -&gt; 3) and invalidates its
 *       still-raised (status=2) schedule rows; creates the new order with an
 *       autocode order number and orderStatus 2 when signed else 1;</li>
 *   <li>signDietOrders (signPatientDietOrders): first order in the list becomes
 *       ACTIVE (2), the rest become REPLACED (3);</li>
 *   <li>orderFoodItems (addPatFooddItemOrder same-day core): per session,
 *       invalidates the session's replaceable (&lt;6) rows for today, skips when
 *       a COMPLETED (6) row already exists for today, else inserts a new row
 *       with itemScheduleStatus=2 and foodOrderedDate=today;</li>
 *   <li>advanceFoodOrder / uncheckFoodOrder (updateDietFoodOrder /
 *       updateUncheckFoodOrder): increment / decrement itemScheduleStatus by 1
 *       and stamp dietStatusTime, keyed on (admissionId, sessionId, orderId,
 *       id) for today;</li>
 *   <li>setFoodActionStatus (updateDietFoodStatusForCancel): stamps
 *       reasonForHold + dietActionStatus;</li>
 *   <li>removeDietItem: removes one itemId from the itemIds CSV; deletes the
 *       row when the list becomes empty.</li>
 * </ul>
 *
 * <p>Deferred cross-domain behavior (documented, IMP-DEC-036): patient /
 * admission / ward / bed / doctor / service resolution and the decoration
 * read-models (getDietOrderList, getDietOrderListCount, getDietKichenDetails
 * ward/service JOINs, getWardWisePatDtls, getActiveWards) JOIN
 * t_admission / t_registration / m_bedmaster / m_wardmaster / m_servicemaster
 * and are deferred until the admission/registration integration slice. The
 * multi-day forward-fill in addPatFooddItemOrder (auto-generating future-day
 * rows from the today-menu) is deferred; the same-day order path is preserved.
 * Callers supply admissionId / patientId / wardId / bedId / doctorId /
 * serviceId as opaque ids.
 */
@Service
public class DietOrderService {

    private static final Logger log = LoggerFactory.getLogger(DietOrderService.class);

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id

    static final long ORDER_UNRELEASED = 1L;
    static final long ORDER_ACTIVE = 2L;
    static final long ORDER_REPLACED = 3L;

    static final long SCHEDULE_REQUEST_RAISED = 2L;
    static final long SCHEDULE_COMPLETED = 6L;

    private final DietOrderDetailsRepository orderRepository;
    private final DietItemScheduleDetailsRepository scheduleRepository;
    private final DietSessionMasterRepository sessionRepository;
    private final TodayDietMenuRepository todayMenuRepository;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager em;

    public DietOrderService(DietOrderDetailsRepository orderRepository,
                            DietItemScheduleDetailsRepository scheduleRepository,
                            DietSessionMasterRepository sessionRepository,
                            TodayDietMenuRepository todayMenuRepository,
                            ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.scheduleRepository = scheduleRepository;
        this.sessionRepository = sessionRepository;
        this.todayMenuRepository = todayMenuRepository;
        this.objectMapper = objectMapper;
    }

    // ------------------------------------------------------- diet orders

    /**
     * Legacy addPatDietOrder (dependency-neutral core). The caller supplies
     * patientId / admissionId / wardId / bedId / doctorId / serviceId (diet
     * type). Pre-checks that every active session in the unit has at least one
     * today-menu item for the diet type; replaces the patient's existing ACTIVE
     * order and its still-raised schedule rows; then creates the new order.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DietOrderDetails raiseDietOrder(JsonNode payload, Long unitId) {
        DietOrderDetails body = bind(payload, DietOrderDetails.class);
        if (body.getServiceId() == null || body.getServiceId() <= 0) {
            // legacy BERA035: orderTypeIen (diet type / serviceId) is mandatory
            throw new InvalidDietRequestException("BERA035", "Diet type (serviceId) is required");
        }
        if (body.getPatientId() == null || body.getAdmissionId() == null) {
            throw new InvalidDietRequestException("BEXA003", "patientId and admissionId are required");
        }
        Long effectiveUnit = body.getUnitId() != null ? body.getUnitId() : unitId;

        // Legacy pre-check: every active session must have a today-menu item
        // for this diet type, otherwise no order is created (returns empty).
        List<DietSessionMaster> sessions = sessionRepository.findScoped(effectiveUnit, true);
        List<TodayDietMenu> menuItems =
                todayMenuRepository.findByDietType(effectiveUnit, body.getServiceId());
        for (DietSessionMaster session : sessions) {
            boolean sessionHasItem = menuItems.stream()
                    .anyMatch(m -> session.getId().equals(m.getDietSessionId()));
            if (!sessionHasItem) {
                throw new InvalidDietRequestException("BEXA003",
                        "No today-menu items configured for session " + session.getId()
                                + " and diet type " + body.getServiceId());
            }
        }

        // Replace the patient's existing ACTIVE order + its still-raised rows.
        orderRepository.findActiveByPatient(body.getPatientId(), effectiveUnit)
                .ifPresent(existing -> {
                    invalidateReplaceableSchedules(existing.getAdmissionId(),
                            existing.getId(), effectiveUnit, body.getFromDate());
                    existing.setOrderStatus(ORDER_REPLACED);
                    stampUpdate(existing);
                    orderRepository.save(existing);
                });

        body.setId(nextId("diet_order_details_seq"));
        body.setOrderNumber(nextOrderNumber());
        body.setUnitId(effectiveUnit);
        // legacy: isSignOrder -> 2 (Active) else 1 (unreleased). The caller
        // signals a signed order by sending orderStatus=2; default is 1.
        if (body.getOrderStatus() == null
                || (body.getOrderStatus() != ORDER_ACTIVE && body.getOrderStatus() != ORDER_UNRELEASED)) {
            body.setOrderStatus(ORDER_UNRELEASED);
        }
        stampCreate(body, effectiveUnit);
        return orderRepository.save(body);
    }

    /** Legacy updateDietOrderDetails: set orderStatus for all rows with orderId. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int updateOrderStatusByOrderId(Long orderId, Long orderStatus) {
        if (orderId == null || orderStatus == null) {
            throw new InvalidDietRequestException("BEXA003", "orderId and orderStatus are required");
        }
        List<DietOrderDetails> rows = orderRepository.findByOrderId(orderId);
        for (DietOrderDetails row : rows) {
            row.setOrderStatus(orderStatus);
            stampUpdate(row);
            orderRepository.save(row);
        }
        return rows.size();
    }

    /** Legacy getPatientDietOrderList: all orders for an admission, newest first. */
    public List<DietOrderDetails> listOrdersByAdmission(Long admissionId, Long unitId) {
        return orderRepository.findByAdmission(admissionId, unitId);
    }

    /** Legacy getFilterwiseDietOrderList: admission + optional strict date range. */
    public List<DietOrderDetails> filterOrders(Long admissionId, Long unitId,
                                               Timestamp fromDate, Timestamp toDate) {
        boolean hasRange = fromDate != null && toDate != null;
        return orderRepository.findFiltered(admissionId, unitId,
                fromDate == null ? Timestamp.from(Instant.EPOCH) : fromDate,
                toDate == null ? Timestamp.from(Instant.EPOCH) : toDate,
                hasRange);
    }

    /**
     * Legacy signPatientDietOrders: the first order in the list becomes ACTIVE
     * (2); every subsequent order becomes REPLACED (3).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean signDietOrders(List<Long> orderIds, Long unitId) {
        if (orderIds == null || orderIds.isEmpty()) {
            throw new InvalidDietRequestException("BEXA003", "At least one order id is required");
        }
        boolean signed = false;
        for (Long id : orderIds) {
            DietOrderDetails details = orderRepository.findById(id)
                    .orElseThrow(() -> new DietNotFoundException("NOT_FOUND",
                            "Diet order not found: " + id));
            details.setOrderStatus(signed ? ORDER_REPLACED : ORDER_ACTIVE);
            signed = true;
            stampUpdate(details);
            orderRepository.save(details);
        }
        return true;
    }

    /** Legacy updateDietOrderDetailsById: set orderStatus on one order by id. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean updateOrderStatusById(Long id, Long orderStatus, Long unitId) {
        if (id == null || orderStatus == null) {
            throw new InvalidDietRequestException("BEXA003", "id and orderStatus are required");
        }
        DietOrderDetails details = orderRepository.findById(id)
                .orElseThrow(() -> new DietNotFoundException("NOT_FOUND", "Diet order not found: " + id));
        details.setOrderStatus(orderStatus);
        stampUpdate(details);
        orderRepository.save(details);
        return true;
    }

    /** Legacy getDietOrderDetailByAdmissionId: newest ACTIVE order for an admission. */
    public DietOrderDetails getLatestActiveOrder(Long admissionId, Long unitId) {
        return orderRepository.findLatestActiveByAdmission(admissionId, unitId)
                .orElseThrow(() -> new DietNotFoundException("NOT_FOUND",
                        "No active diet order for admission " + admissionId));
    }

    public DietOrderDetails getOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new DietNotFoundException("NOT_FOUND", "Diet order not found"));
    }

    // ----------------------------------------------- food item schedules

    /**
     * Legacy addPatFooddItemOrder (same-day core). For each requested session,
     * invalidates the session's replaceable (&lt;6) rows for today, skips when a
     * COMPLETED (6) row already exists for today, else inserts a new row with
     * itemScheduleStatus=2 and foodOrderedDate=today.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<DietItemScheduleDetails> orderFoodItems(JsonNode payload, Long unitId) {
        if (payload == null || !payload.isObject()) {
            throw new InvalidDietRequestException("BEXA003", "Invalid input data !");
        }
        Long admissionId = longField(payload, "admissionId");
        Long orderId = longField(payload, "orderId");
        if (admissionId == null || orderId == null) {
            throw new InvalidDietRequestException("BEXA003", "admissionId and orderId are required");
        }
        getOrder(orderId);
        JsonNode sessionsNode = payload.get("sessions");
        if (sessionsNode == null || !sessionsNode.isArray() || sessionsNode.isEmpty()) {
            throw new InvalidDietRequestException("BEXA003", "sessions array is required");
        }

        Long effectiveUnit = unitId;
        LocalDate today = LocalDate.now();
        Timestamp now = Timestamp.from(Instant.now());
        List<DietItemScheduleDetails> created = new ArrayList<>();

        for (JsonNode sessionNode : sessionsNode) {
            Long sessionId = longField(sessionNode, "sessionId");
            if (sessionId == null) {
                continue;
            }
            List<Long> itemIds = new ArrayList<>();
            JsonNode itemsNode = sessionNode.get("itemIds");
            if (itemsNode != null && itemsNode.isArray()) {
                for (JsonNode itemNode : itemsNode) {
                    if (itemNode.canConvertToLong()) {
                        itemIds.add(itemNode.asLong());
                    }
                }
            }
            if (itemIds.isEmpty()) {
                continue;
            }
            Long calories = longField(sessionNode, "caloriesConsume");

            // Invalidate the session's replaceable rows for today.
            List<DietItemScheduleDetails> replaceable = scheduleRepository.findReplaceable(
                    admissionId, orderId, effectiveUnit, sessionId, true, today, true);
            for (DietItemScheduleDetails row : replaceable) {
                row.setStatus(false);
                stampUpdate(row);
                scheduleRepository.save(row);
            }

            // Skip when a COMPLETED row already exists for today.
            if (scheduleRepository.findCompletedToday(admissionId, sessionId, effectiveUnit, today)
                    .isPresent()) {
                continue;
            }

            DietItemScheduleDetails details = new DietItemScheduleDetails();
            details.setId(nextId("diet_item_schedule_details_seq"));
            details.setAdmissionId(admissionId);
            details.setOrderId(orderId);
            details.setSessionId(sessionId);
            details.setItemIds(joinIds(itemIds));
            details.setItemScheduleStatus(SCHEDULE_REQUEST_RAISED);
            details.setDietStatusTime(now);
            details.setFoodOrderedDate(Timestamp.valueOf(today.atStartOfDay()));
            details.setPresentCalories(calories == null ? 0L : calories);
            stampCreate(details, effectiveUnit);
            created.add(scheduleRepository.save(details));
        }
        return created;
    }

    /** Legacy updateDietFoodOrder: increment itemScheduleStatus by 1 (today). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DietItemScheduleDetails advanceFoodOrder(Long admissionId, Long dietOrderId,
                                                    Long dietSessionId, Long foodOrderId, Long unitId) {
        DietItemScheduleDetails details = requireScheduleRow(admissionId, dietOrderId,
                dietSessionId, foodOrderId, unitId);
        details.setItemScheduleStatus(details.getItemScheduleStatus() + 1);
        details.setDietStatusTime(Timestamp.from(Instant.now()));
        stampUpdate(details);
        return scheduleRepository.save(details);
    }

    /** Legacy updateUncheckFoodOrder: decrement itemScheduleStatus by 1 (today). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DietItemScheduleDetails uncheckFoodOrder(Long admissionId, Long dietOrderId,
                                                    Long dietSessionId, Long foodOrderId, Long unitId) {
        DietItemScheduleDetails details = requireScheduleRow(admissionId, dietOrderId,
                dietSessionId, foodOrderId, unitId);
        details.setItemScheduleStatus(details.getItemScheduleStatus() - 1);
        details.setDietStatusTime(Timestamp.from(Instant.now()));
        stampUpdate(details);
        return scheduleRepository.save(details);
    }

    /** Legacy updateDietFoodStatusForCancel: stamp reasonForHold + dietActionStatus. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DietItemScheduleDetails setFoodActionStatus(Long admissionId, Long dietOrderId,
                                                       Long dietSessionId, Long foodOrderId,
                                                       String actionStatus, String reasonForHold,
                                                       Long unitId) {
        if (actionStatus == null || actionStatus.isBlank()) {
            throw new InvalidDietRequestException("BEXA003", "dietActionStatus is required");
        }
        DietActionStatus status;
        try {
            status = DietActionStatus.valueOf(actionStatus.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidDietRequestException("BEXA003",
                    "Invalid dietActionStatus: " + actionStatus);
        }
        DietItemScheduleDetails details = requireScheduleRow(admissionId, dietOrderId,
                dietSessionId, foodOrderId, unitId);
        details.setReasonForHold(reasonForHold);
        details.setDietActionStatus(status);
        stampUpdate(details);
        return scheduleRepository.save(details);
    }

    /**
     * Legacy removeDietItem: remove one itemId from the itemIds CSV; delete the
     * row when the list becomes empty, else update it.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DietItemScheduleDetails removeDietItem(Long unitId, Long admissionId,
                                                  Long dietSessionId, Long scheduleId,
                                                  String itemIdToRemove) {
        DietItemScheduleDetails details = scheduleRepository.findForRemove(unitId, admissionId,
                        dietSessionId, scheduleId)
                .orElseThrow(() -> new DietNotFoundException("NOT_FOUND",
                        "Diet schedule row not found"));
        String itemIds = details.getItemIds() == null ? "" : details.getItemIds();
        List<String> remaining = new ArrayList<>(Arrays.asList(itemIds.split(",")));
        remaining.removeIf(s -> s.isBlank());
        if (itemIdToRemove != null) {
            remaining.remove(itemIdToRemove);
        }
        if (remaining.isEmpty()) {
            scheduleRepository.delete(details);
            details.setItemIds("");
            return details;
        }
        details.setItemIds(String.join(",", remaining));
        stampUpdate(details);
        return scheduleRepository.save(details);
    }

    /**
     * Legacy getDietKichenDetails (diet-owned half): active rows at or past the
     * kitchen stage (itemScheduleStatus &gt;= 3) for a unit today, optionally one
     * session. Ward / service decoration JOINs are deferred (cross-domain).
     */
    public List<DietItemScheduleDetails> listKitchen(Long unitId, Long dietSessionId) {
        return scheduleRepository.findKitchenList(unitId, LocalDate.now(), true,
                dietSessionId == null ? 0L : dietSessionId, dietSessionId != null);
    }

    /** Active schedule rows of one diet order, ordered by session then id. */
    public List<DietItemScheduleDetails> listSchedulesByOrder(Long orderId) {
        getOrder(orderId);
        return scheduleRepository.findByOrder(orderId);
    }

    // ------------------------------------------------------------- helpers

    private DietItemScheduleDetails requireScheduleRow(Long admissionId, Long dietOrderId,
                                                       Long dietSessionId, Long foodOrderId,
                                                       Long unitId) {
        if (admissionId == null || dietOrderId == null || dietSessionId == null || foodOrderId == null) {
            throw new InvalidDietRequestException("BEXA003",
                    "admissionId, dietOrderId, dietSessionId and foodOrderId are required");
        }
        return scheduleRepository.findByCompositeKey(admissionId, dietSessionId, dietOrderId,
                        foodOrderId, LocalDate.now(), true)
                .orElseThrow(() -> new DietNotFoundException("NOT_FOUND",
                        "Diet schedule row not found"));
    }

    private void invalidateReplaceableSchedules(Long admissionId, Long orderId, Long unitId,
                                                Timestamp fromDate) {
        LocalDate day = fromDate == null ? null : fromDate.toLocalDateTime().toLocalDate();
        List<DietItemScheduleDetails> rows = scheduleRepository.findReplaceable(
                admissionId, orderId, unitId, 0L, false, day, day != null);
        for (DietItemScheduleDetails row : rows) {
            // Legacy invalidates rows that are still REQUEST_RAISED (2) or whose
            // foodOrderedDate matches the new order's fromDate. The findReplaceable
            // query already narrows to status<6; when a fromDate is given it also
            // narrows to that day. Both legacy conditions are covered.
            row.setStatus(false);
            stampUpdate(row);
            scheduleRepository.save(row);
        }
    }

    private String joinIds(List<Long> ids) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(ids.get(i));
        }
        return sb.toString();
    }

    private Long longField(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || !value.canConvertToLong()) {
            return null;
        }
        return value.asLong();
    }

    private <T> T bind(JsonNode payload, Class<T> type) {
        if (payload == null || payload.isNull() || payload.isMissingNode() || !payload.isObject()) {
            throw new InvalidDietRequestException("BEXA003", "Invalid input data !");
        }
        try {
            return objectMapper.treeToValue(payload, type);
        } catch (Exception e) {
            throw new InvalidDietRequestException("BEXA003", "Invalid input data !");
        }
    }

    private void stampCreate(DietBase entity, Long unitId) {
        entity.setAddedBy(AUDIT_ACTOR_ID);
        entity.setAddedDatetime(Timestamp.from(Instant.now()));
        entity.setUpdatedBy(null);
        entity.setUpdatedDatetime(null);
        entity.setSynchronizedd(false);
        if (unitId != null) {
            entity.setUnitId(unitId);
        }
    }

    private void stampUpdate(DietBase entity) {
        entity.setUpdatedBy(AUDIT_ACTOR_ID);
        entity.setUpdatedDatetime(Timestamp.from(Instant.now()));
    }

    private long nextId(String sequenceName) {
        return ((Number) em.createNativeQuery("SELECT nextval('ihms_diet.\"" + sequenceName + "\"')")
                .getSingleResult()).longValue();
    }

    /** Legacy autocode order number (CIMS_GETLATESTAUTOCODEANDINCREMETITBYONE on 't_dietorderdetails'). */
    private String nextOrderNumber() {
        long value = ((Number) em.createNativeQuery("SELECT nextval('ihms_diet.\"diet_order_number_seq\"')")
                .getSingleResult()).longValue();
        return String.format("%05d", value);
    }

    /** M10 diet patient report: count of diet orders by orderStatus. */
    @Transactional(readOnly = true)
    public java.util.Map<String, Long> dietReport() {
        java.util.List<DietOrderDetails> all = orderRepository.findAll();
        java.util.Map<String, Long> counts = new java.util.LinkedHashMap<>();
        counts.put("TOTAL", (long) all.size());
        counts.put("UNRELEASED", all.stream().filter(o -> o.getOrderStatus() != null && o.getOrderStatus() == 1L).count());
        counts.put("ACTIVE", all.stream().filter(o -> o.getOrderStatus() != null && o.getOrderStatus() == 2L).count());
        counts.put("CANCELLED", all.stream().filter(o -> o.getOrderStatus() != null && o.getOrderStatus() == 3L).count());
        return counts;
    }
}
