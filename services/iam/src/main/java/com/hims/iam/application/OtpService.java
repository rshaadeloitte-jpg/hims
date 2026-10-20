package com.hims.iam.application;

import com.hims.iam.api.dto.AuthDtos.LoginResponse;
import com.hims.iam.application.exceptions.InvalidCredentialsException;
import com.hims.iam.application.exceptions.InvalidRequestException;
import com.hims.iam.domain.model.OtpChallenge;
import com.hims.iam.domain.model.UserDetail;
import com.hims.iam.domain.repository.OtpChallengeRepository;
import com.hims.iam.domain.repository.UserDetailRepository;
import com.hims.iam.domain.repository.UserRoleRepository;
import com.hims.iam.domain.repository.UserRoleMasterRepository;
import com.hims.iam.domain.model.UserRole;
import com.hims.iam.domain.model.UserRoleMaster;
import com.hims.iam.security.JwtTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/**
 * OTP login (M35): generate 6-digit OTP (BCrypt hashed, 5-min TTL, 3 attempts)
 * and verify -> issues JWT. ponytail: SecureRandom + BCrypt, no SMS gateway yet
 * (OTP returned in dev response, feature-flagged in prod).
 */
@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    public static final int OTP_TTL_SECONDS = 300;
    public static final int MAX_ATTEMPTS = 3;

    private final OtpChallengeRepository otpRepository;
    private final UserDetailRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserRoleMasterRepository roleMasterRepository;
    private final JwtTokenService jwtTokenService;
    private final PasswordEncoder passwordEncoder;
    private final boolean otpReturnInResponse; // true in dev, false in prod

    public OtpService(OtpChallengeRepository otpRepository,
                      UserDetailRepository userRepository,
                      UserRoleRepository userRoleRepository,
                      UserRoleMasterRepository roleMasterRepository,
                      JwtTokenService jwtTokenService,
                      PasswordEncoder passwordEncoder,
                      @Value("${app.otp.return-in-response:true}") boolean otpReturnInResponse) {
        this.otpRepository = otpRepository;
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleMasterRepository = roleMasterRepository;
        this.jwtTokenService = jwtTokenService;
        this.passwordEncoder = passwordEncoder;
        this.otpReturnInResponse = otpReturnInResponse;
    }

    /** Generate OTP for identifier (username/phone/email). Returns OTP in dev mode for testing. */
    @Transactional
    public OtpGenerateResult generate(String identifier, String channel) {
        if (identifier == null || identifier.isBlank()) {
            throw new InvalidRequestException("IAM_REQ", "identifier is required");
        }
        String idNorm = identifier.trim().toLowerCase();
        // rate-limit: reuse existing unexpired OTP is not reused — always new (ponytail: simplest)
        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));
        String hash = passwordEncoder.encode(otp);

        OtpChallenge c = new OtpChallenge();
        c.setIdentifier(idNorm);
        c.setOtpHash(hash);
        c.setChannel(channel == null ? "SMS" : channel.trim().toUpperCase());
        c.setAttempts(0);
        c.setMaxAttempts(MAX_ATTEMPTS);
        c.setVerified(false);
        c.setExpiresAt(Timestamp.from(Instant.now().plusSeconds(OTP_TTL_SECONDS)));
        c.setCreatedAt(Timestamp.from(Instant.now()));
        otpRepository.save(c);

        log.info("OTP generated for identifier={} channel={} id={} (devReturn={})", idNorm, c.getChannel(), c.getId(), otpReturnInResponse);
        // In prod, OTP would be sent via SMS/Email gateway (interop-service); here we return for dev/test
        String devOtp = otpReturnInResponse ? otp : null;
        return new OtpGenerateResult(c.getId(), idNorm, c.getChannel(), c.getExpiresAt(), devOtp);
    }

    /** Verify OTP and issue JWT on success. */
    @Transactional
    public LoginResponse verify(String identifier, String otp) {
        if (identifier == null || identifier.isBlank()) throw new InvalidRequestException("IAM_REQ", "identifier is required");
        if (otp == null || otp.isBlank()) throw new InvalidRequestException("IAM_REQ", "otp is required");
        String idNorm = identifier.trim().toLowerCase();

        OtpChallenge c = otpRepository.findTopByIdentifierAndVerifiedFalseOrderByCreatedAtDesc(idNorm)
                .orElseThrow(() -> new InvalidCredentialsException("IAM_OTP", "No OTP found for identifier"));

        if (Instant.now().isAfter(c.getExpiresAt().toInstant())) {
            throw new InvalidCredentialsException("IAM_OTP_EXPIRED", "OTP expired");
        }
        if (c.getAttempts() >= c.getMaxAttempts()) {
            throw new InvalidCredentialsException("IAM_OTP_LOCKED", "OTP attempts exceeded");
        }
        if (!passwordEncoder.matches(otp.trim(), c.getOtpHash())) {
            c.setAttempts(c.getAttempts() + 1);
            otpRepository.save(c);
            throw new InvalidCredentialsException("IAM_OTP", "Invalid OTP");
        }
        c.setVerified(true);
        otpRepository.save(c);

        UserDetail user = userRepository.findByUsernameIgnoreCase(idNorm)
                .orElseThrow(() -> new InvalidCredentialsException("IAM002", "Account not found for identifier"));
        if (!Boolean.TRUE.equals(user.getStatus())) {
            throw new InvalidCredentialsException("IAM002", "Account is disabled");
        }
        List<String> roles = rolesOf(user.getId());
        String token = jwtTokenService.issue(user.getUsername(), user.getId(), roles, user.getUnitId());
        log.info("OTP verify success for userId={} identifier={}", user.getId(), idNorm);
        return new LoginResponse(token, "Bearer", jwtTokenService.ttlSeconds(), user.getId(), user.getUsername(), roles);
    }

    private List<String> rolesOf(Long userId) {
        List<UserRole> grants = userRoleRepository.findByUserIdAndStatusTrue(userId);
        return grants.stream()
                .map(g -> roleMasterRepository.findById(g.getRoleId()).map(UserRoleMaster::getCode).orElse(null))
                .filter(code -> code != null)
                .toList();
    }

    public record OtpGenerateResult(Long id, String identifier, String channel, Timestamp expiresAt, String devOtp) {}
}
