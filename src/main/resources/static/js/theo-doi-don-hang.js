(() => {
 const dialog=document.querySelector('#order-transition-dialog');if(!dialog)return;
 const form=dialog.querySelector('form'),submit=form.querySelector('[type=submit]');let trigger;
 document.querySelectorAll('[data-transition]').forEach(button=>button.addEventListener('click',()=>{
  trigger=button;form.reset();delete form.dataset.submitting;delete form.dataset.saving;form.dataset.dirty='false';form.setAttribute('aria-busy','false');
  form.querySelectorAll('[aria-invalid]').forEach(field=>{field.setAttribute('aria-invalid','false');field.classList.remove('is-invalid');});form.querySelectorAll('.field-error').forEach(slot=>{slot.textContent='';slot.hidden=true;});
  form.elements.status.value=button.dataset.transition;
  dialog.querySelector('[data-transition-title]').textContent=button.textContent;
  const shipping=button.dataset.transition==='SHIPPING',reason=['CANCELLED','DELIVERY_FAILED'].includes(button.dataset.transition);
  dialog.querySelector('[data-shipping-fields]').hidden=!shipping;
  dialog.querySelector('[data-reason-field]').hidden=!reason;form.elements.reason.required=reason;
  form.elements.carrier.disabled=!shipping;form.elements.trackingCode.disabled=!shipping;form.elements.reason.disabled=!reason;
  submit.disabled=false;dialog.showModal();(reason?form.elements.reason:shipping?form.elements.carrier:submit).focus();
 }));
 dialog.querySelector('[data-close-transition]').addEventListener('click',()=>{dialog.close();});
 dialog.addEventListener('close',()=>{form.dataset.dirty='false';trigger?.focus();});
 form.addEventListener('submit',event=>{if(event.defaultPrevented)return;if(form.dataset.saving){event.preventDefault();return;}form.dataset.saving='true';form.setAttribute('aria-busy','true');});
})();
