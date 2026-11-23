# 관리자 신고 관리 화면(/admin) JSP·JS·CSS 구현

- 날짜: 2026-11-23
- 작성: Claude, 요청한 사람(팀원)
- 영역: fe
- 기능 ID: REPAIR-3, 4, 5, 10
- 브랜치·PR: `feature/fe-admin-page`, PR 미생성

## 한 일

- Notion 디자인 시안 C(목록), C-2(상세 패널), C-3(삭제 확인)를 기준으로 `/admin` 화면 구현
- 목록: 어두운 상단 바, 요약 카드(새 접수=안 읽음, 접수, 완료, 보류), 건물별 막대, 상태 칩·건물 선택·검색, 안 읽은 신고는 진하게·읽은 신고는 흐리게, 행마다 "처리하기"
- 상세 패널(오른쪽 서랍, `<dialog>`): 내용, 첨부, 핀 위치, 답글(수정·삭제), 답글 작성, 하단 상태 선택·저장, 완료 신고만 삭제(확인 창)

## 바꾼 파일

- `src/main/webapp/WEB-INF/views/admin.jsp`: 새 화면
- `src/main/webapp/static/admin/admin.js`: 상세 패널과 관리자 API 호출
- `src/main/webapp/static/admin/admin.css`: 관리자 화면 전용 스타일

## 맞춘 문서

- Notion: 디자인 페이지 C, C-2, C-3 시안
- repo: `docs/URL_PLAN.md`의 `/admin` 쿼리 파라미터와 관리자 API 목록, `AdminPageController`의 모델 이름

## 정한 것과 미정

- 정한 것: 없음(기존 명세만 따름)
- 미정: 상세를 패널로 할지 별도 페이지로 할지(이번엔 시안대로 패널), "처리 중" 상태(이번엔 접수·보류·완료만 선택 가능, 시안에는 있음), CSS 파일 구조(이번엔 `static/admin/`에 따로 둠)
- 시안과 다른 점: 작성자 열과 "평균 n일째", "이번 주/이번 달" 같은 기간 통계는 API에 값이 없어서 뺌. 건물별 막대는 전체 기간 기준

## 확인한 방법

- Docker(Oracle Free, Tomcat 10.1)로 띄운 테스트 서버와 Orca 브라우저에서 목록, 상세 패널, 답글 저장, 삭제 확인 창을 직접 확인. 상태 변경 저장, 답글 수정·삭제, 실제 삭제는 확인하지 못함

## 남은 일, 다음에 할 일

- 상태 변경 저장, 답글 수정·삭제, 신고 삭제 동작 확인

## 요청 (다른 영역이나 담당자에게)

- BE: `db/data.sql`의 report INSERT에 엔티티에 없는 `reporter_name` 컬럼이 있어 실행이 실패함
- BE: 관리자 세션이 이미 있으면 `/admin?key=...`가 리다이렉트되지 않아 주소창에 키가 남음(`AdminAccessInterceptor`)
