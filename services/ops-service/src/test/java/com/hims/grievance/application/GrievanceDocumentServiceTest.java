package com.hims.grievance.application;

import com.hims.grievance.application.exceptions.GrievanceNotFoundException;
import com.hims.grievance.application.exceptions.InvalidGrievanceRequestException;
import com.hims.grievance.domain.model.GrievanceDocument;
import com.hims.grievance.domain.model.GrievanceTicket;
import com.hims.grievance.domain.repository.GrievanceDocumentRepository;
import com.hims.grievance.domain.repository.GrievanceTicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GrievanceDocumentServiceTest {

    @Mock private GrievanceDocumentRepository documentRepository;
    @Mock private GrievanceTicketRepository ticketRepository;
    private GrievanceDocumentService service;

    @BeforeEach void setUp() { service = new GrievanceDocumentService(documentRepository, ticketRepository); }

    @Test void upload_ok() {
        GrievanceTicket t = new GrievanceTicket(); t.setId(11L);
        when(ticketRepository.findById(11L)).thenReturn(Optional.of(t));
        when(documentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        GrievanceDocument d = service.upload(11L, 7L, "complaint.pdf");
        assertThat(d.getFileName()).isEqualTo("complaint.pdf");
        assertThat(d.getGrievanceId()).isEqualTo(11L);
        assertThat(d.getUserId()).isEqualTo(7L);
    }

    @Test void upload_blankFileName_throws() {
        GrievanceTicket t = new GrievanceTicket(); t.setId(11L);
        when(ticketRepository.findById(11L)).thenReturn(Optional.of(t));
        assertThatThrownBy(() -> service.upload(11L, 7L, "  "))
                .isInstanceOf(InvalidGrievanceRequestException.class);
    }

    @Test void upload_unknownTicket_throws() {
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.upload(99L, 7L, "x"))
                .isInstanceOf(GrievanceNotFoundException.class);
    }

    @Test void listForTicket_returnsRows() {
        when(ticketRepository.existsById(11L)).thenReturn(true);
        when(documentRepository.findByGrievanceIdOrderByIdAsc(11L)).thenReturn(List.of(new GrievanceDocument()));
        assertThat(service.listForTicket(11L)).hasSize(1);
    }

    @Test void listForTicket_unknownTicket_throws() {
        when(ticketRepository.existsById(99L)).thenReturn(false);
        assertThatThrownBy(() -> service.listForTicket(99L))
                .isInstanceOf(GrievanceNotFoundException.class);
    }
}
