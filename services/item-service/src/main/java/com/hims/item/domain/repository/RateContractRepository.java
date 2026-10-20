package com.hims.item.domain.repository;

import com.hims.item.domain.model.RateContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface RateContractRepository extends JpaRepository<RateContract, Long> {
    List<RateContract> findBySupplierIdAndItemId(Long supplierId, Long itemId);
    @Query("SELECT r FROM RateContract r WHERE r.supplierId = :supplierId AND r.itemId = :itemId "
         + "AND r.statusFlag = :active "
         + "AND r.validFrom <= :onDate AND r.validTo >= :onDate")
    Optional<RateContract> findActiveOnDate(@Param("supplierId") Long supplierId,
                                           @Param("itemId") Long itemId,
                                           @Param("active") Short active,
                                           @Param("onDate") java.sql.Date onDate);
    @Query(value = "SELECT nextval('ihms_item.t_rate_contract_seq')", nativeQuery = true)
    long nextId();
}
