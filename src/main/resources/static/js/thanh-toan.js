(() => {
 const form=document.getElementById('checkoutForm');if(!form)return;
 document.title='Thanh toán · F6S Store';
 const money=n=>new Intl.NumberFormat('vi-VN').format(Number(n||0))+' ₫';
 const delivery=form.querySelector('[data-delivery-address]'),bank=form.querySelector('#bankInfo'),dialog=form.querySelector('#checkout-vouchers');
 const message=form.querySelector('[data-checkout-message]'),voucher=form.elements.voucherCode;
 const buttons=[...form.querySelectorAll('[data-delivery-submit]')],blocked=form.dataset.cartBlocked==='true';
 let request,revision=0,timer,loading=false,selectedName='',options=[];
 const payment=()=>form.querySelector('[name=paymentMethod]:checked')?.value;
 const csrf=()=>form.querySelector('[name=_csrf]')?.value||document.querySelector('meta[name=_csrf]')?.content;
 function text(selector,value){form.querySelectorAll(selector).forEach(el=>el.textContent=value);}
 function lock(){buttons.forEach(b=>b.disabled=blocked||loading||Boolean(form.dataset.submitting));}
 function deliverTo(){
  const province=form.querySelector('[data-delivery-province]'),ward=form.querySelector('[data-delivery-ward]'),street=form.querySelector('[data-delivery-street]');
  text('[data-deliver-to]',[street.value.trim(),ward.value?ward.selectedOptions[0]?.text:'',province.value?province.selectedOptions[0]?.text:''].filter(Boolean).join(', ')||'Vui lòng nhập địa chỉ nhận hàng');
 }
 function showQr(qr){
  const canvas=form.querySelector('[data-checkout-qr]'),details=form.querySelector('[data-qr-details]');
  canvas.hidden=details.hidden=!qr.configured;text('[data-qr-message]',qr.message||'');
  if(qr.configured){window.drawVietQr(canvas,qr.payload);text('[data-qr-amount]',money(qr.amount));text('[data-qr-bank]',qr.bankName);text('[data-qr-owner]',qr.accountName);text('[data-qr-account]',qr.account);text('[data-qr-content]',qr.content);}
 }
 async function preview(code=voucher.value,closeOnSuccess=false){
  clearTimeout(timer);request?.abort();request=new AbortController();const current=++revision;loading=true;lock();
  const data=new URLSearchParams(new FormData(form));data.set('voucherCode',code);data.set('_csrf',csrf());
  try{
   const response=await fetch('/checkout/preview',{method:'POST',headers:{Accept:'application/json','Content-Type':'application/x-www-form-urlencoded'},body:data,signal:request.signal});
   if(!response.headers.get('content-type')?.includes('json'))throw Error('Phiên làm việc đã hết hạn. Vui lòng tải lại trang.');
   const result=await response.json();if(current!==revision)return;if(!response.ok)throw Error(result.message||'Chưa cập nhật được đơn hàng. Vui lòng thử lại.');
   const p=result.pricing;
   voucher.value=p.valid?code.trim().toUpperCase():'';
   text('[data-subtotal]',money(p.subtotal));text('[data-total]',money(p.total));text('[data-discount]','− '+money(p.discountAmount));
   text('[data-shipping-fee]',money(p.shippingFee));
   form.querySelector('[data-discount-row]').hidden=!p.valid||Number(p.discountAmount)===0;
   message.textContent=p.valid?'':p.message;
   if(p.valid&&code){selectedName=options.find(o=>o.code.toUpperCase()===code.trim().toUpperCase())?.name||code.trim();text('[data-selected-voucher]',selectedName+' · −'+money(p.discountAmount));}
   else{selectedName='';text('[data-selected-voucher]','Chọn voucher');}
   showQr(result.qr);
   if(closeOnSuccess&&p.valid){dialog.close();form.querySelector('[data-open-vouchers]').focus();}
   else if(!p.valid)dialog.querySelector('[data-voucher-list-message]').textContent=p.message;
  }catch(error){if(error.name!=='AbortError'&&current===revision){message.textContent=error.message;dialog.querySelector('[data-voucher-list-message]').textContent=error.message;form.querySelector('[data-checkout-qr]').hidden=true;}}
  finally{if(current===revision){loading=false;lock();}}
 }
 function schedule(){clearTimeout(timer);timer=setTimeout(()=>preview(),250);}
 function paymentChanged(){
  bank.hidden=payment()!=='BANK_TRANSFER';
  if(!bank.hidden)preview();
  const label = payment()==='VNPAY' ? 'Thanh toán qua VNPay' : 'Đặt hàng';
  text('[data-order-button-label]', label);
 }
 form.querySelectorAll('[name=paymentMethod]').forEach(r=>r.addEventListener('change',paymentChanged));
 if(!payment())form.querySelector('[name=paymentMethod][value=COD]').checked=true;
 bank.hidden=payment()!=='BANK_TRANSFER';
 form.querySelectorAll('[data-address-card]').forEach(r=>r.addEventListener('change',()=>{
  const d=r.dataset;form.elements.receiverName.value=d.receiver||'';form.elements.phone.value=d.phone||'';
  delivery.dispatchEvent(new CustomEvent('delivery:select-address',{detail:{provinceId:d.province||'',wardId:d.ward||'',streetAddress:d.address||'',latitude:d.latitude||'',longitude:d.longitude||'',coordinateSource:d.source||''}}));
  const save=form.querySelector('[data-save-address]');if(save){save.checked=false;save.closest('label').hidden=Boolean(r.value);}
  deliverTo();schedule();
 }));
 const save=form.querySelector('[data-save-address]');if(save)save.closest('label').hidden=Boolean(form.querySelector('[name=shippingAddressId]:checked')?.value);
 form.addEventListener('input',event=>{if(event.target.matches('[data-delivery-street]'))deliverTo();});
 delivery.addEventListener('delivery:changed',()=>{deliverTo();schedule();});
 delivery.addEventListener('change',()=>{deliverTo();schedule();});deliverTo();

 const btnQuickSave = document.getElementById('btn-quick-save-address');
 const msgQuickSave = document.getElementById('quick-save-address-msg');
 if (btnQuickSave) {
  btnQuickSave.addEventListener('click', async () => {
   const receiverName = form.elements.receiverName?.value?.trim();
   const phone = form.elements.phone?.value?.trim();
   const province = form.querySelector('[data-delivery-province]')?.value;
   const ward = form.querySelector('[data-delivery-ward]')?.value;
   const street = form.querySelector('[data-delivery-street]')?.value?.trim();
   const lat = form.querySelector('[data-delivery-latitude]')?.value;
   const lng = form.querySelector('[data-delivery-longitude]')?.value;
   const src = form.querySelector('[data-delivery-source]')?.value;

   if (!receiverName || !phone || !province || !ward || !street) {
    if (msgQuickSave) {
     msgQuickSave.textContent = 'Vui lòng điền đủ họ tên, SĐT, tỉnh/thành, phường/xã và số nhà/đường!';
     msgQuickSave.className = 'small text-danger';
    }
    return;
   }

   btnQuickSave.disabled = true;
   if (msgQuickSave) {
    msgQuickSave.textContent = 'Đang lưu địa chỉ…';
    msgQuickSave.className = 'small text-muted';
   }

   try {
    const formData = new URLSearchParams();
    formData.append('receiverName', receiverName);
    formData.append('phone', phone);
    formData.append('provinceId', province);
    formData.append('wardId', ward);
    formData.append('streetAddress', street);
    if (lat) formData.append('latitude', lat);
    if (lng) formData.append('longitude', lng);
    if (src) formData.append('coordinateSource', src);

    const res = await fetch('/shipping/addresses/quick-save', {
     method: 'POST',
     headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
      'X-CSRF-TOKEN': csrf()
     },
     body: formData.toString()
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.message || 'Lỗi khi lưu địa chỉ');

    const list = document.getElementById('checkout-address-list');
    const cardNew = document.getElementById('card-new-address');
    if (list && cardNew) {
     const label = document.createElement('label');
     label.className = 'checkout-radio address-card';
     label.innerHTML = `
      <input type="radio" name="shippingAddressId" value="${data.id}" data-address-card
             data-receiver="${data.receiverName}" data-phone="${data.phone}"
             data-address="${data.displayStreetAddress || data.fullAddress}"
             data-ward="${data.wardId || ''}" data-province="${data.provinceId || ''}"
             data-latitude="${data.latitude || ''}" data-longitude="${data.longitude || ''}"
             data-source="${data.source || ''}">
      <span><strong>${data.receiverName}</strong><span> · ${data.phone}</span> <span class="address-full">${data.fullAddress}</span></span>
     `;
     list.insertBefore(label, cardNew);

     const newRadio = label.querySelector('input');
     newRadio.addEventListener('change', () => {
      form.elements.receiverName.value = data.receiverName || '';
      form.elements.phone.value = data.phone || '';
      delivery.dispatchEvent(new CustomEvent('delivery:select-address', {
       detail: {
        provinceId: data.provinceId || '',
        wardId: data.wardId || '',
        streetAddress: data.displayStreetAddress || '',
        latitude: data.latitude || '',
        longitude: data.longitude || '',
        coordinateSource: data.source || ''
       }
      }));
      const save=form.querySelector('[data-save-address]');if(save){save.checked=false;save.closest('label').hidden=true;}
      deliverTo();
     });
     newRadio.checked = true;
     newRadio.dispatchEvent(new Event('change', { bubbles: true }));
    }

    if (msgQuickSave) {
     msgQuickSave.textContent = 'Đã lưu địa chỉ thành công! Bạn có thể chọn "+ Dùng địa chỉ khác" để thêm tiếp.';
     msgQuickSave.className = 'small text-success fw-bold';
    }
   } catch (err) {
    if (msgQuickSave) {
     msgQuickSave.textContent = err.message;
     msgQuickSave.className = 'small text-danger';
    }
   } finally {
    btnQuickSave.disabled = false;
   }
  });
 }
 const toggle=form.querySelector('.checkout-order-toggle'),items=form.querySelector('#checkout-items'),mobile=matchMedia('(max-width:991.98px)');
 function summary(){items.hidden=mobile.matches;toggle.setAttribute('aria-expanded',String(!items.hidden));}summary();mobile.addEventListener('change',summary);
 toggle.addEventListener('click',()=>{if(!mobile.matches)return;items.hidden=!items.hidden;toggle.setAttribute('aria-expanded',String(!items.hidden));});
 const list=dialog.querySelector('[data-voucher-list]'),listMessage=dialog.querySelector('[data-voucher-list-message]');let optionRequest;
 function voucherNode(o){
  const button=document.createElement('button');button.type='button';button.className='checkout-voucher-option';button.disabled=!o.eligible;button.dataset.code=o.code;
  function line(tag,cls,value){const node=document.createElement(tag);node.className=cls;node.textContent=value;button.append(node);}
  if(o.best)line('span','voucher-best','Tốt nhất');
  line('strong','',o.name);line('span','voucher-discount','−'+money(o.discountAmount));
  line('span','voucher-detail','Mã: '+o.code);line('span','voucher-detail','Đơn tối thiểu '+money(o.minOrderValue));
  if(o.discountType==='PERCENT'&&o.maxDiscount)line('span','voucher-detail','Giảm '+o.discountValue+'% (tối đa '+money(o.maxDiscount)+')');
  if(!o.eligible)line('span','voucher-detail',Number(o.missingAmount)>0?'Mua thêm '+money(o.missingAmount)+' để dùng':o.message);
  button.addEventListener('click',()=>preview(o.code,true));return button;
 }
 function positionVouchers(){
  if(mobile.matches){dialog.style.margin='';dialog.style.left='';dialog.style.top='';return;}
  const rect=form.querySelector('[data-open-vouchers]').getBoundingClientRect();dialog.style.margin='0';dialog.style.left=Math.max(16,Math.min(rect.right-dialog.offsetWidth,innerWidth-dialog.offsetWidth-16))+'px';dialog.style.top=Math.max(16,Math.min(rect.bottom+8,innerHeight-dialog.offsetHeight-16))+'px';
 }
 window.addEventListener('resize',()=>{if(dialog.open)positionVouchers();});
 form.querySelector('[data-open-vouchers]').addEventListener('click',async()=>{
  dialog.showModal();listMessage.textContent='Đang tải voucher…';list.replaceChildren();optionRequest?.abort();optionRequest=new AbortController();
  positionVouchers();
  try{const response=await fetch('/checkout/vouchers',{headers:{Accept:'application/json'},signal:optionRequest.signal});const data=await response.json();if(!response.ok)throw Error(data.message||'Chưa tải được voucher.');options=data;list.replaceChildren(...data.map(voucherNode));listMessage.textContent=data.length?'':'Chưa có voucher đang có hiệu lực.';positionVouchers();}
  catch(error){if(error.name!=='AbortError')listMessage.textContent=error.message;}
 });
 dialog.querySelector('[data-close-vouchers]').addEventListener('click',()=>dialog.close());
 dialog.addEventListener('close',()=>{optionRequest?.abort();form.querySelector('[data-open-vouchers]').focus();});
 dialog.querySelector('[data-apply-checkout-voucher]').addEventListener('click',()=>preview(dialog.querySelector('#voucherCodeInput').value,true));
 dialog.querySelector('#voucherCodeInput').addEventListener('keydown',event=>{if(event.key==='Enter'&&!event.isComposing){event.preventDefault();preview(event.target.value,true);}});
 dialog.querySelector('[data-clear-voucher]').addEventListener('click',()=>preview('',true));
 form.addEventListener('submit',event=>{
  if(event.defaultPrevented)return;if(blocked||loading||form.dataset.submitting){event.preventDefault();return;}
  let invalid=null;const radios=new Set();
  form.querySelectorAll('[required]').forEach(field=>{
   if(field.type==='radio'){if(radios.has(field.name))return;radios.add(field.name);}
   const missing=field.type==='radio'?!form.querySelector('[name='+field.name+']:checked'):!field.value.trim();
   const error=missing?'Vui lòng nhập thông tin này.':field.validity.typeMismatch?'Giá trị không đúng định dạng.':'';
   const slot=(field.getAttribute('aria-describedby')||field.name+'-error').split(/\s+/).map(id=>document.getElementById(id)).find(Boolean);if(slot)slot.textContent=error;
   field.setAttribute('aria-invalid',String(Boolean(error)));field._comboInput?.setAttribute('aria-invalid',String(Boolean(error)));if(error&&!invalid)invalid=field;
  });
  if(invalid){event.preventDefault();const target=invalid._comboInput||invalid;target.scrollIntoView({block:'center',behavior:'smooth'});target.focus({preventScroll:true});return;}
  form.dataset.submitting='true';form.setAttribute('aria-busy','true');text('[data-order-button-label]','Đang xử lý…');lock();
 });
 window.addEventListener('pageshow',()=>{delete form.dataset.submitting;form.removeAttribute('aria-busy');text('[data-order-button-label]','Đặt hàng');lock();});
 form.querySelectorAll('[aria-invalid=true],.field-error:not(:empty)').forEach(el=>{if(el.classList.contains('field-error')&&el.textContent.trim()){const id=el.id;const field=form.querySelector('[aria-describedby="'+id+'"]');field?.setAttribute('aria-invalid','true');}});
 if(!blocked)preview();else lock();
})();
