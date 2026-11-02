package com.gdgoc.office.global.util;

public final class TextUtils {

    private TextUtils() {
    }

    public static String preview(String text, int length) {
        if (text == null) {
            return null;
        }
        return text.length() <= length ? text : text.substring(0, length);
    }
}
