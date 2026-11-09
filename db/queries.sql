-- =========================================================
-- 화면별 확인용 조회 쿼리 (Oracle, API 구현 참고)
-- =========================================================

-- 1) 건물·층 목록 (GET /api/buildings)
SELECT b.id AS building_id, b.name AS building, f.id AS floor_id, f.name AS floor_name
FROM building b
JOIN building_floor f ON f.building_id = b.id
ORDER BY b.sort_order, f.floor_level;

-- 2) 지도에 그릴 신고 핀 (GET /api/reports/map?floor=2)
SELECT id, pos_x, pos_y, status, title
FROM report
WHERE floor_id = 2;

-- 3) 핀 좌표로 교실 찾기 (신고 등록 시 place_id 자동 지정)
SELECT id, name
FROM place
WHERE floor_id = 2
  AND 0.42 BETWEEN x AND x + width
  AND 0.20 BETWEEN y AND y + height
FETCH FIRST 1 ROW ONLY;

-- 4) 신고 목록: 검색 + 필터 + 정렬 + 페이지 (GET /api/reports)
SELECT r.id,
       r.title,
       b.name || ' ' || f.name || NVL2(p.name, ' ' || p.name, NULL) AS location,
       c.name AS category,
       r.status,
       r.created_at
FROM report r
JOIN building_floor f ON f.id = r.floor_id
JOIN building b       ON b.id = f.building_id
JOIN category c       ON c.id = r.category_id
LEFT JOIN place p     ON p.id = r.place_id
WHERE (r.title LIKE '%고장%' OR r.content LIKE '%고장%' OR p.name LIKE '%고장%')
  AND r.status IN ('RECEIVED', 'HOLD')
ORDER BY r.created_at DESC
OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY;

-- 5) 관리자 상단 요약: 상태별 개수
SELECT status, COUNT(*) AS cnt
FROM report
GROUP BY status;

-- 6) 신고 상세 + 첨부 + 답글
SELECT * FROM report         WHERE id = 3;
SELECT * FROM report_file    WHERE report_id = 3;
SELECT * FROM report_comment WHERE report_id = 3 ORDER BY created_at;

-- 7) 상태 변경 (report와 기록을 한 트랜잭션으로 변경)
INSERT INTO status_history (report_id, from_status, to_status, reason)
  SELECT id, status, 'DONE', NULL FROM report WHERE id = 1;
UPDATE report SET status = 'DONE' WHERE id = 1;
ROLLBACK; -- 확인용이므로 되돌림
