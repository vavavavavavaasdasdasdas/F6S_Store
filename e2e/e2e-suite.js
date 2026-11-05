const { chromium } = require('playwright');

(async () => {
    const baseUrl = process.env.BASE_URL || 'http://localhost:8080';
    console.log(`=== B?T ??U E2E TEST SUITE CHO F6S STORE (${baseUrl}) ===`);
    
    const browser = await chromium.launch({ headless: true });
    let passed = 0;
    let failed = 0;

    const runCheck = async (name, fn) => {
        try {
            process.stdout.write(`- [TEST] ${name}... `);
            await fn();
            console.log('PASS');
            passed++;
        } catch (err) {
            console.log(`FAIL (${err.message})`);
            failed++;
        }
    };

    // 1. Desktop Context (1280x800)
    const desktopContext = await browser.newContext({ viewport: { width: 1280, height: 800 } });
    const page = await desktopContext.newPage();
    const consoleErrors = [];
    page.on('console', msg => {
        if (msg.type() === 'error') consoleErrors.push(msg.text());
    });

    // Test 1: Storefront Home
    await runCheck('Kh?ch truy c?p Trang ch? (1280px)', async () => {
        await page.goto(`${baseUrl}/`);
        if (!await page.$('.store-header, header, nav')) throw new Error('Kh?ng t?m th?y header');
    });

    // Test 2: Storefront Cart
    await runCheck('Kh?ch truy c?p Gi? h?ng', async () => {
        await page.goto(`${baseUrl}/cart`);
        const title = await page.title();
        if (!title.includes('Gi? h?ng') && !title.includes('Store')) throw new Error('Sai ti?u ??');
    });

    // Test 3: Admin Login
    await runCheck('??ng nh?p t?i kho?n Qu?n tr? vi?n (admin/admin123)', async () => {
        await page.goto(`${baseUrl}/login`);
        await page.fill('#username', 'admin');
        await page.fill('#password', 'admin123');
        await page.click('button[type="submit"]');
        await page.waitForNavigation({ waitUntil: 'load' });
        if (!page.url().includes('/admin')) throw new Error(`URL sau ??ng nh?p sai: ${page.url()}`);
    });

    // Test 4: Dashboard
    await runCheck('B?ng ?i?u khi?n Admin Dashboard', async () => {
        await page.goto(`${baseUrl}/admin/dashboard`);
        const h1 = await page.textContent('h1');
        if (!h1.toLowerCase().includes('b?ng ?i?u khi?n') && !h1.toLowerCase().includes('dashboard')) {
            throw new Error('Kh?ng ??ng ti?u ?? Dashboard');
        }
    });

    // Test 5: POS B?n h?ng t?i qu?y
    await runCheck('Trang B?n h?ng t?i qu?y (POS)', async () => {
        await page.goto(`${baseUrl}/admin/pos`);
        const sidebar = await page.textContent('#admin-sidebar');
        if (!sidebar.includes('B?n h?ng t?i qu?y')) throw new Error('Sidebar thi?u B?n h?ng t?i qu?y');
        if (sidebar.includes('B?n online')) throw new Error('Sidebar c?n s?t B?n online');
    });

    // Test 6: Qu?n l? h?a ??n
    await runCheck('Trang Qu?n l? h?a ??n', async () => {
        await page.goto(`${baseUrl}/admin/invoices`);
        const h1 = await page.textContent('h1');
        if (!h1.includes('h?a ??n')) throw new Error('Kh?ng ??ng trang h?a ??n');
    });

    // Test 7: Danh s?ch bi?n th? & T?n kho
    await runCheck('Trang Danh s?ch bi?n th? (t?ch h?p T?n kho)', async () => {
        await page.goto(`${baseUrl}/admin/variants`);
        const select = await page.$('select[name="stock"]');
        if (!select) throw new Error('Thi?u b? l?c t?n kho stock');
    });

    // Test 8: Chat v?i kh?ch
    await runCheck('Trang H?p th? h? tr? kh?ch h?ng', async () => {
        await page.goto(`${baseUrl}/admin/chat`);
        if (!page.url().includes('/admin/chat')) throw new Error('Kh?ng truy c?p ???c /admin/chat');
    });

    // 2. Mobile Context (375x667)
    const mobileContext = await browser.newContext({ viewport: { width: 375, height: 667 } });
    const mobilePage = await mobileContext.newPage();

    // Test 9: Mobile POS
    await runCheck('Giao di?n Mobile (375px) B?n h?ng t?i qu?y', async () => {
        await mobilePage.goto(`${baseUrl}/admin/pos`);
        const toggleBtn = await mobilePage.$('#admin-menu-toggle-fixed, #admin-menu-toggle');
        if (toggleBtn) {
            await toggleBtn.click();
            await mobilePage.waitForTimeout(200);
        }
    });

    // Test 10: Ki?m tra kh?ng c? l?i JavaScript nghi?m tr?ng
    await runCheck('Ki?m tra console kh?ng c? l?i JavaScript nghi?m tr?ng', async () => {
        const severeErrors = consoleErrors.filter(e => !e.includes('favicon') && !e.includes('404'));
        if (severeErrors.length > 0) {
            throw new Error(`Ph?t hi?n l?i console: ${severeErrors[0]}`);
        }
    });

    await browser.close();
    console.log(`=== K?T QU?: ${passed} PASS, ${failed} FAIL ===`);
    if (failed > 0) process.exit(1);
})();
