package com.fixmap.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 신고. 컬럼 이름은 db/schema.sql의 report 테이블과 같다.
 * reporter_name(선택 입력)은 아직 쓰는 기능이 없어 매핑하지 않았다.
 */
@Entity
@Table(name = "report")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report {

    /** 등록할 때의 처리 상태(접수) */
    public static final String STATUS_RECEIVED = "RECEIVED";
    public static final String STATUS_DONE = "DONE";
    public static final String STATUS_HOLD = "HOLD";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "floor_id", nullable = false)
    private Long floorId;

    /** 핀이 들어간 교실 영역. 복도처럼 영역 밖이면 null */
    @Column(name = "place_id")
    private Long placeId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 4000)
    private String content;

    /** 핀 좌표. 층 기준 0~1 비율 */
    @Column(name = "pos_x", nullable = false, precision = 5, scale = 4)
    private BigDecimal posX;

    @Column(name = "pos_y", nullable = false, precision = 5, scale = 4)
    private BigDecimal posY;

    /** RECEIVED(접수), DONE(완료), HOLD(보류) */
    @Column(nullable = false, length = 10)
    private String status;

    /** 관리자가 상세를 열었으면 true. 안 읽은 신고를 목록에서 진하게 보여 주는 데 쓴다. */
    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** 새 신고. 처리 상태는 항상 '접수'로 시작한다. */
    public static Report create(Long floorId, Long placeId, Long categoryId, String title, String content,
                                BigDecimal posX, BigDecimal posY) {
        Report report = new Report();
        report.floorId = floorId;
        report.placeId = placeId;
        report.categoryId = categoryId;
        report.title = title;
        report.content = content;
        report.posX = posX;
        report.posY = posY;
        report.status = STATUS_RECEIVED;
        return report;
    }

    /**
     * 처리 상태를 바꾼다. 값 검사는 부르는 쪽(서비스)이 한다.
     * 트랜잭션 안에서 조회한 엔티티에 부르면 변경 감지로 저장되고 updated_at은 @PreUpdate가 갱신한다.
     *
     * @return 상태가 실제로 바뀌었으면 true
     */
    public boolean changeStatus(String newStatus) {
        if (newStatus == null || newStatus.equals(status)) {
            return false;
        }
        status = newStatus;
        return true;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
