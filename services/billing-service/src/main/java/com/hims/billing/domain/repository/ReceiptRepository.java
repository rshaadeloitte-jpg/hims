package com.hims.billing.domain.repository;

import com.hims.billing.domain.model.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReceiptRepository extends JpaRepository<Receipt, Long> {

    Optional<Receipt> findByReceiptNo(String receiptNo);

    List<Receipt> findByBillIdOrderByReceiptIdAsc(Long billId);
}
