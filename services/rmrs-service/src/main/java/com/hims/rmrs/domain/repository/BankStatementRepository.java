package com.hims.rmrs.domain.repository;
import com.hims.rmrs.domain.model.BankStatement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface BankStatementRepository extends JpaRepository<BankStatement, Long> {
    List<BankStatement> findByDocStatusOrderByTxnDateAsc(Short status);
    @Query(value = "SELECT nextval('ihms_rmrs.t_bank_statement_seq')", nativeQuery = true)
    long nextId();
}
