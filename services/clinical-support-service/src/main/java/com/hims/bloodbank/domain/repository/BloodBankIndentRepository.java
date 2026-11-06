package com.hims.bloodbank.domain.repository;

import com.hims.bloodbank.domain.model.BloodBankIndent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;


public interface BloodBankIndentRepository extends JpaRepository<BloodBankIndent, Long> {
    List<BloodBankIndent> findByDocStatusOrderByAddedDatetimeDesc(Short docStatus);
    List<BloodBankIndent> findByToStoreIdOrderByAddedDatetimeDesc(Long toStoreId);
    List<BloodBankIndent> findByFromStoreIdOrderByAddedDatetimeDesc(Long fromStoreId);

    @Query(value = "SELECT nextval('ihms_bloodbank.t_bloodbank_indent')", nativeQuery = true)
    long nextId();
}
