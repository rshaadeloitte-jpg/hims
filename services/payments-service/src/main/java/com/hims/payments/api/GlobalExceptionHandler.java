package com.hims.payments.api;

import com.hims.payments.application.exceptions.BillingCallException;
import com.hims.payments.application.exceptions.InvalidPaymentRequestException;
import com.hims.payments.application.exceptions.PaymentEntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Invalid payload / rule violation -> 400 with the rule code (incl. legacy BEXAR002). */
    @ExceptionHandler(InvalidPaymentRequestException.class)
    public ResponseEntity<ApiError> invalidRequest(InvalidPaymentRequestException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of("ERROR", e.getErrorCode(), e.getMessage()));
    }

    @ExceptionHandler(PaymentEntityNotFoundException.class)
    public ResponseEntity<ApiError> notFound(PaymentEntityNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of("ERROR", "NOT_FOUND", e.getMessage()));
    }

    /**
     * Synchronous billing settlement failed -> 502: nothing was recorded and no
     * E-08 exists (documented saga deviation — request-scoped settlement step).
     */
    @ExceptionHandler(BillingCallException.class)
    public ResponseEntity<ApiError> billingUnavailable(BillingCallException e) {
        log.error("Billing call failed", e);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiError.of("ERROR", "BILLING_UNREACHABLE", e.getMessage()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> missingParam(MissingServletRequestParameterException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of("ERROR", "PAY_REQ_INVALID",
                        "Missing required parameter: " + e.getParameterName()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> badParam(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of("ERROR", "PAY_REQ_INVALID", "Invalid parameter: " + e.getName()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> noResource(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of("ERROR", "NOT_FOUND", "Resource not found"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of("ERROR", "INTERNAL_ERROR", "Internal server error"));
    }
}
