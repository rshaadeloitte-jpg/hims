package com.hims.lab.api;

import com.hims.lab.application.MachineService;
import com.hims.lab.domain.model.MachineConfiguration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Machine configuration CRUD (GAP-REGISTER WP-7): POST/GET/PUT/DELETE /api/v1/machines. */
@RestController
@RequestMapping("/api/v1/machines")
@Tag(name = "Machines (WP-7)", description = "Analyser/machine configuration (t_machineconfiguration): name, protocol (HL7/ASTM/file), active flag")
public class MachineController {

    private final MachineService service;

    public MachineController(MachineService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Create a machine configuration")
    public ResponseEntity<MachineConfiguration> create(@RequestBody Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(str(body, "name"), str(body, "protocol"), bool(body, "active")));
    }

    @GetMapping
    @Operation(summary = "List machines", description = "?active=true filters to active machines.")
    public List<MachineConfiguration> list(@RequestParam(required = false) Boolean active) {
        return service.list(active);
    }

    @GetMapping("/{id}")
    public MachineConfiguration get(@PathVariable long id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a machine configuration")
    public MachineConfiguration update(@PathVariable long id, @RequestBody Map<String, Object> body) {
        return service.update(id, str(body, "name"), str(body, "protocol"), bool(body, "active"));
    }

    /** Soft-delete: active=false (config history matters for instrument interfaces). */
    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate a machine configuration")
    public MachineConfiguration deactivate(@PathVariable long id) {
        return service.deactivate(id);
    }

    private static String str(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return v == null ? null : String.valueOf(v);
    }

    private static Boolean bool(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null) return null;
        if (v instanceof Boolean b) return b;
        return Boolean.valueOf(String.valueOf(v));
    }
}
