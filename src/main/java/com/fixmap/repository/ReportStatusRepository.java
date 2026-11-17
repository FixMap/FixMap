package com.fixmap.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.fixmap.domain.Report;

/**
 * 신고의 처리 상태만 바꾸는 리포지토리. 공유 파일인 Report, ReportRepository를 고치지 않으려고
 * 따로 두었다. 신고 등록 기능이 병합된 뒤 필요하면 ReportRepository로 합친다.
 */
public interface ReportStatusRepository extends JpaRepository<Report, Long> {

    @Modifying
    @Query("update Report r set r.status = :status where r.id = :id")
    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
