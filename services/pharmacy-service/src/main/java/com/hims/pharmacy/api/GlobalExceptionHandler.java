package com.hims.pharmacy.api;

import com.hims.pharmacy.application.exceptions.IllegalSaleStateException;
import com.hims.pharmacy.application.exceptions.InvalidSaleRequestException;
import com.hims.pharmacy.application.exceptions.ReturnNotFoundException;
import com.hims.pharmacy.application.exceptions.SaleNotFoundException;
import com.hims.pharmacy.application.exceptions.StockIssueException;
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

    /** Illegal lifecycle transition / invalid payload -> 400 with the rule code. */
    @ExceptionHandler({InvalidSaleRequestException.class, IllegalSaleStateException.class})
    public ResponseEntity<ApiError> invalidRequest(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of("ERROR", ((com.hims.pharmacy.application.exceptions.SaleRuleException) e).getErrorCode(), e.getMessage()));
    }

    @ExceptionHandler(SaleNotFoundException.class)
    public ResponseEntity<ApiError> notFound(SaleNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of("ERROR", e.getErrorCode(), e.getMessage()));
    }

    @ExceptionHandler(ReturnNotFoundException.class)
    public ResponseEntity<ApiError> returnNotFound(ReturnNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of("ERROR", "NOT_FOUND", e.getMessage()));
    }

    /**
     * The synchronous item-service stock issue failed — the whole dispense
     * transaction rolls back (sale stays PENDING, nothing marked dispensed,
     * no outbox row) and the caller gets a 502 envelope.
     */
    @ExceptionHandler(StockIssueException.class)
    public ResponseEntity<ApiError> stockUnavailable(StockIssueException e) {
        log.error("Item-service stock call failed", e);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiError.of("ERROR", "STOCK_SERVICE_UNAVAILABLE", e.getMessage()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> missingParam(MissingServletRequestParameterException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of("ERROR", "BEXA003",
                        "Missing required parameter: " + e.getParameterName()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> badParam(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of("ERROR", "BEXA003", "Invalid parameter: " + e.getName()));
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
