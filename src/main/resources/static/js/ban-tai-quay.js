(() => {
    'use strict';

    // -------------------------------------------------------------
    // 1. UTILS & CSRF
    // -------------------------------------------------------------
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content || '';
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';

    function showToast(message) {
        const toast = document.querySelector('#pos-toast');
        const text = document.querySelector('#pos-toast-message');
        if (!toast || !text) return;
        text.textContent = message;
        toast.style.display = 'flex';
        toast.style.opacity = '1';
        setTimeout(() => {
            toast.style.opacity = '0';
            setTimeout(() => { toast.style.display = 'none'; }, 300);
        }, 2500);
    }

    function formatVND(num) {
        return new Intl.NumberFormat('vi-VN').format(Math.max(0, Math.round(Number(num) || 0))) + ' đ';
    }

    function parseVND(str) {
        if (!str) return 0;
        let s = String(str).trim();
        if (s.endsWith('.00')) {
            s = s.substring(0, s.length - 3);
        } else if (s.endsWith(',00')) {
            s = s.substring(0, s.length - 3);
        }
        return Number(s.replace(/[^\d]/g, '')) || 0;
    }

    // -------------------------------------------------------------
    // 2. HÓA ĐƠN CHỜ (HELD ORDERS)
    // -------------------------------------------------------------
    const heldBar = document.querySelector('#pos-held-bar');
    const newTabForm = document.querySelector('#pos-new-tab-form');
    const btnAddHoldTab = document.querySelector('#btn-add-hold-tab');

    if (btnAddHoldTab && newTabForm) {
        btnAddHoldTab.addEventListener('click', () => {
            const customerIdInput = document.querySelector('#customerId');
            const voucherInput = document.querySelector('#posVoucher');
            const noteInput = document.querySelector('#posNote');
            const methodInput = document.querySelector('input[name="paymentMethod"]:checked');
            const deliverySwitch = document.querySelector('#deliverySwitch');

            document.querySelector('#new-tab-customer-id').value = customerIdInput?.value || '';
            document.querySelector('#new-tab-voucher-code').value = voucherInput?.value || '';
            document.querySelector('#new-tab-payment-method').value = methodInput?.value || (deliverySwitch?.checked ? 'COD' : 'CASH');
            document.querySelector('#new-tab-note').value = noteInput?.value || '';
            document.querySelector('#new-tab-delivery').value = deliverySwitch?.checked ? 'true' : 'false';
            document.querySelector('#new-tab-receiver-name').value = document.querySelector('#receiverName')?.value || '';
            document.querySelector('#new-tab-receiver-phone').value = document.querySelector('#receiverPhone')?.value || '';
            document.querySelector('#new-tab-receiver-email').value = document.querySelector('#receiverEmail')?.value || '';
            document.querySelector('#new-tab-province-id').value = document.querySelector('#pos-del-province')?.value || '';
            document.querySelector('#new-tab-ward-id').value = document.querySelector('#pos-del-ward')?.value || '';
            document.querySelector('#new-tab-street-address').value = document.querySelector('#pos-street')?.value || '';
            document.querySelector('#new-tab-latitude').value = document.querySelector('#pos-del-latitude')?.value || '';
            document.querySelector('#new-tab-longitude').value = document.querySelector('#pos-del-longitude')?.value || '';
            document.querySelector('#new-tab-coordinate-source').value = document.querySelector('#pos-del-source')?.value || '';
            document.querySelector('#new-tab-confirm-now').value = document.querySelector('#confirmNow')?.checked ? 'true' : 'false';

            newTabForm.submit();
        });
    }

    // Tab switching
    document.querySelectorAll('[data-open-hold]').forEach(el => {
        el.addEventListener('click', () => {
            const holdId = el.dataset.holdId;
            if (!holdId || el.closest('.pos-tab-item')?.classList.contains('is-active')) return;

            const deliveryChecked = document.querySelector('#deliverySwitch')?.checked;
            const form = document.createElement('form');
            form.method = 'POST';
            form.action = `/admin/pos/holds/${holdId}/open`;
            form.innerHTML = `
                <input type="hidden" name="${csrfHeader === 'X-CSRF-TOKEN' ? '_csrf' : csrfHeader}" value="${csrfToken}">
                <input type="hidden" name="action" value="SAVE">
                <input type="hidden" name="customerId" value="${document.querySelector('#customerId')?.value || ''}">
                <input type="hidden" name="voucherCode" value="${document.querySelector('#posVoucher')?.value || ''}">
                <input type="hidden" name="paymentMethod" value="${document.querySelector('input[name="paymentMethod"]:checked')?.value || (deliveryChecked ? 'COD' : 'CASH')}">
                <input type="hidden" name="note" value="${document.querySelector('#posNote')?.value || ''}">
                <input type="hidden" name="delivery" value="${deliveryChecked ? 'true' : 'false'}">
                <input type="hidden" name="receiverName" value="${document.querySelector('#receiverName')?.value || ''}">
                <input type="hidden" name="receiverPhone" value="${document.querySelector('#receiverPhone')?.value || ''}">
                <input type="hidden" name="receiverEmail" value="${document.querySelector('#receiverEmail')?.value || ''}">
                <input type="hidden" name="provinceId" value="${document.querySelector('#pos-del-province')?.value || ''}">
                <input type="hidden" name="wardId" value="${document.querySelector('#pos-del-ward')?.value || ''}">
                <input type="hidden" name="streetAddress" value="${document.querySelector('#pos-street')?.value || ''}">
                <input type="hidden" name="latitude" value="${document.querySelector('#pos-del-latitude')?.value || ''}">
                <input type="hidden" name="longitude" value="${document.querySelector('#pos-del-longitude')?.value || ''}">
                <input type="hidden" name="coordinateSource" value="${document.querySelector('#pos-del-source')?.value || ''}">
                <input type="hidden" name="confirmNow" value="${document.querySelector('#confirmNow')?.checked ? 'true' : 'false'}">
            `;
            document.body.appendChild(form);
            form.submit();
        });
    });

    // Chuyển về Hóa đơn hiện tại khi click tab Hóa đơn hiện tại
    const tabCurrentOrder = document.querySelector('#tab-current-order');
    if (tabCurrentOrder) {
        tabCurrentOrder.addEventListener('click', () => {
            if (tabCurrentOrder.classList.contains('is-active')) return;
            const deliveryChecked = document.querySelector('#deliverySwitch')?.checked;
            const form = document.createElement('form');
            form.method = 'POST';
            form.action = '/admin/pos/holds/close';
            form.innerHTML = `
                <input type="hidden" name="${csrfHeader === 'X-CSRF-TOKEN' ? '_csrf' : csrfHeader}" value="${csrfToken}">
                <input type="hidden" name="customerId" value="${document.querySelector('#customerId')?.value || ''}">
                <input type="hidden" name="voucherCode" value="${document.querySelector('#posVoucher')?.value || ''}">
                <input type="hidden" name="paymentMethod" value="${document.querySelector('input[name="paymentMethod"]:checked')?.value || (deliveryChecked ? 'COD' : 'CASH')}">
                <input type="hidden" name="note" value="${document.querySelector('#posNote')?.value || ''}">
                <input type="hidden" name="delivery" value="${deliveryChecked ? 'true' : 'false'}">
                <input type="hidden" name="receiverName" value="${document.querySelector('#receiverName')?.value || ''}">
                <input type="hidden" name="receiverPhone" value="${document.querySelector('#receiverPhone')?.value || ''}">
                <input type="hidden" name="receiverEmail" value="${document.querySelector('#receiverEmail')?.value || ''}">
                <input type="hidden" name="provinceId" value="${document.querySelector('#pos-del-province')?.value || ''}">
                <input type="hidden" name="wardId" value="${document.querySelector('#pos-del-ward')?.value || ''}">
                <input type="hidden" name="streetAddress" value="${document.querySelector('#pos-street')?.value || ''}">
                <input type="hidden" name="latitude" value="${document.querySelector('#pos-del-latitude')?.value || ''}">
                <input type="hidden" name="longitude" value="${document.querySelector('#pos-del-longitude')?.value || ''}">
                <input type="hidden" name="coordinateSource" value="${document.querySelector('#pos-del-source')?.value || ''}">
                <input type="hidden" name="confirmNow" value="${document.querySelector('#confirmNow')?.checked ? 'true' : 'false'}">
            `;
            document.body.appendChild(form);
            form.submit();
        });
    }

    // Countdown timers for held orders
    function updateCountdowns() {
        document.querySelectorAll('[data-hold-countdown]').forEach(el => {
            const expires = Number(el.dataset.expires);
            if (!expires) return;
            const diff = expires - Date.now();
            if (diff <= 0) {
                el.textContent = 'Đã hết hạn';
                el.classList.add('text-danger');
            } else {
                const totalMins = Math.floor(diff / 60000);
                const hrs = Math.floor(totalMins / 60);
                const mins = totalMins % 60;
                el.textContent = 'Còn ' + (hrs > 0 ? hrs + 'h ' : '') + mins + 'p';
            }
        });
    }
    updateCountdowns();
    setInterval(updateCountdowns, 60000);

    // -------------------------------------------------------------
    // 3. GIỎ HÀNG (CART OPERATIONS)
    // -------------------------------------------------------------
    // 3. GIỎ HÀNG (CART OPERATIONS)
    // -------------------------------------------------------------
    function updateCartTable(cartItems) {
        const tbody = document.querySelector('.pos-cart-table tbody');
        if (!tbody) return;
        if (!cartItems || cartItems.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="9" class="text-center py-5 text-muted pos-empty-cart">
                        Giỏ hàng trống.
                    </td>
                </tr>
            `;
            return;
        }

        tbody.innerHTML = cartItems.map((item, idx) => `
            <tr class="pos-line-item" data-variant-id="${item.variantId}">
                <td class="row-number text-center">${idx + 1}</td>
                <td>${item.productCode || '—'}</td>
                <td class="col-pos-name">
                    <strong>${item.productName || ''}</strong>
                </td>
                <td>${item.colorName || '—'}</td>
                <td class="text-center">
                    <span class="badge bg-light text-dark border px-2 py-1">${item.size || ''}</span>
                </td>
                <td>
                    <input type="number" min="1" max="${item.maxQuantity || 99}"
                           class="form-control form-control-sm pos-qty-input"
                           value="${item.quantity}" data-variant-id="${item.variantId}"
                           data-max="${item.maxQuantity || 99}"
                           aria-label="Số lượng sản phẩm">
                </td>
                <td class="text-end">
                    ${item.hasPromotion ? `
                        <span class="text-danger fw-bold d-block">${formatVND(item.price)}</span>
                        <del class="small text-muted d-block">${formatVND(item.originalPrice)}</del>
                        <span class="badge bg-danger-subtle text-danger border border-danger-subtle small d-inline-block mt-1">${item.promotionName || 'Đợt giảm giá'}</span>
                    ` : `
                        <span class="fw-semibold text-dark">${formatVND(item.price)}</span>
                    `}
                </td>
                <td class="text-end fw-bold">${formatVND(item.totalPrice)}</td>
                <td class="text-center">
                    <button type="button" class="btn btn-sm btn-outline-danger pos-btn-remove p-1"
                            data-variant-id="${item.variantId}" title="Xóa khỏi giỏ">
                        <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" fill="currentColor" viewBox="0 0 16 16">
                            <path d="M5.5 5.5A.5.5 0 0 1 6 6v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5m2.5 0a.5.5 0 0 1 .5.5v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5m3 .5a.5.5 0 0 0-1 0v6a.5.5 0 0 0 1 0z"/>
                            <path d="M14.5 3a1 1 0 0 1-1 1H13v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V4h-.5a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1H6a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1h3.5a1 1 0 0 1 1 1zM4.118 4 4 4.059V13a1 1 0 0 0 1 1h6a1 1 0 0 0 1-1V4.059L11.882 4zM2.5 3h11V2h-11z"/>
                        </svg>
                    </button>
                </td>
            </tr>
        `).join('');

        attachCartRowEvents();
    }

    function updateModalRow(variantId, availableQty) {
        if (!productsModal) return;
        const row = modalProductsContainer?.querySelector(`.pos-modal-row[data-variant-id="${variantId}"]`);
        if (!row) return;

        const stockBadge = row.querySelector('.pos-modal-stock-badge') || row.querySelector('td:nth-child(10) span');
        if (stockBadge) {
            stockBadge.textContent = availableQty;
            stockBadge.className = 'pos-modal-stock-badge badge ' + (
                availableQty <= 0 ? 'bg-secondary' :
                availableQty <= 5 ? 'bg-warning text-dark' : 'bg-light text-dark border'
            );
        }

        const actionCell = row.querySelector('td:last-child');
        if (actionCell) {
            if (availableQty <= 0) {
                actionCell.innerHTML = `
                    <button type="button" class="btn btn-sm btn-secondary pos-btn-add-variant px-1"
                            data-variant-id="${variantId}" disabled title="Hết hàng" aria-label="Hết hàng">
                        Hết hàng
                    </button>
                `;
            } else {
                const variantName = row.querySelector('td:nth-child(4) span')?.textContent || 'Sản phẩm';
                const sizeVal = row.querySelector('td:nth-child(9) span')?.textContent || '';
                const colorVal = row.querySelector('td:nth-child(6) span:last-child')?.textContent || '';
                const fullName = `${variantName} (${sizeVal} - ${colorVal})`;
                actionCell.innerHTML = `
                    <button type="button" class="btn btn-sm btn-primary pos-btn-add-variant px-2 fw-bold"
                            data-variant-id="${variantId}"
                            data-variant-name="${fullName}"
                            title="Thêm vào giỏ" aria-label="Thêm vào giỏ">
                        +
                    </button>
                `;
                const newBtn = actionCell.querySelector('.pos-btn-add-variant');
                if (newBtn) bindAddButton(newBtn);
            }
        }
    }

    function attachCartRowEvents() {
        // Gõ tay số lượng
        document.querySelectorAll('.pos-qty-input').forEach(input => {
            const updateQty = async () => {
                const variantId = input.dataset.variantId;
                let qty = parseInt(input.value) || 1;
                const maxQty = parseInt(input.dataset.max) || parseInt(input.getAttribute('max')) || 99;
                if (qty > maxQty) {
                    qty = maxQty;
                    input.value = qty;
                    alert('Số lượng vượt quá tồn kho hiện tại (' + maxQty + ').');
                }
                if (qty < 1) {
                    qty = 1;
                    input.value = 1;
                }

                const data = new URLSearchParams();
                data.append('variantId', variantId);
                data.append('quantity', qty);
                data.append('_csrf', csrfToken);

                try {
                    const resp = await fetch('/admin/pos/quantity', {
                        method: 'POST',
                        headers: { 'X-Requested-With': 'XMLHttpRequest' },
                        body: data
                    });
                    const res = await resp.json();
                    if (!resp.ok || !res.success) {
                        alert(res.message || 'Lỗi cập nhật số lượng.');
                        window.location.reload();
                        return;
                    }

                    const row = input.closest('tr');
                    const lineTotalCell = row.querySelector('td:nth-last-child(2)');
                    if (lineTotalCell && res.itemTotal != null) {
                        lineTotalCell.textContent = formatVND(res.itemTotal);
                    }

                    const originalEl = document.querySelector('#pos-subtotal-original');
                    if (originalEl && res.originalTotal != null) {
                        originalEl.textContent = formatVND(res.originalTotal);
                        originalEl.dataset.subtotalOriginal = res.originalTotal;
                    }

                    const promoRow = document.querySelector('#pos-promo-discount-row');
                    const promoDisplay = document.querySelector('#pos-promo-discount-display');
                    if (promoRow && promoDisplay) {
                        if (res.promoDiscount > 0) {
                            promoDisplay.textContent = '-' + formatVND(res.promoDiscount);
                            promoRow.hidden = false;
                        } else {
                            promoRow.hidden = true;
                        }
                    }

                    const subtotalEl = document.querySelector('[data-subtotal]');
                    if (subtotalEl && res.total != null) {
                        subtotalEl.dataset.subtotal = res.total;
                        subtotalEl.textContent = res.total;
                    }

                    const badge = document.querySelector('#pos-cart-badge');
                    if (badge && res.totalItems != null) badge.textContent = `${res.totalItems} sản phẩm`;

                    updateModalRow(variantId, res.availableQuantity);
                    recalculateTotals();
                } catch (e) {
                    window.location.reload();
                }
            };

            input.addEventListener('change', updateQty);
            input.addEventListener('keydown', (e) => {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    input.blur();
                }
            });
        });

        // Xóa dòng giỏ hàng
        document.querySelectorAll('.pos-btn-remove').forEach(btn => {
            btn.addEventListener('click', async () => {
                const variantId = btn.dataset.variantId;
                const data = new URLSearchParams();
                data.append('variantId', variantId);
                data.append('_csrf', csrfToken);

                try {
                    const resp = await fetch('/admin/pos/remove', {
                        method: 'POST',
                        headers: { 'X-Requested-With': 'XMLHttpRequest' },
                        body: data
                    });
                    const res = await resp.json();
                    if (!resp.ok || !res.success) {
                        window.location.reload();
                        return;
                    }

                    const row = btn.closest('tr');
                    row.remove();

                    const tbody = document.querySelector('.pos-cart-table tbody');
                    if (!tbody.querySelector('.pos-line-item')) {
                        tbody.innerHTML = `
                            <tr>
                                <td colspan="9" class="text-center py-5 text-muted pos-empty-cart">
                                    Giỏ hàng trống.
                                </td>
                            </tr>
                        `;
                    } else {
                        tbody.querySelectorAll('.pos-line-item .row-number').forEach((numEl, i) => {
                            numEl.textContent = i + 1;
                        });
                    }

                    const originalEl = document.querySelector('#pos-subtotal-original');
                    if (originalEl && res.originalTotal != null) {
                        originalEl.textContent = formatVND(res.originalTotal);
                        originalEl.dataset.subtotalOriginal = res.originalTotal;
                    }

                    const promoRow = document.querySelector('#pos-promo-discount-row');
                    const promoDisplay = document.querySelector('#pos-promo-discount-display');
                    if (promoRow && promoDisplay) {
                        if (res.promoDiscount > 0) {
                            promoDisplay.textContent = '-' + formatVND(res.promoDiscount);
                            promoRow.hidden = false;
                        } else {
                            promoRow.hidden = true;
                        }
                    }

                    const subtotalEl = document.querySelector('[data-subtotal]');
                    if (subtotalEl && res.total != null) {
                        subtotalEl.dataset.subtotal = res.total;
                        subtotalEl.textContent = res.total;
                    }

                    const badge = document.querySelector('#pos-cart-badge');
                    if (badge && res.totalItems != null) badge.textContent = `${res.totalItems} sản phẩm`;

                    updateModalRow(variantId, res.availableQuantity);
                    recalculateTotals();
                } catch (e) {
                    window.location.reload();
                }
            });
        });
    }

    // Gắn sự kiện ban đầu cho bảng giỏ hàng
    attachCartRowEvents();

    // Hủy hóa đơn
    const btnCancelCart = document.querySelector('#btn-cancel-cart');
    if (btnCancelCart) {
        btnCancelCart.addEventListener('click', () => {
            if (confirm('Bạn có chắc chắn muốn hủy giỏ hàng của hóa đơn hiện tại?')) {
                const form = document.createElement('form');
                form.method = 'POST';
                form.action = '/admin/pos/clear';
                form.innerHTML = `<input type="hidden" name="_csrf" value="${csrfToken}">`;
                document.body.appendChild(form);
                form.submit();
            }
        });
    }

    // -------------------------------------------------------------
    // 4. HỘP THOẠI "DANH SÁCH SẢN PHẨM"
    // -------------------------------------------------------------
    const productsModal = document.querySelector('#pos-product-modal');
    const btnOpenProductsModal = document.querySelector('#btn-open-products-modal');
    const btnCloseProductsModal = document.querySelector('#btn-close-products-modal');
    const btnModalCloseFooter = document.querySelector('#btn-modal-close-footer');
    const modalProductsContainer = document.querySelector('#pos-modal-products-container');

    async function loadProductsModal(page = 0) {
        if (!modalProductsContainer) return;
        const kw = document.querySelector('#modal-search')?.value.trim() || '';
        const cat = document.querySelector('#modal-category')?.value || '';
        const col = document.querySelector('#modal-color')?.value || '';
        const mat = document.querySelector('#modal-material')?.value || '';
        const sole = document.querySelector('#modal-sole')?.value || '';
        const size = document.querySelector('#modal-size')?.value || '';
        const price = document.querySelector('#modal-price')?.value || '';

        const params = new URLSearchParams({
            keyword: kw,
            page: page
        });
        if (cat) params.append('categoryId', cat);
        if (col) params.append('colorId', col);
        if (mat) params.append('materialId', mat);
        if (sole) params.append('soleId', sole);
        if (size) params.append('sizeId', size);
        if (price) params.append('priceRange', price);

        modalProductsContainer.innerHTML = '<div class="text-center py-5 text-muted">Đang tải sản phẩm…</div>';
        try {
            const resp = await fetch('/admin/pos/products-modal?' + params.toString());
            if (!resp.ok) throw new Error('Không thể tải sản phẩm.');
            const html = await resp.text();
            modalProductsContainer.innerHTML = html;
            attachModalActions();
        } catch (e) {
            modalProductsContainer.innerHTML = `<div class="alert alert-danger my-3">${e.message}</div>`;
        }
    }

    function bindAddButton(btn) {
        btn.addEventListener('click', async () => {
            const variantId = btn.dataset.variantId;
            const variantName = btn.dataset.variantName || 'Sản phẩm';

            const data = new URLSearchParams();
            data.append('variantId', variantId);
            data.append('quantity', 1);
            data.append('_csrf', csrfToken);

            try {
                btn.disabled = true;
                btn.textContent = '...';
                const resp = await fetch('/admin/pos/add', {
                    method: 'POST',
                    headers: { 'X-Requested-With': 'XMLHttpRequest' },
                    body: data
                });
                const res = await resp.json();
                if (res.success) {
                    showToast(`Đã thêm ${variantName} vào giỏ`);
                    // 1. Cập nhật tồn khả dụng dòng sản phẩm ngay lập tức
                    updateModalRow(variantId, res.availableQuantity);

                    // 2. Cập nhật badge giỏ hàng
                    const badge = document.querySelector('#pos-cart-badge');
                    if (badge && res.totalItems != null) badge.textContent = `${res.totalItems} sản phẩm`;

                    // 3. Cập nhật tổng tiền hàng gốc & giảm khuyến mãi
                    const originalEl = document.querySelector('#pos-subtotal-original');
                    if (originalEl && res.originalTotal != null) {
                        originalEl.textContent = formatVND(res.originalTotal);
                        originalEl.dataset.subtotalOriginal = res.originalTotal;
                    }

                    const promoRow = document.querySelector('#pos-promo-discount-row');
                    const promoDisplay = document.querySelector('#pos-promo-discount-display');
                    if (promoRow && promoDisplay) {
                        if (res.promoDiscount > 0) {
                            promoDisplay.textContent = '-' + formatVND(res.promoDiscount);
                            promoRow.hidden = false;
                        } else {
                            promoRow.hidden = true;
                        }
                    }

                    const subtotalEl = document.querySelector('[data-subtotal]');
                    if (subtotalEl && res.total != null) {
                        subtotalEl.dataset.subtotal = res.total;
                        subtotalEl.textContent = res.total;
                    }

                    // 4. Cập nhật bảng giỏ hàng ở nền
                    if (res.cartItems) {
                        updateCartTable(res.cartItems);
                    }

                    // 5. Tính lại tổng thanh toán
                    recalculateTotals();
                } else {
                    alert(res.message || 'Không thể thêm sản phẩm.');
                    btn.disabled = false;
                    btn.textContent = '+';
                }
            } catch (e) {
                alert('Lỗi thêm sản phẩm: ' + e.message);
                btn.disabled = false;
                btn.textContent = '+';
            }
        });
    }

    function attachModalActions() {
        modalProductsContainer.querySelectorAll('.pos-modal-page-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                const page = btn.dataset.page;
                if (page != null) loadProductsModal(parseInt(page));
            });
        });

        modalProductsContainer.querySelectorAll('.pos-btn-add-variant').forEach(btn => {
            if (!btn.disabled) {
                bindAddButton(btn);
            }
        });
    }

    if (btnOpenProductsModal && productsModal) {
        btnOpenProductsModal.addEventListener('click', () => {
            productsModal.showModal();
            loadProductsModal(0);
        });

        const closeModal = () => {
            productsModal.close();
        };

        btnCloseProductsModal?.addEventListener('click', closeModal);
        btnModalCloseFooter?.addEventListener('click', closeModal);

        document.querySelector('#btn-modal-filter')?.addEventListener('click', () => loadProductsModal(0));
        document.querySelector('#btn-modal-reset')?.addEventListener('click', () => {
            document.querySelector('#pos-modal-filter-form')?.reset();
            loadProductsModal(0);
        });
        document.querySelector('#modal-search')?.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                loadProductsModal(0);
            }
        });
    }

    // -------------------------------------------------------------
    // 5. HỘP THOẠI "QUÉT MÃ" (BARCODE & CAMERA)
    // -------------------------------------------------------------
    const scannerModal = document.querySelector('#pos-scanner-modal');
    const btnOpenScanner = document.querySelector('#btn-open-scanner');
    const btnCloseScanner = document.querySelector('#btn-close-scanner-modal');
    const btnCloseScannerFooter = document.querySelector('#btn-close-scanner-footer');
    const scannerManualInput = document.querySelector('#scanner-manual-code');
    const btnSubmitScan = document.querySelector('#btn-submit-scan');
    const scannerFeedback = document.querySelector('#scanner-feedback');
    const scannerVideo = document.querySelector('#scanner-video');
    const scannerCanvas = document.querySelector('#scanner-canvas');
    const btnToggleCamera = document.querySelector('#btn-toggle-camera');
    const cameraPlaceholder = document.querySelector('#camera-placeholder');

    let videoStream = null;
    let cameraActive = false;
    let scanThrottle = false;
    let scannerItemsAdded = false;

    async function handleBarcode(code) {
        if (!code || !code.trim()) return;
        scannerFeedback.className = 'alert alert-info small py-2';
        scannerFeedback.textContent = 'Đang tra cứu mã: ' + code + '…';

        const data = new URLSearchParams();
        data.append('barcode', code.trim());
        data.append('_csrf', csrfToken);

        try {
            const resp = await fetch('/admin/pos/scan', {
                method: 'POST',
                headers: { 'X-Requested-With': 'XMLHttpRequest' },
                body: data
            });
            const res = await resp.json();
            if (res.success) {
                scannerItemsAdded = true;
                scannerFeedback.className = 'alert alert-success small py-2';
                scannerFeedback.textContent = res.message;
                showToast(res.message);
                if (scannerManualInput) scannerManualInput.value = '';
            } else {
                scannerFeedback.className = 'alert alert-danger small py-2';
                scannerFeedback.textContent = res.message;
            }
        } catch (e) {
            scannerFeedback.className = 'alert alert-danger small py-2';
            scannerFeedback.textContent = 'Lỗi hệ thống khi quét mã: ' + e.message;
        }
    }

    if (btnSubmitScan && scannerManualInput) {
        btnSubmitScan.addEventListener('click', () => handleBarcode(scannerManualInput.value));
        scannerManualInput.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                handleBarcode(scannerManualInput.value);
            }
        });
    }

    async function startCamera() {
        try {
            videoStream = await navigator.mediaDevices.getUserMedia({
                video: { facingMode: 'environment' }
            });
            scannerVideo.srcObject = videoStream;
            scannerVideo.hidden = false;
            cameraPlaceholder.hidden = true;
            await scannerVideo.play();
            cameraActive = true;
            requestAnimationFrame(scanVideoFrame);
        } catch (err) {
            scannerFeedback.className = 'alert alert-warning small py-2';
            scannerFeedback.textContent = 'Không thể truy cập camera: ' + err.message;
        }
    }

    function stopCamera() {
        if (videoStream) {
            videoStream.getTracks().forEach(t => t.stop());
            videoStream = null;
        }
        cameraActive = false;
        if (scannerVideo) scannerVideo.hidden = true;
        if (cameraPlaceholder) cameraPlaceholder.hidden = false;
    }

    function scanVideoFrame() {
        if (!cameraActive || !scannerVideo || scannerVideo.readyState !== scannerVideo.HAVE_ENOUGH_DATA) {
            if (cameraActive) requestAnimationFrame(scanVideoFrame);
            return;
        }
        const ctx = scannerCanvas.getContext('2d', { willReadFrequently: true });
        scannerCanvas.width = scannerVideo.videoWidth;
        scannerCanvas.height = scannerVideo.videoHeight;
        ctx.drawImage(scannerVideo, 0, 0, scannerCanvas.width, scannerCanvas.height);
        const imageData = ctx.getImageData(0, 0, scannerCanvas.width, scannerCanvas.height);

        if (window.jsQR && !scanThrottle) {
            const qr = window.jsQR(imageData.data, imageData.width, imageData.height, {
                inversionAttempts: 'dontInvert'
            });
            if (qr && qr.data) {
                scanThrottle = true;
                handleBarcode(qr.data);
                setTimeout(() => { scanThrottle = false; }, 2000);
            }
        }
        if (cameraActive) requestAnimationFrame(scanVideoFrame);
    }

    if (btnToggleCamera) {
        btnToggleCamera.addEventListener('click', () => {
            if (cameraActive) stopCamera();
            else startCamera();
        });
    }

    if (btnOpenScanner && scannerModal) {
        btnOpenScanner.addEventListener('click', () => {
            scannerItemsAdded = false;
            scannerModal.showModal();
            setTimeout(() => scannerManualInput?.focus(), 100);
        });

        const closeScanner = () => {
            stopCamera();
            scannerModal.close();
            if (scannerItemsAdded) {
                window.location.reload();
            }
        };

        btnCloseScanner?.addEventListener('click', closeScanner);
        btnCloseScannerFooter?.addEventListener('click', closeScanner);
    }

    // -------------------------------------------------------------
    // 6. THẺ KHÁCH HÀNG (CUSTOMER SEARCH & QUICK CREATE)
    // -------------------------------------------------------------
    const customerSearch = document.querySelector('#customerSearch');
    const customerDropdown = document.querySelector('#pos-customer-dropdown');
    const customerIdInput = document.querySelector('#customerId');
    const guestDisplay = document.querySelector('#pos-guest-display');
    const customerDisplay = document.querySelector('#pos-customer-display');
    const btnClearCustomer = document.querySelector('#btn-clear-customer');
    const customerModeLabel = document.querySelector('#customer-mode-label');

    let searchDebounce = null;

    if (customerSearch && customerDropdown) {
        customerSearch.addEventListener('input', () => {
            clearTimeout(searchDebounce);
            const q = customerSearch.value.trim();
            if (q.length < 2) {
                customerDropdown.innerHTML = '';
                customerDropdown.hidden = true;
                return;
            }
            searchDebounce = setTimeout(async () => {
                try {
                    const resp = await fetch('/admin/pos/customers-json?q=' + encodeURIComponent(q));
                    const list = await resp.json();
                    if (!list || list.length === 0) {
                        customerDropdown.innerHTML = '<div class="p-2 text-muted small">Không tìm thấy khách hàng.</div>';
                    } else {
                        customerDropdown.innerHTML = list.map(c => `
                            <div class="pos-customer-item" data-id="${c.id}" data-name="${c.full_name || c.fullName}" data-phone="${c.phone || ''}" data-code="${c.customer_code || c.customerCode || ''}">
                                <strong>${c.full_name || c.fullName}</strong>
                                <span class="small text-muted d-block">${c.phone || ''} · ${c.customer_code || c.customerCode || ''}</span>
                            </div>
                        `).join('');

                        customerDropdown.querySelectorAll('.pos-customer-item').forEach(item => {
                            item.addEventListener('click', () => {
                                selectCustomer(item.dataset.id, item.dataset.name, item.dataset.phone, item.dataset.code);
                            });
                        });
                    }
                    customerDropdown.hidden = false;
                } catch (e) {
                    customerDropdown.innerHTML = '<div class="p-2 text-danger small">Lỗi tìm kiếm.</div>';
                    customerDropdown.hidden = false;
                }
            }, 250);
        });

        document.addEventListener('click', (e) => {
            if (!customerSearch.contains(e.target) && !customerDropdown.contains(e.target)) {
                customerDropdown.hidden = true;
            }
        });
    }

    function selectCustomer(id, name, phone, code) {
        if (customerIdInput) customerIdInput.value = id;
        document.querySelector('#display-customer-name').textContent = name;
        document.querySelector('#display-customer-phone').textContent = phone;
        document.querySelector('#display-customer-code').textContent = code;

        if (guestDisplay) guestDisplay.hidden = true;
        if (customerDisplay) customerDisplay.hidden = false;
        if (customerDropdown) customerDropdown.hidden = true;
        if (customerSearch) customerSearch.value = '';
        if (customerModeLabel) customerModeLabel.textContent = 'Đã chọn';

        if (typeof loadCustomerAddresses === 'function') {
            loadCustomerAddresses(id);
        }
        recalculateTotals();
    }

    if (btnClearCustomer) {
        btnClearCustomer.addEventListener('click', () => {
            if (customerIdInput) customerIdInput.value = '';
            if (guestDisplay) guestDisplay.hidden = false;
            if (customerDisplay) customerDisplay.hidden = true;
            if (customerModeLabel) customerModeLabel.textContent = 'Khách lẻ';
            if (customerSearch) customerSearch.value = '';
            if (typeof loadCustomerAddresses === 'function') {
                loadCustomerAddresses(null);
            }
            recalculateTotals();
        });
    }

    // Modal Thêm khách hàng nhanh
    const quickCustomerModal = document.querySelector('#pos-quick-customer-modal');
    const btnOpenQuickCustomer = document.querySelector('#btn-open-quick-customer');
    const btnCloseQuickCustomer = document.querySelector('#btn-close-quick-customer-modal');
    const btnCancelQuickCustomer = document.querySelector('#btn-cancel-quick-customer');
    const btnSubmitQuickCustomer = document.querySelector('#btn-submit-quick-customer');
    const quickCustomerAlert = document.querySelector('#quick-customer-alert');

    if (btnOpenQuickCustomer && quickCustomerModal) {
        btnOpenQuickCustomer.addEventListener('click', () => {
            document.querySelector('#pos-quick-customer-form')?.reset();
            if (quickCustomerAlert) quickCustomerAlert.hidden = true;
            quickCustomerModal.showModal();
        });
        const closeQuickModal = () => quickCustomerModal.close();
        btnCloseQuickCustomer?.addEventListener('click', closeQuickModal);
        btnCancelQuickCustomer?.addEventListener('click', closeQuickModal);

        btnSubmitQuickCustomer?.addEventListener('click', async () => {
            const name = document.querySelector('#quick-customer-name')?.value.trim();
            const phone = document.querySelector('#quick-customer-phone')?.value.trim();
            const email = document.querySelector('#quick-customer-email')?.value.trim();

            if (!name || !phone) {
                if (quickCustomerAlert) {
                    quickCustomerAlert.textContent = 'Vui lòng nhập Họ tên và Số điện thoại.';
                    quickCustomerAlert.hidden = false;
                }
                return;
            }

            const data = new URLSearchParams();
            data.append('fullName', name);
            data.append('phone', phone);
            if (email) data.append('email', email);
            data.append('_csrf', csrfToken);

            try {
                btnSubmitQuickCustomer.disabled = true;
                const resp = await fetch('/admin/pos/customers/quick-create', {
                    method: 'POST',
                    headers: { 'X-Requested-With': 'XMLHttpRequest' },
                    body: data
                });
                const res = await resp.json();
                if (!resp.ok) {
                    throw new Error(res.message || 'Lỗi tạo khách hàng.');
                }
                selectCustomer(res.id, res.fullName, res.phone, res.customerCode);
                quickCustomerModal.close();
                showToast(res.message || 'Đã thêm khách hàng thành công.');
            } catch (err) {
                if (quickCustomerAlert) {
                    quickCustomerAlert.textContent = err.message;
                    quickCustomerAlert.hidden = false;
                }
            } finally {
                btnSubmitQuickCustomer.disabled = false;
            }
        });
    }

    // -------------------------------------------------------------
    // 7. THẺ THÔNG TIN ĐƠN HÀNG & THANH TOÁN
    // -------------------------------------------------------------
    const subtotalEl = document.querySelector('[data-subtotal]');
    const finalTotalEl = document.querySelector('#pos-final-total');
    const discountRow = document.querySelector('#pos-discount-row');
    const discountDisplay = document.querySelector('#pos-discount-display');
    const voucherInput = document.querySelector('#posVoucher');
    const btnApplyVoucher = document.querySelector('#btn-apply-voucher');
    const voucherMessage = document.querySelector('#pos-voucher-message');

    const panelCash = document.querySelector('#panel-cash');
    const panelTransfer = document.querySelector('#panel-transfer');
    const panelCombined = document.querySelector('#panel-combined');
    const panelVnpay = document.querySelector('#panel-vnpay');

    const cashInput = document.querySelector('#cashReceived');
    const cashWarning = document.querySelector('#cash-warning');
    const cashChange = document.querySelector('#cash-change');

    const combinedCashInput = document.querySelector('#combinedCash');
    const combinedTransferInput = document.querySelector('#combinedTransfer');
    const combinedWarning = document.querySelector('#combined-warning');
    const combinedQrWrap = document.querySelector('#combined-qr-wrap');

    const transferReceivedSingle = document.querySelector('#transferReceivedSingle');
    const transferReceivedCombined = document.querySelector('#transferReceivedCombined');
    const btnCheckout = document.querySelector('#btn-pos-checkout');

    let currentDiscount = 0;

    function getSubtotal() {
        return parseVND(subtotalEl?.dataset.subtotal || '0');
    }

    function getFinalTotal() {
        return Math.max(0, getSubtotal() - currentDiscount);
    }

    function recalculateTotals() {
        const finalAmount = getFinalTotal();
        if (finalTotalEl) {
            finalTotalEl.textContent = formatVND(finalAmount);
            finalTotalEl.dataset.amount = finalAmount;
        }

        const subtotal = getSubtotal();
        if (btnCheckout) {
            btnCheckout.disabled = (subtotal <= 0);
        }
        if (btnApplyVoucher) {
            btnApplyVoucher.disabled = (subtotal <= 0);
        }
        const tabCurrentOrder = document.querySelector('#tab-current-order .pos-tab-info .small');
        if (tabCurrentOrder) {
            const lineCount = document.querySelectorAll('.pos-cart-table tbody .pos-line-item').length;
            tabCurrentOrder.textContent = `${lineCount} món · ${formatVND(subtotal)}`;
        }

        const method = document.querySelector('input[name="paymentMethod"]:checked')?.value || 'CASH';
        if (method === 'CASH') {
            updateCashCalculations();
        } else if (method === 'TRANSFER') {
            refreshTransferQr();
        } else if (method === 'COMBINED') {
            updateCombinedCalculations();
        } else if (method === 'VNPAY') {
            if (btnCheckout) {
                btnCheckout.disabled = (finalAmount <= 0);
            }
        }
    }

    // Voucher handling
    if (btnApplyVoucher && voucherInput) {
        btnApplyVoucher.addEventListener('click', async () => {
            const code = voucherInput.value.trim();
            if (!code) {
                currentDiscount = 0;
                if (discountRow) discountRow.hidden = true;
                const voucherLabel = document.querySelector('#pos-voucher-label');
                if (voucherLabel) voucherLabel.textContent = 'Giảm phiếu giảm giá:';
                if (voucherMessage) voucherMessage.textContent = '';
                recalculateTotals();
                return;
            }

            const data = new URLSearchParams();
            data.append('code', code);
            if (customerIdInput?.value) data.append('customerId', customerIdInput.value);
            data.append('_csrf', csrfToken);

            try {
                const resp = await fetch('/admin/pos/validate-voucher', {
                    method: 'POST',
                    headers: { 'X-Requested-With': 'XMLHttpRequest' },
                    body: data
                });
                const res = await resp.json();
                if (res.valid) {
                    currentDiscount = Number(res.discountAmount) || 0;
                    if (discountDisplay) discountDisplay.textContent = '-' + formatVND(currentDiscount);
                    const voucherLabel = document.querySelector('#pos-voucher-label');
                    if (voucherLabel) voucherLabel.textContent = `Giảm phiếu giảm giá (${code}):`;
                    if (discountRow) discountRow.hidden = false;
                    if (voucherMessage) {
                        voucherMessage.className = 'small mb-1 text-success';
                        voucherMessage.textContent = res.message || 'Đã áp dụng mã giảm giá.';
                    }
                } else {
                    currentDiscount = 0;
                    if (discountRow) discountRow.hidden = true;
                    const voucherLabel = document.querySelector('#pos-voucher-label');
                    if (voucherLabel) voucherLabel.textContent = 'Giảm phiếu giảm giá:';
                    if (voucherMessage) {
                        voucherMessage.className = 'small mb-1 text-danger';
                        voucherMessage.textContent = res.message || 'Mã giảm giá không hợp lệ.';
                    }
                }
            } catch (e) {
                currentDiscount = 0;
                if (discountRow) discountRow.hidden = true;
                const voucherLabel = document.querySelector('#pos-voucher-label');
                if (voucherLabel) voucherLabel.textContent = 'Giảm phiếu giảm giá:';
                if (voucherMessage) {
                    voucherMessage.className = 'small mb-1 text-danger';
                    voucherMessage.textContent = 'Lỗi kiểm tra voucher.';
                }
            }
            recalculateTotals();
        });
    }

    // Cash method logic
    function updateCashCalculations() {
        if (!cashInput) return;
        const finalAmount = getFinalTotal();
        const given = parseVND(cashInput.value);

        if (cashInput.value) {
            cashInput.value = formatVND(given).replace(' đ', '');
        }

        const change = given - finalAmount;
        if (cashChange) cashChange.textContent = formatVND(Math.max(0, change));

        if (finalAmount > 0 && given < finalAmount) {
            if (cashWarning) cashWarning.hidden = false;
            if (btnCheckout) btnCheckout.disabled = true;
        } else {
            if (cashWarning) cashWarning.hidden = true;
            if (btnCheckout) btnCheckout.disabled = (finalAmount <= 0 && getSubtotal() <= 0);
        }
    }

    if (cashInput) {
        cashInput.addEventListener('input', updateCashCalculations);
    }

    // Transfer QR logic
    async function refreshTransferQr() {
        const finalAmount = getFinalTotal();
        const canvas = document.querySelector('#pos-qr-transfer');
        const details = document.querySelector('#pos-transfer-details');
        const msg = document.querySelector('#transfer-qr-message');
        const transferAmountField = document.querySelector('#transferAmount');

        if (transferAmountField) transferAmountField.value = finalAmount;
        if (transferReceivedSingle) transferReceivedSingle.checked = false;
        if (btnCheckout) btnCheckout.disabled = true;

        if (finalAmount <= 0) {
            if (msg) msg.textContent = 'Giỏ hàng trống.';
            return;
        }

        if (msg) msg.textContent = 'Đang tải mã VietQR…';
        const data = new URLSearchParams();
        data.append('paymentMethod', 'TRANSFER');
        data.append('voucherCode', voucherInput?.value || '');
        if (customerIdInput?.value) data.append('customerId', customerIdInput.value);
        data.append('_csrf', csrfToken);

        try {
            const resp = await fetch('/admin/pos/payment-qr', {
                method: 'POST',
                headers: { 'X-Requested-With': 'XMLHttpRequest' },
                body: data
            });
            const qr = await resp.json();
            if (qr.configured && qr.payload && window.drawVietQr && canvas) {
                window.drawVietQr(canvas, qr.payload);
                if (details) {
                    details.hidden = false;
                    details.querySelector('[data-qr-bank]').textContent = qr.bankName || '';
                    details.querySelector('[data-qr-account]').textContent = qr.account || '';
                    details.querySelector('[data-qr-owner]').textContent = qr.accountName || '';
                    details.querySelector('[data-qr-amount]').textContent = formatVND(qr.amount);
                    details.querySelector('[data-qr-reference]').textContent = qr.content || '';
                }
                if (msg) msg.textContent = 'Quét QR và kiểm tra tiền đã vào tài khoản.';
            } else {
                if (msg) msg.textContent = qr.message || 'Chưa cấu hình tài khoản VietQR.';
            }
        } catch (e) {
            if (msg) msg.textContent = 'Lỗi tạo mã QR: ' + e.message;
        }
    }

    if (transferReceivedSingle) {
        transferReceivedSingle.addEventListener('change', () => {
            if (btnCheckout) {
                btnCheckout.disabled = !transferReceivedSingle.checked || getFinalTotal() <= 0;
            }
        });
    }

    // Combined method logic
    let combinedQrTimer = null;
    function updateCombinedCalculations() {
        const finalAmount = getFinalTotal();
        const cashVal = parseVND(combinedCashInput?.value);

        if (combinedCashInput && combinedCashInput.value) {
            combinedCashInput.value = formatVND(cashVal).replace(' đ', '');
        }

        const transferVal = finalAmount - cashVal;
        if (combinedTransferInput) {
            combinedTransferInput.value = formatVND(Math.max(0, transferVal)).replace(' đ', '');
        }

        const transferAmountField = document.querySelector('#transferAmount');
        if (transferAmountField) transferAmountField.value = transferVal;

        if (cashVal <= 0 || transferVal <= 0 || (cashVal + transferVal !== finalAmount)) {
            if (combinedWarning) combinedWarning.hidden = false;
            if (combinedQrWrap) combinedQrWrap.hidden = true;
            if (btnCheckout) btnCheckout.disabled = true;
            return;
        }

        if (combinedWarning) combinedWarning.hidden = true;
        if (combinedQrWrap) combinedQrWrap.hidden = false;

        // Checkbox controls button
        if (btnCheckout) {
            btnCheckout.disabled = !transferReceivedCombined?.checked;
        }

        clearTimeout(combinedQrTimer);
        combinedQrTimer = setTimeout(refreshCombinedQr, 400);
    }

    async function refreshCombinedQr() {
        const cashVal = parseVND(combinedCashInput?.value);
        const transferVal = getFinalTotal() - cashVal;
        const canvas = document.querySelector('#pos-qr-combined');
        const details = document.querySelector('#pos-combined-details');
        const msg = document.querySelector('#combined-qr-message');

        if (transferVal <= 0) return;
        if (msg) msg.textContent = 'Đang tải mã VietQR cho phần chuyển khoản…';

        const data = new URLSearchParams();
        data.append('paymentMethod', 'COMBINED');
        data.append('transferAmount', transferVal);
        data.append('voucherCode', voucherInput?.value || '');
        if (customerIdInput?.value) data.append('customerId', customerIdInput.value);
        data.append('_csrf', csrfToken);

        try {
            const resp = await fetch('/admin/pos/payment-qr', {
                method: 'POST',
                headers: { 'X-Requested-With': 'XMLHttpRequest' },
                body: data
            });
            const qr = await resp.json();
            if (qr.configured && qr.payload && window.drawVietQr && canvas) {
                window.drawVietQr(canvas, qr.payload);
                if (details) {
                    details.hidden = false;
                    details.querySelector('[data-qr-amount]').textContent = formatVND(qr.amount);
                    details.querySelector('[data-qr-reference]').textContent = qr.content || '';
                }
                if (msg) msg.textContent = 'Quét QR thanh toán ' + formatVND(transferVal);
            }
        } catch (e) {
            if (msg) msg.textContent = 'Lỗi tạo QR kết hợp: ' + e.message;
        }
    }

    if (combinedCashInput) {
        combinedCashInput.addEventListener('input', updateCombinedCalculations);
    }
    if (transferReceivedCombined) {
        transferReceivedCombined.addEventListener('change', () => {
            const finalAmount = getFinalTotal();
            const cashVal = parseVND(combinedCashInput?.value);
            const transferVal = finalAmount - cashVal;
            const valid = (cashVal > 0 && transferVal > 0 && (cashVal + transferVal === finalAmount));
            if (btnCheckout) {
                btnCheckout.disabled = !valid || !transferReceivedCombined.checked;
            }
        });
    }

    // -------------------------------------------------------------
    // GIAO HÀNG (DELIVERY) & PHƯƠNG THỨC THANH TOÁN
    // -------------------------------------------------------------
    const deliverySwitch = document.querySelector('#deliverySwitch');
    const deliverySection = document.querySelector('#pos-delivery-section');
    const methodsCounter = document.querySelector('#methods-counter');
    const methodsDelivery = document.querySelector('#methods-delivery');
    const btnSubmitLabel = document.querySelector('#btn-pos-checkout-label');
    const receiverEmailInput = document.querySelector('#receiverEmail');
    const receiverEmailWarning = document.querySelector('#receiver-email-warning');
    const posAddressSelect = document.querySelector('#pos-address-select');
    const posCustomerAddressBox = document.querySelector('#pos-customer-address-box');

    function toggleDeliveryMode(isDelivery) {
        if (!deliverySection) return;
        if (isDelivery) {
            deliverySection.classList.remove('d-none');
            deliverySection.hidden = false;
            if (methodsCounter) methodsCounter.hidden = true;
            if (methodsDelivery) methodsDelivery.hidden = false;
            if (btnSubmitLabel) btnSubmitLabel.textContent = 'TẠO ĐƠN GIAO HÀNG';

            // Đảm bảo phương thức thanh toán phù hợp khi giao hàng (chỉ COD hoặc TRANSFER)
            const currentMethod = document.querySelector('input[name="paymentMethod"]:checked')?.value;
            if (currentMethod !== 'TRANSFER') {
                const codRadio = document.querySelector('#method-delivery-cod');
                if (codRadio) codRadio.checked = true;
            } else {
                const transferRadio = document.querySelector('#method-delivery-transfer');
                if (transferRadio) transferRadio.checked = true;
            }
            document.dispatchEvent(new CustomEvent('delivery:visible'));
        } else {
            deliverySection.classList.add('d-none');
            deliverySection.hidden = true;
            if (methodsCounter) methodsCounter.hidden = false;
            if (methodsDelivery) methodsDelivery.hidden = true;
            if (btnSubmitLabel) btnSubmitLabel.textContent = 'THANH TOÁN';

            // Khi tắt giao hàng, chuyển về Tiền mặt nếu đang chọn COD
            const currentMethod = document.querySelector('input[name="paymentMethod"]:checked')?.value;
            if (currentMethod === 'COD' || !currentMethod) {
                const cashRadio = document.querySelector('#method-cash');
                if (cashRadio) cashRadio.checked = true;
            } else if (currentMethod === 'TRANSFER') {
                const transferRadio = document.querySelector('#method-transfer');
                if (transferRadio) transferRadio.checked = true;
            }
        }
        syncPaymentPanels();
        recalculateTotals();
    }

    if (deliverySwitch) {
        deliverySwitch.addEventListener('change', () => {
            toggleDeliveryMode(deliverySwitch.checked);
        });
    }

    if (receiverEmailInput && receiverEmailWarning) {
        receiverEmailInput.addEventListener('input', () => {
            receiverEmailWarning.hidden = Boolean(receiverEmailInput.value.trim());
        });
    }

    async function loadCustomerAddresses(customerId) {
        if (!posAddressSelect || !posCustomerAddressBox) return;
        if (!customerId) {
            posCustomerAddressBox.hidden = true;
            posAddressSelect.innerHTML = '<option value="">-- Chọn địa chỉ từ sổ địa chỉ --</option>';
            return;
        }
        try {
            const resp = await fetch(`/admin/pos/customers/${customerId}/addresses`);
            if (!resp.ok) return;
            const addrs = await resp.json();
            if (!addrs || addrs.length === 0) {
                posCustomerAddressBox.hidden = true;
                return;
            }
            posAddressSelect.innerHTML = '<option value="">-- Chọn địa chỉ từ sổ địa chỉ --</option>';
            let defaultAddr = null;
            addrs.forEach(a => {
                const opt = document.createElement('option');
                opt.value = a.id;
                opt.textContent = `${a.receiverName} - ${a.phone} (${a.fullAddress || a.streetAddress})`;
                opt.dataset.json = JSON.stringify(a);
                posAddressSelect.appendChild(opt);
                if (a.isDefault) defaultAddr = a;
            });
            posCustomerAddressBox.hidden = false;

            // Tự điền địa chỉ mặc định hoặc địa chỉ đầu tiên nếu các ô đang trống hoặc là Khách lẻ
            const target = defaultAddr || addrs[0];
            const nameField = document.querySelector('#receiverName');
            if (target && (!nameField?.value || nameField?.value === 'Khách lẻ')) {
                fillAddressData(target);
                posAddressSelect.value = target.id;
            }
        } catch (e) {
            console.error('Không tải được sổ địa chỉ:', e);
        }
    }

    function fillAddressData(a) {
        const nameInput = document.querySelector('#receiverName');
        const phoneInput = document.querySelector('#receiverPhone');
        const emailInput = document.querySelector('#receiverEmail');
        if (nameInput) nameInput.value = a.receiverName || '';
        if (phoneInput) phoneInput.value = a.phone || '';
        if (emailInput && a.email) emailInput.value = a.email;
        if (receiverEmailWarning) receiverEmailWarning.hidden = Boolean(emailInput?.value?.trim());

        const deliveryRoot = document.querySelector('[data-delivery-address]');
        if (deliveryRoot) {
            deliveryRoot.dispatchEvent(new CustomEvent('delivery:select-address', { detail: a }));
        }
    }

    if (posAddressSelect) {
        posAddressSelect.addEventListener('change', () => {
            const selectedOpt = posAddressSelect.selectedOptions[0];
            if (!selectedOpt || !selectedOpt.dataset.json) return;
            try {
                const a = JSON.parse(selectedOpt.dataset.json);
                fillAddressData(a);
            } catch (e) {}
        });
    }

    // Switch payment method panels
    function syncPaymentPanels() {
        const isDelivery = Boolean(deliverySwitch?.checked);
        const val = document.querySelector('input[name="paymentMethod"]:checked')?.value || (isDelivery ? 'COD' : 'CASH');
        if (panelCash) panelCash.hidden = isDelivery || (val !== 'CASH');
        if (panelTransfer) panelTransfer.hidden = (val !== 'TRANSFER');
        if (panelCombined) panelCombined.hidden = isDelivery || (val !== 'COMBINED');
        if (panelVnpay) panelVnpay.hidden = (val !== 'VNPAY');

        if (btnCheckout) {
            if (val === 'VNPAY') {
                btnCheckout.textContent = 'Thanh toán qua VNPay';
                btnCheckout.classList.remove('btn-success');
                btnCheckout.classList.add('btn-primary');
            } else {
                btnCheckout.textContent = 'Thanh toán đơn hàng';
                btnCheckout.classList.remove('btn-primary');
                btnCheckout.classList.add('btn-success');
            }
        }
    }
    syncPaymentPanels();

    document.querySelectorAll('input[name="paymentMethod"]').forEach(radio => {
        radio.addEventListener('change', () => {
            syncPaymentPanels();
            recalculateTotals();
        });
    });

    // Form submit validation
    const paymentForm = document.querySelector('#pos-payment');
    if (paymentForm) {
        paymentForm.addEventListener('submit', (e) => {
            const isDelivery = Boolean(deliverySwitch?.checked);
            const method = document.querySelector('input[name="paymentMethod"]:checked')?.value || (isDelivery ? 'COD' : 'CASH');
            const finalAmount = getFinalTotal();

            if (getSubtotal() <= 0) {
                e.preventDefault();
                alert('Giỏ hàng trống. Vui lòng chọn sản phẩm.');
                return;
            }

            if (isDelivery) {
                const receiverName = document.querySelector('#receiverName')?.value?.trim();
                const receiverPhone = document.querySelector('#receiverPhone')?.value?.trim();
                const provinceId = document.querySelector('#pos-del-province')?.value;
                const wardId = document.querySelector('#pos-del-ward')?.value;
                const streetAddress = document.querySelector('#pos-street')?.value?.trim();

                if (!receiverName) {
                    e.preventDefault();
                    alert('Vui lòng nhập họ tên người nhận.');
                    document.querySelector('#receiverName')?.focus();
                    return;
                }
                if (!receiverPhone) {
                    e.preventDefault();
                    alert('Vui lòng nhập số điện thoại người nhận.');
                    document.querySelector('#receiverPhone')?.focus();
                    return;
                }
                if (!provinceId) {
                    e.preventDefault();
                    alert('Vui lòng chọn Tỉnh/Thành phố giao hàng.');
                    document.querySelector('#pos-del-province')?.focus();
                    return;
                }
                if (!wardId) {
                    e.preventDefault();
                    alert('Vui lòng chọn Phường/Xã giao hàng.');
                    document.querySelector('#pos-del-ward')?.focus();
                    return;
                }
                if (!streetAddress) {
                    e.preventDefault();
                    alert('Vui lòng nhập số nhà, tên đường giao hàng.');
                    document.querySelector('#pos-street')?.focus();
                    return;
                }

                if (method === 'TRANSFER') {
                    if (!transferReceivedSingle?.checked) {
                        e.preventDefault();
                        alert('Vui lòng tích xác nhận "Đã nhận đủ tiền chuyển khoản".');
                        transferReceivedSingle?.focus();
                        return;
                    }
                    const transferAmountField = document.querySelector('#transferAmount');
                    if (transferAmountField) transferAmountField.value = finalAmount;
                }
            } else {
                if (method === 'CASH') {
                    const given = parseVND(cashInput?.value);
                    if (given < finalAmount) {
                        e.preventDefault();
                        alert('Tiền khách đưa phải đủ thanh toán tổng đơn hàng.');
                        cashInput?.focus();
                        return;
                    }
                    if (cashInput) cashInput.value = given;
                } else if (method === 'TRANSFER') {
                    if (!transferReceivedSingle?.checked) {
                        e.preventDefault();
                        alert('Vui lòng tích xác nhận "Đã nhận đủ tiền chuyển khoản".');
                        transferReceivedSingle?.focus();
                        return;
                    }
                    const transferAmountField = document.querySelector('#transferAmount');
                    if (transferAmountField) transferAmountField.value = finalAmount;
                } else if (method === 'COMBINED') {
                    const cashVal = parseVND(combinedCashInput?.value);
                    const transferVal = finalAmount - cashVal;

                    if (cashVal <= 0 || transferVal <= 0 || (cashVal + transferVal !== finalAmount)) {
                        e.preventDefault();
                        alert('Vui lòng nhập số tiền mặt hợp lệ (> 0 và nhỏ hơn tổng tiền).');
                        combinedCashInput?.focus();
                        return;
                    }
                    if (!transferReceivedCombined?.checked) {
                        e.preventDefault();
                        alert('Vui lòng tích xác nhận "Đã nhận đủ tiền chuyển khoản".');
                        transferReceivedCombined?.focus();
                        return;
                    }
                    if (cashInput) cashInput.value = cashVal;
                    const transferAmountField = document.querySelector('#transferAmount');
                    if (transferAmountField) transferAmountField.value = transferVal;
                }
            }
        });
    }

    // Initialize state
    if (deliverySwitch) {
        toggleDeliveryMode(deliverySwitch.checked);
    }
    const initialCustomerId = document.querySelector('#customerId')?.value;
    if (initialCustomerId) {
        loadCustomerAddresses(initialCustomerId);
    }
    recalculateTotals();

    // Check VNPay payment return parameters
    try {
        const urlParams = new URLSearchParams(window.location.search);
        const paymentResult = urlParams.get('paymentResult');
        if (paymentResult) {
            const code = urlParams.get('orderCode') || '';
            const isSuccess = paymentResult === 'success';
            showToast(isSuccess
                ? `Thanh toán VNPay thành công${code ? ` cho đơn ${code}` : ''}!`
                : `Thanh toán VNPay không thành công hoặc đã bị hủy${code ? ` cho đơn ${code}` : ''}.`);
            window.history.replaceState({}, document.title, window.location.pathname);
        }
    } catch (e) {
        console.error('Error handling payment result params:', e);
    }

})();
