(() => {
    const toggles = document.querySelectorAll('#admin-menu-toggle, [data-admin-menu-toggle]');
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

document.querySelectorAll('[data-nav-section]').forEach(section=>{
 const active=!!section.querySelector('[aria-current="page"]'),key='admin-menu:'+section.dataset.navSection,summary=section.querySelector('summary');
 try{section.open=active||localStorage.getItem(key)==='open';}catch(_){section.open=active;}
 const sync=()=>{summary.setAttribute('aria-expanded',String(section.open));try{localStorage.setItem(key,section.open?'open':'closed');}catch(_){}};
 section.addEventListener('toggle',sync);summary.addEventListener('keydown',event=>{if(event.key==='ArrowRight'){section.open=true;event.preventDefault();}else if(event.key==='ArrowLeft'){section.open=false;event.preventDefault();}});sync();
});
