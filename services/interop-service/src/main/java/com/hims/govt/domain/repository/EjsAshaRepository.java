package com.hims.govt.domain.repository;

import com.hims.govt.domain.model.EjsAsha;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EjsAshaRepository extends JpaRepository<EjsAsha, Long> {

    boolean existsByAshaCode(String ashaCode);
}
