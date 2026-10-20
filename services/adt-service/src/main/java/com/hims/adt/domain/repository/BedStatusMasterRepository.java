package com.hims.adt.domain.repository;

import com.hims.adt.domain.model.BedStatusMaster;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BedStatusMasterRepository extends JpaRepository<BedStatusMaster, Short> {
}
