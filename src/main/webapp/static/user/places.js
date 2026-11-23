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

export function fitPlaceLabels(map) {
  const text = document.createRange();
  for (const area of map.querySelectorAll('.place')) {
    const label = area.firstElementChild;
    const bounds = area.getBoundingClientRect();
    text.selectNodeContents(label);
    const fits = () => {
      const rect = text.getBoundingClientRect();
      return rect.left >= bounds.left + 1 && rect.right <= bounds.right - 1
        && rect.top >= bounds.top + 1 && rect.bottom <= bounds.bottom - 1;
    };
    label.style.fontSize = '';
    if (fits()) continue;

    // Find the largest quarter-pixel size that fits the wrapped name.
    let low = 1, high = Math.floor(parseFloat(getComputedStyle(label).fontSize) * 4);
    while (low < high) {
      const size = Math.ceil((low + high) / 2);
      label.style.fontSize = size / 4 + 'px';
      if (fits()) low = size;
      else high = size - 1;
    }
    label.style.fontSize = low / 4 + 'px';
  }
}

export function placeAt(places, x, y) {
  return places.filter(p => x >= p.x && x <= p.x + p.width && y >= p.y && y <= p.y + p.height)
    .sort((a, b) => a.width * a.height - b.width * b.height || a.id - b.id)[0];
}
