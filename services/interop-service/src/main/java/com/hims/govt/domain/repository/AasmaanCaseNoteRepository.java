package com.hims.govt.domain.repository;

import com.hims.govt.domain.model.AasmaanCaseNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AasmaanCaseNoteRepository extends JpaRepository<AasmaanCaseNote, Long> {

    List<AasmaanCaseNote> findByCaseIdOrderByNoteIdAsc(Long caseId);
}
