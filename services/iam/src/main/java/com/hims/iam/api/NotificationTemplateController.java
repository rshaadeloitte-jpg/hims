package com.hims.iam.api;

import com.hims.iam.application.NotificationTemplateService;
import com.hims.iam.domain.model.NotificationTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

@RestController
@RequestMapping("/api/v1/notification-templates")
@Tag(name = "Notification Templates (M35)", description = "Email/SMS/WhatsApp templates (ported from T_EMAILTEMPLATE)")
public class NotificationTemplateController {

    private final NotificationTemplateService service;

    public NotificationTemplateController(NotificationTemplateService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List templates (?channel=EMAIL/SMS/WHATSAPP)")
    public List<NotificationTemplate> list(@RequestParam(required = false) String channel) {
        return service.list(channel);
    }

    @PostMapping
    @Operation(summary = "Create a notification template (unique code)")
    public ResponseEntity<NotificationTemplate> create(@RequestBody Map<String, String> body) {
        NotificationTemplate t = service.create(body.get("code"), body.get("channel"),
                body.get("subject"), body.get("body"), body.get("smsText"));
        return ResponseEntity.status(HttpStatus.CREATED).body(t);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a notification template")
    public NotificationTemplate update(@PathVariable long id, @RequestBody Map<String, String> body) {
        return service.update(id, body.get("channel"), body.get("subject"), body.get("body"), body.get("smsText"));
    }
}
