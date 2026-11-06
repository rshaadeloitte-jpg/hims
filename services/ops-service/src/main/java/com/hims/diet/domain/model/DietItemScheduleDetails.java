package com.hims.diet.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

/**
 * Clean target of legacy T_DIETITEMSCHEDULEDETAILS (DietItemScheduleDetails.java).
 * A kitchen food-item schedule row for a diet order + session. Lifecycle
 * (legacy DietServiceImpl): itemScheduleStatus 2=REQUEST_RAISED (default on
 * order), increments toward 6=COMPLETED/LOCKED; rows with status &lt; 6 are
 * replaceable. dietActionStatus enum HOLD/CANCEL/REJECT/RELEASE.
 */
@Entity
@Table(name = "diet_item_schedule_details")
public class DietItemScheduleDetails extends DietBase {

    @Id
    private Long id;

    @Column(name = "admission_id")
    private Long admissionId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "item_ids")
    private String itemIds;

    @Column(name = "item_schedule_status")
    private Long itemScheduleStatus = 2L;

    @Enumerated(EnumType.STRING)
    @Column(name = "diet_action_status")
    private DietActionStatus dietActionStatus;

    @Column(name = "reason_for_hold")
    private String reasonForHold;

    @Column(name = "diet_status_time")
    private Timestamp dietStatusTime;

    @Column(name = "food_ordered_date")
    private Timestamp foodOrderedDate;

    @Column(name = "present_calories")
    private Long presentCalories;

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public Long getAdmissionId() { return admissionId; }
    public void setAdmissionId(Long admissionId) { this.admissionId = admissionId; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public String getItemIds() { return itemIds; }
    public void setItemIds(String itemIds) { this.itemIds = itemIds; }
    public Long getItemScheduleStatus() { return itemScheduleStatus; }
    public void setItemScheduleStatus(Long itemScheduleStatus) { this.itemScheduleStatus = itemScheduleStatus; }
    public DietActionStatus getDietActionStatus() { return dietActionStatus; }
    public void setDietActionStatus(DietActionStatus dietActionStatus) { this.dietActionStatus = dietActionStatus; }
    public String getReasonForHold() { return reasonForHold; }
    public void setReasonForHold(String reasonForHold) { this.reasonForHold = reasonForHold; }
    public Timestamp getDietStatusTime() { return dietStatusTime; }
    public void setDietStatusTime(Timestamp dietStatusTime) { this.dietStatusTime = dietStatusTime; }
    public Timestamp getFoodOrderedDate() { return foodOrderedDate; }
    public void setFoodOrderedDate(Timestamp foodOrderedDate) { this.foodOrderedDate = foodOrderedDate; }
    public Long getPresentCalories() { return presentCalories; }
    public void setPresentCalories(Long presentCalories) { this.presentCalories = presentCalories; }
}
