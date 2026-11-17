# 처리 상태 "처리 중" 추가, 첨부 파일 최대 5개

- 날짜: 2026-11-17
- 작성: Claude, 요청한 사람은 팀원(소마고3)
- 영역: be / db
- 기능 ID: REPAIR-2, REPAIR-5
- 브랜치·PR: `feature/be-status-category-files`

## 한 일

- 처리 상태에 `PROGRESS`(처리 중)를 더해 접수·처리 중·보류·완료 4개로 맞췄다(시안 C-2의 상태 선택).
- 첨부 파일을 신고 1건에 최대 3개에서 5개로 늘렸다(시안 기준). 요청 크기 한도도 파일 5개에 맞춰 55MB로 올렸다.
- 고장 종류 7개는 `db/data.sql`에 이미 들어 있어 바꾸지 않았다.

## 바꾼 파일

- `domain/Report.java`: `STATUS_PROGRESS` 추가
- `service/AdminReportService.java`: 허용 상태에 `PROGRESS` 추가
- `service/ReportService.java`, `controller/ReportApiController.java`: 첨부 최대 5개
- `config/WebAppInitializer.java`: 요청 최대 크기 55MB
- `db/schema.sql`: 상태 CHECK 3곳, 컬럼 주석
- `db/queries.sql`: 처리 대기 검색 예시에 `PROGRESS` 포함
- `docs/URL_PLAN.md`: 첨부 최대 5개

## 맞춘 문서

- Notion: ERD(상태 값), API 명세서(상태 값, 첨부 개수), 기능 명세서(REPAIR-2, 5)
- repo: `docs/URL_PLAN.md`

## 정한 것

- 상태 코드는 `PROGRESS`다(`VARCHAR2(10)`이라 `IN_PROGRESS`는 들어가지 않는다).
- 이미 `schema.sql`로 테이블을 만든 DB는 CHECK 제약이 옛 값이라, 제약 3개를 다시 만들거나 테이블을 다시 만들어야 한다(`ddl-auto`로 만든 경우도 같다).

## 확인한 방법

- `mvn compile` 통과. Oracle과 실제 화면에서는 확인하지 못했다.
