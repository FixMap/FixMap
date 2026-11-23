package com.fixmap.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.web.servlet.HandlerInterceptor;

import com.fixmap.service.AdminAccess;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

/**
 * /admin, /api/admin/** 접근 제한(MVP용 임시 방식).
 * - /admin?key=값 으로 처음 들어오면 세션에 저장하고 /admin으로 되돌려 보낸다(주소창에 키가 남지 않게).
 * - 이미 세션에 저장되어 있거나, API 요청에 X-Admin-Key 헤더가 맞으면 통과한다.
 * - 아니면 403. 화면은 안내 문구, API는 {"message": "..."} JSON.
 */
@RequiredArgsConstructor
public class AdminAccessInterceptor implements HandlerInterceptor {

    static final String SESSION_ATTR = "fixmap.admin";

    private final AdminAccess adminAccess;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null && Boolean.TRUE.equals(session.getAttribute(SESSION_ATTR))) {
            return true;
        }
        if (adminAccess.matches(request.getHeader("X-Admin-Key"))) {
            return true;
        }
        boolean api = request.getRequestURI().startsWith(request.getContextPath() + "/api/");
        if (!api && "GET".equals(request.getMethod()) && adminAccess.matches(request.getParameter("key"))) {
            request.getSession(true).setAttribute(SESSION_ATTR, true);
            response.sendRedirect(request.getContextPath() + "/admin");
            return false;
        }
        reject(response, api);
        return false;
    }

    private void reject(HttpServletResponse response, boolean api) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        if (api) {
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"관리자 접근 키가 필요합니다\"}");
        } else {
            response.setContentType("text/plain");
            response.getWriter().write("관리자 접근 키가 필요합니다. 관리자에게 받은 키로 /admin?key=키 주소에 접속해 주세요.");
        }
    }
}
