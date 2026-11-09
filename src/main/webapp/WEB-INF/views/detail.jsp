<%@ page pageEncoding="UTF-8" %>
<%@ include file="header.jspf" %>
<a class="back-link" href="<%= request.getContextPath() %>/reports">← 신고 목록</a>
<div id="report-detail" class="panel full-detail" aria-live="polite"><p class="muted">신고 내용을 불러오는 중입니다.</p></div>
<%@ include file="footer.jspf" %>
