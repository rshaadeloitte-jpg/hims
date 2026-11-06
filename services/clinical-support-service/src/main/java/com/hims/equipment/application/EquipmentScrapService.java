package com.hims.equipment.application;

import com.hims.equipment.domain.model.EquipmentScrap;
import com.hims.equipment.domain.repository.EquipmentScrapRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class EquipmentScrapService {
    private final EquipmentScrapRepository repository;
    public EquipmentScrapService(EquipmentScrapRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public List<EquipmentScrap> list() { return repository.findAll(); }

    @Transactional(readOnly = true)
    public EquipmentScrap get(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Scrap not found: " + id));
    }

    public EquipmentScrap create(Map<String, Object> payload) {
        EquipmentScrap s = new EquipmentScrap();
        if (payload.get("equipmentId") != null) s.setEquipmentId(Long.parseLong(String.valueOf(payload.get("equipmentId"))));
        if (payload.get("scrapDate") != null) s.setScrapDate(LocalDate.parse(String.valueOf(payload.get("scrapDate"))));
        if (payload.get("scrapValue") != null) s.setScrapValue(new BigDecimal(String.valueOf(payload.get("scrapValue"))));
        if (payload.get("buyer") != null) s.setBuyer(String.valueOf(payload.get("buyer")));
        if (payload.get("status") != null) s.setStatus(String.valueOf(payload.get("status")));
        if (payload.get("remarks") != null) s.setRemarks(String.valueOf(payload.get("remarks")));
        return repository.save(s);
    }

    public EquipmentScrap updateStatus(Long id, String status) {
        EquipmentScrap s = get(id);
        s.setStatus(status);
        return repository.save(s);
    }
}