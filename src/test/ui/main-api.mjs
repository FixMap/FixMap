import { createRequire } from 'node:module';
import { resolve } from 'node:path';
import assert from 'node:assert/strict';

const require = createRequire(resolve(process.env.FIXMAP_BROWSER_TOOLS || '.', 'package.json'));
const browser = await require('puppeteer-core').launch({ executablePath: process.env.CHROME_PATH || 'C:/Program Files/Google/Chrome/Application/chrome.exe', headless: true });
const origin = (process.env.FIXMAP_URL || 'http://127.0.0.1:11931/FixMap').replace(/\/$/, '');
const prefix = new URL(origin).pathname, calls = [], errors = [];
let failPins = false, injectTitle = false;
try {
  const page = await browser.newPage();
  page.on('pageerror', error => errors.push(error.message));
  await page.setRequestInterception(true);
  page.on('request', async request => {
    const url = new URL(request.url()), route = url.pathname.slice(prefix.length);
    if (route.startsWith('/api/')) {
      calls.push({ method: request.method(), route, query: url.search });
      if (request.method() !== 'GET' || !['/api/buildings', '/api/reports/map'].includes(route)) { errors.push('Unsupported API: ' + request.url()); await request.abort(); return; }
      if (route === '/api/reports/map') {
        assert(url.searchParams.has('floor')); assert(!url.searchParams.has('level'));
        if (failPins) { await request.respond({ status: 500, contentType: 'application/json', body: '{"message":"조회 오류 테스트"}' }); return; }
        if (injectTitle) { await request.respond({ status: 200, contentType: 'application/json', body: JSON.stringify([{ id: 99, title: '<img src=x onerror=alert(1)>', posX: .3, posY: .3, status: 'HOLD' }]) }); return; }
      }
    }
    await request.continue();
  });
  for (const [width, height] of [[1440, 900], [375, 844], [320, 600]]) {
    await page.setViewport({ width, height });
    await page.goto(origin + '/?floor=2', { waitUntil: 'networkidle0' });
    await page.evaluate(() => document.fonts.ready);
    await page.waitForFunction(() => document.querySelector('#map-stage').getAttribute('aria-busy') === 'false' && document.querySelectorAll('.building-map img').length > 0 && [...document.querySelectorAll('.building-map img')].every(image => image.complete && image.naturalWidth));
    assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth));
    assert(await page.$$eval('.building-map img', images => images.every(image => !new URL(image.src).pathname.includes('/FixMap/FixMap/'))));
    assert(await page.$$eval('.building-pins .pin', nodes => nodes.length > 0));
    const map = await page.$('.building-map[data-building-id="1"]'); await map.focus(); await page.keyboard.press('Enter');
    assert(await page.$eval('#report-form', form => !form.hidden));
    assert(await page.$eval('#form-submit', node => { const rect = node.getBoundingClientRect(); return node.disabled && rect.top >= 0 && rect.bottom <= innerHeight; }));
    assert(await page.$eval('.category-field', node => node.disabled));
    await page.type('[name="title"]', '전송하지 않는 로컬 입력'); await page.type('[name="content"]', '접수 API 미연결 상태 확인');
    await (await page.$('#file-input')).uploadFile(resolve('src/main/webapp/static/maps/CAMPUS-0.png'));
    assert.equal(await page.$$eval('#file-previews img', nodes => nodes.length), 1);
    const before = calls.length; await page.$eval('#report-form', form => form.requestSubmit());
    assert.equal(calls.length, before, 'submission has no request or success state');
    await page.$eval('.building-map[data-building-id="1"]', node => node.focus()); await page.keyboard.press('ArrowRight');
    assert.equal(await page.$eval('[name="title"]', node => node.value), '전송하지 않는 로컬 입력');
    assert.equal(await page.$$eval('#file-previews img', nodes => nodes.length), 1);
    await page.click('#form-cancel'); assert.equal(await page.$$eval('.pin.draft', nodes => nodes.length), 0);
    const beforeZoom = await page.$eval('#map-stage', node => node.getBoundingClientRect().width);
    for (let i = 0; i < 6; i++) await page.click('#zoom-in');
    const afterZoom = await page.$eval('#map-stage', node => node.getBoundingClientRect().width);
    assert(Math.abs(afterZoom / beforeZoom - 2.5) < .02);
  }
  await page.setViewport({ width: 1440, height: 900 });
  failPins = true; await page.goto(origin + '/?floor=2', { waitUntil: 'networkidle0' });
  assert.equal(await page.$eval('#pin-count', node => node.textContent), '—');
  assert((await page.$eval('#location-reports', node => node.textContent)).includes('조회 오류 테스트'));
  failPins = false; await page.click('#location-reports button');
  await page.waitForFunction(() => Number(document.querySelector('#pin-count').textContent) > 0);
  injectTitle = true; await page.goto(origin + '/?floor=2', { waitUntil: 'networkidle0' });
  await page.click('.report-card');
  assert.equal(await page.$eval('#selection-panel h2', node => node.textContent), '<img src=x onerror=alert(1)>');
  assert.equal(await page.$$eval('#selection-panel img', nodes => nodes.length), 0);
  assert.deepEqual(errors, []);
  assert(calls.every(call => call.method === 'GET'));
  console.log('PASS: main-only GET contracts, context paths, live pins, disabled submission, local previews, responsive controls, zoom, errors/retry and safe text.');
} finally { await browser.close(); }
