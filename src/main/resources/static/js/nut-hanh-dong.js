/* One presentation rule for table actions, including rows inserted by AJAX. */
(() => {
  const icons = {
    view: 'M2 8s5-8 12 0c-7 8-12 0-12 0zm4 0a2 2 0 1 0 4 0 2 2 0 0 0-4 0',
    edit: 'm3 11 1-4 7-6 3 3-7 7-4 1zm6-8 3 3',
    delete: 'M2 4h12M6 4V2h4v2M4 4l1 10h6l1-10M7 6v6M9 6v6',
    positive: 'M2 8h12M8 2v12', neutral: 'M3 4h10M3 8h10M3 12h10'
  };
  function decorate() {
    document.querySelectorAll('table').forEach(table => {
      const heads = [...table.querySelectorAll('thead tr:last-child th')];
      const indices = heads.map((h,i)=>/^(Hành động|Thao tác)$/i.test(h.textContent.trim()) ? i : -1).filter(i=>i>=0);
      for (const row of table.querySelectorAll('tbody tr')) for (const i of indices) {
        const cell = row.cells[i]; if (!cell || cell.colSpan > 1) continue;
        cell.classList.add('table-actions');
        cell.querySelectorAll('a,button').forEach(button => {
          const label=button.textContent.trim() || button.getAttribute('aria-label') || button.title;
          if (!label) return;
          const kind=/Bán lại|Mở khóa|Hiện|Xác nhận|Lưu|Thêm/i.test(label)?'positive':/Xóa|Hủy|Ngừng|Ẩn|Khóa|Vô hiệu hóa/i.test(label)?'delete':/Sửa|Chỉnh/i.test(label)?'edit':/Xem|Chi tiết|In|Lịch sử/i.test(label)?'view':'neutral';
          const key=kind+':'+label;
          if(button.dataset.tableActionReady===key && button.querySelector('svg[data-action-icon]'))return;
          button.dataset.tableActionReady=key;
          button.classList.remove('action-view','action-edit','action-delete','action-positive','action-neutral');
          button.classList.add('table-action-button','action-'+kind);
          button.querySelectorAll('i').forEach(i=>i.setAttribute('aria-hidden','true'));
          button.querySelectorAll('svg[data-action-icon]').forEach(icon=>icon.remove());
          const svg=document.createElementNS('http://www.w3.org/2000/svg','svg');svg.dataset.actionIcon='true';svg.setAttribute('viewBox','0 0 16 16');svg.setAttribute('aria-hidden','true');svg.setAttribute('fill','none');svg.setAttribute('stroke','currentColor');svg.setAttribute('stroke-width','1.4');
          const path=document.createElementNS(svg.namespaceURI,'path');path.setAttribute('d',icons[kind]);svg.append(path);button.prepend(svg);
          if (!button.textContent.trim()) button.append(document.createTextNode(label));
        });
      }
    });
  }
  let scheduled=false;
  const observer=new MutationObserver(()=>{if(scheduled)return;scheduled=true;requestAnimationFrame(()=>{scheduled=false;decorate();});});
  decorate();observer.observe(document.body,{childList:true,subtree:true});
})();
