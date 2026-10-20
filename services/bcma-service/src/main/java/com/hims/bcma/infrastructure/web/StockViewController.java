package com.hims.bcma.infrastructure.web;

import com.hims.bcma.application.StockViewService;
import com.hims.bcma.domain.model.MedicationStock;
import com.hims.bcma.domain.model.StockAlert;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/bcma/stock")
public class StockViewController {

    private final StockViewService service;

    public StockViewController(StockViewService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<MedicationStock> update(@RequestParam Long stockId, @RequestParam String drugNdc,
                                                   @RequestParam String batchId, @RequestParam BigDecimal quantityOnHand,
                                                   @RequestParam LocalDate expiryDate, @RequestParam String storageLocation) {
        return ResponseEntity.ok(service.updateStock(stockId, drugNdc, batchId, quantityOnHand, expiryDate, storageLocation));
    }

    @PostMapping("/alert")
    public ResponseEntity<StockAlert> createAlert(@RequestParam String drugNdc, @RequestParam String alertType,
                                                  @RequestParam String message, @RequestParam String severity) {
        return ResponseEntity.ok(service.createAlert(drugNdc, alertType, message, severity));
    }

    @PostMapping("/alert/resolve/{alertId}")
    public ResponseEntity<StockAlert> resolve(@PathVariable Long alertId) {
        return ResponseEntity.ok(service.resolveAlert(alertId));
    }

    @GetMapping("/drug/{drugNdc}")
    public ResponseEntity<List<MedicationStock>> byDrug(@PathVariable String drugNdc) {
        return ResponseEntity.ok(service.findByDrug(drugNdc));
    }

    @GetMapping("/alerts/unresolved")
    public ResponseEntity<List<StockAlert>> unresolved() {
        return ResponseEntity.ok(service.findUnresolved());
    }
}
