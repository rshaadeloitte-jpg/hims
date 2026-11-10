package com.hims.aasmaan.application;

import com.hims.aasmaan.domain.model.AasmaanMonitor;
import com.hims.aasmaan.domain.repository.AasmaanMonitorRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AasmaanMonitorService {
    private final AasmaanMonitorRepository repo;
    public AasmaanMonitorService(AasmaanMonitorRepository repo) { this.repo = repo; }

    public AasmaanMonitor record(AasmaanMonitor m) {
        m.setRecordedAt(LocalDateTime.now());
        return repo.save(m);
    }
    public List<AasmaanMonitor> all() { return repo.findAll(); }
}
