package com.fixmap.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fixmap.domain.ReportComment;

public interface ReportCommentRepository extends JpaRepository<ReportComment, Long> {

    List<ReportComment> findByReportIdOrderByIdAsc(Long reportId);

    void deleteByReportId(Long reportId);
}
