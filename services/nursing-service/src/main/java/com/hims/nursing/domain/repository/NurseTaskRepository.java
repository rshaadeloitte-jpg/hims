package com.hims.nursing.domain.repository;

import com.hims.nursing.domain.model.NurseTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NurseTaskRepository extends JpaRepository<NurseTask, Long> {

    List<NurseTask> findByTaskStatus(Short taskStatus);

    List<NurseTask> findByAdmissionIdOrderByIdDesc(Long admissionId);

    List<NurseTask> findByAdmissionIdAndTaskStatusOrderByDueAtAscIdAsc(Long admissionId, Short taskStatus);
}
