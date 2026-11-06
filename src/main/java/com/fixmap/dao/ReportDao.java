package com.fixmap.dao;

import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.fixmap.dto.Report;
import com.fixmap.dto.ReportPin;

/** 신고 테이블 접근. 검색·필터·상태 변경 등은 이 클래스에 이어서 추가한다. */
@Repository
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
            SELECT id, title, pos_x, pos_y, status
            FROM report
            WHERE floor_id = ?
            ORDER BY created_at DESC
            """;

    private final JdbcTemplate jdbc;

    public ReportDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 핀 좌표가 들어가는 교실 영역 id. 없으면(복도 등) null */
    public Long findPlaceAt(long floorId, double x, double y) {
        List<Long> ids = jdbc.queryForList(FIND_PLACE_AT, Long.class, floorId, x, y);
        return ids.isEmpty() ? null : ids.get(0);
    }

    /** 신고 등록 후 생성된 id를 돌려준다. */
    public long insert(Report r) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(INSERT, new String[] {"ID"});
            ps.setLong(1, r.getFloorId());
            if (r.getPlaceId() == null) {
                ps.setNull(2, Types.NUMERIC);
            } else {
                ps.setLong(2, r.getPlaceId());
            }
            ps.setLong(3, r.getCategoryId());
            ps.setString(4, r.getTitle());
            ps.setString(5, r.getContent());
            ps.setDouble(6, r.getPosX());
            ps.setDouble(7, r.getPosY());
            ps.setString(8, r.getReporterName());
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    /** 지도에 그릴 신고 핀 목록 */
    public List<ReportPin> findPinsByFloor(long floorId) {
        return jdbc.query(FIND_PINS_BY_FLOOR,
                (rs, i) -> new ReportPin(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getDouble("pos_x"),
                        rs.getDouble("pos_y"),
                        rs.getString("status")),
                floorId);
    }
}
