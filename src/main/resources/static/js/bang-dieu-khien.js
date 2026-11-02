(() => {
    const data = window.adminDashboard;
    const daysSelect = document.getElementById('dash-days');
    const fromInput = document.getElementById('from');
    const toInput = document.getElementById('to');

    if (daysSelect && fromInput && toInput) {
        daysSelect.addEventListener('change', () => {
            const val = parseInt(daysSelect.value, 10);
            if (val === 7 || val === 30) {
                const refStr = toInput.value || (data && data.today) || new Date().toISOString().slice(0, 10);
                const end = new Date(refStr + 'T00:00:00');
                if (!Number.isNaN(end.getTime())) {
                    const start = new Date(end);
                    start.setDate(start.getDate() - (val - 1));
                    const yyyy = start.getFullYear();
                    const mm = String(start.getMonth() + 1).padStart(2, '0');
                    const dd = String(start.getDate()).padStart(2, '0');
                    fromInput.value = `${yyyy}-${mm}-${dd}`;
                    toInput.value = refStr;
                }
            }
        });
        const markCustom = () => {
            if (daysSelect.value !== '') {
                daysSelect.value = '';
            }
        };
        fromInput.addEventListener('change', markCustom);
        toInput.addEventListener('change', markCustom);
    }

    if (!data) return;
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
