import { $, el } from './dom.js';

export function setupReportForm(onCancel) {
  const form = $('#report-form');
  let files = [], previewUrls = [];
  const error = message => { $('#form-error').textContent = message; $('#form-error').hidden = !message; };
  function renderFiles() {
    previewUrls.forEach(url => URL.revokeObjectURL(url)); previewUrls = [];
    const root = $('#file-previews'); root.replaceChildren();
    files.forEach((file, index) => {
      const item = el('div', 'file-preview'), photo = el('img'), remove = el('button', '', '×');
      photo.src = URL.createObjectURL(file); previewUrls.push(photo.src); photo.alt = file.name;
      remove.type = 'button'; remove.setAttribute('aria-label', file.name + ' 미리보기 삭제');
      remove.addEventListener('click', () => { files.splice(index, 1); renderFiles(); });
      item.append(photo, remove, el('p', '', file.name)); root.append(item);
    });
    $('#drop-zone').hidden = files.length === 3;
    $('.upload-region').classList.toggle('has-files', files.length > 0);
  }
  function reset() { form.reset(); files = []; renderFiles(); error(''); $('#content-count').textContent = '0 / 4,000'; }
  function addFiles(incoming) {
    const next = Array.from(incoming);
    if (files.length + next.length > 3) return error('사진 미리보기는 최대 3장까지 가능합니다.');
    for (const file of next) {
      if (!/\.(jpe?g|png)$/i.test(file.name) || (file.type && !['image/jpeg', 'image/png'].includes(file.type))) return error('JPG·PNG 사진을 선택해 주세요.');
      if (file.size === 0 || file.size > 5 * 1024 * 1024) return error('사진은 0바이트보다 크고 장당 5MB 이하여야 합니다.');
    }
    files.push(...next); error(''); renderFiles();
  }
  $('#file-input').addEventListener('change', event => { addFiles(event.target.files); event.target.value = ''; });
  $('#drop-zone').addEventListener('dragover', event => { event.preventDefault(); $('#drop-zone').classList.add('dragover'); });
  $('#drop-zone').addEventListener('dragleave', () => $('#drop-zone').classList.remove('dragover'));
  $('#drop-zone').addEventListener('drop', event => { event.preventDefault(); $('#drop-zone').classList.remove('dragover'); addFiles(event.dataTransfer.files); });
  form.elements.content.addEventListener('input', event => { $('#content-count').textContent = event.target.value.length.toLocaleString() + ' / 4,000'; });
  $('#form-cancel').addEventListener('click', onCancel);
  // 등록 API는 main에 없다. Enter나 스크립트 제출도 전송·성공 처리하지 않는다.
  form.addEventListener('submit', event => event.preventDefault());
  return {
    open(location) { $('#selected-location').textContent = location; form.hidden = false; },
    reset,
    canDiscard() { return !(form.elements.title.value || form.elements.content.value || files.length) || window.confirm('작성 중인 내용과 사진 미리보기를 취소할까요?'); }
  };
}
