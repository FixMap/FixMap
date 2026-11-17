# 관리자 신고 삭제 API 구현

- 날짜: 2026-11-17
- 작성: Claude, 요청한 사람 소마고3
- 영역: be
- 기능 ID: REPAIR-10
- 브랜치·PR: `feature/be-report-delete` (`feature/be-admin-list`, PR #15 위에 쌓음)

## 한 일

- `DELETE /api/admin/reports/{id}`를 만들었다. 완료(`DONE`)된 신고만 지울 수 있고, 답글, 상태 변경 기록, 첨부 정보를 함께 지운 뒤 DB 삭제가 확정되면 서버에 저장된 첨부 파일도 지운다.
- 성공하면 204. 완료 상태가 아니면 400(`완료된 신고만 삭제할 수 있습니다`), 없는 신고는 404, 숫자가 아닌 id는 400.
- 지운 신고는 복구하지 않는다. 자동 삭제(완료되면 저절로 지워짐)는 만들지 않았다.

## 바꾼 파일

- `controller/AdminReportApiController`: 삭제 메서드 추가
- `service/AdminReportService`: `deleteReport`
- `repository/ReportCommentRepository`, `StatusHistoryRepository`, `ReportFileRepository`: `deleteByReportId`
- `docs/URL_PLAN.md`: 관리자 API에 한 줄 추가

## 맞춘 문서

- Notion: API 명세서에 "관리자 신고 삭제" 행, 기능 명세서의 관리자 페이지(REPAIR-10), 디자인 페이지(삭제 버튼과 확인창 시안)
- repo: `docs/URL_PLAN.md`

## 정한 것과 미정

- 정한 것: 완료된 신고만 삭제한다(접수·보류 상태는 400). 완전 삭제이고 복구하지 않는다.
- 미정: 관리자 접근 제한 방식. 지금은 주소를 아는 누구나 완료된 신고를 지울 수 있으므로 접근 제한이 정해지면 가장 먼저 적용한다.

## 확인한 방법

- `mvn package` 성공.
- H2(Oracle 모드)와 Tomcat 10.1에서 호출했다. 접수 상태 400, 완료로 바꾼 뒤 삭제 204, 첨부 파일이 디스크에서 사라짐, 지도 API에서 사라짐, 같은 신고 다시 삭제 404, 답글·이력이 있는 시드 신고 삭제 204, 숫자가 아닌 id 400.
- Oracle에서는 확인하지 못했다. 화면은 없어 열어 보지 못했다.

## 남은 일, 다음에 할 일

- 관리자 접근 제한 방식이 정해지면 적용

## 요청 (다른 영역이나 담당자에게)

- FE: 관리자 상세 패널에서 상태가 완료인 신고에만 "삭제" 버튼을 보이고, 누르면 확인창을 띄운 뒤 `DELETE /api/admin/reports/{id}`를 부른다. 성공하면 패널을 닫고 `/admin`을 새로고침한다. 시안은 Notion 디자인 페이지 C-3.
