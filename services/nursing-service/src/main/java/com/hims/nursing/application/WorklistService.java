package com.hims.nursing.application;

import com.hims.nursing.domain.model.CarePlan;
import com.hims.nursing.domain.model.NurseNote;
import com.hims.nursing.domain.model.NurseTask;
import com.hims.nursing.domain.model.PatientVitals;
import com.hims.nursing.domain.repository.CarePlanRepository;
import com.hims.nursing.domain.repository.NurseNoteRepository;
import com.hims.nursing.domain.repository.NurseTaskRepository;
import com.hims.nursing.domain.repository.PatientVitalsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** M26 nurse worklist: per-admission rollup of tasks / vitals / notes / care plan. */
@Service
public class WorklistService {
    private final NurseTaskRepository taskRepository;
    private final PatientVitalsRepository vitalsRepository;
    private final NurseNoteRepository noteRepository;
    private final CarePlanRepository carePlanRepository;
    public WorklistService(NurseTaskRepository taskRepository, PatientVitalsRepository vitalsRepository,
                          NurseNoteRepository noteRepository, CarePlanRepository carePlanRepository) {
        this.taskRepository = taskRepository; this.vitalsRepository = vitalsRepository;
        this.noteRepository = noteRepository; this.carePlanRepository = carePlanRepository;
    }

    public record TabView(String name, long open, long total) {}

    @Transactional(readOnly = true)
    public Map<String, Object> worklist(long admissionId) {
        List<NurseTask> tasks = taskRepository.findByAdmissionIdOrderByIdDesc(admissionId);
        List<PatientVitals> vitals = vitalsRepository.findAll().stream()
                .filter(v -> v.getAdmissionId() != null && v.getAdmissionId() == admissionId).toList();
        List<NurseNote> notes = noteRepository.findAll().stream()
                .filter(n -> n.getAdmissionId() != null && n.getAdmissionId() == admissionId).toList();
        List<CarePlan> plans = carePlanRepository.findAll().stream()
                .filter(p -> p.getAdmissionId() != null && p.getAdmissionId() == admissionId).toList();
        long openTasks = tasks.stream().filter(t -> t.getTaskStatus() != null && t.getTaskStatus() == 1).count();
        long totalVitals = vitals.size();
        long totalNotes = notes.size();
        long totalPlans = plans.size();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("admissionId", admissionId);
        out.put("tabs", java.util.List.of(
                new TabView("tasks", openTasks, tasks.size()),
                new TabView("vitals", totalVitals, totalVitals),
                new TabView("notes", totalNotes, totalNotes),
                new TabView("carePlan", totalPlans, totalPlans)));
        out.put("openTasks", openTasks);
        out.put("totalTasks", tasks.size());
        return out;
    }
}
