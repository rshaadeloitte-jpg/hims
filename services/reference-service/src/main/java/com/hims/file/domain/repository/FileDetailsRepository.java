package com.hims.file.domain.repository;

import com.hims.file.domain.model.FileDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FileDetailsRepository extends JpaRepository<FileDetails, Long> {
    Optional<FileDetails> findByFileToken(String fileToken);
    List<FileDetails> findByOwnerTypeAndOwnerId(String ownerType, Long ownerId);
    List<FileDetails> findByFileCategoryId(Long fileCategoryId);
}
