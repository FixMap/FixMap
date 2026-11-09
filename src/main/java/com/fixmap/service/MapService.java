package com.fixmap.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fixmap.domain.Building;
import com.fixmap.domain.BuildingFloor;
import com.fixmap.domain.Place;
import com.fixmap.domain.Report;
import com.fixmap.repository.BuildingFloorRepository;
import com.fixmap.repository.BuildingRepository;
import com.fixmap.repository.PlaceRepository;
import com.fixmap.repository.ReportRepository;

import lombok.RequiredArgsConstructor;

/** 메인 지도 화면에 필요한 조회: 건물·층, 층의 영역, 층의 신고 핀 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MapService {

    private final BuildingRepository buildingRepository;
    private final BuildingFloorRepository floorRepository;
    private final PlaceRepository placeRepository;
    private final ReportRepository reportRepository;

    /** 건물 목록(탭 순서)과 각 건물의 층(층 번호 순서) */
    public List<BuildingWithFloors> findBuildingsWithFloors() {
        Map<Long, List<BuildingFloor>> floorsByBuilding = new LinkedHashMap<>();
        for (BuildingFloor floor : floorRepository.findAllByOrderByFloorLevelAsc()) {
            floorsByBuilding.computeIfAbsent(floor.getBuildingId(), id -> new ArrayList<>()).add(floor);
        }
        List<BuildingWithFloors> result = new ArrayList<>();
        for (Building building : buildingRepository.findAllByOrderBySortOrderAsc()) {
            result.add(new BuildingWithFloors(building, floorsByBuilding.getOrDefault(building.getId(), List.of())));
        }
        return result;
    }

    /** 층의 영역 목록. 없는 층이면 404 */
    public List<Place> findPlaces(Long floorId) {
        if (!floorRepository.existsById(floorId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "층을 찾을 수 없습니다");
        }
        return placeRepository.findByFloorIdOrderByIdAsc(floorId);
    }

    /** 층에 찍힌 신고 핀 목록. 신고가 없으면 빈 목록 */
    public List<Report> findPins(Long floorId) {
        return reportRepository.findByFloorIdOrderByIdAsc(floorId);
    }

    public record BuildingWithFloors(Building building, List<BuildingFloor> floors) {
    }
}
