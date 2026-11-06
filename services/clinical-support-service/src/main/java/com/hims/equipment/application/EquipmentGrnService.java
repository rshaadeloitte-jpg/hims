package com.hims.equipment.application;

import com.hims.equipment.domain.model.EquipmentGrn;
import com.hims.equipment.domain.repository.EquipmentGrnRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class EquipmentGrnService {

    private final EquipmentGrnRepository repository;

    public EquipmentGrnService(EquipmentGrnRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<EquipmentGrn> list() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public EquipmentGrn get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("GRN not found: " + id));
    }

    public EquipmentGrn create(Map<String, Object> payload) {
        EquipmentGrn g = new EquipmentGrn();
        if (payload.get("grnNo") != null) g.setGrnNo(String.valueOf(payload.get("grnNo")));
        if (payload.get("equipmentId") != null) g.setEquipmentId(Long.parseLong(String.valueOf(payload.get("equipmentId"))));
        if (payload.get("supplierId") != null) g.setSupplierId(Long.parseLong(String.valueOf(payload.get("supplierId"))));
        if (payload.get("grnDate") != null) g.setGrnDate(LocalDate.parse(String.valueOf(payload.get("grnDate"))));
        if (payload.get("quantity") != null) g.setQuantity(Integer.parseInt(String.valueOf(payload.get("quantity"))));
        if (payload.get("status") != null) g.setStatus(String.valueOf(payload.get("status")));
        if (payload.get("remarks") != null) g.setRemarks(String.valueOf(payload.get("remarks")));
        return repository.save(g);
    }

    public EquipmentGrn updateStatus(Long id, String status) {
        EquipmentGrn g = get(id);
        g.setStatus(status);
        return repository.save(g);
    }
}