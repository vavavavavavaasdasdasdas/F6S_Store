(() => {
    const toggles = document.querySelectorAll('#admin-menu-toggle, [data-admin-menu-toggle], [data-admin-sidebar-toggle], .admin-sidebar-toggle');
    const sidebar = document.getElementById('admin-sidebar');
    const backdrop = document.querySelector('.admin-backdrop');
    if (!sidebar) return;
    const close = () => {
        document.body.classList.remove('menu-open');
        toggles.forEach(t => {
            t.setAttribute('aria-expanded', 'false');
            t.setAttribute('aria-label', 'Mở menu quản trị');
        });
        if (backdrop) backdrop.hidden = true;
    };
    toggles.forEach(toggle => {
        toggle.addEventListener('click', () => {
            if (document.body.classList.contains('menu-open')) { close(); return; }
            document.body.classList.add('menu-open');
            toggles.forEach(t => {
                t.setAttribute('aria-expanded', 'true');
                t.setAttribute('aria-label', 'Đóng menu quản trị');
            });
            if (backdrop) backdrop.hidden = false;
            const firstLink = sidebar.querySelector('a');
            if (firstLink) firstLink.focus();
        });
    });
    if (backdrop) {
        backdrop.addEventListener('click', () => {
            close();
            const firstToggle = toggles[0];
            if (firstToggle) firstToggle.focus();
        });
    }
    document.addEventListener('keydown', event => {
        if (!document.body.classList.contains('menu-open')) return;
        const firstToggle = toggles[0];
        if (event.key === 'Escape') {
            close();
            if (firstToggle) firstToggle.focus();
        }
        if (event.key === 'Tab') {
            const controls = [...sidebar.querySelectorAll('a, button, summary')].filter(el=>el.getClientRects().length).concat([...toggles]);
            const first = controls[0], last = controls[controls.length - 1];
            if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
            else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
        }
    });
    matchMedia('(min-width:1025px)').addEventListener('change', close);
    document.querySelectorAll('.flash-close').forEach(button => button.addEventListener('click', () => button.parentElement.remove()));
})();

/* ==========================================================================
   GIAI ĐOẠN 30: Accordion Sidebar & Synchronized Navigation
   - Cấp 1: BÁN HÀNG, QUẢN LÝ SẢN PHẨM, TÀI KHOẢN, KHUYẾN MÃI, HỖ TRỢ KHÁCH HÀNG
   - Cấp 2: Sản phẩm, Biến thể (bên trong QUẢN LÝ SẢN PHẨM)
   - Độc quyền theo cấp: Mở 1 nhóm thì các nhóm cùng cấp tự đóng
   - Server-render trạng thái ban đầu, KHÔNG khôi phục từ localStorage
   - Badge tin chưa đọc: nhóm đóng hiện ở tiêu đề nhóm, nhóm mở chỉ hiện ở mục Chat
   ========================================================================== */
(() => {
    // 1. Level 1 accordions
    const level1Accordions = document.querySelectorAll('.admin-nav-accordion[data-nav-level="1"]');
    level1Accordions.forEach(acc => {
        const summary = acc.querySelector(':scope > summary');
        const syncLevel1 = () => {
            if (summary) summary.setAttribute('aria-expanded', String(acc.open));
            // Sync group chat badge
            const groupBadge = acc.querySelector('[data-group-chat-badge]');
            if (groupBadge) {
                const chatBadge = acc.querySelector('[data-staff-chat-badge]');
                const countText = chatBadge ? chatBadge.textContent : groupBadge.textContent;
                const count = parseInt(countText || '0', 10);
                groupBadge.textContent = countText;
                if (acc.open || isNaN(count) || count <= 0) {
                    groupBadge.hidden = true;
                } else {
                    groupBadge.hidden = false;
                }
            }
        };

        acc.addEventListener('toggle', () => {
            syncLevel1();
            if (acc.open) {
                level1Accordions.forEach(other => {
                    if (other !== acc && other.open) {
                        other.open = false;
                    }
                });
            }
        });

        // Bàn phím Enter / Space / ArrowRight / ArrowLeft
        if (summary) {
            summary.addEventListener('keydown', event => {
                if (event.key === 'ArrowRight' && !acc.open) {
                    acc.open = true;
                    event.preventDefault();
                } else if (event.key === 'ArrowLeft' && acc.open) {
                    acc.open = false;
                    event.preventDefault();
                }
            });
        }
        syncLevel1();
    });

    // 2. Level 2 sub-accordions (Sản phẩm & Biến thể)
    const level2Accordions = document.querySelectorAll('.admin-nav-sub-accordion[data-nav-level="2"]');
    level2Accordions.forEach(sub => {
        const summary = sub.querySelector(':scope > summary');
        const syncLevel2 = () => {
            if (summary) summary.setAttribute('aria-expanded', String(sub.open));
        };

        sub.addEventListener('toggle', () => {
            syncLevel2();
            if (sub.open) {
                const parent = sub.closest('.admin-nav-body');
                if (parent) {
                    parent.querySelectorAll('.admin-nav-sub-accordion[data-nav-level="2"]').forEach(other => {
                        if (other !== sub && other.open) {
                            other.open = false;
                        }
                    });
                }
            }
        });

        if (summary) {
            summary.addEventListener('keydown', event => {
                if (event.key === 'ArrowRight' && !sub.open) {
                    sub.open = true;
                    event.preventDefault();
                } else if (event.key === 'ArrowLeft' && sub.open) {
                    sub.open = false;
                    event.preventDefault();
                }
            });
        }
        syncLevel2();
    });
})();

/* ==========================================================================
   GIAI ĐOẠN 30: Dialog Điều chỉnh tồn & Lịch sử kho cho Biến thể
   ========================================================================== */
(() => {
    // Listen for adjust clicks
    document.addEventListener('click', event => {
        const adjustBtn = event.target.closest('[data-variant-adjust]');
        if (adjustBtn) {
            const id = adjustBtn.dataset.id;
            const dialog = document.getElementById('variant-adjust-dialog');
            if (dialog) {
                const idInput = dialog.querySelector('input[name="variantId"]');
                const expInput = dialog.querySelector('input[name="expectedQuantity"]');
                const infoSpan = dialog.querySelector('[data-adjust-info]');
                const currentQtySpan = dialog.querySelector('[data-adjust-current]');
                if (idInput) idInput.value = id;
                if (expInput) expInput.value = adjustBtn.dataset.qty || '0';
                if (currentQtySpan) currentQtySpan.textContent = adjustBtn.dataset.qty || '0';
                if (infoSpan) {
                    infoSpan.textContent = `${adjustBtn.dataset.code || ''} · ${adjustBtn.dataset.name || ''} (Size ${adjustBtn.dataset.size || ''}, Màu ${adjustBtn.dataset.color || ''})`;
                }
                const errBox = dialog.querySelector('[data-adjust-error]');
                if (errBox) { errBox.textContent = ''; errBox.hidden = true; }
                if (typeof dialog.showModal === 'function') dialog.showModal();
                else dialog.setAttribute('open', '');
            } else {
                // Fallback to navigate to adjust page
                window.location.href = `/admin/variants/adjust/${id}`;
            }
        }

        const historyBtn = event.target.closest('[data-variant-history]');
        if (historyBtn) {
            const id = historyBtn.dataset.id;
            const dialog = document.getElementById('variant-history-dialog');
            if (dialog) {
                const titleSpan = dialog.querySelector('[data-history-title]');
                if (titleSpan) titleSpan.textContent = `${historyBtn.dataset.code || ''} · ${historyBtn.dataset.name || ''}`;
                const tbody = dialog.querySelector('[data-history-tbody]');
                if (tbody) tbody.innerHTML = '<tr><td colspan="6" class="text-center py-3">Đang tải lịch sử...</td></tr>';
                if (typeof dialog.showModal === 'function') dialog.showModal();
                else dialog.setAttribute('open', '');

                fetch(`/admin/variants/${id}/history`, {
                    headers: { 'Accept': 'application/json', 'X-Requested-With': 'XMLHttpRequest' }
                })
                .then(res => res.json())
                .then(data => {
                    if (!tbody) return;
                    if (!data.content || data.content.length === 0) {
                        tbody.innerHTML = '<tr><td colspan="6" class="text-center py-3 text-muted">Chưa có lịch sử biến động cho biến thể này.</td></tr>';
                        return;
                    }
                    let html = '';
                    data.content.forEach((log, index) => {
                        const changeStr = (log.quantity_change > 0 ? '+' : '') + log.quantity_change;
                        const changeColor = log.quantity_change > 0 ? 'text-success' : log.quantity_change < 0 ? 'text-danger' : 'text-muted';
                        const timeStr = String(log.created_at || '').substring(0, 19).replace('T', ' ');
                        html += `<tr>
                            <td class="row-number">${index + 1}</td>
                            <td>${timeStr}</td>
                            <td>${log.quantity_before != null ? log.quantity_before : ''}</td>
                            <td><strong class="${changeColor}">${changeStr}</strong></td>
                            <td>${log.quantity_after != null ? log.quantity_after : ''}</td>
                            <td>${log.reason || ''}</td>
                            <td>${log.actor || 'Hệ thống'}</td>
                        </tr>`;
                    });
                    tbody.innerHTML = html;
                })
                .catch(() => {
                    if (tbody) tbody.innerHTML = '<tr><td colspan="6" class="text-center py-3 text-danger">Lỗi khi tải lịch sử biến thể.</td></tr>';
                });
            }
        }
    });

    // Close buttons for dialogs
    document.querySelectorAll('[data-dialog-close]').forEach(btn => {
        btn.addEventListener('click', () => {
            const dialog = btn.closest('dialog');
            if (dialog) {
                if (typeof dialog.close === 'function') dialog.close();
                else dialog.removeAttribute('open');
            }
        });
    });
})();
