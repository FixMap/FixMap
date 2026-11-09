package com.fixmap.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fixmap.service.MapService;

import lombok.RequiredArgsConstructor;

/**
 * 메인(학교 지도) 화면이 부르는 JSON API. 요청·응답 형식은 Notion 기능 명세서의
 * "메인(학교 지도) 페이지"에 적힌 API 표를 따른다.
 */
@RestController
@RequiredArgsConstructor
public class MapApiController {

    private final MapService mapService;

    /** GET /api/buildings : 건물과 층 목록 */
    @GetMapping("/api/buildings")
    public List<BuildingResponse> buildings() {
        return mapService.findBuildingsWithFloors().stream()
                .map(b -> new BuildingResponse(
                        b.building().getId(),
                        b.building().getCode(),
                        b.building().getName(),
                        b.floors().stream()
                                .map(f -> new FloorResponse(f.getId(), f.getFloorLevel(), f.getName(),
                                        f.getMapWidth(), f.getMapHeight()))
                                .toList()))
                .toList();
    }

    /** GET /api/reports/map?floor={층 id} : 층의 신고 핀 목록. floor가 없거나 숫자가 아니면 400 */
    @GetMapping("/api/reports/map")
    public List<PinResponse> pins(@RequestParam("floor") Long floorId) {
        return mapService.findPins(floorId).stream()
                .map(r -> new PinResponse(r.getId(), r.getTitle(), r.getPosX(), r.getPosY(), r.getStatus()))
                .toList();
    }

    /** GET /api/floors/{id}/places : 층의 영역 목록. 없는 층이면 404 */
    @GetMapping("/api/floors/{id}/places")
    public List<PlaceResponse> places(@PathVariable("id") Long floorId) {
        return mapService.findPlaces(floorId).stream()
                .map(p -> new PlaceResponse(p.getId(), p.getName(), p.getPlaceType(),
                        p.getX(), p.getY(), p.getWidth(), p.getHeight()))
                .toList();
    }

    public record BuildingResponse(Long id, String code, String name, List<FloorResponse> floors) {
    }

    public record FloorResponse(Long id, int level, String name, Integer mapWidth, Integer mapHeight) {
    }

    public record PinResponse(Long id, String title, BigDecimal posX, BigDecimal posY, String status) {
    }

    public record PlaceResponse(Long id, String name, String type,
                                BigDecimal x, BigDecimal y, BigDecimal width, BigDecimal height) {
    }
}
