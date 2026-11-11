package com.hims.kiosk.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KioskDepartmentTest {

    @Test
    void testGettersAndSetters() {
        KioskDepartment dept = new KioskDepartment();
        dept.setId(10L);
        dept.setName("Orthopedics");
        dept.setActive(true);

        assertThat(dept.getId()).isEqualTo(10L);
        assertThat(dept.getName()).isEqualTo("Orthopedics");
        assertThat(dept.getActive()).isTrue();
    }

    @Test
    void testConstructor() {
        KioskDepartment dept = new KioskDepartment(20L, "Neurology", false);
        assertThat(dept.getId()).isEqualTo(20L);
        assertThat(dept.getName()).isEqualTo("Neurology");
        assertThat(dept.getActive()).isFalse();
    }
}
