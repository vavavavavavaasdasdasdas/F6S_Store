document.addEventListener('DOMContentLoaded', () => {
  let pending = false;
  let debounceTimers = {};
  const controls = [...document.querySelectorAll('.cart-qty-input,.cart-remove-btn')];

  const change = async (url, data) => {
    if (pending) return;
    pending = true;
    controls.forEach(c => c.disabled = true);
    document.getElementById('store-content')?.setAttribute('aria-busy', 'true');
    storeUI.message('Đang cập nhật giỏ hàng…');
    try {
      await storeUI.post(url, data);
      location.reload();
    } catch (error) {
      storeUI.message(error.message || 'Không thể kết nối. Giỏ hàng được giữ nguyên.');
      pending = false;
      controls.forEach(c => c.disabled = false);
      document.getElementById('store-content')?.removeAttribute('aria-busy');
    }
  };

  document.querySelectorAll('.cart-qty-input').forEach(input => {
    const original = input.value;
    const variantId = input.dataset.variantId;
    input.setAttribute('inputmode', 'numeric');

    const triggerUpdate = (qty) => {
      clearTimeout(debounceTimers[variantId]);
      debounceTimers[variantId] = setTimeout(() => {
        change('/cart/update-ajax', { variantId, quantity: qty }).then(() => {
          if (!pending) input.value = original;
        });
      }, 400);
    };

    input.addEventListener('input', () => {
      let raw = input.value.replace(/[^0-9]/g, '');
      if (raw === '') return; // Allow clearing while typing
      let quantity = Number(raw);
      const max = Number(input.max) || 999999;
      if (quantity > max) {
        quantity = max;
        input.value = quantity;
        storeUI.message('Chỉ còn ' + max + ' sản phẩm');
      } else {
        input.value = quantity;
      }
      if (quantity >= 1) {
        triggerUpdate(quantity);
      }
    });

    input.addEventListener('blur', () => {
      let raw = input.value.trim();
      let quantity = Number(raw);
      const max = Number(input.max) || 999999;
      if (raw === '' || isNaN(quantity) || quantity < 1) {
        input.value = 1;
        triggerUpdate(1);
      } else if (quantity > max) {
        input.value = max;
        triggerUpdate(max);
      }
    });

    input.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') {
        e.preventDefault();
        input.blur();
      }
    });
  });

  document.querySelectorAll('.cart-remove-btn').forEach(button =>
    button.addEventListener('click', async () => {
      if (pending) return;
      if (await storeUI.ask(button.getAttribute('aria-label') + ' khỏi giỏ hàng?', button)) {
        change('/cart/remove-ajax', { variantId: button.dataset.variantId });
      }
    })
  );
});
