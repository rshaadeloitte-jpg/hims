package com.hims.item.domain.repository;

import com.hims.item.domain.model.ExpiryReturnDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ExpiryReturnDetailRepository extends JpaRepository<ExpiryReturnDetail, Long> {
    List<ExpiryReturnDetail> findByExpiryReturnId(Long expiryReturnId);

    /** Server-owned id (IMP-DEC-003 explicit per-table sequence). */
    @Query(value = "SELECT nextval('ihms_item.\"t_expiry_return_detail_seq\"')", nativeQuery = true)
    long nextDetailId();
}
