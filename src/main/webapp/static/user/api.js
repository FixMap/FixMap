const context = document.body.dataset.context;

async function request(path, options = {}) {
  let response;
  try { response = await fetch(context + path, { ...options, headers: { Accept: 'application/json' } }); }
  catch { throw new Error(options.method === 'POST' ? '전송 결과를 확인하지 못했습니다. 신고 목록을 확인한 뒤 다시 시도해 주세요.' : '서버에 연결하지 못했습니다. 다시 시도해 주세요.'); }
  let data;
  try { data = await response.json(); }
  catch { throw new Error(options.method === 'POST' && response.ok ? '접수 응답을 확인하지 못했습니다. 신고 목록을 확인해 주세요.' : '서버 응답을 확인하지 못했습니다. (' + response.status + ')'); }
  if (!response.ok) throw new Error(data?.message || data?.error || '요청을 처리하지 못했습니다. (' + response.status + ')');
  return data;
}

async function list(path) {
  const data = await request(path);
  if (!Array.isArray(data)) throw new Error('조회 데이터의 형식을 확인해 주세요.');
  return data;
}

export function readBuildings() { return list('/api/buildings'); }
export function readPlaces(floorId) { return list('/api/floors/' + encodeURIComponent(floorId) + '/places'); }
export function readPins(level) { return list('/api/reports/map?level=' + encodeURIComponent(level)); }
export function readCategories() { return list('/api/categories'); }
export function readReport(id) { return request('/api/reports/' + encodeURIComponent(id)); }
export function createReport(body) { return request('/api/reports', { method: 'POST', body }); }
export function fileUrl(id) { return context + '/api/files/' + encodeURIComponent(id); }
