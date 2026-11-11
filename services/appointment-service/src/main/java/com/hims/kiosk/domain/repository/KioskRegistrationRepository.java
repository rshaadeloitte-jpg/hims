package com.hims.kiosk.domain.repository;

import com.hims.kiosk.domain.model.KioskRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface KioskRegistrationRepository extends JpaRepository<KioskRegistration, Long> {
    Optional<KioskRegistration> findByRegistrationToken(String token);
}
