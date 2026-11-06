package com.hims.transport.domain.repository;

import com.hims.transport.domain.model.AmbulanceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AmbulanceTypeRepository extends JpaRepository<AmbulanceType, Long> {

    List<AmbulanceType> findByStatusTrueOrderByIdAsc();
}
