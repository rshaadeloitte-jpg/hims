package com.hims.ot.domain.model;

import jakarta.persistence.*;

@Entity
@Table(name = "t_checklist_item")
public class ChecklistItem extends OtBase {

    @Id
    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "checklist_id", nullable = false)
    private Long checklistId;

    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "completed", nullable = false)
    private Boolean completed = false;

    @Column(name = "notes", length = 500)
    private String notes;

    public Long getId() { return itemId; }
    public void setId(Long id) { this.itemId = id; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public Long getChecklistId() { return checklistId; }
    public void setChecklistId(Long checklistId) { this.checklistId = checklistId; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Boolean getCompleted() { return completed; }
    public void setCompleted(Boolean completed) { this.completed = completed; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
