package com.gdgoc.office.domain.keyword.dto;

import com.gdgoc.office.domain.keyword.entity.Keyword;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public final class KeywordResponse {

    private KeywordResponse() {
    }

    @Schema(description = "키워드")
    public record Item(
            @Schema(description = "키워드 ID", example = "1") Long keywordId,
            @Schema(description = "키워드 이름", example = "Spring") String name
    ) {
        public static Item from(Keyword keyword) {
            return new Item(keyword.getId(), keyword.getName());
        }
    }

    @Schema(description = "키워드 검색 결과")
    public record SearchResult(
            @Schema(description = "검색된 키워드 목록") List<Item> items
    ) {
    }

    @Schema(description = "관계별 키워드 목록")
    public record Grouped(
            @Schema(description = "할 수 있어요") List<Item> can,
            @Schema(description = "배우고 있어요") List<Item> learning,
            @Schema(description = "관심 있어요") List<Item> interested
    ) {
    }
}
