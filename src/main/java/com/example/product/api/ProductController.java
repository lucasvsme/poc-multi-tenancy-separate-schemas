package com.example.product.api;

import com.example.product.Product;
import com.example.product.ProductRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;

@RestController
@RequestMapping("/products")
public class ProductController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductController.class);

    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @PostMapping
    public ResponseEntity<Void> createOne(@Valid @RequestBody ProductRequest productRequest,
                                          UriComponentsBuilder uriComponentsBuilder) {
        LOGGER.info("Creating new product (request={})", productRequest);

        final var product = new Product();
        product.setName(productRequest.getName());

        final var productCreated = productRepository.save(product);
        LOGGER.info("New product created (product={})", productCreated);

        final var productUri = uriComponentsBuilder.path("/{productId}")
                .build(productCreated.getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .location(productUri)
                .build();
    }

    @GetMapping
    public ResponseEntity<ProductsResponse> findAll() {
        LOGGER.info("Finding all existing products");

        final var products = new ArrayList<ProductResponse>();
        for (final var product : productRepository.findAll()) {
            final var productResponse = new ProductResponse();
            productResponse.setId(product.getId());
            productResponse.setName(product.getName());
            products.add(productResponse);
        }

        final var productsResponse = new ProductsResponse();
        productsResponse.setProducts(products);
        LOGGER.info("Returning all products found (products={})", productsResponse);

        return ResponseEntity.status(HttpStatus.OK)
                .body(productsResponse);
    }
}
