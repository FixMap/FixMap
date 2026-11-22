package com.fixmap.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fixmap.domain.StatusHistory;

public interface StatusHistoryRepository extends JpaRepository<StatusHistory, Long> {

    void deleteByReportId(Long reportId);
}
