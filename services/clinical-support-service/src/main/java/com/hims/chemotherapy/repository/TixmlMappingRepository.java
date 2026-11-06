package com.hims.chemotherapy.repository;
import com.hims.chemotherapy.domain.model.TixmlMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.*;
@Repository
public interface TixmlMappingRepository extends JpaRepository<TixmlMapping, Long> {
    Optional<TixmlMapping> findByTemplateName(String templateName);
}
