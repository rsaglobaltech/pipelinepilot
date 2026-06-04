package com.example.products;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ProductServiceTest {

    private final ProductService service = new ProductService();

    @Test
    void createAssignsIdAndStores() {
        Product saved = service.create(new Product(null, "Keyboard", new BigDecimal("49.90"), 10));
        assertThat(saved.id()).isNotNull();
        assertThat(service.findById(saved.id())).contains(saved);
    }

    @Test
    void updateReplacesExisting() {
        Product saved = service.create(new Product(null, "Mouse", new BigDecimal("19.90"), 5));
        Product updated = service.update(saved.id(), new Product(null, "Mouse Pro", new BigDecimal("29.90"), 3)).orElseThrow();
        assertThat(updated.name()).isEqualTo("Mouse Pro");
        assertThat(updated.quantity()).isEqualTo(3);
    }

    @Test
    void deleteRemoves() {
        Product saved = service.create(new Product(null, "Cable", new BigDecimal("4.90"), 100));
        assertThat(service.delete(saved.id())).isTrue();
        assertThat(service.findById(saved.id())).isEmpty();
    }

    @Test
    void updateMissingReturnsEmpty() {
        assertThat(service.update(999L, new Product(null, "Ghost", BigDecimal.ONE, 1))).isEmpty();
    }
}
