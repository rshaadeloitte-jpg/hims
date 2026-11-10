package com.hims.file.api;

import com.hims.file.application.FileService;
import com.hims.file.domain.model.FileDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

import java.util.Base64;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/files")
@Tag(name = "Files (M34)", description = "Document upload/download, owner scoping, annotated image view (ported from M_FILEDETAILS + FileManagementServiceImpl + ImageAnnotationMaster)")
public class FileController {

    private final FileService service;

    public FileController(FileService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Upload a file (base64 JSON, 10 MB limit)",
            description = "fileCategoryId nullable; ownerType/ownerId nullable (e.g. PATIENT/123); fileName + fileData (base64, data URI allowed) required. Returns file_token for download.")
    public ResponseEntity<FileDetails> upload(@RequestBody Map<String, Object> body) {
        Long fileCategoryId = body.get("fileCategoryId") == null ? null : Long.valueOf(String.valueOf(body.get("fileCategoryId")));
        String ownerType = (String) body.get("ownerType");
        Long ownerId = body.get("ownerId") == null ? null : Long.valueOf(String.valueOf(body.get("ownerId")));
        String fileName = (String) body.get("fileName");
        String fileType = (String) body.get("fileType");
        String fileData = (String) body.get("fileData");
        FileDetails f = service.upload(fileCategoryId, ownerType, ownerId, fileName, fileType, fileData);
        return ResponseEntity.status(HttpStatus.CREATED).body(f);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get file metadata by id")
    public FileDetails getById(@PathVariable long id) {
        return service.getById(id);
    }

    @GetMapping("/token/{token}")
    @Operation(summary = "Get file metadata by token")
    public FileDetails getByToken(@PathVariable String token) {
        return service.getByToken(token);
    }

    @GetMapping("/token/{token}/download")
    @Operation(summary = "Download file content by token (bytes)")
    public ResponseEntity<byte[]> download(@PathVariable String token) {
        FileDetails f = service.getByToken(token);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + f.getFileName() + "\"")
                .contentType(MediaType.parseMediaType(f.getFileType()))
                .contentLength(f.getFileSize())
                .body(f.getFileContent());
    }

    @GetMapping("/token/{token}/base64")
    @Operation(summary = "Download file as base64 JSON (for browsers without byte handling)")
    public Map<String, Object> downloadBase64(@PathVariable String token) {
        FileDetails f = service.getByToken(token);
        String b64 = Base64.getEncoder().encodeToString(f.getFileContent());
        return Map.of("fileName", f.getFileName(), "fileType", f.getFileType(),
                "fileSize", f.getFileSize(), "fileData", b64, "fileToken", f.getFileToken());
    }

    @GetMapping
    @Operation(summary = "List files by owner (?ownerType=&ownerId=) or by category (?fileCategoryId=)")
    public List<FileDetails> list(@RequestParam(required = false) String ownerType,
                                  @RequestParam(required = false) Long ownerId,
                                  @RequestParam(required = false) Long fileCategoryId) {
        if (fileCategoryId != null) return service.listByCategory(fileCategoryId);
        if (ownerType != null && ownerId != null) return service.listByOwner(ownerType, ownerId);
        throw new com.hims.file.application.exceptions.InvalidFileRequestException("FILE_REQ", "Provide ownerType+ownerId or fileCategoryId");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a file (status=false)")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/annotation")
    @Operation(summary = "Save annotated image view JSON (ImageAnnotationMaster parity)")
    public FileDetails annotate(@PathVariable long id, @RequestBody Map<String, String> body) {
        String annotationJson = body.get("annotationJson");
        if (annotationJson == null) annotationJson = body.get("annotation");
        return service.annotate(id, annotationJson);
    }

    @GetMapping("/{id}/annotation")
    @Operation(summary = "View annotated image JSON")
    public Map<String, String> viewAnnotation(@PathVariable long id) {
        String json = service.viewAnnotation(id);
        return Map.of("annotationJson", json);
    }
}
