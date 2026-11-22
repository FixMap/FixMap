package com.fixmap.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 메인(학교 지도) 화면(/). 지도, 핀, 층의 신고, 신고 작성은 모두 화면에서 JSON API로 불러오므로
 * 이 컨트롤러는 JSP에 연결만 한다. 선택한 층은 쿼리 level(층 번호)로 받는다.
 *
 * 모델 이름: level(쿼리로 받은 층 번호, 없으면 null. 없을 때 기본 층은 화면이 GET /api/buildings 결과로 고른다).
 */
@Controller
public class MainPageController {

    @GetMapping("/")
    public String main(@RequestParam(name = "level", required = false) Integer level, Model model) {
        model.addAttribute("level", level);
        return "main";
    }
}
