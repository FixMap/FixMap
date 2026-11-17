package com.fixmap.controller;

import java.time.format.DateTimeFormatter;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
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

    /** DELETE /api/admin/reports/{id} : 완료된 신고 삭제(답글, 이력, 첨부 파일 포함). 성공하면 204 */
    @DeleteMapping("/api/admin/reports/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReport(@PathVariable("id") Long id) {
        adminReportService.deleteReport(id);
    }

    /** PATCH /api/admin/reports/{id} : 처리 상태 변경과 답글 작성을 한 번에 저장 */
    @PatchMapping("/api/admin/reports/{id}")
    public SaveResponse save(@PathVariable("id") Long id, @RequestBody SaveRequest request) {
        AdminReportService.SaveResult result =
                adminReportService.save(id, request.status(), request.comment(), request.reason());
        return new SaveResponse(result.reportId(), result.status(), toResponse(result.comment()));
    }

    /** PATCH /api/admin/reports/{id}/comments/{commentId} : 답글 내용 수정 */
    @PatchMapping("/api/admin/reports/{id}/comments/{commentId}")
    public CommentResponse editComment(@PathVariable("id") Long id, @PathVariable("commentId") Long commentId,
                                       @RequestBody EditCommentRequest request) {
        return toResponse(adminReportService.editComment(id, commentId, request.content()));
    }

    /** DELETE /api/admin/reports/{id}/comments/{commentId} : 답글 삭제. 성공하면 204 */
    @DeleteMapping("/api/admin/reports/{id}/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable("id") Long id, @PathVariable("commentId") Long commentId) {
        adminReportService.deleteComment(id, commentId);
    }

    private static CommentResponse toResponse(ReportComment comment) {
        if (comment == null) {
            return null;
        }
        return new CommentResponse(comment.getId(), comment.getContent(), comment.getCreatedAt().format(TIME));
    }

    public record SaveRequest(String status, String comment, String reason) {
    }

    public record EditCommentRequest(String content) {
    }

    public record SaveResponse(Long id, String status, CommentResponse comment) {
    }

    public record CommentResponse(Long id, String content, String createdAt) {
    }
}
