package com.hims.grievance.application;

import com.hims.grievance.application.exceptions.GrievanceNotFoundException;
import com.hims.grievance.application.exceptions.InvalidGrievanceRequestException;
import com.hims.grievance.domain.model.GrievanceDocument;
import com.hims.grievance.domain.model.GrievanceTicket;
import com.hims.grievance.domain.repository.GrievanceDocumentRepository;
import com.hims.grievance.domain.repository.GrievanceTicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/** M22 grievance: document upload + listing. */
@Service
public class GrievanceDocumentService {
    private static final long ACTOR = 1L;
    private final GrievanceDocumentRepository documentRepository;
    private final GrievanceTicketRepository ticketRepository;

    public GrievanceDocumentService(GrievanceDocumentRepository documentRepository,
                                   GrievanceTicketRepository ticketRepository) {
        this.documentRepository = documentRepository;
        this.ticketRepository = ticketRepository;
    }

    @Transactional
    public GrievanceDocument upload(long grievanceId, Long userId, String fileName) {
        GrievanceTicket ticket = ticketRepository.findById(grievanceId)
                .orElseThrow(() -> new GrievanceNotFoundException("NOT_FOUND", "Ticket not found: " + grievanceId));
        if (fileName == null || fileName.isBlank()) {
            throw new InvalidGrievanceRequestException("BEXA003", "fileName is required");
        }
        GrievanceDocument d = new GrievanceDocument();
        d.setGrievanceId(ticket.getId());
        d.setUserId(userId);
        d.setFileName(fileName);
        stamp(d);
        return documentRepository.save(d);
    }

    @Transactional(readOnly = true)
    public List<GrievanceDocument> listForTicket(long grievanceId) {
        if (!ticketRepository.existsById(grievanceId)) {
            throw new GrievanceNotFoundException("NOT_FOUND", "Ticket not found: " + grievanceId);
        }
        return documentRepository.findByGrievanceIdOrderByIdAsc(grievanceId);
    }

    private void stamp(com.hims.grievance.domain.model.GrievanceBase entity) {
        entity.setAddedBy(ACTOR);
        entity.setAddedDatetime(Timestamp.from(Instant.now()));
        entity.setUpdatedBy(ACTOR);
        entity.setUpdatedDatetime(Timestamp.from(Instant.now()));
        entity.setSynchronizedd(false);
    }
}
