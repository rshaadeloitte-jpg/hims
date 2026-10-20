package com.hims.aasmaan.application;

import com.hims.aasmaan.domain.model.AasmaanRscCallLog;
import com.hims.aasmaan.domain.repository.AasmaanRscCallLogRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class AasmaanRscCallLogService {
    private final AasmaanRscCallLogRepository repo;
    public AasmaanRscCallLogService(AasmaanRscCallLogRepository repo) { this.repo = repo; }

    public AasmaanRscCallLog log(AasmaanRscCallLog r) {
        r.setLoggedAt(LocalDateTime.now());
        return repo.save(r);
    }
    public java.util.List<AasmaanRscCallLog> all() { return repo.findAll(); }
}
