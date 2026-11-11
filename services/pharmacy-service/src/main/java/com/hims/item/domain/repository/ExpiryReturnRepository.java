package com.hims.item.domain.repository;

import com.hims.item.domain.model.ExpiryReturn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ExpiryReturnRepository extends JpaRepository<ExpiryReturn, Long> {
    List<ExpiryReturn> findByStoreIdOrderByAddedDatetimeDesc(Long storeId);
    List<ExpiryReturn> findByDocStatusOrderByAddedDatetimeDesc(Short docStatus);

    /** Server-owned id (IMP-DEC-003 explicit per-table sequence). */
    @Query(value = "SELECT nextval('ihms_item.\"t_expiry_return_seq\"')", nativeQuery = true)
    long nextReturnId();
}
