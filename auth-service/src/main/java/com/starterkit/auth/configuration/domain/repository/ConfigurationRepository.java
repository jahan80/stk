package com.starterkit.auth.configuration.domain.repository;

import com.starterkit.auth.configuration.domain.entity.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConfigurationRepository extends JpaRepository<Configuration, Long> {

    Optional<Configuration> findByConfigKey(String configKey);

    Optional<Configuration> findByConfigKeyAndEnabledTrue(String configKey);

    List<Configuration> findAllByEnabledTrueOrderByConfigKeyAsc();

}