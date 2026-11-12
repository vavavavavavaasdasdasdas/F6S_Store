document.addEventListener("DOMContentLoaded", function() {
    document.body.classList.add("has-mobile-actions");
    const selectSize = document.getElementById('select-size');
    const selectedVariantId = document.getElementById('selectedVariantId');

    const qtyInput = document.getElementById('quantity');
    const btnMinus = document.getElementById('btn-minus');
    const btnPlus = document.getElementById('btn-plus');
    const stockStatus = document.getElementById('stock-status');
    const sizeError = document.getElementById('size-error');

    const displayFinalPrice = document.getElementById('displayFinalPrice');
    const displayOriginalPrice = document.getElementById('displayOriginalPrice');
    const displayDiscount = document.getElementById('displayDiscount');

    const btnAddCart = document.getElementById('btn-add-cart');
    const btnBuyNow = document.getElementById('btn-buy-now');
    const btnMobileBuy = document.getElementById('btn-mobile-buy');

    let currentMaxQty = 0;

    function formatCurrency(value) {
        return new Intl.NumberFormat('vi-VN').format(value) + ' ₫';
    }

    function onSizeChange() {
        if (sizeError) sizeError.classList.add('d-none');
        if (!selectSize || !selectSize.value) {
            if (selectedVariantId) selectedVariantId.value = '';
            currentMaxQty = 0;
            if (stockStatus) {
                stockStatus.textContent = "Vui lòng chọn kích cỡ để xem số lượng còn lại";
                stockStatus.className = "ms-3 text-muted";
            }
            if (qtyInput) {
                qtyInput.value = 1;
                qtyInput.max = 1;
            }
            return;
        }

        const selectedOption = selectSize.options[selectSize.selectedIndex];
        if (!selectedOption) return;

        if (selectedVariantId) selectedVariantId.value = selectSize.value;
        const qty = parseInt(selectedOption.dataset.qty, 10) || 0;
        currentMaxQty = qty;

        if (qty > 0) {
            if (qtyInput) {
                qtyInput.max = qty;
                qtyInput.value = 1;
            }
            if (stockStatus) {
                stockStatus.textContent = "Còn " + qty + " sản phẩm";
                stockStatus.className = "ms-3 text-success fw-bold";
            }
            if (btnAddCart) btnAddCart.disabled = false;
            if (btnBuyNow) btnBuyNow.disabled = false;
            if (btnMobileBuy) btnMobileBuy.disabled = false;
        } else {
            if (qtyInput) {
                qtyInput.max = 0;
                qtyInput.value = 0;
            }
            if (stockStatus) {
                stockStatus.textContent = "Tạm hết hàng (0 sản phẩm)";
                stockStatus.className = "ms-3 text-danger fw-bold";
            }
            if (btnAddCart) btnAddCart.disabled = true;
            if (btnBuyNow) btnBuyNow.disabled = true;
            if (btnMobileBuy) btnMobileBuy.disabled = true;
        }

        // Cập nhật giá bán và khuyến mãi tương ứng
        const finalPrice = parseFloat(selectedOption.dataset.finalprice);
        const origPrice = parseFloat(selectedOption.dataset.origprice);
        const hasPromo = selectedOption.dataset.haspromo === 'true';
        const discount = selectedOption.dataset.discount;

        if (!isNaN(finalPrice) && displayFinalPrice) {
            displayFinalPrice.textContent = formatCurrency(finalPrice);
        }
        if (hasPromo) {
            if (displayOriginalPrice && !isNaN(origPrice)) {
                displayOriginalPrice.textContent = formatCurrency(origPrice);
                displayOriginalPrice.classList.remove('d-none');
            }
            if (displayDiscount && discount) {
                displayDiscount.textContent = "GIẢM -" + discount + "%";
                displayDiscount.classList.remove('d-none');
            }
        } else {
            if (displayOriginalPrice) displayOriginalPrice.classList.add('d-none');
            if (displayDiscount) displayDiscount.classList.add('d-none');
        }
    }

    if (selectSize) {
        selectSize.addEventListener('change', onSizeChange);
        // Khởi tạo trạng thái ban đầu: chưa chọn kích cỡ
        onSizeChange();
    }

    if (btnMinus && qtyInput) {
        btnMinus.addEventListener('click', function() {
            let val = parseInt(qtyInput.value, 10) || 1;
            if (val > 1) {
                qtyInput.value = val - 1;
                qtyInput.dispatchEvent(new Event('input', { bubbles: true }));
            }
        });
    }

    if (btnPlus && qtyInput) {
        btnPlus.addEventListener('click', function() {
            let val = parseInt(qtyInput.value, 10) || 0;
            if (currentMaxQty > 0 && val < currentMaxQty) {
                qtyInput.value = val + 1;
                qtyInput.dispatchEvent(new Event('input', { bubbles: true }));
            } else if (currentMaxQty > 0 && val >= currentMaxQty) {
                if (stockStatus) {
                    stockStatus.textContent = "Chỉ còn " + currentMaxQty + " sản phẩm";
                    stockStatus.className = "ms-3 text-warning-emphasis fw-bold";
                }
            }
        });
    }

    if (qtyInput) {
        qtyInput.addEventListener('input', function() {
            let raw = this.value.replace(/[^0-9]/g, '');
            if (raw === '') return;
            let val = parseInt(raw, 10);
            if (currentMaxQty > 0 && val > currentMaxQty) {
                this.value = currentMaxQty;
                if (stockStatus) {
                    stockStatus.textContent = "Chỉ còn " + currentMaxQty + " sản phẩm";
                    stockStatus.className = "ms-3 text-warning-emphasis fw-bold";
                }
            } else if (currentMaxQty > 0) {
                this.value = val;
                if (stockStatus) {
                    stockStatus.textContent = "Còn " + currentMaxQty + " sản phẩm";
                    stockStatus.className = "ms-3 text-success fw-bold";
                }
            }
        });

        qtyInput.addEventListener('blur', function() {
            let val = parseInt(this.value, 10);
            if (isNaN(val) || val < 1) {
                this.value = currentMaxQty > 0 ? 1 : 0;
            } else if (currentMaxQty > 0 && val > currentMaxQty) {
                this.value = currentMaxQty;
            }
        });
    }

    function submitForm(isBuyNow) {
        if (!selectedVariantId || !selectedVariantId.value) {
            if (sizeError) sizeError.classList.remove('d-none');
            return;
        }
        if (currentMaxQty <= 0) {
            alert('Kích cỡ này đã hết hàng, vui lòng chọn kích cỡ khác.');
            return;
        }

        const form = document.getElementById('addToCartForm');
        if (isBuyNow) {
            const input = document.createElement('input');
            input.type = 'hidden';
            input.name = 'buyNow';
            input.value = 'true';
            form.appendChild(input);
        }
        form.setAttribute("aria-busy", "true");
        form.querySelectorAll("button").forEach(b => b.disabled = true);
        form.submit();
    }

    document.getElementById('btn-add-cart')?.addEventListener('click', () => submitForm(false));
    document.getElementById('btn-buy-now')?.addEventListener('click', () => submitForm(true));
    document.getElementById('btn-mobile-buy')?.addEventListener('click', () => submitForm(false));
});
