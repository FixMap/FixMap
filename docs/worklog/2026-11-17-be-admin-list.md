# 관리자 목록·요약, 신고 상세 조회, 읽음 처리 구현

- 날짜: 2026-11-17
- 작성: Claude, 요청한 사람 소마고3
- 영역: be
- 기능 ID: REPAIR-3, REPAIR-10
- 브랜치·PR: `feature/be-admin-list`

## 한 일

- `/admin` 페이지 컨트롤러를 만들었다. 요약 타일(전체, 안 읽은 수, 상태별, 건물별)과 목록(검색·상태·건물·안 읽음 필터, 최신순·상태순 정렬, 10개씩 페이지)을 모델에 담아 `admin` 뷰로 넘긴다.
- `GET /api/admin/reports/{id}`를 만들었다. 내용, 위치(건물·층·영역), 핀 좌표, 첨부(`/api/files/{id}` 주소 포함), 답글을 주고, 열면 읽음으로 바꾼다.
- 읽음 표시는 `report.is_read`(`NUMBER(1)`, 기본 0)로 저장한다. 읽음으로 바꿀 때는 `updated_at`을 바꾸지 않는다.

## 바꾼 파일

- `controller/AdminPageController`, `service/AdminReportQueryService`: 새 파일
- `controller/AdminReportApiController`: 상세 조회 메서드 추가
- `domain/Report`: `read` 필드 추가(공유 파일). `repository/ReportRepository`: `markRead`(공유 파일). `ReportFileRepository`, `ReportCommentRepository`: 신고별 조회 메서드
- `db/schema.sql`: `report.is_read`와 체크 제약, 주석 추가(공유 파일)
- `docs/URL_PLAN.md`: 관리자 목록·읽음·요약은 만들지 않는 주소로, 상세 조회는 구현됨으로 고침

## 맞춘 문서

- Notion: API 명세서의 "관리자 신고 상세 조회" 행(응답), 목록·요약·읽음 행의 설명, ERD 설계의 `report` 표와 다이어그램(`is_read`)
- repo: `docs/URL_PLAN.md`

## 정한 것과 미정

- 정한 것: 읽음은 상세를 열 때 서버가 바꾼다. 잘못된 `status`, `sort`, `page` 쿼리는 그 조건을 쓰지 않거나 가까운 값으로 맞춘다(오류를 내지 않는다). 키워드 검색 대상은 제목, 내용, 건물·층·영역 이름이다.
- 미정: 관리자 접근 제한 방식, 처리 상태에 "처리 중"을 넣을지, 상세를 패널로 할지 별도 페이지로 할지, 답글 수정·삭제

## 확인한 방법

- `mvn package` 성공.
- H2(Oracle 모드)와 임베디드 Tomcat 10.1에서 확인했다. 목록 필터(상태, 키워드, 건물, 안 읽음), 상태순 정렬, 올바르지 않은 쿼리, 요약 집계는 모델을 JSON으로 꺼내 보는 임시 확인용 클래스로 봤다(저장소에는 없음). 상세 조회 200, 열고 나면 안 읽은 수가 하나 줄어듦, 없는 신고 404, 숫자가 아닌 id 400.
- H2에서는 `is_read`를 `BOOLEAN`으로 바꾼 스키마로 확인했다(H2가 `NUMBER(1)`과 불리언 비교를 허용하지 않아서). `NUMBER(1)` 열과 Hibernate의 Oracle 쪽 SQL은 확인하지 못했다.
- `admin.jsp`가 없어 `/admin` 화면 자체는 열어 보지 못했다. Oracle에서도 확인하지 못했다.

## 남은 일, 다음에 할 일

- Oracle에서 `is_read` 열(`NUMBER(1)`)과 읽음 처리 쿼리 확인
- 관리자 접근 제한 방식이 정해지면 `/admin`과 `/api/admin/*`에 적용
- 답글 수정·삭제는 나중에

## 요청 (다른 영역이나 담당자에게)

- FE: `WEB-INF/views/admin.jsp`가 필요하다. 모델 이름은 `reports`(목록), `summary`, `page`, `totalPages`, `totalCount`, `q`, `status`, `building`, `unread`, `sort`이고 필드는 `AdminReportQueryService`의 `Row`, `Summary`를 본다. 상세 패널은 `GET /api/admin/reports/{id}`를 부르고, 열면 읽음이 되므로 목록의 읽음 표시는 새로고침해야 바뀐다.
- DB 담당: 이미 `report` 테이블을 만든 DB는 `ALTER TABLE report ADD is_read NUMBER(1) DEFAULT 0 NOT NULL;`로 열을 추가한다(`ddl-auto`는 기존 테이블에 열을 추가하는지 설정에 따라 다르다).
