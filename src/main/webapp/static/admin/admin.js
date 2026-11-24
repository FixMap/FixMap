import { $, el, badge, statusName } from '../user/dom.js';

const context = document.body.dataset.context;
const drawer = $('#drawer');
const detail = $('#detail');
const saveForm = $('#save-form');
const seg = $('#status-seg');
const deleteOpen = $('#delete-open');
const confirmDialog = $('#confirm');
const STATUSES = ['RECEIVED', 'PROGRESS', 'HOLD', 'DONE'];
let current = null;
let confirmAction = null;
let selectedStatus = null;
let version = 0;

async function api(path, method = 'GET', body) {
  let response;
  try {
    response = await fetch(context + path, {
      method,
      headers: body ? { Accept: 'application/json', 'Content-Type': 'application/json' } : { Accept: 'application/json' },
      body: body ? JSON.stringify(body) : undefined,
    });
  } catch { throw new Error('서버에 연결하지 못했습니다. 다시 시도해 주세요.'); }
  if (response.status === 204) return null;
  const data = await response.json().catch(() => null);
  if (!response.ok) throw new Error(data?.message || '요청을 처리하지 못했습니다. (' + response.status + ')');
  return data;
}

const reportPath = id => '/api/admin/reports/' + encodeURIComponent(id);

function longTime(value) {
  const m = /^\d{4}-(\d{2})-(\d{2})T(\d{2}):(\d{2})/.exec(value || '');
  return m ? Number(m[1]) + '월 ' + Number(m[2]) + '일 ' + m[3] + ':' + m[4] : '—';
}

function button(text, onClick) {
  const node = el('button', '', text); node.type = 'button'; node.addEventListener('click', onClick); return node;
}

function notify(message) {
  const status = $('#action-status'); status.textContent = message; status.hidden = false;
}

// 목록: 상태 배지, 처리하기, 건물 선택 시 바로 적용
for (const cell of document.querySelectorAll('.row-status')) cell.append(badge(cell.dataset.status));
for (const row of document.querySelectorAll('tr[data-id]')) row.querySelector('.row-open').addEventListener('click', () => open(row.dataset.id));
$('#filter select[name="building"]')?.addEventListener('change', event => event.target.form.requestSubmit());

drawer.querySelector('.close').addEventListener('click', () => drawer.close());
drawer.addEventListener('click', event => { if (event.target === drawer) drawer.close(); });
drawer.addEventListener('close', () => {
  ++version; current = null;
  for (const row of document.querySelectorAll('tr.selected')) row.classList.remove('selected');
});

async function open(id) {
  const request = ++version;
  for (const row of document.querySelectorAll('tr[data-id]')) row.classList.toggle('selected', row.dataset.id === String(id));
  detail.replaceChildren(el('p', 'empty muted', '신고를 불러오는 중입니다.'));
  saveForm.hidden = true;
  if (!drawer.open) drawer.showModal();
  detail.setAttribute('aria-busy', 'true');
  try {
    const data = await api(reportPath(id));
    if (request !== version) return;
    // 상세를 열면 서버가 읽음으로 바꾼다. 목록 표시도 같이 맞춘다.
    const row = document.querySelector('tr[data-id="' + CSS.escape(String(id)) + '"]');
    if (row?.classList.contains('unread')) {
      row.classList.replace('unread', 'read'); row.querySelector('.title .sr-only')?.remove();
      const unread = $('#unread-count'); unread.textContent = Math.max(0, Number(unread.textContent) - 1);
    }
    render(data);
  } catch (error) {
    if (request !== version) return;
    const retry = button('다시 불러오기', () => open(id)); retry.className = 'text-button';
    detail.replaceChildren(el('p', 'notice error', error.message), retry);
  } finally { if (request === version) detail.setAttribute('aria-busy', 'false'); }
}

function render(data) {
  current = data;
  const kv = el('dl', 'kv');
  for (const [label, value] of [['위치', [data.building, data.floor, data.place].filter(Boolean).join(' · ')], ['종류', data.category], ['접수일', longTime(data.createdAt)]]) {
    kv.append(el('dt', '', label), el('dd', '', value || '—'));
  }
  const files = el('div', 'th');
  for (const file of data.files) {
    const link = el('a'); link.href = context + file.url; link.target = '_blank'; link.rel = 'noopener noreferrer'; link.title = file.name;
    if (file.contentType?.startsWith('image/')) { const image = el('img'); image.src = link.href; image.alt = file.name; image.loading = 'lazy'; image.addEventListener('error', () => { link.textContent = file.name; }); link.append(image); }
    else link.textContent = file.name;
    files.append(link);
  }
  const mini = el('div', 'mini');
  if (data.posX != null && data.posY != null) { const pin = el('i'); pin.style.left = data.posX * 100 + '%'; pin.style.top = data.posY * 100 + '%'; mini.append(pin); }
  // 층 도면 없이 점만 있으면 어느 칸인지 모르니 장소 이름을 같이 보여 준다.
  mini.append(el('span', 'mini-place', [data.building, data.floor, data.place].filter(Boolean).join(' · ') || '위치 정보 없음'));
  mini.setAttribute('aria-label', '층 기준 핀 위치'); mini.setAttribute('role', 'img');

  detail.replaceChildren(badge(data.status), el('h2', 'detail-title', data.title), kv, el('h4', '', '내용'), el('div', 'box', data.content));
  if (data.files.length) detail.append(el('h4', '', '첨부 파일'), files);
  detail.append(el('h4', '', '핀 위치'), mini, el('h4', '', '답글'));
  if (!data.comments.length) detail.append(el('p', 'm', '아직 등록된 답글이 없습니다.'));
  for (const comment of data.comments) detail.append(commentView(comment));
  const reply = el('textarea'); reply.id = 'reply'; reply.maxLength = 2000; reply.placeholder = '처리 안내나 답변을 입력하세요';
  const label = el('label', '', '답글 작성'); label.htmlFor = 'reply';
  const h4 = el('h4'); h4.append(label);
  detail.append(h4, reply);

  selectedStatus = data.status;
  seg.replaceChildren();
  for (const value of STATUSES.includes(data.status) ? STATUSES : [data.status, ...STATUSES]) {
    const option = button(statusName(value), () => { selectedStatus = value; syncSeg(); });
    option.dataset.value = value; option.setAttribute('role', 'radio'); seg.append(option);
  }
  syncSeg();
  deleteOpen.hidden = data.status !== 'DONE';
  saveForm.hidden = false;
}

function syncSeg() {
  for (const option of seg.children) option.setAttribute('aria-checked', String(option.dataset.value === selectedStatus));
}

function commentView(comment) {
  const box = el('div', 'box');
  const meta = el('div', 'reply-meta');
  meta.append(
    el('span', '', '관리자 · ' + longTime(comment.createdAt)),
    button('수정', () => {
      const input = el('textarea'); input.value = comment.content; input.maxLength = 2000;
      const actions = el('div', 'reply-meta');
      actions.append(
        button('저장', () => run(() => api(reportPath(current.id) + '/comments/' + comment.id, 'PATCH', { content: input.value }), '답글을 수정했습니다.')),
        button('취소', () => box.replaceWith(commentView(comment))),
      );
      box.replaceChildren(input, actions); input.focus();
    }),
    button('삭제', () => ask('답글을 삭제할까요?', '삭제한 답글은 되돌릴 수 없어요.',
      () => run(() => api(reportPath(current.id) + '/comments/' + comment.id, 'DELETE'), '답글을 삭제했습니다.'))),
  );
  box.append(meta, document.createTextNode(comment.content));
  return box;
}

saveForm.addEventListener('submit', async event => {
  event.preventDefault();
  const comment = $('#reply')?.value.trim() || null;
  const status = selectedStatus === current.status ? null : selectedStatus;
  if (!status && !comment) { alert('상태를 바꾸거나 답글을 입력해 주세요.'); return; }
  const submit = saveForm.querySelector('[type="submit"]');
  submit.disabled = true;
  try {
    await api(reportPath(current.id), 'PATCH', { status, comment });
    // 상태가 바뀌면 요약과 목록도 바뀌므로 화면을 다시 그린다.
    if (status) { location.reload(); return; }
    notify('답글을 저장했습니다.'); open(current.id);
  } catch (error) { alert(error.message); }
  finally { submit.disabled = false; }
});

// 신고 삭제와 답글 삭제가 같은 확인 창을 쓴다.
function ask(title, text, action) {
  $('#confirm-title').textContent = title; $('#confirm-text').textContent = text;
  confirmAction = action; confirmDialog.returnValue = ''; confirmDialog.showModal();
}
confirmDialog.addEventListener('close', () => {
  const action = confirmAction; confirmAction = null;
  if (confirmDialog.returnValue === 'ok' && action && current) action();
});
deleteOpen.addEventListener('click', () => ask('이 신고를 삭제할까요?', '답글과 첨부 사진도 함께 지워지고, 되돌릴 수 없어요. 완료된 신고만 삭제할 수 있어요.', async () => {
  try { await api(reportPath(current.id), 'DELETE'); location.reload(); }
  catch (error) { alert(error.message); }
}));

async function run(action, message) {
  try { await action(); notify(message); open(current.id); }
  catch (error) { alert(error.message); }
}
