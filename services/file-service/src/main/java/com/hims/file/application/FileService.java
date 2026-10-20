package com.hims.file.application;

import com.hims.file.application.exceptions.FileNotFoundException;
import com.hims.file.application.exceptions.InvalidFileRequestException;
import com.hims.file.domain.model.FileDetails;
import com.hims.file.domain.repository.FileCategoryRepository;
import com.hims.file.domain.repository.FileDetailsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * File store service (M34, ported from FileManagementServiceImpl + FileDetails).
 * ponytail: base64 JSON upload (no multipart), PG bytea until MinIO; annotated image view as JSON text.
 */
@Service
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);
    public static final long AUDIT_ACTOR_ID = 1L;
    public static final long MAX_FILE_BYTES = 10 * 1024 * 1024; // 10 MB

    private final FileDetailsRepository fileRepository;
    private final FileCategoryRepository categoryRepository;

    public FileService(FileDetailsRepository fileRepository, FileCategoryRepository categoryRepository) {
        this.fileRepository = fileRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public FileDetails upload(Long fileCategoryId, String ownerType, Long ownerId,
                              String fileName, String fileType, String base64Data) {
        if (fileName == null || fileName.isBlank()) throw new InvalidFileRequestException("FILE_REQ", "fileName is required");
        if (base64Data == null || base64Data.isBlank()) throw new InvalidFileRequestException("FILE_REQ", "fileData (base64) is required");
        if (fileCategoryId != null) {
            categoryRepository.findById(fileCategoryId)
                    .orElseThrow(() -> new FileNotFoundException("NOT_FOUND", "Category not found: " + fileCategoryId));
        }
        // handle data URI prefix "data:...;base64,..."
        String b64 = base64Data.contains(",") ? base64Data.substring(base64Data.indexOf(",") + 1) : base64Data;
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(b64.trim());
        } catch (IllegalArgumentException e) {
            throw new InvalidFileRequestException("FILE_REQ", "fileData is not valid base64");
        }
        if (bytes.length > MAX_FILE_BYTES) throw new InvalidFileRequestException("FILE_TOO_LARGE", "File exceeds 10 MB");
        if (bytes.length == 0) throw new InvalidFileRequestException("FILE_REQ", "Decoded file is empty");
        String type = fileType != null && !fileType.isBlank() ? fileType.trim() : guessType(fileName);

        FileDetails f = new FileDetails();
        f.setFileCategoryId(fileCategoryId);
        f.setOwnerType(ownerType == null ? null : ownerType.trim().toUpperCase());
        f.setOwnerId(ownerId);
        f.setFileName(fileName.trim());
        f.setFileType(type);
        f.setFileSize((long) bytes.length);
        f.setFileToken(UUID.randomUUID().toString().replace("-", ""));
        f.setFileContent(bytes);
        f.setFileUrl("/api/v1/files/" + f.getFileToken() + "/download");
        f.setStatus(true);
        f.setAddedBy(AUDIT_ACTOR_ID);
        f.setAddedDatetime(Timestamp.from(Instant.now()));
        f.setUpdatedBy(AUDIT_ACTOR_ID);
        f.setUpdatedDatetime(Timestamp.from(Instant.now()));
        f = fileRepository.save(f);
        log.info("Uploaded file id={} token={} name={} size={} owner={}/{}", f.getId(), f.getFileToken(), f.getFileName(), f.getFileSize(), f.getOwnerType(), f.getOwnerId());
        return f;
    }

    @Transactional(readOnly = true)
    public FileDetails getById(long id) {
        return fileRepository.findById(id)
                .orElseThrow(() -> new FileNotFoundException("NOT_FOUND", "File not found: " + id));
    }

    @Transactional(readOnly = true)
    public FileDetails getByToken(String token) {
        return fileRepository.findByFileToken(token)
                .orElseThrow(() -> new FileNotFoundException("NOT_FOUND", "File not found for token: " + token));
    }

    @Transactional(readOnly = true)
    public List<FileDetails> listByOwner(String ownerType, Long ownerId) {
        if (ownerType == null || ownerType.isBlank() || ownerId == null) {
            throw new InvalidFileRequestException("FILE_REQ", "ownerType and ownerId are required");
        }
        return fileRepository.findByOwnerTypeAndOwnerId(ownerType.trim().toUpperCase(), ownerId)
                .stream().filter(f -> Boolean.TRUE.equals(f.getStatus())).toList();
    }

    @Transactional(readOnly = true)
    public List<FileDetails> listByCategory(long categoryId) {
        categoryRepository.findById(categoryId)
                .orElseThrow(() -> new FileNotFoundException("NOT_FOUND", "Category not found: " + categoryId));
        return fileRepository.findByFileCategoryId(categoryId).stream()
                .filter(f -> Boolean.TRUE.equals(f.getStatus())).toList();
    }

    @Transactional
    public void delete(long id) {
        FileDetails f = getById(id);
        f.setStatus(false);
        f.setUpdatedBy(AUDIT_ACTOR_ID);
        f.setUpdatedDatetime(Timestamp.from(Instant.now()));
        fileRepository.save(f);
    }

    @Transactional
    public FileDetails annotate(long id, String annotationJson) {
        if (annotationJson == null || annotationJson.isBlank()) throw new InvalidFileRequestException("FILE_REQ", "annotationJson is required");
        // ponytail: no JSON schema validation yet; add when annotator UI defines contract
        FileDetails f = getById(id);
        f.setAnnotationJson(annotationJson.trim());
        f.setUpdatedBy(AUDIT_ACTOR_ID);
        f.setUpdatedDatetime(Timestamp.from(Instant.now()));
        return fileRepository.save(f);
    }

    @Transactional(readOnly = true)
    public String viewAnnotation(long id) {
        FileDetails f = getById(id);
        if (f.getAnnotationJson() == null) throw new FileNotFoundException("NOT_FOUND", "No annotation for file: " + id);
        return f.getAnnotationJson();
    }

    private static String guessType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".txt")) return "text/plain";
        if (lower.endsWith(".dcm")) return "application/dicom";
        return "application/octet-stream";
    }
}
