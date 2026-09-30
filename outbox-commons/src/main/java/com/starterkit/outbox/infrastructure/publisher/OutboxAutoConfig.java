package com.starterkit.outbox.infrastructure.publisher;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Auto-configuration for the Transactional Outbox Pattern.
 *
 * NOTE: @EntityScan and @EnableJpaRepositories are intentionally NOT declared here.
 * They must be declared on each service's @SpringBootApplication because declaring
 * them here would disable scanning of the service's own repositories.
 *
 * Each service must include:
 *   @EntityScan(basePackages = { "<service-pkg>", "com.starterkit.outbox.domain.entity" })
 *   @EnableJpaRepositories(basePackages = { "<service-pkg>", "com.starterkit.outbox.domain.repository" })
 */
@AutoConfiguration
@ConditionalOnProperty(name = "outbox.enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "com.starterkit.outbox")
@EnableConfigurationProperties(OutboxPublisherProperties.class)
@EnableScheduling
public class OutboxAutoConfig {
}
