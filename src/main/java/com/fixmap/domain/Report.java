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

/**
 * 신고. 지도 핀 조회에 필요한 컬럼만 매핑했다.
 * 신고 등록 기능을 만들 때 나머지 컬럼(content, category_id, place_id 등)을 추가한다.
 */
@Entity
@Table(name = "report")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "floor_id", nullable = false)
    private Long floorId;

    @Column(nullable = false, length = 100)
    private String title;

    /** 핀 좌표. 층 기준 0~1 비율 */
    @Column(name = "pos_x", nullable = false, precision = 5, scale = 4)
    private BigDecimal posX;

    @Column(name = "pos_y", nullable = false, precision = 5, scale = 4)
    private BigDecimal posY;

    /** RECEIVED(접수), DONE(완료), HOLD(보류) */
    @Column(nullable = false, length = 10)
    private String status;
}
