package com.fixmap.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fixmap.domain.ReportFile;

public interface ReportFileRepository extends JpaRepository<ReportFile, Long> {

    List<ReportFile> findByReportIdOrderByIdAsc(Long reportId);
}
