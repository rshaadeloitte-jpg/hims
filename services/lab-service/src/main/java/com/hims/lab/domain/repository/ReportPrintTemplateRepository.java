package com.hims.lab.domain.repository;

import com.hims.lab.domain.model.ReportPrintTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReportPrintTemplateRepository extends JpaRepository<ReportPrintTemplate, Long> {

    Optional<ReportPrintTemplate> findByCode(String code);

    List<ReportPrintTemplate> findByTemplateTypeOrderByCodeAsc(String templateType);

    List<ReportPrintTemplate> findByGenderIdOrderByCodeAsc(Long genderId);

    List<ReportPrintTemplate> findByServiceIdOrderByCodeAsc(Long serviceId);

    boolean existsByCode(String code);
}
