package com.hims.reference.domain.repository;

import com.hims.reference.domain.model.AppConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface AppConfigRepository extends JpaRepository<AppConfig, Long> {
    Optional<AppConfig> findByUnitIdAndConfigKey(Long unitId, String configKey);
    List<AppConfig> findByUnitId(Long unitId);
    @Query(value = "SELECT nextval('ihms_master.app_config_seq')", nativeQuery = true)
    long nextId();
}
