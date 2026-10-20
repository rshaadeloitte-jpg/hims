package com.hims.domain.repository;

import com.hims.domain.model.HprRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HprRegistrationRepository extends JpaRepository<HprRegistration, Long> {
}