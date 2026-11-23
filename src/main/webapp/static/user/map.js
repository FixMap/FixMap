import { readBuildings, readPlaces, readPins } from './api.js';
import { $, el, pin, badge, statusName } from './dom.js';
import { drawPlaces, placeAt } from './places.js';
import { setupReportForm } from './report-form.js';
import { setupReportDetail } from './report-detail.js';

async function start() {
  const buildings = await readBuildings();
  if (buildings.some(building => !Array.isArray(building.floors))) throw new Error('건물·층 데이터 형식을 확인해 주세요.');
  const levels = [...new Set(buildings.flatMap(building => building.floors.map(floor => floor.level)))].sort((a, b) => a - b);
  if (!levels.length) throw new Error('등록된 건물과 층이 없습니다.');
  const stage = $('#map-stage'), views = new Map(), form = $('#report-form');
  const requestedLevel = Number(document.body.dataset.level);
  let level = levels.includes(requestedLevel) ? requestedLevel : levels[0];
  let draft = null, selected = null, reports = [], version = 0, zoom = 100;
  const detail = setupReportDetail(close);
  const composer = setupReportForm({
    onCancel: close, getPosition: () => draft,
    onBusy(value) { stage.inert = value; $('#floor-tabs').querySelectorAll('button').forEach(button => { button.disabled = value; }); },
    async onCreated(created) {
      draft = null;
      $('#action-status').textContent = '신고 #' + created.id + '이 접수되었습니다.'; $('#action-status').hidden = false;
      await loadLevel();
      const report = reports.find(item => item.id === created.id); if (report) chooseReport(report);
    }
  });
  const locationName = view => view.building.name + ' ' + view.floor.name;
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
    views.forEach(view => view.pins.replaceChildren());
    for (const report of reports) {
      const view = views.get(String(report.floorId)); if (!view?.ready) continue;
      const marker = pin(report, report.title + ', ' + statusName(report.status));
      marker.dataset.reportId = report.id; marker.classList.toggle('selected', selected === report.id);
      marker.addEventListener('click', event => { event.stopPropagation(); chooseReport(report); }); view.pins.append(marker);
    }
    if (draft) {
      const marker = pin(draft, '선택한 위치', true);
      marker.addEventListener('click', event => { event.stopPropagation(); form.elements.title.focus(); }); views.get(String(draft.floorId)).pins.append(marker);
    }
  }
  function selectPosition(floorId, x, y, focus = false) {
    const view = views.get(floorId);
    if (composer.busy || stage.classList.contains('busy') || !view?.ready) return;
    const first = !draft; if (first) composer.reset();
    draft = { floorId: view.floor.id, x: Math.max(0, Math.min(1, x)), y: Math.max(0, Math.min(1, y)) };
    const place = placeAt(view.places, draft.x, draft.y);
    selected = null; detail.reset(); mode('write'); composer.open(locationName(view) + (place ? ' · ' + place.name : '')); renderPins();
    $('#action-status').hidden = true; $('#map-status').textContent = locationName(view) + ' 위치를 선택했습니다.';
    if (first && matchMedia('(max-width:980px)').matches) $('.campus-sidebar').scrollIntoView({ block: 'start' });
    if (focus) form.elements.title.focus();
  }
  stage.addEventListener('click', event => {
    if (event.target.closest('.pin')) return;
    const map = event.target.closest('.building-map'); if (!map) return;
    const rect = map.getBoundingClientRect(); selectPosition(map.dataset.floorId, (event.clientX - rect.left) / rect.width, (event.clientY - rect.top) / rect.height);
  });
  stage.addEventListener('keydown', event => {
    if (event.target.closest('.pin')) return;
    const id = event.target.closest('.building-map')?.dataset.floorId || views.keys().next().value; if (!id) return;
    const point = String(draft?.floorId) === id ? draft : { x: .5, y: .5 };
    if (event.key === 'Enter' || event.key === ' ') { event.preventDefault(); selectPosition(id, point.x, point.y, true); }
    if (['ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown'].includes(event.key)) {
      event.preventDefault(); selectPosition(id, point.x + (event.key === 'ArrowRight' ? .02 : event.key === 'ArrowLeft' ? -.02 : 0), point.y + (event.key === 'ArrowDown' ? .02 : event.key === 'ArrowUp' ? -.02 : 0));
    }
    if (event.key === 'Escape') close();
  });
  function tabs() {
    $('#floor-tabs').replaceChildren();
    for (const value of levels) {
      const button = el('button', '', value + 'F'); button.type = 'button'; button.dataset.level = value; button.setAttribute('aria-pressed', String(value === level));
      button.addEventListener('click', () => { if (composer.busy || value === level || (draft && !composer.canDiscard())) return; level = value; loadLevel(); }); $('#floor-tabs').append(button);
    }
  }
  function buildMap() {
    views.clear(); stage.replaceChildren();
    const visible = buildings.filter(building => building.floors.some(floor => floor.level === level));
    stage.classList.toggle('single-building', visible.length === 1);
    visible.forEach(building => {
      const slot = buildings.indexOf(building), floor = building.floors.find(floor => floor.level === level);
      if (![floor.mapWidth, floor.mapHeight].every(value => Number.isFinite(value) && value > 0)) throw new Error('지도 크기 데이터 형식을 확인해 주세요.');
      const section = el('section', 'campus-building'), heading = el('h3', '', building.name), map = el('div', 'building-map'), pins = el('div', 'building-pins');
      // shortcut: 학교 세 건물의 고정 배치다. 건물이 추가되면 배치 규칙을 확장한다.
      const width = Math.min(floor.mapWidth, [468, 344, 482][slot]) / 1108 * 100 + '%';
      section.dataset.position = slot; section.style.width = width; section.style.setProperty('--building-width', width);
      heading.append(el('small', '', floor.name)); map.dataset.buildingId = building.id; map.dataset.floorId = floor.id; map.tabIndex = 0;
      map.setAttribute('role', 'group'); map.setAttribute('aria-label', building.name + ' ' + floor.name + ' 신고 위치 지도');
      map.style.aspectRatio = floor.mapWidth + ' / ' + floor.mapHeight;
      map.append(el('p', 'map-loading muted', '영역을 불러오는 중입니다.'), pins);
      section.append(heading, map); stage.append(section); views.set(String(floor.id), { building, floor, map, pins, places: [], ready: false });
    });
    const yard = el('div', 'campus-yard'); yard.append(el('span', '', '운동장')); stage.append(yard, el('div', 'campus-gate', '정문'));
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
    const request = ++version; draft = null; selected = null; reports = []; zoom = 100; detail.reset(); composer.reset(); mode('list'); tabs();
    try {
      buildMap(); applyZoom(); stage.classList.add('busy'); stage.setAttribute('aria-busy', 'true');
      $('#map-name').textContent = level + 'F · 전체 건물'; $('#pin-count').textContent = '…'; $('#location-reports').replaceChildren(el('p', 'empty muted', '지도와 신고를 불러오는 중입니다.'));
      const url = new URL(location.href); url.searchParams.delete('floor'); url.searchParams.set('level', level); history.replaceState(null, '', url);
      const currentViews = Array.from(views.values());
      const [pins, areas] = await Promise.all([readPins(level), Promise.all(currentViews.map(view => readPlaces(view.floor.id)))]);
      if (request !== version) return;
      currentViews.forEach((view, index) => {
        drawPlaces(view.map, areas[index]); view.map.querySelector('.map-loading').remove(); view.places = areas[index]; view.ready = true;
      });
      reports = pins.map(report => {
        const view = views.get(String(report.floorId));
        if (!view || !Number.isFinite(report.posX) || !Number.isFinite(report.posY) || report.posX < 0 || report.posX > 1 || report.posY < 0 || report.posY > 1) throw new Error('신고 위치 데이터 형식을 확인해 주세요.');
        return { ...report, x: report.posX, y: report.posY, location: locationName(view) };
      });
      rows(); renderPins(); $('#map-status').textContent = level + '층 신고 ' + reports.length + '건';
    } catch (error) {
      if (request !== version) return; $('#pin-count').textContent = '—';
      views.forEach(view => { if (!view.ready) view.map.replaceChildren(el('p', 'map-unavailable', '영역을 불러오지 못했습니다.')); });
      const retry = el('button', 'text-button', '다시 불러오기'); retry.type = 'button'; retry.addEventListener('click', loadLevel);
      $('#location-reports').replaceChildren(el('p', 'notice error', error.message), retry);
    } finally { if (request === version) { stage.classList.remove('busy'); stage.setAttribute('aria-busy', 'false'); } }
  }
  function applyZoom() {
    const viewport = $('#map-scroll'), style = getComputedStyle(viewport);
    const width = viewport.clientWidth - parseFloat(style.paddingLeft) - parseFloat(style.paddingRight), height = viewport.clientHeight - parseFloat(style.paddingTop) - parseFloat(style.paddingBottom);
    stage.style.width = Math.min(width, height * 1108 / 410) * zoom / 100 + 'px'; $('#zoom-value').textContent = zoom + '%';
  }
  addEventListener('resize', applyZoom);
  $('#zoom-in').addEventListener('click', () => { zoom = Math.min(250, zoom + 25); applyZoom(); });
  $('#zoom-out').addEventListener('click', () => { zoom = Math.max(100, zoom - 25); applyZoom(); });
  await loadLevel();
}

start().catch(error => {
  $('#global-error').textContent = error.message; $('#global-error').hidden = false; $('#pin-count').textContent = '—';
  $('#map-name').textContent = '학교 배치도'; $('#location-reports').replaceChildren(el('p', 'empty muted', '지금은 신고 목록을 불러올 수 없습니다. 페이지를 새로고침해 주세요.'));
});
