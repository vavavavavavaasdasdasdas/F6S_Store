(() => {
 document.querySelectorAll('[data-voucher-preview]').forEach(box=>{
   if(box.closest('form')?.querySelector('[data-delivery-address]'))return;
   const input=box.querySelector('[name=voucherCode]'),button=box.querySelector('[data-apply-voucher]'),message=box.querySelector('[data-voucher-message]');
   const customer=box.querySelector('[name=customerId]'),subtotal=box.querySelector('[data-subtotal]'),total=box.querySelector('[data-total]'),discount=box.querySelector('[data-discount]'),row=box.querySelector('[data-discount-row]');
   const money=n=>new Intl.NumberFormat('vi-VN').format(n)+' đ';let pending=null,revision=0;
   function reset() { revision++;pending?.abort();message.textContent='';row.hidden=true;total.textContent=money(Number(subtotal.dataset.subtotal)+Number(box.dataset.shipping||0));button.disabled=false;button.removeAttribute('aria-busy');total.dataset.amount=Number(subtotal.dataset.subtotal)+Number(box.dataset.shipping||0);box.dispatchEvent(new Event('voucherchange')); }
   input.addEventListener('input',reset);customer?.addEventListener('change',reset);
   button.addEventListener('click',async()=>{
     reset();if(!input.value.trim()){message.textContent='Vui lòng nhập mã voucher.';return;}
     const current=revision;pending=new AbortController();button.disabled=true;button.setAttribute('aria-busy','true');message.textContent='Đang kiểm tra voucher…';
     try {
       const data=new URLSearchParams({code:input.value.trim()});if(customer?.value)data.set('customerId',customer.value);
       const token=document.querySelector('meta[name=_csrf]')?.content || box.querySelector('[name=_csrf]')?.value; if(token)data.set('_csrf',token);
       const response=await fetch(box.dataset.voucherPreview,{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:data,signal:pending.signal});
       const result=await response.json();if(current!==revision)return;
       message.textContent=result.message||'Không thể áp dụng voucher.';message.classList.toggle('text-danger',!result.valid);message.classList.toggle('text-success',Boolean(result.valid));
       if(result.subtotal!=null){subtotal.dataset.subtotal=result.subtotal;subtotal.textContent=money(result.subtotal);box.dataset.shipping=result.shippingFee||0;}
       if(result.valid){row.hidden=false;discount.textContent='− '+money(result.discountAmount);total.textContent=money(result.total);}else total.textContent=money(Number(subtotal.dataset.subtotal)+Number(box.dataset.shipping||0));
       total.dataset.amount=result.valid ? result.total : Number(subtotal.dataset.subtotal)+Number(box.dataset.shipping||0);box.dispatchEvent(new Event('voucherchange'));
     } catch(error) { if(error.name!=='AbortError' && current===revision)message.textContent='Không thể kết nối. Giữ nguyên mã và thử lại.'; }
     finally { if(current===revision){button.disabled=false;button.removeAttribute('aria-busy');} }
   });
 });
})();
