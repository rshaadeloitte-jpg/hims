package com.hims.nursing.domain.repository;

import com.hims.nursing.domain.model.NurseNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NurseNoteRepository extends JpaRepository<NurseNote, Long> {

    List<NurseNote> findByAdmissionIdAndStatusTrueOrderByNotedAtDescIdDesc(Long admissionId);
}
