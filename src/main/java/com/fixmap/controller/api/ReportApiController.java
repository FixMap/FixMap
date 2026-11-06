package com.fixmap.controller.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fixmap.dto.ReportPin;
import com.fixmap.service.ReportService;

/** 신고 API. 등록·목록·상세는 이 클래스에 메서드로 추가한다. */
@RestController
@RequestMapping("/api/reports")
public class ReportApiController {

    private final ReportService reportService;

    public ReportApiController(ReportService reportService) {
        this.reportService = reportService;
    }

    /** GET /api/reports/map?floor={층 id} : 지도에 그릴 신고 핀 */
    @GetMapping("/map")
    public List<ReportPin> pins(@RequestParam("floor") long floorId) {
        return reportService.findPins(floorId);
    }
}
