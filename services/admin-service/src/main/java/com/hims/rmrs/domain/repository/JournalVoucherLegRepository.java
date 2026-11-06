package com.hims.rmrs.domain.repository;
import com.hims.rmrs.domain.model.JournalVoucherLeg;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface JournalVoucherLegRepository extends JpaRepository<JournalVoucherLeg, Long> {
    List<JournalVoucherLeg> findByVoucherId(Long voucherId);
    @Query(value = "SELECT nextval('ihms_rmrs.t_journal_voucher_leg_seq')", nativeQuery = true)
    long nextId();
}
