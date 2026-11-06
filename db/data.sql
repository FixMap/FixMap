-- =========================================================
-- 테스트 데이터 (Oracle, schema.sql 실행 후)
-- 건물·층·교실 배치는 임시 값입니다. 실제 배치도를 받으면 교체하세요.
-- =========================================================

-- 건물
INSERT INTO building (id, code, name, sort_order) VALUES (1, 'main', '본관', 1);
INSERT INTO building (id, code, name, sort_order) VALUES (2, 'annex', '별관', 2);
INSERT INTO building (id, code, name, sort_order) VALUES (3, 'gym', '체육관', 3);

-- 층
INSERT INTO building_floor (id, building_id, floor_level, name, map_image_url, map_width, map_height) VALUES (1, 1, 1, '1층', '/static/maps/main_1F.svg', 1200, 600);
INSERT INTO building_floor (id, building_id, floor_level, name, map_image_url, map_width, map_height) VALUES (2, 1, 2, '2층', '/static/maps/main_2F.svg', 1200, 600);
INSERT INTO building_floor (id, building_id, floor_level, name, map_image_url, map_width, map_height) VALUES (3, 1, 3, '3층', '/static/maps/main_3F.svg', 1200, 600);
INSERT INTO building_floor (id, building_id, floor_level, name, map_image_url, map_width, map_height) VALUES (4, 2, 1, '1층', '/static/maps/annex_1F.svg', 1000, 500);
INSERT INTO building_floor (id, building_id, floor_level, name, map_image_url, map_width, map_height) VALUES (5, 2, 2, '2층', '/static/maps/annex_2F.svg', 1000, 500);
INSERT INTO building_floor (id, building_id, floor_level, name, map_image_url, map_width, map_height) VALUES (6, 3, 1, '1층', '/static/maps/gym_1F.svg', 800, 500);

-- 교실·시설 영역 (0~1 비율)
INSERT INTO place (id, floor_id, name, place_type, x, y, width, height) VALUES (1, 1, '교무실', 'OFFICE', 0.05, 0.1, 0.25, 0.35);
INSERT INTO place (id, floor_id, name, place_type, x, y, width, height) VALUES (2, 1, '행정실', 'OFFICE', 0.3, 0.1, 0.15, 0.35);
INSERT INTO place (id, floor_id, name, place_type, x, y, width, height) VALUES (3, 1, '보건실', 'SPECIAL', 0.45, 0.1, 0.15, 0.35);
INSERT INTO place (id, floor_id, name, place_type, x, y, width, height) VALUES (4, 1, '1층 복도', 'HALL', 0.05, 0.45, 0.9, 0.1);
INSERT INTO place (id, floor_id, name, place_type, x, y, width, height) VALUES (5, 1, '1층 화장실', 'TOILET', 0.85, 0.1, 0.1, 0.35);
INSERT INTO place (id, floor_id, name, place_type, x, y, width, height) VALUES (6, 2, '2-1반', 'CLASSROOM', 0.05, 0.1, 0.15, 0.35);
INSERT INTO place (id, floor_id, name, place_type, x, y, width, height) VALUES (7, 2, '2-2반', 'CLASSROOM', 0.2, 0.1, 0.15, 0.35);
INSERT INTO place (id, floor_id, name, place_type, x, y, width, height) VALUES (8, 2, '2-3반', 'CLASSROOM', 0.35, 0.1, 0.15, 0.35);
INSERT INTO place (id, floor_id, name, place_type, x, y, width, height) VALUES (9, 2, '2-4반', 'CLASSROOM', 0.5, 0.1, 0.15, 0.35);
INSERT INTO place (id, floor_id, name, place_type, x, y, width, height) VALUES (10, 2, '2층 복도', 'HALL', 0.05, 0.45, 0.9, 0.1);
INSERT INTO place (id, floor_id, name, place_type, x, y, width, height) VALUES (11, 2, '2층 남자 화장실', 'TOILET', 0.85, 0.1, 0.1, 0.35);
INSERT INTO place (id, floor_id, name, place_type, x, y, width, height) VALUES (12, 2, '2층 계단', 'STAIRS', 0.7, 0.1, 0.1, 0.35);
INSERT INTO place (id, floor_id, name, place_type, x, y, width, height) VALUES (13, 3, '3-1반', 'CLASSROOM', 0.05, 0.1, 0.15, 0.35);
INSERT INTO place (id, floor_id, name, place_type, x, y, width, height) VALUES (14, 3, '3-2반', 'CLASSROOM', 0.2, 0.1, 0.15, 0.35);
INSERT INTO place (id, floor_id, name, place_type, x, y, width, height) VALUES (15, 3, '3층 복도', 'HALL', 0.05, 0.45, 0.9, 0.1);
INSERT INTO place (id, floor_id, name, place_type, x, y, width, height) VALUES (16, 4, '컴퓨터실 1', 'SPECIAL', 0.1, 0.1, 0.35, 0.4);
INSERT INTO place (id, floor_id, name, place_type, x, y, width, height) VALUES (17, 4, '과학실', 'SPECIAL', 0.55, 0.1, 0.35, 0.4);
INSERT INTO place (id, floor_id, name, place_type, x, y, width, height) VALUES (18, 5, '음악실', 'SPECIAL', 0.1, 0.1, 0.35, 0.4);
INSERT INTO place (id, floor_id, name, place_type, x, y, width, height) VALUES (19, 5, '도서관', 'SPECIAL', 0.55, 0.1, 0.35, 0.4);
INSERT INTO place (id, floor_id, name, place_type, x, y, width, height) VALUES (20, 6, '체육관 코트', 'SPECIAL', 0.1, 0.1, 0.8, 0.6);
INSERT INTO place (id, floor_id, name, place_type, x, y, width, height) VALUES (21, 6, '탈의실', 'ETC', 0.1, 0.75, 0.3, 0.2);

-- 신고 분류
INSERT INTO category (id, name, sort_order) VALUES (1, '시설 고장', 1);
INSERT INTO category (id, name, sort_order) VALUES (2, '전기', 2);
INSERT INTO category (id, name, sort_order) VALUES (3, '냉난방', 3);
INSERT INTO category (id, name, sort_order) VALUES (4, '수도·화장실', 4);
INSERT INTO category (id, name, sort_order) VALUES (5, '청결', 5);
INSERT INTO category (id, name, sort_order) VALUES (6, '기타', 6);

-- 신고 (pos_x, pos_y는 해당 place 영역 안에 위치)
INSERT INTO report (id, floor_id, place_id, category_id, title, content, pos_x, pos_y, status, reporter_name, created_at, updated_at) VALUES (1, 2, 8, 2, '2-3반 형광등 깜빡임', '교실 앞쪽 형광등 2개가 계속 깜빡입니다.', 0.42, 0.2, 'RECEIVED', '2학년 김학생', TIMESTAMP '2026-11-03 08:40:00', TIMESTAMP '2026-11-03 08:40:00');
INSERT INTO report (id, floor_id, place_id, category_id, title, content, pos_x, pos_y, status, reporter_name, created_at, updated_at) VALUES (2, 2, 10, 4, '복도 정수기 누수', '2층 복도 정수기 아래로 물이 새서 바닥이 젖어 있어요.', 0.6, 0.5, 'DONE', NULL, TIMESTAMP '2026-11-02 12:10:00', TIMESTAMP '2026-11-03 10:00:00');
INSERT INTO report (id, floor_id, place_id, category_id, title, content, pos_x, pos_y, status, reporter_name, created_at, updated_at) VALUES (3, 2, 6, 1, '창문 잠금장치 고장', '2-1반 뒤쪽 창문 잠금장치가 부러져서 잠기지 않습니다.', 0.1, 0.3, 'HOLD', '2학년 이학생', TIMESTAMP '2026-11-01 09:15:00', TIMESTAMP '2026-11-02 14:00:00');
INSERT INTO report (id, floor_id, place_id, category_id, title, content, pos_x, pos_y, status, reporter_name, created_at, updated_at) VALUES (4, 6, 20, 3, '체육관 에어컨 작동 안 함', '체육관 오른쪽 에어컨이 켜지지 않아요.', 0.8, 0.2, 'RECEIVED', NULL, TIMESTAMP '2026-11-04 15:30:00', TIMESTAMP '2026-11-04 15:30:00');
INSERT INTO report (id, floor_id, place_id, category_id, title, content, pos_x, pos_y, status, reporter_name, created_at, updated_at) VALUES (5, 1, 5, 4, '1층 화장실 변기 막힘', '1층 화장실 안쪽 칸 변기가 막혔습니다.', 0.9, 0.3, 'DONE', '1학년 박학생', TIMESTAMP '2026-10-30 11:00:00', TIMESTAMP '2026-10-30 16:20:00');
INSERT INTO report (id, floor_id, place_id, category_id, title, content, pos_x, pos_y, status, reporter_name, created_at, updated_at) VALUES (6, 4, 16, 1, '컴퓨터실 의자 바퀴 파손', '컴퓨터실 1의 12번 자리 의자 바퀴가 빠졌어요.', 0.25, 0.3, 'RECEIVED', NULL, TIMESTAMP '2026-11-05 13:05:00', TIMESTAMP '2026-11-05 13:05:00');
INSERT INTO report (id, floor_id, place_id, category_id, title, content, pos_x, pos_y, status, reporter_name, created_at, updated_at) VALUES (7, 3, NULL, 5, '3층 계단 옆 쓰레기', '3층 계단 옆 구석에 쓰레기가 쌓여 있습니다.', 0.92, 0.8, 'RECEIVED', '3학년 최학생', TIMESTAMP '2026-11-05 17:45:00', TIMESTAMP '2026-11-05 17:45:00');
INSERT INTO report (id, floor_id, place_id, category_id, title, content, pos_x, pos_y, status, reporter_name, created_at, updated_at) VALUES (8, 5, 19, 2, '도서관 콘센트 고장', '도서관 창가 쪽 콘센트에 전기가 안 들어와요.', 0.8, 0.25, 'HOLD', NULL, TIMESTAMP '2026-10-28 10:20:00', TIMESTAMP '2026-10-29 09:00:00');

-- 첨부 파일 (파일 자체는 /uploads 폴더에 있다고 가정)
INSERT INTO report_file (report_id, original_name, stored_path, content_type, file_size, created_at) VALUES (1, '형광등.jpg', '/uploads/2026/11/7f3c1e2a-0b6d-4c1e-9a51-1f2b3c4d5e01.jpg', 'image/jpeg', 1843200, TIMESTAMP '2026-11-03 08:40:00');
INSERT INTO report_file (report_id, original_name, stored_path, content_type, file_size, created_at) VALUES (2, '정수기누수.jpg', '/uploads/2026/11/0a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c02.jpg', 'image/jpeg', 2457600, TIMESTAMP '2026-11-02 12:10:00');
INSERT INTO report_file (report_id, original_name, stored_path, content_type, file_size, created_at) VALUES (2, '바닥.png', '/uploads/2026/11/1b2c3d4e-5f6a-4b7c-9d8e-0f1a2b3c4d03.png', 'image/png', 980000, TIMESTAMP '2026-11-02 12:10:00');
INSERT INTO report_file (report_id, original_name, stored_path, content_type, file_size, created_at) VALUES (3, 'window.jpg', '/uploads/2026/11/2c3d4e5f-6a7b-4c8d-8e9f-1a2b3c4d5e04.jpg', 'image/jpeg', 1520000, TIMESTAMP '2026-11-01 09:15:00');
INSERT INTO report_file (report_id, original_name, stored_path, content_type, file_size, created_at) VALUES (6, '의자.jpg', '/uploads/2026/11/3d4e5f6a-7b8c-4d9e-9f0a-2b3c4d5e6f05.jpg', 'image/jpeg', 1200000, TIMESTAMP '2026-11-05 13:05:00');

-- 관리자 답글
INSERT INTO report_comment (report_id, content, created_at, updated_at) VALUES (2, '정수기 업체에 연락해서 필터 연결부를 교체했습니다.', TIMESTAMP '2026-11-03 10:00:00', TIMESTAMP '2026-11-03 10:00:00');
INSERT INTO report_comment (report_id, content, created_at, updated_at) VALUES (3, '부품 주문 중이라 보류 처리했습니다. 다음 주 교체 예정입니다.', TIMESTAMP '2026-11-02 14:00:00', TIMESTAMP '2026-11-02 14:00:00');
INSERT INTO report_comment (report_id, content, created_at, updated_at) VALUES (5, '시설 담당 선생님이 확인 후 조치했습니다.', TIMESTAMP '2026-10-30 16:20:00', TIMESTAMP '2026-10-30 16:20:00');
INSERT INTO report_comment (report_id, content, created_at, updated_at) VALUES (8, '전기 공사가 필요해 방학 중 처리 예정입니다.', TIMESTAMP '2026-10-29 09:00:00', TIMESTAMP '2026-10-29 09:00:00');

-- 상태 변경 기록 (report.status와 맞춤)
INSERT INTO status_history (report_id, from_status, to_status, reason, changed_at) VALUES (2, 'RECEIVED', 'DONE', NULL, TIMESTAMP '2026-11-03 10:00:00');
INSERT INTO status_history (report_id, from_status, to_status, reason, changed_at) VALUES (3, 'RECEIVED', 'HOLD', '부품 주문 대기', TIMESTAMP '2026-11-02 14:00:00');
INSERT INTO status_history (report_id, from_status, to_status, reason, changed_at) VALUES (5, 'RECEIVED', 'DONE', NULL, TIMESTAMP '2026-10-30 16:20:00');
INSERT INTO status_history (report_id, from_status, to_status, reason, changed_at) VALUES (8, 'RECEIVED', 'HOLD', '전기 공사 필요 (방학 중)', TIMESTAMP '2026-10-29 09:00:00');

-- id를 직접 넣은 테이블은 IDENTITY 시작값을 현재 최대값 다음으로 맞춤
ALTER TABLE building       MODIFY id GENERATED BY DEFAULT AS IDENTITY (START WITH LIMIT VALUE);
ALTER TABLE building_floor MODIFY id GENERATED BY DEFAULT AS IDENTITY (START WITH LIMIT VALUE);
ALTER TABLE place          MODIFY id GENERATED BY DEFAULT AS IDENTITY (START WITH LIMIT VALUE);
ALTER TABLE category       MODIFY id GENERATED BY DEFAULT AS IDENTITY (START WITH LIMIT VALUE);
ALTER TABLE report         MODIFY id GENERATED BY DEFAULT AS IDENTITY (START WITH LIMIT VALUE);

COMMIT;
