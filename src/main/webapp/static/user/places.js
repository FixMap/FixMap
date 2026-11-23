import { el } from './dom.js';

export function drawPlaces(map, places) {
  const areas = el('div', 'floor-plan');
  for (const place of places) {
    if (![place.x, place.y, place.width, place.height].every(Number.isFinite) || place.x < 0 || place.y < 0 || place.width <= 0 || place.height <= 0 || place.x + place.width > 1.0001 || place.y + place.height > 1.0001) throw new Error('지도 영역 데이터 형식을 확인해 주세요.');
    const area = el('div', 'place'), name = el('span', '', place.name);
    area.dataset.placeId = place.id; area.dataset.type = place.type; area.title = place.name;
    area.style.left = place.x * 100 + '%'; area.style.top = place.y * 100 + '%';
    area.style.width = place.width * 100 + '%'; area.style.height = place.height * 100 + '%';
    area.append(name); areas.append(area);
  }
  map.prepend(areas);
}

export function placeAt(places, x, y) {
  return places.filter(p => x >= p.x && x <= p.x + p.width && y >= p.y && y <= p.y + p.height)
    .sort((a, b) => a.width * a.height - b.width * b.height || a.id - b.id)[0];
}
