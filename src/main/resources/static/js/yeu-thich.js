(() => {
 const back=()=>location.assign('/favorites/return?returnTo='+encodeURIComponent(location.pathname+location.search+location.hash));
 document.addEventListener('click',async event=>{
  const button=event.target.closest('[data-favorite-toggle]');if(!button)return;
  if(button.dataset.authenticated!=='true'){back();return;}if(button.disabled)return;
  const group=[...document.querySelectorAll('[data-favorite-toggle]')].filter(b=>b.dataset.productId===button.dataset.productId),message=document.querySelector('#store-message');
  group.forEach(b=>{b.disabled=true;b.setAttribute('aria-busy','true');});
  try{
   const token=document.querySelector('meta[name=_csrf]')?.content||document.querySelector('input[name=_csrf]')?.value;
   const response=await fetch('/favorites/'+encodeURIComponent(button.dataset.productId)+'/toggle',{method:'POST',body:new URLSearchParams({_csrf:token||''}),headers:{'Content-Type':'application/x-www-form-urlencoded',Accept:'application/json'}});
   if(response.redirected){back();return;}if(!response.ok)throw Error('Chưa cập nhật được yêu thích. Kiểm tra phiên đăng nhập rồi thử lại.');const result=await response.json();
   group.forEach(b=>{const label=result.favorite?'Bỏ yêu thích':'Thêm vào yêu thích';b.setAttribute('aria-pressed',String(result.favorite));b.setAttribute('aria-label',label);b.title=label;const icon=b.querySelector('i');icon.classList.toggle('bi-heart-fill',result.favorite);icon.classList.toggle('bi-heart',!result.favorite);});
   if(message)message.textContent=result.favorite?'Đã thêm vào sản phẩm yêu thích.':'Đã bỏ yêu thích.';
   if(location.pathname==='/favorites'&&!result.favorite)location.reload();
  }catch(error){if(message)message.textContent=error.message;}
  finally{group.forEach(b=>{b.disabled=false;b.removeAttribute('aria-busy');});}
 });
})();
