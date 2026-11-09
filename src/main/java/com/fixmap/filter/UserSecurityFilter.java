package com.fixmap.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.WebFilter;
import java.io.IOException;

@WebFilter("/*")
public final class UserSecurityFilter implements Filter {
    @Override public void doFilter(ServletRequest input, ServletResponse output, FilterChain chain) throws IOException, ServletException {
        var req = (HttpServletRequest) input;
        var res = (HttpServletResponse) output;
        String path=req.getRequestURI().substring(req.getContextPath().length());
        boolean userPage=path.isEmpty()||path.equals("/")||path.equals("/reports")||path.startsWith("/reports/")
            ||path.equals("/api/meta")||path.equals("/api/pins")||path.equals("/api/reports")
            ||(path.startsWith("/api/reports/")&&!path.equals("/api/reports/map"))||path.startsWith("/attachments/");
        if(!userPage){chain.doFilter(req,res);return;}
        req.setCharacterEncoding("UTF-8"); res.setCharacterEncoding("UTF-8");
        res.setHeader("X-Content-Type-Options", "nosniff");
        res.setHeader("Referrer-Policy", "same-origin");
        res.setHeader("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' blob:; object-src 'none'; base-uri 'self'; frame-ancestors 'none'; form-action 'self'");
        res.setHeader("Cache-Control", "no-store");
        if (!java.util.Set.of("GET", "HEAD", "OPTIONS").contains(req.getMethod())) {
            HttpSession session = req.getSession(false);
            String token = req.getHeader("X-CSRF-Token");
            if (session == null || token == null || !token.equals(session.getAttribute("csrf"))) {
                res.setStatus(403); res.setContentType("application/json");
                res.getWriter().write("{\"error\":\"페이지를 새로고침한 뒤 다시 시도해 주세요.\"}"); return;
            }
        }
        chain.doFilter(req,res);
    }
}
