(() => {
 const form=document.querySelector('[data-discount-form]');
 if(form) {
   const percent=form.querySelector('#discountType'),visibility=form.querySelector('#isPublic'),customer=form.querySelector('#customerId');
   function sync() {
     if(percent) { const yes=percent.value==='PERCENT';form.querySelector('[data-percent-only]').hidden=!yes;const max=form.querySelector('#maxDiscount');max.disabled=!yes;const value=form.querySelector('#discountValue');value.min=yes?'1':'0.01';if(yes)value.max='100';else value.removeAttribute('max'); }
     if(visibility) { const privateVoucher=visibility.value==='false';form.querySelector('[data-private-only]').hidden=!privateVoucher;customer.required=privateVoucher;customer.disabled=!privateVoucher;customer.dispatchEvent(new CustomEvent('combo:sync')); }
   }
   percent?.addEventListener('change',sync);visibility?.addEventListener('change',sync);sync();
 }
 const selection=document.querySelector('[data-selection-count]');
 if(selection){const update=()=>selection.textContent='Đã chọn '+document.querySelectorAll('[name=productIds]:checked').length+' sản phẩm trên trang này';document.querySelectorAll('[name=productIds]').forEach(input=>input.addEventListener('change',update));update();}
})();
