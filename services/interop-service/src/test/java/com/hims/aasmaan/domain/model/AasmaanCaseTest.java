package com.hims.aasmaan.domain.model;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AasmaanCaseTest {
    @Test
    void open_case_isOpen_true() {
        AasmaanCase c = new AasmaanCase();
        c.setStatus((short) 1);
        assertTrue(c.isOpen());
    }
    @Test
    void transition_forwardOnly() {
        AasmaanCase c = new AasmaanCase(); c.setStatus((short) 1);
        assertTrue(c.canTransitionTo((short) 2));
        assertFalse(c.canTransitionTo((short) 3));
    }
}
