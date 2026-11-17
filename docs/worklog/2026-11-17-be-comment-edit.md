# 관리자 답글 수정·삭제 API 구현

- 날짜: 2026-11-17
- 작성: Claude, 요청한 사람 소마고3
- 영역: be
- 기능 ID: REPAIR-4
- 브랜치·PR: `feature/be-comment-edit` (`feature/be-admin-list`, PR #15 위에 쌓음)

## 한 일

- `PATCH /api/admin/reports/{id}/comments/{commentId}`: 답글 내용을 고친다. 요청 `{ "content": "..." }`, 응답은 처리 저장의 `comment`와 같은 `{ id, content, createdAt }`.
- `DELETE /api/admin/reports/{id}/comments/{commentId}`: 답글을 지운다. 성공하면 204.
- 오류: 내용이 비었거나 2000자를 넘으면 400, 없는 신고나 그 신고의 답글이 아닌 `commentId`는 404, 숫자가 아닌 id는 400.
- 신고 본문 수정은 만들지 않았다. 로그인이 없어 남의 신고를 고칠 수 있게 되기 때문이다.

## 바꾼 파일

- `controller/AdminReportApiController`: 수정·삭제 메서드와 요청 레코드 추가
- `service/AdminReportService`: `editComment`, `deleteComment`
- `domain/ReportComment`: `edit` 메서드(`updated_at` 갱신)
- `docs/URL_PLAN.md`: 해당 줄을 구현됨으로

## 맞춘 문서

- Notion: API 명세서의 "관리자 답글 수정·삭제" 행, 기능 명세서의 답글 작성 기능(REPAIR-4)
- repo: `docs/URL_PLAN.md`

## 정한 것과 미정

- 정한 것: 수정은 내용만 바꾸고 상태는 건드리지 않는다. 답글은 삭제하면 복구하지 않는다.
- 미정: 관리자 접근 제한 방식(이 API에도 적용하지 않았다)

## 확인한 방법

- `mvn package` 성공.
- H2(Oracle 모드)와 Tomcat 10.1에서 호출했다. 수정 200, 공백 내용 400, 다른 신고의 답글 404, 없는 신고 404, 숫자가 아닌 `commentId` 400, 삭제 204, 같은 답글 다시 삭제 404.
- Oracle에서는 확인하지 못했다. 화면은 없어 열어 보지 못했다.

## 남은 일, 다음에 할 일

- 관리자 접근 제한 방식이 정해지면 적용

## 요청 (다른 영역이나 담당자에게)

- FE: 관리자 상세 패널의 답글에 수정·삭제 버튼을 둔다면 위 두 API를 부른다. 삭제나 수정 뒤에는 상세를 다시 불러 목록을 새로고침한다. 삭제는 되돌릴 수 없으므로 확인을 한 번 받는 것을 권장한다.
