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

    // Khôi phục text ban đầu cho các options trong combobox
    function restoreOptionTexts() {
        if (!selectSize) return;
        for (let i = 1; i < selectSize.options.length; i++) {
            const opt = selectSize.options[i];
            const sizeVal = opt.dataset.sizeValue || opt.text.split('(')[0].trim();
            opt.dataset.sizeValue = sizeVal;
            const originalQty = parseInt(opt.dataset.qty, 10) || 0;
            opt.text = sizeVal + (originalQty <= 0 ? " (Tạm hết hàng)" : " (Còn " + originalQty + " sản phẩm)");
        }
    }

    // Cập nhật số lượng còn lại khi người dùng bấm + hoặc -
    function updateStockDisplay() {
        if (!stockStatus) return;
        if (!selectedVariantId || !selectedVariantId.value || currentMaxQty <= 0) {
            stockStatus.textContent = (currentMaxQty <= 0 && selectedVariantId && selectedVariantId.value)
                ? "Tạm hết hàng (0 sản phẩm)"
                : "Vui lòng chọn kích cỡ để xem số lượng còn lại";
            stockStatus.className = currentMaxQty <= 0 ? "ms-3 text-danger fw-bold" : "ms-3 text-muted";
            return;
        }

        const selectedVal = parseInt(qtyInput ? qtyInput.value : 1, 10) || 1;
        const remaining = Math.max(0, currentMaxQty - selectedVal);

        if (remaining > 0) {
            stockStatus.textContent = "Còn " + remaining + " sản phẩm";
            stockStatus.className = "ms-3 text-success fw-bold";
        } else {
            stockStatus.textContent = "Đã chọn tối đa (còn 0 sản phẩm)";
            stockStatus.className = "ms-3 text-warning-emphasis fw-bold";
        }

        // Đồng bộ cập nhật số lượng còn lại trong combobox size
        if (selectSize && selectSize.selectedIndex > 0) {
            const opt = selectSize.options[selectSize.selectedIndex];
            const sizeVal = opt.dataset.sizeValue || opt.text.split('(')[0].trim();
            opt.dataset.sizeValue = sizeVal;
            opt.text = sizeVal + (remaining <= 0 ? " (Đã chọn hết)" : " (Còn " + remaining + " sản phẩm)");
        }
    }

    function onSizeChange() {
        if (sizeError) sizeError.classList.add('d-none');
        restoreOptionTexts();

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
                qtyInput.max = Math.min(qty, 5);
                qtyInput.value = 1;
            }
            updateStockDisplay();
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
                updateStockDisplay();
            }
        });
    }

    if (btnPlus && qtyInput) {
        btnPlus.addEventListener('click', function() {
            let val = parseInt(qtyInput.value, 10) || 0;
            let effectiveMax = Math.min(currentMaxQty, 5);
            if (currentMaxQty > 0 && val < effectiveMax) {
                qtyInput.value = val + 1;
                updateStockDisplay();
            } else if (currentMaxQty > 0 && val >= effectiveMax) {
                if (stockStatus) {
                    if (effectiveMax === 5 && currentMaxQty > 5) {
                        stockStatus.textContent = "Đơn hàng online giới hạn tối đa 5 sản phẩm";
                    } else {
                        stockStatus.textContent = "Đã chọn tối đa số lượng trong kho (" + currentMaxQty + " sản phẩm)";
                    }
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
            } else if (currentMaxQty > 0 && val < 1) {
                this.value = 1;
            } else {
                this.value = val;
            }
            updateStockDisplay();
        });

        qtyInput.addEventListener('blur', function() {
            let val = parseInt(this.value, 10);
            if (isNaN(val) || val < 1) {
                this.value = currentMaxQty > 0 ? 1 : 0;
            } else if (currentMaxQty > 0 && val > currentMaxQty) {
                this.value = currentMaxQty;
            }
            updateStockDisplay();
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
