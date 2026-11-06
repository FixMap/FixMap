package com.fixmap.controller.api;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.StringJoiner;

import com.fixmap.dao.BuildingDao;
import com.fixmap.dto.Building;
import com.fixmap.dto.BuildingFloor;
import com.fixmap.util.JsonUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** GET /api/buildings : 건물·층 목록과 배치도 경로 */
@WebServlet("/api/buildings")
public class BuildingApiController extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final transient BuildingDao buildingDao = new BuildingDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            List<Building> buildings = buildingDao.findAllWithFloors();
            StringJoiner arr = new StringJoiner(",", "[", "]");
            for (Building b : buildings) {
                StringJoiner floors = new StringJoiner(",", "[", "]");
                for (BuildingFloor f : b.getFloors()) {
                    floors.add("{\"id\":" + f.getId()
                            + ",\"level\":" + f.getFloorLevel()
                            + ",\"name\":" + JsonUtil.quote(f.getName())
                            + ",\"mapImageUrl\":" + JsonUtil.quote(req.getContextPath() + f.getMapImageUrl())
                            + ",\"mapWidth\":" + f.getMapWidth()
                            + ",\"mapHeight\":" + f.getMapHeight() + "}");
                }
                arr.add("{\"id\":" + b.getId()
                        + ",\"code\":" + JsonUtil.quote(b.getCode())
                        + ",\"name\":" + JsonUtil.quote(b.getName())
                        + ",\"floors\":" + floors + "}");
            }
            JsonUtil.write(resp, HttpServletResponse.SC_OK, arr.toString());
        } catch (SQLException e) {
            log("건물 목록 조회 실패", e);
            JsonUtil.write(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "{\"message\":\"DB 오류\"}");
        }
    }
}
