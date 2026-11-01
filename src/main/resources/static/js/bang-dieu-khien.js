(() => {
    const data = window.adminDashboard;
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
                    backgroundColor: ['#f05d23', '#698abd', '#47a582', '#e9bb55', '#a792c2', '#89929f'],
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
