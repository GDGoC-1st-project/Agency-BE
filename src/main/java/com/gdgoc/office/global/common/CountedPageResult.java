package com.gdgoc.office.global.common;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

@Schema(description = "전체 개수를 포함한 페이지 응답")
public record CountedPageResult<T>(
        @Schema(description = "전체 개수", example = "3") int totalCount,
        @Schema(description = "목록") List<T> items,
        @Schema(description = "현재 페이지 (0부터 시작)", example = "0") int page,
        @Schema(description = "페이지 크기", example = "20") int size,
        @Schema(description = "다음 페이지 존재 여부", example = "false") boolean hasNext
) {

    public static <E, T> CountedPageResult<T> of(Page<E> page, Function<E, T> mapper) {
        return new CountedPageResult<>((int) page.getTotalElements(),
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(), page.getSize(), page.hasNext());
    }
}
