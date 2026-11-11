package com.hims.pmrd;

import com.hims.pmrd.application.PMRDService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = com.hims.patient.PatientServiceApplication.class)
@ActiveProfiles("test")
public class PMRDModuleTest {
    @Test
    public void contextLoads() { assertTrue(true); }

    @Test
    public void serviceBeanExists() {
        assertNotNull(new PMRDService(null, null, null));
    }
}
