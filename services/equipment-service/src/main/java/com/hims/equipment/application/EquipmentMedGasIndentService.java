package com.hims.equipment.application;

import com.hims.equipment.domain.model.EquipmentMedGasIndent;
import com.hims.equipment.domain.repository.EquipmentMedGasIndentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional
public class EquipmentMedGasIndentService {
    private final EquipmentMedGasIndentRepository repository;
    public EquipmentMedGasIndentService(EquipmentMedGasIndentRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public List<EquipmentMedGasIndent> list() { return repository.findAll(); }

    @Transactional(readOnly = true)
    public EquipmentMedGasIndent get(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Indent not found: " + id));
    }

    public EquipmentMedGasIndent create(Map<String, Object> payload) {
        EquipmentMedGasIndent i = new EquipmentMedGasIndent();
        if (payload.get("indentNo") != null) i.setIndentNo(String.valueOf(payload.get("indentNo")));
        if (payload.get("departmentId") != null) i.setDepartmentId(Long.parseLong(String.valueOf(payload.get("departmentId"))));
        if (payload.get("gasType") != null) i.setGasType(String.valueOf(payload.get("gasType")));
        if (payload.get("quantity") != null) i.setQuantity(Integer.parseInt(String.valueOf(payload.get("quantity"))));
        if (payload.get("status") != null) i.setStatus(String.valueOf(payload.get("status")));
        if (payload.get("requestedBy") != null) i.setRequestedBy(Long.parseLong(String.valueOf(payload.get("requestedBy"))));
        if (payload.get("approvedBy") != null) i.setApprovedBy(Long.parseLong(String.valueOf(payload.get("approvedBy"))));
        if (payload.get("remarks") != null) i.setRemarks(String.valueOf(payload.get("remarks")));
        return repository.save(i);
    }

    public EquipmentMedGasIndent updateStatus(Long id, String status) {
        EquipmentMedGasIndent i = get(id);
        i.setStatus(status);
        return repository.save(i);
    }
}