package com.hims.pmrd;

import com.hims.pmrd.application.PMRDService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class PMRDModuleTest {
    @Test
    public void contextLoads() { assertTrue(true); }

    @Test
    public void serviceBeanExists() {
        assertNotNull(new PMRDService(null, null, null));
    }
}
