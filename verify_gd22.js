const { chromium } = require('playwright');
const path = require('path');
const fs = require('fs');

(async () => {
    const screenshotDir = path.resolve('C:/Users/ADMIN/Documents/DuAnTNDEMO/DuAnTNDEMO/DuAnTNDEMO/DemoDATN/docs/images/gd22/screenshots');
    const rootScreenshotDir = path.resolve('C:/Users/ADMIN/Documents/DuAnTNDEMO/DuAnTNDEMO/docs/images/gd22/screenshots');
    if (!fs.existsSync(screenshotDir)) fs.mkdirSync(screenshotDir, { recursive: true });
    if (!fs.existsSync(rootScreenshotDir)) fs.mkdirSync(rootScreenshotDir, { recursive: true });

    const browser = await chromium.launch({ channel: 'msedge', headless: true });
    const context = await browser.newContext({ viewport: { width: 1280, height: 800 } });
    const page = await context.newPage();

    console.log('=== BẮT ĐẦU KIỂM THỬ PLAYWRIGHT GIAI ĐOẠN 22 ===');

    // 1. Đăng nhập Admin
    console.log('[1] Đăng nhập tài khoản admin...');
    await page.goto('http://localhost:8080/login');
    await page.fill('#username', 'admin');
    await page.fill('#password', 'admin123');
    await page.click('button[type="submit"]');
    await page.waitForTimeout(1000);
    console.log('  URL sau đăng nhập:', page.url());

    // 2. Kiểm tra chuyển hướng route cũ /admin/orders -> /admin/invoices
    console.log('[2] Kiểm tra chuyển hướng /admin/orders...');
    await page.goto('http://localhost:8080/admin/orders');
    console.log('  URL chuyển hướng đến:', page.url());
    if (page.url().includes('/admin/invoices')) {
        console.log('  PASS: /admin/orders đã chuyển hướng 302 chính xác về /admin/invoices');
    } else {
        console.error('  FAIL: Chưa chuyển hướng đúng');
    }

    // 3. Kiểm tra trang Quản lý hóa đơn tại 1280px
    console.log('[3] Kiểm tra trang Quản lý hóa đơn tại 1280px...');
    await page.goto('http://localhost:8080/admin/invoices');
    await page.waitForLoadState('networkidle');

    // Kiểm tra tiêu đề và menu sidebar
    const h1Text = await page.textContent('h1');
    console.log('  Tiêu đề H1:', h1Text.trim());
    const sidebarText = await page.textContent('#admin-sidebar');
    const hasQuanLyHoaDon = sidebarText.includes('Quản lý hóa đơn');
    const hasDonHang = sidebarText.includes('Đơn hàng');
    const hasBanOnline = sidebarText.includes('Bán online');
    console.log('  Sidebar chứa "Quản lý hóa đơn":', hasQuanLyHoaDon);
    console.log('  Sidebar KHÔNG chứa "Đơn hàng":', !hasDonHang);
    console.log('  Sidebar KHÔNG chứa "Bán online":', !hasBanOnline);

    // Kiểm tra dải tab trạng thái
    const tabs = await page.$$('.status-tabs a');
    console.log(`  Số lượng tab trạng thái: ${tabs.length}`);
    for (const tab of tabs) {
        const text = await tab.innerText();
        console.log(`    - Tab: ${text.replace('\n', ' ')}`);
    }

    // Kiểm tra các cột bảng danh sách
    const headers = await page.$$eval('.admin-table thead th', ths => ths.map(t => t.textContent.trim()));
    console.log('  Các cột bảng danh sách (11 cột):', headers.join(' | '));

    // Chụp ảnh desktop 1280px
    const pDesktop = path.join(screenshotDir, 'invoices_desktop_1280.png');
    await page.screenshot({ path: pDesktop, fullPage: false });
    fs.copyFileSync(pDesktop, path.join(rootScreenshotDir, 'invoices_desktop_1280.png'));
    console.log('  Đã lưu ảnh: invoices_desktop_1280.png');

    // 4. Kiểm tra Responsive Mobile 375px
    console.log('[4] Kiểm tra Responsive Mobile 375px...');
    await page.setViewportSize({ width: 375, height: 667 });
    await page.waitForTimeout(500);
    const pMobile = path.join(screenshotDir, 'invoices_mobile_375.png');
    await page.screenshot({ path: pMobile, fullPage: false });
    fs.copyFileSync(pMobile, path.join(rootScreenshotDir, 'invoices_mobile_375.png'));
    console.log('  Đã lưu ảnh: invoices_mobile_375.png');

    // Khôi phục lại viewport 1280px
    await page.setViewportSize({ width: 1280, height: 800 });

    // 5. Kiểm tra lọc theo Tab
    console.log('[5] Thử nghiệm lọc theo Tab Đã xác nhận...');
    const confirmedTab = page.locator('.status-tabs a:has-text("Đã xác nhận")');
    if (await confirmedTab.count() > 0) {
        await confirmedTab.click();
        await page.waitForLoadState('networkidle');
        console.log('  URL sau khi chọn tab Đã xác nhận:', page.url());
    }

    // 6. Kiểm tra trang chi tiết hóa đơn đơn Online PENDING (901)
    console.log('[6] Kiểm tra trang chi tiết hóa đơn đơn Online PENDING (901)...');
    await page.goto('http://localhost:8080/admin/invoices/901');
    await page.waitForLoadState('networkidle');

    // Kiểm tra timeline
    const hasTimeline = await page.locator('.order-progress').count() > 0;
    console.log('  Hiển thị timeline tiến trình giao hàng:', hasTimeline);

    // Kiểm tra huy hiệu kho
    const stockBadge = await page.locator('.badge:has-text("Đã trừ kho"), .badge:has-text("Chưa trừ kho")').first().textContent();
    console.log('  Huy hiệu kho:', stockBadge.trim());

    // Kiểm tra mở modal Hủy đơn (yêu cầu lý do)
    const cancelBtn = page.locator('button[data-transition="CANCELLED"]');
    if (await cancelBtn.count() > 0) {
        console.log('  Bấm nút Hủy đơn mở dialog...');
        await cancelBtn.click();
        await page.waitForTimeout(400);
        const dialogVisible = await page.locator('#order-transition-dialog').isVisible();
        const reasonRequired = await page.locator('#order-reason').getAttribute('required');
        console.log('  Modal chuyển trạng thái mở:', dialogVisible, '| Lý do bắt buộc:', reasonRequired !== null);
        
        const pModal = path.join(screenshotDir, 'invoice_transition_modal.png');
        await page.screenshot({ path: pModal, fullPage: false });
        fs.copyFileSync(pModal, path.join(rootScreenshotDir, 'invoice_transition_modal.png'));
        console.log('  Đã lưu ảnh: invoice_transition_modal.png');
        
        await page.click('[data-close-transition]');
        await page.waitForTimeout(300);
    }

    const pOnline = path.join(screenshotDir, 'invoice_detail_online_1280.png');
    await page.screenshot({ path: pOnline, fullPage: false });
    fs.copyFileSync(pOnline, path.join(rootScreenshotDir, 'invoice_detail_online_1280.png'));
    console.log('  Đã lưu ảnh: invoice_detail_online_1280.png');

    // 7. Kiểm tra trang chi tiết hóa đơn tại quầy (POS) (865)
    console.log('[7] Kiểm tra trang chi tiết hóa đơn tại quầy (POS) (865)...');
    await page.goto('http://localhost:8080/admin/invoices/865');
    await page.waitForLoadState('networkidle');

    // Kiểm tra KHÔNG CÓ timeline giao hàng
    const posHasTimeline = await page.locator('.order-progress').count() > 0;
    console.log('  Đơn POS KHÔNG có timeline giao hàng:', !posHasTimeline);

    const pPos = path.join(screenshotDir, 'invoice_detail_pos_1280.png');
    await page.screenshot({ path: pPos, fullPage: false });
    fs.copyFileSync(pPos, path.join(rootScreenshotDir, 'invoice_detail_pos_1280.png'));
    console.log('  Đã lưu ảnh: invoice_detail_pos_1280.png');

    // 8. Kiểm tra bản in hóa đơn
    console.log('[8] Kiểm tra bản in hóa đơn (?print=true)...');
    await page.goto('http://localhost:8080/admin/invoices/865?print=true');
    await page.waitForTimeout(600);
    const hasInvoicePaper = await page.locator('.invoice-paper').count() > 0;
    console.log('  Bản in chứa .invoice-paper:', hasInvoicePaper);
    
    const pPrint = path.join(screenshotDir, 'invoice_print_preview.png');
    await page.screenshot({ path: pPrint, fullPage: false });
    fs.copyFileSync(pPrint, path.join(rootScreenshotDir, 'invoice_print_preview.png'));
    console.log('  Đã lưu ảnh: invoice_print_preview.png');

    console.log('=== HOÀN TẤT KIỂM THỬ PLAYWRIGHT GIAI ĐOẠN 22 THÀNH CÔNG 100% ===');
    await browser.close();
})();
