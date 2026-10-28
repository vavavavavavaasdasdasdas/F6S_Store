-- =====================================================================
-- FILE KHỞI TẠO TOÀN BỘ DATABASE: shopbangiay_thu (21 BẢNG + DỮ LIỆU MẪU)
-- Hệ thống bán giày thể thao F6S Store
--
-- Đã gộp đầy đủ:
--   1. Schema chuẩn 21 bảng:
--      - roles, users, customers, dia_chi_nhan_hang, employees
--      - categories (thương hiệu), sizes, chat_lieu, de_giay
--      - products, product_images, product_variants, inventory_logs
--      - vouchers, promotions, promotion_details
--      - orders, order_details, invoices
--      - pos_held_orders, pos_held_items
--      kèm đầy đủ khóa ngoại, unique index, check constraint, unicode NVARCHAR.
--   2. Dữ liệu nền & dữ liệu mẫu đầy đủ:
--      - 6 thương hiệu, 8 kích cỡ, 8 chất liệu, 8 loại đế giày
--      - 15 sản phẩm (SP001..SP015) đầy đủ danh mục & đế giày
--      - 61 biến thể sản phẩm có số lượng và giá bán
--      - 60 lịch sử nhập kho ban đầu
--      - 8 khách hàng mẫu (KH001..KH008) kèm sổ địa chỉ nhận hàng mặc định
--      - 5 phiếu giảm giá (vouchers), 2 đợt khuyến mãi (promotions)
--   3. Tài khoản đăng nhập mẫu:
--      - Khách hàng (ROLE_USER): khach01 ... khach08 (Mật khẩu: Khach@123)
--      - Quản trị viên (ROLE_ADMIN): DataInitializer tự động tạo admin / admin123 khi chạy app.
--
-- Cách chạy:
--   Mở file này trong SSMS (SQL Server Management Studio) và nhấn F5 (Execute).
--   Script tự động tạo mới (hoặc làm mới nếu đã có) database shopbangiay_thu.
-- =====================================================================
SET NOCOUNT ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET ARITHABORT ON;
SET NUMERIC_ROUNDABORT OFF;
GO

USE master;
GO

IF DB_ID(N'shopbangiay_thu') IS NOT NULL
BEGIN
    ALTER DATABASE shopbangiay_thu SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE shopbangiay_thu;
END
GO

CREATE DATABASE shopbangiay_thu;
GO

USE shopbangiay_thu;
GO

SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET ARITHABORT ON;
SET NUMERIC_ROUNDABORT OFF;
SET XACT_ABORT ON;
GO

-- =====================================================================
-- PHẦN 1: TẠO SCHEMA 21 BẢNG & DỮ LIỆU NỀN
-- =====================================================================
BEGIN TRY
    BEGIN TRANSACTION;

    -- 1. Roles (vai trò)
    CREATE TABLE roles (
        id INT IDENTITY(1,1) PRIMARY KEY,
        name VARCHAR(50) NOT NULL UNIQUE
    );

    -- 2. Users (tài khoản đăng nhập)
    CREATE TABLE users (
        id INT IDENTITY(1,1) PRIMARY KEY,
        username VARCHAR(50) NOT NULL UNIQUE,
        password VARCHAR(255) NOT NULL,
        role_id INT NOT NULL FOREIGN KEY REFERENCES roles(id),
        full_name NVARCHAR(255) NULL,
        email NVARCHAR(100) NULL,
        phone NVARCHAR(20) NULL,
        created_at DATETIME2 NULL,
        status BIT DEFAULT 1
    );

    -- 3. Customers (khách hàng)
    CREATE TABLE customers (
        id INT IDENTITY(1,1) PRIMARY KEY,
        customer_code VARCHAR(20) UNIQUE,
        full_name NVARCHAR(100) NOT NULL,
        phone NVARCHAR(20) UNIQUE,
        email NVARCHAR(100),
        address NVARCHAR(255),
        user_id INT FOREIGN KEY REFERENCES users(id),
        created_at DATETIME DEFAULT GETDATE(),
        status BIT DEFAULT 1
    );
    CREATE UNIQUE INDEX UX_customers_email ON customers(email)
        WHERE email IS NOT NULL AND email <> N'';

    -- 4. Sổ địa chỉ nhận hàng (tối đa 3 địa chỉ / khách hàng)
    CREATE TABLE dia_chi_nhan_hang (
        id INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_dia_chi_nhan_hang PRIMARY KEY,
        customer_id INT NOT NULL,
        ten_nguoi_nhan NVARCHAR(100) NOT NULL,
        so_dien_thoai VARCHAR(20) NOT NULL,
        dia_chi NVARCHAR(255) NOT NULL,
        la_mac_dinh BIT NOT NULL CONSTRAINT DF_dia_chi_mac_dinh DEFAULT 0,
        thu_tu TINYINT NOT NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT DF_dia_chi_created_at DEFAULT SYSDATETIME(),
        CONSTRAINT FK_dia_chi_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE,
        CONSTRAINT CK_dia_chi_thu_tu CHECK (thu_tu BETWEEN 1 AND 3),
        CONSTRAINT UQ_dia_chi_customer_slot UNIQUE (customer_id, thu_tu)
    );
    CREATE UNIQUE INDEX UX_dia_chi_customer_default ON dia_chi_nhan_hang(customer_id)
        WHERE la_mac_dinh = 1;

    -- 5. Employees (nhân viên)
    CREATE TABLE employees (
        id INT IDENTITY(1,1) PRIMARY KEY,
        employee_code VARCHAR(20) UNIQUE,
        full_name NVARCHAR(255) NOT NULL,
        phone NVARCHAR(20) UNIQUE,
        email NVARCHAR(100),
        address NVARCHAR(255),
        position NVARCHAR(100),
        user_id INT NOT NULL FOREIGN KEY REFERENCES users(id),
        status BIT DEFAULT 1
    );
    CREATE UNIQUE INDEX UX_employees_user ON employees(user_id);
    CREATE UNIQUE INDEX UX_employees_email ON employees(email)
        WHERE email IS NOT NULL AND email <> N'';

    -- 6. Categories (danh mục / thương hiệu)
    CREATE TABLE categories (
        id INT IDENTITY(1,1) PRIMARY KEY,
        name NVARCHAR(100) NOT NULL,
        status BIT DEFAULT 1
    );

    -- 7. Sizes (kích cỡ)
    CREATE TABLE sizes (
        id INT IDENTITY(1,1) PRIMARY KEY,
        size_value NVARCHAR(10) NOT NULL UNIQUE
    );

    -- 8. Chất liệu
    CREATE TABLE chat_lieu (
        id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        ten NVARCHAR(100) NOT NULL UNIQUE,
        trang_thai BIT NOT NULL DEFAULT 1
    );

    -- 9. Đế giày
    CREATE TABLE de_giay (
        id INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_de_giay PRIMARY KEY,
        ten NVARCHAR(100) NOT NULL,
        trang_thai BIT NOT NULL CONSTRAINT DF_de_giay_trang_thai DEFAULT 1
    );
    CREATE UNIQUE INDEX UX_de_giay_ten ON de_giay(ten);

    -- 10. Products (sản phẩm)
    CREATE TABLE products (
        id INT IDENTITY(1,1) PRIMARY KEY,
        code VARCHAR(50) UNIQUE,
        name NVARCHAR(255) NOT NULL,
        description NVARCHAR(MAX),
        category_id INT FOREIGN KEY REFERENCES categories(id),
        de_giay_id INT NULL FOREIGN KEY REFERENCES de_giay(id),
        image_url NVARCHAR(500),
        status BIT DEFAULT 1
    );
    CREATE INDEX IX_products_de_giay ON products(de_giay_id);

    -- 11. Ảnh sản phẩm
    CREATE TABLE product_images (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        product_id INT NOT NULL,
        duong_dan NVARCHAR(2048) NOT NULL,
        thu_tu INT NOT NULL CHECK (thu_tu >= 0),
        la_anh_chinh BIT NOT NULL DEFAULT 0,
        CONSTRAINT FK_product_images_product FOREIGN KEY (product_id)
            REFERENCES products(id) ON DELETE CASCADE
    );
    CREATE INDEX IX_product_images_order ON product_images(product_id, thu_tu, id);
    CREATE UNIQUE INDEX UX_product_images_main ON product_images(product_id)
        WHERE la_anh_chinh = 1;

    -- 12. Product Variants (biến thể sản phẩm)
    CREATE TABLE product_variants (
        id INT IDENTITY(1,1) PRIMARY KEY,
        product_id INT FOREIGN KEY REFERENCES products(id),
        size_id INT FOREIGN KEY REFERENCES sizes(id),
        chat_lieu_id INT NOT NULL,
        price DECIMAL(18,2) NOT NULL,
        quantity INT NOT NULL DEFAULT 0,
        status BIT DEFAULT 1,
        CONSTRAINT FK_product_variants_chat_lieu FOREIGN KEY (chat_lieu_id) REFERENCES chat_lieu(id),
        CONSTRAINT CK_variant_price_positive CHECK (price > 0),
        CONSTRAINT CK_variant_quantity_nonnegative CHECK (quantity >= 0)
    );
    CREATE UNIQUE INDEX UX_product_variants_product_size_material
        ON product_variants(product_id, size_id, chat_lieu_id);

    -- 13. Lịch sử tồn kho
    CREATE TABLE inventory_logs (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        variant_id INT NOT NULL REFERENCES product_variants(id),
        quantity_change INT NOT NULL,
        quantity_before INT NOT NULL CHECK (quantity_before >= 0),
        quantity_after INT NOT NULL CHECK (quantity_after >= 0),
        reason NVARCHAR(500) NOT NULL,
        actor NVARCHAR(255) NOT NULL,
        created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT CK_inventory_log_delta CHECK (quantity_after = quantity_before + quantity_change)
    );
    CREATE INDEX IX_inventory_logs_variant_time
        ON inventory_logs(variant_id, created_at DESC, id DESC);

    -- 14. Vouchers (phiếu giảm giá)
    CREATE TABLE vouchers (
        id INT IDENTITY(1,1) PRIMARY KEY,
        code VARCHAR(50) UNIQUE NOT NULL,
        name NVARCHAR(100) NOT NULL,
        discount_type VARCHAR(20) NOT NULL, -- PERCENT, FIXED
        discount_value DECIMAL(18,2) NOT NULL,
        min_order_value DECIMAL(18,2) DEFAULT 0,
        max_discount DECIMAL(18,2),
        start_date DATETIME NOT NULL,
        end_date DATETIME NOT NULL,
        usage_limit INT DEFAULT 100,
        used_count INT DEFAULT 0,
        is_public BIT DEFAULT 1,
        customer_id INT FOREIGN KEY REFERENCES customers(id),
        status BIT DEFAULT 1
    );

    -- 15. Promotions (đợt giảm giá)
    CREATE TABLE promotions (
        id INT IDENTITY(1,1) PRIMARY KEY,
        name NVARCHAR(100) NOT NULL,
        discount_type VARCHAR(20) NOT NULL, -- PERCENT, FIXED
        discount_value DECIMAL(18,2) NOT NULL,
        start_date DATETIME NOT NULL,
        end_date DATETIME NOT NULL,
        status BIT DEFAULT 1
    );

    -- 16. Promotion Details (sản phẩm áp dụng đợt giảm giá)
    CREATE TABLE promotion_details (
        id INT IDENTITY(1,1) PRIMARY KEY,
        promotion_id INT FOREIGN KEY REFERENCES promotions(id),
        product_id INT FOREIGN KEY REFERENCES products(id)
    );
    CREATE UNIQUE INDEX UX_promotion_details_product
        ON promotion_details(promotion_id, product_id);

    -- 17. Orders (đơn hàng)
    CREATE TABLE orders (
        id INT IDENTITY(1,1) PRIMARY KEY,
        order_code VARCHAR(50) UNIQUE NOT NULL,
        customer_id INT FOREIGN KEY REFERENCES customers(id),
        employee_id INT FOREIGN KEY REFERENCES employees(id),
        order_type VARCHAR(20) NOT NULL, -- ONLINE, POS
        status VARCHAR(50) NOT NULL, -- PENDING, CONFIRMED, PREPARING, SHIPPING, DELIVERED, CANCELLED
        total_amount DECIMAL(18,2) NOT NULL,
        discount_amount DECIMAL(18,2) DEFAULT 0,
        final_amount DECIMAL(18,2) NOT NULL,
        shipping_fee DECIMAL(18,2) NOT NULL DEFAULT 0,
        payment_status VARCHAR(50) NOT NULL DEFAULT 'UNPAID',
        receiver_name NVARCHAR(255),
        email NVARCHAR(100),
        payment_method VARCHAR(50), -- CASH, TRANSFER, COD, BANK_TRANSFER
        shipping_address NVARCHAR(255),
        phone_number NVARCHAR(20),
        voucher_id INT FOREIGN KEY REFERENCES vouchers(id),
        created_at DATETIME DEFAULT GETDATE(),
        updated_at DATETIME DEFAULT GETDATE(),
        da_tru_kho BIT NOT NULL CONSTRAINT DF_orders_da_tru_kho DEFAULT 0
    );

    -- 18. Order Details (chi tiết đơn hàng)
    CREATE TABLE order_details (
        id INT IDENTITY(1,1) PRIMARY KEY,
        order_id INT FOREIGN KEY REFERENCES orders(id),
        product_variant_id INT FOREIGN KEY REFERENCES product_variants(id),
        price DECIMAL(18,2) NOT NULL,
        quantity INT NOT NULL,
        total_price DECIMAL(18,2) NOT NULL,
        product_name NVARCHAR(255) NULL,
        size_name NVARCHAR(10) NULL,
        ten_chat_lieu NVARCHAR(100) NULL
    );

    -- 19. Invoices (hóa đơn)
    CREATE TABLE invoices (
        id INT IDENTITY(1,1) PRIMARY KEY,
        order_id INT NOT NULL UNIQUE REFERENCES orders(id),
        invoice_code VARCHAR(255) NOT NULL UNIQUE,
        issued_at DATETIME2 NOT NULL,
        customer_name NVARCHAR(255) NOT NULL,
        customer_phone NVARCHAR(255) NULL,
        employee_name NVARCHAR(255) NULL,
        cash_received DECIMAL(19,2) NULL,
        change_amount DECIMAL(19,2) NULL
    );

    -- 20. POS: Đơn tạm giữ
    CREATE TABLE pos_held_orders (
        id BIGINT IDENTITY PRIMARY KEY,
        code VARCHAR(40) NOT NULL UNIQUE,
        staff_user_id INT NOT NULL REFERENCES users(id),
        customer_id INT NULL REFERENCES customers(id),
        customer_label NVARCHAR(255) NULL,
        voucher_code NVARCHAR(50) NULL,
        payment_method VARCHAR(20) NOT NULL CHECK (payment_method IN ('CASH','TRANSFER')),
        note NVARCHAR(500) NULL,
        created_at DATETIME2 NOT NULL,
        expires_at DATETIME2 NOT NULL
    );
    CREATE INDEX ix_pos_hold_staff_expiry ON pos_held_orders(staff_user_id, expires_at);

    -- 21. POS: Mặt hàng trong đơn tạm giữ
    CREATE TABLE pos_held_items (
        id BIGINT IDENTITY PRIMARY KEY,
        held_id BIGINT NOT NULL REFERENCES pos_held_orders(id),
        variant_id INT NOT NULL REFERENCES product_variants(id),
        quantity INT NOT NULL CHECK (quantity > 0),
        CONSTRAINT uq_pos_hold_variant UNIQUE (held_id, variant_id)
    );

    -- Dữ liệu cơ sở nền tảng
    INSERT INTO roles(name) VALUES ('ROLE_ADMIN'), ('ROLE_USER');
    INSERT INTO categories(name) VALUES (N'Nike'), (N'Adidas'), (N'Converse');
    INSERT INTO sizes(size_value) VALUES (N'39'), (N'40'), (N'41');
    INSERT INTO chat_lieu(ten) VALUES
        (N'Da thật'), (N'Da tổng hợp'), (N'Da lộn'), (N'Vải canvas'),
        (N'Vải lưới'), (N'Vải dệt kim'), (N'Cao su'), (N'Khác');
    INSERT INTO de_giay(ten) VALUES
        (N'Cao su'), (N'EVA'), (N'PU'), (N'TPU'),
        (N'Đế bằng'), (N'Đế độn'), (N'Đế chống trượt'), (N'Khác');

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO

-- =====================================================================
-- PHẦN 2: DỮ LIỆU ĐẦY ĐỦ (THƯƠNG HIỆU, SIZE, SẢN PHẨM, BIẾN THỂ, KHÁCH HÀNG, VOUCHER, KHUYẾN MÃI)
-- =====================================================================
BEGIN TRY
    BEGIN TRANSACTION;

    -- 1. Danh mục / Thương hiệu bổ sung
    INSERT INTO categories(name)
    SELECT v.name FROM (VALUES (N'Vans'), (N'Puma'), (N'New Balance')) v(name)
    WHERE NOT EXISTS (SELECT 1 FROM categories c WHERE c.name = v.name);

    -- 2. Size bổ sung
    INSERT INTO sizes(size_value)
    SELECT v.s FROM (VALUES (N'36'), (N'37'), (N'38'), (N'42'), (N'43')) v(s)
    WHERE NOT EXISTS (SELECT 1 FROM sizes z WHERE z.size_value = v.s);

    -- 3. 15 Sản phẩm mẫu (gán chuẩn thương hiệu và đế giày)
    DECLARE @sp TABLE (code VARCHAR(50), ten NVARCHAR(255), mo_ta NVARCHAR(MAX), danh_muc NVARCHAR(100), de_giay NVARCHAR(100));
    INSERT INTO @sp(code, ten, mo_ta, danh_muc, de_giay) VALUES
        ('SP001', N'Nike Air Force 1', N'Giày thể thao Nike Air Force 1 trắng cổ điển', N'Nike', N'Đế bằng'),
        ('SP002', N'Adidas Ultraboost', N'Giày chạy bộ Adidas Ultraboost êm ái', N'Adidas', N'Cao su'),
        ('SP003', N'Converse Chuck Taylor', N'Giày Converse cổ cao kinh điển', N'Converse', N'Cao su'),
        ('SP004', N'Nike Air Max 90', N'Giày Nike Air Max 90 đệm khí, phong cách retro', N'Nike', N'PU'),
        ('SP005', N'Nike Dunk Low', N'Giày Nike Dunk Low phối màu trẻ trung', N'Nike', N'Đế bằng'),
        ('SP006', N'Nike Pegasus 40', N'Giày chạy bộ Nike Pegasus 40 thoáng khí', N'Nike', N'EVA'),
        ('SP007', N'Adidas Stan Smith', N'Giày Adidas Stan Smith trắng gót xanh', N'Adidas', N'Đế bằng'),
        ('SP008', N'Adidas Gazelle', N'Giày Adidas Gazelle da lộn thanh lịch', N'Adidas', N'Cao su'),
        ('SP009', N'Converse Chuck 70', N'Giày Converse Chuck 70 cổ thấp vải dệt cao cấp', N'Converse', N'Cao su'),
        ('SP010', N'Vans Old Skool', N'Giày Vans Old Skool sọc Jazz cá tính', N'Vans', N'Đế bằng'),
        ('SP011', N'Vans Authentic', N'Giày Vans Authentic thiết kế tối giản', N'Vans', N'Đế bằng'),
        ('SP012', N'Puma Suede Classic', N'Giày Puma Suede Classic da lộn êm ái', N'Puma', N'Cao su'),
        ('SP013', N'Puma RS-X', N'Giày Puma RS-X phong cách thể thao hầm hố', N'Puma', N'PU'),
        ('SP014', N'New Balance 574', N'Giày New Balance 574 phối màu vintage', N'New Balance', N'EVA'),
        ('SP015', N'New Balance 530', N'Giày New Balance 530 phong cách running retro', N'New Balance', N'EVA');

    INSERT INTO products(code, name, description, category_id, de_giay_id)
    SELECT s.code, s.ten, s.mo_ta,
           (SELECT TOP 1 c.id FROM categories c WHERE c.name = s.danh_muc ORDER BY c.id),
           (SELECT TOP 1 d.id FROM de_giay d WHERE d.ten = s.de_giay ORDER BY d.id)
    FROM @sp s
    WHERE NOT EXISTS (SELECT 1 FROM products p WHERE p.code = s.code);

    -- 4. 61 Biến thể sản phẩm (Kích cỡ + Chất liệu + Giá + Tồn kho)
    DECLARE @bt TABLE (code VARCHAR(50), size_value NVARCHAR(10), chat_lieu NVARCHAR(100), gia DECIMAL(18,2), sl INT);
    INSERT INTO @bt(code, size_value, chat_lieu, gia, sl) VALUES
        ('SP001', N'39', N'Da thật', 2500000, 25),
        ('SP001', N'40', N'Da thật', 2500000, 30),
        ('SP001', N'41', N'Da thật', 2500000, 20),
        ('SP001', N'42', N'Da thật', 2500000, 15),
        ('SP002', N'39', N'Vải dệt kim', 3500000, 10),
        ('SP002', N'40', N'Vải dệt kim', 3500000, 20),
        ('SP002', N'41', N'Vải dệt kim', 3500000, 15),
        ('SP002', N'42', N'Vải dệt kim', 3500000, 8),
        ('SP003', N'39', N'Vải canvas', 1500000, 100),
        ('SP003', N'40', N'Vải canvas', 1500000, 80),
        ('SP003', N'41', N'Vải canvas', 1500000, 60),
        ('SP004', N'40', N'Da tổng hợp', 3200000, 12),
        ('SP004', N'41', N'Da tổng hợp', 3200000, 10),
        ('SP004', N'42', N'Da tổng hợp', 3200000, 6),
        ('SP004', N'43', N'Da tổng hợp', 3200000, 3),
        ('SP005', N'39', N'Da thật', 2900000, 14),
        ('SP005', N'40', N'Da thật', 2900000, 18),
        ('SP005', N'41', N'Da thật', 2900000, 9),
        ('SP005', N'42', N'Da thật', 2900000, 0),
        ('SP006', N'40', N'Vải lưới', 2700000, 22),
        ('SP006', N'41', N'Vải lưới', 2700000, 25),
        ('SP006', N'42', N'Vải lưới', 2700000, 18),
        ('SP006', N'43', N'Vải lưới', 2700000, 5),
        ('SP007', N'38', N'Da tổng hợp', 2200000, 10),
        ('SP007', N'39', N'Da tổng hợp', 2200000, 16),
        ('SP007', N'40', N'Da tổng hợp', 2200000, 20),
        ('SP007', N'41', N'Da tổng hợp', 2200000, 12),
        ('SP008', N'39', N'Da lộn', 2400000, 8),
        ('SP008', N'40', N'Da lộn', 2400000, 11),
        ('SP008', N'41', N'Da lộn', 2400000, 7),
        ('SP008', N'42', N'Da lộn', 2400000, 4),
        ('SP009', N'38', N'Vải canvas', 1900000, 20),
        ('SP009', N'39', N'Vải canvas', 1900000, 25),
        ('SP009', N'40', N'Vải canvas', 1900000, 30),
        ('SP009', N'41', N'Vải canvas', 1900000, 22),
        ('SP010', N'38', N'Vải canvas', 1800000, 15),
        ('SP010', N'39', N'Vải canvas', 1800000, 20),
        ('SP010', N'40', N'Vải canvas', 1800000, 25),
        ('SP010', N'41', N'Vải canvas', 1800000, 18),
        ('SP010', N'42', N'Vải canvas', 1800000, 10),
        ('SP011', N'37', N'Vải canvas', 1500000, 12),
        ('SP011', N'38', N'Vải canvas', 1500000, 18),
        ('SP011', N'39', N'Vải canvas', 1500000, 22),
        ('SP011', N'40', N'Vải canvas', 1500000, 16),
        ('SP012', N'39', N'Da lộn', 1700000, 9),
        ('SP012', N'40', N'Da lộn', 1700000, 13),
        ('SP012', N'41', N'Da lộn', 1700000, 10),
        ('SP012', N'42', N'Da lộn', 1700000, 6),
        ('SP013', N'40', N'Vải lưới', 2300000, 10),
        ('SP013', N'41', N'Vải lưới', 2300000, 12),
        ('SP013', N'42', N'Vải lưới', 2300000, 8),
        ('SP013', N'43', N'Vải lưới', 2300000, 2),
        ('SP014', N'38', N'Da lộn', 2100000, 7),
        ('SP014', N'39', N'Da lộn', 2100000, 12),
        ('SP014', N'40', N'Da lộn', 2100000, 15),
        ('SP014', N'41', N'Da lộn', 2100000, 10),
        ('SP014', N'42', N'Da lộn', 2100000, 5),
        ('SP015', N'39', N'Vải lưới', 2600000, 11),
        ('SP015', N'40', N'Vải lưới', 2600000, 14),
        ('SP015', N'41', N'Vải lưới', 2600000, 9),
        ('SP015', N'42', N'Vải lưới', 2600000, 3);

    DECLARE @moi TABLE (id INT, sl INT);

    INSERT INTO product_variants(product_id, size_id, chat_lieu_id, price, quantity)
    OUTPUT inserted.id, inserted.quantity INTO @moi(id, sl)
    SELECT p.id, z.id, m.id, b.gia, b.sl
    FROM @bt b
    JOIN products p  ON p.code = b.code
    JOIN sizes z     ON z.size_value = b.size_value
    JOIN chat_lieu m ON m.ten = b.chat_lieu
    WHERE NOT EXISTS (SELECT 1 FROM product_variants v
                      WHERE v.product_id = p.id AND v.size_id = z.id AND v.chat_lieu_id = m.id);

    -- 5. Lịch sử nhập kho ban đầu
    INSERT INTO inventory_logs(variant_id, quantity_change, quantity_before, quantity_after, reason, actor)
    SELECT id, sl, 0, sl, N'Nhập kho ban đầu (dữ liệu mẫu)', N'he_thong'
    FROM @moi WHERE sl > 0;

    -- 6. Khách hàng mẫu (KH001 .. KH008)
    DECLARE @kh TABLE (ma VARCHAR(20), ten NVARCHAR(100), sdt NVARCHAR(20), email NVARCHAR(100), dia_chi NVARCHAR(255));
    INSERT INTO @kh(ma, ten, sdt, email, dia_chi) VALUES
        ('KH001', N'Nguyễn Văn An', N'0900000001', N'khach01@example.com', N'12 Nguyễn Trãi, Thanh Xuân, Hà Nội'),
        ('KH002', N'Trần Thị Bình', N'0900000002', N'khach02@example.com', N'25 Lê Lợi, Quận 1, TP. Hồ Chí Minh'),
        ('KH003', N'Lê Hoàng Cường', N'0900000003', N'khach03@example.com', N'8 Trần Phú, Hải Châu, Đà Nẵng'),
        ('KH004', N'Phạm Thu Dung', N'0900000004', N'khach04@example.com', N'40 Lạch Tray, Ngô Quyền, Hải Phòng'),
        ('KH005', N'Hoàng Minh Đức', N'0900000005', N'khach05@example.com', N'15 Nguyễn Văn Cừ, Ninh Kiều, Cần Thơ'),
        ('KH006', N'Vũ Thị Hà', N'0900000006', N'khach06@example.com', N'30 Lê Duẩn, TP. Huế'),
        ('KH007', N'Đặng Quốc Huy', N'0900000007', N'khach07@example.com', N'22 Trần Phú, TP. Nha Trang'),
        ('KH008', N'Bùi Ngọc Lan', N'0900000008', N'khach08@example.com', N'5 Cầu Giấy, Hà Nội');

    INSERT INTO customers(customer_code, full_name, phone, email, address)
    SELECT k.ma, k.ten, k.sdt, k.email, k.dia_chi
    FROM @kh k
    WHERE NOT EXISTS (SELECT 1 FROM customers c WHERE c.customer_code = k.ma OR c.phone = k.sdt OR c.email = k.email);

    -- 7. Sổ địa chỉ nhận hàng mặc định cho khách hàng mẫu
    INSERT INTO dia_chi_nhan_hang(customer_id, ten_nguoi_nhan, so_dien_thoai, dia_chi, la_mac_dinh, thu_tu, created_at)
    SELECT c.id, c.full_name, COALESCE(c.phone, ''), c.address, 1, 1, COALESCE(c.created_at, SYSDATETIME())
    FROM customers c
    WHERE NULLIF(LTRIM(RTRIM(c.address)), N'') IS NOT NULL
      AND NOT EXISTS (SELECT 1 FROM dia_chi_nhan_hang a WHERE a.customer_id = c.id);

    -- 8. Phiếu giảm giá (5 voucher)
    INSERT INTO vouchers(code, name, discount_type, discount_value, min_order_value, max_discount, start_date, end_date, usage_limit, is_public, customer_id)
    SELECT v.code, v.name, v.loai, v.gia_tri, v.don_toi_thieu, v.giam_toi_da, '20260101', '20301231', v.gioi_han, v.cong_khai,
           CASE WHEN v.cong_khai = 0 THEN (SELECT TOP 1 c.id FROM customers c WHERE c.customer_code = 'KH001') END
    FROM (VALUES
        ('WELCOME10',   N'Giảm 10% khách mới',       'PERCENT', 10,     500000,  200000, 1000, 1),
        ('GIAM50K',     N'Giảm 50K đơn từ 1 triệu',  'FIXED',   50000,  1000000, NULL,   500,  1),
        ('SALE20',      N'Giảm 20% đơn từ 1,5 triệu','PERCENT', 20,    1500000, 300000, 200,  1),
        ('VIP100K',     N'Giảm 100K đơn từ 2 triệu', 'FIXED',   100000, 2000000, NULL,   100,  1),
        ('TRIAN-KH001', N'Voucher tri ân KH001',     'PERCENT', 15,     500000,  250000, 1,    0)
    ) v(code, name, loai, gia_tri, don_toi_thieu, giam_toi_da, gioi_han, cong_khai)
    WHERE NOT EXISTS (SELECT 1 FROM vouchers x WHERE x.code = v.code);

    -- 9. Đợt giảm giá (2 chương trình khuyến mãi)
    INSERT INTO promotions(name, discount_type, discount_value, start_date, end_date)
    SELECT v.name, 'PERCENT', v.gia_tri, '20260101', '20301231'
    FROM (VALUES
        (N'Khuyến mãi mùa hè', 15),
        (N'Sale Converse và Vans', 10)
    ) v(name, gia_tri)
    WHERE NOT EXISTS (SELECT 1 FROM promotions p WHERE p.name = v.name);

    INSERT INTO promotion_details(promotion_id, product_id)
    SELECT pr.id, p.id
    FROM (VALUES
        (N'Khuyến mãi mùa hè', 'SP001'), (N'Khuyến mãi mùa hè', 'SP002'),
        (N'Sale Converse và Vans', 'SP003'), (N'Sale Converse và Vans', 'SP009'),
        (N'Sale Converse và Vans', 'SP010'), (N'Sale Converse và Vans', 'SP011')
    ) v(ten_km, code)
    JOIN promotions pr ON pr.name = v.ten_km
    JOIN products p    ON p.code = v.code
    WHERE NOT EXISTS (SELECT 1 FROM promotion_details d WHERE d.promotion_id = pr.id AND d.product_id = p.id);

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO

-- =====================================================================
-- PHẦN 3: TÀI KHOẢN KHÁCH HÀNG MẪU (khach01 ... khach08 / Mật khẩu: Khach@123)
-- =====================================================================
BEGIN TRY
    BEGIN TRANSACTION;

    DECLARE @tk TABLE (ma_kh VARCHAR(20), username VARCHAR(50), mat_khau VARCHAR(255),
                       ho_ten NVARCHAR(255), sdt NVARCHAR(20), email NVARCHAR(100));
    INSERT INTO @tk(ma_kh, username, mat_khau, ho_ten, sdt, email) VALUES
        ('KH001', 'khach01', '$2a$10$NXEWj5sArxkZI9NcANgyr.7M7oUCdDKZ68jB4x4AvIkwNOiXrzsSC', N'Nguyễn Văn An', N'0900000001', N'khach01@example.com'),
        ('KH002', 'khach02', '$2a$10$b9zrENrU8CAq.iB5VlWODO.hw6WbhuDG26xyfyhqDlH69CKepdNFG', N'Trần Thị Bình', N'0900000002', N'khach02@example.com'),
        ('KH003', 'khach03', '$2a$10$Jo496qvnULdbd7EUYe34Xuk47pGMOgU8EhtZF3PUsRKWDkGjeag8u', N'Lê Hoàng Cường', N'0900000003', N'khach03@example.com'),
        ('KH004', 'khach04', '$2a$10$pSqe4qF40UugRHXoo14Gp.A3.NrQUsSoRdlt.UNG4MB8BZ5uy5pWy', N'Phạm Thu Dung', N'0900000004', N'khach04@example.com'),
        ('KH005', 'khach05', '$2a$10$uHGt3HURvvs/Xs97SV7Nj.VErgEkqBOFf5nv0GEoUwy9Ueauqmgly', N'Hoàng Minh Đức', N'0900000005', N'khach05@example.com'),
        ('KH006', 'khach06', '$2a$10$IvruL0nnfJomOzxQjygdsOrkas03D5TMkLjEz5WtkHx6F6NPUSvR6', N'Vũ Thị Hà', N'0900000006', N'khach06@example.com'),
        ('KH007', 'khach07', '$2a$10$FE2UjOE6NGBLLnNHS8FLkOX416luvVycfAsdi3VWpoGK/lOVzCgH.', N'Đặng Quốc Huy', N'0900000007', N'khach07@example.com'),
        ('KH008', 'khach08', '$2a$10$AOZRrh3J62jAGvUXUfDGZO7NayDstUbOCxbqfnMEnLMy4rWARR.xe', N'Bùi Ngọc Lan', N'0900000008', N'khach08@example.com');

    INSERT INTO users(username, password, role_id, full_name, email, phone, created_at, status)
    SELECT t.username, t.mat_khau,
           (SELECT TOP 1 r.id FROM roles r WHERE r.name = 'ROLE_USER'),
           t.ho_ten, t.email, t.sdt, SYSDATETIME(), 1
    FROM @tk t
    WHERE EXISTS (SELECT 1 FROM customers c WHERE c.customer_code = t.ma_kh)
      AND NOT EXISTS (SELECT 1 FROM users u WHERE u.username = t.username);

    UPDATE c SET c.user_id = u.id
    FROM customers c
    JOIN @tk t   ON t.ma_kh = c.customer_code
    JOIN users u ON u.username = t.username
    WHERE c.user_id IS NULL;

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO

-- =====================================================================
-- KIỂM TRA TỔNG QUAN KẾT QUẢ KHỞI TẠO (21 BẢNG)
-- =====================================================================
SELECT DB_NAME() AS database_name, COUNT(*) AS tong_so_bang
FROM sys.tables WHERE schema_id = SCHEMA_ID(N'dbo');

SELECT N'categories' AS ten_bang, COUNT(*) AS so_ban_ghi FROM categories
UNION ALL SELECT N'sizes', COUNT(*) FROM sizes
UNION ALL SELECT N'chat_lieu', COUNT(*) FROM chat_lieu
UNION ALL SELECT N'de_giay', COUNT(*) FROM de_giay
UNION ALL SELECT N'products', COUNT(*) FROM products
UNION ALL SELECT N'product_variants', COUNT(*) FROM product_variants
UNION ALL SELECT N'inventory_logs', COUNT(*) FROM inventory_logs
UNION ALL SELECT N'customers', COUNT(*) FROM customers
UNION ALL SELECT N'dia_chi_nhan_hang', COUNT(*) FROM dia_chi_nhan_hang
UNION ALL SELECT N'users', COUNT(*) FROM users
UNION ALL SELECT N'vouchers', COUNT(*) FROM vouchers
UNION ALL SELECT N'promotions', COUNT(*) FROM promotions
UNION ALL SELECT N'promotion_details', COUNT(*) FROM promotion_details;
GO
