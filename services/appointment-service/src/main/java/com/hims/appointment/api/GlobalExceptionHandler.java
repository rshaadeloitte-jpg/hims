package com.hims.appointment.api;

import com.hims.appointment.application.exceptions.AppointmentNotFoundException;
import com.hims.appointment.application.exceptions.EncounterNotFoundException;
import com.hims.appointment.application.exceptions.InvalidAppointmentRequestException;
import com.hims.appointment.application.exceptions.SlotAlreadyBookedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Legacy-compatible error envelope {status, errorCode, message} (same shape as
 * grievance/patient/queue services). Validation/parameter problems carry the
 * legacy BEXA003 code; identity collisions across slices reuse BERA011-style
 * codes where applicable; unknown ids map to NOT_FOUND.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InvalidAppointmentRequestException.class)
    public ResponseEntity<ApiError> invalidRequest(InvalidAppointmentRequestException e) {
        return respond(HttpStatus.BAD_REQUEST, e.getErrorCode(), e.getMessage());
    }

    /** Slot lost between search and booking, or concurrent booking won the CAS race (BR-OPD-002). */
    @ExceptionHandler(SlotAlreadyBookedException.class)
    public ResponseEntity<ApiError> slotConflict(SlotAlreadyBookedException e) {
        return respond(HttpStatus.CONFLICT, e.getErrorCode(), e.getMessage());
    }

    @ExceptionHandler({AppointmentNotFoundException.class, EncounterNotFoundException.class})
    public ResponseEntity<ApiError> notFound(Exception e) {
        String code = e instanceof AppointmentNotFoundException a ? a.getErrorCode()
                : ((EncounterNotFoundException) e).getErrorCode();
        return respond(HttpStatus.NOT_FOUND, code, e.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> missingParam(MissingServletRequestParameterException e) {
        return badRequest("Missing required parameter: " + e.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> badParam(MethodArgumentTypeMismatchException e) {
        return badRequest("Invalid parameter: " + e.getName());
    }

    /** Malformed JSON body etc. */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> unreadableBody(org.springframework.http.converter.HttpMessageNotReadableException e) {
        return badRequest("Malformed request body");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> noResource(NoResourceFoundException e) {
        return respond(HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found");
    }

    /** DB-level races (uq_appointment_token / uq_doctor_slot) surface as the slot-conflict envelope. */
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> integrityViolation(org.springframework.dao.DataIntegrityViolationException e) {
        log.warn("Data integrity violation treated as conflict: {}", e.getMostSpecificCause().getMessage());
        return respond(HttpStatus.CONFLICT, "APPT_SLOT_TAKEN", "Slot already booked");
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> illegalState(IllegalStateException e) {
        log.error("Illegal state", e);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Internal server error");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception e) {
        log.error("Unhandled exception", e);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Internal server error");
    }

    private static ResponseEntity<ApiError> badRequest(String message) {
        return respond(HttpStatus.BAD_REQUEST, "BEXA003", message);
    }

    private static ResponseEntity<ApiError> respond(HttpStatus httpStatus, String errorCode, String message) {
        return ResponseEntity.status(httpStatus).body(ApiError.of("ERROR", errorCode, message));
    }
}
