package com.fixmap.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fixmap.dto.Building;
import com.fixmap.dto.BuildingFloor;
import com.fixmap.util.DBUtil;

/** 건물·층 조회 */
public class BuildingDao {

    private static final String FIND_ALL_WITH_FLOORS = """
            SELECT b.id AS building_id, b.code, b.name AS building_name, b.sort_order,
                   f.id AS floor_id, f.floor_level, f.name AS floor_name,
                   f.map_image_url, f.map_width, f.map_height
            FROM building b
            LEFT JOIN building_floor f ON f.building_id = b.id
            ORDER BY b.sort_order, f.floor_level
            """;

    /** 건물 목록과 각 건물의 층 목록 (GET /api/buildings) */
    public List<Building> findAllWithFloors() throws SQLException {
        Map<Long, Building> buildings = new LinkedHashMap<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_ALL_WITH_FLOORS);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                long buildingId = rs.getLong("building_id");
                Building building = buildings.get(buildingId);
                if (building == null) {
                    building = new Building();
                    building.setId(buildingId);
                    building.setCode(rs.getString("code"));
                    building.setName(rs.getString("building_name"));
                    building.setSortOrder(rs.getInt("sort_order"));
                    building.setFloors(new ArrayList<>());
                    buildings.put(buildingId, building);
                }
                long floorId = rs.getLong("floor_id");
                if (rs.wasNull()) {
                    continue;
                }
                BuildingFloor floor = new BuildingFloor();
                floor.setId(floorId);
                floor.setBuildingId(buildingId);
                floor.setFloorLevel(rs.getInt("floor_level"));
                floor.setName(rs.getString("floor_name"));
                floor.setMapImageUrl(rs.getString("map_image_url"));
                floor.setMapWidth(rs.getObject("map_width") == null ? null : rs.getInt("map_width"));
                floor.setMapHeight(rs.getObject("map_height") == null ? null : rs.getInt("map_height"));
                building.getFloors().add(floor);
            }
        }
        return new ArrayList<>(buildings.values());
    }
}
