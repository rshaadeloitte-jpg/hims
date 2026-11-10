package com.hims.aasmaan.domain.repository;

import com.hims.aasmaan.domain.model.AasmaanETicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AasmaanETicketRepository extends JpaRepository<AasmaanETicket, Long> {}
