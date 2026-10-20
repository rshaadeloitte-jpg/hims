package com.hims.lab.application;

import com.hims.lab.application.exceptions.InvalidLabRequestException;
import com.hims.lab.application.exceptions.LabEntityNotFoundException;
import com.hims.lab.domain.model.CriticalAlertDelivery;
import com.hims.lab.domain.model.PathOrderBooking;
import com.hims.lab.domain.model.PathOrderDetail;
import com.hims.lab.domain.model.PathoPatientParameterDetails;
import com.hims.lab.domain.repository.CriticalAlertDeliveryRepository;
import com.hims.lab.domain.repository.PathOrderBookingRepository;
import com.hims.lab.domain.repository.PathOrderDetailRepository;
import com.hims.lab.domain.repository.PathoPatientParameterDetailsRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/**
 * Critical alert delivery service (GAP-REGISTER WP-7 HIGH).
 * Enhanced fan-out: creates delivery records per channel (WHATSAPP/SMS/EMAIL/CALL)
 * and tracks delivery status. Ported from legacy NotifyPatientCriticalDetails.
 */
@Service
public class CriticalAlertDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(CriticalAlertDeliveryService.class);

    public static final long AUDIT_ACTOR_ID = 1L;

    private final CriticalAlertDeliveryRepository deliveryRepository;
    private final PathOrderBookingRepository bookingRepository;
    private final PathOrderDetailRepository detailRepository;
    private final PathoPatientParameterDetailsRepository parameterRepository;

    @PersistenceContext
    private EntityManager em;

    public CriticalAlertDeliveryService(CriticalAlertDeliveryRepository deliveryRepository,
                                        PathOrderBookingRepository bookingRepository,
                                        PathOrderDetailRepository detailRepository,
                                        PathoPatientParameterDetailsRepository parameterRepository) {
        this.deliveryRepository = deliveryRepository;
        this.bookingRepository = bookingRepository;
        this.detailRepository = detailRepository;
        this.parameterRepository = parameterRepository;
    }

    /**
     * Create delivery records for a critical detail across all channels.
     * Returns the list of created delivery records.
     */
    @Transactional
    public List<CriticalAlertDelivery> createDeliveries(Long bookingId, Long detailId,
                                                         String informPassedTo, String informPassedBy,
                                                         Long docId, String ward, String bedNo,
                                                         String emailId, List<String> channels) {
        PathOrderBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new LabEntityNotFoundException("NOT_FOUND",
                        "Booking not found: " + bookingId));
        PathOrderDetail detail = detailRepository.findById(detailId)
                .orElseThrow(() -> new LabEntityNotFoundException("NOT_FOUND",
                        "Detail not found: " + detailId));
        if (!detail.getBookingId().equals(booking.getBookingId())) {
            throw new InvalidLabRequestException("LAB_REQ_INVALID",
                    "Detail " + detailId + " does not belong to booking " + bookingId);
        }
        List<PathoPatientParameterDetails> criticals = parameterRepository
                .findByDetailIdOrderByParamIdAsc(detailId).stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsCritical()))
                .toList();
        if (criticals.isEmpty()) {
            throw new InvalidLabRequestException("LAB_REQ_INVALID",
                    "Detail " + detailId + " has no critical values");
        }
        if (channels == null || channels.isEmpty()) {
            channels = List.of("WHATSAPP");
        }

        PathoPatientParameterDetails firstCritical = criticals.get(0);
        List<CriticalAlertDelivery> created = new java.util.ArrayList<>();
        Timestamp nowTs = now();
        for (String channel : channels) {
            CriticalAlertDelivery d = new CriticalAlertDelivery();
            d.setDeliveryId(nextId("t_critical_alert_delivery_seq"));
            d.setBookingId(bookingId);
            d.setDetailId(detailId);
            d.setPatientId(booking.getPatientId());
            d.setEncounterId(booking.getEncounterId());
            d.setMrnNo(null); // populated from patient service if needed
            d.setPatientName(null);
            d.setTestId(null);
            d.setTestName(detail.getTestName());
            d.setResultValue(firstCritical.getValue());
            d.setReferenceRange(firstCritical.getReferenceRange());
            d.setInformPassedTo(informPassedTo);
            d.setInformPassedBy(informPassedBy);
            d.setDocId(docId);
            d.setWard(ward);
            d.setBedNo(bedNo);
            d.setEmailId(emailId);
            d.setChannel(channel.toUpperCase());
            d.setDeliveryStatus("PENDING");
            d.setRetryCount(0);
            d.setStatus(true);
            d.setSynchronizedd(false);
            d.setAddedBy(AUDIT_ACTOR_ID);
            d.setAddedDatetime(nowTs);
            created.add(deliveryRepository.save(d));
        }
        log.info("Created {} critical alert deliveries for booking {} detail {}",
                created.size(), bookingId, detailId);
        return created;
    }

    /**
     * Mark a delivery as sent.
     */
    @Transactional
    public CriticalAlertDelivery markSent(Long deliveryId) {
        CriticalAlertDelivery d = load(deliveryId);
        if (!"PENDING".equals(d.getDeliveryStatus()) && !"RETRY".equals(d.getDeliveryStatus())) {
            throw new InvalidLabRequestException("LAB_ILLEGAL_TRANSITION",
                    "Cannot mark sent from status " + d.getDeliveryStatus());
        }
        d.setDeliveryStatus("SENT");
        d.setDeliveredAt(now());
        d.setUpdatedBy(AUDIT_ACTOR_ID);
        d.setUpdatedDatetime(now());
        return deliveryRepository.save(d);
    }

    /**
     * Mark a delivery as delivered (confirmed receipt).
     */
    @Transactional
    public CriticalAlertDelivery markDelivered(Long deliveryId) {
        CriticalAlertDelivery d = load(deliveryId);
        if (!"SENT".equals(d.getDeliveryStatus())) {
            throw new InvalidLabRequestException("LAB_ILLEGAL_TRANSITION",
                    "Cannot mark delivered from status " + d.getDeliveryStatus());
        }
        d.setDeliveryStatus("DELIVERED");
        d.setDeliveredAt(now());
        d.setUpdatedBy(AUDIT_ACTOR_ID);
        d.setUpdatedDatetime(now());
        return deliveryRepository.save(d);
    }

    /**
     * Mark a delivery as failed, increment retry count.
     */
    @Transactional
    public CriticalAlertDelivery markFailed(Long deliveryId, String failureReason) {
        CriticalAlertDelivery d = load(deliveryId);
        d.setDeliveryStatus("FAILED");
        d.setFailureReason(failureReason);
        d.setRetryCount(d.getRetryCount() + 1);
        d.setUpdatedBy(AUDIT_ACTOR_ID);
        d.setUpdatedDatetime(now());
        return deliveryRepository.save(d);
    }

    /**
     * Retry a failed delivery: status -> RETRY.
     */
    @Transactional
    public CriticalAlertDelivery retry(Long deliveryId) {
        CriticalAlertDelivery d = load(deliveryId);
        if (!"FAILED".equals(d.getDeliveryStatus())) {
            throw new InvalidLabRequestException("LAB_ILLEGAL_TRANSITION",
                    "Can only retry FAILED deliveries, got " + d.getDeliveryStatus());
        }
        d.setDeliveryStatus("RETRY");
        d.setUpdatedBy(AUDIT_ACTOR_ID);
        d.setUpdatedDatetime(now());
        return deliveryRepository.save(d);
    }

    @Transactional(readOnly = true)
    public CriticalAlertDelivery get(Long id) {
        return load(id);
    }

    @Transactional(readOnly = true)
    public List<CriticalAlertDelivery> listByBooking(Long bookingId) {
        return deliveryRepository.findByBookingIdOrderByDeliveryIdAsc(bookingId);
    }

    @Transactional(readOnly = true)
    public List<CriticalAlertDelivery> listPending() {
        return deliveryRepository.findByDeliveryStatusInOrderByAddedDatetimeAsc(
                List.of("PENDING", "RETRY"));
    }

    @Transactional(readOnly = true)
    public List<CriticalAlertDelivery> listByPatient(Long patientId) {
        return deliveryRepository.findByPatientIdOrderByDeliveryIdAsc(patientId);
    }

    private CriticalAlertDelivery load(Long id) {
        return deliveryRepository.findById(id)
                .orElseThrow(() -> new LabEntityNotFoundException("NOT_FOUND",
                        "Critical alert delivery not found: " + id));
    }

    private Long nextId(String sequenceName) {
        Number n = (Number) em.createNativeQuery("SELECT nextval('ihms_lab." + sequenceName + "')")
                .getSingleResult();
        return n.longValue();
    }

    private static Timestamp now() {
        return Timestamp.from(Instant.now());
    }
}
