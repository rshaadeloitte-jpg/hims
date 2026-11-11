package com.hims.aasmaan.domain.model;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AasmaanChecklistTest {
    @Test
    void checklist_defaults() {
        AasmaanChecklist ch = new AasmaanChecklist();
        assertNotNull(ch.getCompleted());
    }
}
