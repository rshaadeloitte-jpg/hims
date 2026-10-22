package com.hims.payments.api;

import com.hims.payments.application.PaymentTransferService;
import com.hims.payments.domain.model.PaymentTransfer;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payment-transfer")
public class PaymentTransferController {

    private final PaymentTransferService service;

    public PaymentTransferController(PaymentTransferService service) {
        this.service = service;
    }

    public record TransferRequest(String beneficiaryName, String beneficiaryAccountNo, String beneficiaryIfsc,
                                  BigDecimal amount, String scheme, String purpose, String remarks) {}

    public record VerifyRequest(String otp) {}
    public record CancelRequest(String reason) {}
    public record ResendResponse(boolean success) {}
    public record PaginatedTransfers(List<PaymentTransfer> content, long totalElements, int totalPages,
                                    int page, int size) {}

    @GetMapping("/schemes")
    public List<String> schemes() {
        return service.schemes();
    }

    @GetMapping
    public PaginatedTransfers list(@RequestParam(required = false) String fromDate,
                                 @RequestParam(required = false) String toDate,
                                 @RequestParam(required = false) String status,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "20") int size) {
        LocalDateTime from = (fromDate == null || fromDate.isBlank()) ? null : LocalDate.parse(fromDate).atStartOfDay();
        LocalDateTime to = (toDate == null || toDate.isBlank()) ? null : LocalDate.parse(toDate).atTime(23, 59, 59);
        Page<PaymentTransfer> p = service.list(status, from, to, page, size);
        return new PaginatedTransfers(p.getContent(), p.getTotalElements(), p.getTotalPages(), p.getNumber(), p.getSize());
    }

    @GetMapping("/{transferId}")
    public PaymentTransfer get(@PathVariable String transferId) {
        return service.get(Long.parseLong(transferId));
    }

    @PostMapping
    public ResponseEntity<PaymentTransfer> initiate(@RequestBody TransferRequest body) {
        PaymentTransfer t = service.initiate(body.beneficiaryName(), body.beneficiaryAccountNo(), body.beneficiaryIfsc(),
                body.amount(), body.scheme(), body.purpose(), body.remarks());
        return ResponseEntity.status(HttpStatus.CREATED).body(t);
    }

    @PostMapping("/{transferId}/verify")
    public PaymentTransfer verify(@PathVariable String transferId, @RequestBody VerifyRequest body) {
        return service.verifyOtp(Long.parseLong(transferId), body.otp());
    }

    @PostMapping("/{transferId}/resend-otp")
    public ResendResponse resendOtp(@PathVariable String transferId) {
        service.resendOtp(Long.parseLong(transferId));
        return new ResendResponse(true);
    }

    @PostMapping("/{transferId}/cancel")
    public PaymentTransfer cancel(@PathVariable String transferId, @RequestBody CancelRequest body) {
        return service.cancel(Long.parseLong(transferId), body.reason());
    }
}