package com.hims.adt.domain.repository;

import com.hims.adt.domain.model.MortuaryChamber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MortuaryChamberRepository extends JpaRepository<MortuaryChamber, Long> {
}
