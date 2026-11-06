package com.fixmap.controller.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fixmap.dao.BuildingDao;
import com.fixmap.dto.Building;

@RestController
@RequestMapping("/api/buildings")
public class BuildingApiController {

    private final BuildingDao buildingDao;

    public BuildingApiController(BuildingDao buildingDao) {
        this.buildingDao = buildingDao;
    }

    /** GET /api/buildings : 건물·층 목록과 배치도 경로 */
    @GetMapping
    public List<Building> list() {
        return buildingDao.findAllWithFloors();
    }
}
