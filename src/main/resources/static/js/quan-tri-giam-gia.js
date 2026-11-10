(() => {
 const form=document.querySelector('[data-discount-form]');
 if(form){
  const type=form.querySelector('#discountType');
  function sync(){
   if(!type)return;const percent=type.value==='PERCENT',panel=form.querySelector('[data-percent-only]'),max=form.querySelector('#maxDiscount'),value=form.querySelector('#discountValue');
   if(panel)panel.hidden=!percent;if(max)max.disabled=!percent;
   value.min=percent?'1':form.dataset.discountForm==='promotion'?'1':'0.01';
   if(percent)value.max='100';else value.removeAttribute('max');
  }
  type?.addEventListener('change',sync);sync();
 }
 document.querySelectorAll('[data-discount-switch]').forEach(form=>{
  const input=form.querySelector('[role=switch]'),message=form.querySelector('[data-switch-error]');
  input.addEventListener('change',async()=>{
   const desired=input.checked,data=new FormData(form);data.set('active',String(desired));
   input.disabled=true;form.setAttribute('aria-busy','true');message.textContent='';
   try{const response=await fetch(form.action,{method:'POST',body:data,headers:{'X-Requested-With':'XMLHttpRequest'}});if(!response.ok)throw Error('Chưa cập nhật được. Kiểm tra kết nối và phiên đăng nhập rồi thử lại.');const result=await response.json();input.checked=result.active;form.querySelector('[data-effectivity-label]').textContent=result.stateLabel;}
   catch(error){input.checked=!desired;message.textContent=error.message;}
   finally{input.disabled=false;form.removeAttribute('aria-busy');}
  });
 });
 const selection=document.querySelector('[data-promotion-selection]');
 if(!selection)return;
 const count=selection.querySelector('[data-selection-count]'),inputs=[...selection.querySelectorAll('[name=productIds]')],key='promotion-selection-'+selection.dataset.promotionId;
 let draft={},stored=true;
 try{if(selection.dataset.selectionSaved==='true')sessionStorage.removeItem(key);else draft=JSON.parse(sessionStorage.getItem(key)||'{}');}catch(_){stored=false;}
 const update=()=>count.textContent='Đã chọn '+inputs.filter(input=>input.checked).length+' biến thể trên trang này'+(Object.keys(draft).length?' · Có lựa chọn chờ lưu':'');
 function persist(){try{sessionStorage.setItem(key,JSON.stringify(draft));stored=true;}catch(_){stored=false;count.textContent='Không lưu được lựa chọn trong trình duyệt. Hãy Lưu thay đổi trước khi chuyển trang.';}setTimeout(()=>{selection.dataset.dirty=stored?'false':'true';},0);}
 inputs.forEach(input=>{if(Object.prototype.hasOwnProperty.call(draft,input.value))input.checked=draft[input.value]===true;input.addEventListener('change',()=>{draft[input.value]=input.checked;update();persist();});});
 selection.querySelectorAll('[data-selection-all]').forEach(button=>button.addEventListener('click',()=>{inputs.forEach(input=>{input.checked=button.dataset.selectionAll==='true';draft[input.value]=input.checked;});update();persist();}));
 selection.addEventListener('submit',()=>{
  const visible=new Set(inputs.map(input=>input.value));
  selection.querySelectorAll('[data-draft-field]').forEach(input=>input.remove());
  for(const [id,chosen] of Object.entries(draft))if(!visible.has(id)){
   for(const name of chosen?['visibleIds','productIds']:['visibleIds']){const hidden=document.createElement('input');hidden.type='hidden';hidden.name=name;hidden.value=id;hidden.dataset.draftField='true';selection.append(hidden);}
  }
 });
 update();
})();
