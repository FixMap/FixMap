package com.fixmap.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** 화면(JSP) 이동 */
@Controller
public class PageController {

    /** 일반 사용자 메인 (학교 지도). 주소: /FixMap/ */
    @GetMapping("/")
    public String main() {
        return "main";
    }

    /** 관리자 페이지. 주소: /FixMap/admin */
    @GetMapping("/admin")
    public String admin() {
        return "admin";
    }
}
