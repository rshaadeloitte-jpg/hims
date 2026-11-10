package com.hims.item.domain.repository;
import com.hims.item.domain.model.Quotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface QuotationRepository extends JpaRepository<Quotation, Long> {
    List<Quotation> findByDocStatusOrderByAddedDatetimeDesc(Short docStatus);
    List<Quotation> findBySupplierIdOrderByAddedDatetimeDesc(Long supplierId);
    @Query("SELECT q FROM Quotation q WHERE q.id IN "
         + "(SELECT d.quotationId FROM QuotationDetail d WHERE d.itemId = :itemId)")
    List<Quotation> findQuotationsByItemId(@Param("itemId") Long itemId);
    @Query(value = "SELECT nextval('ihms_item.t_quotation_seq')", nativeQuery = true)
    long nextId();
}
