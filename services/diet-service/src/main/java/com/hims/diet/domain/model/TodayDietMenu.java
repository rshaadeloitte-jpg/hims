package com.hims.diet.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Clean target of legacy T_TODAYDIETMENU (TodayDietItemMenu.java). A row of
 * today's kitchen menu: diet type + session + item + description.
 */
@Entity
@Table(name = "today_diet_menu")
public class TodayDietMenu extends DietBase {

    @Id
    private Long id;

    @Column(name = "diet_type_id")
    private Long dietTypeId;

    @Column(name = "description")
    private String description;

    @Column(name = "diet_session_id")
    private Long dietSessionId;

    @Column(name = "item_id")
    private Long itemId;

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public Long getDietTypeId() { return dietTypeId; }
    public void setDietTypeId(Long dietTypeId) { this.dietTypeId = dietTypeId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getDietSessionId() { return dietSessionId; }
    public void setDietSessionId(Long dietSessionId) { this.dietSessionId = dietSessionId; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
}
