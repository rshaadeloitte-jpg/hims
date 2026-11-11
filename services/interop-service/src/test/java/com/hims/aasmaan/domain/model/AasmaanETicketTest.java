package com.hims.aasmaan.domain.model;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AasmaanETicketTest {
    @Test
    void eticket_default_open() {
        AasmaanETicket t = new AasmaanETicket();
        assertEquals("OPEN", t.getStatus());
    }
}
