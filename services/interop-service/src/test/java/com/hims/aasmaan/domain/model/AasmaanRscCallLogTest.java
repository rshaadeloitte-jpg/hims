package com.hims.aasmaan.domain.model;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AasmaanRscCallLogTest {
    @Test
    void rsc_log_ref_not_null() {
        AasmaanRscCallLog r = new AasmaanRscCallLog();
        r.setCallRef("RSC-001");
        assertEquals("RSC-001", r.getCallRef());
    }
}
