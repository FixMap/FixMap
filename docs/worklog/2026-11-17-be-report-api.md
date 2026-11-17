# 신고 등록·첨부 파일·층의 신고 목록 API (BE)

- 날짜: 2026-11-17
- 작성: Claude, 요청한 사람: user
- 영역: be (db/data.sql 포함)
- 기능 ID: REPAIR-1, REPAIR-2, REPAIR-7("이 층의 신고" 목록)
- 브랜치·PR: `feature/be-report` (PR #11 `feature/map-api` 위에 쌓음). PR 번호는 PR 본문 참고

## 한 일

- 신고 등록 API: 필수 항목(층, 고장 종류, 제목, 내용, 핀 좌표)이 비면 400, 처리 상태는 항상 `RECEIVED`(접수)로 시작
- 첨부 파일 업로드·보기 API: 신고 1건에 3개까지, 사진 형식만, 디스크에 UUID 이름으로 저장하고 DB(`report_file`)에는 정보만
- 고장 종류 목록 API, 층의 신고 목록 API(작성 항목 전부 + 처리 상태 + 첨부 파일)
- 등록된 신고는 같은 `report` 테이블을 쓰므로 지도 핀(`/api/reports/map`, PR #11)에 바로 나온다

## 바꾼 파일

- `domain/Report.java`: 나머지 컬럼(place_id, category_id, content, created_at, updated_at) 매핑, 등록용 `create()`
- `domain/Category.java`, `domain/ReportFile.java`: 새 엔티티 (schema.sql과 같은 이름)
- `repository/CategoryRepository`, `ReportFileRepository`, `ReportRepository`(최신순 조회 추가)
- `service/ReportService.java`, `service/ReportFileStorage.java`: 등록·검증, 파일 저장
- `controller/ReportApiController.java`: API 5개
- `controller/ApiExceptionHandler.java`: 검증 실패, JSON 형식 오류, 파일 누락, 파일 크기 초과를 `{"message"}`로 응답
- `db/data.sql`: 고장 종류를 제안된 7개로 교체, 예시 신고의 category_id를 새 종류에 맞춤, 예시 첨부 경로를 상대 경로로
- `db.properties.example`: `upload.dir` 설명 추가

## 맞춘 문서

- repo: `docs/URL_PLAN.md`에 지정된 주소만 사용(`/api/categories`, `/api/reports`, `/api/reports/{id}/files`, `/api/files/{id}`). 새 주소 없음
- Notion: API 명세서에 요청·응답을 아직 적지 못했다(이 도구로 Notion에 접근하지 못함). PR 본문의 형식을 옮겨 확정해야 한다

## 정한 것과 미정

- 정한 것(요청한 사람과): BE만 구현, PR #11 위에 쌓기, 고장 종류 7개를 data.sql에 넣기, 저장 위치는 `upload.dir` 설정
- 제안(팀 확인 필요): API 요청·응답 형식, 사진 형식만 받기(jpg/png/gif/webp), 신고 등록과 첨부를 두 번의 요청으로 나눈 것
- 미정: 고장 종류 확정, 첨부 파일 저장 위치(운영), "처리 중" 상태, 사용자 쪽 신고 상세 주소(`/api/reports/{id}`는 목록에 없어 만들지 않음)

## 확인한 방법

- `mvn compile` 통과
- Oracle에 올려 실제 요청을 보내는 확인은 하지 못했다(로컬에 db.properties와 12c 이상 Oracle이 없음). Tomcat에서 직접 확인 필요

## 남은 일, 다음에 할 일

- 관리자 목록·상세 API(REPAIR-3, `/api/admin/reports`)는 이 PR에 없다. 등록한 신고는 같은 테이블이라 그 API가 생기면 바로 나온다
- 첨부만 올리고 등록이 실패하는 경우는 없지만(등록 → 첨부 순서), 등록 후 첨부가 실패하면 신고는 파일 없이 남는다

## 요청 (다른 영역이나 담당자에게)

- FE: 신고 작성 영역에서 `POST /api/reports`(JSON) 후 받은 id로 `POST /api/reports/{id}/files`(multipart, 필드 `files`)를 부른다. 오류는 `message`를 그대로 보여 주면 된다
- 팀: Notion API 명세서, 기능 명세서(고장 종류), ERD(category 데이터) 반영

## 정정 (2026-11-17)

- PR #11이 먼저 병합되면서 "이 층의 신고" 목록은 `GET /api/reports/map?level={층 번호}`(MapApiController)가 맡게 됐고, `GET /api/reports?floor=`는 `docs/URL_PLAN.md`에서 빠졌다. 그래서 이 PR에서 그 API를 지웠다.
- develop을 합치며 충돌을 풀었다: `Category`, `CategoryRepository`는 이 PR 쪽(정렬 조회 포함), `Report`는 양쪽 컬럼을 모두 유지, `ReportRepository`는 develop의 `findByFloorIdInOrderByIdAsc`만 남김.
- 남은 문제: 사용자 쪽에서 신고 내용·첨부까지 보는 API가 URL 목록에 없다. `/api/reports/map` 응답에 내용·영역·첨부를 넣을지, 상세 주소를 새로 둘지 팀이 정해야 한다.
- #14 리뷰 후 요청을 받아 `Report.changeStatus(String)`를 추가했다. #14의 후속 작업에서 `ReportStatusRepository`의 일괄 update 대신 쓰면 `updated_at`이 갱신된다. `ApiExceptionHandler`의 중복 핸들러는 #14에서 빼기로 해서 이 PR의 것을 유지한다.

## 정정 2 (2026-11-17, #13 리뷰 반영)

- Notion API 명세서에 맞춰 신고 등록을 `POST /api/reports` multipart 한 번(글 항목 + `files` 0~3개)으로 바꾸고 `POST /api/reports/{id}/files`는 지웠다. 등록 후 첨부만 실패해 신고가 파일 없이 남는 문제도 없어졌다.
- 영역(`place_id`)은 요청에서 받지 않고 서버가 핀 좌표로 정한다(ERD, Notion). 여러 영역에 걸치면 가장 작은 영역.
- `createdAt`을 배열이 아닌 `yyyy-MM-dd'T'HH:mm:ss` 문자열로 보낸다(#14와 같은 형식).
- 첨부 형식은 브라우저가 보낸 Content-Type이 아니라 파일 앞부분(매직 바이트)으로 판단한다.
- `@RequestMapping(consumes)`를 빼서 multipart가 아닌 요청도 Tomcat HTML이 아닌 400 `{"message"}`로 응답한다.
- `docs/URL_PLAN.md`의 사용자 API 네 줄 상태를 고쳤다.
- 확인: H2(Oracle 모드) + 내장 Tomcat 10.1 + UTF-8 필터로 HTTP 호출. 정상 등록(파일 0·1·2개, 한글), 영역 자동 지정(교실·복도·없음), 필수 누락·좌표 범위·숫자 아닌 값·없는 층·없는 종류·HTML을 담은 jpg·파일 4개·JSON/본문 없음/text/plain 요청은 400 `{"message"}`, 파일 보기 200(`nosniff`), 없는 파일 404. Oracle에서는 여전히 확인하지 못했다.
- `data.sql`의 IDENTITY 시작값은 이미 끝부분(`START WITH LIMIT VALUE`, Oracle 문법)에서 올리고 있다. H2는 이 문법을 지원하지 않아 리뷰 환경에서 재현된 것으로 보인다.
- 팀 결정이 필요해 이 PR에서 하지 않은 것: 글 종류(고장 신고/문의) 필드(DB 변경), 사용자 쪽 신고 상세 조회 API, `/api/files/{id}` 관리자 전용 제한(접근 제한 방식 미정).
