package com.hims.payments.domain.repository;

import com.hims.payments.domain.model.PaymentTransfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface PaymentTransferRepository extends JpaRepository<PaymentTransfer, Long> {

    @Query("SELECT t FROM PaymentTransfer t WHERE " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:fromDate IS NULL OR t.createdAt >= :fromDate) AND " +
           "(:toDate IS NULL OR t.createdAt <= :toDate)")
    Page<PaymentTransfer> search(@Param("status") String status,
                                @Param("fromDate") LocalDateTime fromDate,
                                @Param("toDate") LocalDateTime toDate,
                                Pageable pageable);
}