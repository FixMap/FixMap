package com.fixmap.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fixmap.service.AdminReportQueryService;

import lombok.RequiredArgsConstructor;

/**
 * 관리자 신고 관리 화면(/admin). 목록과 요약 타일은 JSP가 서버에서 바로 그리므로
 * 이 컨트롤러가 모델에 담아 준다. 상세 패널은 JSON API(GET /api/admin/reports/{id})를 부른다.
 *
 * 모델 이름: reports(목록), summary(요약), page, totalPages, totalCount,
 * q, status, building, unread, sort(받은 쿼리 그대로, 화면의 입력칸을 채우는 용도).
 * 쿼리 값이 올바르지 않으면 그 조건은 쓰지 않는다.
 */
@Controller
@RequiredArgsConstructor
public class AdminPageController {

    private final AdminReportQueryService queryService;

    @GetMapping("/admin")
    public String admin(@RequestParam(name = "q", required = false) String q,
                        @RequestParam(name = "status", required = false) String status,
                        @RequestParam(name = "building", required = false) Long building,
                        @RequestParam(name = "unread", defaultValue = "false") boolean unread,
                        @RequestParam(name = "sort", defaultValue = "latest") String sort,
                        @RequestParam(name = "page", defaultValue = "1") int page,
                        Model model) {
        AdminReportQueryService.ListResult result =
                queryService.list(new AdminReportQueryService.ListQuery(q, status, building, unread, sort, page));

        model.addAttribute("reports", result.reports());
        model.addAttribute("summary", result.summary());
        model.addAttribute("page", result.page());
        model.addAttribute("totalPages", result.totalPages());
        model.addAttribute("totalCount", result.totalCount());
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("status", status == null ? "" : status);
        model.addAttribute("building", building);
        model.addAttribute("unread", unread);
        model.addAttribute("sort", sort);
        return "admin";
    }
}
