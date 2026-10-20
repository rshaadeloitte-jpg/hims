package com.hims.iam.application;

import com.hims.iam.application.exceptions.InvalidRequestException;
import com.hims.iam.application.exceptions.ResourceNotFoundException;
import com.hims.iam.domain.model.NotificationTemplate;
import com.hims.iam.domain.repository.NotificationTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Service
public class NotificationTemplateService {

    public static final long AUDIT_ACTOR_ID = 1L;
    private final NotificationTemplateRepository repository;

    public NotificationTemplateService(NotificationTemplateRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<NotificationTemplate> list(String channel) {
        if (channel != null && !channel.isBlank()) {
            return repository.findAll().stream()
                    .filter(t -> t.getChannel().equalsIgnoreCase(channel.trim()) && Boolean.TRUE.equals(t.getStatus()))
                    .toList();
        }
        return repository.findAll().stream().filter(t -> Boolean.TRUE.equals(t.getStatus())).toList();
    }

    @Transactional
    public NotificationTemplate create(String code, String channel, String subject, String body, String smsText) {
        if (code == null || code.isBlank()) throw new InvalidRequestException("IAM_REQ", "code is required");
        if (body == null || body.isBlank()) throw new InvalidRequestException("IAM_REQ", "body is required");
        if (repository.existsByCode(code.trim())) throw new InvalidRequestException("IAM008", "Template code exists: " + code);
        NotificationTemplate t = new NotificationTemplate();
        t.setCode(code.trim());
        t.setChannel(channel == null ? "EMAIL" : channel.trim().toUpperCase());
        t.setSubject(subject);
        t.setBody(body);
        t.setSmsText(smsText);
        t.setStatus(true);
        t.setAddedBy(AUDIT_ACTOR_ID);
        t.setAddedDatetime(Timestamp.from(Instant.now()));
        t.setUpdatedBy(AUDIT_ACTOR_ID);
        t.setUpdatedDatetime(Timestamp.from(Instant.now()));
        return repository.save(t);
    }

    @Transactional
    public NotificationTemplate update(long id, String channel, String subject, String body, String smsText) {
        NotificationTemplate t = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NOT_FOUND", "Template not found: " + id));
        if (channel != null && !channel.isBlank()) t.setChannel(channel.trim().toUpperCase());
        if (subject != null) t.setSubject(subject);
        if (body != null && !body.isBlank()) t.setBody(body);
        if (smsText != null) t.setSmsText(smsText);
        t.setUpdatedBy(AUDIT_ACTOR_ID);
        t.setUpdatedDatetime(Timestamp.from(Instant.now()));
        return repository.save(t);
    }
}
