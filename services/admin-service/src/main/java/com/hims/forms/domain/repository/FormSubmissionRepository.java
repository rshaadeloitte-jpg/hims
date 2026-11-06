package com.hims.forms.domain.repository;

import com.hims.forms.domain.model.FormSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FormSubmissionRepository extends JpaRepository<FormSubmission, Long> {

    List<FormSubmission> findByFormId(Long formId);

    List<FormSubmission> findByPatientId(Long patientId);

    List<FormSubmission> findByEncounterId(Long encounterId);

    List<FormSubmission> findByFormIdAndPatientId(Long formId, Long patientId);

    List<FormSubmission> findByPatientIdOrderBySubmittedAtDesc(Long patientId);

    Optional<FormSubmission> findByIdAndFormId(Long id, Long formId);
}
