package com.hims.cssd.domain.repository;

import com.hims.cssd.domain.model.KitIssue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KitIssueRepository extends JpaRepository<KitIssue, Long> {

    List<KitIssue> findByKitIdOrderByIssuedAtDesc(Long kitId);
}
