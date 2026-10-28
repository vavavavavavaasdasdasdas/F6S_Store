(() => {
 const dialog=document.getElementById('store-confirm');
 const message=(text)=>{const slot=document.getElementById('store-message');slot.textContent=text;};
 const busy=(form,value)=>{form.setAttribute('aria-busy',String(value));form.querySelectorAll('button[type=submit]').forEach(b=>b.disabled=value);};
 const post=async(url,data)=>{
  const csrf=document.querySelector('meta[name=_csrf]')?.content;if(csrf)data._csrf=csrf;
  let response;
  try { response=await fetch(url,{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded','Accept':'application/json'},body:new URLSearchParams(data)}); }
  catch { throw new Error('Không thể kết nối. Giỏ hàng được giữ nguyên; vui lòng thử lại.'); }
  const result=await response.json();if(!response.ok)throw new Error(result.error||result.message||'Không thể thực hiện thao tác.');return result;
 };
 const ask=(text,trigger,title='Xóa khỏi giỏ hàng?',accept='Xóa')=>new Promise(resolve=>{
  document.getElementById('store-confirm-title').textContent=title;dialog.querySelector('[data-store-accept]').textContent=accept;
  document.getElementById('store-confirm-description').textContent=text;let accepted=false;
  const yes=()=>{accepted=true;dialog.close();};const no=()=>dialog.close();
  dialog.querySelector('[data-store-accept]').addEventListener('click',yes,{once:true});
  dialog.querySelector('[data-store-cancel]').addEventListener('click',no,{once:true});
  dialog.addEventListener('close',()=>{dialog.querySelector('[data-store-accept]').removeEventListener('click',yes);dialog.querySelector('[data-store-cancel]').removeEventListener('click',no);trigger?.focus();resolve(accepted);},{once:true});
  dialog.showModal();dialog.querySelector('[data-store-cancel]').focus();
 });
 window.storeUI={message,busy,post,ask};
 document.querySelectorAll("[data-cancel-order]").forEach(form=>form.addEventListener("submit",async event=>{
  event.preventDefault();if(form.dataset.busy)return;form.dataset.busy="true";busy(form,true);
  if(await ask("Bạn muốn hủy đơn hàng này? Hàng đã trừ sẽ được hoàn kho.",form.querySelector("button"),"Hủy đơn hàng?","Đồng ý hủy")){form.submit();}
  else{delete form.dataset.busy;busy(form,false);form.querySelector("button").focus();}
 }));
 document.querySelectorAll('img').forEach(img=>{const fallback=()=>{if(!img.src.endsWith('/anh/anh-san-pham-thay-the.svg'))img.src='/anh/anh-san-pham-thay-the.svg';};img.addEventListener('error',fallback);if(img.complete&&!img.naturalWidth)fallback();});
 document.querySelectorAll('form[data-user-form]').forEach((form,index)=>{
  const fields=[...form.querySelectorAll('input:not([type=hidden]):not([data-combo-input]),textarea,select')];
  fields.forEach(field=>{
   if(!field.id)field.id='user-'+index+'-'+field.name;
   const label=field.parentElement.querySelector('label');if(label&&!label.htmlFor)label.htmlFor=field.id;
   let error=document.getElementById(field.id+'-error');
   if(!error){error=document.createElement('p');error.id=field.id+'-error';error.className='user-message';error.setAttribute('aria-live','polite');(field._comboWrapper||field).after(error);}
   field.setAttribute('aria-describedby',[...new Set([...(field.getAttribute('aria-describedby')||'').split(/\s+/).filter(Boolean),error.id])].join(' '));
  });
  form.addEventListener('submit',event=>{
   if(form.dataset.busy){event.preventDefault();return;}
   let invalid=null;
   fields.forEach(field=>{
    let text=field.dataset.invalidChoice==='true'?'Không có lựa chọn này.':field.required&&!field.value.trim()?'Vui lòng nhập thông tin này.':field.validity.typeMismatch?'Giá trị không đúng định dạng.':'';
    if(field.name==='confirmPassword'&&field.value!==form.querySelector('[name=password],[name=newPassword]')?.value)text='Mật khẩu xác nhận không khớp.';
    const slot=document.getElementById(field.id+'-error');slot.textContent=text;field.setAttribute('aria-invalid',String(Boolean(text)));
    if(field._comboInput)field._comboInput.setAttribute('aria-invalid',String(Boolean(text)));
    if(text&&!invalid)invalid=field;
   });
   if(invalid){event.preventDefault();(invalid._comboInput||invalid).focus();return;}
   if(!navigator.onLine){event.preventDefault();message('Mất kết nối. Thông tin vẫn được giữ; kết nối lại rồi thử.');return;}
   form.dataset.busy='true';busy(form,true);
  });
 });
 window.addEventListener('pageshow',()=>document.querySelectorAll('form[data-user-form]').forEach(form=>{delete form.dataset.busy;busy(form,false);}));
 const filter=document.getElementById('filterForm');filter?.querySelectorAll('[type=radio]').forEach(input=>input.addEventListener('change',()=>filter.requestSubmit()));
 document.getElementById('sortSelect')?.addEventListener('change',event=>{document.getElementById('sortInput').value=event.target.value;filter.requestSubmit();});
})();
