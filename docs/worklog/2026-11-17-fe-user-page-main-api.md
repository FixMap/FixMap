# 유저 메인 시안과 main 조회 API 연결

- 날짜: 2026-11-17
- 작성: Codex (이 대화의 사용자 요청)
- 영역: fe
- 기능 ID: REPAIR-7, REPAIR-1·2·8의 미연결 화면
- 브랜치·PR: `feature/fe-user-page`, develop 대상 Draft PR

## 한 일

- 첨부 Notion 메인·오른쪽 작성 시안을 JSP/CSS/JS로 적용하고 지도·작성 코드를 나눴습니다.
- 사용자 요청대로 `main`의 `7d34c2e`에서 확인한 건물 조회와 신고 핀 조회만 연결했습니다. `floor`는 층 ID이고 좌표는 `posX`/`posY`입니다. 도면 URL·건물명·층·핀은 응답을 따릅니다.
- main에 없는 등록·상세·분류·첨부 전송/다운로드는 연결하지 않았습니다. 접수 버튼은 비활성화하고 사진은 로컬 미리보기만 제공합니다. 전송·접수 성공을 표시하지 않습니다.
- main의 도면 경로에 필요한 기존 SVG와 공식 학교 전체 참고 PNG를 포함했습니다. 서로 다른 도면에 핀을 임의로 옮기지 않습니다.

## 바꾼 파일

- `WEB-INF/views/`: 유저 메인과 공통 JSP 조각만 추가.
- `static/user/`, `static/vendor/`, `static/maps/`: 메인 화면·조회 연동·미전송 작성 UI·폰트/라이선스·도면.
- `src/test/ui/`: main 계약과 미연결 기능을 확인하는 브라우저 검사.
- Java, DB, Maven, web.xml, 관리자 화면은 수정하지 않았습니다.

## 맞춘 문서

- Notion: 사용자가 제공한 디자인 마크다운과 두 유저 HTML 시안. 라이브 문서·API 명세서는 수정하지 않았습니다.
- repo: 요청받은 main 컨트롤러 원본 및 develop의 `AGENTS.md`, `docs/GIT_CONVENTION.md`, `docs/URL_PLAN.md`, `docs/WORK_SPLIT.md`.

## 정한 것과 미정

- 이번 연결 범위는 사용자가 지정한 main의 두 GET으로 제한했습니다. 팀의 API 규약을 새로 확정한 것은 아닙니다.
- 글 종류·고장 종류·추가 상태·저장/전송 방식은 임의로 구현하지 않았습니다.

## 확인한 방법

- 기존 develop 설정으로 Maven WAR 빌드 성공. backend 파일 diff 없음.
- main 원본 컨트롤러·DDL·초기 데이터를 임시 H2 Oracle 호환 모드와 Tomcat 10.1에서 실행해 JSP와 실제 조회 응답을 확인했습니다. 실제 Oracle 검증은 아닙니다.
- 브라우저 검사: 두 GET만 호출, 컨텍스트 경로 중복 없음, 실제 핀, 비활성 접수·전송 없음, 로컬 미리보기, 확대, 오류 재시도, 안전한 제목 표시, 1440/375/320px 확인.

## 요청 (BE 확인 필요)

- develop의 지도 API는 `level`을 받고 건물 응답에 도면 URL이 없어 main과 다릅니다. 합칠 계약 확인이 필요합니다. 사용자 지시대로 자동으로 develop 계약이나 다른 API로 우회하지 않았습니다.
- develop에는 `/`를 `main.jsp`에 연결하는 페이지 컨트롤러가 없습니다. 해당 연결은 BE 영역에서 필요합니다. 이 PR은 Draft로 두고 그 Java 코드를 추가하지 않습니다.
- 실제 Oracle/Eclipse 통합과 팀 Skill 페이지 등록 확인은 남아 있습니다.
