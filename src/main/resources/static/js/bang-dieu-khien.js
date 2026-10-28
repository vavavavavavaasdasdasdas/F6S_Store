(() => {
    if (typeof Chart === 'undefined') { document.getElementById('chart-error').hidden = false; return; }
    const data = window.adminDashboard;
    const primary = getComputedStyle(document.documentElement).getPropertyValue('--bs-primary').trim();
    Chart.defaults.font.family = getComputedStyle(document.body).fontFamily;
    Chart.defaults.color = '#59636f';
    Chart.defaults.animation = false;
    const options = {responsive:true, maintainAspectRatio:false, plugins:{legend:{display:false}}, scales:{x:{grid:{display:false}},y:{beginAtZero:true,grid:{color:'#f0f2f5'}}}};
    new Chart(document.getElementById('revenue-chart'), {type:'line',data:{labels:data.labels.map(day => day.slice(8) + '/' + day.slice(5,7)),datasets:[{label:'Doanh thu (₫)',data:data.values,borderColor:primary,backgroundColor:'rgba(240,93,35,.08)',fill:true,tension:.3,pointRadius:3,borderWidth:2}]},options});
    if (data.statuses.length) new Chart(document.getElementById('status-chart'), {type:'doughnut',data:{labels:data.statuses.map(row=>data.statusLabels[row.status]||row.status),datasets:[{data:data.statuses.map(row=>row.total),backgroundColor:['#f05d23','#698abd','#47a582','#e9bb55','#a792c2','#89929f'],borderWidth:3}]},options:{responsive:true,maintainAspectRatio:false,cutout:'72%',plugins:{legend:{position:'bottom',labels:{boxWidth:9,padding:18,font:{size:10}}}}}});
    if (data.products.length) new Chart(document.getElementById('products-chart'), {type:'bar',data:{labels:data.products.map(row=>row.name),datasets:[{label:'Số lượng bán',data:data.products.map(row=>row.quantity),backgroundColor:primary,borderRadius:5,barThickness:18}]},options:{...options,indexAxis:'y',scales:{x:{beginAtZero:true,ticks:{precision:0},grid:{color:'#f0f2f5'}},y:{grid:{display:false},ticks:{font:{size:10}}}}}});
})();
