package com.hims.file.application;

import com.hims.file.application.exceptions.InvalidFileRequestException;
import com.hims.file.domain.model.FileCategory;
import com.hims.file.domain.repository.FileCategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FileCategoryServiceTest {

    FileCategoryRepository repo;
    FileCategoryService service;

    @BeforeEach
    void setUp() {
        repo = mock(FileCategoryRepository.class);
        service = new FileCategoryService(repo);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void create_duplicateCode_rejected() {
        when(repo.existsByCode("DUP")).thenReturn(true);
        assertThatThrownBy(() -> service.create("DUP", "Name", "PATIENT", null))
                .isInstanceOf(InvalidFileRequestException.class);
    }

    @Test
    void create_withParent_validatesParentExists() {
        when(repo.existsByCode("CHILD")).thenReturn(false);
        when(repo.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create("CHILD", "Child", "PATIENT", 99L))
                .isInstanceOf(com.hims.file.application.exceptions.FileNotFoundException.class);
    }

    @Test
    void create_valid_returnsCategory() {
        when(repo.existsByCode("NEW")).thenReturn(false);
        FileCategory c = service.create("NEW", "New Cat", "LAB", null);
        assertThat(c.getCode()).isEqualTo("NEW");
        assertThat(c.getDocumentType()).isEqualTo("LAB");
    }

    @Test
    void rename_requiresName() {
        FileCategory c = cat(1L);
        when(repo.findById(1L)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.rename(1L, " "))
                .isInstanceOf(InvalidFileRequestException.class);
        FileCategory renamed = service.rename(1L, "Renamed");
        assertThat(renamed.getName()).isEqualTo("Renamed");
    }

    private static FileCategory cat(long id) {
        FileCategory c = new FileCategory();
        c.setId(id); c.setCode("C"); c.setName("Old"); c.setDocumentType("PATIENT"); c.setStatus(true);
        return c;
    }
}
