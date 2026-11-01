/* The shared Thymeleaf fragment owns icon markup. Existing actions keep their handlers/forms. */
(() => {
 const template=document.querySelector('#table-action-content');if(!template)return;
 const paths={view:'M2 8s5-8 12 0c-7 8-12 0-12 0zm4 0a2 2 0 1 0 4 0 2 2 0 0 0-4 0',edit:'m3 11 1-4 7-6 3 3-7 7-4 1zm6-8 3 3',delete:'M2 4h12M6 4V2h4v2M4 4l1 10h6l1-10M7 6v6M9 6v6',stop:'M5 2v12M11 2v12',positive:'m2 8 4 4 8-9',neutral:'M3 4h10M3 8h10M3 12h10',print:'M4 5V1h8v4M4 11H2V5h12v6h-2M4 9h8v6H4z'};
 function kind(label){return /Ngừng|Ẩn/i.test(label)?'stop':/Bán lại|Mở khóa|Hiện|Xác nhận|Lưu|Thêm/i.test(label)?'positive':/Xóa|Hủy|Khóa|Vô hiệu hóa/i.test(label)?'delete':/Sửa|Chỉnh|Điều chỉnh/i.test(label)?'edit':/^In\b/i.test(label)?'print':/Xem|Chi tiết|Lịch sử/i.test(label)?'view':'neutral';}
 function icon(button){
     const label=button.querySelector('.action-label')?.textContent.trim()||button.textContent.trim()||button.getAttribute('aria-label')||button.title;if(!label)return;
     const type=kind(label),key=type+':'+label;
     if(button.dataset.tableActionReady===key&&button.querySelector('svg[data-action-icon]'))return;
     button.dataset.tableActionReady=key;button.classList.remove('action-view','action-edit','action-delete','action-positive','action-neutral','action-stop','action-print');button.classList.add('table-action-button','action-'+(type==='print'?'view':type));button.title=label;button.setAttribute('aria-label',label);
     const content=template.content.cloneNode(true);content.querySelector('svg').dataset.actionIcon='true';content.querySelector('path').setAttribute('d',paths[type]);content.querySelector('.action-label').textContent=label;button.replaceChildren(content);
 }
 function decorate(){
  document.querySelectorAll('table').forEach(table=>{
   const heads=[...table.querySelectorAll('thead tr:last-child th')],indices=heads.flatMap((h,i)=>/^(Hành động|Thao tác)$/i.test(h.textContent.trim())?[i]:[]);
   table.querySelectorAll('tbody tr').forEach(row=>indices.forEach(i=>{
    const cell=row.cells[i];if(!cell||cell.colSpan>1)return;cell.classList.add('table-actions');
    cell.querySelectorAll('a,button').forEach(icon);
   }));
  });
  document.querySelectorAll('[data-icon-action]').forEach(icon);
  document.querySelectorAll('a.btn,button.btn,[data-address-create]').forEach(button=>{
   if(button.closest('.table-actions')||button.querySelector('.add-prefix'))return;
   if(/^Thêm\s/.test(button.textContent.trim())){const plus=document.createElement('span');plus.className='add-prefix';plus.setAttribute('aria-hidden','true');plus.textContent='+';button.prepend(plus);}
  });
 }
 let scheduled=false;const observer=new MutationObserver(()=>{if(scheduled)return;scheduled=true;requestAnimationFrame(()=>{scheduled=false;decorate();});});decorate();observer.observe(document.body,{childList:true,subtree:true});
})();
