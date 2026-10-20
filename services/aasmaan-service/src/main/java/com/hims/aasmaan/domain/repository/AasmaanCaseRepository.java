package com.hims.aasmaan.domain.repository;

import com.hims.aasmaan.domain.model.AasmaanCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AasmaanCaseRepository extends JpaRepository<AasmaanCase, Long> {}
