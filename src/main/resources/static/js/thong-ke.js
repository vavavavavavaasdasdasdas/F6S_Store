(() => {
    const data = window.adminStatistics;
    const labels = {PENDING:'Chờ xử lý',CONFIRMED:'Đã xác nhận',PREPARING:'Đang chuẩn bị',SHIPPING:'Đang giao',DELIVERED:'Hoàn tất',CANCELLED:'Đã hủy',DELIVERY_FAILED:'Giao hàng thất bại'};
    document.querySelectorAll('[data-status]').forEach(el => el.textContent = labels[el.dataset.status] || el.dataset.status);
    const revenue = document.getElementById('statistics-revenue');
    const status = document.getElementById('statistics-status');
    if (!revenue && !status) return;
    if (typeof Chart === 'undefined') {document.getElementById('statistics-chart-error').hidden=false;return;}
    const primary = getComputedStyle(document.documentElement).getPropertyValue('--bs-primary').trim();
    Chart.defaults.font.family = getComputedStyle(document.body).fontFamily;
    Chart.defaults.animation = false;
    if (revenue) new Chart(revenue,{type:'bar',data:{labels:data.daily.map(d=>d.day.slice(8,10)+'/'+d.day.slice(5,7)+'/'+d.day.slice(0,4)),datasets:[{label:'Doanh thu (₫)',data:data.daily.map(d=>d.amount),backgroundColor:primary}]},options:{responsive:true,maintainAspectRatio:false,plugins:{legend:{display:false}},scales:{y:{beginAtZero:true},x:{ticks:{maxTicksLimit:12}}}}});
    if (status) new Chart(status,{type:'doughnut',data:{labels:data.statuses.map(s=>labels[s.status]||s.status),datasets:[{data:data.statuses.map(s=>s.total),backgroundColor:[primary,'#698abd','#47a582','#e9bb55','#a792c2','#89929f']}]},options:{responsive:true,maintainAspectRatio:false,plugins:{legend:{position:'bottom'}}}});
})();
