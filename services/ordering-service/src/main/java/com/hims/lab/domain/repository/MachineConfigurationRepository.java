package com.hims.lab.domain.repository;

import com.hims.lab.domain.model.MachineConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MachineConfigurationRepository extends JpaRepository<MachineConfiguration, Long> {

    List<MachineConfiguration> findByActiveTrue();
}
