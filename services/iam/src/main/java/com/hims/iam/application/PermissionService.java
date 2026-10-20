package com.hims.iam.application;

import com.hims.iam.application.exceptions.InvalidRequestException;
import com.hims.iam.application.exceptions.ResourceNotFoundException;
import com.hims.iam.domain.model.ModulePermission;
import com.hims.iam.domain.model.RolePermission;
import com.hims.iam.domain.repository.ModulePermissionRepository;
import com.hims.iam.domain.repository.RolePermissionRepository;
import com.hims.iam.domain.repository.UserDetailRepository;
import com.hims.iam.domain.repository.UserRoleMasterRepository;
import com.hims.iam.domain.repository.UserRoleRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Permission service (M35): module permission CRUD + role/user permission assignment
 * (ported from RoleBasedPermission + ModulePermissionMaster).
 * ponytail: JSON set stored as TEXT; Jackson used for ser/deser — no custom parser.
 */
@Service
public class PermissionService {

    private static final Logger log = LoggerFactory.getLogger(PermissionService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    public static final long AUDIT_ACTOR_ID = 1L;

    private final ModulePermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRoleMasterRepository roleMasterRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserDetailRepository userRepository;

    public PermissionService(ModulePermissionRepository permissionRepository,
                           RolePermissionRepository rolePermissionRepository,
                           UserRoleMasterRepository roleMasterRepository,
                           UserRoleRepository userRoleRepository,
                           UserDetailRepository userRepository) {
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.roleMasterRepository = roleMasterRepository;
        this.userRoleRepository = userRoleRepository;
        this.userRepository = userRepository;
    }

    // ------------------------------------------------------------------ permission CRUD

    @Transactional(readOnly = true)
    public List<ModulePermission> listPermissions(String module) {
        if (module != null && !module.isBlank()) {
            return permissionRepository.findAll().stream()
                    .filter(p -> p.getModule().equalsIgnoreCase(module.trim()) && Boolean.TRUE.equals(p.getStatus()))
                    .toList();
        }
        return permissionRepository.findAll().stream().filter(p -> Boolean.TRUE.equals(p.getStatus())).toList();
    }

    @Transactional
    public ModulePermission createPermission(String code, String module, String description) {
        if (code == null || code.isBlank()) throw new InvalidRequestException("IAM_REQ", "code is required");
        if (module == null || module.isBlank()) throw new InvalidRequestException("IAM_REQ", "module is required");
        if (permissionRepository.existsByCode(code.trim())) {
            throw new InvalidRequestException("IAM008", "Permission code already exists: " + code);
        }
        ModulePermission p = new ModulePermission();
        p.setCode(code.trim());
        p.setModule(module.trim());
        p.setDescription(description);
        p.setStatus(true);
        p.setAddedBy(AUDIT_ACTOR_ID);
        p.setAddedDatetime(Timestamp.from(Instant.now()));
        p.setUpdatedBy(AUDIT_ACTOR_ID);
        p.setUpdatedDatetime(Timestamp.from(Instant.now()));
        p = permissionRepository.save(p);
        log.info("Created permission id={} code={} module={}", p.getId(), p.getCode(), p.getModule());
        return p;
    }

    @Transactional
    public ModulePermission updatePermission(long id, String code, String module, String description) {
        ModulePermission p = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NOT_FOUND", "Permission not found: " + id));
        if (code != null && !code.isBlank() && !code.trim().equalsIgnoreCase(p.getCode())) {
            if (permissionRepository.existsByCode(code.trim())) {
                throw new InvalidRequestException("IAM008", "Permission code already exists: " + code);
            }
            p.setCode(code.trim());
        }
        if (module != null && !module.isBlank()) p.setModule(module.trim());
        if (description != null) p.setDescription(description);
        p.setUpdatedBy(AUDIT_ACTOR_ID);
        p.setUpdatedDatetime(Timestamp.from(Instant.now()));
        return permissionRepository.save(p);
    }

    // ------------------------------------------------------------------ role/user permission grants

    @Transactional
    public RolePermission setRolePermissions(long roleId, Set<Long> permissionIds) {
        roleMasterRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("NOT_FOUND", "Role not found: " + roleId));
        // validate all permission IDs exist
        if (permissionIds != null) {
            for (Long pid : permissionIds) {
                permissionRepository.findById(pid)
                        .orElseThrow(() -> new InvalidRequestException("IAM_REQ", "Permission not found: " + pid));
            }
        }
        RolePermission rp = rolePermissionRepository.findByRoleId(roleId).orElseGet(() -> {
            RolePermission n = new RolePermission();
            n.setRoleId(roleId);
            n.setStatus(true);
            n.setAddedBy(AUDIT_ACTOR_ID);
            n.setAddedDatetime(Timestamp.from(Instant.now()));
            return n;
        });
        rp.setEnabledPermissionIds(toJson(permissionIds));
        rp.setUpdatedBy(AUDIT_ACTOR_ID);
        rp.setUpdatedDatetime(Timestamp.from(Instant.now()));
        rp = rolePermissionRepository.save(rp);
        log.info("Set {} permissions for role {}", permissionIds == null ? 0 : permissionIds.size(), roleId);
        return rp;
    }

    @Transactional
    public RolePermission setUserPermissions(long userId, Set<Long> permissionIds) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("NOT_FOUND", "User not found: " + userId));
        if (permissionIds != null) {
            for (Long pid : permissionIds) {
                permissionRepository.findById(pid)
                        .orElseThrow(() -> new InvalidRequestException("IAM_REQ", "Permission not found: " + pid));
            }
        }
        RolePermission rp = rolePermissionRepository.findByUserId(userId).orElseGet(() -> {
            RolePermission n = new RolePermission();
            n.setUserId(userId);
            n.setStatus(true);
            n.setAddedBy(AUDIT_ACTOR_ID);
            n.setAddedDatetime(Timestamp.from(Instant.now()));
            return n;
        });
        rp.setEnabledPermissionIds(toJson(permissionIds));
        rp.setUpdatedBy(AUDIT_ACTOR_ID);
        rp.setUpdatedDatetime(Timestamp.from(Instant.now()));
        rp = rolePermissionRepository.save(rp);
        log.info("Set {} permissions for user {}", permissionIds == null ? 0 : permissionIds.size(), userId);
        return rp;
    }

    @Transactional(readOnly = true)
    public Set<Long> getRolePermissions(long roleId) {
        roleMasterRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("NOT_FOUND", "Role not found: " + roleId));
        return rolePermissionRepository.findByRoleId(roleId)
                .map(rp -> fromJson(rp.getEnabledPermissionIds()))
                .orElse(Set.of());
    }

    @Transactional(readOnly = true)
    public Set<Long> getUserEffectivePermissions(long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("NOT_FOUND", "User not found: " + userId));
        // direct user override takes precedence if present
        var direct = rolePermissionRepository.findByUserId(userId);
        if (direct.isPresent()) {
            return fromJson(direct.get().getEnabledPermissionIds());
        }
        // else union of all role grants
        var roles = userRoleRepository.findByUserIdAndStatusTrue(userId);
        Set<Long> union = new LinkedHashSet<>();
        for (var ur : roles) {
            rolePermissionRepository.findByRoleId(ur.getRoleId())
                    .ifPresent(rp -> union.addAll(fromJson(rp.getEnabledPermissionIds())));
        }
        return union;
    }

    // ------------------------------------------------------------------ helpers

    static String toJson(Set<Long> ids) {
        try {
            if (ids == null || ids.isEmpty()) return "[]";
            return MAPPER.writeValueAsString(new ArrayList<>(new LinkedHashSet<>(ids)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static Set<Long> fromJson(String json) {
        try {
            if (json == null || json.isBlank() || json.equals("[]")) return new LinkedHashSet<>();
            return new LinkedHashSet<>(MAPPER.readValue(json, new TypeReference<List<Long>>() {}));
        } catch (Exception e) {
            return new LinkedHashSet<>();
        }
    }
}
