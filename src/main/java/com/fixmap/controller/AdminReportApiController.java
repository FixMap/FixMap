package com.fixmap.controller;

import java.time.format.DateTimeFormatter;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fixmap.domain.ReportComment;
import com.fixmap.service.AdminReportQueryService;
import com.fixmap.service.AdminReportService;

import lombok.RequiredArgsConstructor;

/**
 * 관리자 화면(/admin)이 부르는 JSON API. 요청·응답 형식은 Notion Backend의
 * "API 명세서" 중 "관리자 처리 저장" 행을 따른다.
 */
@RestController
@RequiredArgsConstructor
public class AdminReportApiController {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final AdminReportService adminReportService;
    private final AdminReportQueryService queryService;

    /** GET /api/admin/reports/{id} : 신고 상세(내용, 첨부, 핀 위치, 답글). 열면 읽음으로 바뀐다. */
    @GetMapping("/api/admin/reports/{id}")
    public AdminReportQueryService.Detail detail(@PathVariable("id") Long id) {
        return queryService.detail(id);
    }

    /** PATCH /api/admin/reports/{id} : 처리 상태 변경과 답글 작성을 한 번에 저장 */
    @PatchMapping("/api/admin/reports/{id}")
    public SaveResponse save(@PathVariable("id") Long id, @RequestBody SaveRequest request) {
        AdminReportService.SaveResult result =
                adminReportService.save(id, request.status(), request.comment(), request.reason());
        return new SaveResponse(result.reportId(), result.status(), toResponse(result.comment()));
    }

    private static CommentResponse toResponse(ReportComment comment) {
        if (comment == null) {
            return null;
        }
        return new CommentResponse(comment.getId(), comment.getContent(), comment.getCreatedAt().format(TIME));
    }

    public record SaveRequest(String status, String comment, String reason) {
    }

    public record SaveResponse(Long id, String status, CommentResponse comment) {
    }

    public record CommentResponse(Long id, String content, String createdAt) {
    }
}
