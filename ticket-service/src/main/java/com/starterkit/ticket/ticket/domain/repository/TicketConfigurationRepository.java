package com.starterkit.ticket.ticket.domain.repository;

import com.starterkit.ticket.ticket.domain.entity.TicketConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TicketConfigurationRepository extends JpaRepository<TicketConfiguration, Long> {

    Optional<TicketConfiguration> findByConfigKey(String configKey);

    Optional<TicketConfiguration> findByConfigKeyAndEnabledTrue(String configKey);

    List<TicketConfiguration> findAllByOrderByConfigKeyAsc();

    boolean existsByConfigKey(String configKey);
}
