package com.krino.backend.dto.common;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

public record PageResponse<T>(
        List<T> content,
        PageMetadata page
)
{
    public record PageMetadata(
            int number,
            int size,
            long totalElements,
            int totalPages
    )
    {}

    public static <T> PageResponse<T> from(Page<T> page)
    {
        return new PageResponse<>(
                page.getContent(),
                new PageMetadata(
                        page.getNumber(),
                        page.getSize(),
                        page.getTotalElements(),
                        page.getTotalPages()
                )
        );
    }

    public static <E, T> PageResponse<T> from(Page<E> page, Function<? super E, ? extends T> mapper)
    {
        return from(page.map(mapper));
    }
}
