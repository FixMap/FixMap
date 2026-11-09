# URL 정리 (제안)

> 팀이 정하기 전의 **제안**이다. URL은 아직 팀에서 최종 확정하지 않았다. 아래 "Notion 명세에 있는 것"만 기능 명세서에 적혀 있고, 나머지는 논의 후 바꾼다.
> 기능 ID(`REPAIR-n`)는 Notion 기능 목록의 ID다.

## Notion 명세에 있는 것 (2026-11-03 정리, 팀 최종 확정은 아직)

| 구분 | URL | 비고 |
| --- | --- | --- |
| 사용자 메인 | `/` | 학교 지도 첫 화면 (REPAIR-7) |
| 관리자 | `/admin` | 로그인 없이 URL로만 구분. 관리자 화면에 `/`로 돌아가는 링크 |
| 관리자 API | `/api/admin/` 아래 | 사용자용 API와 분리 |

로그인은 MVP에서 우선순위가 가장 낮다 (REPAIR-6).

## 화면(JSP) URL 제안

| 화면 | 제안 URL | 기능 | 권한 | 비고 |
| --- | --- | --- | --- | --- |
| 메인(학교 지도) | `/` | REPAIR-7 | 누구나 | Notion 명세에 있음 |
| 신고 작성 | `/reports/new?floor=3&x=0.42&y=0.31` | REPAIR-8, 1, 2 | 사용자 | 핀 위치(층, 0~1 좌표)를 쿼리로 받는다. 모달로 할지는 미정 |
| 신고 조회(목록) | `/reports` | REPAIR-9 | 사용자, 관리자 | 검색·정렬·필터는 쿼리로 (아래 표) |
| 신고 상세 | `/reports/{id}` | REPAIR-9 | 사용자, 관리자 | 내용, 첨부, 답글, 처리 상태 |
| 관리자 신고 관리 | `/admin` | REPAIR-10, 3 | 관리자 | Notion 명세에 있음 |
| 관리자 신고 상세 | `/admin/reports/{id}` | REPAIR-3, 4, 5 | 관리자 | 답글 작성, 완료/보류 처리 |
| 로그인 | `/login` | REPAIR-6 | 비로그인 | 시간이 남으면 추가. MVP 제외 |

### 신고 조회의 검색·정렬·필터 (쿼리 문자열)

`/reports?q=형광등&status=RECEIVED&floor=3&category=1&from=2026-11-01&to=2026-11-07&sort=latest&page=1`

| 파라미터 | 의미 | 값 |
| --- | --- | --- |
| `q` | 키워드 검색 | 제목, 내용 |
| `status` | 처리 상태 | `RECEIVED`, `DONE`, `HOLD` |
| `floor` | 위치(층 id) | `building_floor.id` |
| `category` | 카테고리 | `category.id` |
| `from`, `to` | 기간 | `YYYY-MM-DD` |
| `sort` | 정렬 | `latest`(최신순), `status`(상태순) |
| `page` | 페이지 번호 | 1부터 |

## JSON API 제안

JSP와 JSON을 섞어 쓰기로 했으므로, 화면은 JSP가 그리고 지도·핀·폼 전송처럼 화면을 새로 그리지 않는 부분만 API로 부른다.

| 메서드 | URL | 용도 | 상태 |
| --- | --- | --- | --- |
| GET | `/api/buildings` | 건물과 층 목록 | 초기 세팅 코드에 있던 것(코드는 삭제됨). 메인 페이지 명세에 API 표로 적음 |
| GET | `/api/reports/map?floor={id}` | 층의 핀 목록 | 초기 세팅 코드에 있던 것(코드는 삭제됨). 메인 페이지 명세에 API 표로 적음 |
| GET | `/api/floors/{id}/places` | 층의 영역(place) 목록 | 제안 (메인 페이지 명세에 API 표로 적음) |
| POST | `/api/reports` | 신고 등록 | 제안 |
| POST | `/api/reports/{id}/files` | 첨부 파일 업로드 | 제안 |
| GET | `/api/files/{id}` | 첨부 파일 내려받기 | 제안 (저장 위치가 정해져야 함) |
| GET | `/api/admin/reports` | 관리자 신고 목록 | 제안 |
| PATCH | `/api/admin/reports/{id}/status` | 완료/보류 처리 | 제안 |
| POST | `/api/admin/reports/{id}/comments` | 관리자 답글 | 제안 |

## 정해야 할 것

1. **신고 작성을 별도 페이지로 할지, 지도 위 모달로 할지.** Notion 명세에는 "신고 페이지"와 "지도에서 핀에서 바로 작성"이 둘 다 있다.
2. **일반 사용자가 신고 조회를 할 수 있는지.** Notion 비고에 "본인 신고 조회가 필요한지 결정 필요"라고 적혀 있다. 로그인이 없으면 "본인"을 구분하기 어렵다.
3. **관리자 보호 방식.** `/admin/*`, `/api/admin/*`를 한 필터로 묶어 비밀번호 1개를 확인하는 안이 있다 (Notion 비고의 권장 사항).
4. **첨부 파일 저장 위치.** 정해져야 파일 다운로드 URL(`/api/files/{id}`)을 정할 수 있다.
5. **URL 표기.** 소문자와 복수형(`/reports`)으로 통일하는 안이다.
6. 정해지면 Notion 기능 명세서와 `AGENTS.md`의 "URL 구조" 문구를 함께 고친다.
