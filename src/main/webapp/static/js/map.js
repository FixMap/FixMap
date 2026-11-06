// 메인 지도: 건물·층 탭 → 배치도 표시 → 신고 핀 표시 → 클릭으로 새 핀 위치 계산
(() => {
  const ctx = document.body.dataset.ctx;
  const buildingTabs = document.getElementById('building-tabs');
  const floorTabs = document.getElementById('floor-tabs');
  const mapImage = document.getElementById('map-image');
  const pinLayer = document.getElementById('pin-layer');
  const reportList = document.getElementById('report-list');

  let buildings = [];
  let currentFloor = null;

  async function getJson(url) {
    const res = await fetch(ctx + url);
    if (!res.ok) throw new Error(`${url} 요청 실패 (${res.status})`);
    return res.json();
  }

  function renderTabs(container, items, activeId, onClick) {
    container.innerHTML = '';
    items.forEach(item => {
      const btn = document.createElement('button');
      btn.textContent = item.name;
      btn.className = item.id === activeId ? 'active' : '';
      btn.addEventListener('click', () => onClick(item));
      container.appendChild(btn);
    });
  }

  function selectBuilding(building) {
    renderTabs(buildingTabs, buildings, building.id, selectBuilding);
    if (building.floors.length > 0) selectFloor(building, building.floors[0]);
  }

  async function selectFloor(building, floor) {
    currentFloor = floor;
    renderTabs(floorTabs, building.floors, floor.id, f => selectFloor(building, f));
    mapImage.src = floor.mapImageUrl;
    const pins = await getJson(`/api/reports/map?floor=${floor.id}`);
    pinLayer.innerHTML = '';
    reportList.innerHTML = '';
    pins.forEach(p => {
      addPin(p.posX, p.posY, p.status, p.title);
      const li = document.createElement('li');
      li.textContent = `${p.title} (${p.status})`;
      reportList.appendChild(li);
    });
  }

  // 좌표는 0~1 비율이라 이미지 크기가 바뀌어도 같은 자리에 표시된다
  function addPin(x, y, status, title) {
    const pin = document.createElement('div');
    pin.className = `pin ${status}`;
    pin.style.left = `${x * 100}%`;
    pin.style.top = `${y * 100}%`;
    pin.title = title || '';
    pinLayer.appendChild(pin);
    return pin;
  }

  let newPin = null;
  mapImage.addEventListener('click', e => {
    const rect = mapImage.getBoundingClientRect();
    const x = ((e.clientX - rect.left) / rect.width).toFixed(4);
    const y = ((e.clientY - rect.top) / rect.height).toFixed(4);
    if (newPin) newPin.remove();
    newPin = addPin(x, y, 'NEW', '새 신고 위치');
    // TODO: 신고 작성 모달 열기 (floorId: currentFloor.id, posX: x, posY: y)
    console.log('새 핀', { floorId: currentFloor && currentFloor.id, posX: x, posY: y });
  });

  getJson('/api/buildings')
    .then(data => {
      buildings = data;
      if (buildings.length > 0) selectBuilding(buildings[0]);
    })
    .catch(err => console.error(err));
})();
