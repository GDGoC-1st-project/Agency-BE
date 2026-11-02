package com.gdgoc.office.global.common;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Slice;

@Schema(description = "페이지 응답")
public record PageResult<T>(
        @Schema(description = "목록") List<T> items,
        @Schema(description = "현재 페이지 (0부터 시작)", example = "0") int page,
        @Schema(description = "페이지 크기", example = "20") int size,
        @Schema(description = "다음 페이지 존재 여부", example = "false") boolean hasNext
) {

    public static <E, T> PageResult<T> of(Slice<E> slice, Function<E, T> mapper) {
        return new PageResult<>(slice.getContent().stream().map(mapper).toList(),
                slice.getNumber(), slice.getSize(), slice.hasNext());
    }

    public static <T> PageResult<T> of(List<T> items, int page, int size, boolean hasNext) {
        return new PageResult<>(items, page, size, hasNext);
    }
}
