package com.hims.bcma.domain.repository;

import com.hims.bcma.domain.model.IVPEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IVPEventRepository extends JpaRepository<IVPEvent, Long> {
    Optional<IVPEvent> findByIvpId(Long ivpId);
    List<IVPEvent> findByPatientId(Long patientId);
    List<IVPEvent> findByNurseId(Long nurseId);
    List<IVPEvent> findByWardId(Long wardId);
}
