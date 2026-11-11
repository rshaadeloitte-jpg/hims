package com.hims.abha.domain.model;

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

    @Column(name = "added_by", nullable = false, updatable = false)
    private String addedBy;

    @Column(name = "added_datetime", nullable = false, updatable = false)
    private LocalDateTime addedDatetime;

    @Column(name = "updated_by", nullable = false)
    private String updatedBy;

    @Column(name = "updated_datetime", nullable = false)
    private LocalDateTime updatedDatetime;
}