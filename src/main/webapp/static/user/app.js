const base = document.body.dataset.context;
const $ = (selector, root = document) => root.querySelector(selector);
const el = (tag, className, text) => {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text !== undefined) node.textContent = text;
  return node;
};
const classes = { '접수': 'received', '완료': 'completed', '보류': 'held' };
let meta;
async function api(path, options = {}) {
  let response, data;
  try { response = await fetch(base + path, options); }
  catch { throw new Error('서버에 연결하지 못했습니다. 연결 상태를 확인하고 다시 시도해 주세요.'); }
  try { data = await response.json(); }
  catch { throw new Error('서버 응답을 확인하지 못했습니다. 잠시 후 다시 시도해 주세요.'); }
  if (!response.ok) throw new Error(data.error || '요청을 처리하지 못했습니다. 다시 시도해 주세요.');
  return data;
}
function showError(error) {
  const box = $('#global-error'); box.textContent = error.message; box.hidden = false;
}
function toast(message) {
  const box = $('#toast'); box.textContent = message; box.hidden = false;
  setTimeout(() => { box.hidden = true; }, 5000);
}
function badge(status) { return el('span', `badge ${classes[status] || ''}`, status); }
function date(value) { return new Intl.DateTimeFormat('ko-KR', { timeZone: 'Asia/Seoul', year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date(value)); }
function location(report) { return report.buildingName + (report.floor === 0 ? ' · 교내' : ` ${report.floor}층`); }
function mapImage(building, floor) {
  const level = meta.buildings.find(item => item.id === building)?.levels.find(item => item.level === floor);
  if (!level) throw new Error('배치도 정보를 찾을 수 없습니다. 페이지를 새로고침해 주세요.');
  return base + level.mapImageUrl;
}
function pin(x, y, status, label) {
  const node = el('button', `pin ${classes[status] || 'draft'}`);
  node.type = 'button'; node.style.left = `${x * 100}%`; node.style.top = `${y * 100}%`;
  node.setAttribute('aria-label', label); node.title = label;
  return node;
}
function option(select, value, text) { select.add(new Option(text, value)); }
function renderDetail(root, data, compact = false) {
  const { report, attachments } = data;
  root.replaceChildren(badge(report.status), el(compact ? 'h2' : 'h1', 'detail-title', report.title), el('p', 'detail-info', `#${report.id} · ${location(report)} · ${report.category} · ${date(report.createdAt)}`), el('p', 'detail-content', report.content));
  const mapSection = el('section', 'detail-section'); mapSection.append(el('h2', '', '신고 위치'));
  const map = el('div', 'detail-map'); const image = el('img'); image.src = base + report.mapImageUrl; image.alt = `${location(report)} 신고 위치`;
  const marker = pin(report.x, report.y, report.status, '신고 위치'); marker.disabled = true; marker.tabIndex = -1;
  map.append(image, marker); mapSection.append(map);
  const mapLink = el('a', 'text-link', '학교 지도에서 보기 ↗'); mapLink.href = `${base}/?building=${encodeURIComponent(report.building)}&floor=${report.floor}&report=${report.id}`; mapSection.append(mapLink); root.append(mapSection);
  const fileSection = el('section', 'detail-section'); fileSection.append(el('h2', '', `첨부사진 ${attachments.length}`));
  if (!attachments.length) fileSection.append(el('p', 'muted', '첨부된 사진이 없습니다.'));
  const gallery = el('div', 'attachment-list');
  for (const attachment of attachments) {
    const figure = el('div'); const link = el('a'); link.href = `${base}/attachments/${attachment.id}`; link.target = '_blank'; link.rel = 'noopener';
    const photo = el('img'); photo.src = link.href; photo.alt = attachment.name; photo.loading = 'lazy'; link.append(photo);
    const download = el('a', '', `${attachment.name} · ${(attachment.size / 1024).toFixed(0)} KB ↓`); download.href = `${link.href}?download=1`;
    figure.append(link, download); gallery.append(figure);
  }
  fileSection.append(gallery); root.append(fileSection);
  if (compact) { const link = el('a', 'button secondary', '상세 페이지로 이동 ↗'); link.href = `${base}/reports/${report.id}`; root.append(link); }
}

async function initMap() {
  if (!meta.buildings.length) throw new Error('등록된 배치도가 없습니다. 관리자에게 문의해 주세요.');
  let building = meta.buildings[0].id, floor = meta.buildings[0].levels[0].level, draft = null, reports = [], zoom = 100, requestVersion = 0, selectedReport = null;
  const params = new URLSearchParams(locationSearch());
  const requested = meta.buildings.find(b => b.id === params.get('building'));
  if (requested) { building = requested.id; const f = Number(params.get('floor')); floor = requested.levels.some(level => level.level === f) ? f : requested.levels[0].level; }
  const stage = $('#map-stage'), image = $('#map-image'), selection = $('#selection-panel'), dialog = $('#report-dialog'), form = $('#report-form');
  let files = [], previewUrls = [], submitting = false;
  meta.categories.forEach(category => option($('#report-category'), category, category));
  function buildingName() { return meta.buildings.find(b => b.id === building).name; }
  function locationName() { return buildingName() + (floor ? ` ${floor}층` : ' 배치도'); }
  function clearFiles() { previewUrls.forEach(url => URL.revokeObjectURL(url)); previewUrls = []; }
  function renderFiles() {
    clearFiles(); const root = $('#file-previews'); root.replaceChildren();
    files.forEach((file, index) => {
      const wrapper = el('div', 'file-preview'), photo = el('img'), remove = el('button', '', '×');
      photo.src = URL.createObjectURL(file); previewUrls.push(photo.src); photo.alt = file.name;
      remove.type = 'button'; remove.setAttribute('aria-label', `${file.name} 첨부 취소`); remove.disabled = submitting;
      remove.addEventListener('click', () => { files.splice(index, 1); renderFiles(); });
      wrapper.append(photo, remove, el('p', '', file.name)); root.append(wrapper);
    });
  }
  function formError(message = '') { const box = $('#form-error'); box.textContent = message; box.hidden = !message; }
  function addFiles(incoming) {
    if (submitting) return;
    const next = Array.from(incoming);
    if (files.length + next.length > 3) return formError('사진은 최대 3장까지 첨부할 수 있습니다.');
    for (const file of next) {
      if (!/\.(jpe?g|png)$/i.test(file.name) || (file.type && !['image/jpeg', 'image/png'].includes(file.type))) return formError('JPG·PNG 사진만 첨부할 수 있습니다.');
      if (file.size === 0 || file.size > 5 * 1024 * 1024) return formError('사진은 0바이트보다 크고 장당 5MB 이하여야 합니다.');
    }
    files.push(...next); formError(); renderFiles();
  }
  function resetForm() { form.reset(); files = []; renderFiles(); formError(); $('#content-count').textContent = '0 / 4,000'; }
  function closeForm() { if (submitting) return; dialog.close(); resetForm(); draft = null; renderSelection(); renderPins(); }
  function openForm() {
    if (!draft) return;
    $('#selected-location').textContent = locationName(); $('#preview-map').src = image.src;
    $('#preview-pin').style.left = `${draft.x * 100}%`; $('#preview-pin').style.top = `${draft.y * 100}%`;
    dialog.showModal(); form.elements.title.focus();
  }
  function renderSelection() {
    selection.replaceChildren();
    if (draft) {
      selection.append(el('div', 'selection-icon', '⌖'), el('h2', '', '신고할 위치를 선택했어요'), el('p', '', locationName()), el('p', '', '다른 곳을 누르면 핀을 옮길 수 있어요.'));
      const actions = el('div', 'selection-actions'), write = el('button', 'button primary', '신고 작성'), cancel = el('button', 'button secondary', '취소');
      write.type = cancel.type = 'button'; write.addEventListener('click', openForm); cancel.addEventListener('click', () => { draft = null; renderSelection(); renderPins(); });
      actions.append(write, cancel); selection.append(actions);
    } else if (selectedReport) {
      selection.append(badge(selectedReport.status), el('h2', 'detail-title', selectedReport.title), el('p', '', `${location(selectedReport)} · ${selectedReport.category}`), el('p', '', date(selectedReport.createdAt)));
      const link = el('a', 'button primary', '상세 보기 ↗'); link.href = `${base}/reports/${selectedReport.id}`; selection.append(link);
    } else {
      selection.append(el('div', 'selection-icon', '⌖'), el('h2', '', '지도로 쉽고 정확하게'), el('p', '', '건물과 층을 선택한 뒤, 불편한 위치를 지도에 표시해 주세요.'));
      if (building === 'CAMPUS') selection.append(el('p', '', '건물 내부는 상단 건물·층 탭에서 선택해 주세요.'));
    }
  }
  function chooseReport(report) { draft = null; selectedReport = report; renderSelection(); renderPins(); }
  function renderPins() {
    const root = $('#pins'); root.replaceChildren();
    for (const report of reports) {
      const marker = pin(report.x, report.y, report.status, `${report.title} · ${report.status}`);
      marker.addEventListener('click', event => { event.stopPropagation(); chooseReport(report); }); root.append(marker);
    }
    if (draft) { const marker = pin(draft.x, draft.y, '', '선택한 위치에서 신고 작성'); marker.addEventListener('click', event => { event.stopPropagation(); openForm(); }); root.append(marker); }
  }
  function selectPosition(x, y) {
    if (stage.classList.contains('busy')) return;
    draft = { x: Math.max(0, Math.min(1, x)), y: Math.max(0, Math.min(1, y)) }; selectedReport = null;
    renderSelection(); renderPins(); $('#map-status').textContent = `${locationName()} 위치를 선택했습니다. 신고 작성 버튼으로 작성할 수 있습니다.`;
  }
  stage.addEventListener('click', event => { if (event.target.closest('.pin')) return; const rect = stage.getBoundingClientRect(); selectPosition((event.clientX - rect.left) / rect.width, (event.clientY - rect.top) / rect.height); });
  stage.addEventListener('keydown', event => {
    if (event.target !== stage) return;
    if (event.key === 'Enter' || event.key === ' ') { event.preventDefault(); if (draft) openForm(); else selectPosition(.5, .5); }
    if (['ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown'].includes(event.key)) {
      event.preventDefault(); const point = draft || { x: .5, y: .5 };
      selectPosition(point.x + (event.key === 'ArrowRight' ? .02 : event.key === 'ArrowLeft' ? -.02 : 0), point.y + (event.key === 'ArrowDown' ? .02 : event.key === 'ArrowUp' ? -.02 : 0));
    }
    if (event.key === 'Escape') { draft = null; renderSelection(); renderPins(); }
  });
  function tabs() {
    const buildings = $('#building-tabs'), floors = $('#floor-tabs'); buildings.replaceChildren(); floors.replaceChildren();
    for (const item of meta.buildings) {
      const button = el('button', '', item.name); button.type = 'button'; button.setAttribute('aria-pressed', String(item.id === building));
      button.addEventListener('click', () => { building = item.id; floor = item.levels[0].level; loadMap().catch(showError); }); buildings.append(button);
    }
    const levels = meta.buildings.find(b => b.id === building).levels;
    for (const level of levels) {
      const f = level.level;
      const button = el('button', '', level.name); button.type = 'button'; button.setAttribute('aria-pressed', String(f === floor));
      button.addEventListener('click', () => { floor = f; loadMap().catch(showError); }); floors.append(button);
    }
  }
  async function loadMap() {
    const version = ++requestVersion; draft = null; selectedReport = null; reports = []; zoom = 100; applyZoom(); tabs(); renderSelection(); renderPins();
    stage.classList.add('busy'); image.src = mapImage(building, floor); image.alt = `${locationName()} 배치도`;
    $('#map-name').textContent = locationName(); $('#location-reports').replaceChildren(el('p', 'muted', '신고를 불러오는 중입니다.'));
    const url = new URL(window.location); url.searchParams.set('building', building); url.searchParams.set('floor', floor); history.replaceState(null, '', url);
    try {
      const items = await api(`/api/pins?building=${encodeURIComponent(building)}&floor=${floor}`); if (version !== requestVersion) return;
      reports = items; $('#pin-count').textContent = items.length; const list = $('#location-reports'); list.replaceChildren();
      if (!items.length) list.append(el('p', 'empty muted', '이 위치에 등록된 신고가 없습니다.'));
      for (const report of items.toReversed()) {
        const button = el('button', 'report-card'); button.type = 'button'; button.append(el('strong', '', report.title));
        const cardMeta = el('span', 'card-meta'); cardMeta.append(el('span', '', date(report.createdAt)), badge(report.status)); button.append(cardMeta);
        button.addEventListener('click', () => chooseReport(report)); list.append(button);
      }
      const requestedReport = Number(params.get('report')); const selected = reports.find(r => r.id === requestedReport); if (selected) chooseReport(selected);
      renderPins(); $('#map-status').textContent = `${locationName()}, 신고 ${items.length}건`;
    } catch (error) { if (version === requestVersion) $('#location-reports').replaceChildren(el('p', 'notice error', error.message)); throw error; }
    finally { if (version === requestVersion) stage.classList.remove('busy'); }
  }
  image.addEventListener('error', () => showError(new Error('배치도 이미지를 불러오지 못했습니다. 페이지를 새로고침해 주세요.')));
  function applyZoom() {
    const scroll = $('#map-scroll'), styles = getComputedStyle(scroll);
    const width = scroll.clientWidth - parseFloat(styles.paddingLeft) - parseFloat(styles.paddingRight);
    stage.style.width = building === 'CAMPUS' ? `${Math.min(width, window.innerHeight * .56 * 1060 / 1015) * zoom / 100}px` : `${zoom}%`;
    $('#zoom-value').textContent = `${zoom}%`;
  }
  window.addEventListener('resize', applyZoom);
  $('#zoom-in').addEventListener('click', () => { zoom = Math.min(250, zoom + 25); applyZoom(); });
  $('#zoom-out').addEventListener('click', () => { zoom = Math.max(100, zoom - 25); applyZoom(); });
  $('#file-input').addEventListener('change', event => { addFiles(event.target.files); event.target.value = ''; });
  const dropZone = $('#drop-zone');
  dropZone.addEventListener('dragover', event => { event.preventDefault(); dropZone.classList.add('dragover'); });
  dropZone.addEventListener('dragleave', () => dropZone.classList.remove('dragover'));
  dropZone.addEventListener('drop', event => { event.preventDefault(); dropZone.classList.remove('dragover'); addFiles(event.dataTransfer.files); });
  $('#dialog-close').addEventListener('click', closeForm); $('#form-cancel').addEventListener('click', closeForm);
  dialog.addEventListener('cancel', event => { event.preventDefault(); closeForm(); });
  form.elements.content.addEventListener('input', event => { $('#content-count').textContent = `${event.target.value.length.toLocaleString()} / 4,000`; });
  form.addEventListener('submit', async event => {
    event.preventDefault(); if (submitting || !draft || !form.reportValidity()) return;
    if (!form.elements.title.value.trim() || !form.elements.content.value.trim()) { formError('제목과 내용을 공백 없이 입력해 주세요.'); return; }
    const payload = new FormData(form); payload.delete('files'); files.forEach(file => payload.append('files', file));
    payload.set('building', building); payload.set('floor', floor); payload.set('x', draft.x); payload.set('y', draft.y);
    submitting = true; formError(); Array.from(form.elements).forEach(control => { control.disabled = true; }); $('#form-submit').textContent = '등록 중…';
    let saved;
    try { saved = await api('/api/reports', { method: 'POST', headers: { 'X-CSRF-Token': meta.csrfToken }, body: payload }); }
    catch (error) { formError(error.message); }
    finally { submitting = false; Array.from(form.elements).forEach(control => { control.disabled = false; }); $('#form-submit').textContent = '신고 등록'; }
    if (saved) {
      closeForm(); toast('신고가 접수되었습니다. 지도에 새 핀이 등록되었어요.');
      try { await loadMap(); const created = reports.find(r => r.id === saved.id); if (created) chooseReport(created); } catch (error) { showError(error); }
    }
  });
  await loadMap();
}
function locationSearch() { return window.location.search; }

async function initList() {
  const form = $('#filter-form'), parameters = new URLSearchParams(window.location.search);
  let version = 0, previewVersion = 0, currentPage = 1;
  meta.statuses.forEach(status => option($('#filter-status'), status, status));
  meta.buildings.forEach(building => option($('#filter-building'), building.id, building.name));
  meta.categories.forEach(category => option($('#filter-category'), category, category));
  function floors() {
    const selected = form.elements.floor.value, select = $('#filter-floor'); select.replaceChildren(new Option('전체 층', ''));
    const building = meta.buildings.find(item => item.id === form.elements.building.value);
    const levels = building ? building.levels : meta.buildings.flatMap(item => item.levels);
    const names = new Map(levels.map(level => [level.level, level.name]));
    for (const [floor, name] of [...names].sort((a,b) => a[0]-b[0])) option(select, floor, name);
    if (Array.from(select.options).some(o => o.value === selected)) select.value = selected;
  }
  for (const [key, value] of parameters) if (form.elements.namedItem(key)) form.elements.namedItem(key).value = value;
  floors(); if (parameters.has('floor')) form.elements.floor.value = parameters.get('floor');
  form.elements.building.addEventListener('change', floors);
  async function load(page = 1) {
    const request = ++version; ++previewVersion; $('#global-error').hidden = true;
    const from = form.elements.from.value, to = form.elements.to.value;
    if (from && to && from > to) { showError(new Error('시작일은 종료일보다 늦을 수 없습니다.')); return; }
    const query = new URLSearchParams(new FormData(form)); for (const key of [...query.keys()]) if (!query.get(key)) query.delete(key);
    query.set('page', String(page)); history.replaceState(null, '', `${base}/reports?${query}`);
    $('#report-rows').replaceChildren(); $('#pagination').replaceChildren(); $('#list-status').textContent = '신고 목록을 불러오는 중입니다.';
    const empty = $('#list-empty'); empty.hidden = false; empty.textContent = '신고 목록을 불러오는 중입니다.';
    $('#detail-preview').replaceChildren(el('p', 'muted', '목록에서 신고를 선택하면 자세한 내용을 볼 수 있습니다.'));
    try {
      const result = await api(`/api/reports?${query}`); if (request !== version) return;
      const lastPage = Math.max(1, Math.ceil(result.total / result.pageSize));
      if (result.page > lastPage) { await load(lastPage); return; }
      currentPage = result.page; $('#report-total').textContent = result.total; empty.hidden = result.items.length > 0;
      empty.textContent = '검색 조건에 맞는 신고가 없습니다. 다른 조건으로 검색하거나 지도에서 새 신고를 등록해 주세요.';
      for (const report of result.items) {
        const row = el('tr'), titleCell = el('td'), link = el('a', '', report.title); link.href = `${base}/reports/${report.id}`;
        link.addEventListener('click', async event => {
          if (event.ctrlKey || event.metaKey || event.shiftKey || window.matchMedia('(max-width:780px)').matches) return;
          event.preventDefault(); const previewRequest = ++previewVersion;
          $('#report-rows').querySelectorAll('tr').forEach(tr => tr.classList.remove('selected')); row.classList.add('selected');
          const preview = $('#detail-preview'); preview.replaceChildren(el('p', 'muted', '신고를 불러오는 중입니다.'));
          try { const data = await api(`/api/reports/${report.id}`); if (previewRequest === previewVersion) renderDetail(preview, data, true); } catch (error) { if (previewRequest === previewVersion) preview.replaceChildren(el('p', 'notice error', error.message)); }
        });
        titleCell.append(link); const statusCell = el('td'); statusCell.append(badge(report.status));
        row.append(el('td', '', report.id), titleCell, el('td', '', location(report)), el('td', '', report.category), el('td', '', date(report.createdAt)), statusCell); $('#report-rows').append(row);
      }
      const pagination = $('#pagination');
      function pageButton(label, target, disabled = false) { const button = el('button', '', label); button.type = 'button'; button.disabled = disabled; button.addEventListener('click', () => load(target).catch(showError)); pagination.append(button); return button; }
      if (result.total > 0) {
        pageButton('이전', currentPage - 1, currentPage === 1);
        const start = Math.max(1, Math.min(currentPage - 2, lastPage - 4));
        for (let page = start; page <= Math.min(lastPage, start + 4); page++) { const button = pageButton(String(page), page); button.setAttribute('aria-label', `${page}페이지`); if (page === currentPage) button.setAttribute('aria-current', 'page'); }
        pageButton('다음', currentPage + 1, currentPage === lastPage);
      }
      $('#list-status').textContent = `전체 ${result.total}건, ${currentPage}페이지`;
    } catch (error) { if (request === version) { empty.textContent = '신고를 불러오지 못했습니다. 검색 버튼으로 다시 시도해 주세요.'; showError(error); } }
  }
  form.addEventListener('submit', event => { event.preventDefault(); load(1).catch(showError); });
  form.addEventListener('change', event => { if (event.target.tagName === 'SELECT') load(1).catch(showError); });
  $('#filter-reset').addEventListener('click', () => { form.reset(); floors(); load(1).catch(showError); });
  const page = Number(parameters.get('page')) || 1; await load(Number.isInteger(page) && page > 0 ? page : 1);
}

async function start() {
  const page = document.body.dataset.page;
  $(`[data-nav="${page === 'map' ? 'map' : 'list'}"]`).classList.add('active');
  $(`[data-nav="${page === 'map' ? 'map' : 'list'}"]`).setAttribute('aria-current', 'page');
  meta = await api('/api/meta');
  if (page === 'map') await initMap();
  else if (page === 'list') await initList();
  else if (page === 'detail') { const data = await api(`/api/reports/${document.body.dataset.reportId}`); renderDetail($('#report-detail'), data); document.title = `${data.report.title} · FixMap`; }
}
start().catch(showError);
