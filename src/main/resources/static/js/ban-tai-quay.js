(() => {
 const dialog=document.querySelector('#pos-product-dialog');
 const select=document.querySelector('#pos-variant'),quantity=document.querySelector('#pos-quantity');let opener=null;
 const variantFields=[['color','Màu sắc'],['material','Chất liệu'],['sole','Đế giày'],['size','Kích cỡ']].map(([key,name])=>({key,name,select:document.querySelector('#pos-'+key)}));let variantOptions=[];
 function resolve(){
   variantFields.forEach(({key,name,select:field},idx)=>{
     const previous=field.value,candidates=variantOptions.filter(o=>variantFields.slice(0,idx).every(f=>!f.select.value||o.dataset[f.key+'Id']===f.select.value)),values=new Map();
     candidates.forEach(o=>values.set(o.dataset[key+'Id'],o.dataset[key+'Name']));field.replaceChildren(new Option('Chọn '+name.toLowerCase(),''));
     values.forEach((text,id)=>field.add(new Option(text,id)));if(values.has(previous))field.value=previous;else if(values.size===1)field.selectedIndex=1;
     field.dispatchEvent(new CustomEvent('combo:sync'));
   });
   const found=variantOptions.find(o=>variantFields.every(f=>f.select.value&&o.dataset[f.key+'Id']===f.select.value));select.replaceChildren(...(found?[found.cloneNode(true)]:[]));stock();
 }
 variantFields.forEach(({select:field},idx)=>field.addEventListener('change',()=>{variantFields.slice(idx+1).forEach(f=>f.select.value='');resolve();}));
 function stock(){quantity.max=select.selectedOptions[0]?.dataset.stock||1;quantity.value=1;}
 document.querySelectorAll('[data-pos-product]').forEach(button=>button.addEventListener('click',()=>{
   opener=button;document.querySelector('#pos-product-title').textContent=button.dataset.productName;
   variantOptions=[...document.querySelector('#pos-options-'+button.dataset.productId).content.cloneNode(true).querySelectorAll('option')];variantFields.forEach(f=>f.select.replaceChildren());resolve();dialog.showModal();variantFields[0].select.focus();
 }));select.addEventListener('change',stock);dialog.querySelector('[data-pos-close]').addEventListener('click',()=>dialog.close());dialog.addEventListener('close',()=>{dialog.querySelector('form').dataset.dirty='false';opener?.focus();});
 const customer=document.querySelector('#customerId'),search=document.querySelector('#customerSearch');
 let searchSequence=0,searchTimer;const chosenLabel=document.querySelector('#pos-chosen-customer');
 function label(){let text='';try{text=sessionStorage.getItem('pos-customer-label-'+customer.value)||'';}catch(_){}chosenLabel.textContent=customer.value?(text||'Khách hàng #'+customer.value):'Khách lẻ';}
 async function searchCustomers(page=0){
   const sequence=++searchSequence,results=document.querySelector('#pos-customer-results');results.setAttribute('aria-busy','true');
   document.querySelector('#customerResult').textContent='Đang tìm khách hàng…';
   const params=new URLSearchParams({customerQuery:search.value,customerPage:page});
   try{const response=await fetch('/admin/pos/customers?'+params,{headers:{'X-Requested-With':'XMLHttpRequest'}});if(!response.ok)throw new Error('Không tải được khách hàng. Bạn hãy thử lại.');const html=await response.text();if(sequence!==searchSequence)return;const fragment=new DOMParser().parseFromString(html,'text/html').querySelector('#pos-customer-results');if(!fragment)throw new Error('Không tải được khách hàng. Bạn hãy thử lại.');results.replaceWith(fragment);document.querySelector('#customerResult').textContent='';}
   catch(error){if(sequence===searchSequence)document.querySelector('#customerResult').textContent=error.message;}
   finally{if(sequence===searchSequence)document.querySelector('#pos-customer-results').removeAttribute('aria-busy');}
 }
 search.addEventListener('input',()=>{clearTimeout(searchTimer);searchTimer=setTimeout(()=>searchCustomers(),300);});
 document.querySelector('#pos-customer-query').addEventListener('submit',event=>{event.preventDefault();clearTimeout(searchTimer);searchCustomers();});
 document.addEventListener('click',event=>{
   const option=event.target.closest('[data-customer-id]');if(option){customer.value=option.dataset.customerId;try{sessionStorage.setItem('pos-customer-label-'+customer.value,option.dataset.customerLabel);}catch(_){}chosenLabel.textContent=option.dataset.customerLabel;customer.dispatchEvent(new Event('change',{bubbles:true}));}
   const link=event.target.closest('#pos-customer-results .shared-pagination a');if(link){event.preventDefault();searchCustomers(new URL(link.href).searchParams.get('customerPage')||0);}
 });
 document.querySelector('[data-customer-clear]').addEventListener('click',()=>{customer.value='';label();customer.dispatchEvent(new Event('change',{bubbles:true}));});
 const form=document.querySelector('#pos-payment'),payment=document.querySelector('#paymentMethod'),cash=document.querySelector('#cashReceived'),total=form.querySelector('[data-total]'),pay=document.querySelector('#pos-pay');
 function change(){const isCash=payment.value==='CASH';document.querySelector('#cash-fields').hidden=!isCash;document.querySelector('#transfer-note').hidden=isCash;cash.required=isCash;cash.disabled=!isCash;cash.min=isCash?total.dataset.amount:0;document.querySelector('#cash-change').textContent=new Intl.NumberFormat('vi-VN').format(Math.max(0,Number(cash.value||0)-Number(total.dataset.amount)))+' đ';}
 payment.addEventListener('change',change);cash.addEventListener('input',change);form.addEventListener('voucherchange',change);change();
 const heldBar=document.querySelector('#pos-held-bar');
 const draftKey='pos-checkout-draft',fields=[customer,payment,cash,form.querySelector('[name=voucherCode]'),form.querySelector('[name=note]')];
 try {
   if(pay.dataset.cartEmpty==='true') sessionStorage.removeItem(draftKey);
   else if(!heldBar.dataset.loaded && !document.querySelector('.admin-flash .alert')) {
     const draft=JSON.parse(sessionStorage.getItem(draftKey)||'{}');fields.forEach(field=>{if(draft[field.name]!=null)field.value=draft[field.name];});change();
     if(form.querySelector('[name=voucherCode]').value.trim())form.querySelector('[data-apply-voucher]').click();
   }
 } catch(_) { /* Draft storage is optional; session cart remains authoritative. */ }
 if(heldBar.dataset.loaded && form.querySelector('[name=voucherCode]').value.trim())form.querySelector('[data-apply-voucher]').click();
 label();
 function saveDraft(){try{sessionStorage.setItem(draftKey,JSON.stringify(Object.fromEntries(fields.map(f=>[f.name,f.value]))));}catch(_){} }
 function copyDraft(target) {
   [customer,payment,form.querySelector('[name=voucherCode]'),form.querySelector('[name=note]')].forEach(field=>{
     let hidden=target.querySelector('[name="'+field.name+'"]');
     if(!hidden){hidden=document.createElement('input');hidden.type='hidden';hidden.name=field.name;target.append(hidden);}
     hidden.value=field.value;
   });
 }
 const saveForm=document.querySelector('#pos-hold-save');
 saveForm.addEventListener('submit',()=>{copyDraft(saveForm);saveDraft();form.dataset.dirty='false';},true);
 saveForm.addEventListener('submit',event=>{if(!event.defaultPrevented)document.querySelector('[form="pos-hold-save"]').disabled=true;});
 const switchDialog=document.querySelector('#pos-hold-switch');let switchForm=null,switchOpener=null;
 const restore=()=>switchOpener?.focus();switchDialog.addEventListener('close',restore);
 switchDialog.querySelector('[data-hold-cancel]').addEventListener('click',()=>switchDialog.close());
 function submitSwitch(action) {
   if(!switchForm || switchForm.dataset.submitting==='true')return;
   copyDraft(switchForm);switchForm.querySelector('[name=action]').value=action;
   form.dataset.dirty='false';switchForm.dataset.submitting='true';switchForm.setAttribute('aria-busy','true');
   switchForm.querySelector('button').disabled=true;switchDialog.close();switchForm.submit();
 }
 switchDialog.querySelector('[data-hold-save]').addEventListener('click',()=>submitSwitch('SAVE'));
 switchDialog.querySelector('[data-hold-discard]').addEventListener('click',()=>submitSwitch('DISCARD'));
 document.querySelectorAll('[data-held-open]').forEach(openForm=>openForm.addEventListener('submit',event=>{
   if(openForm.dataset.submitting==='true'){event.preventDefault();return;}
   if(pay.dataset.cartEmpty!=='true') {
     event.preventDefault();switchForm=openForm;switchOpener=openForm.querySelector('button');
     const full=Number(heldBar.dataset.count)>=5;
     switchDialog.querySelector('[data-hold-save]').hidden=full;
     switchDialog.querySelector('#pos-hold-limit').hidden=!full;
     switchDialog.showModal();switchDialog.querySelector('[data-hold-cancel]').focus();
   } else {openForm.dataset.submitting='true';openForm.setAttribute('aria-busy','true');openForm.querySelector('button').disabled=true;form.dataset.dirty='false';}
 }));
 function countdown() {
   document.querySelectorAll('[data-hold-countdown]').forEach(label=>{
     const minutes=Math.max(0,Math.ceil((Number(label.dataset.expires)-Date.now())/60000));
     label.textContent=minutes>0 ? 'còn '+Math.floor(minutes/60)+' giờ '+minutes%60+' phút' : 'Đã hết hạn';
     label.classList.toggle('text-warning-emphasis',minutes<30);
     if(minutes===0) {
       const card=label.closest('[data-held-id]');
       if(card?.dataset.heldId===heldBar.dataset.loaded) {
         pay.disabled=true;document.querySelector('[form="pos-hold-save"]').disabled=true;
         const message=form.querySelector('.form-message');message.hidden=false;message.textContent='Hóa đơn chờ đã hết hạn. Bắt đầu đơn mới.';
       }
       card?.remove();heldBar.dataset.count=String(Math.max(0,Number(heldBar.dataset.count)-1));
       document.querySelector('#pos-hold-counter').textContent='Chờ: '+heldBar.dataset.count+'/5';
     }
   });
 }
 countdown();setInterval(countdown,30000);
 form.addEventListener('input',saveDraft);form.addEventListener('change',saveDraft);
 form.addEventListener('submit',event=>{
   const unsaved=document.querySelector('.pos-line form[data-dirty=true]');
   if(unsaved){event.preventDefault();event.stopImmediatePropagation();const message=form.querySelector('.form-message');message.hidden=false;message.textContent='Lưu số lượng vừa sửa trước khi thanh toán.';unsaved.querySelector('[name=quantity]').focus();}
 },true);
 document.querySelectorAll('form').forEach(other=>{if(other!==form)other.addEventListener('submit',()=>{saveDraft();form.dataset.dirty='false';});});
 window.addEventListener('pageshow',e=>{if(e.persisted)location.reload();});
})();
