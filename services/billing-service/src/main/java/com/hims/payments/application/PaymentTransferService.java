package com.hims.payments.application;

import com.hims.payments.domain.model.PaymentTransfer;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

public interface PaymentTransferService {
    List<String> schemes();
    Page<PaymentTransfer> list(String status, LocalDateTime fromDate, LocalDateTime toDate, int page, int size);
    PaymentTransfer get(Long id);
    PaymentTransfer initiate(String beneficiaryName, String beneficiaryAccountNo, String beneficiaryIfsc,
                             java.math.BigDecimal amount, String scheme, String purpose, String remarks);
    PaymentTransfer verifyOtp(Long id, String otp);
    PaymentTransfer resendOtp(Long id);
    PaymentTransfer cancel(Long id, String reason);
}