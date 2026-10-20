package com.hims.mortuary.domain.repository;

import com.hims.mortuary.domain.model.DeadBody;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeadBodyRepository extends JpaRepository<DeadBody, Long> {

    Optional<DeadBody> findByAdmissionId(Long admissionId);

    List<DeadBody> findByBodyStatus(String bodyStatus);

    List<DeadBody> findByPatientId(Long patientId);

    @Query(value = "SELECT nextval('ihms_mortuary.t_dead_body_seq')", nativeQuery = true)
    long nextId();
}
