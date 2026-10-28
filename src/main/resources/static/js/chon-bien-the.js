document.addEventListener('DOMContentLoaded', () => {
    let pickerIndex = 0;
    document.querySelectorAll('[data-material-picker]').forEach(root => {
        const radios = [...root.querySelectorAll('input[type=radio][name=variantId]')];
        if (!radios.length) return;
        const idx = ++pickerIndex;
        const choices = document.createElement('div');
        choices.className = 'd-grid gap-2 mb-3';
        const create = (text, key) => {
            const group = document.createElement('div');
            const label = document.createElement('label');
            const id = `picker-${key}-${idx}`;
            label.className = 'form-label';
            label.htmlFor = id;
            label.textContent = text;
            const select = document.createElement('select');
            select.id = id;
            select.className = 'form-select';
            select.required = true;
            select.add(new Option('Chọn ' + text.toLowerCase(), ''));
            group.append(label, select);
            choices.append(group);
            return select;
        };
        const material = create('Chất liệu', 'material'), size = create('Kích cỡ', 'size');
        const unique = (key, name, target) => {
            const values = new Map(); radios.forEach(r => values.set(r.dataset[key], r.dataset[name]));
            values.forEach((label, id) => target.add(new Option(label, id))); return values.size;
        };
        const materialCount = unique('materialId', 'materialName', material);
        unique('sizeId', 'sizeName', size);
        const update = changed => {
            if (changed === material) {
                const match = radios.some(r => r.dataset.materialId === material.value && r.dataset.sizeId === size.value && !r.disabled);
                if (!match) size.value = '';
            }
            for (const option of size.options) {
                if (!option.value) continue;
                const available = radios.some(r => r.dataset.sizeId === option.value && (!material.value || r.dataset.materialId === material.value) && !r.disabled);
                option.disabled = !available;
                option.classList.toggle('text-muted', !available);
            }
            for (const option of material.options) {
                if (!option.value) continue;
                option.disabled = !radios.some(r => r.dataset.materialId === option.value && !r.disabled);
            }
            radios.forEach(r => r.checked = false);
            const selected = radios.find(r => r.dataset.materialId === material.value && r.dataset.sizeId === size.value && !r.disabled);
            if (selected) { selected.checked = true; selected.dispatchEvent(new Event('change', {bubbles:true})); }
            material.dispatchEvent(new CustomEvent('combo:sync'));
            size.dispatchEvent(new CustomEvent('combo:sync'));
        };
        material.addEventListener('change', () => update(material)); size.addEventListener('change', () => update(size));
        root.before(choices); root.classList.remove('d-flex'); root.hidden = true;
        if (materialCount === 1) material.selectedIndex = 1;
        update(material);
    });
});
