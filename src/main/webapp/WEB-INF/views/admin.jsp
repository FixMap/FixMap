<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%-- 시안: Notion "디자인" 페이지 C, C-2, C-3. 모델은 record라서 EL 5.0(Tomcat 10.1)에서 메서드로 읽는다: r.title() --%>
<!doctype html>
<html lang="ko">
<head>
  <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
  <title>FixMap | 관리자 신고 관리</title>
  <link rel="stylesheet" href="<c:url value='/static/user/app.css'/>">
  <link rel="stylesheet" href="<c:url value='/static/admin/admin.css'/>">
  <script type="module" src="<c:url value='/static/admin/admin.js'/>"></script>
</head>
<body data-context="<%= request.getContextPath() %>" data-page="admin">
<a class="skip" href="#main">본문으로 건너뛰기</a>
<header class="admin-header">
  <a class="brand" aria-label="FixMap 관리자 홈" href="<c:url value='/admin'/>"><span class="brand-icon" aria-hidden="true"></span><span class="brand-name">Fix<span class="word-map">Map</span></span><span class="admin-tag">관리자</span></a>
  <a class="admin-go" href="<c:url value='/'/>">사용자 사이트로 이동 <span aria-hidden="true">↗</span></a>
</header>
<main id="main" class="admin-main">
<p id="action-status" class="notice success" role="status" hidden></p>
<h1>신고 관리</h1>
<p class="admin-sub">들어온 신고를 확인하고 답글과 처리 상태를 남겨주세요.</p>

<c:set var="maxBuilding" value="1"/>
<c:forEach var="b" items="${summary.byBuilding()}"><c:if test="${b.count() > maxBuilding}"><c:set var="maxBuilding" value="${b.count()}"/></c:if></c:forEach>
<section class="admin-stats" aria-label="신고 요약">
  <div class="card stat"><span class="m">새 접수</span><strong>${summary.unread()}</strong><span class="m small">아직 열어 보지 않음</span></div>
  <div class="card stat"><span class="m">접수</span><strong>${summary.byStatus()['RECEIVED']}</strong><span class="m small">처리 전</span></div>
  <div class="card stat"><span class="m">완료</span><strong class="done">${summary.byStatus()['DONE']}</strong><span class="m small">처리 완료</span></div>
  <div class="card stat"><span class="m">보류</span><strong class="held">${summary.byStatus()['HOLD']}</strong><span class="m small">처리 보류</span></div>
  <div class="card stat buildings"><b>건물별 신고</b>
    <c:forEach var="b" items="${summary.byBuilding()}">
      <div class="bar"><span><c:out value="${b.name()}"/></span><u><i style="width:${b.count() * 100 / maxBuilding}%"></i></u><b>${b.count()}</b></div>
    </c:forEach>
  </div>
</section>

<section class="card admin-list" aria-label="신고 목록">
  <form id="filter" class="admin-filter" method="get" action="<c:url value='/admin'/>" role="search">
    <nav class="chips" aria-label="상태로 거르기">
      <c:forEach var="s" items="${['', 'RECEIVED', 'HOLD', 'DONE']}">
        <c:url var="chipUrl" value="/admin"><c:param name="q" value="${q}"/><c:param name="building" value="${building}"/><c:param name="status" value="${s}"/></c:url>
        <a href="${chipUrl}" class="${status == s ? 'on' : ''}" ${status == s ? 'aria-current="true"' : ''}>${s == '' ? '전체' : s == 'RECEIVED' ? '접수' : s == 'HOLD' ? '보류' : '완료'} ${s == '' ? summary.total() : summary.byStatus()[s]}</a>
      </c:forEach>
    </nav>
    <input type="hidden" name="status" value="<c:out value='${status}'/>">
    <select name="building" aria-label="건물">
      <option value="">모든 건물</option>
      <c:forEach var="b" items="${summary.byBuilding()}">
        <option value="${b.id()}" ${building == b.id() ? 'selected' : ''}><c:out value="${b.name()}"/></option>
      </c:forEach>
    </select>
    <label class="search"><span class="sr-only">검색</span><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true"><circle cx="11" cy="11" r="7"/><path d="M20 20l-3.5-3.5"/></svg><input name="q" value="<c:out value='${q}'/>" placeholder="제목, 위치 검색"></label>
  </form>
  <c:choose>
    <c:when test="${empty reports}"><p class="empty muted">조건에 맞는 신고가 없습니다.</p></c:when>
    <c:otherwise>
      <div class="table-scroll"><table>
        <thead><tr><th scope="col">상태</th><th scope="col">제목</th><th scope="col">위치</th><th scope="col">종류</th><th scope="col">접수일</th><th scope="col" class="end">작업</th></tr></thead>
        <tbody>
        <c:forEach var="r" items="${reports}">
          <tr data-id="${r.id()}" class="${r.read() ? 'read' : 'unread'}">
            <td class="row-status" data-status="${r.status()}"></td>
            <td class="title"><c:out value="${r.title()}"/><c:if test="${!r.read()}"><span class="sr-only"> (안 읽음)</span></c:if></td>
            <td class="m"><c:out value="${r.building()} ${r.floor()}"/></td>
            <td class="m"><c:out value="${r.category()}"/></td>
            <td class="m"><time datetime="${fn:substring(r.createdAt(), 0, 10)}">${fn:replace(fn:substring(r.createdAt(), 5, 10), '-', '.')}</time></td>
            <td class="end"><button type="button" class="small-button row-open">처리하기</button></td>
          </tr>
        </c:forEach>
        </tbody>
      </table></div>
    </c:otherwise>
  </c:choose>
  <c:if test="${totalPages > 1}">
    <nav class="pager" aria-label="페이지">
      <c:forEach var="p" begin="1" end="${totalPages}">
        <c:url var="pageUrl" value="/admin"><c:param name="q" value="${q}"/><c:param name="status" value="${status}"/><c:param name="building" value="${building}"/><c:param name="page" value="${p}"/></c:url>
        <a href="${pageUrl}" class="${p == page ? 'on' : ''}" ${p == page ? 'aria-current="page"' : ''}>${p}</a>
      </c:forEach>
    </nav>
  </c:if>
</section>
</main>

<dialog id="drawer" class="drawer" aria-labelledby="drawer-title">
  <div class="hd"><b id="drawer-title">신고 상세</b><button type="button" class="close" aria-label="닫기">✕</button></div>
  <div class="bd" id="detail" aria-live="polite"></div>
  <form class="ft" id="save-form" hidden>
    <button type="button" id="delete-open" class="button delete" hidden>삭제</button>
    <span class="ft-label" id="status-label">상태</span>
    <div class="seg" id="status-seg" role="radiogroup" aria-labelledby="status-label"></div>
    <button type="submit" class="button primary">저장</button>
  </form>
</dialog>

<dialog id="confirm" class="confirm" aria-labelledby="confirm-title">
  <h3 id="confirm-title">이 신고를 삭제할까요?</h3>
  <p>답글과 첨부 사진도 함께 지워지고, 되돌릴 수 없어요. 완료된 신고만 삭제할 수 있어요.</p>
  <form method="dialog" class="a"><button value="cancel" class="button secondary">취소</button><button value="ok" class="button danger">삭제</button></form>
</dialog>
</body></html>
