package com.example.product.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ProductControllerTest {

    @Container
    private static final PostgreSQLContainer<?> DATABASE_CONTAINER =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres"));

    @DynamicPropertySource
    public static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", DATABASE_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", DATABASE_CONTAINER::getUsername);
        registry.add("spring.datasource.password", DATABASE_CONTAINER::getPassword);
        registry.add("spring.flyway.schemas", () -> "company_x");
    }

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void productNameNull() {
        final var productRequest = new ProductRequest();
        productRequest.setName(null);

        createProduct(productRequest);
    }

    @Test
    void productNameEmpty() {
        final var productRequest = new ProductRequest();
        productRequest.setName("  ");

        createProduct(productRequest);
    }

    @Test
    void productNameTooLong() {
        final var productRequest = new ProductRequest();
        productRequest.setName("MoreThan15chars!");

        createProduct(productRequest);
    }

    private void createProduct(ProductRequest productRequest) {
        webTestClient.post()
                .uri("/products")
                .header("X-Tenant-Id", "company_x")
                .body(BodyInserters.fromValue(productRequest))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);
    }
}