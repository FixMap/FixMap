import { readBuildings, readPins } from './api.js';
import { $, el, pin, badge, statusName } from './dom.js';
import { setupReportForm } from './report-form.js';

async function start() {
  const buildings = await readBuildings();
  if (buildings.some(building => !Array.isArray(building.floors))) throw new Error('건물·층 데이터 형식을 확인해 주세요.');
  const levels = [...new Set(buildings.flatMap(building => building.floors.map(floor => floor.level)))].sort((a, b) => a - b);
  if (!levels.length) throw new Error('등록된 건물과 층이 없습니다.');
  const stage = $('#map-stage'), views = new Map(), form = $('#report-form');
  let level = levels.includes(Number(new URLSearchParams(location.search).get('floor'))) ? Number(new URLSearchParams(location.search).get('floor')) : levels[0];
  let draft = null, selected = null, reports = [], version = 0, zoom = 100;
  const composer = setupReportForm(close);
  const locationName = building => building.name + ' ' + level + 'F';
  function mode(value) { $('#location-panel').hidden = value !== 'list'; $('#selection-panel').hidden = value !== 'detail'; form.hidden = value !== 'write'; }
  function close() { draft = null; selected = null; composer.reset(); mode('list'); renderPins(); stage.focus({ preventScroll: true }); }
  function chooseReport(report) {
    if (draft && !composer.canDiscard()) return;
    draft = null; composer.reset(); selected = report.id; mode('detail'); renderPins();
    const back = el('button', 'text-button panel-back', '이 층의 신고로 돌아가기'); back.type = 'button'; back.addEventListener('click', close);
    const heading = el('h2', 'detail-title', report.title), info = el('dl', 'pin-info');
    for (const [label, value] of [['신고 번호', '#' + report.id], ['위치', report.location], ['처리 상태', statusName(report.status)]]) {
      const item = el('div'); item.append(el('dt', '', label), el('dd', '', value)); info.append(item);
    }
    $('#selection-panel').replaceChildren(back, heading, badge(report.status), info, el('p', 'panel-hint', '상세 내용과 첨부사진은 아직 조회할 수 없습니다.'));
  }
  function renderPins() {
    views.forEach(view => view.pins.replaceChildren());
    for (const report of reports) {
      const view = views.get(report.buildingId); if (!view?.image) continue;
      const marker = pin(report, report.title + ', ' + statusName(report.status));
      marker.dataset.reportId = report.id; marker.classList.toggle('selected', selected === report.id);
      marker.addEventListener('click', event => { event.stopPropagation(); chooseReport(report); }); view.pins.append(marker);
    }
    if (draft) {
      const marker = pin(draft, '선택한 위치', true);
      marker.addEventListener('click', event => { event.stopPropagation(); form.elements.title.focus(); }); views.get(draft.buildingId).pins.append(marker);
    }
  }
  function selectPosition(buildingId, x, y, focus = false) {
    const view = views.get(buildingId);
    if (stage.classList.contains('busy') || !view?.image?.complete || !view.image.naturalWidth) return;
    const first = !draft; if (first) composer.reset();
    draft = { buildingId, floorId: view.floor.id, x: Math.max(0, Math.min(1, x)), y: Math.max(0, Math.min(1, y)) };
    selected = null; mode('write'); composer.open(locationName(view.building)); renderPins();
    $('#map-status').textContent = locationName(view.building) + ' 위치를 선택했습니다. 신고 접수는 아직 지원하지 않습니다.';
    if (first && matchMedia('(max-width:980px)').matches) $('.campus-sidebar').scrollIntoView({ block: 'start' });
    if (focus) form.elements.title.focus();
  }
  stage.addEventListener('click', event => {
    if (event.target.closest('.pin')) return;
    const map = event.target.closest('.building-map'); if (!map) return;
    const rect = map.getBoundingClientRect(); selectPosition(map.dataset.buildingId, (event.clientX - rect.left) / rect.width, (event.clientY - rect.top) / rect.height);
  });
  stage.addEventListener('keydown', event => {
    if (event.target.closest('.pin')) return;
    const id = event.target.closest('.building-map')?.dataset.buildingId || views.keys().next().value; if (!id) return;
    const point = draft?.buildingId === id ? draft : { x: .5, y: .5 };
    if (event.key === 'Enter' || event.key === ' ') { event.preventDefault(); selectPosition(id, point.x, point.y, true); }
    if (['ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown'].includes(event.key)) {
      event.preventDefault(); selectPosition(id, point.x + (event.key === 'ArrowRight' ? .02 : event.key === 'ArrowLeft' ? -.02 : 0), point.y + (event.key === 'ArrowDown' ? .02 : event.key === 'ArrowUp' ? -.02 : 0));
    }
    if (event.key === 'Escape' && composer.canDiscard()) close();
  });
  function tabs() {
    $('#floor-tabs').replaceChildren();
    for (const value of levels) {
      const button = el('button', '', value + 'F'); button.type = 'button'; button.dataset.floor = value; button.setAttribute('aria-pressed', String(value === level));
      button.addEventListener('click', () => { if (value === level || (draft && !composer.canDiscard())) return; level = value; loadLevel(); }); $('#floor-tabs').append(button);
    }
  }
  function buildMap() {
    views.clear(); stage.replaceChildren();
    const visible = buildings.filter(building => building.floors.some(floor => floor.level === level));
    stage.classList.toggle('single-building', visible.length === 1);
    visible.forEach(building => {
      const slot = buildings.indexOf(building), floor = building.floors.find(floor => floor.level === level);
      const section = el('section', 'campus-building'), heading = el('h3', '', building.name), map = el('div', 'building-map'), pins = el('div', 'building-pins');
      section.dataset.position = slot; section.style.setProperty('--building-width', [42.24, 31.05, 43.5][slot] + '%');
      heading.append(el('small', '', level + 'F')); map.dataset.buildingId = String(building.id); map.tabIndex = 0; map.setAttribute('role', 'group'); map.setAttribute('aria-label', locationName(building) + ' 신고 위치 지도');
      let image = null;
      if (floor.mapImageUrl) {
        const url = new URL(floor.mapImageUrl, location.origin);
        if (url.origin !== location.origin) throw new Error('배치도 주소를 확인해 주세요.');
        image = el('img'); image.src = url.href; image.alt = locationName(building) + ' 배치도'; image.draggable = false;
        function fit() {
          const ratio = image.naturalWidth / image.naturalHeight;
          map.style.width = Math.min(1, [139, 143, 104][slot] * ratio / [468, 344, 482][slot]) * 100 + '%';
        }
        image.addEventListener('load', fit);
        image.addEventListener('error', () => { map.replaceChildren(el('p', 'map-unavailable', '도면을 불러오지 못했습니다.')); });
        map.append(image, pins);
      } else map.append(el('p', 'map-unavailable', '도면을 불러올 수 없습니다.'));
      section.append(heading, map); stage.append(section); views.set(String(building.id), { building, floor, image, pins });
    });
    const yard = el('div', 'campus-yard'); yard.append(el('span', '', '운동장')); stage.append(yard, el('div', 'campus-gate', '정문'));
    return visible;
  }
  function rows() {
    const list = $('#location-reports'); list.replaceChildren(); $('#pin-count').textContent = reports.length;
    if (!reports.length) list.append(el('p', 'empty muted', '이 층에 등록된 신고가 없습니다.'));
    for (const report of reports) {
      const button = el('button', 'report-card'); button.type = 'button'; button.title = report.title;
      button.append(el('strong', '', report.title), el('span', 'card-meta', report.location), badge(report.status));
      button.addEventListener('click', () => chooseReport(report)); list.append(button);
    }
  }
  async function loadLevel() {
    const request = ++version; draft = null; selected = null; reports = []; zoom = 100; composer.reset(); mode('list'); tabs();
    try {
      const visible = buildMap(); applyZoom(); stage.classList.add('busy'); stage.setAttribute('aria-busy', 'true');
      $('#map-name').textContent = level + 'F · 전체 건물'; $('#pin-count').textContent = '…'; $('#location-reports').replaceChildren(el('p', 'empty muted', '신고를 불러오는 중입니다.'));
      const url = new URL(location.href); url.searchParams.set('floor', level); history.replaceState(null, '', url);
      const groups = await Promise.all(visible.map(async building => {
        const floor = views.get(String(building.id)).floor;
        return (await readPins(floor.id)).map(report => {
          if (!Number.isFinite(report.posX) || !Number.isFinite(report.posY) || report.posX < 0 || report.posX > 1 || report.posY < 0 || report.posY > 1) throw new Error('신고 위치 데이터 형식을 확인해 주세요.');
          return { id: report.id, title: report.title, status: report.status, x: report.posX, y: report.posY, buildingId: String(building.id), location: locationName(building) };
        });
      }));
      if (request !== version) return; reports = groups.flat(); rows(); renderPins(); $('#map-status').textContent = level + '층 신고 ' + reports.length + '건';
    } catch (error) {
      if (request !== version) return; $('#pin-count').textContent = '—';
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
