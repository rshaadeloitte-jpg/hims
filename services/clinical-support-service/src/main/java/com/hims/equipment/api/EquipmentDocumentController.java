package com.hims.equipment.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.hims.equipment.application.EquipmentDocumentService;
import com.hims.equipment.domain.model.EquipmentDetailDocs;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Equipment document API (MIG-023 Phase 5) — metadata-only document folder per
 * asset. Upload registers where the file lives (filePath); delete is a soft
 * delete so the audit trail survives.
 */
@RestController
@RequestMapping("/api/v1/equipment")
@Tag(name = "Equipment Documents (MIG-023)",
        description = "Document metadata per asset: upload registration, listing and soft delete")
public class EquipmentDocumentController {

    private final EquipmentDocumentService documentService;

    public EquipmentDocumentController(EquipmentDocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/{equipmentId}/documents")
    @Operation(summary = "Upload document metadata for an asset",
            description = "Body is the document record: {fileName (required), docType? (free dictionary e.g. "
                    + "MANUAL, WARRANTY_CARD, CALIBRATION_CERT, CONDEMNATION_REPORT), filePath?, description?}. "
                    + "Metadata only — no file bytes; filePath names where the file lives. Server assigns id from "
                    + "equipment_detail_docs_seq and pairs equipmentId from the path. Rejected for unknown or "
                    + "inactive assets (condemned assets still accept documents — condemnation/disposal paperwork).")
    public ResponseEntity<EquipmentDetailDocs> upload(@PathVariable long equipmentId,
                                                      @RequestBody(required = false) JsonNode body,
                                                      @RequestParam(required = false) Long unitId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.upload(equipmentId, body, unitId));
    }

    @GetMapping("/{equipmentId}/documents")
    @Operation(summary = "List documents of an asset",
            description = "Active document metadata rows of one asset, newest first.")
    public List<EquipmentDetailDocs> listByEquipment(@PathVariable long equipmentId) {
        return documentService.listByEquipment(equipmentId);
    }

    @DeleteMapping("/documents/{docId}")
    @Operation(summary = "Delete a document",
            description = "Soft delete (status=false + update stamps) so the audit trail survives; the document "
                    + "disappears from listings and a repeat delete reads as absent. Returns the tombstoned row.")
    public EquipmentDetailDocs delete(@PathVariable long docId) {
        return documentService.delete(docId);
    }
}
