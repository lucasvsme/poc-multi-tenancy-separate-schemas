package com.example.internal;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;

import javax.sql.DataSource;
import java.util.Set;

@Configuration
public class TenantsDatabaseInitializer implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TenantsDatabaseInitializer.class);

    @Override
    @SuppressWarnings("unchecked")
    public void onApplicationEvent(ContextRefreshedEvent event) {
        final var context = event.getApplicationContext();
        final var environment = context.getEnvironment();

        final var tenants = (Set<String>) environment.getRequiredProperty("spring.flyway.schemas", Set.class);
        for (final var schema : tenants) {
            LOGGER.info("Migrating tenant schema (schema={})", schema);

            final var flyway = Flyway.configure()
                    .dataSource(context.getBean(DataSource.class))
                    .locations(environment.getRequiredProperty("spring.flyway.locations"))
                    .schemas(schema)
                    .load();

            final var migrationResult = flyway.migrate();

            LOGGER.info(
                    "Tenant schema migrated successfully (schema={}, success={})",
                    migrationResult.schemaName,
                    migrationResult.success
            );
        }
    }
}
