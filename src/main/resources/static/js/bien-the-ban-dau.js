document.addEventListener('DOMContentLoaded', () => {
    const preview = document.querySelector('[data-initial-preview]');
    if (!preview) return;
    const form = preview.closest('form');
    const update = () => {
        const sizes = new Set([...form.querySelectorAll('[name=initialSizeIds]:checked')].map(input => input.value));
        const typed = form.querySelector('#newInitialSizeValue')?.value.trim();
        preview.textContent = `Sẽ tạo ${sizes.size + (typed ? 1 : 0)} biến thể`;
    };
    form.addEventListener('change', update);
    form.addEventListener('input', update);
    update();
});
