import { createRequire } from 'node:module';
import { resolve, join } from 'node:path';
import { mkdtemp, writeFile, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import assert from 'node:assert/strict';

assert(process.env.FIXMAP_URL && process.env.FIXMAP_ALLOW_WRITES === '1', 'Use a disposable backend: set FIXMAP_URL and FIXMAP_ALLOW_WRITES=1. This test creates a report and five files.');
const require = createRequire(resolve(process.env.FIXMAP_BROWSER_TOOLS || '.', 'package.json'));
const browser = await require('puppeteer-core').launch({ executablePath: process.env.CHROME_PATH || 'C:/Program Files/Google/Chrome/Application/chrome.exe', headless: true });
const origin = process.env.FIXMAP_URL.replace(/\/$/, ''), prefix = new URL(origin).pathname;
const dir = await mkdtemp(join(tmpdir(), 'fixmap-ui-')), files = [];
const photo = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+j7ioAAAAASUVORK5CYII=', 'base64');
for (let i = 0; i < 5; i++) { const path = join(dir, 'photo-' + i + '.png'); await writeFile(path, photo); files.push(path); }
const calls = [], errors = [];
let failPins = false, failPlaces = false, failCategories = false, failPost = false, unsafe = false, progress = false, slowPost = false, slowLevel = null;
try {
  const page = await browser.newPage();
  page.on('pageerror', error => errors.push(error.message));
  page.on('dialog', dialog => dialog.accept());
  await page.setRequestInterception(true);
  page.on('request', async request => {
    const url = new URL(request.url()), route = url.pathname.slice(prefix.length), method = request.method();
    if (route.startsWith('/api/')) {
      calls.push({ method, route, query: url.search });
      const getAllowed = /^\/api\/(buildings|categories|reports\/map|floors\/\d+\/places|reports\/\d+|files\/\d+)$/.test(route);
      if (!(method === 'GET' && getAllowed) && !(method === 'POST' && route === '/api/reports')) { errors.push('Unsupported API: ' + request.url()); await request.abort(); return; }
      const json = (status, body) => request.respond({ status, contentType: 'application/json', body: JSON.stringify(body) });
      if (route === '/api/reports/map') {
        if (!url.searchParams.has('level') || url.searchParams.has('floor')) errors.push('Wrong pin parameter');
        if (Number(url.searchParams.get('level')) === slowLevel) await new Promise(resolve => setTimeout(resolve, 350));
        if (failPins) return json(500, { message: '신고 조회 오류 테스트' });
        if (progress) {
          const rows = await fetch(url).then(r => r.json());
          return json(200, [...rows, { ...rows[0], id: 999999, status: 'PROGRESS' }]);
        }
      }
      if (failPlaces && /^\/api\/floors\//.test(route)) return json(500, { message: '영역 조회 오류 테스트' });
      if (failCategories && route === '/api/categories') return json(500, { message: '종류 조회 오류 테스트' });
      if (method === 'POST') {
        if (slowPost) await new Promise(resolve => setTimeout(resolve, 350));
        if (failPost) return json(400, { message: '접수 오류 테스트' });
      }
      if (unsafe && /^\/api\/reports\/\d+$/.test(route)) {
        const data = await fetch(url).then(r => r.json());
        data.title = '<img src=x onerror=alert(1)>'; data.content = '<script>alert(1)</script>';
        data.comments = [{ id: 1, content: '<b>답글도 문자</b>', createdAt: '2026-11-23T10:00:00' }];
        data.files = [{ id: 1, name: '<img src=x>', contentType: 'text/html', url: 'https://example.invalid/evil' }];
        return json(200, data);
      }
    }
    await request.continue();
  });
  const loaded = () => page.waitForFunction(() => document.querySelector('#map-stage').getAttribute('aria-busy') === 'false' && document.querySelectorAll('.floor-plan .place').length > 0 && /^\d+$/.test(document.querySelector('#pin-count').textContent));
  const openForm = async () => { await page.focus('.building-map[data-building-id="1"]'); await page.keyboard.press('Enter'); };
  const submit = () => page.$eval('#report-form', form => form.requestSubmit());
  for (const [width, height] of [[1440, 900], [375, 844], [320, 600]]) {
    await page.setViewport({ width, height });
    const before = calls.length;
    await page.goto(origin + '/?level=2', { waitUntil: 'networkidle0' }); await loaded(); await page.evaluate(() => document.fonts.ready);
    assert.equal(await page.$eval('body', body => body.dataset.level), '2', 'MainPageController model');
    assert.equal(calls.slice(before).filter(call => call.route === '/api/reports/map').length, 1, 'one pin GET for all buildings');
    assert.deepEqual(calls.slice(before).filter(call => /^\/api\/floors\//.test(call.route)).map(call => call.route), ['/api/floors/2/places']);
    assert.equal(await page.$$eval('.building-map', nodes => nodes.length), 1);
    assert.equal(await page.$$eval('.building-map img', nodes => nodes.length), 0, 'no temporary map images');
    assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth));
    const places = await fetch(origin + '/api/floors/2/places').then(r => r.json());
    assert(await page.$eval('.place[data-place-id="' + places[0].id + '"]', (node, p) => Math.abs(parseFloat(node.style.left) - p.x * 100) < .001 && Math.abs(parseFloat(node.style.width) - p.width * 100) < .001, places[0]));
    const pins = await fetch(origin + '/api/reports/map?level=2').then(r => r.json());
    assert.deepEqual(await page.$$eval('.pin[data-report-id]', nodes => nodes.map(node => Number(node.dataset.reportId))), pins.filter(report => report.floorId === 2).map(report => report.id));
    assert(await page.$eval('.building-map', map => map.offsetWidth >= map.closest('.map-scroll').clientWidth - 32));
    assert.deepEqual(await page.$$eval('.place', nodes => nodes.filter(node => {
      const range = document.createRange();
      range.selectNodeContents(node.firstElementChild);
      const text = range.getBoundingClientRect();
      const bounds = node.getBoundingClientRect();
      return text.left < bounds.left || text.right > bounds.right || text.top < bounds.top || text.bottom > bounds.bottom;
    }).map(node => node.title)), [], 'place labels fit within their own rectangles');
    await openForm();
    await page.waitForFunction(() => !document.querySelector('#form-submit').disabled);
    assert(await page.$eval('#form-submit', node => { const rect = node.getBoundingClientRect(); return rect.top >= 0 && rect.bottom <= innerHeight; }));
    await page.click('.category-chip span'); await page.type('[name="title"]', '유지할 입력'); await page.type('[name="content"]', '위치를 옮겨도 보존합니다.');
    await (await page.$('#file-input')).uploadFile(files[0]);
    await page.focus('.building-map[data-building-id="1"]'); await page.keyboard.press('ArrowRight');
    assert.equal(await page.$eval('[name="title"]', node => node.value), '유지할 입력');
    assert.equal(await page.$$eval('#file-previews img', nodes => nodes.length), 1);
    await page.click('#form-cancel'); assert.equal(await page.$$eval('.pin.draft', nodes => nodes.length), 0);
    const widthBefore = await page.$eval('#map-stage', node => node.getBoundingClientRect().width);
    for (let i = 0; i < 6; i++) await page.click('#zoom-in');
    assert(Math.abs(await page.$eval('#map-stage', node => node.getBoundingClientRect().width) / widthBefore - 2.5) < .02);
  }
  await page.setViewport({ width: 1440, height: 900 });
  await page.click('#building-tabs [data-building="donghaeng"]');
  await loaded();
  assert.equal(await page.$eval('.building-map', node => node.dataset.floorId), '10', 'keeps the floor when the new building has it');
  await page.click('#floor-tabs [data-level="5"]'); await loaded();
  await page.click('#building-tabs [data-building="main"]'); await loaded();
  assert.equal(await page.$eval('#floor-tabs [aria-pressed="true"]', node => node.dataset.level), '1', 'falls back to the first available floor');
  await page.goto(origin + '/?level=2&building=geumbong', { waitUntil: 'networkidle0' }); await loaded();
  assert.equal(await page.$eval('.building-map', node => node.dataset.floorId), '6', 'building and level survive a reload');
  progress = true; await page.goto(origin + '/?level=2', { waitUntil: 'networkidle0' }); await loaded();
  assert.deepEqual(new Set(await page.$$eval('.report-card .badge', nodes => nodes.map(node => node.textContent))), new Set(['접수', '처리 중', '완료', '보류'])); progress = false;
  slowLevel = 1;
  await page.evaluate(() => { document.querySelector('[data-level="1"]').click(); document.querySelector('[data-level="3"]').click(); });
  await loaded(); await page.waitForNetworkIdle();
  assert.equal(new URL(page.url()).searchParams.get('level'), '3');
  assert(await page.$$eval('.building-map', nodes => nodes.every(node => node.getAttribute('aria-label').includes('3층')))); slowLevel = null;

  failPlaces = true; await page.goto(origin + '/?level=2', { waitUntil: 'networkidle0' });
  assert.equal(await page.$eval('#pin-count', node => node.textContent), '—');
  await openForm(); assert(await page.$eval('#report-form', form => form.hidden), 'failed areas cannot accept a pin');
  failPlaces = false; await page.click('#location-reports button'); await loaded();
  failPins = true; await page.click('[data-level="1"]'); await page.waitForNetworkIdle();
  assert.equal(await page.$eval('#pin-count', node => node.textContent), '—');
  failPins = false; await page.click('#location-reports button'); await loaded();
  failCategories = true; await page.goto(origin + '/?level=2', { waitUntil: 'networkidle0' }); await loaded(); await openForm();
  assert(await page.$eval('#form-submit', node => node.disabled));
  failCategories = false; await page.click('#category-options button'); await page.waitForFunction(() => !document.querySelector('#form-submit').disabled);

  await page.click('.category-chip span'); await page.type('[name="title"]', '첨부 포함 접수 검사'); await page.type('[name="content"]', '실제 multipart 저장과 조회 확인');
  await page.$eval('#file-input', input => {
    const data = new DataTransfer(); data.items.add(new File([new Uint8Array(10 * 1024 * 1024 + 1)], 'large.png', { type: 'image/png' }));
    input.files = data.files; input.dispatchEvent(new Event('change', { bubbles: true }));
  });
  assert((await page.$eval('#form-error', node => node.textContent)).includes('10MB'));
  await page.$eval('#file-input', input => {
    const data = new DataTransfer(); data.items.add(new File(['x'], 'unsafe.svg', { type: 'image/svg+xml' }));
    input.files = data.files; input.dispatchEvent(new Event('change', { bubbles: true }));
  });
  assert((await page.$eval('#form-error', node => node.textContent)).includes('JPG'));
  await (await page.$('#file-input')).uploadFile(...files);
  assert.equal(await page.$$eval('#file-previews img', nodes => nodes.length), 5);
  await page.$eval('#file-input', input => {
    const data = new DataTransfer(); data.items.add(new File(['x'], 'sixth.png', { type: 'image/png' }));
    input.files = data.files; input.dispatchEvent(new Event('change', { bubbles: true }));
  });
  assert((await page.$eval('#form-error', node => node.textContent)).includes('5개'));
  failPost = true; await submit(); await page.waitForNetworkIdle();
  assert((await page.$eval('#form-error', node => node.textContent)).includes('접수 오류 테스트'));
  assert.equal(await page.$$eval('#file-previews img', nodes => nodes.length), 5);
  assert.equal(await page.$eval('[name="title"]', node => node.value), '첨부 포함 접수 검사');
  failPost = false; slowPost = true;
  const beforePost = calls.filter(call => call.method === 'POST').length;
  const createdResponse = page.waitForResponse(response => response.request().method() === 'POST' && response.url() === origin + '/api/reports');
  await submit();
  assert(await page.$eval('#form-submit', node => node.disabled));
  assert(await page.$$eval('#floor-tabs button', nodes => nodes.every(node => node.disabled)));
  await page.$eval('#report-form', form => form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true })));
  const response = await createdResponse; assert.equal(response.status(), 201);
  const created = await response.json();
  assert.equal(calls.filter(call => call.method === 'POST').length - beforePost, 1, 'no duplicate submission');
  assert.equal(created.floorId, 2); assert.equal(created.posX, .5); assert.equal(created.posY, .5); assert.equal(created.files.length, 5);
  await page.waitForFunction(id => document.querySelector('#action-status').textContent.includes('#' + id) && document.querySelector('#selection-panel').getAttribute('aria-busy') === 'false' && document.querySelector('#selection-panel .detail-title')?.textContent === '첨부 포함 접수 검사', {}, created.id);
  assert.equal(await page.$$eval('.detail-file', nodes => nodes.length), 5);
  for (const file of created.files) {
    const result = await fetch(origin + '/api/files/' + file.id); assert.equal(result.status, 200); assert.equal(result.headers.get('content-type').split(';')[0], 'image/png');
    assert.deepEqual(Buffer.from(await result.arrayBuffer()), photo);
  }
  const publicDetail = await fetch(origin + '/api/reports/' + created.id).then(r => r.json());
  assert.equal(publicDetail.content, '실제 multipart 저장과 조회 확인'); assert.equal(publicDetail.files.length, 5); assert.equal('read' in publicDetail, false);
  await page.click('.panel-back'); await page.click('.report-card:nth-child(2)');
  await page.waitForFunction(() => document.querySelector('.report-reply') && document.querySelector('#selection-panel').getAttribute('aria-busy') === 'false');
  assert((await page.$eval('.report-reply', node => node.textContent)).includes('정수기'));
  unsafe = true; await page.click('.panel-back'); await page.click('.report-card');
  await page.waitForFunction(() => document.querySelector('#selection-panel .detail-title')?.textContent === '<img src=x onerror=alert(1)>');
  assert.equal(await page.$$eval('#selection-panel img', nodes => nodes.length), 0);
  assert.equal(await page.$eval('.report-content', node => node.textContent), '<script>alert(1)</script>');
  assert.equal(await page.$eval('.report-reply p', node => node.textContent), '<b>답글도 문자</b>');
  assert.equal(await page.$eval('.detail-file', node => new URL(node.href).pathname), prefix + '/api/files/1');
  assert.deepEqual(errors, []);
  console.log('PASS: develop level/places/floorId, single pin GET, four states, responsive layout, races/retry, multipart creation with five files, duplicate prevention, preserved errors, public detail/replies, file bytes and safe text.');
} finally { await browser.close(); await rm(dir, { recursive: true, force: true }); }
