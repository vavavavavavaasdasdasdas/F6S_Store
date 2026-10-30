(() => {
    const picker=document.querySelector('[data-checkout-address]');
    picker?.addEventListener('change',() => {
        const option=picker.selectedOptions[0],form=picker.form;
        form.elements.receiverName.value=option.dataset.receiver || '';
        form.elements.phone.value=option.dataset.phone || '';
        form.querySelector('[data-delivery-address]')?.dispatchEvent(new CustomEvent('delivery:select-address',{detail:{provinceId:option.dataset.province||'',wardId:option.dataset.ward||'',streetAddress:option.dataset.address||'',latitude:option.dataset.latitude||'',longitude:option.dataset.longitude||'',coordinateSource:option.dataset.source||''}}));
        ['receiverName','phone'].forEach(name=>form.elements[name].dispatchEvent(new Event('input',{bubbles:true})));
    });
    const editor=document.querySelector('[data-address-form]');let dirty=false;
    editor?.addEventListener('input',()=>dirty=true);
    editor?.addEventListener('submit',event=>{if(!event.defaultPrevented)dirty=false;});
    const leave=async trigger=>!dirty || await storeUI.ask('Thông tin địa chỉ vừa sửa chưa được lưu. Tiếp tục sẽ bỏ các thay đổi này.',trigger,'Rời địa chỉ chưa lưu?','Bỏ thay đổi');
    document.querySelector('[data-address-selector] select')?.addEventListener('change',async event=>{
        const select=event.target;
        if(await leave(select)){dirty=false;select.form.requestSubmit();}
        else select.value=select.dataset.selected || '';
    });
    document.querySelector('[data-address-create]')?.addEventListener('click',async event=>{
        if(!dirty)return;event.preventDefault();const link=event.currentTarget;
        if(await leave(link)){dirty=false;location.href=link.href;}
    });
    window.addEventListener('beforeunload',event=>{if(dirty && editor?.dataset.busy!=='true'){event.preventDefault();event.returnValue='';}});
    document.querySelectorAll('[data-address-delete]').forEach(form=>form.addEventListener('submit',async event=>{
        event.preventDefault();if(form.dataset.busy)return;form.dataset.busy='true';
        if(await storeUI.ask('Xóa địa chỉ này? Nếu đây là địa chỉ mặc định, địa chỉ còn lại sẽ được chọn làm mặc định.',form.querySelector('button'),'Xóa địa chỉ nhận hàng?','Xóa địa chỉ')){dirty=false;form.submit();}
        else delete form.dataset.busy;
    }));
    document.querySelector('[data-address-form] [aria-invalid=true]')?.focus();
})();
