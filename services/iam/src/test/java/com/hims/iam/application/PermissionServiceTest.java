package com.hims.iam.application;

import com.hims.iam.application.exceptions.InvalidRequestException;
import com.hims.iam.domain.model.ModulePermission;
import com.hims.iam.domain.model.RolePermission;
import com.hims.iam.domain.repository.ModulePermissionRepository;
import com.hims.iam.domain.repository.RolePermissionRepository;
import com.hims.iam.domain.repository.UserDetailRepository;
import com.hims.iam.domain.repository.UserRoleMasterRepository;
import com.hims.iam.domain.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PermissionServiceTest {

    ModulePermissionRepository permRepo;
    RolePermissionRepository rpRepo;
    UserRoleMasterRepository roleRepo;
    UserRoleRepository userRoleRepo;
    UserDetailRepository userRepo;
    PermissionService service;

    @BeforeEach
    void setUp() {
        permRepo = mock(ModulePermissionRepository.class);
        rpRepo = mock(RolePermissionRepository.class);
        roleRepo = mock(UserRoleMasterRepository.class);
        userRoleRepo = mock(UserRoleRepository.class);
        userRepo = mock(UserDetailRepository.class);
        service = new PermissionService(permRepo, rpRepo, roleRepo, userRoleRepo, userRepo);
        when(permRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(rpRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void create_duplicateCode_rejected() {
        when(permRepo.existsByCode("LAB_READ")).thenReturn(true);
        assertThatThrownBy(() -> service.createPermission("LAB_READ", "lab", "x"))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void toJson_fromJson_roundTrip() {
        Set<Long> ids = Set.of(5L, 12L, 7L);
        String json = PermissionService.toJson(ids);
        assertThat(PermissionService.fromJson(json)).containsExactlyInAnyOrder(5L, 12L, 7L);
        assertThat(PermissionService.fromJson("[]")).isEmpty();
        assertThat(PermissionService.fromJson(null)).isEmpty();
    }

    @Test
    void setRolePermissions_validatesPermissionIds() {
        when(roleRepo.findById(1L)).thenReturn(Optional.of(new com.hims.iam.domain.model.UserRoleMaster()));
        when(permRepo.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.setRolePermissions(1L, Set.of(99L)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Permission not found");
    }

    @Test
    void getUserEffectivePrefersDirectOverride() {
        var user = new com.hims.iam.domain.model.UserDetail();
        user.setId(5L);
        when(userRepo.findById(5L)).thenReturn(Optional.of(user));
        var rp = new RolePermission(); rp.setUserId(5L); rp.setEnabledPermissionIds("[9]");
        when(rpRepo.findByUserId(5L)).thenReturn(Optional.of(rp));
        assertThat(service.getUserEffectivePermissions(5L)).containsExactly(9L);
    }
}
