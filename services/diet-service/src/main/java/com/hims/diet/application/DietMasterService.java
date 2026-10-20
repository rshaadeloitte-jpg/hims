package com.hims.diet.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.diet.application.exceptions.DietNotFoundException;
import com.hims.diet.application.exceptions.InvalidDietRequestException;
import com.hims.diet.domain.model.DietBase;
import com.hims.diet.domain.model.DietItemMaster;
import com.hims.diet.domain.model.DietPlanDetails;
import com.hims.diet.domain.model.DietPlanMaster;
import com.hims.diet.domain.model.DietSessionMaster;
import com.hims.diet.domain.model.TodayDietMenu;
import com.hims.diet.domain.repository.DietItemMasterRepository;
import com.hims.diet.domain.repository.DietPlanDetailsRepository;
import com.hims.diet.domain.repository.DietPlanMasterRepository;
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
import java.util.List;

/**
 * Diet masters + today-menu (MIG-023 sub-slice, S18). Clean target of the
 * legacy diet-domain masters and the today-menu transactional:
 * M_DIETITEMMASTER, M_DIETSESSIONMASTER, M_DIETPLANMASTER, M_DIETPLANDETAILS,
 * and T_TODAYDIETMENU.
 *
 * <p>Preserved legacy behavior:
 * <ul>
 *   <li>diet item / session / plan masters are unit-scoped CRUD with explicit
 *       sequence ids (legacy allowedFilters=status);</li>
 *   <li>addDietItemMenu joins dietSessionIds / dietItemTypes arrays into the
 *       comma-string columns before save;</li>
 *   <li>addDietMenuItem (today menu) is upsert on the (unitId, dietTypeId,
 *       dietSessionId, itemId) key and rejects blank / "Add New Item"
 *       descriptions; removeDietMenuItem soft-deletes (status=false).</li>
 * </ul>
 */
@Service
public class DietMasterService {

    private static final Logger log = LoggerFactory.getLogger(DietMasterService.class);

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id
    private static final String ADD_NEW_ITEM = "Add New Item";

    private final DietItemMasterRepository dietItemRepository;
    private final DietSessionMasterRepository sessionRepository;
    private final DietPlanMasterRepository planRepository;
    private final DietPlanDetailsRepository planDetailsRepository;
    private final TodayDietMenuRepository todayMenuRepository;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager em;

    public DietMasterService(DietItemMasterRepository dietItemRepository,
                             DietSessionMasterRepository sessionRepository,
                             DietPlanMasterRepository planRepository,
                             DietPlanDetailsRepository planDetailsRepository,
                             TodayDietMenuRepository todayMenuRepository,
                             ObjectMapper objectMapper) {
        this.dietItemRepository = dietItemRepository;
        this.sessionRepository = sessionRepository;
        this.planRepository = planRepository;
        this.planDetailsRepository = planDetailsRepository;
        this.todayMenuRepository = todayMenuRepository;
        this.objectMapper = objectMapper;
    }

    // ---------------------------------------------------------- diet items

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DietItemMaster createDietItem(JsonNode payload, Long unitId) {
        DietItemMaster item = bind(payload, DietItemMaster.class);
        item.setId(nextId("diet_item_master_seq"));
        item.setStatus(item.getStatus() == null ? true : item.getStatus());
        stampCreate(item, unitId);
        return dietItemRepository.save(item);
    }

    public List<DietItemMaster> listDietItems(Long unitId) {
        return dietItemRepository.findScoped(unitId, unitId != null);
    }

    public DietItemMaster getDietItem(Long id) {
        return dietItemRepository.findById(id)
                .orElseThrow(() -> new DietNotFoundException("NOT_FOUND", "Diet item not found"));
    }

    // -------------------------------------------------------- diet sessions

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DietSessionMaster createDietSession(JsonNode payload, Long unitId) {
        DietSessionMaster session = bind(payload, DietSessionMaster.class);
        session.setId(nextId("diet_session_master_seq"));
        session.setStatus(session.getStatus() == null ? true : session.getStatus());
        stampCreate(session, unitId);
        return sessionRepository.save(session);
    }

    public List<DietSessionMaster> listDietSessions(Long unitId) {
        return sessionRepository.findScoped(unitId, unitId != null);
    }

    public DietSessionMaster getDietSession(Long id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new DietNotFoundException("NOT_FOUND", "Diet session not found"));
    }

    // ------------------------------------------------------------ diet plans

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DietPlanMaster createDietPlan(JsonNode payload, Long unitId) {
        DietPlanMaster plan = bind(payload, DietPlanMaster.class);
        plan.setId(nextId("diet_plan_master_seq"));
        plan.setStatus(plan.getStatus() == null ? true : plan.getStatus());
        stampCreate(plan, unitId);
        return planRepository.save(plan);
    }

    public List<DietPlanMaster> listDietPlans(Long unitId) {
        return planRepository.findScoped(unitId, unitId != null);
    }

    public DietPlanMaster getDietPlan(Long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new DietNotFoundException("NOT_FOUND", "Diet plan not found"));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DietPlanDetails addDietPlanDetail(JsonNode payload, Long unitId) {
        DietPlanDetails detail = bind(payload, DietPlanDetails.class);
        if (detail.getPlanId() == null) {
            throw new InvalidDietRequestException("BEXA003", "planId is required");
        }
        getDietPlan(detail.getPlanId());
        detail.setId(nextId("diet_plan_details_seq"));
        detail.setStatus(detail.getStatus() == null ? true : detail.getStatus());
        stampCreate(detail, unitId);
        return planDetailsRepository.save(detail);
    }

    public List<DietPlanDetails> listDietPlanDetails(Long planId) {
        getDietPlan(planId);
        return planDetailsRepository.findByPlan(planId);
    }

    // ---------------------------------------------------------- today menu

    /**
     * Legacy addDietMenuItem: upsert on (unitId, dietTypeId, dietSessionId,
     * itemId). A missing row is inserted only when the description is present
     * and not the literal "Add New Item"; an existing row is re-activated and
     * its description replaced.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TodayDietMenu upsertTodayMenuItem(JsonNode payload, Long unitId) {
        TodayDietMenu body = bind(payload, TodayDietMenu.class);
        if (body.getDietTypeId() == null || body.getDietSessionId() == null || body.getItemId() == null) {
            throw new InvalidDietRequestException("BEXA003",
                    "dietTypeId, dietSessionId and itemId are required");
        }
        Long effectiveUnit = body.getUnitId() != null ? body.getUnitId() : unitId;
        return todayMenuRepository.findByKey(effectiveUnit, body.getDietTypeId(),
                        body.getDietSessionId(), body.getItemId())
                .map(existing -> {
                    existing.setDescription(body.getDescription());
                    existing.setStatus(true);
                    stampUpdate(existing);
                    return todayMenuRepository.save(existing);
                })
                .orElseGet(() -> {
                    String description = body.getDescription();
                    if (description == null || description.isBlank()
                            || ADD_NEW_ITEM.equalsIgnoreCase(description)) {
                        throw new InvalidDietRequestException("BEXA003",
                                "A valid description is required to add a menu item");
                    }
                    body.setId(nextId("today_diet_menu_seq"));
                    body.setStatus(true);
                    stampCreate(body, unitId);
                    return todayMenuRepository.save(body);
                });
    }

    /** Legacy removeDietMenuItem: soft-delete (status=false) the keyed row. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TodayDietMenu removeTodayMenuItem(Long unitId, Long dietTypeId, Long dietSessionId, Long itemId) {
        TodayDietMenu existing = todayMenuRepository.findByKey(unitId, dietTypeId, dietSessionId, itemId)
                .orElseThrow(() -> new DietNotFoundException("NOT_FOUND", "Today menu item not found"));
        existing.setStatus(false);
        stampUpdate(existing);
        return todayMenuRepository.save(existing);
    }

    public List<TodayDietMenu> listTodayMenuByDietType(Long unitId, Long dietTypeId) {
        return todayMenuRepository.findByDietType(unitId, dietTypeId);
    }

    public List<TodayDietMenu> listTodayMenu(Long unitId) {
        return todayMenuRepository.findByUnit(unitId);
    }

    // ------------------------------------------------------------- helpers

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
}
