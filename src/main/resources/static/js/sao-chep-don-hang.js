(() => {
 const button=document.querySelector('[data-copy-order-message]'),field=document.querySelector('#order-confirmation-message'),status=document.querySelector('[data-copy-message-status]');if(!button||!field)return;
 button.addEventListener('click',async()=>{try{if(navigator.clipboard?.writeText)await navigator.clipboard.writeText(field.value);else{field.hidden=false;field.select();const copied=document.execCommand('copy');field.hidden=true;if(!copied)throw Error();}status.textContent='Đã sao chép tin nhắn xác nhận.';}catch(error){field.hidden=false;field.select();status.textContent='Trình duyệt chưa cho sao chép. Bạn có thể chọn và sao chép nội dung bên dưới.';}});
})();
