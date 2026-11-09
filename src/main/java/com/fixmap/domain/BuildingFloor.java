package com.fixmap.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 층. 배치도 크기(map_width, map_height)는 화면 비율을 유지할 때 쓴다. */
@Entity
@Table(name = "building_floor")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BuildingFloor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "building_id", nullable = false)
    private Long buildingId;

    @Column(name = "floor_level", nullable = false)
    private int floorLevel;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(name = "map_width")
    private Integer mapWidth;

    @Column(name = "map_height")
    private Integer mapHeight;
}
