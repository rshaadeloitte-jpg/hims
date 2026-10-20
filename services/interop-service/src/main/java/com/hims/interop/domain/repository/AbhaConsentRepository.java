package com.hims.interop.domain.repository;

import com.hims.interop.domain.model.AbhaConsent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface AbhaConsentRepository extends JpaRepository<AbhaConsent, Long> {
    List<AbhaConsent> findByAbhaLinkId(Long abhaLinkId);
    @Query(value = "SELECT nextval('ihms_interop.t_abha_consent')", nativeQuery = true)
    long nextId();
}
