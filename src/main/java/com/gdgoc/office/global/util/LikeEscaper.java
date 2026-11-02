package com.gdgoc.office.global.util;

public final class LikeEscaper {

    public static final char ESCAPE = '!';

    private LikeEscaper() {
    }

    /** LIKE 와일드카드를 이스케이프하고 소문자로 변환한다. JPQL에서 escape '!' 와 함께 사용한다. */
    public static String escape(String raw) {
        return raw.trim().toLowerCase()
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }

    public static String contains(String raw) {
        return "%" + escape(raw) + "%";
    }

    public static String startsWith(String raw) {
        return escape(raw) + "%";
    }
}
