package com.fixmap.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fixmap.domain.Report;

public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findByFloorIdOrderByIdAsc(Long floorId);
}
