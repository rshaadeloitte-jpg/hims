package com.hims.interop.api;

import com.hims.interop.application.InteropEventService;
import com.hims.interop.application.exceptions.InvalidRequestException;
import com.hims.interop.domain.model.OutboundHis;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Outbound HIS queue query API (register WP-T1). */
@RestController
@RequestMapping("/api/v1/outbound")
@Tag(name = "Outbound HIS queue (WP-T1)", description = "t_outbound_his read model: status 0=PENDING 1=SENT 2=FAILED 3=DEAD; targets HL7/EHR/PRINT. Dispatcher draining PENDING rows over MLLP is OUT OF SCOPE.")
public class OutboundController {

    private final InteropEventService service;

    public OutboundController(InteropEventService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List outbound messages", description = "?status= (0..3) and/or ?target= (HL7/EHR/PRINT) filter.")
    public List<OutboundHis> list(@RequestParam(required = false) Short status,
                                  @RequestParam(required = false) String target) {
        if (status != null && (status < OutboundHis.STATUS_PENDING || status > OutboundHis.STATUS_DEAD)) {
            throw new InvalidRequestException("BEXA003", "status must be within 0..3");
        }
        return service.list(status, target);
    }
}
