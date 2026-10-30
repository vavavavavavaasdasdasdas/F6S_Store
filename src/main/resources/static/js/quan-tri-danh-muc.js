(() => {
 document.querySelectorAll('img.catalog-thumb,img.catalog-preview').forEach(img=>{
   const fallback=()=>{if(!img.src.endsWith('/anh/anh-san-pham-thay-the.svg'))img.src='/anh/anh-san-pham-thay-the.svg';};
   img.addEventListener('error',fallback);
   if(img.complete && !img.naturalWidth)fallback();
 });
 const dialog=document.getElementById('admin-confirm');
 if(!dialog) return;
 const title=dialog.querySelector('#confirm-title'), description=dialog.querySelector('#confirm-description');
 const accept=dialog.querySelector('[data-dialog-accept]'), cancel=dialog.querySelector('[data-dialog-cancel]');
 let decision=null, trigger=null;
 function ask(heading,message,label,action,source) {
   title.textContent=heading; description.textContent=message; accept.textContent=label;
   decision=action; trigger=source; dialog.showModal(); cancel.focus();
 }
 window.adminConfirm=(heading,message,label='Xác nhận',source=null)=>new Promise(resolve=>{ask(heading,message,label,()=>resolve(true),source);dialog.addEventListener('close',()=>resolve(false),{once:true});});
 cancel.addEventListener('click',()=>dialog.close());
 accept.addEventListener('click',()=>{const action=decision;dialog.close();action?.();});
 dialog.addEventListener('close',()=>{trigger?.focus();decision=null;});
 const forms=[...document.querySelectorAll('[data-admin-form]')];
 function busy(form) { form.dataset.submitting='true';form.setAttribute('aria-busy','true');form.querySelectorAll('[type=submit]').forEach(b=>b.disabled=true); }
 function error(field,message) {
   const slot=document.getElementById(field.id+'-error');
   if(slot) {slot.textContent=message;slot.hidden=!message;}
   field.setAttribute('aria-invalid',message?'true':'false');
   field.classList.toggle('is-invalid',Boolean(message));
   if(field._comboInput) {
     field._comboInput.setAttribute('aria-invalid',message?'true':'false');
     field._comboInput.classList.toggle('is-invalid',Boolean(message));
   }
 }
 function focusField(el) {
   if(!el) return;
   if(el._comboInput) el._comboInput.focus();
   else el.focus();
 }
 function validate(field) {
   if(!field.id || field.disabled || field.type==='hidden' || field.type==='submit' || field.dataset.comboInput==='true')return true;
   let message='';
   if(field.dataset.invalidChoice==='true') message='Không có lựa chọn này.';
   else if(field.required && !field.value.trim()) message='Vui lòng nhập hoặc chọn thông tin này.';
   else if(field.validity.badInput || field.validity.typeMismatch) message='Giá trị không đúng định dạng.';
   else if(field.validity.patternMismatch) message=field.dataset.formatMessage || 'Giá trị không đúng định dạng.';
   else if(field.validity.rangeUnderflow) message='Giá trị phải ≥ '+field.min+'.';
   else if(field.validity.rangeOverflow) message='Giá trị phải ≤ '+field.max+'.';
   else if(field.validity.stepMismatch) message='Số lượng phải là số nguyên; giá tối đa 2 chữ số thập phân.';
   else if(field.maxLength>0 && field.value.length>field.maxLength) message='Tối đa '+field.maxLength+' ký tự.';
   else if(field.minLength>0 && field.value && field.value.length<field.minLength) message='Ít nhất '+field.minLength+' ký tự.';
   if(field.dataset.match && field.value!==document.getElementById(field.dataset.match)?.value)message='Xác nhận mật khẩu không khớp.';
   if(field.dataset.byteLimit && new TextEncoder().encode(field.value).length>Number(field.dataset.byteLimit))message='Mật khẩu vượt quá 72 byte UTF-8.';
   if(field.id==='endDate' && field.form.matches('[data-discount-form]')) {
     const start=field.form.querySelector('#startDate').value;
     if(start && field.value && new Date(field.value)<=new Date(start))message='Kết thúc phải sau thời điểm bắt đầu.';
   }
   if(field.type==='file' && field.files[0]) {
     const f=field.files[0];
     if(!['image/png','image/jpeg','image/webp'].includes(f.type) || !/\.(png|jpe?g|webp)$/i.test(f.name))message='Chỉ chấp nhận ảnh JPG, PNG hoặc WEBP.';
     else if(f.size>Number(field.dataset.maxBytes || 5242880))message='Ảnh vượt dung lượng cho phép (5 MB).';
   }
   if(field.id==='reason' && field.hasAttribute('data-stock-reason')) {
     const expected=field.form.querySelector('[name=expectedQuantity]')?.value;
     const current=field.form.querySelector('[name=quantity]')?.value;
     if(expected && expected!==current && !field.value.trim())message='Nhập lý do khi thay đổi số lượng.';
   }
   if(field.id==='quantity' && field.form.querySelector('[name=mode]')?.value==='ADD' && Number(field.value)===0)message='Số lượng nhập thêm phải lớn hơn 0.';
   error(field,message);return !message;
 }
 forms.forEach(form=>{
   form.addEventListener('input',event=>{if(event.target.form!==form)return;if(form.method.toLowerCase()!=='get')form.dataset.dirty='true';if(event.target.getAttribute('aria-invalid')==='true')validate(event.target);});
   form.addEventListener('change',event=>{if(event.target.form!==form)return;if(form.method.toLowerCase()!=='get')form.dataset.dirty='true';});
   form.querySelectorAll('input:not([data-combo-input]),select,textarea').forEach(field=>{
     field.addEventListener('blur',()=>validate(field));
     const slot=document.getElementById(field.id+'-error');
     if(slot?.textContent.trim())error(field,slot.textContent.trim());
   });
   form.addEventListener('submit',event=>{
     if(form.dataset.submitting==='true'){event.preventDefault();return;}
     if(form.querySelector('[data-initial-variants]')) {
       const selected=Boolean(form.querySelector('[name=initialSizeIds]:checked')) || Boolean(form.querySelector('#newInitialSizeValue')?.value.trim());
       ['initialMaterialId','initialPrice','initialQuantity'].forEach(name=>{if(form.elements[name])form.elements[name].required=selected;});
     }
     let valid=true;form.querySelectorAll('input:not([data-combo-input]),select,textarea').forEach(f=>{if(!validate(f))valid=false;});
     if(!valid){event.preventDefault();focusField(form.querySelector('[aria-invalid=true]'));return;}
     if(!navigator.onLine){event.preventDefault();const msg=form.querySelector('.form-message');msg.textContent='Mất kết nối. Thông tin vẫn ở đây; kết nối lại rồi lưu.';msg.hidden=false;return;}
     busy(form);
   });
   focusField(form.querySelector('[aria-invalid=true]'));
 });
 document.querySelectorAll('[data-delete]').forEach(button=>button.addEventListener('click',()=>{
   ask('Xóa '+button.dataset.object+'?',button.dataset.consequence,'Xác nhận xóa',()=>{
     if(button.form.dataset.submitting==='true')return;
     busy(button.form);button.form.requestSubmit();
   },button);
 }));
 document.querySelectorAll('[data-confirm]').forEach(button=>button.addEventListener('click',()=>{
   ask(button.dataset.confirmTitle,button.dataset.confirmMessage,'Xác nhận',()=>{
     if(button.form.dataset.submitting==='true')return;
     busy(button.form);button.form.requestSubmit();
   },button);
 }));
 document.querySelectorAll('[data-clear-search]').forEach(button=>{
   const field=button.parentElement.querySelector('input');
   const update=()=>button.hidden=!field.value;
   update();field.addEventListener('input',update);
   button.addEventListener('click',()=>{field.value='';update();field.focus();field.form.requestSubmit();});
 });
 document.querySelectorAll('textarea').forEach(field=>{
   const grow=()=>{field.style.height='auto';field.style.height=Math.max(110,field.scrollHeight)+'px';};
   grow();field.addEventListener('input',grow);
 });
 const imageInput=document.getElementById('image'),preview=document.getElementById('image-preview');
 if(imageInput && preview) {
   const original=preview.src;let objectUrl=null;
   const clearUrl=()=>{if(objectUrl)URL.revokeObjectURL(objectUrl);objectUrl=null;};
   imageInput.addEventListener('change',()=>{
     clearUrl();preview.src=original;
     if(imageInput.files[0] && validate(imageInput)){objectUrl=URL.createObjectURL(imageInput.files[0]);preview.src=objectUrl;}
   });
   document.querySelector('[data-clear-upload]')?.addEventListener('click',()=>{clearUrl();imageInput.value='';preview.src=original;error(imageInput,'');imageInput.focus();});
   window.addEventListener('pagehide',clearUrl);
 }
 const dirty=()=>forms.some(f=>f.dataset.dirty==='true' && f.dataset.submitting!=='true');
 document.addEventListener('click',event=>{
   const link=event.target.closest('a[href]');
   if(event.defaultPrevented || !link || !dirty() || event.ctrlKey || event.metaKey || link.target==='_blank' || link.getAttribute('href').startsWith('#'))return;
   event.preventDefault();
   ask('Rời trang chưa lưu?','Thông tin vừa nhập chưa được lưu. Rời trang sẽ bỏ các thay đổi này.','Rời trang',()=>{forms.forEach(f=>f.dataset.dirty='false');location.href=link.href;},link);
 });
 window.addEventListener('beforeunload',event=>{if(dirty()){event.preventDefault();event.returnValue='';}});
 window.addEventListener('pageshow',()=>document.querySelectorAll('form[aria-busy=true]').forEach(form=>{delete form.dataset.submitting;form.removeAttribute('aria-busy');form.querySelectorAll('[type=submit]').forEach(b=>b.disabled=false);}));
})();
