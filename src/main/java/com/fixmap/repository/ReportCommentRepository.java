package com.fixmap.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fixmap.domain.ReportComment;

public interface ReportCommentRepository extends JpaRepository<ReportComment, Long> {
}
