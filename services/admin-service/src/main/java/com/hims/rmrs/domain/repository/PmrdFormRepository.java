package com.hims.rmrs.domain.repository;

import com.hims.rmrs.domain.model.PmrdForm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PmrdFormRepository extends JpaRepository<PmrdForm, Long> {
}
