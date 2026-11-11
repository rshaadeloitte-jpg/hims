package com.hims.abha.domain.repository;

import com.hims.abha.domain.model.HprRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HprRegistrationRepository extends JpaRepository<HprRegistration, Long> {
}