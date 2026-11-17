const context = document.body.dataset.context;

async function get(path) {
  let response;
  try { response = await fetch(context + path, { headers: { Accept: 'application/json' } }); }
  catch { throw new Error('서버에 연결하지 못했습니다. 다시 시도해 주세요.'); }
  let data;
  try { data = await response.json(); }
  catch { throw new Error('서버 응답을 확인하지 못했습니다.'); }
  if (!response.ok) throw new Error(data.message || data.error || '조회하지 못했습니다. (' + response.status + ')');
  if (!Array.isArray(data)) throw new Error('조회 데이터의 형식을 확인해 주세요.');
  return data;
}

// main 7d34c2e의 두 조회 계약만 사용한다. develop의 level 규약으로 자동 전환하지 않는다.
export function readBuildings() { return get('/api/buildings'); }
export function readPins(floorId) { return get('/api/reports/map?floor=' + encodeURIComponent(floorId)); }
