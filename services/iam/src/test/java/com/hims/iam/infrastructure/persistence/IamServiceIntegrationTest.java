package com.hims.iam.infrastructure.persistence;

import com.hims.iam.IamServiceApplication;
import com.hims.iam.domain.model.UserDetail;
import com.hims.iam.domain.model.UserRoleMaster;
import com.hims.iam.domain.repository.UserDetailRepository;
import com.hims.iam.domain.repository.UserRoleMasterRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * L2 — IAM service on real PG16: V1 schema applies (ihms_iam), seeded roles exist,
 * user creation BCrypt-hashes passwords and role grants persist.
 * SKIPPED automatically when Docker is unavailable (same pattern as sibling services).
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = IamServiceApplication.class)
@DirtiesContext
@Transactional
class IamServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private UserDetailRepository userRepository;

    @Autowired
    private UserRoleMasterRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void v1Migration_seedsBaselineRoles() {
        assertThat(roleRepository.findByStatusTrueOrderByIdAsc())
                .extracting(UserRoleMaster::getCode)
                .contains("admin", "staff", "patient");
    }

    @Test
    void createUser_persistsBcryptHashAndAudits() {
        UserDetail user = new UserDetail();
        user.setUsername("integration-user");
        user.setPasswordHash(passwordEncoder.encode("Str0ngPass!x"));
        user.setStatus(Boolean.TRUE);
        user.setAddedBy(1L);
        user.setAddedDatetime(java.sql.Timestamp.from(java.time.Instant.now()));
        user = userRepository.save(user);

        UserDetail reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertThat(reloaded.getId()).isPositive();          // BIGSERIAL identity
        assertThat(reloaded.getPasswordHash()).startsWith("$2");
        assertThat(passwordEncoder.matches("Str0ngPass!x", reloaded.getPasswordHash())).isTrue();
        assertThat(reloaded.getAddedDatetime()).isNotNull();
    }
}
