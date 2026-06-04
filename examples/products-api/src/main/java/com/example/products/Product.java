package com.example.products;

import java.math.BigDecimal;

/** A product in the catalog. */
public record Product(Long id, String name, BigDecimal price, int quantity) {

    public Product withId(Long newId) {
        return new Product(newId, name, price, quantity);
    }
}
