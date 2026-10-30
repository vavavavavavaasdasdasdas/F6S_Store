document.addEventListener('DOMContentLoaded', () => {
    const panel=document.querySelector('[data-product-variants]');
    if(!panel)return;
    const productId=panel.dataset.productId,base=`/admin/products-crud/${productId}/variants`;
    const message=panel.querySelector('[data-variant-message]'),dialog=panel.querySelector('#batch-variants');
    const batch=dialog.querySelector('form');let opener;
    const say=(text,error=false)=>{message.textContent=text;message.className=error?'text-danger':'text-success';};
    function errors(root,fields={}) {
        root.querySelectorAll('[data-field-error]').forEach(slot=>slot.textContent=fields[slot.dataset.fieldError]||'');
        root.querySelectorAll('[name]').forEach(input=>input.setAttribute('aria-invalid',fields[input.name]?'true':'false'));
    }
    function busy(root,value) {
        root.setAttribute('aria-busy',String(value));
        root.querySelectorAll('input,select,button').forEach(input=>{
            if(value){input.dataset.previouslyDisabled=String(input.disabled);input.disabled=true;}
            else if(input.dataset.previouslyDisabled!==undefined){input.disabled=input.dataset.previouslyDisabled==='true';delete input.dataset.previouslyDisabled;}
        });
    }
    async function post(url,data) {
        const response=await fetch(url,{method:'POST',body:data,headers:{'X-Requested-With':'XMLHttpRequest'}});
        const json=response.headers.get('content-type')?.includes('application/json')?await response.json():{message:'Không lưu được. Kiểm tra phiên đăng nhập rồi thử lại.'};
        if(!response.ok){const error=new Error(json.message);error.fields=json.errors||{};throw error;}
        return json;
    }
    async function refreshRow(row,id) {
        const number=row.querySelector('.row-number').textContent;
        const response=await fetch(`${base}/row/${id}?rowNumber=${encodeURIComponent(number)}`);
        if(!response.ok)throw Error('Đã lưu nhưng chưa tải được dòng mới. Tải lại trang để xem dữ liệu.');
        const doc=new DOMParser().parseFromString('<table><tbody>'+await response.text()+'</tbody></table>','text/html');
        const replacement=doc.querySelector('[data-variant-row]');
        if(!replacement)throw Error('Không tải được dòng biến thể. Tải lại trang.');
        row.replaceWith(replacement);
    }
    async function refreshResults() {
        const response=await fetch(location.href);
        if(!response.ok)throw Error('Thao tác đã lưu nhưng chưa cập nhật được bảng. Tải lại trang.');
        const doc=new DOMParser().parseFromString(await response.text(),'text/html'),results=doc.querySelector('[data-variant-results]');
        if(!results)throw Error('Không tải được bảng biến thể.');
        panel.querySelector('[data-variant-results]').replaceWith(results);
    }
    function editing(row,active) {
        row.dataset.editing=String(active);
        row.querySelectorAll('[data-read]').forEach(el=>el.hidden=active);
        row.querySelectorAll('[data-edit]').forEach(el=>el.hidden=!active);
        if(active)row.querySelector('[data-edit] [role=combobox]')?.focus();
    }
    panel.addEventListener('click',async event=>{
        const button=event.target.closest('button');if(!button)return;
        const row=button.closest('[data-variant-row]');
        if(button.matches('[data-batch-open]')){
            if(panel.querySelector('[data-editing=true]')){say('Lưu hoặc hủy dòng đang sửa trước khi thêm biến thể.',true);return;}
            opener=button;dialog.showModal();return;
        }
        if(button.matches('[data-batch-close]')){dialog.close();return;}
        if(!row || row.getAttribute('aria-busy')==='true')return;
        if(button.matches('[data-row-edit]')){editing(row,true);return;}
        if(button.matches('[data-row-cancel]')){
            row.querySelector('form').reset();row.querySelectorAll('select').forEach(s=>s.dispatchEvent(new CustomEvent('combo:sync')));
            errors(row);row.querySelector('[data-row-error]').textContent='';editing(row,false);return;
        }
        const selling=button.dataset.selling,remove=button.matches('[data-row-delete]');
        if(!remove && !button.matches('[data-row-status]'))return;
        if(panel.querySelector('[data-editing=true]')){say('Lưu hoặc hủy dòng đang sửa trước khi đổi trạng thái hoặc xóa.',true);return;}
        const accepted=await window.adminConfirm(remove?'Xóa biến thể?':selling==='true'?'Bán lại biến thể?':'Ngừng bán biến thể?',remove?'Chỉ xóa được biến thể chưa có trong đơn hoặc hóa đơn chờ. Nếu đã sử dụng, hãy chọn Ngừng bán.':'Thay đổi trạng thái bán, giữ nguyên tồn kho và đơn cũ.',remove?'Xác nhận xóa':'Xác nhận',button);
        if(!accepted)return;
        const form=row.querySelector('form'),data=new FormData(form);if(!remove)data.set('selling',selling);
        busy(row,true);
        try{const result=await post(`${base}/${row.dataset.variantRow}/${remove?'delete':'status'}`,data);if(remove)await refreshResults();else await refreshRow(row,result.id);say(result.message);}
        catch(error){row.querySelector('[data-row-error]').textContent=error.message;say(error.message,true);}
        finally{busy(row,false);}
    });
    panel.addEventListener('submit',async event=>{
        const form=event.target;if(!form.matches('[data-inline-variant],[data-batch-variants]'))return;
        event.preventDefault();const row=form.closest('[data-variant-row]'),root=row||form;
        if(root.getAttribute('aria-busy')==='true')return;
        const data=new FormData(form);errors(root);busy(root,true);say('Đang lưu…');
        try{
            const result=await post(form.action,data);
            if(row)await refreshRow(row,result.id);
            else{await refreshResults();form.reset();form.querySelectorAll('select').forEach(s=>s.dispatchEvent(new CustomEvent('combo:sync')));dialog.close();}
            say(result.message);
        }catch(error){errors(root,error.fields);root.querySelector(row?'[data-row-error]':'[data-batch-error]').textContent=error.message;say(error.message,true);root.querySelector('[aria-invalid=true]')?.focus();}
        finally{busy(root,false);}
    });
    dialog.addEventListener('close',()=>opener?.focus());
});
