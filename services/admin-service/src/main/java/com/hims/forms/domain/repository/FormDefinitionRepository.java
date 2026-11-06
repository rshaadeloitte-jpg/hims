package com.hims.forms.domain.repository;

import com.hims.forms.domain.model.FormDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FormDefinitionRepository extends JpaRepository<FormDefinition, Long> {

    Optional<FormDefinition> findByCode(String code);

    @Query("select f from FormDefinition f where f.status = true order by f.specialty, f.code")
    List<FormDefinition> findAllActive();

    Optional<FormDefinition> findByCodeAndVersion(String code, Integer version);

    @Query("select f from FormDefinition f where f.status = true and f.specialty = ?1 order by f.code")
    List<FormDefinition> findAllActiveBySpecialty(String specialty);

    @Query("select distinct f.specialty from FormDefinition f where f.status = true order by f.specialty")
    List<String> findDistinctSpecialties();
}
