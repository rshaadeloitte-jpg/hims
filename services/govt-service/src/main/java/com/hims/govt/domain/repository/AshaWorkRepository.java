package com.hims.govt.domain.repository;

import com.hims.govt.domain.model.AshaWork;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AshaWorkRepository extends JpaRepository<AshaWork, Long> {
    List<AshaWork> findByTrackingType(String trackingType);
    List<AshaWork> findByAshaId(Long ashaId);
}