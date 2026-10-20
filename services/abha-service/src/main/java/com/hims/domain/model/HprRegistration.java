package com.hims.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hpr_registration")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HprRegistration extends AbhaBase {
    private String abhaId;
    private String healthId;
    private String name;
    private String gender;
    private String dateOfBirth;
    private String mobile;
    private String email;
    private String status;
}