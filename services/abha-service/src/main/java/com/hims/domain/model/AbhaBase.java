package com.hims.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@Getter @Setter
public abstract class AbhaBase {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "abha_seq")
    @SequenceGenerator(name = "abha_seq", sequenceName = "abha_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false, updatable = false)
    private String addedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime addedDatetime;

    @Column(nullable = false)
    private String updatedBy;

    @Column(nullable = false)
    private LocalDateTime updatedDatetime;
}