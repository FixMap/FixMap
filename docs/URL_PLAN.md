# URL 정리

2026-11-17에 팀장이 화면(page)마다 URL을 지정했다. 화면 시안은 Notion "디자인" 페이지(A, B, C, C-2)이고, 시안은 아직 팀 확정 전이다.
API의 요청·응답 형식은 Notion API 명세서(Backend 하위 페이지)가 기준이고, 이 문서는 주소만 모아 둔다. 기능 ID(`REPAIR-n`)는 Notion 기능 목록의 ID다.

표기: **지정**은 이 문서에서 정한 주소다. **구현됨**은 코드에 있다. **미정**은 주소나 방식이 아직 정해지지 않은 것이다.

## 화면(JSP) URL

화면은 두 개뿐이다. 신고 작성, 신고 목록, 관리자 상세는 별도 주소 없이 화면 안의 영역(패널)으로 처리한다.

| 화면 | URL | 기능 | 권한 | 비고 |
| --- | --- | --- | --- | --- |
| 메인(학교 지도) | `/` | REPAIR-7, 8, 1, 2 | 누구나 | 세 건물을 한 화면에 펼치고 층 탭으로 층을 바꾼다. 오른쪽 영역은 "이 층의 신고" 목록이고, 핀을 찍으면 같은 영역이 신고 작성으로 바뀐다. 선택한 층은 쿼리 `?level={층 번호}`로 받고, 없으면 기본 층을 보여 준다 |
| 관리자 신고 관리 | `/admin` | REPAIR-10, 3, 4, 5 | 관리자 | 요약 타일, 신고 목록(검색·정렬·필터), 신고 상세 패널(답글, 상태 변경)을 한 화면에 둔다. 상단에 `/`로 가는 "사용자 사이트로 이동" 버튼. 접근 제한 방식은 미정 |

관리자 목록의 검색·정렬·필터는 쿼리 문자열로 받는다: `/admin?q=형광등&status=RECEIVED&building=1&unread=true&sort=latest&page=1`

| 파라미터 | 의미 | 값 |
| --- | --- | --- |
| `q` | 키워드 검색 | 제목, 내용, 위치 |
| `status` | 처리 상태 | `RECEIVED`, `HOLD`, `DONE`, `IN_PROGRESS`(처리 중은 제안, 미정) |
| `building` | 건물 | `building.id` |
| `unread` | 안 읽은 신고만 | `true` |
| `sort` | 정렬 | `latest`(최신순), `status`(상태순) |
| `page` | 페이지 번호 | 1부터 |

### 쓰지 않는 URL

| URL | 이유 |
| --- | --- |
| `/reports`, `/reports/{id}` | 유저 사이트의 신고 조회는 이번 범위에서 뺀다(2026-11-16 결정). 시간이 되면 나중에 추가 |
| `/reports/new` | 신고 작성은 메인 오른쪽 영역에서 한다 |
| `/admin/reports/{id}` | 관리자 상세는 같은 화면의 패널로 연다(패널 방식은 미정, 별도 페이지로 바꾸면 이 주소를 쓴다) |
| `/login` | MVP에는 로그인이 없다(REPAIR-6, 시간이 남으면 추가) |

## JSON API URL

화면은 JSP가 그리고, 지도·핀·목록·폼 전송처럼 화면을 새로 그리지 않는 부분만 API로 부른다. 관리자 API는 `/api/admin/` 아래에 둔다.

### 사용자

| 메서드 | URL | 용도 | 기능 | 상태 |
| --- | --- | --- | --- | --- |
| GET | `/api/buildings` | 건물과 층 목록 | REPAIR-7 | 구현됨 (PR #11, 병합 전). 건물 탭은 쓰지 않고 층 탭을 만드는 데 쓴다 |
| GET | `/api/floors/{id}/places` | 층의 영역(place) 목록 | REPAIR-7 | 구현됨 (PR #11, 병합 전) |
| GET | `/api/reports/map?level={층 번호}` | 세 건물의 같은 층 신고 목록(제목, 상태, 종류, 위치). 지도 핀과 오른쪽 "이 층의 신고"가 같이 쓴다 | REPAIR-7 | 구현됨 (PR #11) |
| GET | `/api/categories` | 고장 종류 목록 | REPAIR-1 | 지정, 종류 목록(7개 제안)은 미정 |
| POST | `/api/reports` | 신고 등록 | REPAIR-1 | 지정, 요청·응답 미정 |
| POST | `/api/reports/{id}/files` | 첨부 파일 업로드 | REPAIR-2 | 지정, 저장 위치·제한 미정 |
| GET | `/api/files/{id}` | 첨부 파일 내려받기 | REPAIR-2 | 지정, 저장 위치가 정해져야 함 |

### 관리자

| 메서드 | URL | 용도 | 기능 | 상태 |
| --- | --- | --- | --- | --- |
| GET | `/api/admin/reports` | 관리자 신고 목록(검색·정렬·필터, 읽음 여부 포함) | REPAIR-3 | 지정, 요청·응답 미정 |
| GET | `/api/admin/reports/{id}` | 신고 상세(내용, 첨부, 핀 위치, 답글) | REPAIR-3 | 지정, 응답 미정 |
| PATCH | `/api/admin/reports/{id}/read` | 읽음 처리(상세를 열 때) | REPAIR-3, 10 | 지정, 읽음 처리 방식은 미정 |
| PATCH | `/api/admin/reports/{id}/status` | 처리 상태 변경 | REPAIR-5 | 지정, 상태 종류는 미정 |
| POST | `/api/admin/reports/{id}/comments` | 답글 작성 | REPAIR-4 | 지정 |
| PATCH, DELETE | `/api/admin/reports/{id}/comments/{commentId}` | 답글 수정, 삭제 | REPAIR-4 | 지정 |
| GET | `/api/admin/summary` | 요약 통계(안 읽은 신고, 상태별, 건물별 건수) | REPAIR-10 | 지정, 응답 미정 |

## 아직 정하지 않은 것

1. **관리자 상세를 패널로 할지 별도 페이지로 할지.** 패널이면 화면 URL은 `/admin` 하나다.
2. **관리자 보호 방식.** `/admin/*`, `/api/admin/*`를 한 필터로 묶어 비밀번호 1개를 확인하는 안이 있다(Notion 비고의 권장 사항).
3. **읽음 처리 방식과 DB 컬럼.** 상세를 열면 자동으로 읽음 처리할지, 컬럼을 어떤 형태로 둘지.
4. **처리 상태에 "처리 중"을 넣을지**, 고장 종류 7개를 확정할지.
5. **첨부 파일 저장 위치와 제한.** 정해져야 `/api/files/{id}`의 동작을 정할 수 있다.
6. 정해지면 Notion 기능 명세서, API 명세서, `AGENTS.md`의 URL 문구를 함께 고친다.
