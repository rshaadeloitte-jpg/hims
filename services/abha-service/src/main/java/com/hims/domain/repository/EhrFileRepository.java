package com.hims.domain.repository;

import com.hims.domain.model.EhrFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EhrFileRepository extends JpaRepository<EhrFile, Long> {
}