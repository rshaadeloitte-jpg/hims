package com.hims.iam.application;

import com.hims.iam.api.dto.UserDtos.AssignRolesRequest;
import com.hims.iam.api.dto.UserDtos.CreateUserRequest;
import com.hims.iam.api.dto.UserDtos.UpdateUserRequest;
import com.hims.iam.api.dto.UserDtos.UserView;
import com.hims.iam.application.exceptions.InvalidRequestException;
import com.hims.iam.application.exceptions.ResourceNotFoundException;
import com.hims.iam.domain.model.UserDetail;
import com.hims.iam.domain.model.UserRole;
import com.hims.iam.domain.model.UserRoleMaster;
import com.hims.iam.domain.repository.UserDetailRepository;
import com.hims.iam.domain.repository.UserRoleMasterRepository;
import com.hims.iam.domain.repository.UserRoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/**
 * User management write path (Catalog #22 — AddUser/usermanagement slice).
 * Server-owned ids (BIGSERIAL), audit columns stamped server-side, passwords
 * BCrypt-hashed and never returned.
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-00x: principal id

    private final UserDetailRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserRoleMasterRepository roleMasterRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public UserService(UserDetailRepository userRepository,
                       UserRoleRepository userRoleRepository,
                       UserRoleMasterRepository roleMasterRepository,
                       org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleMasterRepository = roleMasterRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserView create(CreateUserRequest request) {
        userRepository.findByUsernameIgnoreCase(request.username().trim()).ifPresent(u -> {
            throw new InvalidRequestException("IAM005", "Username already exists: " + request.username());
        });
        UserDetail user = new UserDetail();
        user.setUsername(request.username().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setEmail(request.email());
        user.setFullName(request.fullName());
        user.setEmployeeId(request.employeeId());
        user.setUnitId(request.unitId());
        user.setStatus(Boolean.TRUE);
        stampCreate(user, request.unitId());
        user = userRepository.save(user);

        if (request.roleIds() != null && !request.roleIds().isEmpty()) {
            grantRoles(user, request.roleIds());
        }
        log.info("Created user id={}", user.getId());
        return toView(user);
    }

    @Transactional(readOnly = true)
    public UserView get(long id) {
        return toView(requireUser(id));
    }

    @Transactional(readOnly = true)
    public List<UserView> list(Long unitId, Long roleId) {
        List<UserDetail> users = unitId == null
                ? userRepository.findByStatusTrueOrderByIdAsc()
                : userRepository.findByStatusTrueAndUnitIdOrderByIdAsc(unitId);
        if (roleId != null) {
            users = users.stream()
                    .filter(u -> userRoleRepository.findByUserIdAndStatusTrue(u.getId()).stream()
                            .anyMatch(r -> roleId.equals(r.getRoleId())))
                    .toList();
        }
        return users.stream().map(this::toView).toList();
    }

    @Transactional
    public UserView update(long id, UpdateUserRequest request) {
        UserDetail user = requireUser(id);
        if (request.email() != null) {
            user.setEmail(request.email());
        }
        if (request.fullName() != null) {
            user.setFullName(request.fullName());
        }
        if (request.employeeId() != null) {
            user.setEmployeeId(request.employeeId());
        }
        if (request.unitId() != null) {
            user.setUnitId(request.unitId());
        }
        if (request.status() != null) {
            user.setStatus(request.status());
        }
        if (request.storeId() != null) user.setStoreId(request.storeId());
        if (request.wardId() != null) user.setWardId(request.wardId());
        if (request.departmentId() != null) user.setDepartmentId(request.departmentId());
        user.setUpdatedBy(AUDIT_ACTOR_ID);
        user.setUpdatedDatetime(Timestamp.from(Instant.now()));
        return toView(userRepository.save(user));
    }

    /** Replace the user's active role grants with the given roleIds (PUT semantics). */
    @Transactional
    public UserView assignRoles(long id, AssignRolesRequest request) {
        UserDetail user = requireUser(id);
        List<Long> roleIds = request.roleIds() == null ? List.of() : request.roleIds();
        for (Long roleId : roleIds) {
            requireRole(roleId);
        }
        List<UserRole> existing = userRoleRepository.findByUserIdAndStatusTrue(id);
        for (UserRole grant : existing) {          // soft-deactivate removed grants
            if (!roleIds.contains(grant.getRoleId())) {
                grant.setStatus(Boolean.FALSE);
                grant.setUpdatedBy(AUDIT_ACTOR_ID);
                grant.setUpdatedDatetime(Timestamp.from(Instant.now()));
                userRoleRepository.save(grant);
            }
        }
        for (Long roleId : roleIds) {              // re-activate or create requested grants
            UserRole match = existing.stream().filter(r -> r.getRoleId().equals(roleId)).findFirst().orElse(null);
            if (match != null && !Boolean.TRUE.equals(match.getStatus())) {
                match.setStatus(Boolean.TRUE);
                match.setUpdatedBy(AUDIT_ACTOR_ID);
                match.setUpdatedDatetime(Timestamp.from(Instant.now()));
                userRoleRepository.save(match);
            } else if (match == null) {
                grantRoles(user, List.of(roleId));
            }
        }
        return toView(user);
    }

    // ------------------------------------------------------------------ helpers

    private void grantRoles(UserDetail user, List<Long> roleIds) {
        for (Long roleId : roleIds) {
            requireRole(roleId);
            boolean alreadyActive = userRoleRepository.findByUserIdAndStatusTrue(user.getId()).stream()
                    .anyMatch(r -> r.getRoleId().equals(roleId));
            if (!alreadyActive) {
                UserRole grant = new UserRole();
                grant.setUserId(user.getId());
                grant.setRoleId(roleId);
                grant.setUnitId(user.getUnitId());
                grant.setStatus(Boolean.TRUE);
                stampCreate(grant, user.getUnitId());
                userRoleRepository.save(grant);
            }
        }
    }

    private UserDetail requireUser(long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("IAM006", "User not found: " + id));
    }

    private UserRoleMaster requireRole(Long roleId) {
        return roleMasterRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("IAM007", "Role not found: " + roleId));
    }

    private UserView toView(UserDetail user) {
        List<UserRole> grants = userRoleRepository.findByUserIdAndStatusTrue(user.getId());
        List<Long> roleIds = grants.stream().map(UserRole::getRoleId).toList();
        List<String> roles = grants.stream()
                .map(g -> roleMasterRepository.findById(g.getRoleId())
                        .map(UserRoleMaster::getCode).orElse(null))
                .filter(code -> code != null)
                .toList();
        return new UserView(user.getId(), user.getUsername(), user.getEmail(), user.getFullName(),
                user.getEmployeeId(), user.getUnitId(), user.getStoreId(), user.getWardId(), user.getDepartmentId(),
                Boolean.TRUE.equals(user.getStatus()),
                roleIds, roles);
    }

    private void stampCreate(com.hims.iam.domain.model.UserDetail entity, Long unitId) {
        Timestamp now = Timestamp.from(Instant.now());
        entity.setAddedBy(AUDIT_ACTOR_ID);
        entity.setAddedDatetime(now);
        entity.setUpdatedBy(AUDIT_ACTOR_ID);
        entity.setUpdatedDatetime(now);
        if (unitId != null) {
            entity.setUnitId(unitId);
        }
    }

    private void stampCreate(com.hims.iam.domain.model.UserRole entity, Long unitId) {
        Timestamp now = Timestamp.from(Instant.now());
        entity.setAddedBy(AUDIT_ACTOR_ID);
        entity.setAddedDatetime(now);
        entity.setUpdatedBy(AUDIT_ACTOR_ID);
        entity.setUpdatedDatetime(now);
        if (unitId != null) {
            entity.setUnitId(unitId);
        }
    }
}
