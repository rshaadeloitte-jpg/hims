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
import com.hims.iam.application.exceptions.InvalidRequestException;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * AuthN core (Catalog #22): login verifies the BCrypt password hash and issues an
 * HS256 JWT; logout adds the token (SHA-256 hashed) to the revoke list; validate
 * introspects signature, expiry and revocation.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserDetailRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserRoleMasterRepository roleMasterRepository;
    private final JwtTokenService jwtTokenService;
    private final TokenRevocationService revocationService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserDetailRepository userRepository,
                       UserRoleRepository userRoleRepository,
                       UserRoleMasterRepository roleMasterRepository,
                       JwtTokenService jwtTokenService,
                       TokenRevocationService revocationService,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleMasterRepository = roleMasterRepository;
        this.jwtTokenService = jwtTokenService;
        this.revocationService = revocationService;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse login(LoginRequest request) {
        UserDetail user = userRepository.findByUsernameIgnoreCase(request.username().trim())
                .orElseThrow(() -> new InvalidCredentialsException("IAM001", "Invalid username or password"));
        if (!Boolean.TRUE.equals(user.getStatus())) {
            throw new InvalidCredentialsException("IAM002", "Account is disabled");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("IAM001", "Invalid username or password");
        }
        List<String> roles = rolesOf(user.getId());
        String token = jwtTokenService.issue(user.getUsername(), user.getId(), roles, user.getUnitId());
        log.info("Login success for userId={} corr-logged", user.getId());
        return new LoginResponse(token, "Bearer", jwtTokenService.ttlSeconds(),
                user.getId(), user.getUsername(), roles);
    }

    public void logout(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidCredentialsException("IAM004", "Bearer token required for logout");
        }
        Claims claims = jwtTokenService.parse(token);
        Instant until = claims != null && claims.getExpiration() != null
                ? claims.getExpiration().toInstant()
                : Instant.now().plusSeconds(3600);
        revocationService.revoke(token, until);
    }

    public ValidateResponse validate(String token) {
        if (token == null || token.isBlank()) {
            return new ValidateResponse(false, null, null, null, null, "MISSING_TOKEN");
        }
        Claims claims = jwtTokenService.parse(token);
        if (claims == null) {
            return new ValidateResponse(false, null, null, null, null, "INVALID_OR_EXPIRED");
        }
        if (revocationService.isRevoked(token)) {
            return new ValidateResponse(false, claims.getSubject(), claims.get(jwtUid(), Long.class),
                    jwtTokenService.roles(claims), jwtTokenService.expiryOf(claims), "REVOKED");
        }
        return new ValidateResponse(true, claims.getSubject(), claims.get(jwtUid(), Long.class),
                jwtTokenService.roles(claims), jwtTokenService.expiryOf(claims), null);
    }

    @Transactional
    public void changePassword(String token, String currentPassword, String newPassword) {
        if (token == null || token.isBlank()) {
            throw new InvalidCredentialsException("IAM001", "Invalid username or password");
        }
        Claims claims = jwtTokenService.parse(token);
        if (claims == null) {
            throw new InvalidCredentialsException("IAM001", "Invalid or expired token");
        }
        String username = claims.getSubject();
        UserDetail user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new InvalidCredentialsException("IAM001", "Invalid username or password"));
        if (!Boolean.TRUE.equals(user.getStatus())) {
            throw new InvalidCredentialsException("IAM002", "Account is disabled");
        }
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException("IAM001", "Invalid username or password");
        }
        if (newPassword.equals(currentPassword) || passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new InvalidRequestException("IAM008", "New password must differ from the current password");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedBy(user.getId());
        user.setUpdatedDatetime(java.sql.Timestamp.from(java.time.Instant.now()));
        userRepository.save(user);
        log.info("Password changed for userId={}", user.getId());

        Instant until = claims.getExpiration() != null
                ? claims.getExpiration().toInstant()
                : Instant.now().plusSeconds(3600);
        revocationService.revoke(token, until);
    }

    private String jwtUid() {
        return JwtTokenService.CLAIM_UID;
    }

    private List<String> rolesOf(Long userId) {
        List<UserRole> grants = userRoleRepository.findByUserIdAndStatusTrue(userId);
        return grants.stream()
                .map(g -> roleMasterRepository.findById(g.getRoleId()).map(UserRoleMaster::getCode).orElse(null))
                .filter(code -> code != null)
                .toList();
    }
}
