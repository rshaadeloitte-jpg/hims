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
 * SMS notification queue (MIG-055). eSanchar gateway outbound — same
 * forward-only status machine pattern as the WhatsApp queue:
 *   0=QUEUED 1=SENT 2=DELIVERED 3=DELIVERY_FAILED 4=EXPIRED (terminal)
 * PUT .../status applies a webhook-driven transition; every accepted
 * transition appends a row to t_sms_notification_status.
 * The provider send job (send.sms.url / send.sms.url.otp) is OUT OF SCOPE
 * (comment-only per register WP-T1).
 */
@Entity
@Table(name = "t_sms_notification_queue")
public class SmsNotificationQueue extends InteropBase {

    public static final short STATUS_QUEUED = 0;
    public static final short STATUS_SENT = 1;
    public static final short STATUS_DELIVERED = 2;
    public static final short STATUS_DELIVERY_FAILED = 3;
    public static final short STATUS_EXPIRED = 4;

    public static final String GATEWAY_ESANCHAR = "eSanchar";

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "template", length = 60)
    private String template;

    @Column(name = "recipient", nullable = false, length = 20)
    private String recipient;

    @Column(name = "gateway", nullable = false, length = 20)
    private String gateway = GATEWAY_ESANCHAR;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_vars", columnDefinition = "jsonb")
    private String payloadVars;

    @Column(name = "status", nullable = false)
    private Short status = STATUS_QUEUED;

    @Column(name = "provider_message_id", length = 64)
    private String providerMessageId;

    /** Not persisted: transition history loaded via repository in the service layer. */
    @jakarta.persistence.Transient
    private List<SmsNotificationStatus> history = new ArrayList<>();

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public String getTemplate() { return template; }
    public void setTemplate(String template) { this.template = template; }
    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }
    public String getGateway() { return gateway; }
    public void setGateway(String gateway) { this.gateway = gateway; }
    public String getPayloadVars() { return payloadVars; }
    public void setPayloadVars(String payloadVars) { this.payloadVars = payloadVars; }
    public Short getStatus() { return status; }
    public void setStatus(Short status) { this.status = status; }
    public String getProviderMessageId() { return providerMessageId; }
    public void setProviderMessageId(String providerMessageId) { this.providerMessageId = providerMessageId; }
    public List<SmsNotificationStatus> getHistory() { return history; }
    public void setHistory(List<SmsNotificationStatus> history) { this.history = history; }

    /** Forward-only machine: the single legal successor on the delivery path. */
    public static Short nextForward(short status) {
        return switch (status) {
            case STATUS_QUEUED -> STATUS_SENT;
            case STATUS_SENT -> STATUS_DELIVERED;
            default -> null;
        };
    }

    /** DELIVERY_FAILED (3) is legal ONLY from QUEUED or SENT; EXPIRED (4) from any non-terminal. */
    public boolean canFail() {
        return status == STATUS_QUEUED || status == STATUS_SENT;
    }
}
