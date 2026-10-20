package com.hims.iam.api;

import com.hims.iam.api.dto.AuthDtos.LoginResponse;
import com.hims.iam.application.OtpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth/otp")
@Tag(name = "OTP Auth (M35)", description = "OTP generate/verify -> JWT (SMS/EMAIL/WHATSAPP, 5-min TTL, 3 attempts)")
public class OtpController {

    private final OtpService otpService;

    public OtpController(OtpService otpService) {
        this.otpService = otpService;
    }

    @PostMapping("/generate")
    @Operation(summary = "Generate OTP for identifier (phone/email/username)")
    public ResponseEntity<Map<String, Object>> generate(@RequestBody Map<String, String> body) {
        String identifier = body.get("identifier");
        if (identifier == null) identifier = body.get("username");
        if (identifier == null) identifier = body.get("phone");
        String channel = body.get("channel");
        OtpService.OtpGenerateResult r = otpService.generate(identifier, channel);
        // in dev, devOtp is included; in prod it is null (OTP sent via gateway)
        var resp = new java.util.LinkedHashMap<String, Object>();
        resp.put("id", r.id());
        resp.put("identifier", r.identifier());
        resp.put("channel", r.channel());
        resp.put("expiresAt", r.expiresAt().toInstant().toString());
        if (r.devOtp() != null) resp.put("devOtp", r.devOtp());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify OTP and issue JWT")
    public ResponseEntity<LoginResponse> verify(@RequestBody Map<String, String> body) {
        String identifier = body.get("identifier");
        if (identifier == null) identifier = body.get("username");
        if (identifier == null) identifier = body.get("phone");
        String otp = body.get("otp");
        return ResponseEntity.ok(otpService.verify(identifier, otp));
    }
}
