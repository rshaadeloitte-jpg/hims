package com.hims.iam.application;

import com.hims.iam.api.dto.AuthDtos.LoginRequest;
import com.hims.iam.api.dto.AuthDtos.LoginResponse;
import com.hims.iam.api.dto.AuthDtos.ValidateResponse;
import com.hims.iam.application.exceptions.InvalidCredentialsException;
import com.hims.iam.domain.model.UserDetail;
import com.hims.iam.domain.model.UserRole;
import com.hims.iam.domain.model.UserRoleMaster;
import com.hims.iam.domain.repository.UserDetailRepository;
import com.hims.iam.domain.repository.UserRoleMasterRepository;
import com.hims.iam.domain.repository.UserRoleRepository;
import com.hims.iam.security.JwtTokenService;
import com.hims.iam.security.TokenRevocationService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Service-layer unit tests for login/logout/validate (Mockito). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock private UserDetailRepository userRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private UserRoleMasterRepository roleMasterRepository;
    @Mock private JwtTokenService jwtTokenService;
    @Mock private TokenRevocationService revocationService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private Claims claims;

    private AuthService authService;
    private UserDetail activeUser;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, userRoleRepository, roleMasterRepository,
                jwtTokenService, revocationService, passwordEncoder);

        activeUser = new UserDetail();
        activeUser.setId(10L);
        activeUser.setUsername("rsharma");
        activeUser.setPasswordHash("$2a$10$stored");
        activeUser.setStatus(Boolean.TRUE);
        activeUser.setUnitId(3L);

        UserRole grant = new UserRole();
        grant.setUserId(10L);
        grant.setRoleId(2L);
        UserRoleMaster staff = new UserRoleMaster();
        staff.setId(2L);
        staff.setCode("staff");

        when(userRepository.findByUsernameIgnoreCase("rsharma")).thenReturn(Optional.of(activeUser));
        when(userRoleRepository.findByUserIdAndStatusTrue(10L)).thenReturn(List.of(grant));
        when(roleMasterRepository.findById(2L)).thenReturn(Optional.of(staff));
    }

    @Test
    void login_success_issuesBearerToken() {
        when(passwordEncoder.matches("secret", "$2a$10$stored")).thenReturn(true);
        when(jwtTokenService.issue("rsharma", 10L, List.of("staff"), 3L)).thenReturn("jwt-token");
        when(jwtTokenService.ttlSeconds()).thenReturn(1800L);

        LoginResponse response = authService.login(new LoginRequest("rsharma", "secret"));

        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresInSeconds()).isEqualTo(1800L);
        assertThat(response.userId()).isEqualTo(10L);
        assertThat(response.roles()).containsExactly("staff");
    }

    @Test
    void login_wrongPassword_throws401CodeIAM001() {
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("rsharma", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class)
                .satisfies(e -> assertThat(((InvalidCredentialsException) e).getErrorCode()).isEqualTo("IAM001"));
        verify(jwtTokenService, never()).issue(any(), anyLong(), any(), any());
    }

    @Test
    void login_unknownUser_throws() {
        when(userRepository.findByUsernameIgnoreCase("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost", "whatever")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid username or password");
    }

    @Test
    void login_disabledAccount_throwsIAM002() {
        activeUser.setStatus(Boolean.FALSE);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("rsharma", "secret")))
                .isInstanceOf(InvalidCredentialsException.class)
                .satisfies(e -> assertThat(((InvalidCredentialsException) e).getErrorCode()).isEqualTo("IAM002"));
    }

    @Test
    void logout_revokesTokenHash() {
        when(jwtTokenService.parse("tok-123")).thenReturn(claims);
        when(claims.getExpiration()).thenReturn(java.sql.Timestamp.from(Instant.now().plusSeconds(60)));

        authService.logout("tok-123");

        verify(revocationService).revoke(anyString(), any(Instant.class));
    }

    @Test
    void logout_withoutToken_throws() {
        assertThatThrownBy(() -> authService.logout(null))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Bearer token required");
        verify(revocationService, never()).revoke(anyString(), any(Instant.class));
    }

    @Test
    void validate_validToken_returnsValidTrue() {
        when(jwtTokenService.parse("good-token")).thenReturn(claims);
        when(revocationService.isRevoked("good-token")).thenReturn(false);
        when(claims.getSubject()).thenReturn("rsharma");
        when(claims.get(JwtTokenService.CLAIM_UID, Long.class)).thenReturn(10L);
        when(jwtTokenService.roles(claims)).thenReturn(List.of("staff"));
        Instant exp = Instant.now().plusSeconds(600);
        when(jwtTokenService.expiryOf(claims)).thenReturn(exp);

        ValidateResponse response = authService.validate("good-token");

        assertThat(response.valid()).isTrue();
        assertThat(response.username()).isEqualTo("rsharma");
        assertThat(response.reason()).isNull();
    }

    @Test
    void validate_garbage_returnsInvalid() {
        when(jwtTokenService.parse("garbage")).thenReturn(null);

        ValidateResponse response = authService.validate("garbage");

        assertThat(response.valid()).isFalse();
        assertThat(response.reason()).isEqualTo("INVALID_OR_EXPIRED");
    }

    @Test
    void validate_revoked_returnsRevoked() {
        when(jwtTokenService.parse("used-token")).thenReturn(claims);
        when(revocationService.isRevoked("used-token")).thenReturn(true);
        when(claims.getSubject()).thenReturn("rsharma");
        when(claims.get(JwtTokenService.CLAIM_UID, Long.class)).thenReturn(10L);
        when(jwtTokenService.roles(claims)).thenReturn(List.of());
        when(jwtTokenService.expiryOf(claims)).thenReturn(Instant.now().plusSeconds(30));

        ValidateResponse response = authService.validate("used-token");

        assertThat(response.valid()).isFalse();
        assertThat(response.reason()).isEqualTo("REVOKED");
    }

    @Test
    void sha256_isDeterministicHex() {
        assertThat(AuthService.sha256("abc")).isEqualTo(AuthService.sha256("abc"))
                .hasSize(64)
                .matches("[0-9a-f]+");
    }

}
