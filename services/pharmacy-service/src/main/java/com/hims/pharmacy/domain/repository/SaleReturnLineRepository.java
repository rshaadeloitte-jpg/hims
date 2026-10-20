package com.hims.pharmacy.domain.repository;

import com.hims.pharmacy.domain.model.SaleReturnLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SaleReturnLineRepository extends JpaRepository<SaleReturnLine, SaleReturnLine.Key> {
    List<SaleReturnLine> findByReturnId(Long returnId);
}
