package com.hims.interop.domain.repository;

import com.hims.interop.domain.model.AbhaLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface AbhaLinkRepository extends JpaRepository<AbhaLink, Long> {
    List<AbhaLink> findByPatientId(Long patientId);
    List<AbhaLink> findByLinkStatus(Short linkStatus);
    Optional<AbhaLink> findFirstByAbhaAddress(String abhaAddress);
    @Query(value = "SELECT nextval('ihms_interop.t_abha_link')", nativeQuery = true)
    long nextId();
}
