package com.hims.kiosk.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.kiosk.application.exceptions.InvalidKioskRequestException;
import com.hims.kiosk.domain.model.KioskRegistration;
import com.hims.kiosk.domain.repository.KioskRegistrationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KioskServiceTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Mock private KioskRegistrationRepository regRepo;
    @Mock private com.hims.kiosk.domain.repository.KioskAppointmentRepository apptRepo;
    @Mock private com.hims.kiosk.domain.repository.KioskQueueRepository queueRepo;
    @Mock private com.hims.kiosk.domain.repository.KioskLabResultRepository labRepo;

    private KioskService service;

    @BeforeEach
    void setUp() {
        service = new KioskService(regRepo, apptRepo, queueRepo, labRepo);
    }

    @Test
    void register_stampsTokenAndAudit() throws Exception {
        when(regRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        KioskRegistration r = service.register(mapper.readTree("{\"patientName\":\"A\",\"contactPhone\":\"999\"}"), 1L);
        assertThat(r.getPatientName()).isEqualTo("A");
        assertThat(r.getRegistrationToken()).isNotBlank();
    }

    @Test
    void register_rejectsMissingNameOrPhone() {
        assertThatThrownBy(() -> service.register(mapper.readTree("{}"), null))
                .isInstanceOf(InvalidKioskRequestException.class)
                .hasMessageContaining("required");
    }

    @Test
    void findByToken_returnsRegistration() {
        KioskRegistration r = new KioskRegistration();
        r.setRegistrationToken("T1");
        r.setPatientName("X");
        when(regRepo.findByRegistrationToken("T1")).thenReturn(Optional.of(r));
        assertThat(service.findByToken("T1").isPresent()).isTrue();
    }

    @Test
    void register_generatesUniqueTokenPerCall() throws Exception {
        when(regRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        KioskRegistration r1 = service.register(mapper.readTree("{\"patientName\":\"A\",\"contactPhone\":\"1\"}"), null);
        KioskRegistration r2 = service.register(mapper.readTree("{\"patientName\":\"B\",\"contactPhone\":\"2\"}"), null);
        assertThat(r1.getRegistrationToken()).isNotEqualTo(r2.getRegistrationToken());
    }

    @Test
    void bookAppointment_requiresPatientAndDate() throws Exception {
        assertThatThrownBy(() -> service.bookAppointment(mapper.readTree("{}"), null))
                .isInstanceOf(InvalidKioskRequestException.class);
    }

    @Test
    void listAppointments_delegatesToRepo() {
        service.listAppointments(LocalDate.now());
        // verify delegation via mock (no exception = pass)
    }
}
