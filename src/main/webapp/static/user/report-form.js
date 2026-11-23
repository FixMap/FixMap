import { readCategories, createReport } from './api.js';
import { $, el } from './dom.js';

export function setupReportForm({ onCancel, getPosition, onCreated, onBusy }) {
  const form = $('#report-form');
  let files = [], previewUrls = [], busy = false, categoriesReady = false;
  const error = message => { $('#form-error').textContent = message; $('#form-error').hidden = !message; };
  const dirty = () => Boolean(form.elements.title.value || form.elements.content.value || form.elements.categoryId?.value || files.length);
  function renderFiles() {
    previewUrls.forEach(url => URL.revokeObjectURL(url)); previewUrls = [];
    const root = $('#file-previews'); root.replaceChildren();
    files.forEach((file, index) => {
      const item = el('div', 'file-preview'), photo = el('img'), remove = el('button', '', '×');
      photo.src = URL.createObjectURL(file); previewUrls.push(photo.src); photo.alt = file.name;
      remove.type = 'button'; remove.setAttribute('aria-label', file.name + ' 첨부 삭제');
      remove.addEventListener('click', () => { if (!busy) { files.splice(index, 1); renderFiles(); } });
      item.append(photo, remove, el('p', '', file.name)); root.append(item);
    });
    $('#drop-zone').hidden = files.length === 5;
    $('.upload-region').classList.toggle('has-files', files.length > 0);
  }
  function reset() { form.reset(); files = []; renderFiles(); error(''); $('#content-count').textContent = '0 / 4,000'; }
  function addFiles(incoming) {
    if (busy) return;
    const next = Array.from(incoming);
    if (files.length + next.length > 5) return error('사진은 최대 5개까지 첨부할 수 있습니다.');
    for (const file of next) {
      if (!/\.(jpe?g|png|gif|webp)$/i.test(file.name) || (file.type && !['image/jpeg', 'image/png', 'image/gif', 'image/webp'].includes(file.type))) return error('JPG·PNG·GIF·WebP 사진을 선택해 주세요.');
      if (file.size === 0 || file.size > 10 * 1024 * 1024) return error('사진은 0바이트보다 크고 개당 10MB 이하여야 합니다.');
    }
    files.push(...next); error(''); renderFiles();
  }
  function setBusy(value) {
    busy = value; form.setAttribute('aria-busy', String(value)); $('#composer-fields').disabled = value;
    $('#form-cancel').disabled = value; $('#form-submit').disabled = value || !categoriesReady;
    $('#form-submit').textContent = value ? '접수 중…' : '신고 접수하기'; onBusy(value);
  }
  async function categories() {
    categoriesReady = false; $('#form-submit').disabled = true;
    $('#category-options').replaceChildren(el('p', 'muted', '고장 종류를 불러오는 중입니다.'));
    try {
      const list = await readCategories();
      if (!list.length) throw new Error('등록된 고장 종류가 없습니다.');
      $('#category-options').replaceChildren();
      for (const category of list) {
        const label = el('label', 'category-chip'), input = el('input'); input.type = 'radio'; input.name = 'categoryId'; input.value = category.id; input.required = true;
        label.append(input, el('span', '', category.name)); $('#category-options').append(label);
      }
      categoriesReady = true; $('#form-submit').disabled = busy;
    } catch (failure) {
      const retry = el('button', 'text-button', '다시 불러오기'); retry.type = 'button'; retry.addEventListener('click', categories);
      $('#category-options').replaceChildren(el('p', 'notice error', failure.message), retry);
    }
  }
  $('#file-input').addEventListener('change', event => { addFiles(event.target.files); event.target.value = ''; });
  $('#drop-zone').addEventListener('dragover', event => { event.preventDefault(); if (!busy) $('#drop-zone').classList.add('dragover'); });
  $('#drop-zone').addEventListener('dragleave', () => $('#drop-zone').classList.remove('dragover'));
  $('#drop-zone').addEventListener('drop', event => { event.preventDefault(); $('#drop-zone').classList.remove('dragover'); addFiles(event.dataTransfer.files); });
  form.elements.content.addEventListener('input', event => { $('#content-count').textContent = event.target.value.length.toLocaleString() + ' / 4,000'; });
  $('#form-cancel').addEventListener('click', () => { if (!busy) onCancel(); });
  addEventListener('beforeunload', event => { if (!form.hidden && dirty()) { event.preventDefault(); event.returnValue = ''; } });
  form.addEventListener('submit', async event => {
    event.preventDefault();
    if (busy || !categoriesReady || !form.reportValidity()) return;
    const position = getPosition(), title = form.elements.title.value.trim(), content = form.elements.content.value.trim();
    if (!position) return error('지도에서 신고 위치를 선택해 주세요.');
    if (!title || !content) return error('제목과 내용을 입력해 주세요.');
    const body = new FormData();
    for (const [name, value] of Object.entries({ floorId: position.floorId, posX: position.x, posY: position.y, categoryId: form.elements.categoryId.value, title, content })) body.append(name, value);
    files.forEach(file => body.append('files', file, file.name));
    error(''); setBusy(true);
    let created;
    try {
      created = await createReport(body);
      if (!created?.id) throw new Error('접수 응답을 확인하지 못했습니다. 신고 목록을 확인해 주세요.');
    } catch (failure) { error(failure.message); }
    finally { setBusy(false); }
    if (created?.id) { reset(); onCreated(created); }
  });
  categories();
  return {
    get busy() { return busy; },
    open(location) { $('#selected-location').textContent = location; form.hidden = false; },
    reset,
    canDiscard() { return !busy && (!dirty() || window.confirm('작성 중인 내용과 첨부사진을 취소할까요?')); }
  };
}
