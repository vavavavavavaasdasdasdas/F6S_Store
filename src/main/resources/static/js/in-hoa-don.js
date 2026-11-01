document.querySelector('[data-print-invoice]')?.addEventListener('click', () => window.print());

if(new URLSearchParams(location.search).get('print')==='true')window.addEventListener('load',()=>window.print());

 document.querySelector('[data-copy-confirmation]')?.addEventListener('click',async()=>{const text=document.querySelector('#order-confirmation-text'),status=document.querySelector('[data-copy-confirmation-status]');try{await navigator.clipboard.writeText(text.value);status.textContent='Đã sao chép tin nhắn xác nhận.';}catch(_){text.focus();text.select();status.textContent='Chưa sao chép tự động được. Nhấn Ctrl+C để sao chép nội dung đã chọn.';}});
