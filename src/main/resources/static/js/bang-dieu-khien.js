(() => {
    const data = window.adminDashboard || {};
    const form = document.querySelector('form.sales-filters');
    const daysSelect = document.getElementById('dash-days');
    const fromInput = document.getElementById('from');
    const toInput = document.getElementById('to');
    const formMessage = form ? form.querySelector('.form-message') : null;

    function formatDate(d) {
        const yyyy = d.getFullYear();
        const mm = String(d.getMonth() + 1).padStart(2, '0');
        const dd = String(d.getDate()).padStart(2, '0');
        return `${yyyy}-${mm}-${dd}`;
    }

    function getToday() {
        if (data && data.today) {
            const parts = data.today.split('-');
            if (parts.length === 3) {
                return new Date(parseInt(parts[0], 10), parseInt(parts[1], 10) - 1, parseInt(parts[2], 10));
            }
        }
        const now = new Date();
        return new Date(now.getFullYear(), now.getMonth(), now.getDate());
    }

    function applyQuickRange(val) {
        if (!val || val === 'custom') return;
        const today = getToday();
        let start, end;
        if (val === 'today') {
            start = new Date(today);
            end = new Date(today);
        } else if (val === '7') {
            start = new Date(today);
            start.setDate(start.getDate() - 6);
            end = new Date(today);
        } else if (val === '30') {
            start = new Date(today);
            start.setDate(start.getDate() - 29);
            end = new Date(today);
        } else if (val === 'month') {
            start = new Date(today.getFullYear(), today.getMonth(), 1);
            end = new Date(today);
        } else if (val === 'last-month') {
            start = new Date(today.getFullYear(), today.getMonth() - 1, 1);
            end = new Date(today.getFullYear(), today.getMonth(), 0);
        }
        if (start && end && fromInput && toInput) {
            fromInput.value = formatDate(start);
            toInput.value = formatDate(end);
            clearError();
        }
    }

    function clearError() {
        if (formMessage) {
            formMessage.textContent = '';
            formMessage.hidden = true;
        }
        if (fromInput) fromInput.setCustomValidity('');
        if (toInput) toInput.setCustomValidity('');
    }

    function validateDates() {
        if (fromInput && toInput && fromInput.value && toInput.value) {
            if (fromInput.value > toInput.value) {
                const msg = 'Từ ngày phải nhỏ hơn hoặc bằng đến ngày.';
                if (formMessage) {
                    formMessage.textContent = msg;
                    formMessage.hidden = false;
                }
                toInput.setCustomValidity(msg);
                return false;
            }
        }
        clearError();
        return true;
    }

    if (daysSelect && fromInput && toInput) {
        daysSelect.addEventListener('change', () => {
            applyQuickRange(daysSelect.value);
        });

        const onDateManualChange = () => {
            clearError();
            if (daysSelect.value !== 'custom') {
                daysSelect.value = 'custom';
            }
            validateDates();
        };

        fromInput.addEventListener('input', onDateManualChange);
        toInput.addEventListener('input', onDateManualChange);
        fromInput.addEventListener('change', onDateManualChange);
        toInput.addEventListener('change', onDateManualChange);
    }

    if (form) {
        form.addEventListener('submit', (e) => {
            if (!validateDates()) {
                e.preventDefault();
                e.stopPropagation();
                if (toInput && typeof toInput.reportValidity === 'function') {
                    toInput.reportValidity();
                }
                return false;
            }
        });

        form.querySelectorAll('input, select').forEach(el => {
            el.addEventListener('keydown', (e) => {
                if (e.key === 'Enter') {
                    if (validateDates()) {
                        form.submit();
                    } else {
                        e.preventDefault();
                    }
                }
            });
        });
    }

    const statusEl = document.getElementById('status-chart');
    if (!statusEl) return;
    if (typeof Chart === 'undefined') {
        const err = document.getElementById('chart-error');
        if (err) err.hidden = false;
        return;
    }
    const primary = getComputedStyle(document.documentElement).getPropertyValue('--bs-primary').trim() || '#f05d23';
    Chart.defaults.font.family = getComputedStyle(document.body).fontFamily;
    Chart.defaults.color = '#59636f';
    Chart.defaults.animation = false;
    if (data.statuses && data.statuses.length && statusEl) {
        new Chart(statusEl, {
            type: 'doughnut',
            data: {
                labels: data.statuses.map(row => (data.statusLabels && data.statusLabels[row.status]) || row.status),
                datasets: [{
                    data: data.statuses.map(row => row.total),
                    backgroundColor: [primary, '#698abd', '#47a582', '#e9bb55', '#a792c2', '#89929f'],
                    borderWidth: 3
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                cutout: '72%',
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: { boxWidth: 9, padding: 18, font: { size: 10 } }
                    }
                }
            }
        });
    }
})();
