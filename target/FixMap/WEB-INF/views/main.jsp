<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>FixMap - 학교 고장 신고</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/common.css">
</head>
<body data-ctx="${pageContext.request.contextPath}">
  <header class="top-bar">
    <a class="logo" href="${pageContext.request.contextPath}/">FixMap</a>
    <nav>
      <a class="active" href="${pageContext.request.contextPath}/">학교 지도</a>
      <%-- TODO: 신고 조회 페이지 --%>
    </nav>
  </header>

  <main class="map-page">
    <section class="map-panel">
      <div class="tabs" id="building-tabs"></div>
      <div class="tabs" id="floor-tabs"></div>
      <%-- 배치도 이미지 위에 핀을 겹쳐 그린다. 핀 좌표는 0~1 비율 --%>
      <div class="map-wrap" id="map-wrap">
        <img id="map-image" alt="학교 배치도">
        <div id="pin-layer"></div>
      </div>
    </section>
    <aside class="side-panel">
      <h2>범례</h2>
      <p><span class="dot RECEIVED"></span> 접수 <span class="dot DONE"></span> 완료 <span class="dot HOLD"></span> 보류</p>
      <h2>이 층의 신고</h2>
      <ul id="report-list"></ul>
    </aside>
  </main>

  <script src="${pageContext.request.contextPath}/static/js/map.js"></script>
</body>
</html>
