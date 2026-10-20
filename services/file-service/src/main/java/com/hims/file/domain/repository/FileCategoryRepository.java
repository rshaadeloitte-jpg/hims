package com.hims.file.domain.repository;

import com.hims.file.domain.model.FileCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FileCategoryRepository extends JpaRepository<FileCategory, Long> {
    Optional<FileCategory> findByCode(String code);
    boolean existsByCode(String code);
    List<FileCategory> findByDocumentType(String documentType);
    List<FileCategory> findByParentId(Long parentId);
}
