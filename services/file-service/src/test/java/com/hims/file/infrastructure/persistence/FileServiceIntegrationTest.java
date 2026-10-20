package com.hims.file.infrastructure.persistence;

import com.hims.file.FileServiceApplication;
import com.hims.file.application.FileCategoryService;
import com.hims.file.application.FileService;
import com.hims.file.domain.model.FileCategory;
import com.hims.file.domain.model.FileDetails;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * L2 — file service on real PG16: V1 schema applies (ihms_file),
 * category tree + file upload/download + owner scoping + annotate.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = FileServiceApplication.class)
@DirtiesContext
@Transactional
class FileServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired FileCategoryService categoryService;
    @Autowired FileService fileService;
    @PersistenceContext EntityManager em;

    @Test
    void v1Schema_applies() {
        List<String> tables = em.createNativeQuery("""
                        SELECT table_name FROM information_schema.tables
                        WHERE table_schema = 'ihms_file' AND table_name IN
                          ('t_file_category','t_file_details')
                        ORDER BY table_name""").getResultList();
        assertThat(tables).containsExactly("t_file_category", "t_file_details");
    }

    @Test
    void category_createAndTree() {
        FileCategory c = categoryService.create("TEST_CAT", "Test Cat", "GENERIC", null);
        assertThat(c.getId()).isNotNull();
        FileCategory child = categoryService.create("CHILD", "Child", "GENERIC", c.getId());
        var tree = categoryService.tree("GENERIC");
        assertThat(tree).isNotEmpty();
    }

    @Test
    void file_uploadDownloadByToken_andOwnerList_andAnnotate() {
        String b64 = Base64.getEncoder().encodeToString("hello file content".getBytes());
        FileDetails f = fileService.upload(null, "PATIENT", 123L, "report.pdf", "application/pdf", b64);
        assertThat(f.getId()).isNotNull();
        assertThat(f.getFileToken()).hasSize(32);
        assertThat(f.getFileSize()).isEqualTo(18L);

        FileDetails byToken = fileService.getByToken(f.getFileToken());
        assertThat(byToken.getId()).isEqualTo(f.getId());
        assertThat(new String(byToken.getFileContent())).isEqualTo("hello file content");

        var list = fileService.listByOwner("PATIENT", 123L);
        assertThat(list).hasSize(1);

        FileDetails annotated = fileService.annotate(f.getId(), "{\"shapes\":[{\"x\":10}]}");
        assertThat(annotated.getAnnotationJson()).contains("shapes");
        assertThat(fileService.viewAnnotation(f.getId())).contains("shapes");
    }

    @Test
    void file_uploadBase64DataUri_handled() {
        String dataUri = "data:text/plain;base64," + Base64.getEncoder().encodeToString("hi".getBytes());
        FileDetails f = fileService.upload(null, "LAB", 1L, "a.txt", null, dataUri);
        assertThat(f.getFileType()).isEqualTo("text/plain");
        assertThat(new String(f.getFileContent())).isEqualTo("hi");
    }
}
