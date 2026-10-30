(() => {
 const online=document.querySelector('#pos-online-order'),counter=document.querySelector('#pos-payment');if(!online||!counter)return;
 const modes=document.querySelectorAll('[name=posSaleMode]');
 const sync=()=>{const enabled=[...modes].find(input=>input.checked)?.value==='ONLINE';online.hidden=!enabled;counter.hidden=enabled;document.dispatchEvent(new Event('delivery:visible'));};
 modes.forEach(input=>input.addEventListener('change',sync));
 if(online.dataset.open==='true')document.querySelector('[name=posSaleMode][value=ONLINE]').checked=true;
 const method=online.querySelector('#online-payment'),note=online.querySelector('[data-online-bank-note]');const payment=()=>note.hidden=method.value!=='BANK_TRANSFER';method.addEventListener('change',payment);payment();sync();
})();
