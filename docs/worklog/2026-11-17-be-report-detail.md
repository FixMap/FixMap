# 사용자용 신고 상세 조회 API 구현

- 날짜: 2026-11-17
- 작성: Claude, 요청한 사람 소마고3
- 영역: be
- 기능 ID: REPAIR-8
- 브랜치·PR: `feature/be-report-detail` (`feature/be-admin-list`, PR #15 위에 쌓음)

## 한 일

- 메인 오른쪽 영역에서 "이 층의 신고" 목록의 항목이나 지도의 핀을 누르면 그 신고의 내용과 관리자 답글을 보여 주려고 `GET /api/reports/{id}`를 만들었다.
- 관리자 상세(`GET /api/admin/reports/{id}`)와 같은 조회 코드를 쓰고 읽음 처리만 하지 않는다. 응답 형식도 같다(`read` 필드는 읽음 처리가 일어난 뒤의 관리자 읽음 여부라 사용자 화면에서는 쓰지 않는다).

## 바꾼 파일

- `controller/ReportApiController`: 상세 조회 메서드 추가
- `service/AdminReportQueryService`: `view`(읽음 처리 없는 조회) 추가, `detail`은 읽음 처리 뒤 같은 코드를 부른다
- `docs/URL_PLAN.md`: 사용자 API에 한 줄 추가

## 맞춘 문서

- Notion: API 명세서에 "신고 상세 조회(사용자)" 행 추가
- repo: `docs/URL_PLAN.md`

## 정한 것과 미정

- 정한 것: 신고자가 답글을 보는 곳은 메인 오른쪽 영역이다. 별도 신고 조회 화면(`/reports`)은 계속 쓰지 않는다.
- 미정: 관리자 접근 제한 방식(이 API는 누구나 열 수 있는 사용자용이다)

## 확인한 방법

- `mvn package` 성공.
- H2(Oracle 모드)와 Tomcat 10.1에서 호출했다. 상세 200(내용, 첨부, 답글), 없는 신고 404, 숫자가 아닌 id 400, `/api/reports/map` 매핑이 겹치지 않음. 사용자 상세를 열어도 안 읽은 수가 그대로이고 관리자 상세를 열면 줄어드는 것을 확인했다.
- Oracle에서는 확인하지 못했다. 화면(JSP, JS)은 없어서 열어 보지 못했다.

## 남은 일, 다음에 할 일

- FE가 우측 영역에서 이 API를 불러 내용·첨부·답글을 그린다.

## 요청 (다른 영역이나 담당자에게)

- FE: 우측 영역에서 목록 항목이나 핀을 누르면 `GET /api/reports/{id}`로 내용(제목, 종류, 위치, 상태, 작성일, 본문), 첨부(`url`을 이미지로 표시), 관리자 답글(`comments`)을 보여 준다. 응답의 `read`, `categoryId`, `floorId`, `placeId`, `posX`, `posY`는 쓰지 않아도 된다. 없는 신고(404)는 목록을 새로고침한다.
