package com.hims.radiology.domain.repository;

import com.hims.radiology.domain.model.RadTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface RadTemplateRepository extends JpaRepository<RadTemplate, Long> {
    Optional<RadTemplate> findByTemplateCode(String templateCode);
    List<RadTemplate> findByModality(String modality);
    @Query(value = "SELECT nextval('ihms_rad.t_rad_template_seq')", nativeQuery = true)
    long nextId();
}
