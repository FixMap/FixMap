package com.fixmap.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.fixmap.domain.Report;

public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findByFloorIdInOrderByIdAsc(Collection<Long> floorIds);

    /** 읽음으로 바꾼다. 내용이 바뀐 것이 아니라서 updated_at은 건드리지 않는다(@PreUpdate를 거치지 않는 일괄 update). */
    @Modifying
    @Query("update Report r set r.read = true where r.id = :id and r.read = false")
    int markRead(@Param("id") Long id);
}
