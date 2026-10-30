document.addEventListener('DOMContentLoaded', () => {
    let index = 0;
    document.querySelectorAll('[data-material-picker]').forEach(root => {
        const radios = [...root.querySelectorAll('input[type=radio][name=variantId]')];
        if (!radios.length) return;
        const choices = document.createElement('div'); choices.className = 'd-grid gap-2 mb-3';
        const fields = [['color','Màu sắc'],['material','Chất liệu'],['sole','Đế giày'],['size','Kích cỡ']].map(([key,name]) => {
            const group=document.createElement('div'),label=document.createElement('label'),select=document.createElement('select');
            select.id=`variant-${key}-${++index}`;select.className='form-select';select.required=true;
            label.htmlFor=select.id;label.className='form-label';label.textContent=name;group.append(label,select);choices.append(group);
            if(key==='color'){const swatch=document.createElement('span');swatch.className='color-swatch';swatch.hidden=true;label.prepend(swatch);select.swatch=swatch;}
            return {key,name,select};
        });
        function update() {
            fields.forEach(({key,name,select},idx)=>{
                const previous=select.value;
                const candidates=radios.filter(r=>fields.slice(0,idx).every(f=>!f.select.value||r.dataset[f.key+'Id']===f.select.value));
                const values=new Map();candidates.forEach(r=>values.set(r.dataset[key+'Id'],r.dataset[key+'Name']));
                select.replaceChildren(new Option('Chọn '+name.toLowerCase(),''));
                values.forEach((text,id)=>{const available=candidates.some(r=>r.dataset[key+'Id']===id&&!r.disabled),option=new Option(text+(available?'':' (Hết hàng)'),id);option.disabled=!available;option.classList.toggle('text-muted',!available);select.add(option);});
                const available=[...select.options].filter(o=>o.value&&!o.disabled);
                if(available.some(o=>o.value===previous))select.value=previous;
                else if(values.size===1&&available.length===1)select.value=available[0].value;
                if(select.swatch){const hex=candidates.find(r=>r.dataset.colorId===select.value)?.dataset.colorHex;select.swatch.hidden=!hex;select.swatch.style.backgroundColor=/^#[0-9a-f]{6}$/i.test(hex||'')?hex:'';}
                select.dispatchEvent(new CustomEvent('combo:sync'));
            });
            radios.forEach(r=>r.checked=false);
            const selected=radios.find(r=>!r.disabled&&fields.every(f=>f.select.value&&r.dataset[f.key+'Id']===f.select.value));
            if(selected){selected.checked=true;selected.dispatchEvent(new Event('change',{bubbles:true}));}
        }
        fields.forEach(({select},idx)=>select.addEventListener('change',()=>{fields.slice(idx+1).forEach(f=>f.select.value='');update();}));
        root.before(choices);root.classList.remove('d-flex');root.hidden=true;update();
        // Chờ các listener giá/tồn của trang chi tiết được đăng ký trước khi tự chọn.
        setTimeout(update,0);
    });
});
