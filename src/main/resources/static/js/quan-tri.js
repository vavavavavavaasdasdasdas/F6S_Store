(() => {
    const toggle = document.getElementById('admin-menu-toggle');
    const sidebar = document.getElementById('admin-sidebar');
    const backdrop = document.querySelector('.admin-backdrop');
    const close = () => {
        document.body.classList.remove('menu-open');
        toggle.setAttribute('aria-expanded', 'false');
        toggle.setAttribute('aria-label', 'Mở menu quản trị');
        backdrop.hidden = true;
    };
    toggle.addEventListener('click', () => {
        if (document.body.classList.contains('menu-open')) { close(); return; }
        document.body.classList.add('menu-open');
        toggle.setAttribute('aria-expanded', 'true');
        toggle.setAttribute('aria-label', 'Đóng menu quản trị');
        backdrop.hidden = false;
        sidebar.querySelector('a').focus();
    });
    backdrop.addEventListener('click', () => { close(); toggle.focus(); });
    document.addEventListener('keydown', event => {
        if (!document.body.classList.contains('menu-open')) return;
        if (event.key === 'Escape') { close(); toggle.focus(); }
        if (event.key === 'Tab') {
            const controls = [...sidebar.querySelectorAll('a, button, summary')].filter(el=>el.getClientRects().length).concat(toggle);
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
