package com.hims.billing.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.billing.application.BillingSagaIngressService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BillingSagaKafkaConsumerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static Map<String, Object> realItem() {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("lineNo", 1);
        item.put("itemType", "LAB");
        item.put("itemId", "901");
        item.put("serviceName", "CBC");
        item.put("rate", 10);
        item.put("qty", 1);
        item.put("doctorSharePercent", null);   // null doctorSharePercent
        return item;
    }

    private static Map<String, Object> envelope(String eventId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", "501");
        payload.put("patientId", "505");
        payload.put("encounterId", "7777");
        payload.put("admissionId", null);        // null admissionId
        payload.put("prescriberId", "9");
        payload.put("wardId", "3");
        payload.put("pharmacyPrescriptionId", null);
        payload.put("items", List.of(realItem()));
        payload.put("grossTotal", 10);
        payload.put("concessionPercent", 0);
        payload.put("concessionAmount", 0);
        payload.put("netTotal", 10);
        Map<String, Object> env = new LinkedHashMap<>();
        env.put("eventId", eventId);
        env.put("eventType", "OrderInitiated");
        env.put("payload", payload);
        return env;
    }

    private static ConsumerRecord<String, String> record(Object value) throws Exception {
        String v;
        if (value instanceof String s) {
            v = s;
        } else {
            v = MAPPER.writeValueAsString(value);
        }
        return new ConsumerRecord<>("ihms.ordering.events", 0, 0L, null, v);
    }

    @Test
    void orderInitiatedReachesIngressWithOrderCreatedAndLines() throws Exception {
        BillingSagaIngressService ingress = mock(BillingSagaIngressService.class);
        when(ingress.ingest(any(), any(), any())).thenReturn(
                BillingSagaIngressService.IngestResult.applied("evt-1", 10L, 501L, 1));
        BillingSagaKafkaConsumer consumer = new BillingSagaKafkaConsumer(ingress, MAPPER);

        Map<String, Object> env = envelope("evt-1");
        Map<String, Object> originalPayload = (Map<String, Object>) env.get("payload");

        consumer.listen(record(env));

        verify(ingress, times(1)).ingest(eq("evt-1"), eq("OrderCreated"),
                argThat(p -> p instanceof Map<?, ?> && ((Map<?, ?>) p).containsKey("lines")
                        && ((Map<?, ?>) p).get("lines").equals(originalPayload.get("items"))));
        // original payload must not have been mutated
        assertThat(originalPayload).doesNotContainKey("lines");
    }

    @Test
    void sameEventTwiceCallsIngressTwice() throws Exception {
        BillingSagaIngressService ingress = mock(BillingSagaIngressService.class);
        when(ingress.ingest(any(), any(), any()))
                .thenReturn(BillingSagaIngressService.IngestResult.applied("evt-2", 10L, 501L, 1))
                .thenReturn(BillingSagaIngressService.IngestResult.duplicate("evt-2", 10L, 501L));
        BillingSagaKafkaConsumer consumer = new BillingSagaKafkaConsumer(ingress, MAPPER);

        consumer.listen(record(envelope("evt-2")));
        consumer.listen(record(envelope("evt-2")));

        verify(ingress, times(2)).ingest(eq("evt-2"), eq("OrderCreated"), any());
    }

    @Test
    void malformedJsonSkipped() throws Exception {
        BillingSagaIngressService ingress = mock(BillingSagaIngressService.class);
        BillingSagaKafkaConsumer consumer = new BillingSagaKafkaConsumer(ingress, MAPPER);

        consumer.listen(record("{not json"));

        verify(ingress, never()).ingest(any(), any(), any());
    }

    @Test
    void missingPayloadSkipped() throws Exception {
        BillingSagaIngressService ingress = mock(BillingSagaIngressService.class);
        BillingSagaKafkaConsumer consumer = new BillingSagaKafkaConsumer(ingress, MAPPER);

        Map<String, Object> noPayload = new LinkedHashMap<>(envelope("p"));
        noPayload.put("payload", null);
        consumer.listen(record(noPayload));

        verify(ingress, never()).ingest(any(), any(), any());
    }

    @Test
    void runtimeExceptionFromIngressPropagates() throws Exception {
        BillingSagaIngressService ingress = mock(BillingSagaIngressService.class);
        when(ingress.ingest(eq("evt-x"), eq("OrderCreated"), any()))
                .thenThrow(new RuntimeException("boom"));
        BillingSagaKafkaConsumer consumer = new BillingSagaKafkaConsumer(ingress, MAPPER);

        assertThatThrownBy(() -> consumer.listen(record(envelope("evt-x"))))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("boom");
    }

    @Test
    void orderCreatedNowIgnored() throws Exception {
        BillingSagaIngressService ingress = mock(BillingSagaIngressService.class);
        BillingSagaKafkaConsumer consumer = new BillingSagaKafkaConsumer(ingress, MAPPER);

        Map<String, Object> other = new LinkedHashMap<>(envelope("z"));
        other.put("eventType", "OrderCreated");
        consumer.listen(record(other));

        verify(ingress, never()).ingest(any(), any(), any());
    }

    @Test
    void otherEventTypeIgnored() throws Exception {
        BillingSagaIngressService ingress = mock(BillingSagaIngressService.class);
        BillingSagaKafkaConsumer consumer = new BillingSagaKafkaConsumer(ingress, MAPPER);

        Map<String, Object> other = new LinkedHashMap<>(envelope("z2"));
        other.put("eventType", "LabResultFinalized");
        consumer.listen(record(other));

        verify(ingress, never()).ingest(any(), any(), any());
    }
}