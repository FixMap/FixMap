<%@ page pageEncoding="UTF-8" %>
<%@ include file="header.jspf" %>
<div class="page-heading"><div><p class="eyebrow">OUR CAMPUS, TOGETHER</p><h1>어디가 불편한가요?</h1><p>지도에서 위치를 선택하고, 학교의 불편한 점을 알려주세요.</p></div><a class="text-link" href="<%= request.getContextPath() %>/reports">등록된 신고 살펴보기 <span aria-hidden="true">↗</span></a></div>
<div class="map-grid">
  <section class="panel map-panel" aria-label="학교 배치도">
    <div class="map-toolbar"><div id="building-tabs" class="segmented" role="group" aria-label="건물 선택"></div><div id="floor-tabs" class="floor-tabs" role="group" aria-label="층 선택"></div></div>
    <div class="map-subtitle"><span id="map-name">교내 전체 배치도</span><span>지도를 클릭해 위치 선택</span></div>
    <div class="map-scroll" id="map-scroll"><div class="map-stage" id="map-stage" tabindex="0" role="group" aria-label="신고 위치 지도. 방향키로 위치 이동, Enter로 위치 선택">
      <img id="map-image" alt="광주소프트웨어마이스터고등학교 교내 배치도" draggable="false">
      <div id="pins"></div>
    </div></div>
    <div class="map-bottom"><span>방향키로 이동 · Enter로 선택</span><div class="zoom-controls"><button id="zoom-out" type="button" aria-label="지도 축소">−</button><output id="zoom-value">100%</output><button id="zoom-in" type="button" aria-label="지도 확대">+</button></div></div>
    <p id="map-status" class="sr-only" role="status"></p>
  </section>
  <aside class="map-sidebar">
    <section class="panel selection-panel" id="selection-panel" aria-live="polite"></section>
    <section class="panel"><div class="section-heading"><h2>이 위치의 신고</h2><span id="pin-count" class="count">0</span></div><div class="legend"><span><i class="dot received"></i>접수</span><span><i class="dot completed"></i>완료</span><span><i class="dot held"></i>보류</span></div><div id="location-reports"><p class="muted">신고를 불러오는 중입니다.</p></div></section>
    <div class="tip"><span class="tip-icon" aria-hidden="true">i</span><p>사진을 첨부하면 문제를 더 쉽게 확인할 수 있어요.<br>JPG·PNG, 최대 3장까지 등록할 수 있습니다.</p></div>
  </aside>
</div>
<dialog id="report-dialog" aria-labelledby="report-dialog-title">
  <form id="report-form" enctype="multipart/form-data">
    <div class="dialog-heading"><div><p class="eyebrow">REPORT A PROBLEM</p><h2 id="report-dialog-title">문의/신고 작성</h2></div><button id="dialog-close" class="icon-button" type="button" aria-label="작성 취소하고 닫기">×</button></div>
    <div class="location-preview"><div class="mini-map"><img id="preview-map" alt="선택 위치 미리보기"><i id="preview-pin" class="preview-pin"></i></div><div><strong id="selected-location"></strong><p>지도에서 선택한 위치가 함께 저장됩니다.</p></div></div>
    <div class="form-columns"><label>제목 <span class="required">*</span><input name="title" required maxlength="100" placeholder="예) 교실 형광등이 깜빡여요" autocomplete="off"></label><label>카테고리 <span class="required">*</span><select id="report-category" name="category" required></select></label></div>
    <label>내용 <span class="required">*</span><textarea name="content" required maxlength="4000" rows="5" placeholder="고장이나 불편한 상황을 자세히 알려주세요."></textarea></label><div class="field-meta"><span>개인정보가 포함되지 않도록 작성해 주세요.</span><span id="content-count">0 / 4,000</span></div>
    <div class="upload-heading"><label for="file-input">첨부사진</label><span>JPG·PNG · 장당 5MB · 최대 3장</span></div>
    <div class="drop-zone" id="drop-zone"><input id="file-input" name="files" type="file" accept="image/jpeg,image/png,.jpg,.jpeg,.png" multiple><label for="file-input"><span aria-hidden="true">＋</span><strong>사진을 끌어다 놓거나 선택해 주세요</strong><small>문제 상황이 잘 보이는 사진을 첨부해 주세요.</small></label></div>
    <div id="file-previews" class="file-previews"></div>
    <p id="form-error" class="notice error" role="alert" hidden></p>
    <div class="dialog-actions"><button id="form-cancel" type="button" class="button secondary">취소</button><button id="form-submit" type="submit" class="button primary">신고 등록</button></div>
  </form>
</dialog>
<%@ include file="footer.jspf" %>
