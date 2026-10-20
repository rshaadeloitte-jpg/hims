package com.hims.govt.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Free-text note on an Aasmaan case (register WP-T4).
 * FLAG: table name UNVERIFIED against the legacy Oracle schema — catalog name
 * used as-is (flagged in the migration comments and WP-T4-govt.md too).
 * Guard: notes on CLOSED cases are rejected by the service (400 GOVT_CASE_CLOSED).
 */
@Entity
@Table(name = "t_aasmaan_case_note")
public class AasmaanCaseNote extends GovtBase {

    @Id
    @Column(name = "note_id")
    private Long noteId;

    @Column(name = "case_id", nullable = false)
    private Long caseId;

    @Column(name = "note", nullable = false, columnDefinition = "text")
    private String note;

    @Column(name = "author", length = 80)
    private String author;

    public Long getNoteId() { return noteId; }
    public void setNoteId(Long noteId) { this.noteId = noteId; }
    @Override public Long getId() { return noteId; }
    @Override public void setId(Long id) { this.noteId = id; }
    public Long getCaseId() { return caseId; }
    public void setCaseId(Long caseId) { this.caseId = caseId; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
}
