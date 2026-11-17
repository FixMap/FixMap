package com.fixmap.service;

import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fixmap.domain.Report;
import com.fixmap.domain.ReportComment;
import com.fixmap.domain.StatusHistory;
import com.fixmap.repository.ReportCommentRepository;
import com.fixmap.repository.ReportFileRepository;
import com.fixmap.repository.ReportRepository;
import com.fixmap.repository.StatusHistoryRepository;

import lombok.RequiredArgsConstructor;

/** 관리자 처리 저장: 상태 변경과 답글 작성을 한 트랜잭션으로 처리한다. */
@Service
@RequiredArgsConstructor
public class AdminReportService {

    private static final Set<String> STATUSES = Set.of(Report.STATUS_RECEIVED, Report.STATUS_PROGRESS, Report.STATUS_DONE,
            Report.STATUS_HOLD);
    private static final int MAX_COMMENT = 2000;
    private static final int MAX_REASON = 255;

    private final ReportRepository reportRepository;
    private final ReportCommentRepository commentRepository;
    private final StatusHistoryRepository historyRepository;
    private final ReportFileRepository fileRepository;
    private final ReportFileStorage fileStorage;

    /**
     * @param status  바꿀 상태(없으면 null)
     * @param comment 답글(없거나 빈 문자열이면 null)
     * @param reason  상태를 바꾼 이유(없으면 null)
     */
    @Transactional
    public SaveResult save(Long reportId, String status, String comment, String reason) {
        String newComment = blankToNull(comment);
        String newReason = blankToNull(reason);
        String newStatus = blankToNull(status);

        if (newStatus == null && newComment == null) {
            throw badRequest("상태나 답글 중 하나는 필요합니다");
        }
        if (newStatus != null && !STATUSES.contains(newStatus)) {
            throw badRequest("status 값이 올바르지 않습니다");
        }
        if (newComment != null && newComment.length() > MAX_COMMENT) {
            throw badRequest("comment 값이 올바르지 않습니다");
        }
        if (newReason != null && newReason.length() > MAX_REASON) {
            throw badRequest("reason 값이 올바르지 않습니다");
        }

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "신고를 찾을 수 없습니다"));

        String currentStatus = report.getStatus();
        if (report.changeStatus(newStatus)) {
            historyRepository.save(new StatusHistory(reportId, currentStatus, newStatus, newReason));
            currentStatus = newStatus;
        }

        ReportComment saved = null;
        if (newComment != null) {
            saved = commentRepository.save(new ReportComment(reportId, newComment));
        }
        return new SaveResult(reportId, currentStatus, saved);
    }

    /**
     * 완료(DONE)된 신고를 지운다. 답글, 상태 변경 기록, 첨부 정보를 함께 지우고 저장된 첨부 파일도 지운다.
     * 없는 신고는 404, 완료 상태가 아니면 400. 지운 신고는 복구하지 않는다.
     */
    @Transactional
    public void deleteReport(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "신고를 찾을 수 없습니다"));
        if (!Report.STATUS_DONE.equals(report.getStatus())) {
            throw badRequest("완료된 신고만 삭제할 수 있습니다");
        }
        List<String> storedPaths = fileRepository.findByReportIdOrderByIdAsc(reportId).stream()
                .map(f -> f.getStoredPath())
                .toList();

        commentRepository.deleteByReportId(reportId);
        historyRepository.deleteByReportId(reportId);
        fileRepository.deleteByReportId(reportId);
        reportRepository.delete(report);

        // 파일은 DB 삭제가 확정된 뒤에 지운다(롤백되면 파일은 그대로 둔다).
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                storedPaths.forEach(fileStorage::deleteQuietly);
            }
        });
    }

    /** 답글 내용을 고친다. 신고나 답글이 없거나 그 신고의 답글이 아니면 404, 내용이 비었거나 2000자를 넘으면 400. */
    @Transactional
    public ReportComment editComment(Long reportId, Long commentId, String content) {
        String newContent = blankToNull(content);
        if (newContent == null || newContent.length() > MAX_COMMENT) {
            throw badRequest("content 값이 올바르지 않습니다");
        }
        ReportComment comment = findComment(reportId, commentId);
        comment.edit(newContent);
        return comment;
    }

    /** 답글을 지운다. 신고나 답글이 없거나 그 신고의 답글이 아니면 404. */
    @Transactional
    public void deleteComment(Long reportId, Long commentId) {
        commentRepository.delete(findComment(reportId, commentId));
    }

    private ReportComment findComment(Long reportId, Long commentId) {
        if (!reportRepository.existsById(reportId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "신고를 찾을 수 없습니다");
        }
        return commentRepository.findById(commentId)
                .filter(c -> c.getReportId().equals(reportId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "답글을 찾을 수 없습니다"));
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    public record SaveResult(Long reportId, String status, ReportComment comment) {
    }
}
