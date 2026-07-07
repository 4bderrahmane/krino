package com.krino.backend.utility;

import com.krino.backend.exception.InvalidSortFieldException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

/**
 * Immutable allow-list of the entity properties a paged endpoint may be sorted by.
 *
 * <p>A raw Spring {@code Pageable} lets a client sort by ANY persistent property of
 * the entity — including sensitive columns (password hashes, resume object keys) and
 * lazily-joined relations that turn a cheap query into an expensive join or table
 * scan. Each paged endpoint declares the small set of columns it actually exposes and
 * runs the incoming {@code Pageable} through {@link #sanitize(Pageable)} before it
 * reaches the repository. Anything outside the set is rejected with a 400 (the global
 * handler maps {@link InvalidSortFieldException} to a validation error) instead of
 * reaching the database.
 */
public final class SortWhitelist {

    private final Set<String> allowedProperties;

    private SortWhitelist(Set<String> allowedProperties) {
        this.allowedProperties = allowedProperties;
    }

    /** Builds a whitelist from the given property names. Duplicates are rejected by {@link Set#of}. */
    public static SortWhitelist of(String... properties) {
        return new SortWhitelist(Set.of(properties));
    }

    /**
     * Returns {@code pageable} unchanged when every one of its sort properties is
     * allowed (an unsorted request passes through untouched), otherwise throws
     * {@link InvalidSortFieldException} naming the first offending property.
     */
    public Pageable sanitize(Pageable pageable) {
        for (Sort.Order order : pageable.getSort()) {
            if (!allowedProperties.contains(order.getProperty())) {
                throw new InvalidSortFieldException(order.getProperty(), allowedProperties);
            }
        }
        return pageable;
    }

    public Set<String> allowedProperties() {
        return allowedProperties;
    }
}
