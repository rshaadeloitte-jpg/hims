package com.hims.ot.domain.repository;

import com.hims.ot.domain.model.ChecklistCompletion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChecklistCompletionRepository extends JpaRepository<ChecklistCompletion, Long> {
    Optional<ChecklistCompletion> findByChecklistId(Long checklistId);
}
