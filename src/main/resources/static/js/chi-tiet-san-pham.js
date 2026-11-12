document.addEventListener("DOMContentLoaded", function() {
    document.body.classList.add("has-mobile-actions");
    const selectSize = document.getElementById('select-size');
    const selectColor = document.getElementById('select-color');
    const selectMaterial = document.getElementById('select-material');
    const selectSole = document.getElementById('select-sole');
    const selectedVariantId = document.getElementById('selectedVariantId');

    const qtyInput = document.getElementById('quantity');
    const btnMinus = document.getElementById('btn-minus');
    const btnPlus = document.getElementById('btn-plus');
    const stockStatus = document.getElementById('stock-status');
    const sizeError = document.getElementById('size-error');

    const displayFinalPrice = document.getElementById('displayFinalPrice');
    const displayOriginalPrice = document.getElementById('displayOriginalPrice');
    const displayDiscount = document.getElementById('displayDiscount');

    let currentMaxQty = 0;

    function formatCurrency(value) {
        return new Intl.NumberFormat('vi-VN').format(value) + ' ₫';
    }

    const variants = Array.from(document.querySelectorAll('#variants-data span')).map(el => ({
        id: el.dataset.id,
        qty: parseInt(el.dataset.qty, 10) || 0,
        sizeId: el.dataset.sizeId || '',
        sizeName: el.dataset.sizeName || '',
        colorId: el.dataset.colorId || '',
        colorName: el.dataset.colorName || '',
        materialId: el.dataset.materialId || '',
        materialName: el.dataset.materialName || '',
        soleId: el.dataset.soleId || '',
        soleName: el.dataset.soleName || '',
        finalPrice: parseFloat(el.dataset.finalprice) || 0,
        origPrice: parseFloat(el.dataset.origprice) || 0,
        hasPromo: el.dataset.haspromo === 'true',
        discount: el.dataset.discount || 0
    }));

    function updateVariantSelection() {
        if (sizeError) sizeError.classList.add('d-none');
        const sVal = selectSize ? selectSize.value : '';
        const cVal = selectColor ? selectColor.value : '';
        const mVal = selectMaterial ? selectMaterial.value : '';
        const soVal = selectSole ? selectSole.value : '';

        // Check if any combobox is not yet selected
        if (!sVal || !cVal || !mVal || !soVal) {
            if (selectedVariantId) selectedVariantId.value = '';
            currentMaxQty = 0;
            if (stockStatus) {
                stockStatus.textContent = "Vui lòng chọn kích cỡ, màu sắc, chất liệu và đế giày";
                stockStatus.className = "ms-3 text-muted";
            }
            return;
        }

        // Find match in variants list
        const match = variants.find(v =>
            String(v.sizeId) === String(sVal) &&
            String(v.colorId) === String(cVal) &&
            String(v.materialId) === String(mVal) &&
            String(v.soleId) === String(soVal)
        );

        if (match) {
            if (selectedVariantId) selectedVariantId.value = match.id;
            const qty = match.qty;
            currentMaxQty = qty;
            if (qtyInput) qtyInput.max = qty;

            if (qty > 0) {
                if (qtyInput) qtyInput.value = 1;
                if (stockStatus) {
                    stockStatus.textContent = "Còn " + qty + " sản phẩm";
                    stockStatus.className = "ms-3 text-success fw-semibold";
                }
            } else {
                if (qtyInput) qtyInput.value = 0;
                if (stockStatus) {
                    stockStatus.textContent = "Tạm hết hàng";
                    stockStatus.className = "ms-3 text-danger fw-semibold";
                }
            }

            // Update price
            if (displayFinalPrice) displayFinalPrice.textContent = formatCurrency(match.finalPrice);
            if (match.hasPromo) {
                if (displayOriginalPrice) {
                    displayOriginalPrice.textContent = formatCurrency(match.origPrice);
                    displayOriginalPrice.classList.remove('d-none');
                }
                if (displayDiscount) {
                    displayDiscount.textContent = "GIẢM -" + match.discount + "%";
                    displayDiscount.classList.remove('d-none');
                }
            } else {
                if (displayOriginalPrice) displayOriginalPrice.classList.add('d-none');
                if (displayDiscount) displayDiscount.classList.add('d-none');
            }
        } else {
            if (selectedVariantId) selectedVariantId.value = '';
            currentMaxQty = 0;
            if (qtyInput) qtyInput.value = 0;
            if (stockStatus) {
                stockStatus.textContent = "Không có sẵn kết hợp này";
                stockStatus.className = "ms-3 text-danger fw-semibold";
            }
        }
    }

    [selectSize, selectColor, selectMaterial, selectSole].forEach(sel => {
        if (sel) {
            sel.addEventListener('change', updateVariantSelection);
        }
    });

    // Auto-select first in-stock variant or first variant
    if (variants.length > 0) {
        const defaultVar = variants.find(v => v.qty > 0) || variants[0];
        if (selectSize && defaultVar.sizeId) selectSize.value = defaultVar.sizeId;
        if (selectColor && defaultVar.colorId) selectColor.value = defaultVar.colorId;
        if (selectMaterial && defaultVar.materialId) selectMaterial.value = defaultVar.materialId;
        if (selectSole && defaultVar.soleId) selectSole.value = defaultVar.soleId;
        updateVariantSelection();
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
                    stockStatus.className = "ms-3 text-success";
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
            alert('Thuộc tính biến thể này đã hết hàng, vui lòng chọn phân loại khác.');
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
