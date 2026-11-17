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
