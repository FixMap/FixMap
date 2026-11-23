import { readBuildings, readPlaces, readPins } from './api.js';
import { $, el, pin, badge, statusName } from './dom.js';
import { drawPlaces, fitPlaceLabels, placeAt } from './places.js';
import { setupReportForm } from './report-form.js';
import { setupReportDetail } from './report-detail.js';

async function start() {
  const buildings = (await readBuildings()).filter(building => Array.isArray(building.floors) && building.floors.length);
  if (!buildings.length) throw new Error('등록된 건물과 층이 없습니다.');
  const stage = $('#map-stage'), viewport = $('#map-scroll'), form = $('#report-form');
  const buildingTabs = $('#building-tabs'), floorTabs = $('#floor-tabs');
  const requestedLevel = Number(document.body.dataset.level);
  const requestedBuilding = new URLSearchParams(location.search).get('building');
  let building = buildings.find(item => item.code === requestedBuilding)
    || buildings.find(item => item.floors.some(floor => floor.level === requestedLevel))
    || buildings[0];
  let floor = floorFor(building, requestedLevel);
  const MIN_ZOOM = 100, MAX_ZOOM = 400;
  let view, draft = null, selected = null, reports = [], version = 0, zoom = MIN_ZOOM;
  const detail = setupReportDetail(close);
  const composer = setupReportForm({
    onCancel: close, getPosition: () => draft,
    onBusy(value) {
      stage.inert = value;
      document.querySelectorAll('#map-controls button').forEach(button => { button.disabled = value; });
    },
    async onCreated(created) {
      draft = null;
      $('#action-status').textContent = '신고 #' + created.id + '이 접수되었습니다.'; $('#action-status').hidden = false;
      const loaded = loadLevel();
      const request = version;
      await loaded;
      const report = reports.find(item => item.id === created.id);
      if (request === version && report) chooseReport(report);
    }
  });
  const locationName = () => building.name + ' ' + floor.name;
  function floorFor(item, level) {
    return item.floors.find(floor => floor.level === level) || item.floors[0];
  }
  function mode(value) { $('#location-panel').hidden = value !== 'list'; $('#selection-panel').hidden = value !== 'detail'; form.hidden = value !== 'write'; }
  function close() {
    if (composer.busy || (draft && !composer.canDiscard())) return;
    draft = null; selected = null; detail.reset(); composer.reset(); mode('list'); renderPins(); stage.focus({ preventScroll: true });
  }
  function chooseReport(report) {
    if (composer.busy || (draft && !composer.canDiscard())) return;
    draft = null; composer.reset(); selected = report.id; mode('detail'); renderPins(); detail.show(report);
    if (matchMedia('(max-width:980px)').matches) $('.campus-sidebar').scrollIntoView({ block: 'start' });
  }
  function renderPins() {
    if (!view?.ready) return;
    view.pins.replaceChildren();
    for (const report of reports) {
      const marker = pin(report, report.title + ', ' + statusName(report.status));
      marker.dataset.reportId = report.id; marker.classList.toggle('selected', selected === report.id);
      marker.addEventListener('click', event => { event.stopPropagation(); chooseReport(report); }); view.pins.append(marker);
    }
    if (draft) {
      const marker = pin(draft, '선택한 위치', true);
      marker.addEventListener('click', event => { event.stopPropagation(); form.elements.title.focus(); }); view.pins.append(marker);
    }
  }
  function selectPosition(x, y, focus = false) {
    if (composer.busy || stage.classList.contains('busy') || !view?.ready) return;
    const first = !draft; if (first) composer.reset();
    draft = { floorId: floor.id, x: Math.max(0, Math.min(1, x)), y: Math.max(0, Math.min(1, y)) };
    const place = placeAt(view.places, draft.x, draft.y);
    selected = null; detail.reset(); mode('write'); composer.open(locationName() + (place ? ' · ' + place.name : '')); renderPins();
    $('#action-status').hidden = true; $('#map-status').textContent = locationName() + ' 위치를 선택했습니다.';
    if (first && matchMedia('(max-width:980px)').matches) $('.campus-sidebar').scrollIntoView({ block: 'start' });
    if (focus) form.elements.title.focus();
  }
  stage.addEventListener('click', event => {
    if (event.target.closest('.pin')) return;
    const map = event.target.closest('.building-map'); if (!map) return;
    const rect = map.getBoundingClientRect(); selectPosition((event.clientX - rect.left) / rect.width, (event.clientY - rect.top) / rect.height);
  });
  stage.addEventListener('keydown', event => {
    if (event.target.closest('.pin')) return;
    const point = draft || { x: .5, y: .5 };
    if (event.key === 'Enter' || event.key === ' ') { event.preventDefault(); selectPosition(point.x, point.y, true); }
    if (['ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown'].includes(event.key)) {
      event.preventDefault(); selectPosition(point.x + (event.key === 'ArrowRight' ? .02 : event.key === 'ArrowLeft' ? -.02 : 0), point.y + (event.key === 'ArrowDown' ? .02 : event.key === 'ArrowUp' ? -.02 : 0));
    }
    if (event.key === 'Escape') close();
  });
  function selectionButton(group, name, active, select) {
    const button = el('button', '', name);
    button.type = 'button';
    button.setAttribute('aria-pressed', String(active));
    button.addEventListener('click', () => {
      if (composer.busy || active || (draft && !composer.canDiscard())) return;
      select();
      loadLevel();
      group.querySelector('[aria-pressed="true"]').focus({ preventScroll: true });
    });
    group.append(button);
    return button;
  }
  function tabs() {
    buildingTabs.replaceChildren();
    for (const item of buildings) {
      selectionButton(buildingTabs, item.name, item === building, () => {
        building = item;
        floor = floorFor(item, floor.level);
      }).dataset.building = item.code;
    }
    floorTabs.replaceChildren();
    for (const item of building.floors) {
      selectionButton(floorTabs, item.name, item === floor, () => { floor = item; }).dataset.level = item.level;
    }
  }
  function buildMap() {
    view = null;
    stage.replaceChildren();
    if (![floor.mapWidth, floor.mapHeight].every(value => Number.isFinite(value) && value > 0)) throw new Error('지도 크기 데이터 형식을 확인해 주세요.');
    const map = el('div', 'building-map'), pins = el('div', 'building-pins');
    map.dataset.buildingId = building.id; map.dataset.floorId = floor.id; map.tabIndex = 0;
    map.setAttribute('role', 'group');
    map.setAttribute('aria-label', locationName() + ' 지도. 방향키로 위치 이동, Enter로 신고 작성');
    map.style.aspectRatio = floor.mapWidth + ' / ' + floor.mapHeight;
    map.append(el('p', 'map-loading muted', '영역을 불러오는 중입니다.'), pins);
    stage.append(map);
    view = { map, pins, places: [], ready: false };
  }
  function rows() {
    const list = $('#location-reports'); list.replaceChildren(); $('#pin-count').textContent = reports.length;
    if (!reports.length) list.append(el('p', 'empty muted', '이 층에 등록된 신고가 없습니다.'));
    for (const report of reports) {
      const button = el('button', 'report-card'); button.type = 'button'; button.title = report.title;
      button.append(el('strong', '', report.title), el('span', 'card-meta', report.location + ' · ' + report.category), badge(report.status));
      button.addEventListener('click', () => chooseReport(report)); list.append(button);
    }
  }
  async function loadLevel() {
    const request = ++version; draft = null; selected = null; reports = []; zoom = MIN_ZOOM; detail.reset(); composer.reset(); mode('list'); tabs();
    try {
      buildMap(); applyZoom(); viewport.scrollTo(0, 0); stage.classList.add('busy'); stage.setAttribute('aria-busy', 'true');
      $('#map-name').textContent = locationName(); $('#pin-count').textContent = '…'; $('#location-reports').replaceChildren(el('p', 'empty muted', '지도와 신고를 불러오는 중입니다.'));
      const url = new URL(location.href);
      url.searchParams.delete('floor');
      url.searchParams.set('level', floor.level);
      url.searchParams.set('building', building.code);
      history.replaceState(null, '', url);
      const [pins, areas] = await Promise.all([readPins(floor.level), readPlaces(floor.id)]);
      if (request !== version) return;
      reports = pins.filter(report => report.floorId === floor.id).map(report => {
        if (!Number.isFinite(report.posX) || !Number.isFinite(report.posY) || report.posX < 0 || report.posX > 1 || report.posY < 0 || report.posY > 1) throw new Error('신고 위치 데이터 형식을 확인해 주세요.');
        return { ...report, x: report.posX, y: report.posY, location: locationName() };
      });
      drawPlaces(view.map, areas);
      view.map.querySelector('.map-loading').remove();
      view.places = areas;
      view.ready = true;
      fitPlaceLabels(view.map);
      document.fonts.ready.then(() => {
        if (request === version) fitPlaceLabels(view.map);
      });
      rows(); renderPins(); $('#map-status').textContent = locationName() + ' 신고 ' + reports.length + '건';
    } catch (error) {
      if (request !== version) return; $('#pin-count').textContent = '—';
      if (view && !view.ready) view.map.replaceChildren(el('p', 'map-unavailable', '영역을 불러오지 못했습니다.'));
      const retry = el('button', 'text-button', '다시 불러오기'); retry.type = 'button'; retry.addEventListener('click', loadLevel);
      $('#location-reports').replaceChildren(el('p', 'notice error', error.message), retry);
    } finally { if (request === version) { stage.classList.remove('busy'); stage.setAttribute('aria-busy', 'false'); } }
  }
  function applyZoom() {
    if (!view) return;
    const style = getComputedStyle(viewport);
    const width = viewport.offsetWidth - parseFloat(style.paddingLeft) - parseFloat(style.paddingRight);
    const height = viewport.offsetHeight - parseFloat(style.paddingTop) - parseFloat(style.paddingBottom);
    const fit = Math.min(width, height * floor.mapWidth / floor.mapHeight);
    // Keep room labels usable on phones. The map can scroll inside its card.
    stage.style.width = Math.max(fit, 640) * zoom / 100 + 'px';
    $('.campus-card').classList.toggle('map-can-scroll', stage.offsetWidth > width);
    $('#zoom-value').textContent = zoom + '%';
    if (view.ready) scheduleFit();
  }
  let fitFrame = 0;
  function scheduleFit() {
    if (fitFrame) return;
    fitFrame = requestAnimationFrame(() => { fitFrame = 0; if (view?.ready) fitPlaceLabels(view.map); });
  }
  function setZoom(next, clientX, clientY) {
    next = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, Math.round(next)));
    if (next === zoom || !view) return;
    const before = stage.getBoundingClientRect(), oldWidth = stage.offsetWidth;
    zoom = next; applyZoom();
    if (clientX === undefined) return;
    // Keep the point under the cursor where it was.
    const ratio = stage.offsetWidth / oldWidth, after = stage.getBoundingClientRect();
    viewport.scrollLeft += after.left + (clientX - before.left) * ratio - clientX;
    viewport.scrollTop += after.top + (clientY - before.top) * ratio - clientY;
  }
  addEventListener('resize', applyZoom);
  $('#zoom-in').addEventListener('click', () => setZoom(zoom + 25));
  $('#zoom-out').addEventListener('click', () => setZoom(zoom - 25));
  // Drag to pan. A drag must not select the labels or count as a click that places a pin.
  let drag = null, dragged = false;
  viewport.addEventListener('pointerdown', event => {
    dragged = false;
    if (event.button !== 0 || event.pointerType === 'touch' || event.target.closest('.pin') || composer.busy) return;
    drag = { id: event.pointerId, x: event.clientX, y: event.clientY, left: viewport.scrollLeft, top: viewport.scrollTop };
  });
  viewport.addEventListener('pointermove', event => {
    if (!drag || event.pointerId !== drag.id) return;
    const dx = event.clientX - drag.x, dy = event.clientY - drag.y;
    if (!dragged) {
      if (Math.hypot(dx, dy) < 5) return;
      dragged = true; viewport.setPointerCapture(drag.id); viewport.classList.add('panning');
    }
    viewport.scrollLeft = drag.left - dx; viewport.scrollTop = drag.top - dy;
  });
  const endDrag = () => { drag = null; viewport.classList.remove('panning'); };
  viewport.addEventListener('pointerup', endDrag);
  viewport.addEventListener('pointercancel', endDrag);
  viewport.addEventListener('click', event => { if (dragged) { event.stopPropagation(); dragged = false; } }, true);
  viewport.addEventListener('dragstart', event => event.preventDefault());
  viewport.addEventListener('wheel', event => {
    if (composer.busy || stage.classList.contains('busy') || !view) return;
    event.preventDefault();
    const delta = event.deltaMode === 1 ? event.deltaY * 16 : event.deltaY;
    setZoom(zoom * Math.exp(-Math.max(-120, Math.min(120, delta)) * .0015), event.clientX, event.clientY);
  }, { passive: false });
  await loadLevel();
}

start().catch(error => {
  $('#global-error').textContent = error.message; $('#global-error').hidden = false; $('#pin-count').textContent = '—';
    $('#map-name').textContent = '학교 배치도'; $('#location-reports').replaceChildren(el('p', 'empty muted', '지금은 신고 목록을 불러올 수 없습니다. 페이지를 새로고침해 주세요.'));
});
