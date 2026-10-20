package com.hims.aasmaan.application;

import com.hims.aasmaan.domain.model.AasmaanETicket;
import com.hims.aasmaan.domain.repository.AasmaanETicketRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AasmaanETicketService {
    private final AasmaanETicketRepository repo;
    public AasmaanETicketService(AasmaanETicketRepository repo) { this.repo = repo; }

    public AasmaanETicket create(AasmaanETicket t) {
        t.setCreatedAt(LocalDateTime.now());
        t.setStatus("OPEN");
        return repo.save(t);
    }
    public List<AasmaanETicket> all() { return repo.findAll(); }
    public AasmaanETicket byId(Long id) { return repo.findById(id).orElseThrow(); }
}
