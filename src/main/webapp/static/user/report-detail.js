import { readReport, fileUrl } from './api.js';
import { $, el, badge } from './dom.js';

export function setupReportDetail(onClose) {
  const panel = $('#selection-panel');
  let version = 0;
  async function show(report, backLabel = '이 층의 신고로 돌아가기') {
    const request = ++version;
    const back = el('button', 'text-button panel-back', backLabel); back.type = 'button'; back.addEventListener('click', onClose);
    panel.replaceChildren(back, el('h2', 'detail-title', report.title), el('p', 'empty muted', '신고를 불러오는 중입니다.'));
    panel.setAttribute('aria-busy', 'true');
    try {
      const data = await readReport(report.id);
      if (request !== version) return;
      if (!data || typeof data.title !== 'string' || typeof data.content !== 'string' || !Array.isArray(data.files) || !Array.isArray(data.comments)) throw new Error('신고 데이터 형식을 확인해 주세요.');
      const info = el('dl', 'pin-info');
      for (const [label, value] of [['신고 번호', '#' + data.id], ['위치', [data.building, data.floor, data.place].filter(Boolean).join(' · ')], ['고장 종류', data.category], ['작성일', data.createdAt?.replace('T', ' ')]]) {
        const item = el('div'); item.append(el('dt', '', label), el('dd', '', value || '—')); info.append(item);
      }
      panel.replaceChildren(back, el('h2', 'detail-title', data.title), badge(data.status), info, el('h3', 'detail-section-title', '신고 내용'), el('p', 'report-content', data.content));
      const files = el('div', 'detail-files');
      for (const file of data.files) {
        const link = el('a', 'detail-file'); link.href = fileUrl(file.id); link.target = '_blank'; link.rel = 'noopener noreferrer';
        if (['image/jpeg', 'image/png', 'image/gif', 'image/webp'].includes(file.contentType)) {
          const image = el('img'); image.src = link.href; image.alt = file.name; image.loading = 'lazy';
          image.addEventListener('error', () => image.replaceWith(el('span', 'muted', '사진을 불러오지 못했습니다.'))); link.append(image);
        }
        link.append(el('span', '', file.name)); files.append(link);
      }
      panel.append(el('h3', 'detail-section-title', '첨부사진 ' + data.files.length + '개'), files);
      panel.append(el('h3', 'detail-section-title', '관리자 답글'));
      if (!data.comments.length) panel.append(el('p', 'muted', '아직 등록된 답글이 없습니다.'));
      for (const comment of data.comments) {
        const reply = el('article', 'report-reply'); reply.append(el('p', 'report-content', comment.content), el('time', 'muted', comment.createdAt?.replace('T', ' '))); panel.append(reply);
      }
    } catch (error) {
      if (request !== version) return;
      const retry = el('button', 'text-button', '다시 불러오기'); retry.type = 'button'; retry.addEventListener('click', () => show(report, backLabel));
      panel.replaceChildren(back, el('p', 'notice error', error.message), retry);
    } finally { if (request === version) panel.setAttribute('aria-busy', 'false'); }
  }
  return { show, reset() { ++version; panel.setAttribute('aria-busy', 'false'); } };
}
