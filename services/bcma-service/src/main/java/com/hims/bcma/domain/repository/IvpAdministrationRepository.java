package com.hims.bcma.domain.repository;

import com.hims.bcma.domain.model.IvpAdministration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IvpAdministrationRepository extends JpaRepository<IvpAdministration, Long> {
    Optional<IvpAdministration> findByIvpId(Long ivpId);
    List<IvpAdministration> findByPatientId(Long patientId);
    List<IvpAdministration> findByAdministrationStatus(String administrationStatus);
}
