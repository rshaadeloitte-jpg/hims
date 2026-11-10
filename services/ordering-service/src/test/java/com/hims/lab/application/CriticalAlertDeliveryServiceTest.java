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
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CriticalAlertDeliveryServiceTest {

    private CriticalAlertDeliveryRepository deliveryRepository;
    private PathOrderBookingRepository bookingRepository;
    private PathOrderDetailRepository detailRepository;
    private PathoPatientParameterDetailsRepository parameterRepository;
    private EntityManager em;
    private CriticalAlertDeliveryService service;

    @BeforeEach
    void setUp() {
        deliveryRepository = mock(CriticalAlertDeliveryRepository.class);
        bookingRepository = mock(PathOrderBookingRepository.class);
        detailRepository = mock(PathOrderDetailRepository.class);
        parameterRepository = mock(PathoPatientParameterDetailsRepository.class);
        em = mock(EntityManager.class);
        service = new CriticalAlertDeliveryService(deliveryRepository, bookingRepository,
                detailRepository, parameterRepository);
        ReflectionTestUtils.setField(service, "em", em);

        Query seqQuery = mock(Query.class);
        when(seqQuery.getSingleResult()).thenAnswer(inv -> 200L);
        when(em.createNativeQuery(contains("nextval"))).thenReturn(seqQuery);
        when(deliveryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createDeliveries_noCriticalValues_rejected() {
        PathOrderBooking b = new PathOrderBooking();
        b.setBookingId(10L);
        PathOrderDetail d = new PathOrderDetail();
        d.setDetailId(21L);
        d.setBookingId(10L);
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(b));
        when(detailRepository.findById(21L)).thenReturn(Optional.of(d));
        when(parameterRepository.findByDetailIdOrderByParamIdAsc(21L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.createDeliveries(10L, 21L, "Dr. Smith", "Lab Tech", 1L, "ICU", "B1", "a@b.com", List.of("WHATSAPP")))
                .isInstanceOf(InvalidLabRequestException.class)
                .hasMessageContaining("no critical values");
    }

    @Test
    void createDeliveries_valid_createsOnePerChannel() {
        PathOrderBooking b = new PathOrderBooking();
        b.setBookingId(10L);
        PathOrderDetail d = new PathOrderDetail();
        d.setDetailId(21L);
        d.setBookingId(10L);
        d.setTestName("CBC");
        PathoPatientParameterDetails p = new PathoPatientParameterDetails();
        p.setIsCritical(true);
        p.setValue("5.0");
        p.setReferenceRange("12-16");
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(b));
        when(detailRepository.findById(21L)).thenReturn(Optional.of(d));
        when(parameterRepository.findByDetailIdOrderByParamIdAsc(21L)).thenReturn(List.of(p));

        List<CriticalAlertDelivery> deliveries = service.createDeliveries(
                10L, 21L, "Dr. Smith", "Tech", 1L, "ICU", "B1", "a@b.com",
                List.of("WHATSAPP", "SMS", "EMAIL"));

        assertThat(deliveries).hasSize(3);
        assertThat(deliveries).extracting(CriticalAlertDelivery::getChannel)
                .containsExactlyInAnyOrder("WHATSAPP", "SMS", "EMAIL");
        assertThat(deliveries).extracting(CriticalAlertDelivery::getDeliveryStatus)
                .containsOnly("PENDING");
    }

    @Test
    void createDeliveries_defaultChannel_whenEmpty() {
        PathOrderBooking b = new PathOrderBooking();
        b.setBookingId(10L);
        PathOrderDetail d = new PathOrderDetail();
        d.setDetailId(21L);
        d.setBookingId(10L);
        d.setTestName("CBC");
        PathoPatientParameterDetails p = new PathoPatientParameterDetails();
        p.setIsCritical(true);
        p.setValue("5.0");
        p.setReferenceRange("12-16");
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(b));
        when(detailRepository.findById(21L)).thenReturn(Optional.of(d));
        when(parameterRepository.findByDetailIdOrderByParamIdAsc(21L)).thenReturn(List.of(p));

        List<CriticalAlertDelivery> deliveries = service.createDeliveries(
                10L, 21L, "Dr. Smith", "Tech", 1L, "ICU", "B1", "a@b.com", List.of());

        assertThat(deliveries).hasSize(1);
        assertThat(deliveries.get(0).getChannel()).isEqualTo("WHATSAPP");
    }

    @Test
    void markSent_fromPending_allowed() {
        CriticalAlertDelivery d = new CriticalAlertDelivery();
        d.setDeliveryId(5L);
        d.setDeliveryStatus("PENDING");
        when(deliveryRepository.findById(5L)).thenReturn(Optional.of(d));

        CriticalAlertDelivery sent = service.markSent(5L);
        assertThat(sent.getDeliveryStatus()).isEqualTo("SENT");
        assertThat(sent.getDeliveredAt()).isNotNull();
    }

    @Test
    void markSent_fromDelivered_illegal() {
        CriticalAlertDelivery d = new CriticalAlertDelivery();
        d.setDeliveryId(5L);
        d.setDeliveryStatus("DELIVERED");
        when(deliveryRepository.findById(5L)).thenReturn(Optional.of(d));

        assertThatThrownBy(() -> service.markSent(5L))
                .isInstanceOf(InvalidLabRequestException.class)
                .extracting(e -> ((InvalidLabRequestException) e).getErrorCode())
                .isEqualTo("LAB_ILLEGAL_TRANSITION");
    }

    @Test
    void markFailed_incrementsRetryCount() {
        CriticalAlertDelivery d = new CriticalAlertDelivery();
        d.setDeliveryId(5L);
        d.setDeliveryStatus("SENT");
        d.setRetryCount(0);
        when(deliveryRepository.findById(5L)).thenReturn(Optional.of(d));

        CriticalAlertDelivery failed = service.markFailed(5L, "network error");
        assertThat(failed.getDeliveryStatus()).isEqualTo("FAILED");
        assertThat(failed.getRetryCount()).isEqualTo(1);
        assertThat(failed.getFailureReason()).isEqualTo("network error");
    }

    @Test
    void retry_fromFailed_allowed() {
        CriticalAlertDelivery d = new CriticalAlertDelivery();
        d.setDeliveryId(5L);
        d.setDeliveryStatus("FAILED");
        when(deliveryRepository.findById(5L)).thenReturn(Optional.of(d));

        CriticalAlertDelivery retried = service.retry(5L);
        assertThat(retried.getDeliveryStatus()).isEqualTo("RETRY");
    }

    @Test
    void retry_fromPending_illegal() {
        CriticalAlertDelivery d = new CriticalAlertDelivery();
        d.setDeliveryId(5L);
        d.setDeliveryStatus("PENDING");
        when(deliveryRepository.findById(5L)).thenReturn(Optional.of(d));

        assertThatThrownBy(() -> service.retry(5L))
                .isInstanceOf(InvalidLabRequestException.class)
                .hasMessageContaining("Can only retry FAILED");
    }

    @Test
    void get_unknownId_throws404() {
        when(deliveryRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(999L))
                .isInstanceOf(LabEntityNotFoundException.class);
    }
}
