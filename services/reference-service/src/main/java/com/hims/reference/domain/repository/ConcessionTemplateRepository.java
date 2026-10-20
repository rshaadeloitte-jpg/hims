package com.hims.reference.domain.repository;

import com.hims.reference.domain.model.ConcessionTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ConcessionTemplateRepository extends JpaRepository<ConcessionTemplate, Long> {
    List<ConcessionTemplate> findByScheme(String scheme);
    List<ConcessionTemplate> findByDocStatus(Short docStatus);
    @Query(value = "SELECT nextval('ihms_master.concession_template_seq')", nativeQuery = true)
    long nextId();
}
