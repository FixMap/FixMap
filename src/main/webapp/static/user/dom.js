export const $ = selector => document.querySelector(selector);
export function el(tag, className = '', text) {
  const node = document.createElement(tag);
  node.className = className;
  if (text !== undefined) node.textContent = text;
  return node;
}
export function statusName(status) { return ({ RECEIVED: '접수', DONE: '완료', HOLD: '보류' })[status] || status || '상태 없음'; }
export function badge(status) {
  return el('span', 'badge ' + (({ RECEIVED: 'received', DONE: 'completed', HOLD: 'held' })[status] || ''), statusName(status));
}
export function pin(point, label, draft = false) {
  const node = el('button', 'pin' + (draft ? ' draft' : ''));
  node.type = 'button'; node.style.left = point.x * 100 + '%'; node.style.top = point.y * 100 + '%';
  node.dataset.stateShort = '1'; node.setAttribute('aria-label', label); node.title = label;
  return node;
}
