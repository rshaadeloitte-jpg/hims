package com.hims.rmrs.domain.repository;
import com.hims.rmrs.domain.model.JournalVoucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface JournalVoucherRepository extends JpaRepository<JournalVoucher, Long> {
    List<JournalVoucher> findByDocStatusOrderByAddedDatetimeDesc(Short status);
    @Query(value = "SELECT nextval('ihms_rmrs.t_journal_voucher_seq')", nativeQuery = true)
    long nextId();
}
