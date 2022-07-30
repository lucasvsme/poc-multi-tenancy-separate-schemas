package com.example.internal;

import com.example.testing.TenantInterceptorTestConfiguration;
import com.example.testing.TestController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebMvcTest(TestController.class)
@Import(TenantInterceptorTestConfiguration.class)
class TenantInterceptorTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void missingTenant() {
        webTestClient.get()
                .uri("/")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Missing database tenant")
                .jsonPath("$.detail").isEqualTo("Header X-Tenant-Id was not present in the request")
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.properties").doesNotExist();
    }

    @Test
    void unknownTenant() {
        final var tenantId = "foo";

        webTestClient.get()
                .uri("/")
                .header("X-Tenant-Id", tenantId)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Unknown database tenant")
                .jsonPath("$.detail").isEqualTo("Value of header X-Tenant-Id does not match a known database tenant")
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.properties").isMap()
                .jsonPath("$.properties.tenantId").isEqualTo(tenantId);
    }

    @Test
    void knownTenant() {
        final var tenantId = "company_a";

        webTestClient.get()
                .uri("/test")
                .header("X-Tenant-Id", tenantId)
                .exchange()
                .expectBody().isEmpty();
    }
}