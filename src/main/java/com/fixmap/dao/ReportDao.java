package com.fixmap.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.fixmap.dto.Report;
import com.fixmap.util.DBUtil;

/** 신고 등록·조회. 검색·필터·상태 변경 등은 이 클래스에 이어서 추가한다. */
public class ReportDao {

    private static final String FIND_PLACE_AT = """
            SELECT id FROM place
            WHERE floor_id = ?
              AND ? BETWEEN x AND x + width
              AND ? BETWEEN y AND y + height
            FETCH FIRST 1 ROW ONLY
            """;

    private static final String INSERT = """
            INSERT INTO report (floor_id, place_id, category_id, title, content, pos_x, pos_y, reporter_name)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String FIND_PINS_BY_FLOOR = """
            SELECT id, floor_id, place_id, category_id, title, pos_x, pos_y, status, created_at
            FROM report
            WHERE floor_id = ?
            ORDER BY created_at DESC
            """;

    /** 신고 등록. 핀 좌표가 교실 영역 안이면 place_id를 자동으로 채운다. 생성된 id를 돌려준다. */
    public long insert(Report report) throws SQLException {
        try (Connection conn = DBUtil.getConnection()) {
            return insert(conn, report);
        }
    }

    /** 첨부 저장과 같은 트랜잭션을 사용하며 커밋은 호출자가 담당한다. */
    public long insert(Connection conn, Report report) throws SQLException {
        Long placeId = findPlaceAt(conn, report.getFloorId(), report.getPosX(), report.getPosY());
        try (PreparedStatement ps = conn.prepareStatement(INSERT, new String[] {"ID"})) {
            ps.setLong(1, report.getFloorId());
            if (placeId == null) {
                ps.setNull(2, java.sql.Types.NUMERIC);
            } else {
                ps.setLong(2, placeId);
            }
            ps.setLong(3, report.getCategoryId());
            ps.setString(4, report.getTitle());
            ps.setString(5, report.getContent());
            ps.setDouble(6, report.getPosX());
            ps.setDouble(7, report.getPosY());
            ps.setString(8, report.getReporterName());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    /** 지도에 그릴 신고 핀 목록 (GET /api/reports/map?floor=) */
    public List<Report> findPinsByFloor(long floorId) throws SQLException {
        List<Report> reports = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_PINS_BY_FLOOR)) {
            ps.setLong(1, floorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Report r = new Report();
                    r.setId(rs.getLong("id"));
                    r.setFloorId(rs.getLong("floor_id"));
                    long placeId = rs.getLong("place_id");
                    r.setPlaceId(rs.wasNull() ? null : placeId);
                    r.setCategoryId(rs.getLong("category_id"));
                    r.setTitle(rs.getString("title"));
                    r.setPosX(rs.getDouble("pos_x"));
                    r.setPosY(rs.getDouble("pos_y"));
                    r.setStatus(rs.getString("status"));
                    r.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    reports.add(r);
                }
            }
        }
        return reports;
    }

    private Long findPlaceAt(Connection conn, long floorId, double x, double y) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(FIND_PLACE_AT)) {
            ps.setLong(1, floorId);
            ps.setDouble(2, x);
            ps.setDouble(3, y);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong("id") : null;
            }
        }
    }
}
