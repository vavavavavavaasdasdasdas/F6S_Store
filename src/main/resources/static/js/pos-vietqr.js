document.addEventListener('DOMContentLoaded',()=>{
    const form=document.querySelector('#pos-payment');if(!form)return;
    const method=form.querySelector('[name=paymentMethod]'),panel=form.querySelector('#pos-transfer-panel'),received=form.querySelector('#transferReceived');
    const amount=form.querySelector('#transferAmount'),canvas=form.querySelector('#pos-qr'),details=form.querySelector('#pos-bank-details'),message=form.querySelector('#pos-qr-message'),pay=form.querySelector('#pos-pay');
    const unavailable=pay.disabled;let pending=null,revision=0,loading=false;
    function lock(){pay.disabled=unavailable||form.dataset.submitting==='true'||(method.value==='TRANSFER'&&(!received.checked||!amount.value||loading));}
    async function refresh(){
        const current=++revision;pending?.abort();received.checked=false;received.disabled=true;amount.value='';canvas.hidden=true;details.hidden=true;
        const active=method.value==='TRANSFER';panel.hidden=!active;
        if(!active||pay.dataset.cartEmpty==='true'){loading=false;lock();return;}
        pending=new AbortController();loading=true;lock();message.textContent='Đang tạo mã QR…';
        const discountRow=form.querySelector('[data-discount-row]');
        const data=new URLSearchParams({_csrf:form.querySelector('[name=_csrf]').value,voucherCode:discountRow.hidden?'':form.querySelector('[name=voucherCode]').value});
        if(form.querySelector('[name=customerId]').value)data.set('customerId',form.querySelector('[name=customerId]').value);
        try{
            const response=await fetch('/admin/pos/payment-qr',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:data,signal:pending.signal});
            if(!response.ok)throw Error('Không tạo được QR. Kiểm tra phiên đăng nhập rồi thử lại.');
            const qr=await response.json();if(current!==revision)return;
            const displayed=(form.querySelector('[data-total]').dataset.amount||'').replace(/\.0+$/,'');
            if(qr.amount!=null&&displayed!==qr.amount){message.textContent='Giá hoặc phiếu giảm giá đã thay đổi. Tải lại giỏ trước khi chuyển khoản.';return;}
            message.textContent=qr.message||'Quét QR và kiểm tra tiền đã vào tài khoản trước khi xác nhận.';
            amount.value=qr.amount??'';received.disabled=qr.amount==null;
            if(qr.configured){window.drawVietQr(canvas,qr.payload);canvas.hidden=false;details.hidden=false;
                details.querySelector('[data-qr-amount]').textContent=new Intl.NumberFormat('vi-VN').format(BigInt(qr.amount))+' đ';
                details.querySelector('[data-qr-bank]').textContent=qr.bankName;details.querySelector('[data-qr-owner]').textContent=qr.accountName;
                details.querySelector('[data-qr-account]').textContent=qr.account;details.querySelector('[data-qr-reference]').textContent=qr.content;
            }
        }catch(error){if(error.name!=='AbortError'&&current===revision)message.textContent=error.message;}
        finally{if(current===revision){loading=false;lock();}}
    }
    received.addEventListener('change',lock);method.addEventListener('change',refresh);form.addEventListener('voucherchange',refresh);
    form.addEventListener('submit',event=>{if(method.value==='TRANSFER'&&(!received.checked||!amount.value||loading)){event.preventDefault();event.stopImmediatePropagation();message.textContent='Cần xác nhận đã nhận đủ số tiền.';}},true);
    refresh();
});
