package com.hims.abha.domain.repository;

import com.hims.abha.domain.model.EhrFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EhrFileRepository extends JpaRepository<EhrFile, Long> {
}