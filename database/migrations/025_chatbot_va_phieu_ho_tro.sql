-- Migration 025: Chatbot FAQ, Support Tickets, and Chat Logs
-- Giai đoạn 25

-- 1. Bảng chatbot_faq
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'chatbot_faq')
BEGIN
    CREATE TABLE chatbot_faq (
        id INT IDENTITY(1,1) PRIMARY KEY,
        ma NVARCHAR(50) NOT NULL UNIQUE,
        cau_hoi NVARCHAR(300) NOT NULL,
        tu_khoa NVARCHAR(300) NULL,
        tra_loi NVARCHAR(1500) NOT NULL,
        hien_thi BIT NOT NULL DEFAULT 1
    );

    -- Seed FAQ dựa trên chính sách cửa hàng hiện có
    INSERT INTO chatbot_faq (ma, cau_hoi, tu_khoa, tra_loi, hien_thi)
    VALUES
    (N'KB001', N'Chính sách đổi trả hàng như thế nào?', N'đổi trả, doi tra, hoàn tiền, hoan tien, trả hàng, tra hang, bảo hành, bao hanh', N'F6S Store hỗ trợ đổi trả trong vòng 7 ngày kể từ khi nhận hàng với điều kiện giày còn nguyên tem mác, hộp và chưa qua sử dụng.', 1),
    (N'KB002', N'Thời gian và chính sách giao hàng ra sao?', N'giao hàng, giao hang, vận chuyển, van chuyen, ship, phí ship, phi ship, bao lâu nhận', N'F6S Store giao hàng toàn quốc qua các đơn vị vận chuyển uy tín. Thời gian giao từ 2-4 ngày làm việc. Phí vận chuyển do bên thứ ba thu theo biểu phí thực tế.', 1),
    (N'KB003', N'Cửa hàng hỗ trợ những phương thức thanh toán nào?', N'thanh toán, thanh toan, cod, chuyển khoản, chuyen khoan, quét mã, qr, tiền mặt, tien mat', N'Cửa hàng hỗ trợ thanh toán khi nhận hàng (COD), chuyển khoản ngân hàng qua VietQR tự động, và thanh toán tiền mặt/kết hợp trực tiếp tại quầy.', 1),
    (N'KB004', N'Chính sách bảo mật thông tin khách hàng?', N'bảo mật, bao mat, thông tin, thong tin, quyền riêng tư, quyen rieng tu', N'Chúng tôi cam kết bảo mật thông tin cá nhân của bạn. Dữ liệu chỉ dùng để xử lý đơn hàng và liên hệ hỗ trợ khi cần thiết.', 1),
    (N'KB005', N'Làm thế nào để đặt mua hàng trực tuyến?', N'mua hàng, mua hang, đặt hàng, dat hang, cách mua, cach mua, hướng dẫn, huong dan', N'Bạn chỉ cần chọn sản phẩm ưng ý, chọn size và màu sắc, bấm Thêm vào giỏ và tiến hành Đặt hàng tại trang thanh toán.', 1);
END;

-- 2. Bảng phieu_ho_tro (Support Tickets)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'phieu_ho_tro')
BEGIN
    CREATE TABLE phieu_ho_tro (
        id INT IDENTITY(1,1) PRIMARY KEY,
        ma NVARCHAR(50) NOT NULL UNIQUE,
        khach_id INT NULL FOREIGN KEY REFERENCES customers(id),
        ho_ten NVARCHAR(100) NOT NULL,
        lien_he NVARCHAR(100) NOT NULL,
        noi_dung NVARCHAR(1000) NOT NULL,
        trich_doan_chat NVARCHAR(MAX) NULL,
        trang_thai NVARCHAR(50) NOT NULL DEFAULT N'Mới',
        created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        xu_ly_boi INT NULL FOREIGN KEY REFERENCES employees(id),
        tra_loi NVARCHAR(MAX) NULL,
        tra_loi_luc DATETIME2 NULL
    );

    CREATE INDEX ix_phieu_ho_tro_trang_thai ON phieu_ho_tro(trang_thai);
    CREATE INDEX ix_phieu_ho_tro_created_at ON phieu_ho_tro(created_at);
END;

-- 3. Bảng chat_hoi_thoai (Chat Conversation Sessions)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'chat_hoi_thoai')
BEGIN
    CREATE TABLE chat_hoi_thoai (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        session_id NVARCHAR(100) NOT NULL UNIQUE,
        khach_id INT NULL FOREIGN KEY REFERENCES customers(id),
        ip_address NVARCHAR(50) NULL,
        created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        updated_at DATETIME2 NOT NULL DEFAULT GETDATE()
    );

    CREATE INDEX ix_chat_hoi_thoai_created_at ON chat_hoi_thoai(created_at);
END;

-- 4. Bảng chat_tin_nhan (Chat Messages)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'chat_tin_nhan')
BEGIN
    CREATE TABLE chat_tin_nhan (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        hoi_thoai_id BIGINT NOT NULL FOREIGN KEY REFERENCES chat_hoi_thoai(id) ON DELETE CASCADE,
        sender NVARCHAR(20) NOT NULL,
        noi_dung NVARCHAR(2000) NOT NULL,
        payload_json NVARCHAR(MAX) NULL,
        created_at DATETIME2 NOT NULL DEFAULT GETDATE()
    );

    CREATE INDEX ix_chat_tin_nhan_hoi_thoai ON chat_tin_nhan(hoi_thoai_id);
    CREATE INDEX ix_chat_tin_nhan_created_at ON chat_tin_nhan(created_at);
END;
