package com.example.product.api;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public final class ProductResponse {

    @NotNull
    private Long id;

    @NotNull
    private String name;
}
