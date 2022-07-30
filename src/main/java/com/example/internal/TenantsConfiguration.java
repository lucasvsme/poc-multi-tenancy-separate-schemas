package com.example.internal;

import com.example.Application;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Configuration
public class TenantsConfiguration {

    @Bean
    @SuppressWarnings("unchecked")
    Tenants tenants(Environment environment) {
        return new Tenants(environment.getRequiredProperty("spring.flyway.schemas", Set.class));
    }

    @Bean
    CurrentTenantIdentifierResolver currentTenantIdentifierResolver() {
        return new TenantSchemaSelector();
    }

    @Bean
    MultiTenantConnectionProvider multiTenantConnectionProvider(DataSource dataSource) {
        return new TenantConnectionProvider(dataSource);
    }

    @Bean
    public JpaVendorAdapter jpaVendorAdapter() {
        return new HibernateJpaVendorAdapter();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource,
                                                                       MultiTenantConnectionProvider multiTenantConnectionProviderImpl,
                                                                       CurrentTenantIdentifierResolver currentTenantIdentifierResolverImpl) {
        final var entityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
        entityManagerFactoryBean.setDataSource(dataSource);
        entityManagerFactoryBean.setPackagesToScan(Application.class.getPackageName());
        entityManagerFactoryBean.setJpaVendorAdapter(jpaVendorAdapter());
        entityManagerFactoryBean.setJpaPropertyMap(Map.ofEntries(
                Map.entry("hibernate.multiTenancy", "SCHEMA"),
                Map.entry("hibernate.multi_tenant_connection_provider", multiTenantConnectionProviderImpl),
                Map.entry("hibernate.tenant_identifier_resolver", currentTenantIdentifierResolverImpl)
        ));
        return entityManagerFactoryBean;
    }

    public static final class TenantSchemaSelector implements CurrentTenantIdentifierResolver {

        private static final Logger LOGGER = LoggerFactory.getLogger(TenantSchemaSelector.class);
        private static final String DEFAULT_SCHEMA = "public";

        @Override
        public String resolveCurrentTenantIdentifier() {
            final var tenant = Optional.ofNullable(Tenant.get())
                    .orElse(DEFAULT_SCHEMA);
            LOGGER.debug("Resolving tenant identifier (tenant={})", tenant);
            return tenant;
        }

        @Override
        public boolean validateExistingCurrentSessions() {
            return false;
        }
    }

    public static final class TenantConnectionProvider implements MultiTenantConnectionProvider {

        private static final Logger LOGGER = LoggerFactory.getLogger(TenantConnectionProvider.class);

        private final DataSource dataSource;

        public TenantConnectionProvider(DataSource dataSource) {
            this.dataSource = dataSource;
        }

        @Override
        public Connection getAnyConnection() throws SQLException {
            return dataSource.getConnection();
        }

        @Override
        public void releaseAnyConnection(Connection connection) throws SQLException {
            connection.close();
        }

        @Override
        public Connection getConnection(String tenantIdentifier) throws SQLException {
            LOGGER.debug("Getting connection for a tenant (tenantIdentifier={})", tenantIdentifier);

            final var connection = getAnyConnection();
            connection.setSchema(tenantIdentifier);
            return connection;
        }

        @Override
        public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
            LOGGER.debug("Releasing connection for a tenant (tenantIdentifier={})", tenantIdentifier);
            releaseAnyConnection(connection);
        }

        @Override
        public boolean supportsAggressiveRelease() {
            return false;
        }

        @Override
        public boolean isUnwrappableAs(Class<?> unwrapType) {
            return false;
        }

        @Override
        public <T> T unwrap(Class<T> unwrapType) {
            return null;
        }
    }
}