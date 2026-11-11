package com.hims.payments.application;

import com.hims.payments.domain.model.PaymentTransfer;
import com.hims.payments.domain.repository.PaymentTransferRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PaymentTransferServiceImpl implements PaymentTransferService {

    private static final List<String> SCHEMES = List.of("PMJAY", "JSY", "PMKVY", "NSAP", "PMAY", "OTHER");
    private static final int OTP_TTL_MINUTES = 5;

    private final PaymentTransferRepository repository;

    public PaymentTransferServiceImpl(PaymentTransferRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> schemes() {
        return SCHEMES;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentTransfer> list(String status, LocalDateTime fromDate, LocalDateTime toDate, int page, int size) {
        return repository.search(status, fromDate, toDate, PageRequest.of(page, Math.max(1, size)));
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentTransfer get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found: " + id));
    }

    @Override
    public PaymentTransfer initiate(String beneficiaryName, String beneficiaryAccountNo, String beneficiaryIfsc,
                                   BigDecimal amount, String scheme, String purpose, String remarks) {
        PaymentTransfer t = new PaymentTransfer();
        t.setBeneficiaryName(beneficiaryName);
        t.setBeneficiaryAccountNo(beneficiaryAccountNo);
        t.setBeneficiaryIfsc(beneficiaryIfsc);
        t.setAmount(amount);
        t.setScheme(scheme);
        t.setPurpose(purpose);
        t.setRemarks(remarks);
        t.setStatus("PENDING");
        t.setOtp(generateOtp());
        t.setOtpExpiresAt(LocalDateTime.now().plusMinutes(OTP_TTL_MINUTES));
        t.setCreatedAt(LocalDateTime.now());
        t.setUpdatedAt(LocalDateTime.now());
        return repository.save(t);
    }

    @Override
    public PaymentTransfer verifyOtp(Long id, String otp) {
        PaymentTransfer t = get(id);
        if (!"PENDING".equals(t.getStatus()) && !"PROCESSING".equals(t.getStatus())) {
            throw new IllegalStateException("Transfer not in verifiable state: " + t.getStatus());
        }
        if (t.getOtpExpiresAt() == null || LocalDateTime.now().isAfter(t.getOtpExpiresAt())) {
            throw new IllegalArgumentException("OTP expired");
        }
        if (t.getOtp() == null || !t.getOtp().equals(otp)) {
            throw new IllegalArgumentException("Invalid OTP");
        }
        t.setStatus("COMPLETED");
        t.setUtrNo("UTR" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        t.setCompletedAt(LocalDateTime.now());
        t.setUpdatedAt(LocalDateTime.now());
        t.setOtp(null);
        t.setOtpExpiresAt(null);
        return repository.save(t);
    }

    @Override
    public PaymentTransfer resendOtp(Long id) {
        PaymentTransfer t = get(id);
        if (!"PENDING".equals(t.getStatus())) {
            throw new IllegalStateException("Cannot resend OTP for status: " + t.getStatus());
        }
        t.setOtp(generateOtp());
        t.setOtpExpiresAt(LocalDateTime.now().plusMinutes(OTP_TTL_MINUTES));
        t.setUpdatedAt(LocalDateTime.now());
        return repository.save(t);
    }

    @Override
    public PaymentTransfer cancel(Long id, String reason) {
        PaymentTransfer t = get(id);
        if ("COMPLETED".equals(t.getStatus()) || "CANCELLED".equals(t.getStatus())) {
            throw new IllegalStateException("Cannot cancel transfer in status: " + t.getStatus());
        }
        t.setStatus("CANCELLED");
        t.setFailureReason(reason);
        t.setUpdatedAt(LocalDateTime.now());
        return repository.save(t);
    }

    private String generateOtp() {
        return String.valueOf((int) (Math.random() * 900000) + 100000);
    }
}