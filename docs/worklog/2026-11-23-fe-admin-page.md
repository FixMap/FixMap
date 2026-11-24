# 관리자 신고 관리 화면(/admin) JSP·JS·CSS 구현

- 날짜: 2026-11-23
- 작성: Claude, 요청한 사람(팀원)
- 영역: fe
- 기능 ID: REPAIR-3, 4, 5, 10
- 브랜치·PR: `feature/fe-admin-page`, PR #22

## 한 일

- Notion 디자인 시안 C(목록), C-2(상세 패널), C-3(삭제 확인)를 기준으로 `/admin` 화면 구현
- 목록: 어두운 상단 바, 요약 카드(새 접수=안 읽음, 접수, 처리 중, 완료, 보류), 건물별 막대, 상태 칩·건물 선택·검색, 안 읽은 신고는 진하게·읽은 신고는 흐리게, 행마다 "처리하기"
- 상세 패널(오른쪽 서랍, `<dialog>`): 내용, 첨부, 핀 위치(장소 이름 같이 표시), 답글(수정·삭제), 답글 작성, 하단 상태 선택(접수·처리 중·보류·완료)·저장, 완료 신고만 삭제
- 신고 삭제와 답글 삭제는 같은 `<dialog>` 확인 창을 씀
- 상세를 열어 읽음 처리되면 "새 접수" 숫자도 바로 1 줄임
- 리뷰 반영(PR #22): "처리 중"(#19에서 확정) 추가, 제목·부제 간격, 좁은 화면에서 서랍 하단 배치 정리

## 바꾼 파일

- `src/main/webapp/WEB-INF/views/admin.jsp`: 새 화면
- `src/main/webapp/static/admin/admin.js`: 상세 패널과 관리자 API 호출
- `src/main/webapp/static/admin/admin.css`: 관리자 화면 전용 스타일

## 맞춘 문서

- Notion: 디자인 페이지 C, C-2, C-3 시안
- repo: `docs/URL_PLAN.md`의 `/admin` 쿼리 파라미터와 관리자 API 목록, `AdminPageController`의 모델 이름

## 정한 것과 미정

- 정한 것: 없음(기존 명세만 따름)
- 미정: 상세를 패널로 할지 별도 페이지로 할지(이번엔 시안대로 패널), CSS 파일 구조(이번엔 `static/admin/`에 따로 둠)
- 시안과 다른 점: 작성자 열과 "평균 n일째", "이번 주/이번 달" 같은 기간 통계는 API에 값이 없어서 뺌. 건물별 막대는 전체 기간 기준

## 확인한 방법

- Docker(Oracle Free, Tomcat 10.1)로 띄운 테스트 서버와 Orca 브라우저에서 목록, 상세 패널, 답글 저장, 삭제 확인 창을 직접 확인. 상태 변경 저장, 답글 수정·삭제, 실제 삭제는 BE 리뷰(PR #22)에서 확인됨
- 리뷰 반영 후: "처리 중"으로 바꿔 저장, 요약·칩 숫자, 읽음 시 "새 접수" 감소, 답글 삭제 확인 창을 같은 환경에서 확인. 375px 서랍 화면은 확인하지 못함

## 남은 일, 다음에 할 일

- 상태 저장 후 `location.reload()`로 새로고침하는 것은 그대로 둠(요약과 목록을 한 번에 맞추기 위해). 필요하면 행과 숫자만 갱신하도록 바꿀 수 있음

## 요청 (다른 영역이나 담당자에게)

- BE: `/admin?status=PROGRESS` 필터가 적용되지 않고 전체가 나옴. `AdminReportQueryService.STATUS_ORDER`에 `PROGRESS`가 없어서 `isStatus`가 거짓이 됨(정렬 순서와 요약 0 채우기도 같은 목록을 씀)
- BE: `db/data.sql`의 `reporter_name`은 JPA(`ddl-auto`)로 테이블을 만들 때만 실패함(`schema.sql`로 만들면 정상). 엔티티에 `reporterName`이 없음
- BE: 관리자 세션이 이미 있으면 `/admin?key=...`가 리다이렉트되지 않아 주소창에 키가 남음(`AdminAccessInterceptor`)
