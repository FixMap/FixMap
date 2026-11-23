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

## 정정 (2026-11-23)

- 사용자가 전달한 [BE 리뷰](https://github.com/FixMap/FixMap/pull/20#pullrequestreview-5936244474)에 따라 `develop@432680a`를 같은 브랜치에 병합했습니다. 위의 main 전용 계약·미연결·페이지 컨트롤러 부재 설명은 이전 작업 당시의 기록입니다.
- 층 번호 `level`로 신고를 한 번 조회하고 `floorId`로 건물에 배치합니다. 모델 `level`과 주소 `?level=`을 사용하고, 지도는 층 크기와 places의 비율 좌표로 그립니다. 이 PR에 넣었던 임시 SVG와 PNG 도면은 제거했습니다.
- 고장 종류, multipart 접수(사진 5개·개당 10MB), 공개 상세·첨부·관리자 답글 조회를 연결했습니다. 답글 작성과 관리자 API 호출은 포함하지 않습니다. 상태는 `PROGRESS`를 포함한 4개를 표시합니다.
- 접수 중 중복 전송/위치 변경을 막고, 실패 시 입력·사진을 유지합니다. 실제 접수 응답 뒤에만 성공을 알리며, 느린 이전 층·상세 응답이 현재 선택을 덮지 않게 합니다.
- 변경 범위: 기존 FE 파일, `places.js`, `report-detail.js`, 브라우저 검사와 설명. develop 병합 외에 Java·DB·공유 설정은 수정하지 않았습니다.
- 명세 확인: 전달받은 BE 리뷰, 최신 컨트롤러 응답·서비스 검증과 `docs/URL_PLAN.md`를 대조했습니다. Notion 연결 도구에서 프로젝트 페이지는 404여서 라이브 Notion 원문 확인은 하지 못했고 Notion 문서는 수정하지 않았습니다.
- 실제 Oracle/Eclipse 통합은 BE 확인이 필요합니다. 임시 검증 서버의 DB/업로드 경로 변경은 PR에 포함하지 않습니다.
- 검증: 최신 develop 설정으로 `clean package` 성공, WAR에서 임시 도면 자산 제거 확인. 실제 컨트롤러·서비스·Repository·JPA와 임시 H2/Tomcat을 사용한 `develop-api.mjs`가 통과했습니다(층당 단일 조회, 영역/핀 좌표, 4개 상태, 1440/375/320px, 지연/오류/재시도, 사진 5개 실제 접수·파일 바이트 조회, 상세·답글, 중복 전송 방지, 입력 보존, 안전한 텍스트). develop 대비 BE·공유 설정 diff는 없습니다.
