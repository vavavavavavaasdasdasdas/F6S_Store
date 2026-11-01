(() => {
    const data = window.adminStatistics;
    if (!data) return;
    const labels = {PENDING:'Chờ xử lý',CONFIRMED:'Đã xác nhận',PREPARING:'Đang chuẩn bị',SHIPPING:'Đang giao',DELIVERED:'Hoàn tất',CANCELLED:'Đã hủy',DELIVERY_FAILED:'Giao hàng thất bại'};
    document.querySelectorAll('[data-status]').forEach(el => el.textContent = labels[el.dataset.status] || el.dataset.status);
    const status = document.getElementById('statistics-status');
    if (!status) return;
    if (typeof Chart === 'undefined') {
        const err = document.getElementById('statistics-chart-error');
        if (err) err.hidden = false;
        return;
    }
    const primary = getComputedStyle(document.documentElement).getPropertyValue('--bs-primary').trim() || '#f05d23';
    Chart.defaults.font.family = getComputedStyle(document.body).fontFamily;
    Chart.defaults.animation = false;
    if (data.statuses && data.statuses.length && status) {
        new Chart(status, {
            type: 'doughnut',
            data: {
                labels: data.statuses.map(s => labels[s.status] || s.status),
                datasets: [{
                    data: data.statuses.map(s => s.total),
                    backgroundColor: [primary, '#698abd', '#47a582', '#e9bb55', '#a792c2', '#89929f']
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { position: 'bottom' }
                }
            }
        });
    }
})();
