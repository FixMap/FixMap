# 관리자 접근 키로 /admin 제한 (MVP용 임시 방식)

- 날짜: 2026-11-23
- 작성: Claude, 요청한 사람은 팀원(소마고3)
- 영역: be
- 기능 ID: REPAIR-10
- 브랜치·PR: `feature/be-admin-access`

## 한 일

- 로그인이 없는 MVP라서 `db.properties`의 `admin.key` 하나로 관리자 화면과 API를 막았다.
- `/admin?key=키`로 처음 접속하면 세션에 저장하고 `/admin`으로 되돌려 보낸다. API는 `X-Admin-Key` 헤더로도 열 수 있다.
- 키가 없거나 틀리면 403(화면은 안내 문구, API는 `{ "message": ... }`). `admin.key`를 비우면 모두 막힌다.

## 바꾼 파일

- `service/AdminAccess.java`(새 파일): 키 비교(시간 일정 비교)
- `config/AdminAccessInterceptor.java`(새 파일): `/admin`, `/api/admin/**` 검사
- `config/WebConfig.java`: 인터셉터 등록(공유 설정 파일이라 한 줄 규모의 변경)
- `src/main/resources/db.properties.example`: `admin.key` 항목
- `docs/URL_PLAN.md`, `AGENTS.md`: 접근 제한 방식 반영

## 맞춘 문서

- Notion: 기능 명세서(관리자 페이지)의 접근 제한 문구, API 명세서의 변경 기록
- repo: `docs/URL_PLAN.md`

## 정한 것

- 키 하나를 공유하는 방식은 MVP용 임시 방식이다. 로그인을 도입하면 대체한다.
- FE: `admin.jsp`는 따로 인증을 다루지 않아도 된다. API는 같은 세션으로 열린다(`fetch`는 같은 출처라 쿠키가 자동으로 간다).
- 이미 만든 `db.properties`에는 `admin.key=`를 직접 추가해야 한다(예시 파일만 바뀐다).

## 확인한 방법

- H2(Oracle 모드)와 임베디드 Tomcat 10.1: 키 없이 `/admin`, `/api/admin/reports/1` 403, 틀린 키 403, 맞는 키로 `/admin?key=` 302(`/admin`), 세션 쿠키로 API 200, `X-Admin-Key` 헤더로 API 200, 사용자 API(`/api/reports/map`) 200.
- `admin.jsp`가 없어서 `/admin` 화면 자체는 열어 보지 못했다. Oracle과 실제 브라우저 흐름은 확인하지 못했다.
