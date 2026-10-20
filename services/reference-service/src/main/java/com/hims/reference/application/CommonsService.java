package com.hims.reference.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.reference.application.exceptions.InvalidMasterRequestException;
import com.hims.reference.application.exceptions.MasterNotFoundException;
import com.hims.reference.domain.model.AppConfig;
import com.hims.reference.domain.model.AuditLog;
import com.hims.reference.domain.repository.AppConfigRepository;
import com.hims.reference.domain.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class CommonsService {
    private static final long ACTOR = 1L;
    private final AppConfigRepository appConfigRepository;
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CommonsService(AppConfigRepository appConfigRepository, AuditLogRepository auditLogRepository) {
        this.appConfigRepository = appConfigRepository;
        this.auditLogRepository = auditLogRepository;
    }

    // ============================================================ App config

    @Transactional
    public AppConfig setConfig(Long unitId, String key, String value, String description) {
        if (key == null || key.isBlank()) throw new InvalidMasterRequestException("BEXA003", "key is required");
        AppConfig cfg = appConfigRepository.findByUnitIdAndConfigKey(unitId, key).orElseGet(() -> {
            AppConfig fresh = new AppConfig();
            fresh.setId(appConfigRepository.nextId());
            fresh.setUnitId(unitId);
            fresh.setConfigKey(key);
            fresh.setAddedby(ACTOR);
            fresh.setAddeddatetime(Timestamp.from(Instant.now()));
            return fresh;
        });
        cfg.setConfigValue(value);
        cfg.setDescription(description);
        cfg.setUpdatedby(ACTOR);
        cfg.setUpdateddatetime(Timestamp.from(Instant.now()));
        cfg.setSynchronizedd(false);
        cfg.setStatus(true);
        return appConfigRepository.save(cfg);
    }

    @Transactional(readOnly = true)
    public String getConfig(Long unitId, String key) {
        return appConfigRepository.findByUnitIdAndConfigKey(unitId, key)
                .map(AppConfig::getConfigValue).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<AppConfig> listConfig(Long unitId) {
        return appConfigRepository.findByUnitId(unitId);
    }

    // ============================================================ Audit log

    @Transactional
    public AuditLog record(String entityType, Long entityId, String action,
                            Long actorId, String actorUsername, Map<String, Object> diff) {
        if (entityType == null || entityType.isBlank()) throw new InvalidMasterRequestException("BEXA003", "entityType is required");
        if (action == null || action.isBlank()) throw new InvalidMasterRequestException("BEXA003", "action is required");
        AuditLog a = new AuditLog();
        a.setId(auditLogRepository.nextId());
        a.setEntityType(entityType);
        a.setEntityId(entityId);
        a.setAction(action);
        a.setActorId(actorId);
        a.setActorUsername(actorUsername);
        try { a.setDiff(diff == null ? null : objectMapper.writeValueAsString(diff)); }
        catch (JsonProcessingException e) { throw new InvalidMasterRequestException("BEXA003", "diff is not serializable"); }
        a.setOccurredAt(Timestamp.from(Instant.now()));
        a.setAddedby(ACTOR);
        a.setAddeddatetime(Timestamp.from(Instant.now()));
        a.setSynchronizedd(false);
        a.setStatus(true);
        return auditLogRepository.save(a);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> findByEntity(String entityType, Long entityId) {
        return auditLogRepository.findByEntity(entityType, entityId);
    }
    @Transactional(readOnly = true)
    public List<AuditLog> findByActor(Long actorId) {
        return auditLogRepository.findByActor(actorId);
    }
}
