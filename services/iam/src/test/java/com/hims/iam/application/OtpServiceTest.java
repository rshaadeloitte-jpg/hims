package com.hims.iam.application;

import com.hims.iam.application.exceptions.InvalidCredentialsException;
import com.hims.iam.domain.model.OtpChallenge;
import com.hims.iam.domain.model.UserDetail;
import com.hims.iam.domain.repository.OtpChallengeRepository;
import com.hims.iam.domain.repository.UserDetailRepository;
import com.hims.iam.domain.repository.UserRoleMasterRepository;
import com.hims.iam.domain.repository.UserRoleRepository;
import com.hims.iam.security.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OtpServiceTest {

    OtpChallengeRepository otpRepo;
    UserDetailRepository userRepo;
    UserRoleRepository userRoleRepo;
    UserRoleMasterRepository roleRepo;
    JwtTokenService jwt;
    PasswordEncoder encoder;
    OtpService service;

    @BeforeEach
    void setUp() {
        otpRepo = mock(OtpChallengeRepository.class);
        userRepo = mock(UserDetailRepository.class);
        userRoleRepo = mock(UserRoleRepository.class);
        roleRepo = mock(UserRoleMasterRepository.class);
        jwt = mock(JwtTokenService.class);
        encoder = new BCryptPasswordEncoder();
        service = new OtpService(otpRepo, userRepo, userRoleRepo, roleRepo, jwt, encoder, true);
        when(otpRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRoleRepo.findByUserIdAndStatusTrue(any())).thenReturn(java.util.List.of());
        when(jwt.issue(any(), any(), any(), any())).thenReturn("tok");
        when(jwt.ttlSeconds()).thenReturn(1800L);
    }

    @Test
    void generate_storesHashedOtp() {
        var r = service.generate("  User1 ", "sms");
        assertThat(r.identifier()).isEqualTo("user1");
        assertThat(r.devOtp()).hasSize(6);
        assertThat(r.expiresAt()).isAfter(Timestamp.from(Instant.now()));
    }

    @Test
    void verify_success_issuesJwtAndMarksVerified() {
        String otp = "123456";
        OtpChallenge c = challenge("user1", otp, false, 0);
        when(otpRepo.findTopByIdentifierAndVerifiedFalseOrderByCreatedAtDesc("user1")).thenReturn(Optional.of(c));
        UserDetail u = user(); when(userRepo.findByUsernameIgnoreCase("user1")).thenReturn(Optional.of(u));

        var resp = service.verify("user1", otp);
        assertThat(resp.accessToken()).isEqualTo("tok");
        assertThat(c.getVerified()).isTrue();
    }

    @Test
    void verify_wrongOtp_incrementsAttempts() {
        OtpChallenge c = challenge("user1", "999999", false, 0);
        when(otpRepo.findTopByIdentifierAndVerifiedFalseOrderByCreatedAtDesc("user1")).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.verify("user1", "000000"))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThat(c.getAttempts()).isEqualTo(1);
    }

    @Test
    void verify_expired_rejected() {
        OtpChallenge c = challenge("user1", "123456", false, 0);
        c.setExpiresAt(Timestamp.from(Instant.now().minusSeconds(10)));
        when(otpRepo.findTopByIdentifierAndVerifiedFalseOrderByCreatedAtDesc("user1")).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.verify("user1", "123456"))
                .isInstanceOf(InvalidCredentialsException.class)
                .extracting(e -> ((InvalidCredentialsException) e).getErrorCode())
                .isEqualTo("IAM_OTP_EXPIRED");
    }

    @Test
    void verify_lockedAfter3Attempts() {
        OtpChallenge c = challenge("user1", "123456", false, 3);
        when(otpRepo.findTopByIdentifierAndVerifiedFalseOrderByCreatedAtDesc("user1")).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.verify("user1", "123456"))
                .extracting(e -> ((InvalidCredentialsException) e).getErrorCode())
                .isEqualTo("IAM_OTP_LOCKED");
    }

    private OtpChallenge challenge(String id, String otp, boolean verified, int attempts) {
        OtpChallenge c = new OtpChallenge();
        c.setIdentifier(id); c.setOtpHash(encoder.encode(otp)); c.setVerified(verified);
        c.setAttempts(attempts); c.setMaxAttempts(3);
        c.setExpiresAt(Timestamp.from(Instant.now().plusSeconds(300)));
        c.setCreatedAt(Timestamp.from(Instant.now())); c.setChannel("SMS");
        return c;
    }

    private static UserDetail user() {
        UserDetail u = new UserDetail(); u.setId(1L); u.setUsername("user1"); u.setStatus(true);
        u.setUnitId(1L); u.setPasswordHash("x"); return u;
    }
}
