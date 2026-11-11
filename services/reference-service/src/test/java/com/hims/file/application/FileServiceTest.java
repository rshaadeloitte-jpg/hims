package com.hims.file.application;

import com.hims.file.application.exceptions.InvalidFileRequestException;
import com.hims.file.domain.model.FileDetails;
import com.hims.file.domain.repository.FileCategoryRepository;
import com.hims.file.domain.repository.FileDetailsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FileServiceTest {

    FileDetailsRepository fileRepo;
    FileCategoryRepository catRepo;
    FileService service;

    @BeforeEach
    void setUp() {
        fileRepo = mock(FileDetailsRepository.class);
        catRepo = mock(FileCategoryRepository.class);
        service = new FileService(fileRepo, catRepo);
        when(fileRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void upload_validDecodesAndStores() {
        String b64 = Base64.getEncoder().encodeToString("hello world".getBytes());
        FileDetails f = service.upload(null, "PATIENT", 123L, "a.pdf", null, b64);
        assertThat(f.getFileName()).isEqualTo("a.pdf");
        assertThat(f.getFileSize()).isEqualTo(11L);
        assertThat(f.getFileToken()).hasSize(32);
        assertThat(f.getFileContent()).isEqualTo("hello world".getBytes());
        assertThat(f.getOwnerType()).isEqualTo("PATIENT");
    }

    @Test
    void upload_dataUriPrefix_handled() {
        String b64 = "data:text/plain;base64," + Base64.getEncoder().encodeToString("hi".getBytes());
        FileDetails f = service.upload(null, null, null, "a.txt", "text/plain", b64);
        assertThat(f.getFileContent()).isEqualTo("hi".getBytes());
    }

    @Test
    void upload_invalidBase64_rejected() {
        assertThatThrownBy(() -> service.upload(null, null, null, "a.pdf", null, "not-base64!!!"))
                .isInstanceOf(InvalidFileRequestException.class);
    }

    @Test
    void upload_unknownCategory_rejected() {
        when(catRepo.findById(999L)).thenReturn(Optional.empty());
        String b64 = Base64.getEncoder().encodeToString("hi".getBytes());
        assertThatThrownBy(() -> service.upload(999L, null, null, "a.pdf", null, b64))
                .isInstanceOf(com.hims.file.application.exceptions.FileNotFoundException.class);
    }

    @Test
    void upload_tooLarge_rejected() {
        byte[] big = new byte[(int) (FileService.MAX_FILE_BYTES + 1)];
        String b64 = Base64.getEncoder().encodeToString(big);
        assertThatThrownBy(() -> service.upload(null, null, null, "big.bin", null, b64))
                .isInstanceOf(InvalidFileRequestException.class)
                .hasMessageContaining("10 MB");
    }

    @Test
    void annotate_requiresJson() {
        when(fileRepo.findById(1L)).thenReturn(Optional.of(file(1L)));
        assertThatThrownBy(() -> service.annotate(1L, " "))
                .isInstanceOf(InvalidFileRequestException.class);
        FileDetails f = service.annotate(1L, "{\"shapes\":[]}");
        assertThat(f.getAnnotationJson()).isEqualTo("{\"shapes\":[]}");
    }

    private static FileDetails file(long id) {
        FileDetails f = new FileDetails();
        f.setId(id); f.setFileName("a.pdf"); f.setFileType("application/pdf");
        f.setFileContent("hi".getBytes()); f.setFileSize(2L); f.setFileToken("tok"); f.setStatus(true);
        return f;
    }
}
