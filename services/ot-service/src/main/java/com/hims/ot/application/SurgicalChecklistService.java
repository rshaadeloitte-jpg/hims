package com.hims.ot.application;

import com.hims.ot.application.exceptions.OtEntityNotFoundException;
import com.hims.ot.application.exceptions.OtRuleException;
import com.hims.ot.domain.model.*;
import com.hims.ot.domain.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Service
public class SurgicalChecklistService {

    private final SurgicalChecklistRepository checklistRepo;
    private final ChecklistItemRepository itemRepo;
    private final ChecklistCompletionRepository completionRepo;

    public SurgicalChecklistService(SurgicalChecklistRepository checklistRepo,
                                    ChecklistItemRepository itemRepo,
                                    ChecklistCompletionRepository completionRepo) {
        this.checklistRepo = checklistRepo;
        this.itemRepo = itemRepo;
        this.completionRepo = completionRepo;
    }

    public SurgicalChecklist create(Long scheduleId, Long patientId, String phase) {
        if (scheduleId == null || patientId == null || phase == null || phase.isEmpty()) {
            throw new OtRuleException("BEOT005", "scheduleId, patientId and phase required");
        }
        SurgicalChecklist sc = new SurgicalChecklist();
        sc.setScheduleId(scheduleId);
        sc.setPatientId(patientId);
        sc.setPhase(phase);
        sc.setCompleted(false);
        sc.setAddedBy(1L);
        sc.setAddedDatetime(Timestamp.from(Instant.now()));
        return checklistRepo.save(sc);
    }

    public ChecklistItem addItem(Long checklistId, String itemName, String category) {
        ChecklistItem ci = new ChecklistItem();
        ci.setChecklistId(checklistId);
        ci.setItemName(itemName);
        ci.setCategory(category);
        ci.setCompleted(false);
        ci.setAddedBy(1L);
        ci.setAddedDatetime(Timestamp.from(Instant.now()));
        return itemRepo.save(ci);
    }

    public ChecklistItem completeItem(Long itemId) {
        ChecklistItem ci = itemRepo.findById(itemId).orElseThrow(() -> new OtEntityNotFoundException("ITEM_NOT_FOUND", "Checklist item not found"));
        ci.setCompleted(true);
        ci.setUpdatedBy(1L);
        ci.setUpdatedDatetime(Timestamp.from(Instant.now()));
        return itemRepo.save(ci);
    }

    public ChecklistCompletion recordCompletion(Long checklistId, String overallStatus) {
        ChecklistCompletion cc = new ChecklistCompletion();
        cc.setChecklistId(checklistId);
        cc.setOverallStatus(overallStatus);
        cc.setCompletedBy(1L);
        cc.setCompletedAt(Timestamp.from(Instant.now()));
        cc.setAddedBy(1L);
        cc.setAddedDatetime(Timestamp.from(Instant.now()));
        return completionRepo.save(cc);
    }

    public List<SurgicalChecklist> findBySchedule(Long scheduleId) {
        return checklistRepo.findByScheduleId(scheduleId);
    }
}
