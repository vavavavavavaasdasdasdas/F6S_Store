/**
 * F6S Store - Hệ thống xử lý ô nhập số thông minh (Numeric Input System)
 * Giai đoạn 20: Cho phép gõ tay, dán chuỗi có định dạng, chặn ký tự lạ, tự điều chỉnh [min, max],
 * debounce AJAX cập nhật giỏ hàng/POS.
 */
(() => {
  'use strict';

  function sanitizeDigits(str) {
    return (str || '').replace(/[^0-9]/g, '');
  }

  function formatThousand(val) {
    if (val === '' || val == null || isNaN(val)) return '';
    return new Intl.NumberFormat('vi-VN').format(val);
  }

  function initNumericInputs(root = document) {
    const inputs = root.querySelectorAll(
      'input[type="number"], input[inputmode="numeric"], input[data-numeric], .cart-qty-input, #quantity, #pos-quantity, #cashReceived, input[name="quantity"], input[name="price"], #initialPrice, #initialQuantity, #discountValue, #minOrderValue, #maxDiscount'
    );

    inputs.forEach(input => {
      if (input.dataset.numInitialized === 'true') return;
      input.dataset.numInitialized = 'true';

      // Đảm bảo inputmode="numeric" trên thiết bị di động
      if (!input.getAttribute('inputmode')) {
        input.setAttribute('inputmode', 'numeric');
      }

      // Xóa readonly nếu có trên các ô số lượng
      if (input.id === 'quantity' && input.hasAttribute('readonly')) {
        input.removeAttribute('readonly');
      }

      // 1. Chặn phím không hợp lệ (e, E, +, -) cho số nguyên dương
      input.addEventListener('keydown', e => {
        // Cho phép phím điều hướng và phím tắt
        if (
          [
            'Backspace', 'Delete', 'Tab', 'Escape', 'Enter',
            'ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown',
            'Home', 'End'
          ].includes(e.key) ||
          ((e.ctrlKey || e.metaKey) && ['a', 'c', 'v', 'x', 'z'].includes(e.key.toLowerCase()))
        ) {
          if (e.key === 'Enter') {
            e.preventDefault();
            input.blur();
          }
          return;
        }

        // Chặn phím âm, số mũ, dấu chấm cho các ô số lượng nguyên
        const isDecimalAllowed = input.getAttribute('step') && input.getAttribute('step').includes('.');
        if (['e', 'E', '+', '-'].includes(e.key)) {
          e.preventDefault();
          return;
        }
        if (!isDecimalAllowed && (e.key === '.' || e.key === ',')) {
          e.preventDefault();
          return;
        }
        if (!/^[0-9]$/.test(e.key) && (!isDecimalAllowed || (e.key !== '.' && e.key !== ','))) {
          e.preventDefault();
        }
      });

      // 2. Xử lý dán (paste): Cho phép dán chuỗi có dấu phân cách ("1.500.000", "  200 ")
      input.addEventListener('paste', e => {
        const pasteData = (e.clipboardData || window.clipboardData).getData('text');
        if (!pasteData) return;

        const isDecimal = input.getAttribute('step') && input.getAttribute('step').includes('.');
        let cleaned = pasteData.trim();
        if (!isDecimal) {
          cleaned = sanitizeDigits(cleaned);
        } else {
          cleaned = cleaned.replace(/[^0-9.,]/g, '').replace(',', '.');
        }

        if (cleaned) {
          e.preventDefault();
          const start = input.selectionStart || 0;
          const end = input.selectionEnd || 0;
          const current = input.value;
          const updated = current.slice(0, start) + cleaned + current.slice(end);
          input.value = updated;
          input.dispatchEvent(new Event('input', { bubbles: true }));
          input.dispatchEvent(new Event('change', { bubbles: true }));
        }
      });

      // 3. Giới hạn [min, max] khi người dùng nhập liệu (input)
      input.addEventListener('input', () => {
        const raw = input.value;
        if (raw === '') return; // Cho phép người dùng xóa trắng để gõ lại

        const minAttr = input.getAttribute('min');
        const maxAttr = input.getAttribute('max');
        const min = minAttr !== null && minAttr !== '' ? Number(minAttr) : null;
        const max = maxAttr !== null && maxAttr !== '' ? Number(maxAttr) : null;

        const val = Number(raw);
        if (!isNaN(val)) {
          if (max !== null && val > max) {
            input.value = max;
            // Thông báo nếu vượt tồn kho
            showStockWarning(input, max);
          } else if (min !== null && val < 0 && min >= 0) {
            input.value = min;
          }
        }
      });

      // 4. Khi rời ô (blur): Tự quay về giá trị hợp lệ gần nhất nếu để trống hoặc không hợp lệ
      input.addEventListener('blur', () => {
        const raw = input.value.trim();
        const minAttr = input.getAttribute('min');
        const maxAttr = input.getAttribute('max');
        const min = minAttr !== null && minAttr !== '' ? Number(minAttr) : null;
        const max = maxAttr !== null && maxAttr !== '' ? Number(maxAttr) : null;

        if (raw === '' || isNaN(Number(raw))) {
          // Trống: lấy min hoặc 1 cho ô số lượng
          const fallback = min !== null ? min : (input.name === 'quantity' || input.id === 'quantity' ? 1 : 0);
          input.value = fallback;
          input.dispatchEvent(new Event('change', { bubbles: true }));
        } else {
          let val = Number(raw);
          if (min !== null && val < min) {
            val = min;
            input.value = val;
            input.dispatchEvent(new Event('change', { bubbles: true }));
          } else if (max !== null && val > max) {
            val = max;
            input.value = val;
            input.dispatchEvent(new Event('change', { bubbles: true }));
          }
        }
      });
    });
  }

  function showStockWarning(input, max) {
    let msgEl = input.parentElement?.querySelector('.field-error, .stock-warning');
    if (!msgEl) {
      msgEl = document.getElementById('stock-status') || document.getElementById('size-error');
    }
    if (msgEl) {
      msgEl.textContent = 'Chỉ còn ' + max + ' sản phẩm';
      msgEl.classList.remove('d-none');
    }
    if (window.storeUI && typeof window.storeUI.message === 'function') {
      window.storeUI.message('Chỉ còn ' + max + ' sản phẩm');
    }
  }

  // Khởi chạy khi DOM sẵn sàng
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => initNumericInputs());
  } else {
    initNumericInputs();
  }

  // Hỗ trợ khởi tạo lại sau khi tải AJAX/dynamic content
  window.initNumericInputs = initNumericInputs;
})();
