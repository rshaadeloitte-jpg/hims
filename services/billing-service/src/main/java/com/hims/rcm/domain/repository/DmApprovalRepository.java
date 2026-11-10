package com.hims.rcm.domain.repository;

import com.hims.rcm.domain.model.DmApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface DmApprovalRepository extends JpaRepository<DmApproval, Long> {
    List<DmApproval> findByDocStatusOrderByRequestedAtDesc(Short docStatus);
    List<DmApproval> findByPatientIdOrderByRequestedAtDesc(Long patientId);
    @Query(value = "SELECT nextval('ihms_rcm.t_dm_approval_seq')", nativeQuery = true)
    long nextId();
}
