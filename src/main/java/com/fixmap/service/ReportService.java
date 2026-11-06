package com.fixmap.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fixmap.dao.ReportDao;
import com.fixmap.dto.Report;
import com.fixmap.dto.ReportPin;

/** 신고 업무 로직. 여러 DAO 호출을 한 트랜잭션으로 묶을 때 여기서 처리한다. */
@Service
@Transactional(readOnly = true)
public class ReportService {

    private final ReportDao reportDao;

    public ReportService(ReportDao reportDao) {
        this.reportDao = reportDao;
    }

    public List<ReportPin> findPins(long floorId) {
        return reportDao.findPinsByFloor(floorId);
    }

    /** 신고 등록. 핀 좌표가 교실 영역 안이면 place_id를 자동으로 채운다. */
    @Transactional
    public long create(Report report) {
        if (report.getPosX() < 0 || report.getPosX() > 1 || report.getPosY() < 0 || report.getPosY() > 1) {
            throw new IllegalArgumentException("핀 좌표는 0~1 사이여야 합니다.");
        }
        report.setPlaceId(reportDao.findPlaceAt(report.getFloorId(), report.getPosX(), report.getPosY()));
        return reportDao.insert(report);
    }
}
