package com.fixmap.dto;

/** 지도에 표시할 신고 핀 (GET /api/reports/map 응답) */
public record ReportPin(long id, String title, double posX, double posY, String status) {
}
