package com.hims.bcma.domain.repository;

import com.hims.bcma.domain.model.IvpDrugConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IvpDrugConfigRepository extends JpaRepository<IvpDrugConfig, Long> {
    Optional<IvpDrugConfig> findByDrugNdc(String drugNdc);
    Optional<IvpDrugConfig> findByConfigId(Long configId);
}
