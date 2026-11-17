package com.fixmap.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fixmap.domain.ReportFile;

public interface ReportFileRepository extends JpaRepository<ReportFile, Long> {

    List<ReportFile> findByReportIdInOrderByIdAsc(Collection<Long> reportIds);

    long countByReportId(Long reportId);
}
