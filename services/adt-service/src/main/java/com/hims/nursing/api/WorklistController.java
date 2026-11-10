package com.hims.nursing.api;

import com.hims.nursing.application.WorklistService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/nursing")
public class WorklistController {
    private final WorklistService service;
    public WorklistController(WorklistService service) { this.service = service; }

    @GetMapping("/worklist/{admissionId}")
    public Map<String, Object> worklist(@PathVariable long admissionId) {
        return service.worklist(admissionId);
    }
}
