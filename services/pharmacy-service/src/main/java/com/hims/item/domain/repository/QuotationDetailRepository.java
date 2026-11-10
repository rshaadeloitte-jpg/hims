package com.hims.item.domain.repository;

import com.hims.item.domain.model.QuotationDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface QuotationDetailRepository extends JpaRepository<QuotationDetail, Long> {
    List<QuotationDetail> findByQuotationId(Long quotationId);
    @Query(value = "SELECT nextval('ihms_item.t_quotation_detail_seq')", nativeQuery = true)
    long nextId();
}
