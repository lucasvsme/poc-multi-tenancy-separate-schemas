package com.example.internal;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class TenantsDatabaseInitializerTest {

    @Container
    private static final PostgreSQLContainer<?> CONTAINER =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres"));

    private AnnotationConfigApplicationContext applicationContext;

    @BeforeEach
    public void beforeEach() {
        // Creating context with application properties already registered
        final var parentContext = new GenericApplicationContext();
        parentContext.getEnvironment()
                .getPropertySources()
                .addLast(new MapPropertySource("default", Map.ofEntries(
                        Map.entry("spring.datasource.url", CONTAINER.getJdbcUrl()),
                        Map.entry("spring.datasource.username", CONTAINER.getUsername()),
                        Map.entry("spring.datasource.password", CONTAINER.getPassword()),
                        Map.entry("spring.flyway.locations", "db/migration"),
                        Map.entry("spring.flyway.schemas", "tenant_a,tenant_b")
                )));
        parentContext.refresh();

        // Creating a new context that is going to read properties already registered
        // and create beans using them
        this.applicationContext = new AnnotationConfigApplicationContext();
        this.applicationContext.setParent(parentContext);
        this.applicationContext.registerBean(DataSource.class, () -> {
            final var environment = parentContext.getEnvironment();
            final var dataSource = new HikariDataSource();
            dataSource.setJdbcUrl(environment.getRequiredProperty("spring.datasource.url"));
            dataSource.setUsername(environment.getRequiredProperty("spring.datasource.username"));
            dataSource.setPassword(environment.getRequiredProperty("spring.datasource.password"));
            return dataSource;
        });
        this.applicationContext.register(TenantsConfiguration.class);
    }

    @Test
    void runningDatabaseMigrationsForEachDataSource() {
        // Telling ApplicationContext to create the dependency tree and run the system under test
        applicationContext.addApplicationListener(new TenantsDatabaseInitializer());
        applicationContext.refresh();

        final var dataSource = applicationContext.getBean(DataSource.class);
        final var migrationsRunForTenantA = migrationTableCreated(dataSource, "tenant_a");
        final var migrationsRunForTenantB = migrationTableCreated(dataSource, "tenant_b");

        assertTrue(migrationsRunForTenantA);
        assertTrue(migrationsRunForTenantB);
    }

    private boolean migrationTableCreated(DataSource dataSource, String schema) {
        try (final var connection = dataSource.getConnection()) {
            final var query = "select exists(select from information_schema.tables where table_schema = ?)";
            final var statement = connection.prepareStatement(query);
            statement.setString(1, schema);
            final var resultSet = statement.executeQuery();
            resultSet.next();

            return resultSet.getBoolean(1);
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }
}