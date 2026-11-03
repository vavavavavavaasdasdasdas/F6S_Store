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

            document.querySelector('#new-tab-customer-id').value = customerIdInput?.value || '';
            document.querySelector('#new-tab-voucher-code').value = voucherInput?.value || '';
            document.querySelector('#new-tab-payment-method').value = methodInput?.value || 'CASH';
            document.querySelector('#new-tab-note').value = noteInput?.value || '';

            newTabForm.submit();
        });
    }

    // Tab switching
    document.querySelectorAll('[data-open-hold]').forEach(el => {
        el.addEventListener('click', () => {
            const holdId = el.dataset.holdId;
            if (!holdId || el.closest('.pos-tab-item')?.classList.contains('is-active')) return;

            const form = document.createElement('form');
            form.method = 'POST';
            form.action = `/admin/pos/holds/${holdId}/open`;
            form.innerHTML = `
                <input type="hidden" name="${csrfHeader === 'X-CSRF-TOKEN' ? '_csrf' : csrfHeader}" value="${csrfToken}">
                <input type="hidden" name="action" value="SAVE">
                <input type="hidden" name="customerId" value="${document.querySelector('#customerId')?.value || ''}">
                <input type="hidden" name="voucherCode" value="${document.querySelector('#posVoucher')?.value || ''}">
                <input type="hidden" name="paymentMethod" value="${document.querySelector('input[name="paymentMethod"]:checked')?.value || 'CASH'}">
                <input type="hidden" name="note" value="${document.querySelector('#posNote')?.value || ''}">
            `;
            document.body.appendChild(form);
            form.submit();
        });
    });

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
    // Gõ tay số lượng
    document.querySelectorAll('.pos-qty-input').forEach(input => {
        const updateQty = async () => {
            const variantId = input.dataset.variantId;
            const qty = Math.max(1, parseInt(input.value) || 1);
            input.value = qty;

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
                if (!resp.ok) {
                    const err = await resp.json();
                    alert(err.message || 'Lỗi cập nhật số lượng.');
                    window.location.reload();
                    return;
                }
                window.location.reload();
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
                window.location.reload();
            } catch (e) {
                window.location.reload();
            }
        });
    });

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

    let modalItemsAdded = false;

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

    function attachModalActions() {
        // Pagination buttons in modal
        modalProductsContainer.querySelectorAll('.pos-modal-page-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                const page = btn.dataset.page;
                if (page != null) loadProductsModal(parseInt(page));
            });
        });

        // Add variant to cart button in modal
        modalProductsContainer.querySelectorAll('.pos-btn-add-variant').forEach(btn => {
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
                        modalItemsAdded = true;
                        showToast(`Đã thêm ${variantName} vào giỏ`);
                        const badge = document.querySelector('#pos-cart-badge');
                        if (badge && res.totalItems) badge.textContent = `${res.totalItems} sản phẩm`;
                        const subtotalEl = document.querySelector('[data-subtotal]');
                        if (subtotalEl && res.total != null) subtotalEl.textContent = formatVND(res.total);
                        recalculateTotals();
                    } else {
                        alert(res.message || 'Không thể thêm sản phẩm.');
                    }
                } catch (e) {
                    showToast(`Đã thêm ${variantName} vào giỏ`);
                    modalItemsAdded = true;
                } finally {
                    btn.disabled = false;
                    btn.textContent = '+ Thêm';
                }
            });
        });
    }

    if (btnOpenProductsModal && productsModal) {
        btnOpenProductsModal.addEventListener('click', () => {
            modalItemsAdded = false;
            productsModal.showModal();
            loadProductsModal(0);
        });

        const closeModal = () => {
            productsModal.close();
            if (modalItemsAdded) {
                window.location.reload();
            }
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

        recalculateTotals();
    }

    if (btnClearCustomer) {
        btnClearCustomer.addEventListener('click', () => {
            if (customerIdInput) customerIdInput.value = '';
            if (guestDisplay) guestDisplay.hidden = false;
            if (customerDisplay) customerDisplay.hidden = true;
            if (customerModeLabel) customerModeLabel.textContent = 'Khách lẻ';
            if (customerSearch) customerSearch.value = '';
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

        const method = document.querySelector('input[name="paymentMethod"]:checked')?.value || 'CASH';
        if (method === 'CASH') {
            updateCashCalculations();
        } else if (method === 'TRANSFER') {
            refreshTransferQr();
        } else if (method === 'COMBINED') {
            updateCombinedCalculations();
        }
    }

    // Voucher handling
    if (btnApplyVoucher && voucherInput) {
        btnApplyVoucher.addEventListener('click', async () => {
            const code = voucherInput.value.trim();
            if (!code) {
                currentDiscount = 0;
                if (discountRow) discountRow.hidden = true;
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
                    if (discountRow) discountRow.hidden = false;
                    if (voucherMessage) {
                        voucherMessage.className = 'small mb-1 text-success';
                        voucherMessage.textContent = res.message || 'Đã áp dụng mã giảm giá.';
                    }
                } else {
                    currentDiscount = 0;
                    if (discountRow) discountRow.hidden = true;
                    if (voucherMessage) {
                        voucherMessage.className = 'small mb-1 text-danger';
                        voucherMessage.textContent = res.message || 'Mã giảm giá không hợp lệ.';
                    }
                }
            } catch (e) {
                currentDiscount = 0;
                if (discountRow) discountRow.hidden = true;
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

    // Switch payment method radios
    function syncPaymentPanels() {
        const val = document.querySelector('input[name="paymentMethod"]:checked')?.value || 'CASH';
        if (panelCash) panelCash.hidden = (val !== 'CASH');
        if (panelTransfer) panelTransfer.hidden = (val !== 'TRANSFER');
        if (panelCombined) panelCombined.hidden = (val !== 'COMBINED');
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
            const method = document.querySelector('input[name="paymentMethod"]:checked')?.value || 'CASH';
            const finalAmount = getFinalTotal();

            if (getSubtotal() <= 0) {
                e.preventDefault();
                alert('Giỏ hàng trống. Vui lòng chọn sản phẩm.');
                return;
            }

            if (method === 'CASH') {
                const given = parseVND(cashInput?.value);
                if (given < finalAmount) {
                    e.preventDefault();
                    alert('Tiền khách đưa phải đủ thanh toán tổng đơn hàng.');
                    cashInput?.focus();
                    return;
                }
                // Format to standard number before submission
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
                // Set cashReceived as the cash portion for server
                if (cashInput) cashInput.value = cashVal;
                const transferAmountField = document.querySelector('#transferAmount');
                if (transferAmountField) transferAmountField.value = transferVal;
            }
        });
    }

    // Initialize state
    recalculateTotals();

})();
