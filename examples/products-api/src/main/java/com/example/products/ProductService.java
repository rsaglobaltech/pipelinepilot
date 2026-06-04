package com.example.products;

import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** In-memory product catalog. Swap for a JPA repository in a real deployment. */
@Service
public class ProductService {

    private final ConcurrentHashMap<Long, Product> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    public Collection<Product> findAll() {
        return store.values();
    }

    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    public Product create(Product product) {
        long id = sequence.incrementAndGet();
        Product saved = product.withId(id);
        store.put(id, saved);
        return saved;
    }

    public Optional<Product> update(Long id, Product product) {
        if (!store.containsKey(id)) {
            return Optional.empty();
        }
        Product saved = product.withId(id);
        store.put(id, saved);
        return Optional.of(saved);
    }

    public boolean delete(Long id) {
        return store.remove(id) != null;
    }
}
