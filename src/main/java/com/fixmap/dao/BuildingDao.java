package com.fixmap.dao;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.fixmap.dto.Building;
import com.fixmap.dto.BuildingFloor;

/** 건물·층 조회 */
@Repository
public class BuildingDao {

    private static final String FIND_ALL_WITH_FLOORS = """
            SELECT b.id AS building_id, b.code, b.name AS building_name, b.sort_order,
                   f.id AS floor_id, f.floor_level, f.name AS floor_name,
                   f.map_image_url, f.map_width, f.map_height
            FROM building b
            LEFT JOIN building_floor f ON f.building_id = b.id
            ORDER BY b.sort_order, f.floor_level
            """;

    private final JdbcTemplate jdbc;

    public BuildingDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 건물 목록과 각 건물의 층 목록 */
    public List<Building> findAllWithFloors() {
        Map<Long, Building> buildings = new LinkedHashMap<>();
        jdbc.query(FIND_ALL_WITH_FLOORS, rs -> {
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
                return;
            }
            BuildingFloor floor = new BuildingFloor();
            floor.setId(floorId);
            floor.setBuildingId(buildingId);
            floor.setFloorLevel(rs.getInt("floor_level"));
            floor.setName(rs.getString("floor_name"));
            floor.setMapImageUrl(rs.getString("map_image_url"));
            floor.setMapWidth(rs.getObject("map_width", Integer.class));
            floor.setMapHeight(rs.getObject("map_height", Integer.class));
            building.getFloors().add(floor);
        });
        return new ArrayList<>(buildings.values());
    }
}
