package com.fixmap.domain;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 교실·시설 영역. x, y, width, height는 층 기준 0~1 비율이다. */
@Entity
@Table(name = "place")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "floor_id", nullable = false)
    private Long floorId;

    @Column(nullable = false, length = 50)
    private String name;

    /** CLASSROOM, SPECIAL, TOILET, HALL, STAIRS, OFFICE, ETC */
    @Column(name = "place_type", nullable = false, length = 20)
    private String placeType;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal x;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal y;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal width;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal height;
}
