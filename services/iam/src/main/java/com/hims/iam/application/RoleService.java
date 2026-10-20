package com.hims.iam.application;

import com.hims.iam.api.dto.RoleDtos.CreateRoleRequest;
import com.hims.iam.api.dto.RoleDtos.RoleView;
import com.hims.iam.application.exceptions.InvalidRequestException;
import com.hims.iam.domain.model.UserRoleMaster;
import com.hims.iam.domain.repository.UserRoleMasterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/** Role catalogue write path (T_USERROLEMASTER slice). */
@Service
public class RoleService {

    private static final Logger log = LoggerFactory.getLogger(RoleService.class);

    public static final long AUDIT_ACTOR_ID = 1L;

    private final UserRoleMasterRepository roleRepository;

    public RoleService(UserRoleMasterRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public List<RoleView> list() {
        return roleRepository.findByStatusTrueOrderByIdAsc().stream().map(this::toView).toList();
    }

    @Transactional
    public RoleView create(CreateRoleRequest request) {
        roleRepository.findByCodeIgnoreCase(request.code().trim()).ifPresent(r -> {
            throw new InvalidRequestException("IAM008", "Role code already exists: " + request.code());
        });
        UserRoleMaster role = new UserRoleMaster();
        role.setCode(request.code().trim());
        role.setDescription(request.description());
        role.setUnitId(request.unitId());
        role.setStatus(Boolean.TRUE);
        Timestamp now = Timestamp.from(Instant.now());
        role.setAddedBy(AUDIT_ACTOR_ID);
        role.setAddedDatetime(now);
        role.setUpdatedBy(AUDIT_ACTOR_ID);
        role.setUpdatedDatetime(now);
        role = roleRepository.save(role);
        log.info("Created role id={} code={}", role.getId(), role.getCode());
        return toView(role);
    }

    private RoleView toView(UserRoleMaster role) {
        return new RoleView(role.getId(), role.getCode(), role.getDescription(),
                role.getUnitId(), Boolean.TRUE.equals(role.getStatus()));
    }
}
