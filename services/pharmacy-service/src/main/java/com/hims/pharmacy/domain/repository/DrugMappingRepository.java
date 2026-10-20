package com.hims.pharmacy.domain.repository;
import com.hims.pharmacy.domain.model.DrugMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DrugMappingRepository extends JpaRepository<DrugMapping, Long> {
    Optional<DrugMapping> findByItemId(Long itemId);
    List<DrugMapping> findByIsActiveTrue();
    List<DrugMapping> findByGenericNameContainingIgnoreCase(String genericName);
}
