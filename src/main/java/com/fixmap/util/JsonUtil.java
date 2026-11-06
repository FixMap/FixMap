package com.fixmap.util;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 외부 라이브러리 없이 JSON 응답을 만들 때 쓰는 최소 도구.
 * 응답이 복잡해지면 Gson 등의 jar를 WEB-INF/lib에 추가해서 바꿔도 된다.
 */
public final class JsonUtil {

    private JsonUtil() {
    }

    /** 문자열을 JSON 문자열 값으로 바꾼다. null이면 null. */
    public static String quote(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder(value.length() + 2).append('"');
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }

    public static void write(HttpServletResponse resp, int status, String json) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json; charset=UTF-8");
        resp.getWriter().write(json);
    }
}
