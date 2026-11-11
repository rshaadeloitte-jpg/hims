package com.hims.file.application;

import com.hims.file.application.exceptions.FileNotFoundException;
import com.hims.file.application.exceptions.InvalidFileRequestException;
import com.hims.file.domain.model.FileCategory;
import com.hims.file.domain.repository.FileCategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Category tree service (M34, ported from FileManagementServiceImpl FileStore category).
 * ponytail: in-memory parent map for tree; add DB recursive CTE when volume grows.
 */
@Service
public class FileCategoryService {

    private static final Logger log = LoggerFactory.getLogger(FileCategoryService.class);
    public static final long AUDIT_ACTOR_ID = 1L;

    private final FileCategoryRepository repository;

    public FileCategoryService(FileCategoryRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<FileCategory> list(String documentType) {
        if (documentType != null && !documentType.isBlank()) {
            return repository.findByDocumentType(documentType.trim().toUpperCase());
        }
        return repository.findAll().stream().filter(c -> Boolean.TRUE.equals(c.getStatus())).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> tree(String documentType) {
        List<FileCategory> all = list(documentType);
        Map<Long, Map<String, Object>> byId = new HashMap<>();
        for (FileCategory c : all) {
            Map<String, Object> node = new HashMap<>();
            node.put("id", c.getId());
            node.put("code", c.getCode());
            node.put("name", c.getName());
            node.put("documentType", c.getDocumentType());
            node.put("parentId", c.getParentId());
            node.put("children", new ArrayList<>());
            byId.put(c.getId(), node);
        }
        List<Map<String, Object>> roots = new ArrayList<>();
        for (FileCategory c : all) {
            var node = byId.get(c.getId());
            if (c.getParentId() == null) roots.add(node);
            else {
                var parent = byId.get(c.getParentId());
                if (parent != null) ((List) parent.get("children")).add(node);
                else roots.add(node);
            }
        }
        return roots;
    }

    @Transactional
    public FileCategory create(String code, String name, String documentType, Long parentId) {
        if (code == null || code.isBlank()) throw new InvalidFileRequestException("FILE_REQ", "code is required");
        if (name == null || name.isBlank()) throw new InvalidFileRequestException("FILE_REQ", "name is required");
        if (repository.existsByCode(code.trim())) throw new InvalidFileRequestException("FILE_DUP", "Category code exists: " + code);
        if (parentId != null) repository.findById(parentId)
                .orElseThrow(() -> new FileNotFoundException("NOT_FOUND", "Parent category not found: " + parentId));
        FileCategory c = new FileCategory();
        c.setCode(code.trim());
        c.setName(name.trim());
        c.setDocumentType(documentType == null ? "GENERIC" : documentType.trim().toUpperCase());
        c.setParentId(parentId);
        c.setStatus(true);
        c.setAddedBy(AUDIT_ACTOR_ID);
        c.setAddedDatetime(Timestamp.from(Instant.now()));
        c.setUpdatedBy(AUDIT_ACTOR_ID);
        c.setUpdatedDatetime(Timestamp.from(Instant.now()));
        c = repository.save(c);
        log.info("Created file category id={} code={}", c.getId(), c.getCode());
        return c;
    }

    @Transactional
    public FileCategory rename(long id, String name) {
        FileCategory c = repository.findById(id)
                .orElseThrow(() -> new FileNotFoundException("NOT_FOUND", "Category not found: " + id));
        if (name == null || name.isBlank()) throw new InvalidFileRequestException("FILE_REQ", "name is required");
        c.setName(name.trim());
        c.setUpdatedBy(AUDIT_ACTOR_ID);
        c.setUpdatedDatetime(Timestamp.from(Instant.now()));
        return repository.save(c);
    }

    @Transactional
    public void delete(long id) {
        FileCategory c = repository.findById(id)
                .orElseThrow(() -> new FileNotFoundException("NOT_FOUND", "Category not found: " + id));
        // soft delete: status false (legacy FileManagementServiceImpl deleteDocumentCategory parity)
        c.setStatus(false);
        c.setUpdatedBy(AUDIT_ACTOR_ID);
        c.setUpdatedDatetime(Timestamp.from(Instant.now()));
        repository.save(c);
    }
}
