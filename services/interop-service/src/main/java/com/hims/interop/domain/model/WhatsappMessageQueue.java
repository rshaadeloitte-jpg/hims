package com.hims.interop.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * WhatsApp notification queue row (register WP-T1). FORWARD-ONLY status
 * machine (pinned by tests):
 *   0=QUEUED -&gt; 1=SENT -&gt; 2=DELIVERED -&gt; 3=READ (strictly one step forward)
 *   4=FAILED allowed ONLY from 0 or 1
 *   everything else -> 400 INTEROP_ILLEGAL_TRANSITION.
 * The provider send / webhook-receive jobs are OUT OF SCOPE (comment-only);
 * PUT .../status is the webhook emulation surface.
 */
@Entity
@Table(name = "t_whatsapp_message_queue")
public class WhatsappMessageQueue extends InteropBase {

    public static final short STATUS_QUEUED = 0;
    public static final short STATUS_SENT = 1;
    public static final short STATUS_DELIVERED = 2;
    public static final short STATUS_READ = 3;
    public static final short STATUS_FAILED = 4;

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "template", length = 60)
    private String template;

    @Column(name = "recipient", nullable = false, length = 20)
    private String recipient;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_vars", columnDefinition = "jsonb")
    private String payloadVars;

    @Column(name = "status", nullable = false)
    private Short status = STATUS_QUEUED;

    @Column(name = "provider_message_id", length = 64)
    private String providerMessageId;

    /** Not persisted: transition history loaded via repository in the service layer. */
    @jakarta.persistence.Transient
    private List<WhatsappMessageStatus> history = new ArrayList<>();

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public String getTemplate() { return template; }
    public void setTemplate(String template) { this.template = template; }
    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }
    public String getPayloadVars() { return payloadVars; }
    public void setPayloadVars(String payloadVars) { this.payloadVars = payloadVars; }
    public Short getStatus() { return status; }
    public void setStatus(Short status) { this.status = status; }
    public String getProviderMessageId() { return providerMessageId; }
    public void setProviderMessageId(String providerMessageId) { this.providerMessageId = providerMessageId; }
    public List<WhatsappMessageStatus> getHistory() { return history; }
    public void setHistory(List<WhatsappMessageStatus> history) { this.history = history; }

    /** Forward-only machine: the single legal successor on the delivery path. */
    public static Short nextForward(short status) {
        return switch (status) {
            case STATUS_QUEUED -> STATUS_SENT;
            case STATUS_SENT -> STATUS_DELIVERED;
            case STATUS_DELIVERED -> STATUS_READ;
            default -> null;
        };
    }

    /** FAILED (4) is legal ONLY from QUEUED or SENT — never after DELIVERED/READ. */
    public boolean canFail() {
        return status == STATUS_QUEUED || status == STATUS_SENT;
    }
}
