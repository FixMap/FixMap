<%@ page pageEncoding="UTF-8" %>
<%@ include file="header.jspf" %>
<div class="page-heading campus-heading">
  <span class="school-tag">광주소프트웨어마이스터고등학교</span>
  <h1>고장 난 곳, 지도에서 바로 알려주세요</h1>
  <p>건물과 층을 고르고 지도에서 신고할 위치를 선택하세요.</p>
</div>
<div class="map-grid campus-workspace">
  <section class="campus-left" aria-label="학교 배치도">
    <div class="campus-toolbar" id="map-controls">
      <div id="building-tabs" class="map-tabs" role="group" aria-label="건물 선택">
        <span class="muted">건물을 불러오는 중입니다.</span>
      </div>
      <div id="floor-tabs" class="map-tabs floor-tabs" role="group" aria-label="층 선택"></div>
    </div>
    <div class="panel map-panel campus-card">
      <div class="map-scroll campus-viewport" id="map-scroll"><div class="map-stage" id="map-stage" tabindex="-1">
        <p class="empty muted">학교 지도를 불러오는 중입니다.</p>
      </div></div>
      <div class="zoom-controls campus-zoom"><button id="zoom-in" type="button" aria-label="지도 확대">+</button><button id="zoom-out" type="button" aria-label="지도 축소">−</button><output id="zoom-value" class="sr-only" aria-live="polite">100%</output></div>
      <p class="map-scroll-hint">좌우로 밀어 도면을 볼 수 있어요.</p>
      <p id="map-status" class="sr-only" role="status"></p>
    </div>
  </section>
  <aside class="panel campus-sidebar" aria-label="이 층의 신고와 작성">
    <section id="location-panel" class="campus-list-mode" aria-labelledby="location-title">
      <div class="campus-panel-heading"><p id="map-name" class="panel-context">학교 배치도</p><h2 id="location-title">이 층의 신고 <span id="pin-count">—</span>건</h2><p class="panel-hint">핀을 누르면 위치와 처리 상태를 확인할 수 있어요.</p></div>
      <div id="location-reports"><p class="empty muted">신고를 불러오는 중입니다.</p></div>
    </section>
    <section id="selection-panel" class="campus-detail-mode" aria-live="polite" hidden></section>
    <form id="report-form" class="composer" enctype="multipart/form-data" aria-labelledby="report-form-title" hidden>
      <fieldset id="composer-fields" class="composer-body">
        <div class="campus-panel-heading"><p class="panel-context">새 신고 · 핀으로 위치 선택됨</p><h2 id="report-form-title">신고 작성</h2></div>
        <div class="selected-location"><strong id="selected-location"></strong></div>
        <fieldset class="category-field"><legend>고장 종류 <span class="required">필수</span></legend><div id="category-options" class="category-options"></div></fieldset>
        <label class="composer-field">제목 <span class="required">필수</span><input name="title" required aria-required="true" maxlength="100" placeholder="어떤 점이 불편한가요?" autocomplete="off"></label>
        <label class="composer-field">내용 <span class="required">필수</span><span id="content-count" class="content-count">0 / 4,000</span><textarea name="content" required aria-required="true" maxlength="4000" rows="5" placeholder="고장이나 문의 내용을 자세히 알려주세요."></textarea></label>
        <p class="privacy-hint">개인정보가 포함되지 않도록 작성해 주세요.</p>
        <div class="upload-heading"><label for="file-input">첨부사진</label><span class="upload-rules">최대 5개 · 개당 10MB</span></div>
        <div class="upload-region"><div id="file-previews" class="file-previews"></div><div class="drop-zone" id="drop-zone"><input id="file-input" name="files" type="file" accept="image/jpeg,image/png,image/gif,image/webp,.jpg,.jpeg,.png,.gif,.webp" multiple><label for="file-input"><span aria-hidden="true">+</span><strong>사진 추가</strong><small>JPG · PNG · GIF · WebP</small></label></div></div>
        <p id="form-error" class="notice error" role="alert" hidden></p>
      </fieldset>
      <div class="composer-actions"><button id="form-cancel" type="button" class="button secondary">취소</button><button id="form-submit" type="submit" class="button primary" disabled>신고 접수하기</button></div>
    </form>
  </aside>
</div>
<%@ include file="footer.jspf" %>
