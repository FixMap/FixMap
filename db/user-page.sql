-- 공식 학교 배치도 등록. schema.sql 실행 후 이 파일을 실행합니다.

-- 기존 임시 건물·층·신고를 수정하거나 삭제하지 않습니다. 재실행해도 중복되지 않습니다.

MERGE INTO building b
USING (SELECT 'CAMPUS' AS code, '교내 전체' AS name, -4 AS sort_order FROM dual) s
ON (b.code=s.code)
WHEN NOT MATCHED THEN INSERT (code,name,sort_order) VALUES (s.code,s.name,s.sort_order);

MERGE INTO building_floor f
USING (SELECT id AS building_id, 0 AS floor_level FROM building WHERE code='CAMPUS') s
ON (f.building_id=s.building_id AND f.floor_level=s.floor_level)
WHEN NOT MATCHED THEN INSERT (building_id,floor_level,name,map_image_url,map_width,map_height)
VALUES (s.building_id,s.floor_level,'교내','/static/maps/CAMPUS-0.png',1060,1015);

MERGE INTO building b
USING (SELECT 'MAIN' AS code, '본관·실습동' AS name, -3 AS sort_order FROM dual) s
ON (b.code=s.code)
WHEN NOT MATCHED THEN INSERT (code,name,sort_order) VALUES (s.code,s.name,s.sort_order);

MERGE INTO building_floor f
USING (SELECT id AS building_id, 1 AS floor_level FROM building WHERE code='MAIN') s
ON (f.building_id=s.building_id AND f.floor_level=s.floor_level)
WHEN NOT MATCHED THEN INSERT (building_id,floor_level,name,map_image_url,map_width,map_height)
VALUES (s.building_id,s.floor_level,'1층','/static/maps/MAIN-1.png',1000,310);

MERGE INTO building_floor f
USING (SELECT id AS building_id, 2 AS floor_level FROM building WHERE code='MAIN') s
ON (f.building_id=s.building_id AND f.floor_level=s.floor_level)
WHEN NOT MATCHED THEN INSERT (building_id,floor_level,name,map_image_url,map_width,map_height)
VALUES (s.building_id,s.floor_level,'2층','/static/maps/MAIN-2.png',1000,260);

MERGE INTO building_floor f
USING (SELECT id AS building_id, 3 AS floor_level FROM building WHERE code='MAIN') s
ON (f.building_id=s.building_id AND f.floor_level=s.floor_level)
WHEN NOT MATCHED THEN INSERT (building_id,floor_level,name,map_image_url,map_width,map_height)
VALUES (s.building_id,s.floor_level,'3층','/static/maps/MAIN-3.png',1000,245);

MERGE INTO building_floor f
USING (SELECT id AS building_id, 4 AS floor_level FROM building WHERE code='MAIN') s
ON (f.building_id=s.building_id AND f.floor_level=s.floor_level)
WHEN NOT MATCHED THEN INSERT (building_id,floor_level,name,map_image_url,map_width,map_height)
VALUES (s.building_id,s.floor_level,'4층','/static/maps/MAIN-4.png',1000,225);

MERGE INTO building b
USING (SELECT 'GEUMBONG' AS code, '금봉관' AS name, -2 AS sort_order FROM dual) s
ON (b.code=s.code)
WHEN NOT MATCHED THEN INSERT (code,name,sort_order) VALUES (s.code,s.name,s.sort_order);

MERGE INTO building_floor f
USING (SELECT id AS building_id, 1 AS floor_level FROM building WHERE code='GEUMBONG') s
ON (f.building_id=s.building_id AND f.floor_level=s.floor_level)
WHEN NOT MATCHED THEN INSERT (building_id,floor_level,name,map_image_url,map_width,map_height)
VALUES (s.building_id,s.floor_level,'1층','/static/maps/GEUMBONG-1.png',730,305);

MERGE INTO building_floor f
USING (SELECT id AS building_id, 2 AS floor_level FROM building WHERE code='GEUMBONG') s
ON (f.building_id=s.building_id AND f.floor_level=s.floor_level)
WHEN NOT MATCHED THEN INSERT (building_id,floor_level,name,map_image_url,map_width,map_height)
VALUES (s.building_id,s.floor_level,'2층','/static/maps/GEUMBONG-2.png',730,290);

MERGE INTO building_floor f
USING (SELECT id AS building_id, 3 AS floor_level FROM building WHERE code='GEUMBONG') s
ON (f.building_id=s.building_id AND f.floor_level=s.floor_level)
WHEN NOT MATCHED THEN INSERT (building_id,floor_level,name,map_image_url,map_width,map_height)
VALUES (s.building_id,s.floor_level,'3층','/static/maps/GEUMBONG-3.png',730,250);

MERGE INTO building_floor f
USING (SELECT id AS building_id, 4 AS floor_level FROM building WHERE code='GEUMBONG') s
ON (f.building_id=s.building_id AND f.floor_level=s.floor_level)
WHEN NOT MATCHED THEN INSERT (building_id,floor_level,name,map_image_url,map_width,map_height)
VALUES (s.building_id,s.floor_level,'4층','/static/maps/GEUMBONG-4.png',730,260);

MERGE INTO building b
USING (SELECT 'DONGHAENG' AS code, '동행관' AS name, -1 AS sort_order FROM dual) s
ON (b.code=s.code)
WHEN NOT MATCHED THEN INSERT (code,name,sort_order) VALUES (s.code,s.name,s.sort_order);

MERGE INTO building_floor f
USING (SELECT id AS building_id, 1 AS floor_level FROM building WHERE code='DONGHAENG') s
ON (f.building_id=s.building_id AND f.floor_level=s.floor_level)
WHEN NOT MATCHED THEN INSERT (building_id,floor_level,name,map_image_url,map_width,map_height)
VALUES (s.building_id,s.floor_level,'1층','/static/maps/DONGHAENG-1.png',1030,220);

MERGE INTO building_floor f
USING (SELECT id AS building_id, 2 AS floor_level FROM building WHERE code='DONGHAENG') s
ON (f.building_id=s.building_id AND f.floor_level=s.floor_level)
WHEN NOT MATCHED THEN INSERT (building_id,floor_level,name,map_image_url,map_width,map_height)
VALUES (s.building_id,s.floor_level,'2층','/static/maps/DONGHAENG-2.png',1030,220);

MERGE INTO building_floor f
USING (SELECT id AS building_id, 3 AS floor_level FROM building WHERE code='DONGHAENG') s
ON (f.building_id=s.building_id AND f.floor_level=s.floor_level)
WHEN NOT MATCHED THEN INSERT (building_id,floor_level,name,map_image_url,map_width,map_height)
VALUES (s.building_id,s.floor_level,'3층','/static/maps/DONGHAENG-3.png',1030,220);

MERGE INTO building_floor f
USING (SELECT id AS building_id, 4 AS floor_level FROM building WHERE code='DONGHAENG') s
ON (f.building_id=s.building_id AND f.floor_level=s.floor_level)
WHEN NOT MATCHED THEN INSERT (building_id,floor_level,name,map_image_url,map_width,map_height)
VALUES (s.building_id,s.floor_level,'4층','/static/maps/DONGHAENG-4.png',1030,215);

MERGE INTO building_floor f
USING (SELECT id AS building_id, 5 AS floor_level FROM building WHERE code='DONGHAENG') s
ON (f.building_id=s.building_id AND f.floor_level=s.floor_level)
WHEN NOT MATCHED THEN INSERT (building_id,floor_level,name,map_image_url,map_width,map_height)
VALUES (s.building_id,s.floor_level,'5층','/static/maps/DONGHAENG-5.png',1030,220);

MERGE INTO category c
USING (SELECT '시설 고장' AS name FROM dual) s ON (c.name=s.name)
WHEN NOT MATCHED THEN INSERT (name,sort_order) VALUES (s.name,1);

MERGE INTO category c
USING (SELECT '전기' AS name FROM dual) s ON (c.name=s.name)
WHEN NOT MATCHED THEN INSERT (name,sort_order) VALUES (s.name,2);

MERGE INTO category c
USING (SELECT '냉난방' AS name FROM dual) s ON (c.name=s.name)
WHEN NOT MATCHED THEN INSERT (name,sort_order) VALUES (s.name,3);

MERGE INTO category c
USING (SELECT '수도·화장실' AS name FROM dual) s ON (c.name=s.name)
WHEN NOT MATCHED THEN INSERT (name,sort_order) VALUES (s.name,4);

MERGE INTO category c
USING (SELECT '청결' AS name FROM dual) s ON (c.name=s.name)
WHEN NOT MATCHED THEN INSERT (name,sort_order) VALUES (s.name,5);

MERGE INTO category c
USING (SELECT '기타' AS name FROM dual) s ON (c.name=s.name)
WHEN NOT MATCHED THEN INSERT (name,sort_order) VALUES (s.name,6);

COMMIT;
