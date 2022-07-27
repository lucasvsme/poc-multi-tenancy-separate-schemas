package com.example.product.api;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Data
@NoArgsConstructor
public final class ProductRequest {

    @NotBlank(message = "Product name must be informed")
    @Length(min = 1, max = 15, message = "Product name length must be at most ${max} characters long")
    private String name;
}
