package com.hims.govt.domain.repository;

import com.hims.govt.domain.model.Geography;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GeographyRepository extends JpaRepository<Geography, Long> {
    List<Geography> findByAshaId(Long ashaId);
}