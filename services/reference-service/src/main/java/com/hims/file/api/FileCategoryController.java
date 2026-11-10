package com.hims.file.api;

import com.hims.file.application.FileCategoryService;
import com.hims.file.domain.model.FileCategory;
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

@RestController
@RequestMapping("/api/v1/file-categories")
@Tag(name = "File Categories (M34)", description = "Document categories tree (ported from M_FILESTORE FileManagementServiceImpl)")
public class FileCategoryController {

    private final FileCategoryService service;

    public FileCategoryController(FileCategoryService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List categories (?documentType= filter)")
    public List<FileCategory> list(@RequestParam(required = false) String documentType) {
        return service.list(documentType);
    }

    @GetMapping("/tree")
    @Operation(summary = "Category tree (parentId nesting)")
    public List<Map<String, Object>> tree(@RequestParam(required = false) String documentType) {
        return service.tree(documentType);
    }

    @PostMapping
    @Operation(summary = "Create a category (unique code)")
    public ResponseEntity<FileCategory> create(@RequestBody Map<String, Object> body) {
        String code = (String) body.get("code");
        String name = (String) body.get("name");
        String documentType = (String) body.get("documentType");
        Long parentId = body.get("parentId") == null ? null : Long.valueOf(String.valueOf(body.get("parentId")));
        FileCategory c = service.create(code, name, documentType, parentId);
        return ResponseEntity.status(HttpStatus.CREATED).body(c);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Rename a category")
    public FileCategory rename(@PathVariable long id, @RequestBody Map<String, String> body) {
        return service.rename(id, body.get("name"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a category (status=false)")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
