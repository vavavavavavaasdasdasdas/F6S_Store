-- Script xóa sạch dữ liệu nghiệp vụ: sản phẩm, nhân viên, khách hàng, phiếu giảm giá, đợt giảm giá, đơn hàng
-- Giữ lại 2 tài khoản login: 'admin' (ROLE_ADMIN) và 'user' (ROLE_USER)
-- Giữ lại danh mục thuộc tính gốc (thương hiệu, đế, màu, chất liệu, kích cỡ, địa giới hành chính)

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET NOCOUNT ON;

BEGIN TRANSACTION;

BEGIN TRY
    PRINT N'1. Xóa dữ liệu giữ chỗ POS và chi tiết đơn...';
    IF OBJECT_ID('pos_held_items', 'U') IS NOT NULL DELETE FROM pos_held_items;
    IF OBJECT_ID('pos_held_orders', 'U') IS NOT NULL DELETE FROM pos_held_orders;

    PRINT N'2. Xóa chi tiết đơn hàng, lịch sử trạng thái, mail log và hóa đơn...';
    IF OBJECT_ID('order_details', 'U') IS NOT NULL DELETE FROM order_details;
    IF OBJECT_ID('order_status_history', 'U') IS NOT NULL DELETE FROM order_status_history;
    IF OBJECT_ID('order_mail_log', 'U') IS NOT NULL DELETE FROM order_mail_log;
    IF OBJECT_ID('invoices', 'U') IS NOT NULL DELETE FROM invoices;
    IF OBJECT_ID('orders', 'U') IS NOT NULL DELETE FROM orders;

    PRINT N'3. Xóa lịch sử kho, đánh giá, yêu thích...';
    IF OBJECT_ID('inventory_logs', 'U') IS NOT NULL DELETE FROM inventory_logs;
    IF OBJECT_ID('danh_gia', 'U') IS NOT NULL DELETE FROM danh_gia;
    IF OBJECT_ID('yeu_thich', 'U') IS NOT NULL DELETE FROM yeu_thich;

    PRINT N'4. Xóa phiếu giảm giá và đợt giảm giá...';
    IF OBJECT_ID('promotion_details', 'U') IS NOT NULL DELETE FROM promotion_details;
    IF OBJECT_ID('promotions', 'U') IS NOT NULL DELETE FROM promotions;
    IF OBJECT_ID('vouchers', 'U') IS NOT NULL DELETE FROM vouchers;

    PRINT N'5. Xóa biến thể sản phẩm, ảnh sản phẩm và sản phẩm...';
    IF OBJECT_ID('product_images', 'U') IS NOT NULL DELETE FROM product_images;
    IF OBJECT_ID('product_variants', 'U') IS NOT NULL DELETE FROM product_variants;
    IF OBJECT_ID('products', 'U') IS NOT NULL DELETE FROM products;

    PRINT N'6. Xóa dữ liệu chat và phiếu hỗ trợ...';
    IF OBJECT_ID('chat_tin_nhan', 'U') IS NOT NULL DELETE FROM chat_tin_nhan;
    IF OBJECT_ID('chat_action_audit', 'U') IS NOT NULL DELETE FROM chat_action_audit;
    IF OBJECT_ID('chat_mail_log', 'U') IS NOT NULL DELETE FROM chat_mail_log;
    IF OBJECT_ID('phieu_ho_tro', 'U') IS NOT NULL DELETE FROM phieu_ho_tro;
    IF OBJECT_ID('chat_hoi_thoai', 'U') IS NOT NULL DELETE FROM chat_hoi_thoai;
    IF OBJECT_ID('admin_chat_tin_nhan', 'U') IS NOT NULL DELETE FROM admin_chat_tin_nhan;
    IF OBJECT_ID('admin_chat_audit', 'U') IS NOT NULL DELETE FROM admin_chat_audit;
    IF OBJECT_ID('admin_chat_hoi_thoai', 'U') IS NOT NULL DELETE FROM admin_chat_hoi_thoai;

    PRINT N'7. Xóa sổ địa chỉ khách hàng...';
    IF OBJECT_ID('dia_chi_nhan_hang', 'U') IS NOT NULL DELETE FROM dia_chi_nhan_hang;
    IF OBJECT_ID('customer_addresses', 'U') IS NOT NULL DELETE FROM customer_addresses;

    PRINT N'8. Xóa khách hàng, giữ lại khách hàng gắn với tài khoản user (id=2)...';
    IF OBJECT_ID('customers', 'U') IS NOT NULL 
    BEGIN
        DELETE FROM customers WHERE user_id IS NULL OR user_id NOT IN (SELECT id FROM users WHERE username = 'user');
        UPDATE customers 
        SET full_name = N'Khách Hàng Mẫu', 
            phone = '0900000001', 
            email = 'user@example.com',
            customer_code = 'KH00001'
        WHERE user_id IN (SELECT id FROM users WHERE username = 'user');
    END;

    PRINT N'9. Xóa nhân viên, giữ lại đúng nhân viên gắn với tài khoản admin (id=1)...';
    IF OBJECT_ID('employees', 'U') IS NOT NULL 
    BEGIN
        DELETE FROM employees WHERE user_id IS NULL OR user_id NOT IN (SELECT id FROM users WHERE username = 'admin');
        UPDATE employees
        SET full_name = N'Quản Trị Viên',
            phone = '0900000000',
            email = 'admin@example.com',
            employee_code = 'NV00001',
            status = 1
        WHERE user_id IN (SELECT id FROM users WHERE username = 'admin');
    END;

    PRINT N'10. Xóa tất cả tài khoản users ngoại trừ admin và user...';
    IF OBJECT_ID('users', 'U') IS NOT NULL 
    BEGIN
        DELETE FROM users WHERE username NOT IN ('admin', 'user');
        UPDATE users SET status = 1 WHERE username IN ('admin', 'user');
    END;

    COMMIT TRANSACTION;
    PRINT N'==> XÓA SẠCH DỮ LIỆU THÀNH CÔNG!';
END TRY
BEGIN CATCH
    ROLLBACK TRANSACTION;
    PRINT N'LỖI KHI XÓA DỮ LIỆU: ' + ERROR_MESSAGE();
    THROW;
END CATCH;
