package com.hims.iam.domain.repository;

import com.hims.iam.domain.model.OtpChallenge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, Long> {
    Optional<OtpChallenge> findTopByIdentifierAndVerifiedFalseOrderByCreatedAtDesc(String identifier);
}
