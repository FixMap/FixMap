<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>FixMap 관리자</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/common.css">
</head>
<body data-ctx="${pageContext.request.contextPath}">
  <header class="top-bar">
    <a class="logo" href="${pageContext.request.contextPath}/admin">FixMap 관리자</a>
    <nav>
      <a class="active" href="${pageContext.request.contextPath}/admin">신고 관리</a>
    </nav>
    <%-- 관리자 페이지에서 일반 사용자 사이트로 바로 이동 --%>
    <a class="btn" href="${pageContext.request.contextPath}/">사용자 사이트로 이동</a>
  </header>

  <main class="admin-page">
    <section class="summary">
      <div class="card">전체 신고 <strong id="count-all">-</strong></div>
      <div class="card">접수 <strong id="count-received">-</strong></div>
      <div class="card">완료 <strong id="count-done">-</strong></div>
      <div class="card">보류 <strong id="count-hold">-</strong></div>
    </section>
    <%-- TODO: 신고 목록(검색·정렬·필터), 상세, 상태 변경, 답글 --%>
  </main>
</body>
</html>
