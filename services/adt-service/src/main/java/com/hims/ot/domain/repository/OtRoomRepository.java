package com.hims.ot.domain.repository;

import com.hims.ot.domain.model.OtRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OtRoomRepository extends JpaRepository<OtRoom, Long> {
}
