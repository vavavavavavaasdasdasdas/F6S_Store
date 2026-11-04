-- Migration 026: Trợ lý cửa hàng trong trang Admin (Admin Chatbot & Audit)
-- Giai đoạn 26

-- 1. Thêm cột doi_tuong vào bảng chatbot_faq (KHACH / NOI_BO)
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('chatbot_faq') AND name = 'doi_tuong')
BEGIN
    ALTER TABLE chatbot_faq ADD doi_tuong NVARCHAR(20) NOT NULL CONSTRAINT df_chatbot_faq_doi_tuong DEFAULT N'KHACH';
END;
GO

-- 2. Seed hướng dẫn thao tác nội bộ dựa trên chức năng thật của hệ thống
IF NOT EXISTS (SELECT * FROM chatbot_faq WHERE ma = 'KB_ADMIN_01')
BEGIN
    INSERT INTO chatbot_faq (ma, cau_hoi, tu_khoa, tra_loi, hien_thi, doi_tuong)
    VALUES
    (N'KB_ADMIN_01', N'Hướng dẫn thao tác bán hàng tại quầy (POS)', 
     N'pos, ban hang tai quay, tao don pos, hoa don cho, que the, quet ma, vietqr, giao hang pos', 
     N'Để bán hàng tại quầy: 1) Truy cập menu Bán hàng tại quầy (/admin/pos); 2) Chọn hóa đơn chờ hoặc tạo mới (tối đa 5 hóa đơn chờ); 3) Chọn khách hàng hoặc dùng Khách lẻ mặc định; 4) Bấm nút "Thêm sản phẩm" (+) để chọn giày và số lượng (tồn kho giảm ngay); 5) Chọn phương thức thanh toán: Tiền mặt, Chuyển khoản (VietQR tự động) hoặc gạt công tắc "Giao hàng" nếu khách muốn ship hàng online; 6) Bấm Hoàn tất đơn hàng.', 
     1, N'NOI_BO'),

    (N'KB_ADMIN_02', N'Hướng dẫn xử lý đơn hàng và hóa đơn', 
     N'don hang, hoa don, xu ly don, xac nhan don, in hoa don, giao hang, huy don, invoices', 
     N'Để quản lý đơn hàng: 1) Vào menu Quản lý hóa đơn (/admin/invoices); 2) Sử dụng các tab trạng thái (Chờ xử lý, Đã xác nhận, Đang chuẩn bị, Đang giao, Đã giao, Đã hủy) hoặc thanh lọc từ khóa/kênh bán; 3) Bấm "Xem chi tiết" để xem hóa đơn; 4) Tiến hành "Xác nhận đơn", chuyển trạng thái hoặc bấm "In hóa đơn" để in phiếu giao hàng cho khách.', 
     1, N'NOI_BO'),

    (N'KB_ADMIN_03', N'Hướng dẫn kiểm tra tồn kho và nhập hàng', 
     N'nhap hang, ton kho, kiem kho, dieu chinh ton, kho, inventory, bao ton', 
     N'Để kiểm tra và cập nhật tồn kho: 1) Truy cập Quản lý tồn kho (/admin/inventory); 2) Lọc theo thương hiệu, kích cỡ, màu sắc hoặc tìm theo mã sản phẩm/biến thể; 3) Bấm "Điều chỉnh tồn" để tăng/giảm số lượng thực tế kèm ghi chú lý do (Nhập hàng mới, Kiểm kê định kỳ, Hàng lỗi/xuất hủy); 4) Mọi lịch sử biến động đều được lưu vết trong Lịch sử kho.', 
     1, N'NOI_BO'),

    (N'KB_ADMIN_04', N'Hướng dẫn xử lý phiếu hỗ trợ khách hàng', 
     N'phieu ho tro, ho tro khach, support ticket, tra loi khach, khach can gap', 
     N'Để tiếp nhận và phản hồi phiếu hỗ trợ: 1) Vào menu Hỗ trợ > Phiếu hỗ trợ (/admin/support-tickets); 2) Lọc các phiếu trạng thái "Mới"; 3) Bấm "Xem chi tiết" để đọc nội dung và trích đoạn 10 tin nhắn gần nhất của khách với Chatbot; 4) Nhập nội dung giải đáp vào ô "Phản hồi" và bấm Gửi (hệ thống tự động gửi email thông báo cho khách nếu có email, hoặc ghi chú liên hệ SĐT).', 
     1, N'NOI_BO');
END;

-- 3. Bảng admin_chat_audit (Nhật ký truy vấn trợ lý nội bộ)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'admin_chat_audit')
BEGIN
    CREATE TABLE admin_chat_audit (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        username NVARCHAR(100) NOT NULL,
        role NVARCHAR(50) NOT NULL,
        thoi_gian DATETIME2 NOT NULL DEFAULT GETDATE(),
        cau_hoi_tom_tat NVARCHAR(500) NOT NULL,
        cong_cu_da_goi NVARCHAR(100) NOT NULL,
        session_id NVARCHAR(100) NULL
    );

    CREATE INDEX ix_admin_chat_audit_thoi_gian ON admin_chat_audit(thoi_gian);
END;

-- 4. Bảng hội thoại và tin nhắn nội bộ của Admin/Nhân viên
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'admin_chat_hoi_thoai')
BEGIN
    CREATE TABLE admin_chat_hoi_thoai (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        session_id NVARCHAR(100) NOT NULL UNIQUE,
        username NVARCHAR(100) NOT NULL,
        created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        updated_at DATETIME2 NOT NULL DEFAULT GETDATE()
    );

    CREATE INDEX ix_admin_chat_hoi_thoai_created ON admin_chat_hoi_thoai(created_at);
END;

IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'admin_chat_tin_nhan')
BEGIN
    CREATE TABLE admin_chat_tin_nhan (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        conversation_id BIGINT NOT NULL FOREIGN KEY REFERENCES admin_chat_hoi_thoai(id) ON DELETE CASCADE,
        sender NVARCHAR(20) NOT NULL,
        noi_dung NVARCHAR(MAX) NOT NULL,
        created_at DATETIME2 NOT NULL DEFAULT GETDATE()
    );

    CREATE INDEX ix_admin_chat_tin_nhan_created ON admin_chat_tin_nhan(created_at);
END;
