-- Khởi tạo schema mới trong database đã chọn bằng sqlcmd -d; không tạo/chuyển database.
IF DB_NAME() NOT IN (N'shopbangiay_kt',N'shop_verify',N'shop_fresh',N'shopbangiay_thu')
   AND NOT (DB_NAME()=N'shopbangiay' AND COALESCE(TRY_CONVERT(INT,SESSION_CONTEXT(N'shop.allow-production-migration')),0)=1)
    AND DB_NAME() NOT LIKE N'shopbangiay[_]kt[_]%'
    THROW 51000,N'Database không được phép chạy script.',1;
IF OBJECT_ID(N'dbo.users',N'U') IS NOT NULL THROW 51001,N'Database đã có schema; hãy dùng migrations.',1;
GO
-- 1. Roles
CREATE TABLE roles (
    id INT IDENTITY(1,1) PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

-- 2. Users (Accounts)
CREATE TABLE users (
    id INT IDENTITY(1,1) PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role_id INT NOT NULL FOREIGN KEY REFERENCES roles(id),
    full_name NVARCHAR(255),
    email NVARCHAR(100),
    phone NVARCHAR(20),
    created_at DATETIME2,
    status BIT DEFAULT 1
);

-- 3. Customers
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

-- 4. Employees
CREATE TABLE employees (
    id INT IDENTITY(1,1) PRIMARY KEY,
    employee_code VARCHAR(20) UNIQUE,
    full_name NVARCHAR(100) NOT NULL,
    phone NVARCHAR(20) UNIQUE,
    email NVARCHAR(100),
    address NVARCHAR(255),
    position NVARCHAR(100),
    user_id INT FOREIGN KEY REFERENCES users(id),
    status BIT DEFAULT 1
);

-- 5. Categories
CREATE TABLE categories (
    id INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100) NOT NULL,
    status BIT DEFAULT 1
);

-- 6. Sizes
CREATE TABLE sizes (
    id INT IDENTITY(1,1) PRIMARY KEY,
    size_value NVARCHAR(10) NOT NULL UNIQUE
);

-- 7. Products
CREATE TABLE products (
    id INT IDENTITY(1,1) PRIMARY KEY,
    code VARCHAR(50) UNIQUE,
    name NVARCHAR(255) NOT NULL,
    description NVARCHAR(MAX),
    category_id INT FOREIGN KEY REFERENCES categories(id),
    image_url NVARCHAR(500),
    status BIT DEFAULT 1
);

-- 8. Product Variants
CREATE TABLE product_variants (
    id INT IDENTITY(1,1) PRIMARY KEY,
    product_id INT FOREIGN KEY REFERENCES products(id),
    size_id INT FOREIGN KEY REFERENCES sizes(id),
    price DECIMAL(18,2) NOT NULL,
    quantity INT NOT NULL DEFAULT 0,
    status BIT DEFAULT 1
);

-- 9. Vouchers
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
    customer_id INT FOREIGN KEY REFERENCES customers(id), -- For private vouchers
    status BIT DEFAULT 1
);

-- 10. Promotions
CREATE TABLE promotions (
    id INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100) NOT NULL,
    discount_type VARCHAR(20) NOT NULL, -- PERCENT, FIXED
    discount_value DECIMAL(18,2) NOT NULL,
    start_date DATETIME NOT NULL,
    end_date DATETIME NOT NULL,
    status BIT DEFAULT 1
);

-- 11. Promotion Details
CREATE TABLE promotion_details (
    id INT IDENTITY(1,1) PRIMARY KEY,
    promotion_id INT FOREIGN KEY REFERENCES promotions(id),
    product_id INT FOREIGN KEY REFERENCES products(id)
);

-- 12. Orders (Invoices)
CREATE TABLE orders (
    id INT IDENTITY(1,1) PRIMARY KEY,
    order_code VARCHAR(50) UNIQUE NOT NULL,
    customer_id INT FOREIGN KEY REFERENCES customers(id),
    employee_id INT FOREIGN KEY REFERENCES employees(id), -- Null if online order
    order_type VARCHAR(20) NOT NULL, -- ONLINE, POS
    status VARCHAR(50) NOT NULL, -- PENDING, CONFIRMED, PREPARING, SHIPPING, DELIVERED, CANCELLED
    total_amount DECIMAL(18,2) NOT NULL,
    discount_amount DECIMAL(18,2) DEFAULT 0,
    final_amount DECIMAL(18,2) NOT NULL,
    shipping_fee DECIMAL(18,2) NOT NULL DEFAULT 0,
    payment_status VARCHAR(50) NOT NULL DEFAULT 'UNPAID',
    receiver_name NVARCHAR(255),
    email NVARCHAR(100),
    payment_method VARCHAR(50), -- CASH, TRANSFER
    shipping_address NVARCHAR(255),
    phone_number NVARCHAR(20),
    voucher_id INT FOREIGN KEY REFERENCES vouchers(id),
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE()
);

-- 13. Order Details
CREATE TABLE order_details (
    id INT IDENTITY(1,1) PRIMARY KEY,
    order_id INT FOREIGN KEY REFERENCES orders(id),
    product_variant_id INT FOREIGN KEY REFERENCES product_variants(id),
    price DECIMAL(18,2) NOT NULL,
    quantity INT NOT NULL,
    total_price DECIMAL(18,2) NOT NULL
);


INSERT roles(name) VALUES('ROLE_ADMIN'),('ROLE_USER');
INSERT categories(name) VALUES(N'Nike'),(N'Adidas'),(N'Converse');
INSERT sizes(size_value) VALUES(N'39'),(N'40'),(N'41');
-- DataInitializer tạo admin với PasswordEncoder; không seed khách/sản phẩm/đơn hoặc hash mẫu.
GO

-- database/phase6.sql
IF DB_NAME() NOT IN (N'shopbangiay_kt',N'shop_verify',N'shop_fresh',N'shopbangiay_thu')
   AND NOT (DB_NAME()=N'shopbangiay' AND COALESCE(TRY_CONVERT(INT,SESSION_CONTEXT(N'shop.allow-production-migration')),0)=1)
    AND DB_NAME() NOT LIKE N'shopbangiay[_]kt[_]%'
    THROW 51000,N'Database không được phép chạy script.',1;
-- Phase 6: additive/idempotent migration. Run against the existing shopbangiay database.
-- Does not drop tables or alter prices/stock/order totals.
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF EXISTS (SELECT 1 FROM product_variants GROUP BY product_id,size_id HAVING COUNT(*)>1)
    AND DB_NAME() NOT LIKE N'shopbangiay[_]kt[_]%'
    THROW 51000, 'Duplicate product/size pairs must be resolved before Phase 6 migration.', 1;
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='UX_product_variants_product_size' AND object_id=OBJECT_ID('product_variants'))
    CREATE UNIQUE INDEX UX_product_variants_product_size ON product_variants(product_id,size_id);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name='CK_variant_price_positive')
    ALTER TABLE product_variants WITH CHECK ADD CONSTRAINT CK_variant_price_positive CHECK(price>0);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name='CK_variant_quantity_nonnegative')
    ALTER TABLE product_variants WITH CHECK ADD CONSTRAINT CK_variant_quantity_nonnegative CHECK(quantity>=0);
IF OBJECT_ID('inventory_logs','U') IS NULL
BEGIN
    CREATE TABLE inventory_logs (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        variant_id INT NOT NULL REFERENCES product_variants(id),
        quantity_change INT NOT NULL,
        quantity_before INT NOT NULL CHECK(quantity_before>=0),
        quantity_after INT NOT NULL CHECK(quantity_after>=0),
        reason NVARCHAR(500) NOT NULL,
        actor NVARCHAR(255) NOT NULL,
        created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT CK_inventory_log_delta CHECK(quantity_after=quantity_before+quantity_change)
    );
    CREATE INDEX IX_inventory_logs_variant_time ON inventory_logs(variant_id,created_at DESC,id DESC);
END;
IF COL_LENGTH('order_details','product_name') IS NULL
    ALTER TABLE order_details ADD product_name NVARCHAR(255) NULL;
IF COL_LENGTH('order_details','size_name') IS NULL
    ALTER TABLE order_details ADD size_name NVARCHAR(10) NULL;
-- Dynamic SQL allows the new snapshot columns to be used in the same migration batch.
EXEC sp_executesql N'UPDATE d SET product_name=COALESCE(d.product_name,p.name),size_name=COALESCE(d.size_name,s.size_value) FROM order_details d JOIN product_variants v ON v.id=d.product_variant_id JOIN products p ON p.id=v.product_id JOIN sizes s ON s.id=v.size_id WHERE d.product_name IS NULL OR d.size_name IS NULL';
COMMIT TRANSACTION;


GO

-- database/phase7.sql
IF DB_NAME() NOT IN (N'shopbangiay_kt',N'shop_verify',N'shop_fresh',N'shopbangiay_thu')
   AND NOT (DB_NAME()=N'shopbangiay' AND COALESCE(TRY_CONVERT(INT,SESSION_CONTEXT(N'shop.allow-production-migration')),0)=1)
    AND DB_NAME() NOT LIKE N'shopbangiay[_]kt[_]%'
    THROW 51000,N'Database không được phép chạy script.',1;
-- Phase 7: additive account/profile integrity. Run on the existing shopbangiay DB.
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF EXISTS (SELECT 1 FROM employees WHERE user_id IS NULL)
    AND DB_NAME() NOT LIKE N'shopbangiay[_]kt[_]%'
    THROW 51000, 'Employee without User: resolve the existing identity before migration.', 1;
IF EXISTS (SELECT 1 FROM employees GROUP BY user_id HAVING COUNT(*)>1)
    THROW 51001, 'Multiple Employees reference one User.', 1;
IF EXISTS (SELECT 1 FROM users u JOIN roles r ON r.id=u.role_id WHERE r.name='ROLE_ADMIN' AND NOT EXISTS(SELECT 1 FROM employees e WHERE e.user_id=u.id))
    THROW 51002, 'ROLE_ADMIN User without Employee: resolve before migration.', 1;
IF EXISTS (SELECT 1 FROM customers WHERE email IS NOT NULL AND email<>'' GROUP BY LOWER(email) HAVING COUNT(*)>1)
    THROW 51003, 'Duplicate customer email: resolve before migration.', 1;
IF EXISTS (SELECT 1 FROM employees WHERE email IS NOT NULL AND email<>'' GROUP BY LOWER(email) HAVING COUNT(*)>1)
    THROW 51004, 'Duplicate employee email: resolve before migration.', 1;
ALTER TABLE employees ALTER COLUMN user_id INT NOT NULL;
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('employees') AND name='UX_employees_user_phase7')
    CREATE UNIQUE INDEX UX_employees_user_phase7 ON employees(user_id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('employees') AND name='UX_employees_email_phase7')
    CREATE UNIQUE INDEX UX_employees_email_phase7 ON employees(email) WHERE email IS NOT NULL AND email<>'';
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('customers') AND name='UX_customers_email_phase7')
    CREATE UNIQUE INDEX UX_customers_email_phase7 ON customers(email) WHERE email IS NOT NULL AND email<>'';
COMMIT TRANSACTION;

GO

-- database/phase8.sql
IF DB_NAME() NOT IN (N'shopbangiay_kt',N'shop_verify',N'shop_fresh',N'shopbangiay_thu')
   AND NOT (DB_NAME()=N'shopbangiay' AND COALESCE(TRY_CONVERT(INT,SESSION_CONTEXT(N'shop.allow-production-migration')),0)=1)
    AND DB_NAME() NOT LIKE N'shopbangiay[_]kt[_]%'
    THROW 51000,N'Database không được phép chạy script.',1;
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF EXISTS (SELECT 1 FROM promotion_details GROUP BY promotion_id,product_id HAVING COUNT(*)>1)
    THROW 51008, 'Duplicate promotion/product links: resolve before Phase 8 migration.', 1;
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='UX_promotion_details_product_phase8' AND object_id=OBJECT_ID('promotion_details'))
    CREATE UNIQUE INDEX UX_promotion_details_product_phase8 ON promotion_details(promotion_id,product_id);
COMMIT TRANSACTION;

GO

-- database/phase9.sql
IF DB_NAME() NOT IN (N'shopbangiay_kt',N'shop_verify',N'shop_fresh',N'shopbangiay_thu')
   AND NOT (DB_NAME()=N'shopbangiay' AND COALESCE(TRY_CONVERT(INT,SESSION_CONTEXT(N'shop.allow-production-migration')),0)=1)
    AND DB_NAME() NOT LIKE N'shopbangiay[_]kt[_]%'
    THROW 51000,N'Database không được phép chạy script.',1;
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF OBJECT_ID(N'dbo.invoices', N'U') IS NULL
CREATE TABLE dbo.invoices (
    id INT IDENTITY(1,1) PRIMARY KEY,
    order_id INT NOT NULL UNIQUE REFERENCES dbo.orders(id),
    invoice_code VARCHAR(255) NOT NULL UNIQUE,
    issued_at DATETIME2 NOT NULL,
    customer_name NVARCHAR(255) NOT NULL,
    customer_phone NVARCHAR(255) NULL,
    employee_name NVARCHAR(255) NULL,
    cash_received DECIMAL(19,2) NULL,
    change_amount DECIMAL(19,2) NULL
);
INSERT INTO dbo.invoices(order_id,invoice_code,issued_at,customer_name,customer_phone,employee_name)
SELECT o.id, 'HD'+RIGHT('000000'+CAST(o.id AS VARCHAR(20)),6), COALESCE(o.created_at,SYSDATETIME()),
       COALESCE(c.full_name,o.receiver_name,N'Khách lẻ'),COALESCE(c.phone,o.phone_number),
       COALESCE(e.full_name,CASE WHEN o.order_type='ONLINE' THEN N'Đơn online' ELSE N'Không lưu người bán' END)
FROM dbo.orders o LEFT JOIN dbo.customers c ON c.id=o.customer_id LEFT JOIN dbo.employees e ON e.id=o.employee_id
WHERE NOT EXISTS (SELECT 1 FROM dbo.invoices i WHERE i.order_id=o.id);
COMMIT;

GO

-- database/admin-account-pos-hold.sql
IF DB_NAME() NOT IN (N'shopbangiay_kt',N'shop_verify',N'shop_fresh',N'shopbangiay_thu')
   AND NOT (DB_NAME()=N'shopbangiay' AND COALESCE(TRY_CONVERT(INT,SESSION_CONTEXT(N'shop.allow-production-migration')),0)=1)
    AND DB_NAME() NOT LIKE N'shopbangiay[_]kt[_]%'
    THROW 51000,N'Database không được phép chạy script.',1;
-- Apply explicitly to the intended database. Verification uses shop_verify only.
IF COL_LENGTH('users','full_name') IS NULL ALTER TABLE users ADD full_name nvarchar(255) NULL;
IF COL_LENGTH('users','email') IS NULL ALTER TABLE users ADD email nvarchar(255) NULL;
IF COL_LENGTH('users','phone') IS NULL ALTER TABLE users ADD phone nvarchar(255) NULL;
IF COL_LENGTH('users','created_at') IS NULL ALTER TABLE users ADD created_at datetime2 NULL;
ALTER TABLE employees ALTER COLUMN full_name nvarchar(255) NOT NULL;
ALTER TABLE users ALTER COLUMN full_name nvarchar(255) NULL;
-- Historical creation dates are unknown: leave old records NULL rather than invent dates.
IF OBJECT_ID('pos_held_orders','U') IS NULL
CREATE TABLE pos_held_orders (
 id bigint IDENTITY PRIMARY KEY, code varchar(40) NOT NULL UNIQUE,
 staff_user_id int NOT NULL REFERENCES users(id), customer_id int NULL REFERENCES customers(id),
 customer_label nvarchar(255) NULL, voucher_code nvarchar(50) NULL,
 payment_method varchar(20) NOT NULL CHECK(payment_method IN ('CASH','TRANSFER')),
 note nvarchar(500) NULL, created_at datetime2 NOT NULL, expires_at datetime2 NOT NULL
);
IF OBJECT_ID('pos_held_items','U') IS NULL
CREATE TABLE pos_held_items (
 id bigint IDENTITY PRIMARY KEY, held_id bigint NOT NULL REFERENCES pos_held_orders(id),
 variant_id int NOT NULL REFERENCES product_variants(id), quantity int NOT NULL CHECK(quantity>0),
 CONSTRAINT uq_pos_hold_variant UNIQUE(held_id,variant_id)
);
ALTER TABLE pos_held_orders ALTER COLUMN customer_label nvarchar(255) NULL;
ALTER TABLE pos_held_orders ALTER COLUMN note nvarchar(500) NULL;
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE name='ix_pos_hold_staff_expiry' AND object_id=OBJECT_ID('pos_held_orders'))
CREATE INDEX ix_pos_hold_staff_expiry ON pos_held_orders(staff_user_id,expires_at);
-- Defaults: shop.timezone=Asia/Ho_Chi_Minh; shop.pos.hold.expiry-mode=END_OF_DAY;
-- shop.pos.hold.cleanup-interval=PT5M. HOURS_24 is the other supported expiry mode.

GO

-- database/migrations/004_anh_san_pham.sql
IF DB_NAME() NOT IN (N'shopbangiay_kt',N'shop_verify',N'shop_fresh',N'shopbangiay_thu')
   AND NOT (DB_NAME()=N'shopbangiay' AND COALESCE(TRY_CONVERT(INT,SESSION_CONTEXT(N'shop.allow-production-migration')),0)=1)
    AND DB_NAME() NOT LIKE N'shopbangiay[_]kt[_]%'
    THROW 51000,N'Database không được phép chạy script.',1;
SET NOCOUNT ON;
SET XACT_ABORT ON;
BEGIN TRY
 BEGIN TRANSACTION;
 IF OBJECT_ID(N'dbo.product_images',N'U') IS NULL
 BEGIN
  CREATE TABLE dbo.product_images (
   id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
   product_id INT NOT NULL,
   duong_dan NVARCHAR(2048) NOT NULL,
   thu_tu INT NOT NULL CHECK(thu_tu>=0),
   la_anh_chinh BIT NOT NULL DEFAULT 0,
   CONSTRAINT FK_product_images_product FOREIGN KEY(product_id) REFERENCES dbo.products(id) ON DELETE CASCADE
  );
  CREATE INDEX IX_product_images_order ON dbo.product_images(product_id,thu_tu,id);
  CREATE UNIQUE INDEX UX_product_images_main ON dbo.product_images(product_id) WHERE la_anh_chinh=1;
 END;
 INSERT INTO dbo.product_images(product_id,duong_dan,thu_tu,la_anh_chinh)
 SELECT p.id,p.image_url,0,1 FROM dbo.products p
 WHERE p.image_url IS NOT NULL AND LTRIM(RTRIM(p.image_url))<>N''
 AND NOT EXISTS(SELECT 1 FROM dbo.product_images i WHERE i.product_id=p.id);
 COMMIT TRANSACTION;
 SELECT N'COMMIT' ket_qua,DB_NAME() database_name,COUNT_BIG(*) image_count FROM dbo.product_images;
END TRY
BEGIN CATCH
 IF XACT_STATE()<>0 ROLLBACK TRANSACTION;
 THROW;
END CATCH;

GO

-- database/migrations/005_chat_lieu.sql
IF DB_NAME() NOT IN (N'shopbangiay_kt',N'shop_verify',N'shop_fresh',N'shopbangiay_thu')
   AND NOT (DB_NAME()=N'shopbangiay' AND COALESCE(TRY_CONVERT(INT,SESSION_CONTEXT(N'shop.allow-production-migration')),0)=1)
    AND DB_NAME() NOT LIKE N'shopbangiay[_]kt[_]%'
    THROW 51000,N'Database không được phép chạy script.',1;
-- SQL Server; chạy trên bản sao kiểm thử trước. Không tự động chọn database.
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;
    IF OBJECT_ID(N'dbo.chat_lieu',N'U') IS NULL
        CREATE TABLE dbo.chat_lieu (
            id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
            ten NVARCHAR(100) NOT NULL UNIQUE,
            trang_thai BIT NOT NULL DEFAULT 1
        );
    INSERT INTO dbo.chat_lieu(ten,trang_thai)
        SELECT v.ten,1 FROM (VALUES (N'Da thật'),(N'Da tổng hợp'),(N'Da lộn'),
            (N'Vải canvas'),(N'Vải lưới'),(N'Vải dệt kim'),(N'Cao su'),(N'Khác')) v(ten)
        WHERE NOT EXISTS (SELECT 1 FROM dbo.chat_lieu m WHERE m.ten=v.ten);
    IF COL_LENGTH('dbo.product_variants','chat_lieu_id') IS NULL
        ALTER TABLE dbo.product_variants ADD chat_lieu_id INT NULL;
    EXEC sp_executesql N'UPDATE dbo.product_variants SET chat_lieu_id=(SELECT id FROM dbo.chat_lieu WHERE ten=N''Khác'') WHERE chat_lieu_id IS NULL;
        ALTER TABLE dbo.product_variants ALTER COLUMN chat_lieu_id INT NOT NULL;';
    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='FK_product_variants_chat_lieu')
        EXEC sp_executesql N'ALTER TABLE dbo.product_variants ADD CONSTRAINT FK_product_variants_chat_lieu FOREIGN KEY(chat_lieu_id) REFERENCES dbo.chat_lieu(id);';
    DECLARE @drop NVARCHAR(MAX)=N'';
    SELECT @drop=@drop+CASE WHEN i.is_unique_constraint=1 THEN N'ALTER TABLE dbo.product_variants DROP CONSTRAINT '+QUOTENAME(i.name)+N';'
        ELSE N'DROP INDEX '+QUOTENAME(i.name)+N' ON dbo.product_variants;' END
    FROM sys.indexes i WHERE i.object_id=OBJECT_ID(N'dbo.product_variants') AND i.is_unique=1 AND i.is_primary_key=0
        AND (SELECT COUNT(*) FROM sys.index_columns ic WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal>0)=2
        AND (SELECT COUNT(*) FROM sys.index_columns ic JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id
            WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal>0 AND c.name IN ('product_id','size_id'))=2;
    EXEC sp_executesql @drop;
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.product_variants') AND name='UX_product_variants_product_size_material')
        AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.product_variants') AND name='UX_variants_five_attributes')
        EXEC sp_executesql N'CREATE UNIQUE INDEX UX_product_variants_product_size_material ON dbo.product_variants(product_id,size_id,chat_lieu_id);';
    IF COL_LENGTH('dbo.order_details','ten_chat_lieu') IS NULL
        ALTER TABLE dbo.order_details ADD ten_chat_lieu NVARCHAR(100) NULL;
    EXEC sp_executesql N'UPDATE dbo.order_details SET ten_chat_lieu=N''Khác'' WHERE ten_chat_lieu IS NULL;';
    COMMIT;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT>0 ROLLBACK;
    THROW;
END CATCH;

GO

-- database/migrations/006_luat_ton_kho.sql
IF DB_NAME() NOT IN (N'shopbangiay_kt',N'shop_verify',N'shop_fresh',N'shopbangiay_thu')
   AND NOT (DB_NAME()=N'shopbangiay' AND COALESCE(TRY_CONVERT(INT,SESSION_CONTEXT(N'shop.allow-production-migration')),0)=1)
    AND DB_NAME() NOT LIKE N'shopbangiay[_]kt[_]%'
    THROW 51000,N'Database không được phép chạy script.',1;
-- SQL Server. Chạy sau migration 005 trên database đã được sao lưu.
-- Chỉ bù cờ một lần; chạy lại không thay đổi cờ của các đơn theo luật mới.
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;
    IF COL_LENGTH('dbo.orders','da_tru_kho') IS NULL
    BEGIN
        ALTER TABLE dbo.orders ADD da_tru_kho BIT NOT NULL
            CONSTRAINT DF_orders_da_tru_kho DEFAULT 0 WITH VALUES;
        EXEC sp_executesql N'UPDATE dbo.orders SET da_tru_kho=CASE WHEN status=''CANCELLED'' THEN 0 ELSE 1 END;';
    END;
    -- Lý do trừ/hoàn kho phải giữ nguyên tiếng Việt khi ghi lịch sử.
    IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id=OBJECT_ID('dbo.inventory_logs') AND name='reason' AND TYPE_NAME(user_type_id)<>'nvarchar')
        ALTER TABLE dbo.inventory_logs ALTER COLUMN reason NVARCHAR(500) NOT NULL;
    COMMIT;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT>0 ROLLBACK;
    THROW;
END CATCH;

GO

-- database/migrations/007_tieng_viet_unicode.sql
IF DB_NAME() NOT IN (N'shopbangiay_kt',N'shop_verify',N'shop_fresh',N'shopbangiay_thu')
   AND NOT (DB_NAME()=N'shopbangiay' AND COALESCE(TRY_CONVERT(INT,SESSION_CONTEXT(N'shop.allow-production-migration')),0)=1)
    AND DB_NAME() NOT LIKE N'shopbangiay[_]kt[_]%'
    THROW 51000,N'Database không được phép chạy script.',1;
-- SQL Server. Chạy sau 006, sau khi sao lưu database.
-- Chuyển các cột văn bản hiển thị sang NVARCHAR, giữ độ dài và NULL/NOT NULL.
-- Không thể khôi phục dấu đã mất thành '?' trước khi áp dụng migration.
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;
    DECLARE @table SYSNAME, @column SYSNAME, @length INT, @nullable BIT, @statement NVARCHAR(MAX);
    DECLARE text_columns CURSOR LOCAL FAST_FORWARD FOR
        SELECT t.name,c.name,c.max_length,c.is_nullable
        FROM sys.tables t
        JOIN sys.columns c ON c.object_id=t.object_id
        JOIN sys.types ty ON ty.user_type_id=c.user_type_id
        JOIN (VALUES
            (N'products',N'name'),(N'products',N'description'),
            (N'customers',N'full_name'),(N'customers',N'address'),
            (N'orders',N'receiver_name'),(N'orders',N'shipping_address'),
            (N'order_details',N'product_name'),(N'order_details',N'size_name'),
            (N'invoices',N'customer_name'),(N'invoices',N'employee_name'),
            (N'employees',N'full_name'),(N'employees',N'address'),(N'employees',N'position'),
            (N'categories',N'name'),(N'promotions',N'name'),(N'vouchers',N'name'),
            (N'users',N'full_name'),(N'inventory_logs',N'reason'),
            (N'pos_held_orders',N'customer_label'),(N'pos_held_orders',N'note')
        ) wanted(table_name,column_name) ON wanted.table_name=t.name AND wanted.column_name=c.name
        WHERE t.schema_id=SCHEMA_ID(N'dbo') AND ty.name=N'varchar';
    OPEN text_columns;
    FETCH NEXT FROM text_columns INTO @table,@column,@length,@nullable;
    WHILE @@FETCH_STATUS=0
    BEGIN
        SET @statement=N'ALTER TABLE dbo.'+QUOTENAME(@table)+N' ALTER COLUMN '+QUOTENAME(@column)
            +N' NVARCHAR('+CASE WHEN @length=-1 THEN N'MAX' ELSE CONVERT(NVARCHAR(10),@length) END+N') '
            +CASE WHEN @nullable=1 THEN N'NULL' ELSE N'NOT NULL' END+N';';
        EXEC sp_executesql @statement;
        FETCH NEXT FROM text_columns INTO @table,@column,@length,@nullable;
    END;
    CLOSE text_columns;
    DEALLOCATE text_columns;
    COMMIT;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT>0 ROLLBACK;
    THROW;
END CATCH;

GO

-- database/migrations/008_nvarchar_con_lai.sql
IF DB_NAME() NOT IN (N'shopbangiay_kt',N'shop_verify',N'shop_fresh',N'shopbangiay_thu')
   AND NOT (DB_NAME()=N'shopbangiay' AND COALESCE(TRY_CONVERT(INT,SESSION_CONTEXT(N'shop.allow-production-migration')),0)=1)
    AND DB_NAME() NOT LIKE N'shopbangiay[_]kt[_]%'
    THROW 51000,N'Database không được phép chạy script.',1;
-- Chỉ chạy trên database test; triển khai thật phải bật session context rõ ràng.
IF DB_NAME() NOT IN (N'shopbangiay_kt',N'shop_verify',N'shop_fresh',N'shopbangiay_thu')
   AND NOT (DB_NAME()=N'shopbangiay' AND COALESCE(TRY_CONVERT(INT,SESSION_CONTEXT(N'shop.allow-production-migration')),0)=1)
    AND DB_NAME() NOT LIKE N'shopbangiay[_]kt[_]%'
    THROW 51000,N'Database không được phép chạy migration.',1;
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;
    -- Giữ mã, enum, username/password băm và actor (username); còn lại dùng Unicode.
    SELECT t.object_id,t.name table_name,c.column_id,c.name column_name,
           c.max_length,c.is_nullable,ty.name type_name,c.collation_name
    INTO #unicode_columns
    FROM sys.tables t JOIN sys.columns c ON c.object_id=t.object_id
    JOIN sys.types ty ON ty.user_type_id=c.user_type_id
    WHERE t.schema_id=SCHEMA_ID(N'dbo') AND ty.name IN(N'varchar',N'char',N'text')
      AND c.name NOT IN (N'code',N'customer_code',N'employee_code',N'invoice_code',N'order_code',
          N'voucher_code',N'order_type',N'status',N'payment_method',N'payment_status',N'discount_type',
          N'username',N'password',N'actor',N'so_cccd',N'ma_mau')
      AND NOT (t.name=N'roles' AND c.name=N'name')
      AND NOT (t.name=N'dia_chi_nhan_hang' AND c.name=N'so_dien_thoai');

    -- Giữ nguyên tính duy nhất, thứ tự khóa, INCLUDE và bộ lọc của index phụ thuộc.
    SELECT DISTINCT i.object_id,i.index_id,t.name table_name,i.name index_name,
        CAST(CASE WHEN i.is_unique_constraint=1 THEN
            N'ALTER TABLE dbo.'+QUOTENAME(t.name)+N' DROP CONSTRAINT '+QUOTENAME(i.name)+N';'
            ELSE N'DROP INDEX '+QUOTENAME(i.name)+N' ON dbo.'+QUOTENAME(t.name)+N';' END AS NVARCHAR(MAX)) drop_sql,
        CAST(CASE WHEN i.is_unique_constraint=1 THEN
            N'ALTER TABLE dbo.'+QUOTENAME(t.name)+N' ADD CONSTRAINT '+QUOTENAME(i.name)+N' UNIQUE '
            ELSE N'CREATE '+CASE WHEN i.is_unique=1 THEN N'UNIQUE ' ELSE N'' END END
            +CASE WHEN i.type=1 THEN N'CLUSTERED ' ELSE N'NONCLUSTERED ' END
            +CASE WHEN i.is_unique_constraint=1 THEN N'' ELSE N'INDEX '+QUOTENAME(i.name)+N' ON dbo.'+QUOTENAME(t.name)+N' ' END
            +N'('+keys.cols+N')'
            +CASE WHEN included.cols IS NULL THEN N'' ELSE N' INCLUDE ('+included.cols+N')' END
            +CASE WHEN i.filter_definition IS NULL THEN N'' ELSE N' WHERE '+i.filter_definition END+N';' AS NVARCHAR(MAX)) create_sql
    INTO #unicode_indexes
    FROM sys.indexes i JOIN sys.tables t ON t.object_id=i.object_id
    CROSS APPLY (SELECT STRING_AGG(CAST(QUOTENAME(c.name)+CASE WHEN ic.is_descending_key=1 THEN N' DESC' ELSE N' ASC' END AS NVARCHAR(MAX)),N',') WITHIN GROUP(ORDER BY ic.key_ordinal) cols
        FROM sys.index_columns ic JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id
        WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal>0) keys
    OUTER APPLY (SELECT STRING_AGG(CAST(QUOTENAME(c.name) AS NVARCHAR(MAX)),N',') WITHIN GROUP(ORDER BY ic.index_column_id) cols
        FROM sys.index_columns ic JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id
        WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.is_included_column=1) included
    WHERE i.is_primary_key=0 AND i.type IN(1,2)
      AND EXISTS(SELECT 1 FROM sys.index_columns ic JOIN #unicode_columns u ON u.object_id=ic.object_id AND u.column_id=ic.column_id
                 WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id);

    DECLARE @statement NVARCHAR(MAX),@table SYSNAME,@column SYSNAME,@length INT,@nullable BIT,@type SYSNAME,@collation SYSNAME;
    DECLARE drop_indexes CURSOR LOCAL FAST_FORWARD FOR SELECT drop_sql FROM #unicode_indexes;
    OPEN drop_indexes; FETCH NEXT FROM drop_indexes INTO @statement;
    WHILE @@FETCH_STATUS=0 BEGIN EXEC sp_executesql @statement; FETCH NEXT FROM drop_indexes INTO @statement; END;
    CLOSE drop_indexes; DEALLOCATE drop_indexes;
    DECLARE columns_to_convert CURSOR LOCAL FAST_FORWARD FOR
        SELECT table_name,column_name,max_length,is_nullable,type_name,collation_name FROM #unicode_columns ORDER BY table_name,column_id;
    OPEN columns_to_convert; FETCH NEXT FROM columns_to_convert INTO @table,@column,@length,@nullable,@type,@collation;
    WHILE @@FETCH_STATUS=0
    BEGIN
        SET @statement=N'ALTER TABLE dbo.'+QUOTENAME(@table)+N' ALTER COLUMN '+QUOTENAME(@column)+N' NVARCHAR('
            +CASE WHEN @length=-1 OR @length>4000 OR @type=N'text' THEN N'MAX' ELSE CONVERT(NVARCHAR(10),@length) END+N')'
            +CASE WHEN @collation IS NULL THEN N'' ELSE N' COLLATE '+@collation END
            +CASE WHEN @nullable=1 THEN N' NULL;' ELSE N' NOT NULL;' END;
        EXEC sp_executesql @statement;
        FETCH NEXT FROM columns_to_convert INTO @table,@column,@length,@nullable,@type,@collation;
    END;
    CLOSE columns_to_convert; DEALLOCATE columns_to_convert;
    DECLARE create_indexes CURSOR LOCAL FAST_FORWARD FOR SELECT create_sql FROM #unicode_indexes;
    OPEN create_indexes; FETCH NEXT FROM create_indexes INTO @statement;
    WHILE @@FETCH_STATUS=0 BEGIN EXEC sp_executesql @statement; FETCH NEXT FROM create_indexes INTO @statement; END;
    CLOSE create_indexes; DEALLOCATE create_indexes;
    SELECT COUNT(*) converted_columns FROM #unicode_columns;
    DROP TABLE #unicode_indexes;
    DROP TABLE #unicode_columns;
    COMMIT;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT>0 ROLLBACK;
    THROW;
END CATCH;

GO

-- Migration 009: sổ địa chỉ nhận hàng.
GO
-- SQL Server. Chọn database bằng sqlcmd -d; không tự chuyển database.
IF DB_NAME() NOT IN (N'shopbangiay_kt',N'shop_verify',N'shopbangiay_thu')
    AND NOT (DB_NAME()=N'shopbangiay' AND COALESCE(TRY_CONVERT(INT,SESSION_CONTEXT(N'shop.allow-production-migration')),0)=1)
    AND DB_NAME() NOT LIKE N'shopbangiay[_]kt[_]%'
    THROW 51000,N'Database không được phép chạy migration.',1;
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET ARITHABORT ON;
SET NUMERIC_ROUNDABORT OFF;
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;
    IF OBJECT_ID(N'dbo.dia_chi_nhan_hang',N'U') IS NULL
        CREATE TABLE dbo.dia_chi_nhan_hang (
            id INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_dia_chi_nhan_hang PRIMARY KEY,
            customer_id INT NOT NULL,
            ten_nguoi_nhan NVARCHAR(100) NOT NULL,
            so_dien_thoai VARCHAR(20) NOT NULL,
            dia_chi NVARCHAR(255) NOT NULL,
            la_mac_dinh BIT NOT NULL CONSTRAINT DF_dia_chi_mac_dinh DEFAULT 0,
            thu_tu TINYINT NOT NULL,
            created_at DATETIME2 NOT NULL CONSTRAINT DF_dia_chi_created_at DEFAULT SYSDATETIME(),
            CONSTRAINT FK_dia_chi_customer FOREIGN KEY(customer_id) REFERENCES dbo.customers(id) ON DELETE CASCADE,
            CONSTRAINT CK_dia_chi_thu_tu CHECK(thu_tu BETWEEN 1 AND 3),
            CONSTRAINT UQ_dia_chi_customer_slot UNIQUE(customer_id,thu_tu)
        );
    IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.dia_chi_nhan_hang') AND name=N'UX_dia_chi_customer_default')
        CREATE UNIQUE INDEX UX_dia_chi_customer_default ON dbo.dia_chi_nhan_hang(customer_id) WHERE la_mac_dinh=1;
    -- Chỉ bù khách chưa có sổ; chạy lại không ghi đè địa chỉ đã được người dùng sửa.
    INSERT dbo.dia_chi_nhan_hang(customer_id,ten_nguoi_nhan,so_dien_thoai,dia_chi,la_mac_dinh,thu_tu,created_at)
        SELECT c.id,c.full_name,COALESCE(c.phone,''),c.address,1,1,COALESCE(c.created_at,SYSDATETIME())
        FROM dbo.customers c WITH(UPDLOCK,HOLDLOCK)
        WHERE NULLIF(LTRIM(RTRIM(c.address)),N'') IS NOT NULL
          AND NOT EXISTS(SELECT 1 FROM dbo.dia_chi_nhan_hang a WHERE a.customer_id=c.id);
    COMMIT;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT>0 ROLLBACK;
    THROW;
END CATCH;

GO

GO
PRINT N'BẮT ĐẦU 010_de_giay.sql';
GO
-- SQL Server. Chọn database bằng sqlcmd -d; không tự chuyển database.
IF DB_NAME() NOT IN (N'shopbangiay_kt',N'shop_verify',N'shop_fresh')
    AND DB_NAME() NOT LIKE N'shopbangiay[_]kt[_]%'
    AND NOT (DB_NAME()=N'shopbangiay' AND COALESCE(TRY_CONVERT(INT,SESSION_CONTEXT(N'shop.allow-production-migration')),0)=1)
    THROW 51000,N'Database không được phép chạy migration.',1;
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET ARITHABORT ON;
SET NUMERIC_ROUNDABORT OFF;
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;
    IF OBJECT_ID(N'dbo.de_giay',N'U') IS NULL
        CREATE TABLE dbo.de_giay (
            id INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_de_giay PRIMARY KEY,
            ten NVARCHAR(100) COLLATE Latin1_General_100_CI_AS NOT NULL,
            trang_thai BIT NOT NULL CONSTRAINT DF_de_giay_trang_thai DEFAULT 1
        );
    IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.de_giay') AND name=N'UX_de_giay_ten')
        CREATE UNIQUE INDEX UX_de_giay_ten ON dbo.de_giay(ten);
    IF COL_LENGTH(N'dbo.products',N'de_giay_id') IS NULL
        ALTER TABLE dbo.products ADD de_giay_id INT NULL;
    IF NOT EXISTS(SELECT 1 FROM sys.foreign_keys WHERE parent_object_id=OBJECT_ID(N'dbo.products') AND name=N'FK_products_de_giay')
        ALTER TABLE dbo.products WITH CHECK ADD CONSTRAINT FK_products_de_giay FOREIGN KEY(de_giay_id) REFERENCES dbo.de_giay(id);
    IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.products') AND name=N'IX_products_de_giay')
        CREATE INDEX IX_products_de_giay ON dbo.products(de_giay_id);
    INSERT dbo.de_giay(ten,trang_thai)
        SELECT seed.ten,1 FROM (VALUES (N'Cao su'),(N'EVA'),(N'PU'),(N'TPU'),(N'Đế bằng'),(N'Đế độn'),(N'Đế chống trượt'),(N'Khác')) seed(ten)
        WHERE NOT EXISTS(SELECT 1 FROM dbo.de_giay current_value WITH(UPDLOCK,HOLDLOCK) WHERE current_value.ten=seed.ten);
    COMMIT;
    PRINT N'Hoàn tất 010_de_giay.sql';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT>0 ROLLBACK;
    THROW;
END CATCH;

GO
PRINT N'HOÀN TẤT 010_de_giay.sql';
GO

GO
PRINT N'BẮT ĐẦU 011_can_cuoc_tai_khoan_email.sql';
GO
-- Additive, idempotent. Explicitly select the database; never changes database.
IF DB_NAME() NOT IN (N'shopbangiay_kt',N'shop_verify',N'shop_fresh')
 AND DB_NAME() NOT LIKE N'shopbangiay[_]kt[_]%'
 AND NOT (DB_NAME()=N'shopbangiay' AND COALESCE(TRY_CONVERT(INT,SESSION_CONTEXT(N'shop.allow-production-migration')),0)=1)
 THROW 51000,N'Database không được phép chạy migration.',1;
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET ARITHABORT ON;
SET NUMERIC_ROUNDABORT OFF;
SET XACT_ABORT ON;
BEGIN TRY
 BEGIN TRANSACTION;
 IF COL_LENGTH(N'dbo.customers',N'so_cccd') IS NULL ALTER TABLE dbo.customers ADD so_cccd VARCHAR(12) NULL;
 IF COL_LENGTH(N'dbo.customers',N'ngay_sinh') IS NULL ALTER TABLE dbo.customers ADD ngay_sinh DATE NULL;
 IF COL_LENGTH(N'dbo.customers',N'gioi_tinh') IS NULL ALTER TABLE dbo.customers ADD gioi_tinh NVARCHAR(10) NULL;
 IF COL_LENGTH(N'dbo.employees',N'so_cccd') IS NULL ALTER TABLE dbo.employees ADD so_cccd VARCHAR(12) NULL;
 IF COL_LENGTH(N'dbo.employees',N'ngay_sinh') IS NULL ALTER TABLE dbo.employees ADD ngay_sinh DATE NULL;
 IF COL_LENGTH(N'dbo.employees',N'gioi_tinh') IS NULL ALTER TABLE dbo.employees ADD gioi_tinh NVARCHAR(10) NULL;
 IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.customers') AND name=N'UX_customers_so_cccd')
  EXEC(N'CREATE UNIQUE INDEX UX_customers_so_cccd ON dbo.customers(so_cccd) WHERE so_cccd IS NOT NULL');
 IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.employees') AND name=N'UX_employees_so_cccd')
  EXEC(N'CREATE UNIQUE INDEX UX_employees_so_cccd ON dbo.employees(so_cccd) WHERE so_cccd IS NOT NULL');
 IF COL_LENGTH(N'dbo.users',N'must_change_password') IS NULL ALTER TABLE dbo.users ADD must_change_password BIT NOT NULL CONSTRAINT DF_users_must_change_password DEFAULT 0 WITH VALUES;
 -- Delivery metadata contains no cleartext password. Row locks enforce the hourly retry limit.
 IF COL_LENGTH(N'dbo.users',N'credential_mail_failed') IS NULL ALTER TABLE dbo.users ADD credential_mail_failed BIT NOT NULL CONSTRAINT DF_users_credential_mail_failed DEFAULT 0 WITH VALUES;
 IF COL_LENGTH(N'dbo.users',N'credential_mail_window') IS NULL ALTER TABLE dbo.users ADD credential_mail_window DATETIME2 NULL;
 IF COL_LENGTH(N'dbo.users',N'credential_mail_count') IS NULL ALTER TABLE dbo.users ADD credential_mail_count INT NOT NULL CONSTRAINT DF_users_credential_mail_count DEFAULT 0 WITH VALUES;
 IF COL_LENGTH(N'dbo.users',N'credential_mail_pending_until') IS NULL ALTER TABLE dbo.users ADD credential_mail_pending_until DATETIME2 NULL;
 COMMIT;
 PRINT N'HOÀN TẤT 011_can_cuoc_tai_khoan_email.sql';
END TRY
BEGIN CATCH
 IF @@TRANCOUNT>0 ROLLBACK;
 THROW;
END CATCH;

GO

GO
PRINT N'BẮT ĐẦU 012_ma_san_pham_tu_sinh.sql';
GO
-- Select the target database explicitly. No USE; no data deletion or code renaming.
IF DB_NAME() NOT IN (N'shopbangiay_kt',N'shop_verify',N'shop_fresh')
 AND DB_NAME() NOT LIKE N'shopbangiay[_]kt[_]%'
 AND NOT (DB_NAME()=N'shopbangiay' AND COALESCE(TRY_CONVERT(INT,SESSION_CONTEXT(N'shop.allow-production-migration')),0)=1)
 THROW 51000,N'Database không được phép chạy migration.',1;
SET XACT_ABORT ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
BEGIN TRY
 BEGIN TRANSACTION;
 DECLARE @largest BIGINT, @next BIGINT, @sql NVARCHAR(MAX);
 -- MAX is used only to initialize/advance a sequence during migration, never to allocate in the app.
 IF EXISTS(SELECT 1 FROM dbo.products WITH(TABLOCKX,HOLDLOCK)
   WHERE code COLLATE Latin1_General_100_BIN2 LIKE 'SP[0-9]%'
   AND SUBSTRING(code,3,50) COLLATE Latin1_General_100_BIN2 NOT LIKE '%[^0-9]%'
   AND TRY_CONVERT(BIGINT,SUBSTRING(code,3,50)) IS NULL)
  THROW 51001,N'Mã sản phẩm hiện có vượt giới hạn số BIGINT. Không tự sửa mã.',1;
 SELECT @largest=COALESCE(MAX(TRY_CONVERT(BIGINT,SUBSTRING(code,3,50))),0)
 FROM dbo.products WITH(TABLOCKX,HOLDLOCK)
 WHERE code COLLATE Latin1_General_100_BIN2 LIKE 'SP[0-9]%'
 AND SUBSTRING(code,3,50) COLLATE Latin1_General_100_BIN2 NOT LIKE '%[^0-9]%'
 AND DATALENGTH(code)=DATALENGTH(LTRIM(RTRIM(code)));
 IF @largest=9223372036854775807 THROW 51002,N'Đã hết giới hạn sinh mã sản phẩm.',1;
 SET @next=@largest+1;
 IF OBJECT_ID(N'dbo.product_code_seq',N'SO') IS NULL
 BEGIN
  SET @sql=N'CREATE SEQUENCE dbo.product_code_seq AS BIGINT START WITH '+CONVERT(NVARCHAR(30),@next)+N' INCREMENT BY 1 MINVALUE 1 NO CYCLE NO CACHE';
  EXEC sys.sp_executesql @sql;
 END
 ELSE
 BEGIN
  IF EXISTS(SELECT 1 FROM sys.sequences WHERE object_id=OBJECT_ID(N'dbo.product_code_seq')
    AND (CONVERT(BIGINT,increment)<>1 OR is_cycling=1 OR is_exhausted=1 OR TYPE_NAME(system_type_id)<>'bigint'))
   THROW 51003,N'Sequence mã sản phẩm có cấu hình không phù hợp; không tự đặt lại.',1;
  IF (SELECT CONVERT(BIGINT,current_value) FROM sys.sequences WHERE object_id=OBJECT_ID(N'dbo.product_code_seq'))<@next
  BEGIN
   SET @sql=N'ALTER SEQUENCE dbo.product_code_seq RESTART WITH '+CONVERT(NVARCHAR(30),@next);
   EXEC sys.sp_executesql @sql;
  END
 END
 IF NOT EXISTS(SELECT 1 FROM sys.indexes i WHERE i.object_id=OBJECT_ID(N'dbo.products')
   AND i.is_unique=1 AND i.has_filter=0 AND i.is_disabled=0 AND i.ignore_dup_key=0
   AND (SELECT COUNT(*) FROM sys.index_columns ic WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal>0)=1
   AND EXISTS(SELECT 1 FROM sys.index_columns ic JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id
      WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal=1 AND c.name=N'code'))
 BEGIN
  IF EXISTS(SELECT code FROM dbo.products GROUP BY code HAVING COUNT_BIG(*)>1)
   THROW 51004,N'Mã sản phẩm bị trùng. Cần xử lý rõ ràng trước khi tạo chỉ mục; không tự đổi mã.',1;
  CREATE UNIQUE INDEX UX_products_code ON dbo.products(code);
 END
 COMMIT;
 PRINT N'HOÀN TẤT 012_ma_san_pham_tu_sinh.sql';
END TRY
BEGIN CATCH
 IF @@TRANCOUNT>0 ROLLBACK;
 THROW;
END CATCH;

GO

GO

-- 013: màu sắc và đế giày thuộc biến thể
-- SQL Server: chọn database bằng sqlcmd -d. Không tự chuyển database.
IF DB_NAME() NOT IN (N'shopbangiay_kt',N'shop_verify',N'shop_fresh')
 AND DB_NAME() NOT LIKE N'shopbangiay[_]kt[_]%'
 AND NOT (DB_NAME()=N'shopbangiay' AND COALESCE(TRY_CONVERT(INT,SESSION_CONTEXT(N'shop.allow-production-migration')),0)=1)
 THROW 51000,N'Database không được phép chạy migration.',1;
SET XACT_ABORT ON;
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
BEGIN TRY
 BEGIN TRANSACTION;
 IF OBJECT_ID(N'dbo.mau_sac',N'U') IS NULL
  CREATE TABLE dbo.mau_sac (
   id INT IDENTITY PRIMARY KEY,
   ten NVARCHAR(100) COLLATE Latin1_General_100_CI_AI NOT NULL,
   ma_mau VARCHAR(7) NULL,
   trang_thai BIT NOT NULL CONSTRAINT DF_mau_sac_status DEFAULT 1,
   CONSTRAINT UX_mau_sac_ten UNIQUE(ten),
   CONSTRAINT CK_mau_sac_ma CHECK(ma_mau IS NULL OR (DATALENGTH(ma_mau)=7 AND ma_mau COLLATE Latin1_General_100_BIN2 LIKE '#[0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f]'))
  );
 INSERT dbo.mau_sac(ten,ma_mau,trang_thai)
 SELECT s.ten,s.ma,1 FROM (VALUES (N'Đen','#111111'),(N'Trắng','#FFFFFF'),(N'Xám','#808080'),(N'Đỏ','#FF0000'),(N'Xanh dương','#0000FF'),(N'Xanh lá','#008000'),(N'Vàng','#FFFF00'),(N'Nâu','#A52A2A'),(N'Be','#F5F5DC'),(N'Hồng','#FFC0CB'),(N'Khác',NULL)) s(ten,ma)
 WHERE NOT EXISTS(SELECT 1 FROM dbo.mau_sac WITH(UPDLOCK,HOLDLOCK) WHERE ten=s.ten);
 IF NOT EXISTS(SELECT 1 FROM dbo.de_giay WITH(UPDLOCK,HOLDLOCK) WHERE ten=N'Khác')
  INSERT dbo.de_giay(ten,trang_thai) VALUES(N'Khác',1);
 IF COL_LENGTH(N'dbo.product_variants',N'mau_sac_id') IS NULL
  ALTER TABLE dbo.product_variants ADD mau_sac_id INT NULL;
 IF COL_LENGTH(N'dbo.product_variants',N'de_giay_id') IS NULL
  ALTER TABLE dbo.product_variants ADD de_giay_id INT NULL;
 EXEC sp_executesql N'
 UPDATE v SET mau_sac_id=c.id FROM dbo.product_variants v CROSS JOIN dbo.mau_sac c WHERE v.mau_sac_id IS NULL AND c.ten=N''Khác'';
 UPDATE v SET de_giay_id=COALESCE(p.de_giay_id,s.id) FROM dbo.product_variants v JOIN dbo.products p ON p.id=v.product_id CROSS JOIN dbo.de_giay s WHERE v.de_giay_id IS NULL AND s.ten=N''Khác'';
 ALTER TABLE dbo.product_variants ALTER COLUMN mau_sac_id INT NOT NULL;
 ALTER TABLE dbo.product_variants ALTER COLUMN de_giay_id INT NOT NULL;';
 IF NOT EXISTS(SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_variants_mau_sac' AND parent_object_id=OBJECT_ID(N'dbo.product_variants'))
  EXEC(N'ALTER TABLE dbo.product_variants WITH CHECK ADD CONSTRAINT FK_variants_mau_sac FOREIGN KEY(mau_sac_id) REFERENCES dbo.mau_sac(id)');
 IF NOT EXISTS(SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_variants_de_giay' AND parent_object_id=OBJECT_ID(N'dbo.product_variants'))
  EXEC(N'ALTER TABLE dbo.product_variants WITH CHECK ADD CONSTRAINT FK_variants_de_giay FOREIGN KEY(de_giay_id) REFERENCES dbo.de_giay(id)');
 -- Chỉ bỏ các khóa duy nhất cũ trên bộ thuộc tính, không đụng khóa chính.
 DECLARE @drop NVARCHAR(MAX)=N'';
 SELECT @drop=@drop+CASE WHEN i.is_unique_constraint=1 THEN N'ALTER TABLE dbo.product_variants DROP CONSTRAINT '+QUOTENAME(i.name)+N';' ELSE N'DROP INDEX '+QUOTENAME(i.name)+N' ON dbo.product_variants;' END
 FROM sys.indexes i WHERE i.object_id=OBJECT_ID(N'dbo.product_variants') AND i.is_unique=1 AND i.is_primary_key=0
 AND (SELECT COUNT(*) FROM sys.index_columns ic WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal>0) IN (2,3)
 AND NOT EXISTS(SELECT 1 FROM sys.index_columns ic JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal>0 AND c.name NOT IN(N'product_id',N'size_id',N'chat_lieu_id'));
 EXEC sp_executesql @drop;
 IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.product_variants') AND name=N'UX_variants_five_attributes')
  EXEC(N'CREATE UNIQUE INDEX UX_variants_five_attributes ON dbo.product_variants(product_id,size_id,chat_lieu_id,mau_sac_id,de_giay_id)');
 IF COL_LENGTH(N'dbo.order_details',N'ten_mau_sac') IS NULL ALTER TABLE dbo.order_details ADD ten_mau_sac NVARCHAR(100) NULL;
 IF COL_LENGTH(N'dbo.order_details',N'ten_de_giay') IS NULL ALTER TABLE dbo.order_details ADD ten_de_giay NVARCHAR(100) NULL;
 COMMIT;
 PRINT N'HOÀN TẤT 013_mau_sac_bien_the.sql';
END TRY
BEGIN CATCH
 IF @@TRANCOUNT>0 ROLLBACK;
 THROW;
END CATCH;

GO

GO

IF DB_NAME() NOT IN (N'shopbangiay_kt',N'shop_verify',N'shop_fresh')
 AND DB_NAME() NOT LIKE N'shopbangiay[_]kt[_]%'
 AND NOT (DB_NAME()=N'shopbangiay' AND COALESCE(TRY_CONVERT(INT,SESSION_CONTEXT(N'shop.allow-production-migration')),0)=1)
 THROW 51000,N'Database không được phép chạy migration.',1;
SET XACT_ABORT ON;
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET ARITHABORT ON;
SET NUMERIC_ROUNDABORT OFF;
BEGIN TRY
 BEGIN TRANSACTION;
 IF COL_LENGTH(N'dbo.orders',N'payment_reference') IS NULL
  ALTER TABLE dbo.orders ADD payment_reference NVARCHAR(40) NULL;
 IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.orders') AND name=N'UX_orders_payment_reference')
  EXEC(N'CREATE UNIQUE INDEX UX_orders_payment_reference ON dbo.orders(payment_reference) WHERE payment_reference IS NOT NULL');
 COMMIT;
 PRINT N'HOÀN TẤT 014_tham_chieu_thanh_toan.sql';
END TRY
BEGIN CATCH
 IF @@TRANCOUNT>0 ROLLBACK;
 THROW;
END CATCH;

GO

GO
-- 015_theo_doi_giao_hang.sql
IF DB_NAME() NOT IN (N'shopbangiay_kt',N'shop_verify',N'shop_fresh')
 AND DB_NAME() NOT LIKE N'shopbangiay[_]kt[_]%'
 AND NOT (DB_NAME()=N'shopbangiay' AND COALESCE(TRY_CONVERT(INT,SESSION_CONTEXT(N'shop.allow-production-migration')),0)=1)
 THROW 51000,N'Database không được phép chạy migration.',1;
SET XACT_ABORT ON;
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET ARITHABORT ON;
SET NUMERIC_ROUNDABORT OFF;
BEGIN TRY
 BEGIN TRANSACTION;
 IF OBJECT_ID(N'dbo.vung_giao_hang',N'U') IS NULL
 CREATE TABLE dbo.vung_giao_hang(ma NVARCHAR(10) NOT NULL PRIMARY KEY,ten NVARCHAR(100) NOT NULL,ngay_min INT NOT NULL,ngay_max INT NOT NULL,CONSTRAINT CK_vung_giao_hang_ngay CHECK(ngay_min BETWEEN 1 AND 30 AND ngay_max BETWEEN ngay_min AND 30));
 IF OBJECT_ID(N'dbo.tinh_thanh',N'U') IS NULL
 CREATE TABLE dbo.tinh_thanh(id INT IDENTITY PRIMARY KEY,ten NVARCHAR(100) COLLATE Latin1_General_100_CI_AI NOT NULL UNIQUE,vung NVARCHAR(10) NOT NULL REFERENCES dbo.vung_giao_hang(ma));
 IF OBJECT_ID(N'dbo.ten_tinh_thanh_cu',N'U') IS NULL
 CREATE TABLE dbo.ten_tinh_thanh_cu(id INT IDENTITY PRIMARY KEY,ten NVARCHAR(100) COLLATE Latin1_General_100_CI_AI NOT NULL UNIQUE,tinh_thanh_id INT NOT NULL REFERENCES dbo.tinh_thanh(id));
 INSERT dbo.vung_giao_hang(ma,ten,ngay_min,ngay_max)
 SELECT s.* FROM (VALUES('BAC',N'Miền Bắc',3,3),('TRUNG',N'Miền Trung',5,7),('NAM',N'Miền Nam',5,7)) s(ma,ten,ngay_min,ngay_max)
 WHERE NOT EXISTS(SELECT 1 FROM dbo.vung_giao_hang t WHERE t.ma=s.ma);
 INSERT dbo.tinh_thanh(ten,vung) SELECT s.ten,s.vung FROM (VALUES(N'Hà Nội','BAC'),(N'Cao Bằng','BAC'),(N'Tuyên Quang','BAC'),(N'Điện Biên','BAC'),(N'Lai Châu','BAC'),(N'Sơn La','BAC'),(N'Lào Cai','BAC'),(N'Thái Nguyên','BAC'),(N'Lạng Sơn','BAC'),(N'Quảng Ninh','BAC'),(N'Bắc Ninh','BAC'),(N'Phú Thọ','BAC'),(N'Hải Phòng','BAC'),(N'Hưng Yên','BAC'),(N'Ninh Bình','BAC'),(N'Thanh Hóa','TRUNG'),(N'Nghệ An','TRUNG'),(N'Hà Tĩnh','TRUNG'),(N'Quảng Trị','TRUNG'),(N'Huế','TRUNG'),(N'Đà Nẵng','TRUNG'),(N'Quảng Ngãi','TRUNG'),(N'Gia Lai','TRUNG'),(N'Khánh Hòa','TRUNG'),(N'Đắk Lắk','TRUNG'),(N'Lâm Đồng','TRUNG'),(N'Đồng Nai','NAM'),(N'Hồ Chí Minh','NAM'),(N'Tây Ninh','NAM'),(N'Đồng Tháp','NAM'),(N'Vĩnh Long','NAM'),(N'An Giang','NAM'),(N'Cần Thơ','NAM'),(N'Cà Mau','NAM')) s(ten,vung) WHERE NOT EXISTS(SELECT 1 FROM dbo.tinh_thanh t WHERE t.ten=s.ten COLLATE Latin1_General_100_CI_AI);
 INSERT dbo.ten_tinh_thanh_cu(ten,tinh_thanh_id) SELECT s.ten,p.id FROM (VALUES(N'Hà Nội',N'Hà Nội'),(N'Cao Bằng',N'Cao Bằng'),(N'Tuyên Quang',N'Tuyên Quang'),(N'Hà Giang',N'Tuyên Quang'),(N'Điện Biên',N'Điện Biên'),(N'Lai Châu',N'Lai Châu'),(N'Sơn La',N'Sơn La'),(N'Lào Cai',N'Lào Cai'),(N'Yên Bái',N'Lào Cai'),(N'Thái Nguyên',N'Thái Nguyên'),(N'Bắc Kạn',N'Thái Nguyên'),(N'Lạng Sơn',N'Lạng Sơn'),(N'Quảng Ninh',N'Quảng Ninh'),(N'Bắc Ninh',N'Bắc Ninh'),(N'Bắc Giang',N'Bắc Ninh'),(N'Phú Thọ',N'Phú Thọ'),(N'Vĩnh Phúc',N'Phú Thọ'),(N'Hòa Bình',N'Phú Thọ'),(N'Hải Phòng',N'Hải Phòng'),(N'Hải Dương',N'Hải Phòng'),(N'Hưng Yên',N'Hưng Yên'),(N'Thái Bình',N'Hưng Yên'),(N'Ninh Bình',N'Ninh Bình'),(N'Hà Nam',N'Ninh Bình'),(N'Nam Định',N'Ninh Bình'),(N'Thanh Hóa',N'Thanh Hóa'),(N'Nghệ An',N'Nghệ An'),(N'Hà Tĩnh',N'Hà Tĩnh'),(N'Quảng Trị',N'Quảng Trị'),(N'Quảng Bình',N'Quảng Trị'),(N'Huế',N'Huế'),(N'Thừa Thiên Huế',N'Huế'),(N'Đà Nẵng',N'Đà Nẵng'),(N'Quảng Nam',N'Đà Nẵng'),(N'Quảng Ngãi',N'Quảng Ngãi'),(N'Kon Tum',N'Quảng Ngãi'),(N'Gia Lai',N'Gia Lai'),(N'Bình Định',N'Gia Lai'),(N'Khánh Hòa',N'Khánh Hòa'),(N'Ninh Thuận',N'Khánh Hòa'),(N'Đắk Lắk',N'Đắk Lắk'),(N'Phú Yên',N'Đắk Lắk'),(N'Lâm Đồng',N'Lâm Đồng'),(N'Đắk Nông',N'Lâm Đồng'),(N'Bình Thuận',N'Lâm Đồng'),(N'Đồng Nai',N'Đồng Nai'),(N'Bình Phước',N'Đồng Nai'),(N'Hồ Chí Minh',N'Hồ Chí Minh'),(N'Bà Rịa Vũng Tàu',N'Hồ Chí Minh'),(N'Bình Dương',N'Hồ Chí Minh'),(N'TPHCM',N'Hồ Chí Minh'),(N'TP HCM',N'Hồ Chí Minh'),(N'HCM',N'Hồ Chí Minh'),(N'Sài Gòn',N'Hồ Chí Minh'),(N'Tây Ninh',N'Tây Ninh'),(N'Long An',N'Tây Ninh'),(N'Đồng Tháp',N'Đồng Tháp'),(N'Tiền Giang',N'Đồng Tháp'),(N'Vĩnh Long',N'Vĩnh Long'),(N'Bến Tre',N'Vĩnh Long'),(N'Trà Vinh',N'Vĩnh Long'),(N'An Giang',N'An Giang'),(N'Kiên Giang',N'An Giang'),(N'Cần Thơ',N'Cần Thơ'),(N'Sóc Trăng',N'Cần Thơ'),(N'Hậu Giang',N'Cần Thơ'),(N'Cà Mau',N'Cà Mau'),(N'Bạc Liêu',N'Cà Mau')) s(ten,now_name) JOIN dbo.tinh_thanh p ON p.ten=s.now_name COLLATE Latin1_General_100_CI_AI WHERE NOT EXISTS(SELECT 1 FROM dbo.ten_tinh_thanh_cu a WHERE a.ten=s.ten COLLATE Latin1_General_100_CI_AI);
 IF COL_LENGTH(N'dbo.dia_chi_nhan_hang',N'tinh_thanh_id') IS NULL ALTER TABLE dbo.dia_chi_nhan_hang ADD tinh_thanh_id INT NULL;
 IF COL_LENGTH(N'dbo.orders',N'tinh_thanh_id') IS NULL ALTER TABLE dbo.orders ADD tinh_thanh_id INT NULL;
 IF COL_LENGTH(N'dbo.orders',N'ten_tinh_thanh') IS NULL ALTER TABLE dbo.orders ADD ten_tinh_thanh NVARCHAR(100) NULL;
 IF COL_LENGTH(N'dbo.orders',N'du_kien_giao_tu') IS NULL ALTER TABLE dbo.orders ADD du_kien_giao_tu DATE NULL;
 IF COL_LENGTH(N'dbo.orders',N'du_kien_giao_den') IS NULL ALTER TABLE dbo.orders ADD du_kien_giao_den DATE NULL;
 IF COL_LENGTH(N'dbo.orders',N'ngay_giao_thuc_te') IS NULL ALTER TABLE dbo.orders ADD ngay_giao_thuc_te DATE NULL;
 IF NOT EXISTS(SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_dia_chi_nhan_hang_tinh_thanh') EXEC(N'ALTER TABLE dbo.dia_chi_nhan_hang ADD CONSTRAINT FK_dia_chi_nhan_hang_tinh_thanh FOREIGN KEY(tinh_thanh_id) REFERENCES dbo.tinh_thanh(id)');
 IF NOT EXISTS(SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_orders_tinh_thanh') EXEC(N'ALTER TABLE dbo.orders ADD CONSTRAINT FK_orders_tinh_thanh FOREIGN KEY(tinh_thanh_id) REFERENCES dbo.tinh_thanh(id)');
 IF OBJECT_ID(N'dbo.order_status_history',N'U') IS NULL
 CREATE TABLE dbo.order_status_history(id INT IDENTITY PRIMARY KEY,order_id INT NOT NULL REFERENCES dbo.orders(id),status NVARCHAR(255) NOT NULL,changed_at DATETIME2 NOT NULL,actor NVARCHAR(100) NOT NULL,note NVARCHAR(1000) NULL,carrier NVARCHAR(100) NULL,tracking_code NVARCHAR(100) NULL);
 IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.order_status_history') AND name=N'IX_order_status_history_order')
 CREATE INDEX IX_order_status_history_order ON dbo.order_status_history(order_id,changed_at,id);
 EXEC(N'
 -- Only match administrative prefixes or the address suffix. Multiple provinces remain unknown.
 SELECT kind,id,CONVERT(NVARCHAR(MAX),address_text) address_text INTO #addresses FROM (
 SELECT ''A'' kind,id,dia_chi address_text FROM dbo.dia_chi_nhan_hang WHERE tinh_thanh_id IS NULL
 UNION ALL SELECT ''O'',id,shipping_address FROM dbo.orders WHERE tinh_thanh_id IS NULL AND order_type=''ONLINE'') x;
 UPDATE #addresses SET address_text=N'' ''+LTRIM(RTRIM(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(address_text,N''-'',N'' ''),N'','',N'' | ''),N''.'',N'' ''),N'';'',N'' | ''),N''Việt Nam'',N'''')))+N'' '';
 WHILE EXISTS(SELECT 1 FROM #addresses WHERE address_text LIKE N''%  %'') UPDATE #addresses SET address_text=REPLACE(address_text,N''  '',N'' '');
 WHILE EXISTS(SELECT 1 FROM #addresses WHERE RIGHT(RTRIM(address_text),1)=N''|'') UPDATE #addresses SET address_text=RTRIM(LEFT(RTRIM(address_text),LEN(RTRIM(address_text))-1))+N'' '' WHERE RIGHT(RTRIM(address_text),1)=N''|'';
 SELECT a.kind,a.id,MIN(n.tinh_thanh_id) province_id INTO #matched
 FROM #addresses a JOIN dbo.ten_tinh_thanh_cu n ON
 (a.address_text COLLATE Latin1_General_100_CI_AI LIKE N''%| ''+n.ten+N'' ''
 OR a.address_text COLLATE Latin1_General_100_CI_AI = N'' ''+n.ten+N'' ''
 OR a.address_text COLLATE Latin1_General_100_CI_AI LIKE N''%| tỉnh ''+n.ten+N'' %''
 OR a.address_text COLLATE Latin1_General_100_CI_AI LIKE N''%| thành phố ''+n.ten+N'' %''
 OR a.address_text COLLATE Latin1_General_100_CI_AI LIKE N''%| tp ''+n.ten+N'' %''
 OR a.address_text COLLATE Latin1_General_100_CI_AI LIKE N'' tỉnh ''+n.ten+N'' %''
 OR a.address_text COLLATE Latin1_General_100_CI_AI LIKE N'' thành phố ''+n.ten+N'' %''
 OR a.address_text COLLATE Latin1_General_100_CI_AI LIKE N'' tp ''+n.ten+N'' %'')
 GROUP BY a.kind,a.id HAVING COUNT(DISTINCT n.tinh_thanh_id)=1;
 UPDATE a SET tinh_thanh_id=m.province_id FROM dbo.dia_chi_nhan_hang a JOIN #matched m ON m.kind=''A'' AND m.id=a.id WHERE a.tinh_thanh_id IS NULL;
 UPDATE o SET tinh_thanh_id=p.id,ten_tinh_thanh=p.ten FROM dbo.orders o JOIN #matched m ON m.kind=''O'' AND m.id=o.id JOIN dbo.tinh_thanh p ON p.id=m.province_id WHERE o.tinh_thanh_id IS NULL;
 DROP TABLE #matched; DROP TABLE #addresses;
 INSERT dbo.order_status_history(order_id,status,changed_at,actor,note)
 SELECT o.id,o.status,COALESCE(o.updated_at,o.created_at,SYSDATETIME()),N''Hệ thống'',N''Bù trạng thái hiện tại từ dữ liệu cũ; không suy đoán thời gian các bước trước.''
 FROM dbo.orders o WHERE NOT EXISTS(SELECT 1 FROM dbo.order_status_history h WHERE h.order_id=o.id);
');
 COMMIT;
 PRINT N'HOÀN TẤT 015_theo_doi_giao_hang.sql';
END TRY
BEGIN CATCH
 IF @@TRANCOUNT>0 ROLLBACK;
 THROW;
END CATCH;

GO

GO
-- 016_dia_chi_phi_ship_don_online.sql
IF DB_NAME() NOT IN (N'shopbangiay_kt',N'shop_verify',N'shop_fresh')
 AND DB_NAME() NOT LIKE N'shopbangiay[_]kt[_]%'
 AND NOT (DB_NAME()=N'shopbangiay' AND COALESCE(TRY_CONVERT(INT,SESSION_CONTEXT(N'shop.allow-production-migration')),0)=1)
 THROW 51000,N'Database không được phép chạy migration.',1;
SET XACT_ABORT ON;
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET ARITHABORT ON;
SET NUMERIC_ROUNDABORT OFF;
BEGIN TRY
 BEGIN TRANSACTION;
 IF COL_LENGTH(N'dbo.tinh_thanh',N'ma_hanh_chinh') IS NULL ALTER TABLE dbo.tinh_thanh ADD ma_hanh_chinh NVARCHAR(2) NULL;
 IF OBJECT_ID(N'dbo.phuong_xa',N'U') IS NULL
 CREATE TABLE dbo.phuong_xa(id INT IDENTITY PRIMARY KEY,ma NVARCHAR(5) NOT NULL UNIQUE,ten NVARCHAR(100) NOT NULL,tinh_thanh_id INT NOT NULL REFERENCES dbo.tinh_thanh(id),latitude DECIMAL(10,7) NULL,longitude DECIMAL(10,7) NULL,geocoded_at DATETIME2 NULL,geocode_retry_after DATETIME2 NULL,CONSTRAINT CK_phuong_xa_point CHECK((latitude IS NULL AND longitude IS NULL) OR (latitude IS NOT NULL AND longitude IS NOT NULL AND latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180)));
 IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.phuong_xa') AND name=N'IX_phuong_xa_tinh')
 CREATE INDEX IX_phuong_xa_tinh ON dbo.phuong_xa(tinh_thanh_id,ten) INCLUDE(ma,latitude,longitude);
 IF OBJECT_ID(N'dbo.cau_hinh_phi_ship',N'U') IS NULL
 CREATE TABLE dbo.cau_hinh_phi_ship(id INT NOT NULL PRIMARY KEY CHECK(id=1),dia_chi_kho NVARCHAR(255) NOT NULL,latitude_kho DECIMAL(10,7) NOT NULL,longitude_kho DECIMAL(10,7) NOT NULL,moc_1_km DECIMAL(10,2) NOT NULL,moc_2_km DECIMAL(10,2) NOT NULL,phi_1 DECIMAL(18,2) NOT NULL,phi_2 DECIMAL(18,2) NOT NULL,phi_xa DECIMAL(18,2) NOT NULL,so_luong_mien_phi INT NOT NULL,CONSTRAINT CK_cau_hinh_phi_ship CHECK(latitude_kho BETWEEN -90 AND 90 AND longitude_kho BETWEEN -180 AND 180 AND moc_1_km>=0 AND moc_2_km>moc_1_km AND phi_1>=0 AND phi_2>=0 AND phi_xa>=0 AND phi_1%1=0 AND phi_2%1=0 AND phi_xa%1=0 AND so_luong_mien_phi BETWEEN 1 AND 1000));
 IF NOT EXISTS(SELECT 1 FROM dbo.cau_hinh_phi_ship WHERE id=1)
 INSERT dbo.cau_hinh_phi_ship VALUES(1,N'Cao đẳng FPT Polytechnic, Trịnh Văn Bô, Hà Nội',21.0380100,105.7469000,30,50,25000,35000,55000,2);
 EXEC(N'UPDATE p SET ma_hanh_chinh=s.ma FROM dbo.tinh_thanh p JOIN (VALUES(N''01'',N''Hà Nội''),(N''04'',N''Cao Bằng''),(N''08'',N''Tuyên Quang''),(N''11'',N''Điện Biên''),(N''12'',N''Lai Châu''),(N''14'',N''Sơn La''),(N''15'',N''Lào Cai''),(N''19'',N''Thái Nguyên''),(N''20'',N''Lạng Sơn''),(N''22'',N''Quảng Ninh''),(N''24'',N''Bắc Ninh''),(N''25'',N''Phú Thọ''),(N''31'',N''Hải Phòng''),(N''33'',N''Hưng Yên''),(N''37'',N''Ninh Bình''),(N''38'',N''Thanh Hóa''),(N''40'',N''Nghệ An''),(N''42'',N''Hà Tĩnh''),(N''44'',N''Quảng Trị''),(N''46'',N''Huế''),(N''48'',N''Đà Nẵng''),(N''51'',N''Quảng Ngãi''),(N''52'',N''Gia Lai''),(N''56'',N''Khánh Hòa''),(N''66'',N''Đắk Lắk''),(N''68'',N''Lâm Đồng''),(N''75'',N''Đồng Nai''),(N''79'',N''Hồ Chí Minh''),(N''80'',N''Tây Ninh''),(N''82'',N''Đồng Tháp''),(N''86'',N''Vĩnh Long''),(N''91'',N''An Giang''),(N''92'',N''Cần Thơ''),(N''96'',N''Cà Mau'')) s(ma,ten) ON p.ten COLLATE Latin1_General_100_CI_AI=s.ten COLLATE Latin1_General_100_CI_AI WHERE p.ma_hanh_chinh IS NULL;');
 IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.tinh_thanh') AND name=N'UX_tinh_thanh_ma_hanh_chinh') EXEC(N'CREATE UNIQUE INDEX UX_tinh_thanh_ma_hanh_chinh ON dbo.tinh_thanh(ma_hanh_chinh) WHERE ma_hanh_chinh IS NOT NULL');
 EXEC(N'INSERT dbo.phuong_xa(ma,ten,tinh_thanh_id) SELECT s.ma,s.ten,p.id FROM (VALUES(N''00004'',N''Phường Ba Đình'',N''01''),(N''00008'',N''Phường Ngọc Hà'',N''01''),(N''00025'',N''Phường Giảng Võ'',N''01''),(N''00070'',N''Phường Hoàn Kiếm'',N''01''),(N''00082'',N''Phường Cửa Nam'',N''01''),(N''00091'',N''Phường Phú Thượng'',N''01''),(N''00097'',N''Phường Hồng Hà'',N''01''),(N''00103'',N''Phường Tây Hồ'',N''01''),(N''00118'',N''Phường Bồ Đề'',N''01''),(N''00127'',N''Phường Việt Hưng'',N''01''),(N''00136'',N''Phường Phúc Lợi'',N''01''),(N''00145'',N''Phường Long Biên'',N''01''),(N''00160'',N''Phường Nghĩa Đô'',N''01''),(N''00166'',N''Phường Cầu Giấy'',N''01''),(N''00175'',N''Phường Yên Hòa'',N''01''),(N''00190'',N''Phường Ô Chợ Dừa'',N''01''),(N''00199'',N''Phường Láng'',N''01''),(N''00226'',N''Phường Văn Miếu - Quốc Tử Giám'',N''01''),(N''00229'',N''Phường Kim Liên'',N''01''),(N''00235'',N''Phường Đống Đa'',N''01''),(N''00256'',N''Phường Hai Bà Trưng'',N''01''),(N''00283'',N''Phường Vĩnh Tuy'',N''01''),(N''00292'',N''Phường Bạch Mai'',N''01''),(N''00301'',N''Phường Vĩnh Hưng'',N''01''),(N''00316'',N''Phường Định Công'',N''01''),(N''00322'',N''Phường Tương Mai'',N''01''),(N''00328'',N''Phường Lĩnh Nam'',N''01''),(N''00331'',N''Phường Hoàng Mai'',N''01''),(N''00337'',N''Phường Hoàng Liệt'',N''01''),(N''00340'',N''Phường Yên Sở'',N''01''),(N''00352'',N''Phường Phương Liệt'',N''01''),(N''00364'',N''Phường Khương Đình'',N''01''),(N''00367'',N''Phường Thanh Xuân'',N''01''),(N''00376'',N''Xã Sóc Sơn'',N''01''),(N''00382'',N''Xã Kim Anh'',N''01''),(N''00385'',N''Xã Trung Giã'',N''01''),(N''00430'',N''Xã Đa Phúc'',N''01''),(N''00433'',N''Xã Nội Bài'',N''01''),(N''00454'',N''Xã Đông Anh'',N''01''),(N''00466'',N''Xã Phúc Thịnh'',N''01''),(N''00475'',N''Xã Thư Lâm'',N''01''),(N''00493'',N''Xã Thiên Lộc'',N''01''),(N''00508'',N''Xã Vĩnh Thanh'',N''01''),(N''00541'',N''Xã Phù Đổng'',N''01''),(N''00562'',N''Xã Thuận An'',N''01''),(N''00565'',N''Xã Gia Lâm'',N''01''),(N''00577'',N''Xã Bát Tràng'',N''01''),(N''00592'',N''Phường Từ Liêm'',N''01''),(N''00598'',N''Phường Thượng Cát'',N''01''),(N''00602'',N''Phường Đông Ngạc'',N''01''),(N''00611'',N''Phường Xuân Đỉnh'',N''01''),(N''00613'',N''Phường Tây Tựu'',N''01''),(N''00619'',N''Phường Phú Diễn'',N''01''),(N''00622'',N''Phường Xuân Phương'',N''01''),(N''00634'',N''Phường Tây Mỗ'',N''01''),(N''00637'',N''Phường Đại Mỗ'',N''01''),(N''00640'',N''Xã Thanh Trì'',N''01''),(N''00643'',N''Phường Thanh Liệt'',N''01''),(N''00664'',N''Xã Đại Thanh'',N''01''),(N''00679'',N''Xã Ngọc Hồi'',N''01''),(N''00685'',N''Xã Nam Phù'',N''01''),(N''04930'',N''Xã Yên Xuân'',N''01''),(N''08974'',N''Xã Quang Minh'',N''01''),(N''08980'',N''Xã Yên Lãng'',N''01''),(N''08995'',N''Xã Tiến Thắng'',N''01''),(N''09022'',N''Xã Mê Linh'',N''01''),(N''09552'',N''Phường Kiến Hưng'',N''01''),(N''09556'',N''Phường Hà Đông'',N''01''),(N''09562'',N''Phường Yên Nghĩa'',N''01''),(N''09568'',N''Phường Phú Lương'',N''01''),(N''09574'',N''Phường Sơn Tây'',N''01''),(N''09604'',N''Phường Tùng Thiện'',N''01''),(N''09616'',N''Xã Đoài Phương'',N''01''),(N''09619'',N''Xã Quảng Oai'',N''01''),(N''09634'',N''Xã Cổ Đô'',N''01''),(N''09661'',N''Xã Minh Châu'',N''01''),(N''09664'',N''Xã Vật Lại'',N''01''),(N''09676'',N''Xã Bất Bạt'',N''01''),(N''09694'',N''Xã Suối Hai'',N''01''),(N''09700'',N''Xã Ba Vì'',N''01''),(N''09706'',N''Xã Yên Bài'',N''01''),(N''09715'',N''Xã Phúc Thọ'',N''01''),(N''09739'',N''Xã Phúc Lộc'',N''01''),(N''09772'',N''Xã Hát Môn'',N''01''),(N''09784'',N''Xã Đan Phượng'',N''01''),(N''09787'',N''Xã Liên Minh'',N''01''),(N''09817'',N''Xã Ô Diên'',N''01''),(N''09832'',N''Xã Hoài Đức'',N''01''),(N''09856'',N''Xã Dương Hòa'',N''01''),(N''09871'',N''Xã Sơn Đồng'',N''01''),(N''09877'',N''Xã An Khánh'',N''01''),(N''09886'',N''Phường Dương Nội'',N''01''),(N''09895'',N''Xã Quốc Oai'',N''01''),(N''09910'',N''Xã Kiều Phú'',N''01''),(N''09931'',N''Xã Hưng Đạo'',N''01''),(N''09952'',N''Xã Phú Cát'',N''01''),(N''09955'',N''Xã Thạch Thất'',N''01''),(N''09982'',N''Xã Hạ Bằng'',N''01''),(N''09988'',N''Xã Hòa Lạc'',N''01''),(N''10003'',N''Xã Tây Phương'',N''01''),(N''10015'',N''Phường Chương Mỹ'',N''01''),(N''10030'',N''Xã Phú Nghĩa'',N''01''),(N''10045'',N''Xã Xuân Mai'',N''01''),(N''10072'',N''Xã Quảng Bị'',N''01''),(N''10081'',N''Xã Trần Phú'',N''01''),(N''10096'',N''Xã Hòa Phú'',N''01''),(N''10114'',N''Xã Thanh Oai'',N''01''),(N''10126'',N''Xã Bình Minh'',N''01''),(N''10144'',N''Xã Tam Hưng'',N''01''),(N''10180'',N''Xã Dân Hòa'',N''01''),(N''10183'',N''Xã Thường Tín'',N''01''),(N''10210'',N''Xã Hồng Vân'',N''01''),(N''10231'',N''Xã Thượng Phúc'',N''01''),(N''10237'',N''Xã Chương Dương'',N''01''),(N''10273'',N''Xã Phú Xuyên'',N''01''),(N''10279'',N''Xã Phượng Dực'',N''01''),(N''10330'',N''Xã Chuyên Mỹ'',N''01''),(N''10342'',N''Xã Đại Xuyên'',N''01''),(N''10354'',N''Xã Vân Đình'',N''01''),(N''10369'',N''Xã Ứng Thiên'',N''01''),(N''10402'',N''Xã Ứng Hòa'',N''01''),(N''10417'',N''Xã Hòa Xá'',N''01''),(N''10441'',N''Xã Mỹ Đức'',N''01''),(N''10459'',N''Xã Phúc Sơn'',N''01''),(N''10465'',N''Xã Hồng Sơn'',N''01''),(N''10489'',N''Xã Hương Sơn'',N''01''),(N''01273'',N''Phường Thục Phán'',N''04''),(N''01279'',N''Phường Nùng Trí Cao'',N''04''),(N''01288'',N''Phường Tân Giang'',N''04''),(N''01290'',N''Xã Bảo Lâm'',N''04''),(N''01294'',N''Xã Lý Bôn'',N''04''),(N''01297'',N''Xã Nam Quang'',N''04''),(N''01304'',N''Xã Quảng Lâm'',N''04''),(N''01318'',N''Xã Yên Thổ'',N''04''),(N''01321'',N''Xã Bảo Lạc'',N''04''),(N''01324'',N''Xã Cốc Pàng'',N''04''),(N''01327'',N''Xã Cô Ba'',N''04''),(N''01336'',N''Xã Khánh Xuân'',N''04''),(N''01339'',N''Xã Xuân Trường'',N''04''),(N''01351'',N''Xã Hưng Đạo'',N''04''),(N''01354'',N''Xã Huy Giáp'',N''04''),(N''01360'',N''Xã Sơn Lộ'',N''04''),(N''01363'',N''Xã Thông Nông'',N''04''),(N''01366'',N''Xã Cần Yên'',N''04''),(N''01387'',N''Xã Thanh Long'',N''04''),(N''01392'',N''Xã Trường Hà'',N''04''),(N''01393'',N''Xã Lũng Nặm'',N''04''),(N''01414'',N''Xã Tổng Cọt'',N''04''),(N''01438'',N''Xã Hà Quảng'',N''04''),(N''01447'',N''Xã Trà Lĩnh'',N''04''),(N''01456'',N''Xã Quang Hán'',N''04''),(N''01465'',N''Xã Quang Trung'',N''04''),(N''01477'',N''Xã Trùng Khánh'',N''04''),(N''01489'',N''Xã Đình Phong'',N''04''),(N''01501'',N''Xã Đàm Thủy'',N''04''),(N''01525'',N''Xã Đoài Dương'',N''04''),(N''01537'',N''Xã Lý Quốc'',N''04''),(N''01552'',N''Xã Quang Long'',N''04''),(N''01558'',N''Xã Hạ Lang'',N''04''),(N''01561'',N''Xã Vinh Quý'',N''04''),(N''01576'',N''Xã Quảng Uyên'',N''04''),(N''01594'',N''Xã Độc Lập'',N''04''),(N''01618'',N''Xã Hạnh Phúc'',N''04''),(N''01636'',N''Xã Bế Văn Đàn'',N''04''),(N''01648'',N''Xã Phục Hòa'',N''04''),(N''01654'',N''Xã Hòa An'',N''04''),(N''01660'',N''Xã Nam Tuấn'',N''04''),(N''01699'',N''Xã Nguyễn Huệ'',N''04''),(N''01708'',N''Xã Bạch Đằng'',N''04''),(N''01726'',N''Xã Nguyên Bình'',N''04''),(N''01729'',N''Xã Tĩnh Túc'',N''04''),(N''01738'',N''Xã Ca Thành'',N''04''),(N''01747'',N''Xã Minh Tâm'',N''04''),(N''01768'',N''Xã Phan Thanh'',N''04''),(N''01774'',N''Xã Tam Kim'',N''04''),(N''01777'',N''Xã Thành Công'',N''04''),(N''01786'',N''Xã Đông Khê'',N''04''),(N''01789'',N''Xã Canh Tân'',N''04''),(N''01792'',N''Xã Kim Đồng'',N''04''),(N''01795'',N''Xã Minh Khai'',N''04''),(N''01807'',N''Xã Thạch An'',N''04''),(N''01822'',N''Xã Đức Long'',N''04''),(N''00691'',N''Phường Hà Giang 2'',N''08''),(N''00694'',N''Phường Hà Giang 1'',N''08''),(N''00700'',N''Xã Ngọc Đường'',N''08''),(N''00706'',N''Xã Phú Linh'',N''08''),(N''00715'',N''Xã Lũng Cú'',N''08''),(N''00721'',N''Xã Đồng Văn'',N''08''),(N''00733'',N''Xã Sà Phìn'',N''08''),(N''00745'',N''Xã Phố Bảng'',N''08''),(N''00763'',N''Xã Lũng Phìn'',N''08''),(N''00769'',N''Xã Mèo Vạc'',N''08''),(N''00778'',N''Xã Sơn Vĩ'',N''08''),(N''00787'',N''Xã Sủng Máng'',N''08''),(N''00802'',N''Xã Khâu Vai'',N''08''),(N''00808'',N''Xã Tát Ngà'',N''08''),(N''00817'',N''Xã Niêm Sơn'',N''08''),(N''00820'',N''Xã Yên Minh'',N''08''),(N''00829'',N''Xã Thắng Mố'',N''08''),(N''00832'',N''Xã Bạch Đích'',N''08'')) s(ma,ten,tinh) JOIN dbo.tinh_thanh p ON p.ma_hanh_chinh=s.tinh WHERE NOT EXISTS(SELECT 1 FROM dbo.phuong_xa w WHERE w.ma=s.ma);');
 EXEC(N'INSERT dbo.phuong_xa(ma,ten,tinh_thanh_id) SELECT s.ma,s.ten,p.id FROM (VALUES(N''00847'',N''Xã Mậu Duệ'',N''08''),(N''00859'',N''Xã Ngọc Long'',N''08''),(N''00865'',N''Xã Đường Thượng'',N''08''),(N''00871'',N''Xã Du Già'',N''08''),(N''00874'',N''Xã Quản Bạ'',N''08''),(N''00883'',N''Xã Cán Tỷ'',N''08''),(N''00889'',N''Xã Nghĩa Thuận'',N''08''),(N''00892'',N''Xã Tùng Vài'',N''08''),(N''00901'',N''Xã Lùng Tám'',N''08''),(N''00913'',N''Xã Vị Xuyên'',N''08''),(N''00919'',N''Xã Minh Tân'',N''08''),(N''00922'',N''Xã Thuận Hoà'',N''08''),(N''00925'',N''Xã Tùng Bá'',N''08''),(N''00928'',N''Xã Thanh Thủy'',N''08''),(N''00937'',N''Xã Lao Chải'',N''08''),(N''00952'',N''Xã Cao Bồ'',N''08''),(N''00958'',N''Xã Thượng Sơn'',N''08''),(N''00967'',N''Xã Việt Lâm'',N''08''),(N''00970'',N''Xã Linh Hồ'',N''08''),(N''00976'',N''Xã Bạch Ngọc'',N''08''),(N''00982'',N''Xã Minh Sơn'',N''08''),(N''00985'',N''Xã Giáp Trung'',N''08''),(N''00991'',N''Xã Bắc Mê'',N''08''),(N''00994'',N''Xã Minh Ngọc'',N''08''),(N''01006'',N''Xã Yên Cường'',N''08''),(N''01012'',N''Xã Đường Hồng'',N''08''),(N''01021'',N''Xã Hoàng Su Phì'',N''08''),(N''01024'',N''Xã Bản Máy'',N''08''),(N''01033'',N''Xã Thàng Tín'',N''08''),(N''01051'',N''Xã Tân Tiến'',N''08''),(N''01057'',N''Xã Pờ Ly Ngài'',N''08''),(N''01075'',N''Xã Nậm Dịch'',N''08''),(N''01084'',N''Xã Hồ Thầu'',N''08''),(N''01090'',N''Xã Thông Nguyên'',N''08''),(N''01096'',N''Xã Pà Vầy Sủ'',N''08''),(N''01108'',N''Xã Xín Mần'',N''08''),(N''01117'',N''Xã Trung Thịnh'',N''08''),(N''01141'',N''Xã Nấm Dẩn'',N''08''),(N''01144'',N''Xã Quảng Nguyên'',N''08''),(N''01147'',N''Xã Khuôn Lùng'',N''08''),(N''01153'',N''Xã Bắc Quang'',N''08''),(N''01156'',N''Xã Vĩnh Tuy'',N''08''),(N''01165'',N''Xã Đồng Tâm'',N''08''),(N''01171'',N''Xã Tân Quang'',N''08''),(N''01180'',N''Xã Bằng Hành'',N''08''),(N''01192'',N''Xã Liên Hiệp'',N''08''),(N''01201'',N''Xã Hùng An'',N''08''),(N''01216'',N''Xã Đồng Yên'',N''08''),(N''01225'',N''Xã Tiên Nguyên'',N''08''),(N''01234'',N''Xã Yên Thành'',N''08''),(N''01237'',N''Xã Quang Bình'',N''08''),(N''01243'',N''Xã Tân Trịnh'',N''08''),(N''01246'',N''Xã Bằng Lang'',N''08''),(N''01255'',N''Xã Xuân Giang'',N''08''),(N''01261'',N''Xã Tiên Yên'',N''08''),(N''02212'',N''Phường Nông Tiến'',N''08''),(N''02215'',N''Phường Minh Xuân'',N''08''),(N''02221'',N''Xã Nà Hang'',N''08''),(N''02239'',N''Xã Thượng Nông'',N''08''),(N''02245'',N''Xã Côn Lôn'',N''08''),(N''02248'',N''Xã Yên Hoa'',N''08''),(N''02260'',N''Xã Hồng Thái'',N''08''),(N''02266'',N''Xã Lâm Bình'',N''08''),(N''02269'',N''Xã Thượng Lâm'',N''08''),(N''02287'',N''Xã Chiêm Hoá'',N''08''),(N''02296'',N''Xã Bình An'',N''08''),(N''02302'',N''Xã Minh Quang'',N''08''),(N''02305'',N''Xã Trung Hà'',N''08''),(N''02308'',N''Xã Tân Mỹ'',N''08''),(N''02317'',N''Xã Yên Lập'',N''08''),(N''02320'',N''Xã Tân An'',N''08''),(N''02332'',N''Xã Kiên Đài'',N''08''),(N''02350'',N''Xã Kim Bình'',N''08''),(N''02353'',N''Xã Hoà An'',N''08''),(N''02359'',N''Xã Tri Phú'',N''08''),(N''02365'',N''Xã Yên Nguyên'',N''08''),(N''02374'',N''Xã Hàm Yên'',N''08''),(N''02380'',N''Xã Bạch Xa'',N''08''),(N''02392'',N''Xã Phù Lưu'',N''08''),(N''02398'',N''Xã Yên Phú'',N''08''),(N''02404'',N''Xã Bình Xa'',N''08''),(N''02407'',N''Xã Thái Sơn'',N''08''),(N''02419'',N''Xã Thái Hoà'',N''08''),(N''02425'',N''Xã Hùng Đức'',N''08''),(N''02434'',N''Xã Lực Hành'',N''08''),(N''02437'',N''Xã Kiến Thiết'',N''08''),(N''02449'',N''Xã Xuân Vân'',N''08''),(N''02455'',N''Xã Hùng Lợi'',N''08''),(N''02458'',N''Xã Trung Sơn'',N''08''),(N''02470'',N''Xã Tân Long'',N''08''),(N''02473'',N''Xã Yên Sơn'',N''08''),(N''02494'',N''Xã Thái Bình'',N''08''),(N''02509'',N''Phường Mỹ Lâm'',N''08''),(N''02512'',N''Phường An Tường'',N''08''),(N''02524'',N''Phường Bình Thuận'',N''08''),(N''02530'',N''Xã Nhữ Khê'',N''08''),(N''02536'',N''Xã Sơn Dương'',N''08''),(N''02545'',N''Xã Tân Trào'',N''08''),(N''02548'',N''Xã Bình Ca'',N''08''),(N''02554'',N''Xã Minh Thanh'',N''08''),(N''02572'',N''Xã Đông Thọ'',N''08''),(N''02578'',N''Xã Tân Thanh'',N''08''),(N''02608'',N''Xã Hồng Sơn'',N''08''),(N''02611'',N''Xã Phú Lương'',N''08''),(N''02620'',N''Xã Sơn Thuỷ'',N''08''),(N''02623'',N''Xã Trường Sinh'',N''08''),(N''03127'',N''Phường Điện Biên Phủ'',N''11''),(N''03151'',N''Phường Mường Lay'',N''11''),(N''03158'',N''Xã Sín Thầu'',N''11''),(N''03160'',N''Xã Mường Nhé'',N''11''),(N''03162'',N''Xã Nậm Kè'',N''11''),(N''03163'',N''Xã Mường Toong'',N''11''),(N''03164'',N''Xã Quảng Lâm'',N''11''),(N''03166'',N''Xã Mường Chà'',N''11''),(N''03169'',N''Xã Nà Hỳ'',N''11''),(N''03172'',N''Xã Na Sang'',N''11''),(N''03175'',N''Xã Chà Tở'',N''11''),(N''03176'',N''Xã Nà Bủng'',N''11''),(N''03181'',N''Xã Mường Tùng'',N''11''),(N''03193'',N''Xã Pa Ham'',N''11''),(N''03194'',N''Xã Nậm Nèn'',N''11''),(N''03199'',N''Xã Si Pa Phìn'',N''11''),(N''03202'',N''Xã Mường Pồn'',N''11''),(N''03203'',N''Xã Na Son'',N''11''),(N''03208'',N''Xã Xa Dung'',N''11''),(N''03214'',N''Xã Mường Luân'',N''11''),(N''03217'',N''Xã Tủa Chùa'',N''11''),(N''03220'',N''Xã Tủa Thàng'',N''11''),(N''03226'',N''Xã Sín Chải'',N''11''),(N''03241'',N''Xã Sính Phình'',N''11''),(N''03244'',N''Xã Sáng Nhè'',N''11''),(N''03253'',N''Xã Tuần Giáo'',N''11''),(N''03256'',N''Xã Mường Ảng'',N''11''),(N''03260'',N''Xã Pú Nhung'',N''11''),(N''03268'',N''Xã Mường Mùn'',N''11''),(N''03283'',N''Xã Chiềng Sinh'',N''11''),(N''03295'',N''Xã Quài Tở'',N''11''),(N''03301'',N''Xã Búng Lao'',N''11''),(N''03313'',N''Xã Mường Lạn'',N''11''),(N''03316'',N''Xã Nà Tấu'',N''11''),(N''03325'',N''Xã Mường Phăng'',N''11''),(N''03328'',N''Xã Thanh Nưa'',N''11''),(N''03334'',N''Phường Mường Thanh'',N''11''),(N''03349'',N''Xã Thanh Yên'',N''11''),(N''03352'',N''Xã Thanh An'',N''11''),(N''03356'',N''Xã Sam Mứn'',N''11''),(N''03358'',N''Xã Núa Ngam'',N''11''),(N''03368'',N''Xã Mường Nhà'',N''11''),(N''03370'',N''Xã Pu Nhi'',N''11''),(N''03382'',N''Xã Phình Giàng'',N''11''),(N''03385'',N''Xã Tìa Dình'',N''11''),(N''03388'',N''Phường Đoàn Kết'',N''12''),(N''03390'',N''Xã Bình Lư'',N''12''),(N''03394'',N''Xã Sin Suối Hồ'',N''12''),(N''03405'',N''Xã Tả Lèng'',N''12''),(N''03408'',N''Phường Tân Phong'',N''12''),(N''03424'',N''Xã Bản Bo'',N''12''),(N''03430'',N''Xã Khun Há'',N''12''),(N''03433'',N''Xã Bum Tở'',N''12''),(N''03434'',N''Xã Nậm Hàng'',N''12''),(N''03439'',N''Xã Thu Lũm'',N''12''),(N''03442'',N''Xã Pa Ủ'',N''12''),(N''03445'',N''Xã Mường Tè'',N''12''),(N''03451'',N''Xã Mù Cả'',N''12''),(N''03460'',N''Xã Hua Bum'',N''12''),(N''03463'',N''Xã Tà Tổng'',N''12''),(N''03466'',N''Xã Bum Nưa'',N''12''),(N''03472'',N''Xã Mường Mô'',N''12''),(N''03478'',N''Xã Sìn Hồ'',N''12''),(N''03487'',N''Xã Lê Lợi'',N''12''),(N''03503'',N''Xã Pa Tần'',N''12''),(N''03508'',N''Xã Hồng Thu'',N''12''),(N''03517'',N''Xã Nậm Tăm'',N''12''),(N''03529'',N''Xã Tủa Sín Chải'',N''12''),(N''03532'',N''Xã Pu Sam Cáp'',N''12''),(N''03538'',N''Xã Nậm Mạ'',N''12''),(N''03544'',N''Xã Nậm Cuổi'',N''12''),(N''03549'',N''Xã Phong Thổ'',N''12''),(N''03562'',N''Xã Sì Lở Lầu'',N''12''),(N''03571'',N''Xã Dào San'',N''12''),(N''03583'',N''Xã Khổng Lào'',N''12''),(N''03595'',N''Xã Than Uyên'',N''12''),(N''03598'',N''Xã Tân Uyên'',N''12''),(N''03601'',N''Xã Mường Khoa'',N''12''),(N''03613'',N''Xã Nậm Sỏ'',N''12''),(N''03616'',N''Xã Pắc Ta'',N''12''),(N''03618'',N''Xã Mường Than'',N''12''),(N''03637'',N''Xã Mường Kim'',N''12''),(N''03640'',N''Xã Khoen On'',N''12''),(N''03646'',N''Phường Tô Hiệu'',N''14''),(N''03664'',N''Phường Chiềng An'',N''14''),(N''03670'',N''Phường Chiềng Cơi'',N''14''),(N''03679'',N''Phường Chiềng Sinh'',N''14''),(N''03688'',N''Xã Mường Chiên'',N''14''),(N''03694'',N''Xã Mường Giôn'',N''14''),(N''03703'',N''Xã Quỳnh Nhai'',N''14''),(N''03712'',N''Xã Mường Sại'',N''14''),(N''03721'',N''Xã Thuận Châu'',N''14''),(N''03724'',N''Xã Bình Thuận'',N''14''),(N''03727'',N''Xã Mường É'',N''14'')) s(ma,ten,tinh) JOIN dbo.tinh_thanh p ON p.ma_hanh_chinh=s.tinh WHERE NOT EXISTS(SELECT 1 FROM dbo.phuong_xa w WHERE w.ma=s.ma);');
 EXEC(N'INSERT dbo.phuong_xa(ma,ten,tinh_thanh_id) SELECT s.ma,s.ten,p.id FROM (VALUES(N''03754'',N''Xã Chiềng La'',N''14''),(N''03757'',N''Xã Mường Khiêng'',N''14''),(N''03760'',N''Xã Mường Bám'',N''14''),(N''03763'',N''Xã Long Hẹ'',N''14''),(N''03781'',N''Xã Co Mạ'',N''14''),(N''03784'',N''Xã Nậm Lầu'',N''14''),(N''03799'',N''Xã Muổi Nọi'',N''14''),(N''03808'',N''Xã Mường La'',N''14''),(N''03814'',N''Xã Chiềng Lao'',N''14''),(N''03820'',N''Xã Ngọc Chiến'',N''14''),(N''03847'',N''Xã Mường Bú'',N''14''),(N''03850'',N''Xã Chiềng Hoa'',N''14''),(N''03856'',N''Xã Bắc Yên'',N''14''),(N''03862'',N''Xã Xím Vàng'',N''14''),(N''03868'',N''Xã Tà Xùa'',N''14''),(N''03871'',N''Xã Pắc Ngà'',N''14''),(N''03880'',N''Xã Tạ Khoa'',N''14''),(N''03892'',N''Xã Chiềng Sại'',N''14''),(N''03901'',N''Xã Suối Tọ'',N''14''),(N''03907'',N''Xã Mường Cơi'',N''14''),(N''03910'',N''Xã Phù Yên'',N''14''),(N''03922'',N''Xã Gia Phù'',N''14''),(N''03943'',N''Xã Mường Bang'',N''14''),(N''03958'',N''Xã Tường Hạ'',N''14''),(N''03961'',N''Xã Kim Bon'',N''14''),(N''03970'',N''Xã Tân Phong'',N''14''),(N''03979'',N''Phường Mộc Sơn'',N''14''),(N''03980'',N''Phường Mộc Châu'',N''14''),(N''03982'',N''Phường Thảo Nguyên'',N''14''),(N''03985'',N''Xã Chiềng Sơn'',N''14''),(N''03997'',N''Xã Tân Yên'',N''14''),(N''04000'',N''Xã Đoàn Kết'',N''14''),(N''04006'',N''Xã Song Khủa'',N''14''),(N''04018'',N''Xã Tô Múa'',N''14''),(N''04033'',N''Phường Vân Sơn'',N''14''),(N''04045'',N''Xã Lóng Sập'',N''14''),(N''04048'',N''Xã Vân Hồ'',N''14''),(N''04057'',N''Xã Xuân Nha'',N''14''),(N''04075'',N''Xã Yên Châu'',N''14''),(N''04078'',N''Xã Chiềng Hặc'',N''14''),(N''04087'',N''Xã Yên Sơn'',N''14''),(N''04096'',N''Xã Lóng Phiêng'',N''14''),(N''04099'',N''Xã Phiêng Khoài'',N''14''),(N''04105'',N''Xã Mai Sơn'',N''14''),(N''04108'',N''Xã Chiềng Sung'',N''14''),(N''04117'',N''Xã Mường Chanh'',N''14''),(N''04123'',N''Xã Chiềng Mung'',N''14''),(N''04132'',N''Xã Chiềng Mai'',N''14''),(N''04136'',N''Xã Tà Hộc'',N''14''),(N''04144'',N''Xã Phiêng Cằm'',N''14''),(N''04159'',N''Xã Phiêng Pằn'',N''14''),(N''04168'',N''Xã Sông Mã'',N''14''),(N''04171'',N''Xã Bó Sinh'',N''14''),(N''04183'',N''Xã Mường Lầm'',N''14''),(N''04186'',N''Xã Nậm Ty'',N''14''),(N''04195'',N''Xã Chiềng Sơ'',N''14''),(N''04204'',N''Xã Chiềng Khoong'',N''14''),(N''04210'',N''Xã Huổi Một'',N''14''),(N''04219'',N''Xã Mường Hung'',N''14''),(N''04222'',N''Xã Chiềng Khương'',N''14''),(N''04228'',N''Xã Púng Bánh'',N''14''),(N''04231'',N''Xã Sốp Cộp'',N''14''),(N''04240'',N''Xã Mường Lèo'',N''14''),(N''04246'',N''Xã Mường Lạn'',N''14''),(N''02647'',N''Phường Lào Cai'',N''15''),(N''02671'',N''Phường Cam Đường'',N''15''),(N''02680'',N''Xã Hợp Thành'',N''15''),(N''02683'',N''Xã Bát Xát'',N''15''),(N''02686'',N''Xã A Mú Sung'',N''15''),(N''02695'',N''Xã Trịnh Tường'',N''15''),(N''02701'',N''Xã Y Tý'',N''15''),(N''02707'',N''Xã Dền Sáng'',N''15''),(N''02725'',N''Xã Bản Xèo'',N''15''),(N''02728'',N''Xã Mường Hum'',N''15''),(N''02746'',N''Xã Cốc San'',N''15''),(N''02752'',N''Xã Pha Long'',N''15''),(N''02761'',N''Xã Mường Khương'',N''15''),(N''02782'',N''Xã Cao Sơn'',N''15''),(N''02788'',N''Xã Bản Lầu'',N''15''),(N''02809'',N''Xã Si Ma Cai'',N''15''),(N''02824'',N''Xã Sín Chéng'',N''15''),(N''02839'',N''Xã Bắc Hà'',N''15''),(N''02842'',N''Xã Tả Củ Tỷ'',N''15''),(N''02848'',N''Xã Lùng Phình'',N''15''),(N''02869'',N''Xã Bản Liền'',N''15''),(N''02890'',N''Xã Bảo Nhai'',N''15''),(N''02896'',N''Xã Cốc Lầu'',N''15''),(N''02902'',N''Xã Phong Hải'',N''15''),(N''02905'',N''Xã Bảo Thắng'',N''15''),(N''02908'',N''Xã Tằng Loỏng'',N''15''),(N''02923'',N''Xã Gia Phú'',N''15''),(N''02926'',N''Xã Xuân Quang'',N''15''),(N''02947'',N''Xã Bảo Yên'',N''15''),(N''02953'',N''Xã Nghĩa Đô'',N''15''),(N''02962'',N''Xã Xuân Hòa'',N''15''),(N''02968'',N''Xã Thượng Hà'',N''15''),(N''02989'',N''Xã Bảo Hà'',N''15''),(N''02998'',N''Xã Phúc Khánh'',N''15''),(N''03004'',N''Xã Ngũ Chỉ Sơn'',N''15''),(N''03006'',N''Phường Sa Pa'',N''15''),(N''03013'',N''Xã Tả Phìn'',N''15''),(N''03037'',N''Xã Tả Van'',N''15''),(N''03043'',N''Xã Mường Bo'',N''15''),(N''03046'',N''Xã Bản Hồ'',N''15''),(N''03061'',N''Xã Võ Lao'',N''15''),(N''03076'',N''Xã Nậm Chày'',N''15''),(N''03082'',N''Xã Văn Bàn'',N''15''),(N''03085'',N''Xã Nậm Xé'',N''15''),(N''03091'',N''Xã Chiềng Ken'',N''15''),(N''03103'',N''Xã Khánh Yên'',N''15''),(N''03106'',N''Xã Dương Quỳ'',N''15''),(N''03121'',N''Xã Minh Lương'',N''15''),(N''04252'',N''Phường Yên Bái'',N''15''),(N''04273'',N''Phường Nam Cường'',N''15''),(N''04279'',N''Phường Văn Phú'',N''15''),(N''04288'',N''Phường Nghĩa Lộ'',N''15''),(N''04303'',N''Xã Lục Yên'',N''15''),(N''04309'',N''Xã Lâm Thượng'',N''15''),(N''04336'',N''Xã Tân Lĩnh'',N''15''),(N''04342'',N''Xã Khánh Hòa'',N''15''),(N''04345'',N''Xã Mường Lai'',N''15''),(N''04363'',N''Xã Phúc Lợi'',N''15''),(N''04375'',N''Xã Mậu A'',N''15''),(N''04381'',N''Xã Lâm Giang'',N''15''),(N''04387'',N''Xã Châu Quế'',N''15''),(N''04399'',N''Xã Đông Cuông'',N''15''),(N''04402'',N''Xã Phong Dụ Hạ'',N''15''),(N''04423'',N''Xã Phong Dụ Thượng'',N''15''),(N''04429'',N''Xã Tân Hợp'',N''15''),(N''04441'',N''Xã Xuân Ái'',N''15''),(N''04450'',N''Xã Mỏ Vàng'',N''15''),(N''04456'',N''Xã Mù Cang Chải'',N''15''),(N''04462'',N''Xã Nậm Có'',N''15''),(N''04465'',N''Xã Khao Mang'',N''15''),(N''04474'',N''Xã Lao Chải'',N''15''),(N''04489'',N''Xã Chế Tạo'',N''15''),(N''04492'',N''Xã Púng Luông'',N''15''),(N''04498'',N''Xã Trấn Yên'',N''15''),(N''04531'',N''Xã Quy Mông'',N''15''),(N''04537'',N''Xã Lương Thịnh'',N''15''),(N''04543'',N''Phường Âu Lâu'',N''15''),(N''04564'',N''Xã Việt Hồng'',N''15''),(N''04576'',N''Xã Hưng Khánh'',N''15''),(N''04585'',N''Xã Hạnh Phúc'',N''15''),(N''04603'',N''Xã Tà Xi Láng'',N''15''),(N''04606'',N''Xã Trạm Tấu'',N''15''),(N''04609'',N''Xã Phình Hồ'',N''15''),(N''04630'',N''Xã Tú Lệ'',N''15''),(N''04636'',N''Xã Gia Hội'',N''15''),(N''04651'',N''Xã Sơn Lương'',N''15''),(N''04660'',N''Xã Liên Sơn'',N''15''),(N''04663'',N''Phường Trung Tâm'',N''15''),(N''04672'',N''Xã Văn Chấn'',N''15''),(N''04681'',N''Phường Cầu Thia'',N''15''),(N''04693'',N''Xã Cát Thịnh'',N''15''),(N''04699'',N''Xã Chấn Thịnh'',N''15''),(N''04705'',N''Xã Thượng Bằng La'',N''15''),(N''04711'',N''Xã Nghĩa Tâm'',N''15''),(N''04714'',N''Xã Yên Bình'',N''15''),(N''04717'',N''Xã Thác Bà'',N''15''),(N''04726'',N''Xã Cảm Nhân'',N''15''),(N''04744'',N''Xã Yên Thành'',N''15''),(N''04750'',N''Xã Bảo Ái'',N''15''),(N''01840'',N''Phường Đức Xuân'',N''19''),(N''01843'',N''Phường Bắc Kạn'',N''19''),(N''01849'',N''Xã Phong Quang'',N''19''),(N''01864'',N''Xã Bằng Thành'',N''19''),(N''01879'',N''Xã Cao Minh'',N''19''),(N''01882'',N''Xã Nghiên Loan'',N''19''),(N''01894'',N''Xã Phúc Lộc'',N''19''),(N''01906'',N''Xã Ba Bể'',N''19''),(N''01912'',N''Xã Chợ Rã'',N''19''),(N''01921'',N''Xã Thượng Minh'',N''19''),(N''01933'',N''Xã Đồng Phúc'',N''19''),(N''01936'',N''Xã Nà Phặc'',N''19''),(N''01942'',N''Xã Bằng Vân'',N''19''),(N''01954'',N''Xã Ngân Sơn'',N''19''),(N''01957'',N''Xã Thượng Quan'',N''19''),(N''01960'',N''Xã Hiệp Lực'',N''19''),(N''01969'',N''Xã Phủ Thông'',N''19''),(N''01981'',N''Xã Vĩnh Thông'',N''19''),(N''02008'',N''Xã Cẩm Giàng'',N''19''),(N''02014'',N''Xã Bạch Thông'',N''19''),(N''02020'',N''Xã Chợ Đồn'',N''19''),(N''02026'',N''Xã Nam Cường'',N''19''),(N''02038'',N''Xã Quảng Bạch'',N''19''),(N''02044'',N''Xã Yên Thịnh'',N''19''),(N''02071'',N''Xã Nghĩa Tá'',N''19''),(N''02083'',N''Xã Yên Phong'',N''19''),(N''02086'',N''Xã Chợ Mới'',N''19''),(N''02101'',N''Xã Thanh Mai'',N''19''),(N''02104'',N''Xã Tân Kỳ'',N''19''),(N''02107'',N''Xã Thanh Thịnh'',N''19''),(N''02116'',N''Xã Yên Bình'',N''19''),(N''02143'',N''Xã Văn Lang'',N''19''),(N''02152'',N''Xã Cường Lợi'',N''19''),(N''02155'',N''Xã Na Rì'',N''19''),(N''02176'',N''Xã Trần Phú'',N''19''),(N''02185'',N''Xã Côn Minh'',N''19''),(N''02191'',N''Xã Xuân Dương'',N''19'')) s(ma,ten,tinh) JOIN dbo.tinh_thanh p ON p.ma_hanh_chinh=s.tinh WHERE NOT EXISTS(SELECT 1 FROM dbo.phuong_xa w WHERE w.ma=s.ma);');
 EXEC(N'INSERT dbo.phuong_xa(ma,ten,tinh_thanh_id) SELECT s.ma,s.ten,p.id FROM (VALUES(N''05443'',N''Phường Phan Đình Phùng'',N''19''),(N''05455'',N''Phường Quyết Thắng'',N''19''),(N''05467'',N''Phường Gia Sàng'',N''19''),(N''05482'',N''Phường Quan Triều'',N''19''),(N''05488'',N''Xã Đại Phúc'',N''19''),(N''05500'',N''Phường Tích Lương'',N''19''),(N''05503'',N''Xã Tân Cương'',N''19''),(N''05518'',N''Phường Sông Công'',N''19''),(N''05528'',N''Phường Bách Quang'',N''19''),(N''05533'',N''Phường Bá Xuyên'',N''19''),(N''05542'',N''Xã Lam Vỹ'',N''19''),(N''05551'',N''Xã Kim Phượng'',N''19''),(N''05563'',N''Xã Phượng Tiến'',N''19''),(N''05569'',N''Xã Định Hóa'',N''19''),(N''05581'',N''Xã Trung Hội'',N''19''),(N''05587'',N''Xã Bình Yên'',N''19''),(N''05602'',N''Xã Phú Đình'',N''19''),(N''05605'',N''Xã Bình Thành'',N''19''),(N''05611'',N''Xã Phú Lương'',N''19''),(N''05620'',N''Xã Yên Trạch'',N''19''),(N''05632'',N''Xã Hợp Thành'',N''19''),(N''05641'',N''Xã Vô Tranh'',N''19''),(N''05662'',N''Xã Trại Cau'',N''19''),(N''05665'',N''Xã Văn Lăng'',N''19''),(N''05674'',N''Xã Quang Sơn'',N''19''),(N''05680'',N''Xã Văn Hán'',N''19''),(N''05692'',N''Xã Đồng Hỷ'',N''19''),(N''05707'',N''Xã Nam Hòa'',N''19''),(N''05710'',N''Phường Linh Sơn'',N''19''),(N''05716'',N''Xã Võ Nhai'',N''19''),(N''05719'',N''Xã Sảng Mộc'',N''19''),(N''05722'',N''Xã Nghinh Tường'',N''19''),(N''05725'',N''Xã Thần Sa'',N''19''),(N''05740'',N''Xã La Hiên'',N''19''),(N''05746'',N''Xã Tràng Xá'',N''19''),(N''05755'',N''Xã Dân Tiến'',N''19''),(N''05773'',N''Xã Phú Xuyên'',N''19''),(N''05776'',N''Xã Đức Lương'',N''19''),(N''05788'',N''Xã Phú Lạc'',N''19''),(N''05800'',N''Xã Phú Thịnh'',N''19''),(N''05809'',N''Xã An Khánh'',N''19''),(N''05818'',N''Xã La Bằng'',N''19''),(N''05830'',N''Xã Đại Từ'',N''19''),(N''05845'',N''Xã Vạn Phú'',N''19''),(N''05851'',N''Xã Quân Chu'',N''19''),(N''05857'',N''Phường Phúc Thuận'',N''19''),(N''05860'',N''Phường Phổ Yên'',N''19''),(N''05881'',N''Xã Thành Công'',N''19''),(N''05890'',N''Phường Vạn Xuân'',N''19''),(N''05899'',N''Phường Trung Thành'',N''19''),(N''05908'',N''Xã Phú Bình'',N''19''),(N''05917'',N''Xã Tân Khánh'',N''19''),(N''05923'',N''Xã Tân Thành'',N''19''),(N''05941'',N''Xã Điềm Thụy'',N''19''),(N''05953'',N''Xã Kha Sơn'',N''19''),(N''05977'',N''Phường Đông Kinh'',N''20''),(N''05983'',N''Phường Lương Văn Tri'',N''20''),(N''05986'',N''Phường Tam Thanh'',N''20''),(N''06001'',N''Xã Đoàn Kết'',N''20''),(N''06004'',N''Xã Quốc Khánh'',N''20''),(N''06019'',N''Xã Tân Tiến'',N''20''),(N''06037'',N''Xã Kháng Chiến'',N''20''),(N''06040'',N''Xã Thất Khê'',N''20''),(N''06046'',N''Xã Tràng Định'',N''20''),(N''06058'',N''Xã Quốc Việt'',N''20''),(N''06073'',N''Xã Hoa Thám'',N''20''),(N''06076'',N''Xã Quý Hòa'',N''20''),(N''06079'',N''Xã Hồng Phong'',N''20''),(N''06085'',N''Xã Thiện Hòa'',N''20''),(N''06091'',N''Xã Thiện Thuật'',N''20''),(N''06103'',N''Xã Thiện Long'',N''20''),(N''06112'',N''Xã Bình Gia'',N''20''),(N''06115'',N''Xã Tân Văn'',N''20''),(N''06124'',N''Xã Na Sầm'',N''20''),(N''06148'',N''Xã Thụy Hùng'',N''20''),(N''06151'',N''Xã Hội Hoan'',N''20''),(N''06154'',N''Xã Văn Lãng'',N''20''),(N''06172'',N''Xã Hoàng Văn Thụ'',N''20''),(N''06184'',N''Xã Đồng Đăng'',N''20''),(N''06187'',N''Phường Kỳ Lừa'',N''20''),(N''06196'',N''Xã Ba Sơn'',N''20''),(N''06211'',N''Xã Cao Lộc'',N''20''),(N''06220'',N''Xã Công Sơn'',N''20''),(N''06253'',N''Xã Văn Quan'',N''20''),(N''06280'',N''Xã Điềm He'',N''20''),(N''06286'',N''Xã Khánh Khê'',N''20''),(N''06298'',N''Xã Yên Phúc'',N''20''),(N''06313'',N''Xã Tri Lễ'',N''20''),(N''06316'',N''Xã Tân Đoàn'',N''20''),(N''06325'',N''xã Bắc Sơn'',N''20''),(N''06337'',N''Xã Tân Tri'',N''20''),(N''06349'',N''Xã Hưng Vũ'',N''20''),(N''06364'',N''Xã Vũ Lễ'',N''20''),(N''06367'',N''Xã Vũ Lăng'',N''20''),(N''06376'',N''Xã Nhất Hòa'',N''20''),(N''06385'',N''Xã Hữu Lũng'',N''20''),(N''06391'',N''Xã Yên Bình'',N''20''),(N''06400'',N''Xã Hữu Liên'',N''20''),(N''06415'',N''Xã Vân Nham'',N''20''),(N''06427'',N''Xã Cai Kinh'',N''20''),(N''06436'',N''Xã Thiện Tân'',N''20''),(N''06445'',N''Xã Tân Thành'',N''20''),(N''06457'',N''Xã Tuấn Sơn'',N''20''),(N''06463'',N''Xã Chi Lăng'',N''20''),(N''06475'',N''Xã Bằng Mạc'',N''20''),(N''06481'',N''Xã Chiến Thắng'',N''20''),(N''06496'',N''Xã Nhân Lý'',N''20''),(N''06505'',N''Xã Vạn Linh'',N''20''),(N''06517'',N''Xã Quan Sơn'',N''20''),(N''06526'',N''Xã Na Dương'',N''20''),(N''06529'',N''Xã Lộc Bình'',N''20''),(N''06541'',N''Xã Mẫu Sơn'',N''20''),(N''06565'',N''Xã Khuất Xá'',N''20''),(N''06577'',N''Xã Thống Nhất'',N''20''),(N''06601'',N''Xã Lợi Bác'',N''20''),(N''06607'',N''Xã Xuân Dương'',N''20''),(N''06613'',N''Xã Đình Lập'',N''20''),(N''06616'',N''Xã Thái Bình'',N''20''),(N''06625'',N''Xã Kiên Mộc'',N''20''),(N''06637'',N''Xã Châu Sơn'',N''20''),(N''06652'',N''Phường Hà Tu'',N''22''),(N''06658'',N''Phường Cao Xanh'',N''22''),(N''06661'',N''Phường Việt Hưng'',N''22''),(N''06673'',N''Phường Bãi Cháy'',N''22''),(N''06676'',N''Phường Hà Lầm'',N''22''),(N''06685'',N''Phường Hồng Gai'',N''22''),(N''06688'',N''Phường Hạ Long'',N''22''),(N''06706'',N''Phường Tuần Châu'',N''22''),(N''06709'',N''Phường Móng Cái 2'',N''22''),(N''06712'',N''Phường Móng Cái 1'',N''22''),(N''06724'',N''Xã Hải Sơn'',N''22''),(N''06733'',N''Xã Hải Ninh'',N''22''),(N''06736'',N''Phường Móng Cái 3'',N''22''),(N''06757'',N''Xã Vĩnh Thực'',N''22''),(N''06760'',N''Phường Mông Dương'',N''22''),(N''06778'',N''Phường Quang Hanh'',N''22''),(N''06781'',N''Phường Cửa Ông'',N''22''),(N''06793'',N''Phường Cẩm Phả'',N''22''),(N''06799'',N''Xã Hải Hòa'',N''22''),(N''06811'',N''Phường Uông Bí'',N''22''),(N''06820'',N''Phường Vàng Danh'',N''22''),(N''06832'',N''Phường Yên Tử'',N''22''),(N''06838'',N''Xã Bình Liêu'',N''22''),(N''06841'',N''Xã Hoành Mô'',N''22''),(N''06856'',N''Xã Lục Hồn'',N''22''),(N''06862'',N''Xã Tiên Yên'',N''22''),(N''06874'',N''Xã Điền Xá'',N''22''),(N''06877'',N''Xã Đông Ngũ'',N''22''),(N''06886'',N''Xã Hải Lạng'',N''22''),(N''06895'',N''Xã Đầm Hà'',N''22''),(N''06913'',N''Xã Quảng Tân'',N''22''),(N''06922'',N''Xã Quảng Hà'',N''22''),(N''06931'',N''Xã Quảng Đức'',N''22''),(N''06946'',N''Xã Đường Hoa'',N''22''),(N''06967'',N''Xã Cái Chiên'',N''22''),(N''06970'',N''Xã Ba Chẽ'',N''22''),(N''06979'',N''Xã Kỳ Thượng'',N''22''),(N''06985'',N''Xã Lương Minh'',N''22''),(N''06994'',N''Đặc khu Vân Đồn'',N''22''),(N''07030'',N''Phường Hoành Bồ'',N''22''),(N''07054'',N''Xã Quảng La'',N''22''),(N''07060'',N''Xã Thống Nhất'',N''22''),(N''07069'',N''Phường Mạo Khê'',N''22''),(N''07081'',N''Phường Bình Khê'',N''22''),(N''07090'',N''Phường An Sinh'',N''22''),(N''07093'',N''Phường Đông Triều'',N''22''),(N''07114'',N''Phường Hoàng Quế'',N''22''),(N''07132'',N''Phường Quảng Yên'',N''22''),(N''07135'',N''Phường Đông Mai'',N''22''),(N''07147'',N''Phường Hiệp Hòa'',N''22''),(N''07168'',N''Phường Hà An'',N''22''),(N''07180'',N''Phường Liên Hòa'',N''22''),(N''07183'',N''Phường Phong Cốc'',N''22''),(N''07192'',N''Đặc khu Cô Tô'',N''22''),(N''07210'',N''Phường Bắc Giang'',N''24''),(N''07228'',N''Phường Đa Mai'',N''24''),(N''07246'',N''Xã Xuân Lương'',N''24''),(N''07264'',N''Xã Tam Tiến'',N''24''),(N''07282'',N''Xã Đồng Kỳ'',N''24''),(N''07288'',N''Xã Yên Thế'',N''24''),(N''07294'',N''Phường Bố Hạ'',N''24''),(N''07306'',N''Xã Nhã Nam'',N''24''),(N''07330'',N''Xã Phúc Hòa'',N''24''),(N''07333'',N''Xã Quang Trung'',N''24''),(N''07339'',N''Xã Tân Yên'',N''24''),(N''07351'',N''Xã Ngọc Thiện'',N''24''),(N''07375'',N''Phường Lạng Giang'',N''24''),(N''07381'',N''Xã Tiên Lục'',N''24''),(N''07399'',N''Phường Kép'',N''24''),(N''07420'',N''Xã Mỹ Thái'',N''24''),(N''07432'',N''Xã Tân Dĩnh'',N''24''),(N''07444'',N''Phường Lục Nam'',N''24''),(N''07450'',N''Xã Đông Phú'',N''24''),(N''07462'',N''Xã Bảo Đài'',N''24''),(N''07486'',N''Xã Nghĩa Phương'',N''24''),(N''07489'',N''Xã Trường Sơn'',N''24''),(N''07492'',N''Xã Lục Sơn'',N''24''),(N''07498'',N''Xã Bắc Lũng'',N''24''),(N''07519'',N''Xã Cẩm Lý'',N''24''),(N''07525'',N''Phường Chũ'',N''24'')) s(ma,ten,tinh) JOIN dbo.tinh_thanh p ON p.ma_hanh_chinh=s.tinh WHERE NOT EXISTS(SELECT 1 FROM dbo.phuong_xa w WHERE w.ma=s.ma);');
 EXEC(N'INSERT dbo.phuong_xa(ma,ten,tinh_thanh_id) SELECT s.ma,s.ten,p.id FROM (VALUES(N''07531'',N''Xã Tân Sơn'',N''24''),(N''07534'',N''Xã Sa Lý'',N''24''),(N''07537'',N''Xã Biên Sơn'',N''24''),(N''07543'',N''Xã Sơn Hải'',N''24''),(N''07552'',N''Xã Kiên Lao'',N''24''),(N''07573'',N''Xã Biển Động'',N''24''),(N''07582'',N''Xã Lục Ngạn'',N''24''),(N''07594'',N''Xã Đèo Gia'',N''24''),(N''07603'',N''Xã Nam Dương'',N''24''),(N''07612'',N''Phường Phượng Sơn'',N''24''),(N''07615'',N''Xã Sơn Động'',N''24''),(N''07616'',N''Xã Tây Yên Tử'',N''24''),(N''07621'',N''Xã Vân Sơn'',N''24''),(N''07627'',N''Xã Đại Sơn'',N''24''),(N''07642'',N''Xã Yên Định'',N''24''),(N''07654'',N''Xã An Lạc'',N''24''),(N''07663'',N''Xã Tuấn Đạo'',N''24''),(N''07672'',N''Xã Dương Hưu'',N''24''),(N''07681'',N''Phường Yên Dũng'',N''24''),(N''07682'',N''Phường Tân An'',N''24''),(N''07696'',N''Phường Tiền Phong'',N''24''),(N''07699'',N''Phường Tân Tiến'',N''24''),(N''07735'',N''Xã Đồng Việt'',N''24''),(N''07738'',N''Phường Cảnh Thụy'',N''24''),(N''07774'',N''Phường Tự Lạn'',N''24''),(N''07777'',N''Phường Việt Yên'',N''24''),(N''07795'',N''Phường Nếnh'',N''24''),(N''07798'',N''Phường Vân Hà'',N''24''),(N''07822'',N''Xã Hoàng Vân'',N''24''),(N''07840'',N''Phường Hiệp Hoà'',N''24''),(N''07864'',N''Xã Hợp Thịnh'',N''24''),(N''07870'',N''Xã Xuân Cẩm'',N''24''),(N''09169'',N''Phường Vũ Ninh'',N''24''),(N''09187'',N''Phường Kinh Bắc'',N''24''),(N''09190'',N''Phường Võ Cường'',N''24''),(N''09193'',N''Phường Yên Phong'',N''24''),(N''09202'',N''Xã Tam Giang'',N''24''),(N''09205'',N''Xã Yên Trung'',N''24''),(N''09208'',N''Xã Tam Đa'',N''24''),(N''09238'',N''Xã Văn Môn'',N''24''),(N''09247'',N''Phường Quế Võ'',N''24''),(N''09253'',N''Phường Nhân Hòa'',N''24''),(N''09265'',N''Phường Phương Liễu'',N''24''),(N''09286'',N''Phường Nam Sơn'',N''24''),(N''09292'',N''Phường Phù Lãng'',N''24''),(N''09295'',N''Phường Bồng Lai'',N''24''),(N''09301'',N''Phường Đào Viên'',N''24''),(N''09313'',N''Phường Chi Lăng'',N''24''),(N''09319'',N''Phường Tiên Du'',N''24''),(N''09325'',N''Phường Hạp Lĩnh'',N''24''),(N''09334'',N''Xã Liên Bão'',N''24''),(N''09340'',N''Xã Đại Đồng'',N''24''),(N''09343'',N''Xã Tân Chi'',N''24''),(N''09349'',N''Xã Phật Tích'',N''24''),(N''09367'',N''Phường Từ Sơn'',N''24''),(N''09370'',N''Phường Tam Sơn'',N''24''),(N''09379'',N''Phường Phù Khê'',N''24''),(N''09385'',N''Phường Đồng Nguyên'',N''24''),(N''09400'',N''Phường Thuận Thành'',N''24''),(N''09409'',N''Phường Mão Điền'',N''24''),(N''09427'',N''Phường Trí Quả'',N''24''),(N''09430'',N''Phường Trạm Lộ'',N''24''),(N''09433'',N''Phường Song Liễu'',N''24''),(N''09445'',N''Phường Ninh Xá'',N''24''),(N''09454'',N''Phường Gia Bình'',N''24''),(N''09466'',N''Xã Cao Đức'',N''24''),(N''09469'',N''Xã Đại Lai'',N''24''),(N''09475'',N''Phường Nhân Thắng'',N''24''),(N''09487'',N''Xã Đông Cứu'',N''24''),(N''09496'',N''Phường Lương Tài'',N''24''),(N''09499'',N''Xã Trung Kênh'',N''24''),(N''09523'',N''Xã Trung Chính'',N''24''),(N''09529'',N''Xã Lâm Thao'',N''24''),(N''04792'',N''Phường Tân Hòa'',N''25''),(N''04795'',N''Phường Hòa Bình'',N''25''),(N''04828'',N''Phường Thống Nhất'',N''25''),(N''04831'',N''Xã Đà Bắc'',N''25''),(N''04846'',N''Xã Đức Nhàn'',N''25''),(N''04849'',N''Xã Tân Pheo'',N''25''),(N''04873'',N''Xã Quy Đức'',N''25''),(N''04876'',N''Xã Cao Sơn'',N''25''),(N''04891'',N''Xã Tiền Phong'',N''25''),(N''04894'',N''Phường Kỳ Sơn'',N''25''),(N''04897'',N''Xã Thịnh Minh'',N''25''),(N''04924'',N''Xã Lương Sơn'',N''25''),(N''04960'',N''Xã Liên Sơn'',N''25''),(N''04978'',N''Xã Kim Bôi'',N''25''),(N''04990'',N''Xã Nật Sơn'',N''25''),(N''05014'',N''Xã Mường Động'',N''25''),(N''05047'',N''Xã Cao Dương'',N''25''),(N''05068'',N''Xã Hợp Kim'',N''25''),(N''05086'',N''Xã Dũng Tiến'',N''25''),(N''05089'',N''Xã Cao Phong'',N''25''),(N''05092'',N''Xã Thung Nai'',N''25''),(N''05116'',N''Xã Mường Thàng'',N''25''),(N''05128'',N''Xã Tân Lạc'',N''25''),(N''05134'',N''Xã Mường Hoa'',N''25''),(N''05152'',N''Xã Vân Sơn'',N''25''),(N''05158'',N''Xã Mường Bi'',N''25''),(N''05191'',N''Xã Toàn Thắng'',N''25''),(N''05200'',N''Xã Mai Châu'',N''25''),(N''05206'',N''Xã Tân Mai'',N''25''),(N''05212'',N''Xã Pà Cò'',N''25''),(N''05245'',N''Xã Bao La'',N''25''),(N''05251'',N''Xã Mai Hạ'',N''25''),(N''05266'',N''Xã Lạc Sơn'',N''25''),(N''05287'',N''Xã Mường Vang'',N''25''),(N''05290'',N''Xã Nhân Nghĩa'',N''25''),(N''05293'',N''Xã Thượng Cốc'',N''25''),(N''05305'',N''Xã Yên Phú'',N''25''),(N''05323'',N''Xã Quyết Thắng'',N''25''),(N''05329'',N''Xã Ngọc Sơn'',N''25''),(N''05347'',N''Xã Đại Đồng'',N''25''),(N''05353'',N''Xã Yên Thủy'',N''25''),(N''05362'',N''Xã Lạc Lương'',N''25''),(N''05386'',N''Xã Yên Trị'',N''25''),(N''05392'',N''Xã Lạc Thủy'',N''25''),(N''05395'',N''Xã An Nghĩa'',N''25''),(N''05425'',N''Xã An Bình'',N''25''),(N''07894'',N''Phường Nông Trang'',N''25''),(N''07900'',N''Phường Việt Trì'',N''25''),(N''07909'',N''Phường Thanh Miếu'',N''25''),(N''07918'',N''Phường Vân Phú'',N''25''),(N''07942'',N''Phường Phú Thọ'',N''25''),(N''07948'',N''Phường Âu Cơ'',N''25''),(N''07954'',N''Phường Phong Châu'',N''25''),(N''07969'',N''Xã Đoan Hùng'',N''25''),(N''07996'',N''Xã Bằng Luân'',N''25''),(N''07999'',N''Xã Chí Đám'',N''25''),(N''08023'',N''Xã Tây Cốc'',N''25''),(N''08038'',N''Xã Chân Mộng'',N''25''),(N''08053'',N''Xã Hạ Hòa'',N''25''),(N''08071'',N''Xã Đan Thượng'',N''25''),(N''08110'',N''Xã Hiền Lương'',N''25''),(N''08113'',N''Xã Yên Kỳ'',N''25''),(N''08134'',N''Xã Văn Lang'',N''25''),(N''08143'',N''Xã Vĩnh Chân'',N''25''),(N''08152'',N''Xã Thanh Ba'',N''25''),(N''08173'',N''Xã Quảng Yên'',N''25''),(N''08203'',N''Xã Hoàng Cương'',N''25''),(N''08209'',N''Xã Đông Thành'',N''25''),(N''08218'',N''Xã Chí Tiên'',N''25''),(N''08227'',N''Xã Liên Minh'',N''25''),(N''08230'',N''Xã Phù Ninh'',N''25''),(N''08236'',N''Xã Phú Mỹ'',N''25''),(N''08245'',N''Xã Trạm Thản'',N''25''),(N''08254'',N''Xã Dân Chủ'',N''25''),(N''08275'',N''Xã Bình Phú'',N''25''),(N''08290'',N''Xã Yên Lập'',N''25''),(N''08296'',N''Xã Sơn Lương'',N''25''),(N''08305'',N''Xã Xuân Viên'',N''25''),(N''08311'',N''Xã Trung Sơn'',N''25''),(N''08323'',N''Xã Thượng Long'',N''25''),(N''08338'',N''Xã Minh Hòa'',N''25''),(N''08341'',N''Xã Cẩm Khê'',N''25''),(N''08344'',N''Xã Tiên Lương'',N''25''),(N''08377'',N''Xã Vân Bán'',N''25''),(N''08398'',N''Xã Phú Khê'',N''25''),(N''08416'',N''Xã Hùng Việt'',N''25''),(N''08431'',N''Xã Đồng Lương'',N''25''),(N''08434'',N''Xã Tam Nông'',N''25''),(N''08443'',N''Xã Hiền Quan'',N''25''),(N''08467'',N''Xã Vạn Xuân'',N''25''),(N''08479'',N''Xã Thọ Văn'',N''25''),(N''08494'',N''Xã Lâm Thao'',N''25''),(N''08500'',N''Xã Xuân Lũng'',N''25''),(N''08515'',N''Xã Hy Cương'',N''25''),(N''08521'',N''Xã Phùng Nguyên'',N''25''),(N''08527'',N''Xã Bản Nguyên'',N''25''),(N''08542'',N''Xã Thanh Sơn'',N''25''),(N''08545'',N''Xã Thu Cúc'',N''25''),(N''08560'',N''Xã Lai Đồng'',N''25''),(N''08566'',N''Xã Tân Sơn'',N''25''),(N''08584'',N''Xã Võ Miếu'',N''25''),(N''08590'',N''Xã Xuân Đài'',N''25''),(N''08593'',N''Xã Minh Đài'',N''25''),(N''08611'',N''Xã Văn Miếu'',N''25''),(N''08614'',N''Xã Cự Đồng'',N''25''),(N''08620'',N''Xã Long Cốc'',N''25''),(N''08632'',N''Xã Hương Cần'',N''25''),(N''08635'',N''Xã Khả Cửu'',N''25''),(N''08656'',N''Xã Yên Sơn'',N''25''),(N''08662'',N''Xã Đào Xá'',N''25''),(N''08674'',N''Xã Thanh Thủy'',N''25''),(N''08686'',N''Xã Tu Vũ'',N''25''),(N''08707'',N''Phường Vĩnh Yên'',N''25''),(N''08716'',N''Phường Vĩnh Phúc'',N''25''),(N''08740'',N''Phường Phúc Yên'',N''25''),(N''08746'',N''Phường Xuân Hòa'',N''25''),(N''08761'',N''Xã Lập Thạch'',N''25''),(N''08770'',N''Xã Hợp Lý'',N''25''),(N''08773'',N''Xã Yên Lãng'',N''25''),(N''08782'',N''Xã Hải Lựu'',N''25''),(N''08788'',N''Xã Thái Hòa'',N''25''),(N''08812'',N''Xã Liên Hòa'',N''25''),(N''08824'',N''Xã Tam Sơn'',N''25''),(N''08842'',N''Xã Tiên Lữ'',N''25''),(N''08848'',N''Xã Sông Lô'',N''25''),(N''08866'',N''Xã Sơn Đông'',N''25''),(N''08869'',N''Xã Tam Dương'',N''25'')) s(ma,ten,tinh) JOIN dbo.tinh_thanh p ON p.ma_hanh_chinh=s.tinh WHERE NOT EXISTS(SELECT 1 FROM dbo.phuong_xa w WHERE w.ma=s.ma);');
 EXEC(N'INSERT dbo.phuong_xa(ma,ten,tinh_thanh_id) SELECT s.ma,s.ten,p.id FROM (VALUES(N''08872'',N''Xã Tam Dương Bắc'',N''25''),(N''08896'',N''Xã Hoàng An'',N''25''),(N''08905'',N''Xã Hội Thịnh'',N''25''),(N''08911'',N''Xã Tam Đảo'',N''25''),(N''08914'',N''Xã Đạo Trù'',N''25''),(N''08923'',N''Xã Đại Đình'',N''25''),(N''08935'',N''Xã Bình Nguyên'',N''25''),(N''08944'',N''Xã Bình Tuyền'',N''25''),(N''08950'',N''Xã Bình Xuyên'',N''25''),(N''08971'',N''Xã Xuân Lãng'',N''25''),(N''09025'',N''Xã Yên Lạc'',N''25''),(N''09040'',N''Xã Tề Lỗ'',N''25''),(N''09043'',N''Xã Tam Hồng'',N''25''),(N''09052'',N''Xã Nguyệt Đức'',N''25''),(N''09064'',N''Xã Liên Châu'',N''25''),(N''09076'',N''Xã Vĩnh Tường'',N''25''),(N''09079'',N''Xã Vĩnh An'',N''25''),(N''09100'',N''Xã Vĩnh Hưng'',N''25''),(N''09106'',N''Xã Vĩnh Thành'',N''25''),(N''09112'',N''Xã Thổ Tang'',N''25''),(N''09154'',N''Xã Vĩnh Phú'',N''25''),(N''10507'',N''Phường Thành Đông'',N''31''),(N''10525'',N''Phường Hải Dương'',N''31''),(N''10532'',N''Phường Lê Thanh Nghị'',N''31''),(N''10537'',N''Phường Tân Hưng'',N''31''),(N''10543'',N''Phường Việt Hòa'',N''31''),(N''10546'',N''Phường Chí Linh'',N''31''),(N''10549'',N''Phường Chu Văn An'',N''31''),(N''10552'',N''Phường Nguyễn Trãi'',N''31''),(N''10570'',N''Phường Trần Hưng Đạo'',N''31''),(N''10573'',N''Phường Trần Nhân Tông'',N''31''),(N''10603'',N''Phường Lê Đại Hành'',N''31''),(N''10606'',N''Xã Nam Sách'',N''31''),(N''10615'',N''Xã Hợp Tiến'',N''31''),(N''10633'',N''Xã Trần Phú'',N''31''),(N''10642'',N''Xã Thái Tân'',N''31''),(N''10645'',N''Xã An Phú'',N''31''),(N''10660'',N''Phường Ái Quốc'',N''31''),(N''10675'',N''Phường Kinh Môn'',N''31''),(N''10678'',N''Phường Bắc An Phụ'',N''31''),(N''10705'',N''Xã Nam An Phụ'',N''31''),(N''10714'',N''Phường Nhị Chiểu'',N''31''),(N''10726'',N''Phường Phạm Sư Mạnh'',N''31''),(N''10729'',N''Phường Trần Liễu'',N''31''),(N''10744'',N''Phường Nguyễn Đại Năng'',N''31''),(N''10750'',N''Xã Phú Thái'',N''31''),(N''10756'',N''Xã Lai Khê'',N''31''),(N''10792'',N''Xã An Thành'',N''31''),(N''10804'',N''Xã Kim Thành'',N''31''),(N''10813'',N''Xã Thanh Hà'',N''31''),(N''10816'',N''Xã Hà Bắc'',N''31''),(N''10837'',N''Phường Nam Đồng'',N''31''),(N''10843'',N''Xã Hà Nam'',N''31''),(N''10846'',N''Xã Hà Tây'',N''31''),(N''10882'',N''Xã Hà Đông'',N''31''),(N''10888'',N''Xã Cẩm Giang'',N''31''),(N''10891'',N''Phường Tứ Minh'',N''31''),(N''10903'',N''Xã Cẩm Giàng'',N''31''),(N''10909'',N''Xã Tuệ Tĩnh'',N''31''),(N''10930'',N''Xã Mao Điền'',N''31''),(N''10945'',N''Xã Kẻ Sặt'',N''31''),(N''10966'',N''Xã Bình Giang'',N''31''),(N''10972'',N''Xã Đường An'',N''31''),(N''10993'',N''Xã Thượng Hồng'',N''31''),(N''10999'',N''Xã Gia Lộc'',N''31''),(N''11002'',N''Phường Thạch Khôi'',N''31''),(N''11020'',N''Xã Yết Kiêu'',N''31''),(N''11050'',N''Xã Gia Phúc'',N''31''),(N''11065'',N''Xã Trường Tân'',N''31''),(N''11074'',N''Xã Tứ Kỳ'',N''31''),(N''11086'',N''Xã Đại Sơn'',N''31''),(N''11113'',N''Xã Tân Kỳ'',N''31''),(N''11131'',N''Xã Chí Minh'',N''31''),(N''11140'',N''Xã Lạc Phượng'',N''31''),(N''11146'',N''Xã Nguyên Giáp'',N''31''),(N''11164'',N''Xã Vĩnh Lại'',N''31''),(N''11167'',N''Xã Tân An'',N''31''),(N''11203'',N''Xã Ninh Giang'',N''31''),(N''11218'',N''Xã Hồng Châu'',N''31''),(N''11224'',N''Xã Khúc Thừa Dụ'',N''31''),(N''11239'',N''Xã Thanh Miện'',N''31''),(N''11242'',N''Xã Nguyễn Lương Bằng'',N''31''),(N''11254'',N''Xã Bắc Thanh Miện'',N''31''),(N''11257'',N''Xã Hải Hưng'',N''31''),(N''11284'',N''Xã Nam Thanh Miện'',N''31''),(N''11311'',N''Phường Hồng Bàng'',N''31''),(N''11329'',N''Phường Ngô Quyền'',N''31''),(N''11359'',N''Phường Gia Viên'',N''31''),(N''11383'',N''Phường Lê Chân'',N''31''),(N''11407'',N''Phường An Biên'',N''31''),(N''11411'',N''Phường Đông Hải'',N''31''),(N''11413'',N''Phường Hải An'',N''31''),(N''11443'',N''Phường Kiến An'',N''31''),(N''11446'',N''Phường Phù Liễn'',N''31''),(N''11455'',N''Phường Đồ Sơn'',N''31''),(N''11473'',N''Phường Bạch Đằng'',N''31''),(N''11488'',N''Phường Lưu Kiếm'',N''31''),(N''11503'',N''Xã Việt Khê'',N''31''),(N''11506'',N''Phường Lê Ích Mộc'',N''31''),(N''11533'',N''Phường Hòa Bình'',N''31''),(N''11542'',N''Phường Nam Triệu'',N''31''),(N''11557'',N''Phường Thiên Hương'',N''31''),(N''11560'',N''Phường Thủy Nguyên'',N''31''),(N''11581'',N''Phường An Dương'',N''31''),(N''11593'',N''Phường An Phong'',N''31''),(N''11602'',N''Phường Hồng An'',N''31''),(N''11617'',N''Phường An Hải'',N''31''),(N''11629'',N''Xã An Lão'',N''31''),(N''11635'',N''Xã An Trường'',N''31''),(N''11647'',N''Xã An Quang'',N''31''),(N''11668'',N''Xã An Khánh'',N''31''),(N''11674'',N''Xã An Hưng'',N''31''),(N''11680'',N''Xã Kiến Thụy'',N''31''),(N''11689'',N''Phường Hưng Đạo'',N''31''),(N''11692'',N''Phường Dương Kinh'',N''31''),(N''11713'',N''Xã Nghi Dương'',N''31''),(N''11725'',N''Xã Kiến Minh'',N''31''),(N''11728'',N''Xã Kiến Hưng'',N''31''),(N''11737'',N''Phường Nam Đồ Sơn'',N''31''),(N''11749'',N''Xã Kiến Hải'',N''31''),(N''11755'',N''Xã Tiên Lãng'',N''31''),(N''11761'',N''Xã Quyết Thắng'',N''31''),(N''11779'',N''Xã Tân Minh'',N''31''),(N''11791'',N''Xã Tiên Minh'',N''31''),(N''11806'',N''Xã Chấn Hưng'',N''31''),(N''11809'',N''Xã Hùng Thắng'',N''31''),(N''11824'',N''Xã Vĩnh Bảo'',N''31''),(N''11836'',N''Xã Vĩnh Thịnh'',N''31''),(N''11842'',N''Xã Vĩnh Thuận'',N''31''),(N''11848'',N''Xã Vĩnh Hòa'',N''31''),(N''11875'',N''Xã Vĩnh Hải'',N''31''),(N''11887'',N''Xã Vĩnh Am'',N''31''),(N''11911'',N''Xã Nguyễn Bỉnh Khiêm'',N''31''),(N''11914'',N''Đặc khu Cát Hải'',N''31''),(N''11948'',N''Đặc khu Bạch Long Vĩ'',N''31''),(N''11953'',N''Phường Phố Hiến'',N''33''),(N''11977'',N''Xã Tân Hưng'',N''33''),(N''11980'',N''Phường Hồng Châu'',N''33''),(N''11983'',N''Phường Sơn Nam'',N''33''),(N''11992'',N''Xã Lạc Đạo'',N''33''),(N''11995'',N''Xã Đại Đồng'',N''33''),(N''12004'',N''Xã Như Quỳnh'',N''33''),(N''12019'',N''Xã Văn Giang'',N''33''),(N''12025'',N''Xã Phụng Công'',N''33''),(N''12031'',N''Xã Nghĩa Trụ'',N''33''),(N''12049'',N''Xã Mễ Sở'',N''33''),(N''12064'',N''Xã Nguyễn Văn Linh'',N''33''),(N''12070'',N''Xã Hoàn Long'',N''33''),(N''12073'',N''Xã Yên Mỹ'',N''33''),(N''12091'',N''Xã Việt Yên'',N''33''),(N''12103'',N''Phường Mỹ Hào'',N''33''),(N''12127'',N''Phường Thượng Hồng'',N''33''),(N''12133'',N''Phường Đường Hào'',N''33''),(N''12142'',N''Xã Ân Thi'',N''33''),(N''12148'',N''Xã Phạm Ngũ Lão'',N''33''),(N''12166'',N''Xã Xuân Trúc'',N''33''),(N''12184'',N''Xã Nguyễn Trãi'',N''33''),(N''12196'',N''Xã Hồng Quang'',N''33''),(N''12205'',N''Xã Khoái Châu'',N''33''),(N''12223'',N''Xã Triệu Việt Vương'',N''33''),(N''12238'',N''Xã Việt Tiến'',N''33''),(N''12247'',N''Xã Châu Ninh'',N''33''),(N''12271'',N''Xã Chí Minh'',N''33''),(N''12280'',N''Xã Lương Bằng'',N''33''),(N''12286'',N''Xã Nghĩa Dân'',N''33''),(N''12313'',N''Xã Đức Hợp'',N''33''),(N''12322'',N''Xã Hiệp Cường'',N''33''),(N''12337'',N''Xã Hoàng Hoa Thám'',N''33''),(N''12361'',N''Xã Tiên Hoa'',N''33''),(N''12364'',N''Xã Tiên Lữ'',N''33''),(N''12391'',N''Xã Quang Hưng'',N''33''),(N''12406'',N''Xã Đoàn Đào'',N''33''),(N''12424'',N''Xã Tiên Tiến'',N''33''),(N''12427'',N''Xã Tống Trân'',N''33''),(N''12452'',N''Phường Trần Hưng Đạo'',N''33''),(N''12454'',N''Phường Trần Lãm'',N''33''),(N''12466'',N''Phường Vũ Phúc'',N''33''),(N''12472'',N''Xã Quỳnh Phụ'',N''33''),(N''12499'',N''Xã A Sào'',N''33''),(N''12511'',N''Xã Minh Thọ'',N''33''),(N''12517'',N''Xã Ngọc Lâm'',N''33''),(N''12523'',N''Xã Phụ Dực'',N''33''),(N''12526'',N''Xã Đồng Bằng'',N''33''),(N''12532'',N''Xã Nguyễn Du'',N''33''),(N''12577'',N''Xã Quỳnh An'',N''33''),(N''12583'',N''Xã Tân Tiến'',N''33''),(N''12586'',N''Xã Hưng Hà'',N''33''),(N''12595'',N''Xã Ngự Thiên'',N''33''),(N''12613'',N''Xã Long Hưng'',N''33''),(N''12619'',N''Xã Diên Hà'',N''33''),(N''12631'',N''Xã Thần Khê'',N''33''),(N''12634'',N''Xã Tiên La'',N''33''),(N''12676'',N''Xã Lê Quý Đôn'',N''33''),(N''12685'',N''Xã Hồng Minh'',N''33''),(N''12688'',N''Xã Đông Hưng'',N''33''),(N''12694'',N''Xã Bắc Đông Hưng'',N''33''),(N''12700'',N''Xã Bắc Tiên Hưng'',N''33''),(N''12736'',N''Xã Đông Tiên Hưng'',N''33''),(N''12745'',N''Xã Bắc Đông Quan'',N''33''),(N''12754'',N''Xã Tiên Hưng'',N''33'')) s(ma,ten,tinh) JOIN dbo.tinh_thanh p ON p.ma_hanh_chinh=s.tinh WHERE NOT EXISTS(SELECT 1 FROM dbo.phuong_xa w WHERE w.ma=s.ma);');
 EXEC(N'INSERT dbo.phuong_xa(ma,ten,tinh_thanh_id) SELECT s.ma,s.ten,p.id FROM (VALUES(N''12763'',N''Xã Nam Tiên Hưng'',N''33''),(N''12775'',N''Xã Nam Đông Hưng'',N''33''),(N''12793'',N''Xã Đông Quan'',N''33''),(N''12817'',N''Phường Trà Lý'',N''33''),(N''12826'',N''Xã Thái Thụy'',N''33''),(N''12850'',N''Xã Tây Thụy Anh'',N''33''),(N''12859'',N''Xã Bắc Thụy Anh'',N''33''),(N''12862'',N''Xã Đông Thụy Anh'',N''33''),(N''12865'',N''Xã Thụy Anh'',N''33''),(N''12904'',N''Xã Nam Thụy Anh'',N''33''),(N''12916'',N''Xã Bắc Thái Ninh'',N''33''),(N''12919'',N''Xã Tây Thái Ninh'',N''33''),(N''12922'',N''Xã Thái Ninh'',N''33''),(N''12943'',N''Xã Đông Thái Ninh'',N''33''),(N''12961'',N''Xã Nam Thái Ninh'',N''33''),(N''12970'',N''Xã Tiền Hải'',N''33''),(N''12988'',N''Xã Đông Tiền Hải'',N''33''),(N''13003'',N''Xã Đồng Châu'',N''33''),(N''13021'',N''Xã Ái Quốc'',N''33''),(N''13039'',N''Xã Tây Tiền Hải'',N''33''),(N''13057'',N''Xã Nam Cường'',N''33''),(N''13063'',N''Xã Nam Tiền Hải'',N''33''),(N''13066'',N''Xã Hưng Phú'',N''33''),(N''13075'',N''Xã Kiến Xương'',N''33''),(N''13093'',N''Xã Trà Giang'',N''33''),(N''13096'',N''Xã Bình Nguyên'',N''33''),(N''13120'',N''Xã Lê Lợi'',N''33''),(N''13132'',N''Xã Quang Lịch'',N''33''),(N''13141'',N''Xã Vũ Quý'',N''33''),(N''13159'',N''Xã Hồng Vũ'',N''33''),(N''13183'',N''Xã Bình Thanh'',N''33''),(N''13186'',N''Xã Bình Định'',N''33''),(N''13192'',N''Xã Vũ Thư'',N''33''),(N''13219'',N''Xã Vạn Xuân'',N''33''),(N''13222'',N''Xã Thư Trì'',N''33''),(N''13225'',N''Phường Thái Bình'',N''33''),(N''13246'',N''Xã Tân Thuận'',N''33''),(N''13264'',N''Xã Thư Vũ'',N''33''),(N''13279'',N''Xã Vũ Tiên'',N''33''),(N''13285'',N''Phường Phủ Lý'',N''37''),(N''13291'',N''Phường Phù Vân'',N''37''),(N''13318'',N''Phường Châu Sơn'',N''37''),(N''13324'',N''Phường Duy Tiên'',N''37''),(N''13330'',N''Phường Duy Tân'',N''37''),(N''13336'',N''Phường Duy Hà'',N''37''),(N''13348'',N''Phường Đồng Văn'',N''37''),(N''13363'',N''Phường Tiên Sơn'',N''37''),(N''13366'',N''Phường Hà Nam'',N''37''),(N''13384'',N''Phường Kim Bảng'',N''37''),(N''13393'',N''Phường Lê Hồ'',N''37''),(N''13396'',N''Phường Nguyễn Uý'',N''37''),(N''13402'',N''Phường Kim Thanh'',N''37''),(N''13420'',N''Phường Tam Chúc'',N''37''),(N''13435'',N''Phường Lý Thường Kiệt'',N''37''),(N''13444'',N''Phường Liêm Tuyền'',N''37''),(N''13456'',N''Xã Liêm Hà'',N''37''),(N''13474'',N''Xã Tân Thanh'',N''37''),(N''13483'',N''Xã Thanh Bình'',N''37''),(N''13489'',N''Xã Thanh Lâm'',N''37''),(N''13495'',N''Xã Thanh Liêm'',N''37''),(N''13501'',N''Xã Bình Mỹ'',N''37''),(N''13504'',N''Xã Bình Lục'',N''37''),(N''13531'',N''Xã Bình Giang'',N''37''),(N''13540'',N''Xã Bình An'',N''37''),(N''13558'',N''Xã Bình Sơn'',N''37''),(N''13573'',N''Xã Lý Nhân'',N''37''),(N''13579'',N''Xã Bắc Lý'',N''37''),(N''13591'',N''Xã Nam Xang'',N''37''),(N''13594'',N''Xã Trần Thương'',N''37''),(N''13597'',N''Xã Vĩnh Trụ'',N''37''),(N''13609'',N''Xã Nhân Hà'',N''37''),(N''13627'',N''Xã Nam Lý'',N''37''),(N''13669'',N''Phường Nam Định'',N''37''),(N''13684'',N''Phường Thiên Trường'',N''37''),(N''13693'',N''Phường Đông A'',N''37''),(N''13699'',N''Phường Thành Nam'',N''37''),(N''13708'',N''Phường Mỹ Lộc'',N''37''),(N''13741'',N''Xã Vụ Bản'',N''37''),(N''13750'',N''Xã Minh Tân'',N''37''),(N''13753'',N''Xã Hiển Khánh'',N''37''),(N''13777'',N''Phường Trường Thi'',N''37''),(N''13786'',N''Xã Liên Minh'',N''37''),(N''13795'',N''Xã Ý Yên'',N''37''),(N''13807'',N''Xã Tân Minh'',N''37''),(N''13822'',N''Xã Phong Doanh'',N''37''),(N''13834'',N''Xã Vũ Dương'',N''37''),(N''13864'',N''Xã Vạn Thắng'',N''37''),(N''13870'',N''Xã Yên Cường'',N''37''),(N''13879'',N''Xã Yên Đồng'',N''37''),(N''13891'',N''Xã Nghĩa Hưng'',N''37''),(N''13894'',N''Xã Rạng Đông'',N''37''),(N''13900'',N''Xã Đồng Thịnh'',N''37''),(N''13918'',N''Xã Nghĩa Sơn'',N''37''),(N''13927'',N''Xã Hồng Phong'',N''37''),(N''13939'',N''Xã Quỹ Nhất'',N''37''),(N''13957'',N''Xã Nghĩa Lâm'',N''37''),(N''13966'',N''Xã Nam Trực'',N''37''),(N''13972'',N''Phường Vị Khê'',N''37''),(N''13984'',N''Phường Hồng Quang'',N''37''),(N''13987'',N''Xã Nam Hồng'',N''37''),(N''14005'',N''Xã Nam Ninh'',N''37''),(N''14011'',N''Xã Nam Minh'',N''37''),(N''14014'',N''Xã Nam Đồng'',N''37''),(N''14026'',N''Xã Cổ Lễ'',N''37''),(N''14038'',N''Xã Ninh Giang'',N''37''),(N''14053'',N''Xã Trực Ninh'',N''37''),(N''14056'',N''Xã Cát Thành'',N''37''),(N''14062'',N''Xã Quang Hưng'',N''37''),(N''14071'',N''Xã Minh Thái'',N''37''),(N''14077'',N''Xã Ninh Cường'',N''37''),(N''14089'',N''Xã Xuân Trường'',N''37''),(N''14095'',N''Xã Xuân Hồng'',N''37''),(N''14104'',N''Xã Xuân Giang'',N''37''),(N''14122'',N''Xã Xuân Hưng'',N''37''),(N''14161'',N''Xã Giao Minh'',N''37''),(N''14167'',N''Xã Giao Thuỷ'',N''37''),(N''14179'',N''Xã Giao Hưng'',N''37''),(N''14182'',N''Xã Giao Hoà'',N''37''),(N''14194'',N''Xã Giao Bình'',N''37''),(N''14203'',N''Xã Giao Phúc'',N''37''),(N''14212'',N''Xã Giao Ninh'',N''37''),(N''14215'',N''Xã Hải Hậu'',N''37''),(N''14218'',N''Xã Hải Tiến'',N''37''),(N''14221'',N''Xã Hải Thịnh'',N''37''),(N''14236'',N''Xã Hải Anh'',N''37''),(N''14248'',N''Xã Hải Hưng'',N''37''),(N''14281'',N''Xã Hải An'',N''37''),(N''14287'',N''Xã Hải Quang'',N''37''),(N''14308'',N''Xã Hải Xuân'',N''37''),(N''14329'',N''Phường Hoa Lư'',N''37''),(N''14359'',N''Phường Nam Hoa Lư'',N''37''),(N''14362'',N''Phường Tam Điệp'',N''37''),(N''14365'',N''Phường Trung Sơn'',N''37''),(N''14371'',N''Phường Yên Sơn'',N''37''),(N''14389'',N''Xã Gia Lâm'',N''37''),(N''14401'',N''Xã Gia Tường'',N''37''),(N''14404'',N''Xã Cúc Phương'',N''37''),(N''14407'',N''Xã Phú Sơn'',N''37''),(N''14428'',N''Xã Nho Quan'',N''37''),(N''14434'',N''Xã Thanh Sơn'',N''37''),(N''14452'',N''Xã Quỳnh Lưu'',N''37''),(N''14458'',N''Xã Phú Long'',N''37''),(N''14464'',N''Xã Gia Viễn'',N''37''),(N''14482'',N''Xã Gia Hưng'',N''37''),(N''14488'',N''Xã Gia Vân'',N''37''),(N''14494'',N''Xã Gia Trấn'',N''37''),(N''14500'',N''Xã Đại Hoàng'',N''37''),(N''14524'',N''Xã Gia Phong'',N''37''),(N''14533'',N''Phường Tây Hoa Lư'',N''37''),(N''14560'',N''Xã Yên Khánh'',N''37''),(N''14563'',N''Xã Khánh Thiện'',N''37''),(N''14566'',N''Phường Đông Hoa Lư'',N''37''),(N''14608'',N''Xã Khánh Trung'',N''37''),(N''14611'',N''Xã Khánh Nhạc'',N''37''),(N''14614'',N''Xã Khánh Hội'',N''37''),(N''14620'',N''Xã Phát Diệm'',N''37''),(N''14623'',N''Xã Bình Minh'',N''37''),(N''14638'',N''Xã Kim Sơn'',N''37''),(N''14647'',N''Xã Quang Thiện'',N''37''),(N''14653'',N''Xã Chất Bình'',N''37''),(N''14674'',N''Xã Lai Thành'',N''37''),(N''14677'',N''Xã Định Hóa'',N''37''),(N''14698'',N''Xã Kim Đông'',N''37''),(N''14701'',N''Xã Yên Mô'',N''37''),(N''14725'',N''Phường Yên Thắng'',N''37''),(N''14728'',N''Xã Yên Từ'',N''37''),(N''14743'',N''Xã Yên Mạc'',N''37''),(N''14746'',N''Xã Đồng Thái'',N''37''),(N''14758'',N''Phường Hàm Rồng'',N''38''),(N''14797'',N''Phường Hạc Thành'',N''38''),(N''14812'',N''Phường Bỉm Sơn'',N''38''),(N''14818'',N''Phường Quang Trung'',N''38''),(N''14845'',N''Xã Mường Lát'',N''38''),(N''14848'',N''Xã Tam Chung'',N''38''),(N''14854'',N''Xã Mường Lý'',N''38''),(N''14857'',N''Xã Trung Lý'',N''38''),(N''14860'',N''Xã Quang Chiểu'',N''38''),(N''14863'',N''Xã Pù Nhi'',N''38''),(N''14864'',N''Xã Nhi Sơn'',N''38''),(N''14866'',N''Xã Mường Chanh'',N''38''),(N''14869'',N''Xã Hồi Xuân'',N''38''),(N''14872'',N''Xã Trung Thành'',N''38''),(N''14875'',N''Xã Trung Sơn'',N''38''),(N''14878'',N''Xã Phú Lệ'',N''38''),(N''14890'',N''Xã Phú Xuân'',N''38''),(N''14896'',N''Xã Hiền Kiệt'',N''38''),(N''14902'',N''Xã Nam Xuân'',N''38''),(N''14908'',N''Xã Thiên Phủ'',N''38''),(N''14923'',N''Xã Bá Thước'',N''38''),(N''14932'',N''Xã Điền Quang'',N''38''),(N''14950'',N''Xã Điền Lư'',N''38''),(N''14953'',N''Xã Quý Lương'',N''38''),(N''14956'',N''Xã Pù Luông'',N''38''),(N''14959'',N''Xã Cổ Lũng'',N''38''),(N''14974'',N''Xã Văn Nho'',N''38''),(N''14980'',N''Xã Thiết Ống'',N''38''),(N''15001'',N''Xã Trung Hạ'',N''38''),(N''15007'',N''Xã Tam Thanh'',N''38''),(N''15010'',N''Xã Sơn Thủy'',N''38''),(N''15013'',N''Xã Na Mèo'',N''38'')) s(ma,ten,tinh) JOIN dbo.tinh_thanh p ON p.ma_hanh_chinh=s.tinh WHERE NOT EXISTS(SELECT 1 FROM dbo.phuong_xa w WHERE w.ma=s.ma);');
 EXEC(N'INSERT dbo.phuong_xa(ma,ten,tinh_thanh_id) SELECT s.ma,s.ten,p.id FROM (VALUES(N''15016'',N''Xã Quan Sơn'',N''38''),(N''15019'',N''Xã Tam Lư'',N''38''),(N''15022'',N''Xã Sơn Điện'',N''38''),(N''15025'',N''Xã Mường Mìn'',N''38''),(N''15031'',N''Xã Yên Khương'',N''38''),(N''15034'',N''Xã Yên Thắng'',N''38''),(N''15043'',N''Xã Giao An'',N''38''),(N''15049'',N''Xã Văn Phú'',N''38''),(N''15055'',N''Xã Linh Sơn'',N''38''),(N''15058'',N''Xã Đồng Lương'',N''38''),(N''15061'',N''Xã Ngọc Lặc'',N''38''),(N''15085'',N''Xã Thạch Lập'',N''38''),(N''15091'',N''Xã Ngọc Liên'',N''38''),(N''15106'',N''Xã Nguyệt Ấn'',N''38''),(N''15112'',N''Xã Kiên Thọ'',N''38''),(N''15124'',N''Xã Minh Sơn'',N''38''),(N''15127'',N''Xã Cẩm Thủy'',N''38''),(N''15142'',N''Xã Cẩm Thạch'',N''38''),(N''15148'',N''Xã Cẩm Tú'',N''38''),(N''15163'',N''Xã Cẩm Vân'',N''38''),(N''15178'',N''Xã Cẩm Tân'',N''38''),(N''15187'',N''Xã Kim Tân'',N''38''),(N''15190'',N''Xã Vân Du'',N''38''),(N''15199'',N''Xã Thạch Quảng'',N''38''),(N''15211'',N''Xã Thạch Bình'',N''38''),(N''15229'',N''Xã Thành Vinh'',N''38''),(N''15250'',N''Xã Ngọc Trạo'',N''38''),(N''15271'',N''Xã Hà Trung'',N''38''),(N''15274'',N''Xã Hà Long'',N''38''),(N''15286'',N''Xã Hoạt Giang'',N''38''),(N''15298'',N''Xã Lĩnh Toại'',N''38''),(N''15316'',N''Xã Tống Sơn'',N''38''),(N''15349'',N''Xã Vĩnh Lộc'',N''38''),(N''15361'',N''Xã Tây Đô'',N''38''),(N''15382'',N''Xã Biện Thượng'',N''38''),(N''15409'',N''Xã Yên Phú'',N''38''),(N''15412'',N''Xã Quý Lộc'',N''38''),(N''15421'',N''Xã Yên Trường'',N''38''),(N''15442'',N''Xã Yên Ninh'',N''38''),(N''15448'',N''Xã Định Hòa'',N''38''),(N''15457'',N''Xã Định Tân'',N''38''),(N''15469'',N''Xã Yên Định'',N''38''),(N''15499'',N''Xã Thọ Xuân'',N''38''),(N''15505'',N''Xã Thọ Long'',N''38''),(N''15520'',N''Xã Xuân Hòa'',N''38''),(N''15544'',N''Xã Lam Sơn'',N''38''),(N''15553'',N''Xã Sao Vàng'',N''38''),(N''15568'',N''Xã Thọ Lập'',N''38''),(N''15574'',N''Xã Xuân Tín'',N''38''),(N''15592'',N''Xã Xuân Lập'',N''38''),(N''15607'',N''Xã Bát Mọt'',N''38''),(N''15610'',N''Xã Yên Nhân'',N''38''),(N''15622'',N''Xã Vạn Xuân'',N''38''),(N''15628'',N''Xã Lương Sơn'',N''38''),(N''15634'',N''Xã Luận Thành'',N''38''),(N''15643'',N''Xã Thắng Lộc'',N''38''),(N''15646'',N''Xã Thường Xuân'',N''38''),(N''15658'',N''Xã Xuân Chinh'',N''38''),(N''15661'',N''Xã Tân Thành'',N''38''),(N''15664'',N''Xã Triệu Sơn'',N''38''),(N''15667'',N''Xã Thọ Bình'',N''38''),(N''15682'',N''Xã Hợp Tiến'',N''38''),(N''15715'',N''Xã Tân Ninh'',N''38''),(N''15724'',N''Xã Đồng Tiến'',N''38''),(N''15754'',N''Xã Thọ Ngọc'',N''38''),(N''15763'',N''Xã Thọ Phú'',N''38''),(N''15766'',N''Xã An Nông'',N''38''),(N''15772'',N''Xã Thiệu Hóa'',N''38''),(N''15778'',N''Xã Thiệu Tiến'',N''38''),(N''15796'',N''Xã Thiệu Quang'',N''38''),(N''15820'',N''Xã Thiệu Toán'',N''38''),(N''15835'',N''Xã Thiệu Trung'',N''38''),(N''15853'',N''Phường Đông Tiến'',N''38''),(N''15865'',N''Xã Hoằng Hóa'',N''38''),(N''15880'',N''Xã Hoằng Giang'',N''38''),(N''15889'',N''Xã Hoằng Phú'',N''38''),(N''15910'',N''Xã Hoằng Sơn'',N''38''),(N''15925'',N''Phường Nguyệt Viên'',N''38''),(N''15961'',N''Xã Hoằng Lộc'',N''38''),(N''15976'',N''Xã Hoằng Châu'',N''38''),(N''15991'',N''Xã Hoằng Tiến'',N''38''),(N''16000'',N''Xã Hoằng Thanh'',N''38''),(N''16012'',N''Xã Hậu Lộc'',N''38''),(N''16021'',N''Xã Triệu Lộc'',N''38''),(N''16033'',N''Xã Đông Thành'',N''38''),(N''16072'',N''Xã Hoa Lộc'',N''38''),(N''16078'',N''Xã Vạn Lộc'',N''38''),(N''16093'',N''Xã Nga Sơn'',N''38''),(N''16108'',N''Xã Tân Tiến'',N''38''),(N''16114'',N''Xã Nga Thắng'',N''38''),(N''16138'',N''Xã Hồ Vương'',N''38''),(N''16144'',N''Xã Nga An'',N''38''),(N''16171'',N''Xã Ba Đình'',N''38''),(N''16174'',N''Xã Như Xuân'',N''38''),(N''16177'',N''Xã Xuân Bình'',N''38''),(N''16186'',N''Xã Hóa Quỳ'',N''38''),(N''16213'',N''Xã Thanh Phong'',N''38''),(N''16222'',N''Xã Thanh Quân'',N''38''),(N''16225'',N''Xã Thượng Ninh'',N''38''),(N''16228'',N''Xã Như Thanh'',N''38''),(N''16234'',N''Xã Xuân Du'',N''38''),(N''16249'',N''Xã Mậu Lâm'',N''38''),(N''16258'',N''Xã Xuân Thái'',N''38''),(N''16264'',N''Xã Yên Thọ'',N''38''),(N''16273'',N''Xã Thanh Kỳ'',N''38''),(N''16279'',N''Xã Nông Cống'',N''38''),(N''16297'',N''Xã Trung Chính'',N''38''),(N''16309'',N''Xã Thắng Lợi'',N''38''),(N''16342'',N''Xã Thăng Bình'',N''38''),(N''16348'',N''Xã Trường Văn'',N''38''),(N''16363'',N''Xã Tượng Lĩnh'',N''38''),(N''16369'',N''Xã Công Chính'',N''38''),(N''16378'',N''Phường Đông Sơn'',N''38''),(N''16417'',N''Phường Đông Quang'',N''38''),(N''16438'',N''Xã Lưu Vệ'',N''38''),(N''16480'',N''Xã Quảng Yên'',N''38''),(N''16489'',N''Xã Quảng Chính'',N''38''),(N''16498'',N''Xã Quảng Ngọc'',N''38''),(N''16516'',N''Phường Nam Sầm Sơn'',N''38''),(N''16522'',N''Phường Quảng Phú'',N''38''),(N''16531'',N''Phường Sầm Sơn'',N''38''),(N''16540'',N''Xã Quảng Ninh'',N''38''),(N''16543'',N''Xã Quảng Bình'',N''38''),(N''16549'',N''Xã Tiên Trang'',N''38''),(N''16561'',N''Phường Tĩnh Gia'',N''38''),(N''16576'',N''Phường Ngọc Sơn'',N''38''),(N''16591'',N''Xã Các Sơn'',N''38''),(N''16594'',N''Phường Tân Dân'',N''38''),(N''16597'',N''Phường Hải Lĩnh'',N''38''),(N''16609'',N''Phường Đào Duy Từ'',N''38''),(N''16624'',N''Phường Trúc Lâm'',N''38''),(N''16636'',N''Xã Trường Lâm'',N''38''),(N''16645'',N''Phường Hải Bình'',N''38''),(N''16654'',N''Phường Nghi Sơn'',N''38''),(N''16681'',N''Phường Thành Vinh'',N''40''),(N''16690'',N''Phường Trường Vinh'',N''40''),(N''16702'',N''Phường Vinh Phú'',N''40''),(N''16708'',N''Phường Vinh Lộc'',N''40''),(N''16732'',N''Phường Cửa Lò'',N''40''),(N''16738'',N''Xã Quế Phong'',N''40''),(N''16744'',N''Xã Thông Thụ'',N''40''),(N''16750'',N''Xã Tiền Phong'',N''40''),(N''16756'',N''Xã Tri Lễ'',N''40''),(N''16774'',N''Xã Mường Quàng'',N''40''),(N''16777'',N''Xã Quỳ Châu'',N''40''),(N''16792'',N''Xã Châu Tiến'',N''40''),(N''16801'',N''Xã Hùng Chân'',N''40''),(N''16804'',N''Xã Châu Bình'',N''40''),(N''16813'',N''Xã Mường Xén'',N''40''),(N''16816'',N''Xã Mỹ Lý'',N''40''),(N''16819'',N''Xã Bắc Lý'',N''40''),(N''16822'',N''Xã Keng Đu'',N''40''),(N''16828'',N''Xã Huồi Tụ'',N''40''),(N''16831'',N''Xã Mường Lống'',N''40''),(N''16834'',N''Xã Na Loi'',N''40''),(N''16837'',N''Xã Nậm Cắn'',N''40''),(N''16849'',N''Xã Hữu Kiệm'',N''40''),(N''16855'',N''Xã Chiêu Lưu'',N''40''),(N''16858'',N''Xã Mường Típ'',N''40''),(N''16870'',N''Xã Na Ngoi'',N''40''),(N''16876'',N''Xã Tương Dương'',N''40''),(N''16882'',N''Xã Nhôn Mai'',N''40''),(N''16885'',N''Xã Hữu Khuông'',N''40''),(N''16903'',N''Xã Nga My'',N''40''),(N''16906'',N''Xã Lượng Minh'',N''40''),(N''16909'',N''Xã Yên Hòa'',N''40''),(N''16912'',N''Xã Yên Na'',N''40''),(N''16933'',N''Xã Tam Quang'',N''40''),(N''16936'',N''Xã Tam Thái'',N''40''),(N''16939'',N''Phường Thái Hòa'',N''40''),(N''16941'',N''Xã Nghĩa Đàn'',N''40''),(N''16951'',N''Xã Nghĩa Lâm'',N''40''),(N''16969'',N''Xã Nghĩa Thọ'',N''40''),(N''16972'',N''Xã Nghĩa Hưng'',N''40''),(N''16975'',N''Xã Nghĩa Mai'',N''40''),(N''17011'',N''Phường Tây Hiếu'',N''40''),(N''17017'',N''Xã Đông Hiếu'',N''40''),(N''17029'',N''Xã Nghĩa Lộc'',N''40''),(N''17032'',N''Xã Nghĩa Khánh'',N''40''),(N''17035'',N''Xã Quỳ Hợp'',N''40''),(N''17044'',N''Xã Châu Hồng'',N''40''),(N''17056'',N''Xã Châu Lộc'',N''40''),(N''17059'',N''Xã Tam Hợp'',N''40''),(N''17071'',N''Xã Minh Hợp'',N''40''),(N''17077'',N''Xã Mường Ham'',N''40''),(N''17089'',N''Xã Mường Chọng'',N''40''),(N''17110'',N''Phường Hoàng Mai'',N''40''),(N''17125'',N''Phường Quỳnh Mai'',N''40''),(N''17128'',N''Phường Tân Mai'',N''40''),(N''17143'',N''Xã Quỳnh Văn'',N''40''),(N''17149'',N''Xã Quỳnh Tam'',N''40''),(N''17170'',N''Xã Quỳnh Sơn'',N''40''),(N''17176'',N''Xã Quỳnh Anh'',N''40''),(N''17179'',N''Xã Quỳnh Lưu'',N''40''),(N''17212'',N''Xã Quỳnh Phú'',N''40''),(N''17224'',N''Xã Quỳnh Thắng'',N''40''),(N''17230'',N''Xã Bình Chuẩn'',N''40''),(N''17239'',N''Xã Mậu Thạch'',N''40''),(N''17242'',N''Xã Cam Phục'',N''40''),(N''17248'',N''Xã Châu Khê'',N''40'')) s(ma,ten,tinh) JOIN dbo.tinh_thanh p ON p.ma_hanh_chinh=s.tinh WHERE NOT EXISTS(SELECT 1 FROM dbo.phuong_xa w WHERE w.ma=s.ma);');
 EXEC(N'INSERT dbo.phuong_xa(ma,ten,tinh_thanh_id) SELECT s.ma,s.ten,p.id FROM (VALUES(N''17254'',N''Xã Con Cuông'',N''40''),(N''17263'',N''Xã Môn Sơn'',N''40''),(N''17266'',N''Xã Tân Kỳ'',N''40''),(N''17272'',N''Xã Tân Phú'',N''40''),(N''17278'',N''Xã Giai Xuân'',N''40''),(N''17284'',N''Xã Nghĩa Đồng'',N''40''),(N''17287'',N''Xã Tiên Đồng'',N''40''),(N''17305'',N''Xã Tân An'',N''40''),(N''17326'',N''Xã Nghĩa Hành'',N''40''),(N''17329'',N''Xã Anh Sơn'',N''40''),(N''17335'',N''Xã Thành Bình Thọ'',N''40''),(N''17344'',N''Xã Nhân Hòa'',N''40''),(N''17357'',N''Xã Vĩnh Tường'',N''40''),(N''17365'',N''Xã Anh Sơn Đông'',N''40''),(N''17380'',N''Xã Yên Xuân'',N''40''),(N''17395'',N''Xã Hùng Châu'',N''40''),(N''17416'',N''Xã Đức Châu'',N''40''),(N''17419'',N''Xã Hải Châu'',N''40''),(N''17443'',N''Xã Quảng Châu'',N''40''),(N''17464'',N''Xã Diễn Châu'',N''40''),(N''17476'',N''Xã Minh Châu'',N''40''),(N''17479'',N''Xã An Châu'',N''40''),(N''17488'',N''Xã Tân Châu'',N''40''),(N''17506'',N''Xã Yên Thành'',N''40''),(N''17515'',N''Xã Bình Minh'',N''40''),(N''17521'',N''Xã Quang Đồng'',N''40''),(N''17524'',N''Xã Giai Lạc'',N''40''),(N''17530'',N''Xã Đông Thành'',N''40''),(N''17560'',N''Xã Vân Du'',N''40''),(N''17569'',N''Xã Quan Thành'',N''40''),(N''17605'',N''Xã Hợp Minh'',N''40''),(N''17611'',N''Xã Vân Tụ'',N''40''),(N''17623'',N''Xã Bạch Ngọc'',N''40''),(N''17641'',N''Xã Lương Sơn'',N''40''),(N''17662'',N''Xã Đô Lương'',N''40''),(N''17677'',N''Xã Văn Hiến'',N''40''),(N''17689'',N''Xã Thuần Trung'',N''40''),(N''17707'',N''Xã Bạch Hà'',N''40''),(N''17713'',N''Xã Đại Đồng'',N''40''),(N''17722'',N''Xã Hạnh Lâm'',N''40''),(N''17728'',N''Xã Cát Ngạn'',N''40''),(N''17743'',N''Xã Tam Đồng'',N''40''),(N''17759'',N''Xã Sơn Lâm'',N''40''),(N''17770'',N''Xã Hoa Quân'',N''40''),(N''17779'',N''Xã Xuân Lâm'',N''40''),(N''17791'',N''Xã Kim Bảng'',N''40''),(N''17818'',N''Xã Bích Hào'',N''40''),(N''17827'',N''Xã Nghi Lộc'',N''40''),(N''17833'',N''Xã Hải Lộc'',N''40''),(N''17842'',N''Xã Thần Lĩnh'',N''40''),(N''17854'',N''Xã Văn Kiều'',N''40''),(N''17857'',N''Xã Phúc Lộc'',N''40''),(N''17866'',N''Xã Trung Lộc'',N''40''),(N''17878'',N''Xã Đông Lộc'',N''40''),(N''17920'',N''Phường Vinh Hưng'',N''40''),(N''17935'',N''Xã Nam Đàn'',N''40''),(N''17944'',N''Xã Đại Huệ'',N''40''),(N''17950'',N''Xã Vạn An'',N''40''),(N''17971'',N''Xã Kim Liên'',N''40''),(N''17989'',N''Xã Thiên Nhẫn'',N''40''),(N''18001'',N''Xã Hưng Nguyên'',N''40''),(N''18007'',N''Xã Yên Trung'',N''40''),(N''18028'',N''Xã Hưng Nguyên Nam'',N''40''),(N''18040'',N''Xã Lam Thành'',N''40''),(N''18073'',N''Phường Thành Sen'',N''42''),(N''18100'',N''Phường Trần Phú'',N''42''),(N''18115'',N''Phường Bắc Hồng Lĩnh'',N''42''),(N''18118'',N''Phường Nam Hồng Lĩnh'',N''42''),(N''18133'',N''Xã Hương Sơn'',N''42''),(N''18160'',N''Xã Sơn Hồng'',N''42''),(N''18163'',N''Xã Sơn Tiến'',N''42''),(N''18172'',N''Xã Sơn Tây'',N''42''),(N''18184'',N''Xã Sơn Giang'',N''42''),(N''18196'',N''Xã Sơn Kim 1'',N''42''),(N''18199'',N''Xã Sơn Kim 2'',N''42''),(N''18202'',N''Xã Tứ Mỹ'',N''42''),(N''18223'',N''Xã Kim Hoa'',N''42''),(N''18229'',N''Xã Đức Thọ'',N''42''),(N''18244'',N''Xã Đức Minh'',N''42''),(N''18262'',N''Xã Đức Quang'',N''42''),(N''18277'',N''Xã Đức Thịnh'',N''42''),(N''18304'',N''Xã Đức Đồng'',N''42''),(N''18313'',N''Xã Vũ Quang'',N''42''),(N''18322'',N''Xã Mai Hoa'',N''42''),(N''18328'',N''Xã Thượng Đức'',N''42''),(N''18352'',N''Xã Nghi Xuân'',N''42''),(N''18364'',N''Xã Đan Hải'',N''42''),(N''18373'',N''Xã Tiên Điền'',N''42''),(N''18394'',N''Xã Cổ Đạm'',N''42''),(N''18406'',N''Xã Can Lộc'',N''42''),(N''18409'',N''Xã Hồng Lộc'',N''42''),(N''18418'',N''Xã Tùng Lộc'',N''42''),(N''18436'',N''Xã Trường Lưu'',N''42''),(N''18466'',N''Xã Gia Hanh'',N''42''),(N''18481'',N''Xã Xuân Lộc'',N''42''),(N''18484'',N''Xã Đồng Lộc'',N''42''),(N''18496'',N''Xã Hương Khê'',N''42''),(N''18502'',N''Xã Hà Linh'',N''42''),(N''18523'',N''Xã Hương Bình'',N''42''),(N''18532'',N''Xã Hương Phố'',N''42''),(N''18544'',N''Xã Hương Xuân'',N''42''),(N''18547'',N''Xã Phúc Trạch'',N''42''),(N''18550'',N''Xã Hương Đô'',N''42''),(N''18562'',N''Xã Thạch Hà'',N''42''),(N''18568'',N''Xã Lộc Hà'',N''42''),(N''18583'',N''Xã Mai Phụ'',N''42''),(N''18586'',N''Xã Đông Kinh'',N''42''),(N''18601'',N''Xã Việt Xuyên'',N''42''),(N''18604'',N''Xã Thạch Khê'',N''42''),(N''18619'',N''Xã Đồng Tiến'',N''42''),(N''18628'',N''Xã Thạch Lạc'',N''42''),(N''18634'',N''Xã Toàn Lưu'',N''42''),(N''18652'',N''Phường Hà Huy Tập'',N''42''),(N''18667'',N''Xã Thạch Xuân'',N''42''),(N''18673'',N''Xã Cẩm Xuyên'',N''42''),(N''18676'',N''Xã Thiên Cầm'',N''42''),(N''18682'',N''Xã Yên Hòa'',N''42''),(N''18685'',N''Xã Cẩm Bình'',N''42''),(N''18736'',N''Xã Cẩm Hưng'',N''42''),(N''18739'',N''Xã Cẩm Duệ'',N''42''),(N''18742'',N''Xã Cẩm Trung'',N''42''),(N''18748'',N''Xã Cẩm Lạc'',N''42''),(N''18754'',N''Phường Sông Trí'',N''42''),(N''18766'',N''Xã Kỳ Xuân'',N''42''),(N''18775'',N''Xã Kỳ Anh'',N''42''),(N''18781'',N''Phường Hải Ninh'',N''42''),(N''18787'',N''Xã Kỳ Văn'',N''42''),(N''18790'',N''Xã Kỳ Khang'',N''42''),(N''18814'',N''Xã Kỳ Hoa'',N''42''),(N''18823'',N''Phường Vũng Áng'',N''42''),(N''18832'',N''Phường Hoành Sơn'',N''42''),(N''18838'',N''Xã Kỳ Lạc'',N''42''),(N''18844'',N''Xã Kỳ Thượng'',N''42''),(N''18859'',N''Phường Đồng Thuận'',N''44''),(N''18871'',N''Phường Đồng Sơn'',N''44''),(N''18880'',N''Phường Đồng Hới'',N''44''),(N''18901'',N''Xã Minh Hóa'',N''44''),(N''18904'',N''Xã Dân Hóa'',N''44''),(N''18919'',N''Xã Tân Thành'',N''44''),(N''18922'',N''Xã Kim Điền'',N''44''),(N''18943'',N''Xã Kim Phú'',N''44''),(N''18949'',N''Xã Đồng Lê'',N''44''),(N''18952'',N''Xã Tuyên Sơn'',N''44''),(N''18958'',N''Xã Tuyên Lâm'',N''44''),(N''18985'',N''Xã Tuyên Phú'',N''44''),(N''18991'',N''Xã Tuyên Bình'',N''44''),(N''18997'',N''Xã Tuyên Hóa'',N''44''),(N''19009'',N''Phường Ba Đồn'',N''44''),(N''19021'',N''Xã Phú Trạch'',N''44''),(N''19030'',N''Xã Trung Thuần'',N''44''),(N''19033'',N''Xã Hòa Trạch'',N''44''),(N''19051'',N''Xã Tân Gianh'',N''44''),(N''19057'',N''Xã Quảng Trạch'',N''44''),(N''19066'',N''Phường Bắc Gianh'',N''44''),(N''19075'',N''Xã Nam Ba Đồn'',N''44''),(N''19093'',N''Xã Nam Gianh'',N''44''),(N''19111'',N''Xã Hoàn Lão'',N''44''),(N''19126'',N''Xã Bắc Trạch'',N''44''),(N''19138'',N''Xã Phong Nha'',N''44''),(N''19141'',N''Xã Bố Trạch'',N''44''),(N''19147'',N''Xã Thượng Trạch'',N''44''),(N''19159'',N''Xã Đông Trạch'',N''44''),(N''19198'',N''Xã Nam Trạch'',N''44''),(N''19204'',N''Xã Trường Sơn'',N''44''),(N''19207'',N''Xã Quảng Ninh'',N''44''),(N''19225'',N''Xã Ninh Châu'',N''44''),(N''19237'',N''Xã Trường Ninh'',N''44''),(N''19246'',N''Xã Lệ Ninh'',N''44''),(N''19249'',N''Xã Lệ Thủy'',N''44''),(N''19255'',N''Xã Cam Hồng'',N''44''),(N''19288'',N''Xã Sen Ngư'',N''44''),(N''19291'',N''Xã Tân Mỹ'',N''44''),(N''19309'',N''Xã Trường Phú'',N''44''),(N''19318'',N''Xã Kim Ngân'',N''44''),(N''19333'',N''Phường Đông Hà'',N''44''),(N''19351'',N''Phường Nam Đông Hà'',N''44''),(N''19360'',N''Phường Quảng Trị'',N''44''),(N''19363'',N''Xã Vĩnh Linh'',N''44''),(N''19366'',N''Xã Bến Quan'',N''44''),(N''19372'',N''Xã Vĩnh Hoàng'',N''44''),(N''19405'',N''Xã Vĩnh Thủy'',N''44''),(N''19414'',N''Xã Cửa Tùng'',N''44''),(N''19429'',N''Xã Khe Sanh'',N''44''),(N''19432'',N''Xã Lao Bảo'',N''44''),(N''19435'',N''Xã Hướng Lập'',N''44''),(N''19441'',N''Xã Hướng Phùng'',N''44''),(N''19462'',N''Xã Tân Lập'',N''44''),(N''19483'',N''Xã A Dơi'',N''44''),(N''19489'',N''Xã Lìa'',N''44''),(N''19495'',N''Xã Gio Linh'',N''44''),(N''19496'',N''Xã Cửa Việt'',N''44''),(N''19501'',N''Xã Bến Hải'',N''44''),(N''19537'',N''Xã Cồn Tiên'',N''44''),(N''19555'',N''Xã Hướng Hiệp'',N''44''),(N''19564'',N''Xã Đakrông'',N''44''),(N''19567'',N''Xã Ba Lòng'',N''44''),(N''19588'',N''Xã Tà Rụt'',N''44''),(N''19594'',N''Xã La Lay'',N''44''),(N''19597'',N''Xã Cam Lộ'',N''44''),(N''19603'',N''Xã Hiếu Giang'',N''44'')) s(ma,ten,tinh) JOIN dbo.tinh_thanh p ON p.ma_hanh_chinh=s.tinh WHERE NOT EXISTS(SELECT 1 FROM dbo.phuong_xa w WHERE w.ma=s.ma);');
 EXEC(N'INSERT dbo.phuong_xa(ma,ten,tinh_thanh_id) SELECT s.ma,s.ten,p.id FROM (VALUES(N''19624'',N''Xã Triệu Phong'',N''44''),(N''19639'',N''Xã Nam Cửa Việt'',N''44''),(N''19645'',N''Xã Triệu Bình'',N''44''),(N''19654'',N''Xã Triệu Cơ'',N''44''),(N''19669'',N''Xã Ái Tử'',N''44''),(N''19681'',N''Xã Diên Sanh'',N''44''),(N''19699'',N''Xã Vĩnh Định'',N''44''),(N''19702'',N''Xã Hải Lăng'',N''44''),(N''19735'',N''Xã Nam Hải Lăng'',N''44''),(N''19741'',N''Xã Mỹ Thủy'',N''44''),(N''19742'',N''Đặc khu Cồn Cỏ'',N''44''),(N''19753'',N''Phường Phú Xuân'',N''46''),(N''19774'',N''Phường Kim Long'',N''46''),(N''19777'',N''Phường Vỹ Dạ'',N''46''),(N''19789'',N''Phường Thuận Hóa'',N''46''),(N''19804'',N''Phường Hương An'',N''46''),(N''19813'',N''Phường Thủy Xuân'',N''46''),(N''19815'',N''Phường An Cựu'',N''46''),(N''19819'',N''Phường Phong Điền'',N''46''),(N''19828'',N''Phường Phong Phú'',N''46''),(N''19831'',N''Phường Phong Dinh'',N''46''),(N''19858'',N''Phường Phong Thái'',N''46''),(N''19867'',N''Xã Quảng Điền'',N''46''),(N''19873'',N''Phường Phong Quảng'',N''46''),(N''19885'',N''Xã Đan Điền'',N''46''),(N''19900'',N''Phường Thuận An'',N''46''),(N''19909'',N''Phường Dương Nỗ'',N''46''),(N''19918'',N''Xã Phú Hồ'',N''46''),(N''19930'',N''Phường Mỹ Thượng'',N''46''),(N''19942'',N''Xã Phú Vang'',N''46''),(N''19945'',N''Xã Phú Vinh'',N''46''),(N''19960'',N''Phường Phú Bài'',N''46''),(N''19969'',N''Phường Thanh Thủy'',N''46''),(N''19975'',N''Phường Hương Thủy'',N''46''),(N''19996'',N''Phường Hương Trà'',N''46''),(N''20014'',N''Phường Hóa Châu'',N''46''),(N''20017'',N''Phường Kim Trà'',N''46''),(N''20035'',N''Xã Bình Điền'',N''46''),(N''20044'',N''Xã A Lưới 2'',N''46''),(N''20050'',N''Xã A Lưới 5'',N''46''),(N''20056'',N''Xã A Lưới 1'',N''46''),(N''20071'',N''Xã A Lưới 3'',N''46''),(N''20101'',N''Xã A Lưới 4'',N''46''),(N''20107'',N''Xã Phú Lộc'',N''46''),(N''20122'',N''Xã Vinh Lộc'',N''46''),(N''20131'',N''Xã Hưng Lộc'',N''46''),(N''20137'',N''Xã Chân Mây - Lăng Cô'',N''46''),(N''20140'',N''Xã Lộc An'',N''46''),(N''20161'',N''Xã Khe Tre'',N''46''),(N''20179'',N''Xã Nam Đông'',N''46''),(N''20182'',N''Xã Long Quảng'',N''46''),(N''20194'',N''Phường Hải Vân'',N''48''),(N''20197'',N''Phường Liên Chiểu'',N''48''),(N''20200'',N''Phường Hòa Khánh'',N''48''),(N''20209'',N''Phường Thanh Khê'',N''48''),(N''20242'',N''Phường Hải Châu'',N''48''),(N''20257'',N''Phường Hòa Cường'',N''48''),(N''20260'',N''Phường Cẩm Lệ'',N''48''),(N''20263'',N''Phường Sơn Trà'',N''48''),(N''20275'',N''Phường An Hải'',N''48''),(N''20285'',N''Phường Ngũ Hành Sơn'',N''48''),(N''20305'',N''Phường An Khê'',N''48''),(N''20308'',N''Xã Bà Nà'',N''48''),(N''20314'',N''Phường Hòa Xuân'',N''48''),(N''20320'',N''Xã Hòa Vang'',N''48''),(N''20332'',N''Xã Hòa Tiến'',N''48''),(N''20333'',N''Đặc khu Hoàng Sa'',N''48''),(N''20335'',N''Phường Bàn Thạch'',N''48''),(N''20341'',N''Phường Tam Kỳ'',N''48''),(N''20350'',N''Phường Hương Trà'',N''48''),(N''20356'',N''Phường Quảng Phú'',N''48''),(N''20364'',N''Xã Chiên Đàn'',N''48''),(N''20380'',N''Xã Tây Hồ'',N''48''),(N''20392'',N''Xã Phú Ninh'',N''48''),(N''20401'',N''Phường Hội An Tây'',N''48''),(N''20410'',N''Phường Hội An'',N''48''),(N''20413'',N''Phường Hội An Đông'',N''48''),(N''20434'',N''Xã Tân Hiệp'',N''48''),(N''20443'',N''Xã Hùng Sơn'',N''48''),(N''20455'',N''Xã Tây Giang'',N''48''),(N''20458'',N''Xã Avương'',N''48''),(N''20467'',N''Xã Đông Giang'',N''48''),(N''20476'',N''Xã Sông Kôn'',N''48''),(N''20485'',N''Xã Sông Vàng'',N''48''),(N''20494'',N''Xã Bến Hiên'',N''48''),(N''20500'',N''Xã Đại Lộc'',N''48''),(N''20506'',N''Xã Thượng Đức'',N''48''),(N''20515'',N''Xã Hà Nha'',N''48''),(N''20539'',N''Xã Vu Gia'',N''48''),(N''20542'',N''Xã Phú Thuận'',N''48''),(N''20551'',N''Phường Điện Bàn'',N''48''),(N''20557'',N''Phường Điện Bàn Bắc'',N''48''),(N''20569'',N''Xã Điện Bàn Tây'',N''48''),(N''20575'',N''Phường An Thắng'',N''48''),(N''20579'',N''Phường Điện Bàn Đông'',N''48''),(N''20587'',N''Xã Gò Nổi'',N''48''),(N''20599'',N''Xã Nam Phước'',N''48''),(N''20611'',N''Xã Thu Bồn'',N''48''),(N''20623'',N''Xã Duy Xuyên'',N''48''),(N''20635'',N''Xã Duy Nghĩa'',N''48''),(N''20641'',N''Xã Quế Sơn'',N''48''),(N''20650'',N''Xã Xuân Phú'',N''48''),(N''20656'',N''Xã Nông Sơn'',N''48''),(N''20662'',N''Xã Quế Sơn Trung'',N''48''),(N''20669'',N''Xã Quế Phước'',N''48''),(N''20695'',N''Xã Thạnh Mỹ'',N''48''),(N''20698'',N''Xã La Êê'',N''48''),(N''20704'',N''Xã La Dêê'',N''48''),(N''20707'',N''Xã Nam Giang'',N''48''),(N''20710'',N''Xã Bến Giằng'',N''48''),(N''20716'',N''Xã Đắc Pring'',N''48''),(N''20722'',N''Xã Khâm Đức'',N''48''),(N''20728'',N''Xã Phước Hiệp'',N''48''),(N''20734'',N''Xã Phước Năng'',N''48''),(N''20740'',N''Xã Phước Chánh'',N''48''),(N''20752'',N''Xã Phước Thành'',N''48''),(N''20767'',N''Xã Việt An'',N''48''),(N''20770'',N''Xã Phước Trà'',N''48''),(N''20779'',N''Xã Hiệp Đức'',N''48''),(N''20791'',N''Xã Thăng Bình'',N''48''),(N''20794'',N''Xã Thăng An'',N''48''),(N''20818'',N''Xã Đồng Dương'',N''48''),(N''20827'',N''Xã Thăng Phú'',N''48''),(N''20836'',N''Xã Thăng Trường'',N''48''),(N''20848'',N''Xã Thăng Điền'',N''48''),(N''20854'',N''Xã Tiên Phước'',N''48''),(N''20857'',N''Xã Sơn Cẩm Hà'',N''48''),(N''20875'',N''Xã Lãnh Ngọc'',N''48''),(N''20878'',N''Xã Thạnh Bình'',N''48''),(N''20900'',N''Xã Trà My'',N''48''),(N''20908'',N''Xã Trà Liên'',N''48''),(N''20920'',N''Xã Trà Đốc'',N''48''),(N''20923'',N''Xã Trà Tân'',N''48''),(N''20929'',N''Xã Trà Giáp'',N''48''),(N''20938'',N''Xã Trà Leng'',N''48''),(N''20941'',N''Xã Trà Tập'',N''48''),(N''20944'',N''Xã Nam Trà My'',N''48''),(N''20950'',N''Xã Trà Linh'',N''48''),(N''20959'',N''Xã Trà Vân'',N''48''),(N''20965'',N''Xã Núi Thành'',N''48''),(N''20971'',N''Xã Tam Xuân'',N''48''),(N''20977'',N''Xã Đức Phú'',N''48''),(N''20984'',N''Xã Tam Anh'',N''48''),(N''20992'',N''Xã Tam Hải'',N''48''),(N''21004'',N''Xã Tam Mỹ'',N''48''),(N''21025'',N''Phường Cẩm Thành'',N''51''),(N''21028'',N''Phường Nghĩa Lộ'',N''51''),(N''21034'',N''Xã An Phú'',N''51''),(N''21040'',N''Xã Bình Sơn'',N''51''),(N''21061'',N''Xã Vạn Tường'',N''51''),(N''21085'',N''Xã Bình Minh'',N''51''),(N''21100'',N''Xã Bình Chương'',N''51''),(N''21109'',N''Xã Đông Sơn'',N''51''),(N''21115'',N''Xã Trà Bồng'',N''51''),(N''21124'',N''Xã Thanh Bồng'',N''51''),(N''21127'',N''Xã Đông Trà Bồng'',N''51''),(N''21136'',N''Xã Cà Đam'',N''51''),(N''21154'',N''Xã Tây Trà'',N''51''),(N''21157'',N''Xã Tây Trà Bồng'',N''51''),(N''21172'',N''Phường Trương Quang Trọng'',N''51''),(N''21181'',N''Xã Thọ Phong'',N''51''),(N''21196'',N''Xã Trường Giang'',N''51''),(N''21205'',N''Xã Ba Gia'',N''51''),(N''21211'',N''Xã Tịnh Khê'',N''51''),(N''21220'',N''Xã Sơn Tịnh'',N''51''),(N''21235'',N''Xã Tư Nghĩa'',N''51''),(N''21238'',N''Xã Vệ Giang'',N''51''),(N''21244'',N''Xã Trà Giang'',N''51''),(N''21250'',N''Xã Nghĩa Giang'',N''51''),(N''21289'',N''Xã Sơn Hà'',N''51''),(N''21292'',N''Xã Sơn Hạ'',N''51''),(N''21307'',N''Xã Sơn Linh'',N''51''),(N''21319'',N''Xã Sơn Thủy'',N''51''),(N''21325'',N''Xã Sơn Kỳ'',N''51''),(N''21334'',N''Xã Sơn Tây Thượng'',N''51''),(N''21340'',N''Xã Sơn Tây'',N''51''),(N''21343'',N''Xã Sơn Tây Hạ'',N''51''),(N''21349'',N''Xã Sơn Mai'',N''51''),(N''21361'',N''Xã Minh Long'',N''51''),(N''21364'',N''Xã Nghĩa Hành'',N''51''),(N''21370'',N''Xã Phước Giang'',N''51''),(N''21385'',N''Xã Đình Cương'',N''51''),(N''21388'',N''Xã Thiện Tín'',N''51''),(N''21400'',N''Xã Mộ Đức'',N''51''),(N''21409'',N''Xã Long Phụng'',N''51''),(N''21421'',N''Xã Mỏ Cày'',N''51''),(N''21433'',N''Xã Lân Phong'',N''51''),(N''21439'',N''Phường Đức Phổ'',N''51''),(N''21451'',N''Phường Trà Câu'',N''51''),(N''21457'',N''Xã Nguyễn Nghiêm'',N''51''),(N''21472'',N''Xã Khánh Cường'',N''51''),(N''21478'',N''Phường Sa Huỳnh'',N''51''),(N''21484'',N''Xã Ba Tơ'',N''51''),(N''21490'',N''Xã Ba Vinh'',N''51''),(N''21496'',N''Xã Ba Động'',N''51''),(N''21499'',N''Xã Ba Dinh'',N''51''),(N''21520'',N''Xã Đặng Thùy Trâm'',N''51''),(N''21523'',N''Xã Ba Tô'',N''51''),(N''21529'',N''Xã Ba Vì'',N''51''),(N''21538'',N''Xã Ba Xa'',N''51'')) s(ma,ten,tinh) JOIN dbo.tinh_thanh p ON p.ma_hanh_chinh=s.tinh WHERE NOT EXISTS(SELECT 1 FROM dbo.phuong_xa w WHERE w.ma=s.ma);');
 EXEC(N'INSERT dbo.phuong_xa(ma,ten,tinh_thanh_id) SELECT s.ma,s.ten,p.id FROM (VALUES(N''21548'',N''Đặc khu Lý Sơn'',N''51''),(N''23284'',N''Phường Đăk Cấm'',N''51''),(N''23293'',N''Phường Kon Tum'',N''51''),(N''23302'',N''Phường Đăk Bla'',N''51''),(N''23317'',N''Xã Ngọk Bay'',N''51''),(N''23326'',N''Xã Ia Chim'',N''51''),(N''23332'',N''Xã Đăk Rơ Wa'',N''51''),(N''23341'',N''Xã Đăk Pék'',N''51''),(N''23344'',N''Xã Đăk Plô'',N''51''),(N''23356'',N''Xã Xốp'',N''51''),(N''23365'',N''Xã Ngọc Linh'',N''51''),(N''23368'',N''Xã Đăk Long'',N''51''),(N''23374'',N''Xã Đăk Môn'',N''51''),(N''23377'',N''Xã Bờ Y'',N''51''),(N''23383'',N''Xã Dục Nông'',N''51''),(N''23392'',N''Xã Sa Loong'',N''51''),(N''23401'',N''Xã Đăk Tô'',N''51''),(N''23416'',N''Xã Đăk Sao'',N''51''),(N''23419'',N''Xã Đăk Tờ Kan'',N''51''),(N''23425'',N''Xã Tu Mơ Rông'',N''51''),(N''23428'',N''Xã Ngọk Tụ'',N''51''),(N''23430'',N''Xã Kon Đào'',N''51''),(N''23446'',N''Xã Măng Ri'',N''51''),(N''23455'',N''Xã Măng Bút'',N''51''),(N''23473'',N''Xã Măng Đen'',N''51''),(N''23476'',N''Xã Kon Plông'',N''51''),(N''23479'',N''Xã Đăk Rve'',N''51''),(N''23485'',N''Xã Đăk Kôi'',N''51''),(N''23497'',N''Xã Kon Braih'',N''51''),(N''23500'',N''Xã Đăk Hà'',N''51''),(N''23504'',N''Xã Đăk Pxi'',N''51''),(N''23510'',N''Xã Đăk Ui'',N''51''),(N''23512'',N''Xã Đăk Mar'',N''51''),(N''23515'',N''Xã Ngọk Réo'',N''51''),(N''23527'',N''Xã Sa Thầy'',N''51''),(N''23530'',N''Xã Rờ Kơi'',N''51''),(N''23534'',N''Xã Sa Bình'',N''51''),(N''23535'',N''Xã Ia Đal'',N''51''),(N''23536'',N''Xã Mô Rai'',N''51''),(N''23538'',N''Xã Ia Tơi'',N''51''),(N''23548'',N''Xã Ya Ly'',N''51''),(N''21553'',N''Phường Quy Nhơn Bắc'',N''52''),(N''21583'',N''Phường Quy Nhơn'',N''52''),(N''21589'',N''Phường Quy Nhơn Tây'',N''52''),(N''21592'',N''Phường Quy Nhơn Nam'',N''52''),(N''21601'',N''Phường Quy Nhơn Đông'',N''52''),(N''21607'',N''Xã Nhơn Châu'',N''52''),(N''21609'',N''Xã An Lão'',N''52''),(N''21616'',N''Xã An Vinh'',N''52''),(N''21622'',N''Xã An Toàn'',N''52''),(N''21628'',N''Xã An Hòa'',N''52''),(N''21637'',N''Phường Tam Quan'',N''52''),(N''21640'',N''Phường Bồng Sơn'',N''52''),(N''21655'',N''Phường Hoài Nhơn Bắc'',N''52''),(N''21661'',N''Phường Hoài Nhơn Tây'',N''52''),(N''21664'',N''Phường Hoài Nhơn'',N''52''),(N''21670'',N''Phường Hoài Nhơn Đông'',N''52''),(N''21673'',N''Phường Hoài Nhơn Nam'',N''52''),(N''21688'',N''Xã Hoài Ân'',N''52''),(N''21697'',N''Xã Ân Hảo'',N''52''),(N''21703'',N''Xã Vạn Đức'',N''52''),(N''21715'',N''Xã Ân Tường'',N''52''),(N''21727'',N''Xã Kim Sơn'',N''52''),(N''21730'',N''Xã Phù Mỹ'',N''52''),(N''21733'',N''Xã Bình Dương'',N''52''),(N''21739'',N''Xã Phù Mỹ Bắc'',N''52''),(N''21751'',N''Xã Phù Mỹ Đông'',N''52''),(N''21757'',N''Xã Phù Mỹ Tây'',N''52''),(N''21769'',N''Xã An Lương'',N''52''),(N''21775'',N''Xã Phù Mỹ Nam'',N''52''),(N''21786'',N''Xã Vĩnh Thạnh'',N''52''),(N''21787'',N''Xã Vĩnh Sơn'',N''52''),(N''21796'',N''Xã Vĩnh Thịnh'',N''52''),(N''21805'',N''Xã Vĩnh Quang'',N''52''),(N''21808'',N''Xã Tây Sơn'',N''52''),(N''21817'',N''Xã Bình Hiệp'',N''52''),(N''21820'',N''Xã Bình Khê'',N''52''),(N''21829'',N''Xã Bình An'',N''52''),(N''21835'',N''Xã Bình Phú'',N''52''),(N''21853'',N''Xã Phù Cát'',N''52''),(N''21859'',N''Xã Đề Gi'',N''52''),(N''21868'',N''Xã Hội Sơn'',N''52''),(N''21871'',N''Xã Hòa Hội'',N''52''),(N''21880'',N''Xã Cát Tiến'',N''52''),(N''21892'',N''Xã Xuân An'',N''52''),(N''21901'',N''Xã Ngô Mây'',N''52''),(N''21907'',N''Phường Bình Định'',N''52''),(N''21910'',N''Phường An Nhơn'',N''52''),(N''21925'',N''Phường An Nhơn Bắc'',N''52''),(N''21934'',N''Phường An Nhơn Đông'',N''52''),(N''21940'',N''Xã An Nhơn Tây'',N''52''),(N''21943'',N''Phường An Nhơn Nam'',N''52''),(N''21952'',N''Xã Tuy Phước'',N''52''),(N''21964'',N''Xã Tuy Phước Bắc'',N''52''),(N''21970'',N''Xã Tuy Phước Đông'',N''52''),(N''21985'',N''Xã Tuy Phước Tây'',N''52''),(N''21994'',N''Xã Vân Canh'',N''52''),(N''21997'',N''Xã Canh Liên'',N''52''),(N''22006'',N''Xã Canh Vinh'',N''52''),(N''23563'',N''Phường Diên Hồng'',N''52''),(N''23575'',N''Phường Pleiku'',N''52''),(N''23584'',N''Phường Thống Nhất'',N''52''),(N''23586'',N''Phường Hội Phú'',N''52''),(N''23590'',N''Xã Biển Hồ'',N''52''),(N''23602'',N''Phường An Phú'',N''52''),(N''23611'',N''Xã Gào'',N''52''),(N''23614'',N''Phường An Bình'',N''52''),(N''23617'',N''Phường An Khê'',N''52''),(N''23629'',N''Xã Cửu An'',N''52''),(N''23638'',N''Xã Kbang'',N''52''),(N''23644'',N''Xã Đak Rong'',N''52''),(N''23647'',N''Xã Sơn Lang'',N''52''),(N''23650'',N''Xã Krong'',N''52''),(N''23668'',N''Xã Tơ Tung'',N''52''),(N''23674'',N''Xã Kông Bơ La'',N''52''),(N''23677'',N''Xã Đak Đoa'',N''52''),(N''23683'',N''Xã Đak Sơmei'',N''52''),(N''23701'',N''Xã Kon Gang'',N''52''),(N''23710'',N''Xã Ia Băng'',N''52''),(N''23714'',N''Xã KDang'',N''52''),(N''23722'',N''Xã Chư Păh'',N''52''),(N''23728'',N''Xã Ia Khươl'',N''52''),(N''23734'',N''Xã Ia Ly'',N''52''),(N''23749'',N''Xã Ia Phí'',N''52''),(N''23764'',N''Xã Ia Grai'',N''52''),(N''23767'',N''Xã Ia Hrung'',N''52''),(N''23776'',N''Xã Ia Krái'',N''52''),(N''23782'',N''Xã Ia O'',N''52''),(N''23788'',N''Xã Ia Chia'',N''52''),(N''23794'',N''Xã Mang Yang'',N''52''),(N''23798'',N''Xã Ayun'',N''52''),(N''23799'',N''Xã Hra'',N''52''),(N''23812'',N''Xã Lơ Pang'',N''52''),(N''23818'',N''Xã Kon Chiêng'',N''52''),(N''23824'',N''Xã Kông Chro'',N''52''),(N''23830'',N''Xã Chư Krey'',N''52''),(N''23833'',N''Xã Ya Ma'',N''52''),(N''23839'',N''Xã SRó'',N''52''),(N''23842'',N''Xã Đăk Song'',N''52''),(N''23851'',N''Xã Chơ Long'',N''52''),(N''23857'',N''Xã Đức Cơ'',N''52''),(N''23866'',N''Xã Ia Krêl'',N''52''),(N''23869'',N''Xã Ia Dơk'',N''52''),(N''23872'',N''Xã Ia Dom'',N''52''),(N''23881'',N''Xã Ia Pnôn'',N''52''),(N''23884'',N''Xã Ia Nan'',N''52''),(N''23887'',N''Xã Chư Prông'',N''52''),(N''23896'',N''Xã Bàu Cạn'',N''52''),(N''23908'',N''Xã Ia Tôr'',N''52''),(N''23911'',N''Xã Ia Boòng'',N''52''),(N''23917'',N''Xã Ia Púch'',N''52''),(N''23926'',N''Xã Ia Pia'',N''52''),(N''23935'',N''Xã Ia Lâu'',N''52''),(N''23938'',N''Xã Ia Mơ'',N''52''),(N''23941'',N''Xã Chư Sê'',N''52''),(N''23942'',N''Xã Chư Pưh'',N''52''),(N''23947'',N''Xã Bờ Ngoong'',N''52''),(N''23954'',N''Xã Al Bá'',N''52''),(N''23971'',N''Xã Ia Hrú'',N''52''),(N''23977'',N''Xã Ia Ko'',N''52''),(N''23986'',N''Xã Ia Le'',N''52''),(N''23995'',N''Xã Đak Pơ'',N''52''),(N''24007'',N''Xã Ya Hội'',N''52''),(N''24013'',N''Xã Pờ Tó'',N''52''),(N''24022'',N''Xã Ia Pa'',N''52''),(N''24028'',N''Xã Ia Tul'',N''52''),(N''24043'',N''Xã Phú Thiện'',N''52''),(N''24044'',N''Phường Ayun Pa'',N''52''),(N''24049'',N''Xã Chư A Thai'',N''52''),(N''24061'',N''Xã Ia Hiao'',N''52''),(N''24065'',N''Xã Ia Rbol'',N''52''),(N''24073'',N''Xã Ia Sao'',N''52''),(N''24076'',N''Xã Phú Túc'',N''52''),(N''24100'',N''Xã Ia Dreh'',N''52''),(N''24109'',N''Xã Uar'',N''52''),(N''24112'',N''Xã Ia Rsai'',N''52''),(N''22333'',N''Phường Bắc Nha Trang'',N''56''),(N''22366'',N''Phường Nha Trang'',N''56''),(N''22390'',N''Phường Tây Nha Trang'',N''56''),(N''22402'',N''Phường Nam Nha Trang'',N''56''),(N''22411'',N''Phường Bắc Cam Ranh'',N''56''),(N''22420'',N''Phường Cam Ranh'',N''56''),(N''22423'',N''Phường Ba Ngòi'',N''56''),(N''22432'',N''Phường Cam Linh'',N''56''),(N''22435'',N''Xã Cam Hiệp'',N''56''),(N''22453'',N''Xã Cam Lâm'',N''56''),(N''22465'',N''Xã Cam An'',N''56''),(N''22480'',N''Xã Nam Cam Ranh'',N''56''),(N''22489'',N''Xã Vạn Ninh'',N''56''),(N''22498'',N''Xã Tu Bông'',N''56''),(N''22504'',N''Xã Đại Lãnh'',N''56''),(N''22516'',N''Xã Vạn Thắng'',N''56''),(N''22525'',N''Xã Vạn Hưng'',N''56''),(N''22528'',N''Phường Ninh Hòa'',N''56''),(N''22546'',N''Xã Bắc Ninh Hòa'',N''56''),(N''22552'',N''Xã Tây Ninh Hòa'',N''56''),(N''22558'',N''Xã Hòa Trí'',N''56''),(N''22561'',N''Phường Đông Ninh Hòa'',N''56''),(N''22576'',N''Xã Tân Định'',N''56''),(N''22591'',N''Phường Hòa Thắng'',N''56'')) s(ma,ten,tinh) JOIN dbo.tinh_thanh p ON p.ma_hanh_chinh=s.tinh WHERE NOT EXISTS(SELECT 1 FROM dbo.phuong_xa w WHERE w.ma=s.ma);');
 EXEC(N'INSERT dbo.phuong_xa(ma,ten,tinh_thanh_id) SELECT s.ma,s.ten,p.id FROM (VALUES(N''22597'',N''Xã Nam Ninh Hòa'',N''56''),(N''22609'',N''Xã Khánh Vĩnh'',N''56''),(N''22612'',N''Xã Trung Khánh Vĩnh'',N''56''),(N''22615'',N''Xã Bắc Khánh Vĩnh'',N''56''),(N''22624'',N''Xã Tây Khánh Vĩnh'',N''56''),(N''22648'',N''Xã Nam Khánh Vĩnh'',N''56''),(N''22651'',N''Xã Diên Khánh'',N''56''),(N''22657'',N''Xã Diên Điền'',N''56''),(N''22660'',N''Xã Diên Lâm'',N''56''),(N''22672'',N''Xã Diên Thọ'',N''56''),(N''22678'',N''Xã Diên Lạc'',N''56''),(N''22702'',N''Xã Suối Hiệp'',N''56''),(N''22708'',N''Xã Suối Dầu'',N''56''),(N''22714'',N''Xã Khánh Sơn'',N''56''),(N''22720'',N''Xã Tây Khánh Sơn'',N''56''),(N''22732'',N''Xã Đông Khánh Sơn'',N''56''),(N''22736'',N''Đặc khu Trường Sa'',N''56''),(N''22738'',N''Phường Đô Vinh'',N''56''),(N''22741'',N''Phường Bảo An'',N''56''),(N''22759'',N''Phường Phan Rang'',N''56''),(N''22780'',N''Phường Đông Hải'',N''56''),(N''22786'',N''Xã Bác Ái Tây'',N''56''),(N''22795'',N''Xã Bác Ái'',N''56''),(N''22801'',N''Xã Bác Ái Đông'',N''56''),(N''22810'',N''Xã Ninh Sơn'',N''56''),(N''22813'',N''Xã Lâm Sơn'',N''56''),(N''22822'',N''Xã Mỹ Sơn'',N''56''),(N''22828'',N''Xã Anh Dũng'',N''56''),(N''22834'',N''Phường Ninh Chử'',N''56''),(N''22840'',N''Xã Công Hải'',N''56''),(N''22846'',N''Xã Vĩnh Hải'',N''56''),(N''22849'',N''Xã Thuận Bắc'',N''56''),(N''22852'',N''Xã Ninh Hải'',N''56''),(N''22861'',N''Xã Xuân Hải'',N''56''),(N''22870'',N''Xã Ninh Phước'',N''56''),(N''22873'',N''Xã Phước Hậu'',N''56''),(N''22888'',N''Xã Phước Dinh'',N''56''),(N''22891'',N''Xã Phước Hữu'',N''56''),(N''22897'',N''Xã Thuận Nam'',N''56''),(N''22900'',N''Xã Phước Hà'',N''56''),(N''22909'',N''Xã Cà Ná'',N''56''),(N''22015'',N''Phường Tuy Hòa'',N''66''),(N''22045'',N''Phường Bình Kiến'',N''66''),(N''22051'',N''Phường Sông Cầu'',N''66''),(N''22057'',N''Xã Xuân Lộc'',N''66''),(N''22060'',N''Xã Xuân Cảnh'',N''66''),(N''22075'',N''Xã Xuân Thọ'',N''66''),(N''22076'',N''Phường Xuân Đài'',N''66''),(N''22081'',N''Xã Đồng Xuân'',N''66''),(N''22090'',N''Xã Xuân Lãnh'',N''66''),(N''22096'',N''Xã Phú Mỡ'',N''66''),(N''22111'',N''Xã Xuân Phước'',N''66''),(N''22114'',N''Xã Tuy An Bắc'',N''66''),(N''22120'',N''Xã Tuy An Đông'',N''66''),(N''22132'',N''Xã Tuy An Tây'',N''66''),(N''22147'',N''Xã Ô Loan'',N''66''),(N''22153'',N''Xã Tuy An Nam'',N''66''),(N''22165'',N''Xã Sơn Hòa'',N''66''),(N''22171'',N''Xã Tây Sơn'',N''66''),(N''22177'',N''Xã Vân Hòa'',N''66''),(N''22192'',N''Xã Suối Trai'',N''66''),(N''22207'',N''Xã Sông Hinh'',N''66''),(N''22222'',N''Xã Đức Bình'',N''66''),(N''22225'',N''Xã Ea Bá'',N''66''),(N''22237'',N''Xã Ea Ly'',N''66''),(N''22240'',N''Phường Phú Yên'',N''66''),(N''22250'',N''Xã Sơn Thành'',N''66''),(N''22255'',N''Xã Tây Hòa'',N''66''),(N''22258'',N''Phường Đông Hòa'',N''66''),(N''22261'',N''Phường Hòa Hiệp'',N''66''),(N''22276'',N''Xã Hòa Thịnh'',N''66''),(N''22285'',N''Xã Hòa Mỹ'',N''66''),(N''22291'',N''Xã Hòa Xuân'',N''66''),(N''22303'',N''Xã Phú Hòa 2'',N''66''),(N''22319'',N''Xã Phú Hòa 1'',N''66''),(N''24121'',N''Phường Tân Lập'',N''66''),(N''24133'',N''Phường Buôn Ma Thuột'',N''66''),(N''24154'',N''Phường Thành Nhất'',N''66''),(N''24163'',N''Phường Tân An'',N''66''),(N''24169'',N''Phường Ea Kao'',N''66''),(N''24175'',N''Xã Hòa Phú'',N''66''),(N''24181'',N''Xã Ea Drăng'',N''66''),(N''24184'',N''Xã Ea H’Leo'',N''66''),(N''24187'',N''Xã Ea Hiao'',N''66''),(N''24193'',N''Xã Ea Wy'',N''66''),(N''24208'',N''Xã Ea Khăl'',N''66''),(N''24211'',N''Xã Ea Súp'',N''66''),(N''24214'',N''Xã Ia Lốp'',N''66''),(N''24217'',N''Xã Ea Rốk'',N''66''),(N''24221'',N''Xã Ia Rvê'',N''66''),(N''24229'',N''Xã Ea Bung'',N''66''),(N''24235'',N''Xã Buôn Đôn'',N''66''),(N''24241'',N''Xã Ea Wer'',N''66''),(N''24250'',N''Xã Ea Nuôl'',N''66''),(N''24259'',N''Xã Quảng Phú'',N''66''),(N''24265'',N''Xã Ea Kiết'',N''66''),(N''24277'',N''Xã Ea Tul'',N''66''),(N''24280'',N''Xã Cư M’gar'',N''66''),(N''24286'',N''Xã Ea M’Droh'',N''66''),(N''24301'',N''Xã Cuôr Đăng'',N''66''),(N''24305'',N''Phường Buôn Hồ'',N''66''),(N''24310'',N''Xã Krông Búk'',N''66''),(N''24313'',N''Xã Cư Pơng'',N''66''),(N''24316'',N''Xã Pơng Drang'',N''66''),(N''24328'',N''Xã Ea Drông'',N''66''),(N''24340'',N''Phường Cư Bao'',N''66''),(N''24343'',N''Xã Krông Năng'',N''66''),(N''24346'',N''Xã Dliê Ya'',N''66''),(N''24352'',N''Xã Tam Giang'',N''66''),(N''24364'',N''Xã Phú Xuân'',N''66''),(N''24373'',N''Xã Ea Kar'',N''66''),(N''24376'',N''Xã Ea Knốp'',N''66''),(N''24400'',N''Xã Ea Păl'',N''66''),(N''24403'',N''Xã Ea Ô'',N''66''),(N''24406'',N''Xã Cư Yang'',N''66''),(N''24412'',N''Xã M’Drắk'',N''66''),(N''24415'',N''Xã Cư Prao'',N''66''),(N''24433'',N''Xã Ea Riêng'',N''66''),(N''24436'',N''Xã Cư M’ta'',N''66''),(N''24444'',N''Xã Krông Á'',N''66''),(N''24445'',N''Xã Ea Trang'',N''66''),(N''24448'',N''Xã Krông Bông'',N''66''),(N''24454'',N''Xã Dang Kang'',N''66''),(N''24472'',N''Xã Hòa Sơn'',N''66''),(N''24478'',N''Xã Cư Pui'',N''66''),(N''24484'',N''Xã Yang Mao'',N''66''),(N''24490'',N''Xã Krông Pắc'',N''66''),(N''24496'',N''Xã Ea Kly'',N''66''),(N''24502'',N''Xã Ea Phê'',N''66''),(N''24505'',N''Xã Ea Knuếc'',N''66''),(N''24526'',N''Xã Tân Tiến'',N''66''),(N''24529'',N''Xã Vụ Bổn'',N''66''),(N''24538'',N''Xã Krông Ana'',N''66''),(N''24540'',N''Xã Ea Ning'',N''66''),(N''24544'',N''Xã Ea Ktur'',N''66''),(N''24559'',N''Xã Ea Na'',N''66''),(N''24561'',N''Xã Dray Bhăng'',N''66''),(N''24568'',N''Xã Dur Kmăl'',N''66''),(N''24580'',N''Xã Liên Sơn Lắk'',N''66''),(N''24595'',N''Xã Đắk Liêng'',N''66''),(N''24598'',N''Xã Đắk Phơi'',N''66''),(N''24604'',N''Xã Krông Nô'',N''66''),(N''24607'',N''Xã Nam Ka'',N''66''),(N''22918'',N''Phường Mũi Né'',N''68''),(N''22924'',N''Phường Phú Thuỷ'',N''68''),(N''22933'',N''Phường Hàm Thắng'',N''68''),(N''22945'',N''Phường Phan Thiết'',N''68''),(N''22954'',N''Phường Tiến Thành'',N''68''),(N''22960'',N''Phường Bình Thuận'',N''68''),(N''22963'',N''Xã Tuyên Quang'',N''68''),(N''22969'',N''Xã Liên Hương'',N''68''),(N''22972'',N''Xã Phan Rí Cửa'',N''68''),(N''22978'',N''Xã Tuy Phong'',N''68''),(N''22981'',N''Xã Vĩnh Hảo'',N''68''),(N''23005'',N''Xã Bắc Bình'',N''68''),(N''23008'',N''Xã Phan Sơn'',N''68''),(N''23020'',N''Xã Hải Ninh'',N''68''),(N''23023'',N''Xã Sông Lũy'',N''68''),(N''23032'',N''Xã Lương Sơn'',N''68''),(N''23041'',N''Xã Hồng Thái'',N''68''),(N''23053'',N''Xã Hòa Thắng'',N''68''),(N''23059'',N''Xã Hàm Thuận'',N''68''),(N''23065'',N''Xã La Dạ'',N''68''),(N''23074'',N''Xã Đông Giang'',N''68''),(N''23086'',N''Xã Hồng Sơn'',N''68''),(N''23089'',N''Xã Hàm Thuận Bắc'',N''68''),(N''23095'',N''Xã Hàm Liêm'',N''68''),(N''23110'',N''Xã Hàm Thuận Nam'',N''68''),(N''23122'',N''Xã Hàm Thạnh'',N''68''),(N''23128'',N''Xã Hàm Kiệm'',N''68''),(N''23134'',N''Xã Tân Lập'',N''68''),(N''23143'',N''Xã Tân Thành'',N''68''),(N''23149'',N''Xã Tánh Linh'',N''68''),(N''23152'',N''Xã Bắc Ruộng'',N''68''),(N''23158'',N''Xã Nghị Đức'',N''68''),(N''23173'',N''Xã Đồng Kho'',N''68''),(N''23188'',N''Xã Suối Kiết'',N''68''),(N''23191'',N''Xã Đức Linh'',N''68''),(N''23194'',N''Xã Hoài Đức'',N''68''),(N''23200'',N''Xã Nam Thành'',N''68''),(N''23227'',N''Xã Trà Tân'',N''68''),(N''23230'',N''Xã Tân Minh'',N''68''),(N''23231'',N''Phường Phước Hội'',N''68''),(N''23235'',N''Phường La Gi'',N''68''),(N''23236'',N''Xã Hàm Tân'',N''68''),(N''23246'',N''Xã Tân Hải'',N''68''),(N''23266'',N''Xã Sơn Mỹ'',N''68''),(N''23272'',N''Đặc khu Phú Quý'',N''68''),(N''24611'',N''Phường Bắc Gia Nghĩa'',N''68''),(N''24615'',N''Phường Nam Gia Nghĩa'',N''68''),(N''24616'',N''Xã Quảng Sơn'',N''68''),(N''24617'',N''Phường Đông Gia Nghĩa'',N''68''),(N''24620'',N''Xã Quảng Hòa'',N''68''),(N''24631'',N''Xã Quảng Khê'',N''68''),(N''24637'',N''Xã Tà Đùng'',N''68''),(N''24640'',N''Xã Cư Jút'',N''68''),(N''24646'',N''Xã Đắk Wil'',N''68''),(N''24649'',N''Xã Nam Dong'',N''68''),(N''24664'',N''Xã Đức Lập'',N''68''),(N''24670'',N''Xã Đắk Mil'',N''68'')) s(ma,ten,tinh) JOIN dbo.tinh_thanh p ON p.ma_hanh_chinh=s.tinh WHERE NOT EXISTS(SELECT 1 FROM dbo.phuong_xa w WHERE w.ma=s.ma);');
 EXEC(N'INSERT dbo.phuong_xa(ma,ten,tinh_thanh_id) SELECT s.ma,s.ten,p.id FROM (VALUES(N''24678'',N''Xã Đắk Sắk'',N''68''),(N''24682'',N''Xã Thuận An'',N''68''),(N''24688'',N''Xã Krông Nô'',N''68''),(N''24697'',N''Xã Nam Đà'',N''68''),(N''24703'',N''Xã Nâm Nung'',N''68''),(N''24712'',N''Xã Quảng Phú'',N''68''),(N''24717'',N''Xã Đức An'',N''68''),(N''24718'',N''Xã Đắk Song'',N''68''),(N''24722'',N''Xã Thuận Hạnh'',N''68''),(N''24730'',N''Xã Trường Xuân'',N''68''),(N''24733'',N''Xã Kiến Đức'',N''68''),(N''24736'',N''Xã Quảng Trực'',N''68''),(N''24739'',N''Xã Tuy Đức'',N''68''),(N''24748'',N''Xã Quảng Tân'',N''68''),(N''24751'',N''Xã Nhân Cơ'',N''68''),(N''24760'',N''Xã Quảng Tín'',N''68''),(N''24778'',N''Phường Lâm Viên - Đà Lạt'',N''68''),(N''24781'',N''Phường Xuân Hương - Đà Lạt'',N''68''),(N''24787'',N''Phường Cam Ly - Đà Lạt'',N''68''),(N''24805'',N''Phường Xuân Trường - Đà Lạt'',N''68''),(N''24820'',N''Phường 2 Bảo Lộc'',N''68''),(N''24823'',N''Phường 1 Bảo Lộc'',N''68''),(N''24829'',N''Phường B’Lao'',N''68''),(N''24841'',N''Phường 3 Bảo Lộc'',N''68''),(N''24846'',N''Phường Lang Biang - Đà Lạt'',N''68''),(N''24848'',N''Xã Lạc Dương'',N''68''),(N''24853'',N''Xã Đam Rông 4'',N''68''),(N''24868'',N''Xã Nam Ban Lâm Hà'',N''68''),(N''24871'',N''Xã Đinh Văn Lâm Hà'',N''68''),(N''24875'',N''Xã Đam Rông 3'',N''68''),(N''24877'',N''Xã Đam Rông 2'',N''68''),(N''24883'',N''Xã Nam Hà Lâm Hà'',N''68''),(N''24886'',N''Xã Đam Rông 1'',N''68''),(N''24895'',N''Xã Phú Sơn Lâm Hà'',N''68''),(N''24907'',N''Xã Phúc Thọ Lâm Hà'',N''68''),(N''24916'',N''Xã Tân Hà Lâm Hà'',N''68''),(N''24931'',N''Xã Đơn Dương'',N''68''),(N''24934'',N''Xã D’Ran'',N''68''),(N''24943'',N''Xã Ka Đô'',N''68''),(N''24955'',N''Xã Quảng Lập'',N''68''),(N''24958'',N''Xã Đức Trọng'',N''68''),(N''24967'',N''Xã Hiệp Thạnh'',N''68''),(N''24976'',N''Xã Tân Hội'',N''68''),(N''24985'',N''Xã Ninh Gia'',N''68''),(N''24988'',N''Xã Tà Năng'',N''68''),(N''24991'',N''Xã Tà Hine'',N''68''),(N''25000'',N''Xã Di Linh'',N''68''),(N''25007'',N''Xã Đinh Trang Thượng'',N''68''),(N''25015'',N''Xã Gia Hiệp'',N''68''),(N''25018'',N''Xã Bảo Thuận'',N''68''),(N''25036'',N''Xã Hòa Ninh'',N''68''),(N''25042'',N''Xã Hòa Bắc'',N''68''),(N''25051'',N''Xã Sơn Điền'',N''68''),(N''25054'',N''Xã Bảo Lâm 1'',N''68''),(N''25057'',N''Xã Bảo Lâm 5'',N''68''),(N''25063'',N''Xã Bảo Lâm 4'',N''68''),(N''25084'',N''Xã Bảo Lâm 2'',N''68''),(N''25093'',N''Xã Bảo Lâm 3'',N''68''),(N''25099'',N''Xã Đạ Huoai'',N''68''),(N''25105'',N''Xã Đạ Huoai 2'',N''68''),(N''25114'',N''Xã Đạ Huoai 3'',N''68''),(N''25126'',N''Xã Đạ Tẻh'',N''68''),(N''25135'',N''Xã Đạ Tẻh 3'',N''68''),(N''25138'',N''Xã Đạ Tẻh 2'',N''68''),(N''25159'',N''Xã Cát Tiên'',N''68''),(N''25162'',N''Xã Cát Tiên 3'',N''68''),(N''25180'',N''Xã Cát Tiên 2'',N''68''),(N''25195'',N''Phường Bình Phước'',N''75''),(N''25210'',N''Phường Đồng Xoài'',N''75''),(N''25217'',N''Phường Phước Long'',N''75''),(N''25220'',N''Phường Phước Bình'',N''75''),(N''25222'',N''Xã Bù Gia Mập'',N''75''),(N''25225'',N''Xã Đăk Ơ'',N''75''),(N''25231'',N''Xã Đa Kia'',N''75''),(N''25246'',N''Xã Bình Tân'',N''75''),(N''25252'',N''Xã Phú Riềng'',N''75''),(N''25255'',N''Xã Long Hà'',N''75''),(N''25261'',N''Xã Phú Trung'',N''75''),(N''25267'',N''Xã Phú Nghĩa'',N''75''),(N''25270'',N''Phường Lộc Ninh'',N''75''),(N''25279'',N''Xã Lộc Tấn'',N''75''),(N''25280'',N''Xã Lộc Thạnh'',N''75''),(N''25292'',N''Xã Lộc Quang'',N''75''),(N''25294'',N''Xã Lộc Thành'',N''75''),(N''25303'',N''Xã Lộc Hưng'',N''75''),(N''25308'',N''Xã Thiện Hưng'',N''75''),(N''25309'',N''Xã Hưng Phước'',N''75''),(N''25318'',N''Xã Tân Tiến'',N''75''),(N''25326'',N''Phường Bình Long'',N''75''),(N''25333'',N''Phường An Lộc'',N''75''),(N''25345'',N''Xã Tân Hưng'',N''75''),(N''25349'',N''Xã Minh Đức'',N''75''),(N''25351'',N''Xã Tân Quan'',N''75''),(N''25357'',N''Phường Tân Khai'',N''75''),(N''25363'',N''Phường Đồng Phú'',N''75''),(N''25378'',N''Xã Tân Lợi'',N''75''),(N''25387'',N''Xã Thuận Lợi'',N''75''),(N''25390'',N''Xã Đồng Tâm'',N''75''),(N''25396'',N''Xã Bù Đăng'',N''75''),(N''25399'',N''Xã Đak Nhau'',N''75''),(N''25402'',N''Xã Thọ Sơn'',N''75''),(N''25405'',N''Xã Bom Bo'',N''75''),(N''25417'',N''Xã Nghĩa Trung'',N''75''),(N''25420'',N''Xã Phước Sơn'',N''75''),(N''25432'',N''Phường Chơn Thành'',N''75''),(N''25441'',N''Phường Minh Hưng'',N''75''),(N''25450'',N''Xã Nha Bích'',N''75''),(N''25993'',N''Phường Trảng Dài'',N''75''),(N''26005'',N''Phường Hố Nai'',N''75''),(N''26017'',N''Phường Tam Hiệp'',N''75''),(N''26020'',N''Phường Long Bình'',N''75''),(N''26041'',N''Phường Trấn Biên'',N''75''),(N''26068'',N''Phường Biên Hòa'',N''75''),(N''26080'',N''Phường Long Khánh'',N''75''),(N''26089'',N''Phường Bình Lộc'',N''75''),(N''26098'',N''Phường Bảo Vinh'',N''75''),(N''26104'',N''Phường Xuân Lập'',N''75''),(N''26113'',N''Phường Hàng Gòn'',N''75''),(N''26116'',N''Phường Tân Phú'',N''75''),(N''26119'',N''Xã Đak Lua'',N''75''),(N''26122'',N''Xã Nam Cát Tiên'',N''75''),(N''26134'',N''Xã Tà Lài'',N''75''),(N''26158'',N''Xã Phú Lâm'',N''75''),(N''26170'',N''Phường Trị An'',N''75''),(N''26173'',N''Xã Phú Lý'',N''75''),(N''26179'',N''Xã Tân An'',N''75''),(N''26188'',N''Phường Tân Triều'',N''75''),(N''26206'',N''Xã Định Quán'',N''75''),(N''26209'',N''Xã Thanh Sơn'',N''75''),(N''26215'',N''Xã Phú Vinh'',N''75''),(N''26221'',N''Xã Phú Hòa'',N''75''),(N''26227'',N''Xã La Ngà'',N''75''),(N''26248'',N''Phường Trảng Bom'',N''75''),(N''26254'',N''Xã Bàu Hàm'',N''75''),(N''26278'',N''Xã Bình Minh'',N''75''),(N''26281'',N''Xã Hưng Thịnh'',N''75''),(N''26296'',N''Xã An Viễn'',N''75''),(N''26299'',N''Xã Thống Nhất'',N''75''),(N''26311'',N''Xã Gia Kiệm'',N''75''),(N''26326'',N''Phường Dầu Giây'',N''75''),(N''26332'',N''Xã Xuân Quế'',N''75''),(N''26341'',N''Xã Cẩm Mỹ'',N''75''),(N''26347'',N''Xã Xuân Đường'',N''75''),(N''26359'',N''Xã Xuân Đông'',N''75''),(N''26362'',N''Xã Sông Ray'',N''75''),(N''26368'',N''Phường Long Thành'',N''75''),(N''26374'',N''Phường Tam Phước'',N''75''),(N''26377'',N''Phường Phước Tân'',N''75''),(N''26380'',N''Phường Long Hưng'',N''75''),(N''26383'',N''Xã An Phước'',N''75''),(N''26389'',N''Xã Bình An'',N''75''),(N''26413'',N''Xã Long Phước'',N''75''),(N''26422'',N''Xã Phước Thái'',N''75''),(N''26425'',N''Phường Xuân Lộc'',N''75''),(N''26428'',N''Xã Xuân Bắc'',N''75''),(N''26434'',N''Xã Xuân Thành'',N''75''),(N''26446'',N''Xã Xuân Hòa'',N''75''),(N''26458'',N''Xã Xuân Phú'',N''75''),(N''26461'',N''Xã Xuân Định'',N''75''),(N''26485'',N''Phường Nhơn Trạch'',N''75''),(N''26491'',N''Xã Đại Phước'',N''75''),(N''26503'',N''Xã Phước An'',N''75''),(N''25747'',N''Phường Thủ Dầu Một'',N''79''),(N''25750'',N''Phường Phú Lợi'',N''79''),(N''25760'',N''Phường Bình Dương'',N''79''),(N''25768'',N''Phường Phú An'',N''79''),(N''25771'',N''Phường Chánh Hiệp'',N''79''),(N''25777'',N''Xã Dầu Tiếng'',N''79''),(N''25780'',N''Xã Minh Thạnh'',N''79''),(N''25792'',N''Xã Long Hòa'',N''79''),(N''25807'',N''Xã Thanh An'',N''79''),(N''25813'',N''Phường Bến Cát'',N''79''),(N''25819'',N''Xã Trừ Văn Thố'',N''79''),(N''25822'',N''Xã Bàu Bàng'',N''79''),(N''25837'',N''Phường Chánh Phú Hòa'',N''79''),(N''25840'',N''Phường Long Nguyên'',N''79''),(N''25843'',N''Phường Tây Nam'',N''79''),(N''25846'',N''Phường Thới Hòa'',N''79''),(N''25849'',N''Phường Hòa Lợi'',N''79''),(N''25858'',N''Xã Phú Giáo'',N''79''),(N''25864'',N''Xã Phước Thành'',N''79''),(N''25867'',N''Xã An Long'',N''79''),(N''25882'',N''Xã Phước Hòa'',N''79''),(N''25888'',N''Phường Tân Uyên'',N''79''),(N''25891'',N''Phường Tân Khánh'',N''79''),(N''25906'',N''Xã Bắc Tân Uyên'',N''79''),(N''25909'',N''Xã Thường Tân'',N''79''),(N''25912'',N''Phường Vĩnh Tân'',N''79''),(N''25915'',N''Phường Bình Cơ'',N''79''),(N''25920'',N''Phường Tân Hiệp'',N''79''),(N''25942'',N''Phường Dĩ An'',N''79''),(N''25945'',N''Phường Tân Đông Hiệp'',N''79''),(N''25951'',N''Phường Đông Hòa'',N''79''),(N''25966'',N''Phường Lái Thiêu'',N''79''),(N''25969'',N''Phường Thuận Giao'',N''79''),(N''25975'',N''Phường An Phú'',N''79''),(N''25978'',N''Phường Thuận An'',N''79''),(N''25987'',N''Phường Bình Hòa'',N''79''),(N''26506'',N''Phường Vũng Tàu'',N''79''),(N''26526'',N''Phường Tam Thắng'',N''79'')) s(ma,ten,tinh) JOIN dbo.tinh_thanh p ON p.ma_hanh_chinh=s.tinh WHERE NOT EXISTS(SELECT 1 FROM dbo.phuong_xa w WHERE w.ma=s.ma);');
 EXEC(N'INSERT dbo.phuong_xa(ma,ten,tinh_thanh_id) SELECT s.ma,s.ten,p.id FROM (VALUES(N''26536'',N''Phường Rạch Dừa'',N''79''),(N''26542'',N''Phường Phước Thắng'',N''79''),(N''26545'',N''Xã Long Sơn'',N''79''),(N''26560'',N''Phường Bà Rịa'',N''79''),(N''26566'',N''Phường Long Hương'',N''79''),(N''26572'',N''Phường Tam Long'',N''79''),(N''26575'',N''Xã Ngãi Giao'',N''79''),(N''26584'',N''Xã Xuân Sơn'',N''79''),(N''26590'',N''Xã Bình Giã'',N''79''),(N''26596'',N''Xã Châu Đức'',N''79''),(N''26608'',N''Xã Kim Long'',N''79''),(N''26617'',N''Xã Nghĩa Thành'',N''79''),(N''26620'',N''Xã Hồ Tràm'',N''79''),(N''26632'',N''Xã Xuyên Mộc'',N''79''),(N''26638'',N''Xã Bàu Lâm'',N''79''),(N''26641'',N''Xã Hòa Hội'',N''79''),(N''26647'',N''Xã Hòa Hiệp'',N''79''),(N''26656'',N''Xã Bình Châu'',N''79''),(N''26659'',N''Xã Long Điền'',N''79''),(N''26662'',N''Xã Long Hải'',N''79''),(N''26680'',N''Xã Đất Đỏ'',N''79''),(N''26686'',N''Xã Phước Hải'',N''79''),(N''26704'',N''Phường Phú Mỹ'',N''79''),(N''26710'',N''Phường Tân Hải'',N''79''),(N''26713'',N''Phường Tân Phước'',N''79''),(N''26725'',N''Phường Tân Thành'',N''79''),(N''26728'',N''Xã Châu Pha'',N''79''),(N''26732'',N''Đặc khu Côn Đảo'',N''79''),(N''26737'',N''Phường Tân Định'',N''79''),(N''26740'',N''Phường Sài Gòn'',N''79''),(N''26743'',N''Phường Bến Thành'',N''79''),(N''26758'',N''Phường Cầu Ông Lãnh'',N''79''),(N''26767'',N''Phường An Phú Đông'',N''79''),(N''26773'',N''Phường Thới An'',N''79''),(N''26782'',N''Phường Tân Thới Hiệp'',N''79''),(N''26785'',N''Phường Trung Mỹ Tây'',N''79''),(N''26791'',N''Phường Đông Hưng Thuận'',N''79''),(N''26800'',N''Phường Linh Xuân'',N''79''),(N''26803'',N''Phường Tam Bình'',N''79''),(N''26809'',N''Phường Hiệp Bình'',N''79''),(N''26824'',N''Phường Thủ Đức'',N''79''),(N''26833'',N''Phường Long Bình'',N''79''),(N''26842'',N''Phường Tăng Nhơn Phú'',N''79''),(N''26848'',N''Phường Phước Long'',N''79''),(N''26857'',N''Phường Long Phước'',N''79''),(N''26860'',N''Phường Long Trường'',N''79''),(N''26876'',N''Phường An Nhơn'',N''79''),(N''26878'',N''Phường An Hội Đông'',N''79''),(N''26882'',N''Phường An Hội Tây'',N''79''),(N''26884'',N''Phường Gò Vấp'',N''79''),(N''26890'',N''Phường Hạnh Thông'',N''79''),(N''26898'',N''Phường Thông Tây Hội'',N''79''),(N''26905'',N''Phường Bình Lợi Trung'',N''79''),(N''26911'',N''Phường Bình Quới'',N''79''),(N''26929'',N''Phường Bình Thạnh'',N''79''),(N''26944'',N''Phường Gia Định'',N''79''),(N''26956'',N''Phường Thạnh Mỹ Tây'',N''79''),(N''26968'',N''Phường Tân Sơn Nhất'',N''79''),(N''26977'',N''Phường Tân Sơn Hòa'',N''79''),(N''26983'',N''Phường Bảy Hiền'',N''79''),(N''26995'',N''Phường Tân Hòa'',N''79''),(N''27004'',N''Phường Tân Bình'',N''79''),(N''27007'',N''Phường Tân Sơn'',N''79''),(N''27013'',N''Phường Tây Thạnh'',N''79''),(N''27019'',N''Phường Tân Sơn Nhì'',N''79''),(N''27022'',N''Phường Phú Thọ Hòa'',N''79''),(N''27028'',N''Phường Phú Thạnh'',N''79''),(N''27031'',N''Phường Tân Phú'',N''79''),(N''27043'',N''Phường Đức Nhuận'',N''79''),(N''27058'',N''Phường Cầu Kiệu'',N''79''),(N''27073'',N''Phường Phú Nhuận'',N''79''),(N''27094'',N''Phường An Khánh'',N''79''),(N''27097'',N''Phường Bình Trưng'',N''79''),(N''27112'',N''Phường Cát Lái'',N''79''),(N''27139'',N''Phường Xuân Hòa'',N''79''),(N''27142'',N''Phường Nhiêu Lộc'',N''79''),(N''27154'',N''Phường Bàn Cờ'',N''79''),(N''27163'',N''Phường Hòa Hưng'',N''79''),(N''27169'',N''Phường Diên Hồng'',N''79''),(N''27190'',N''Phường Vườn Lài'',N''79''),(N''27211'',N''Phường Hòa Bình'',N''79''),(N''27226'',N''Phường Phú Thọ'',N''79''),(N''27232'',N''Phường Bình Thới'',N''79''),(N''27238'',N''Phường Minh Phụng'',N''79''),(N''27259'',N''Phường Xóm Chiếu'',N''79''),(N''27265'',N''Phường Khánh Hội'',N''79''),(N''27286'',N''Phường Vĩnh Hội'',N''79''),(N''27301'',N''Phường Chợ Quán'',N''79''),(N''27316'',N''Phường An Đông'',N''79''),(N''27343'',N''Phường Chợ Lớn'',N''79''),(N''27349'',N''Phường Phú Lâm'',N''79''),(N''27364'',N''Phường Bình Phú'',N''79''),(N''27367'',N''Phường Bình Tây'',N''79''),(N''27373'',N''Phường Bình Tiên'',N''79''),(N''27418'',N''Phường Chánh Hưng'',N''79''),(N''27424'',N''Phường Bình Đông'',N''79''),(N''27427'',N''Phường Phú Định'',N''79''),(N''27439'',N''Phường Bình Hưng Hòa'',N''79''),(N''27442'',N''Phường Bình Tân'',N''79''),(N''27448'',N''Phường Bình Trị Đông'',N''79''),(N''27457'',N''Phường Tân Tạo'',N''79''),(N''27460'',N''Phường An Lạc'',N''79''),(N''27475'',N''Phường Tân Hưng'',N''79''),(N''27478'',N''Phường Tân Thuận'',N''79''),(N''27484'',N''Phường Phú Thuận'',N''79''),(N''27487'',N''Phường Tân Mỹ'',N''79''),(N''27496'',N''Xã Tân An Hội'',N''79''),(N''27508'',N''Xã An Nhơn Tây'',N''79''),(N''27511'',N''Xã Nhuận Đức'',N''79''),(N''27526'',N''Xã Thái Mỹ'',N''79''),(N''27541'',N''Xã Phú Hòa Đông'',N''79''),(N''27544'',N''Xã Bình Mỹ'',N''79''),(N''27553'',N''Xã Củ Chi'',N''79''),(N''27559'',N''Xã Hóc Môn'',N''79''),(N''27568'',N''Xã Đông Thạnh'',N''79''),(N''27577'',N''Xã Xuân Thới Sơn'',N''79''),(N''27592'',N''Xã Bà Điểm'',N''79''),(N''27595'',N''Xã Tân Nhựt'',N''79''),(N''27601'',N''Xã Vĩnh Lộc'',N''79''),(N''27604'',N''Xã Tân Vĩnh Lộc'',N''79''),(N''27610'',N''Xã Bình Lợi'',N''79''),(N''27619'',N''Xã Bình Hưng'',N''79''),(N''27628'',N''Xã Hưng Long'',N''79''),(N''27637'',N''Xã Bình Chánh'',N''79''),(N''27655'',N''Xã Nhà Bè'',N''79''),(N''27658'',N''Xã Hiệp Phước'',N''79''),(N''27664'',N''Xã Cần Giờ'',N''79''),(N''27667'',N''Xã Bình Khánh'',N''79''),(N''27673'',N''Xã An Thới Đông'',N''79''),(N''27676'',N''Xã Thạnh An'',N''79''),(N''25459'',N''Phường Tân Ninh'',N''80''),(N''25480'',N''Phường Bình Minh'',N''80''),(N''25486'',N''Xã Tân Biên'',N''80''),(N''25489'',N''Xã Tân Lập'',N''80''),(N''25498'',N''Xã Thạnh Bình'',N''80''),(N''25510'',N''Xã Trà Vong'',N''80''),(N''25516'',N''Xã Tân Châu'',N''80''),(N''25522'',N''Xã Tân Đông'',N''80''),(N''25525'',N''Xã Tân Hội'',N''80''),(N''25531'',N''Xã Tân Hòa'',N''80''),(N''25534'',N''Xã Tân Thành'',N''80''),(N''25549'',N''Xã Tân Phú'',N''80''),(N''25552'',N''Xã Dương Minh Châu'',N''80''),(N''25567'',N''Phường Ninh Thạnh'',N''80''),(N''25573'',N''Xã Cầu Khởi'',N''80''),(N''25579'',N''Xã Lộc Ninh'',N''80''),(N''25585'',N''Xã Châu Thành'',N''80''),(N''25588'',N''Xã Hảo Đước'',N''80''),(N''25591'',N''Xã Phước Vinh'',N''80''),(N''25606'',N''Xã Hòa Hội'',N''80''),(N''25621'',N''Xã Ninh Điền'',N''80''),(N''25630'',N''Phường Long Hoa'',N''80''),(N''25633'',N''Phường Thanh Điền'',N''80''),(N''25645'',N''Phường Hòa Thành'',N''80''),(N''25654'',N''Phường Gò Dầu'',N''80''),(N''25657'',N''Xã Thạnh Đức'',N''80''),(N''25663'',N''Xã Phước Thạnh'',N''80''),(N''25666'',N''Xã Truông Mít'',N''80''),(N''25672'',N''Phường Gia Lộc'',N''80''),(N''25681'',N''Xã Bến Cầu'',N''80''),(N''25684'',N''Xã Long Chữ'',N''80''),(N''25702'',N''Xã Long Thuận'',N''80''),(N''25708'',N''Phường Trảng Bàng'',N''80''),(N''25711'',N''Xã Hưng Thuận'',N''80''),(N''25729'',N''Xã Phước Chỉ'',N''80''),(N''25732'',N''Phường An Tịnh'',N''80''),(N''27694'',N''Phường Long An'',N''80''),(N''27712'',N''Phường Tân An'',N''80''),(N''27715'',N''Phường Khánh Hậu'',N''80''),(N''27721'',N''Xã Tân Hưng'',N''80''),(N''27727'',N''Xã Hưng Điền'',N''80''),(N''27736'',N''Xã Vĩnh Thạnh'',N''80''),(N''27748'',N''Xã Vĩnh Châu'',N''80''),(N''27757'',N''Xã Vĩnh Hưng'',N''80''),(N''27763'',N''Xã Khánh Hưng'',N''80''),(N''27775'',N''Xã Tuyên Bình'',N''80''),(N''27787'',N''Phường Kiến Tường'',N''80''),(N''27793'',N''Xã Bình Hiệp'',N''80''),(N''27811'',N''Xã Bình Hòa'',N''80''),(N''27817'',N''Xã Tuyên Thạnh'',N''80''),(N''27823'',N''Xã Mộc Hóa'',N''80''),(N''27826'',N''Xã Tân Thạnh'',N''80''),(N''27838'',N''Xã Nhơn Hòa Lập'',N''80''),(N''27841'',N''Xã Hậu Thạnh'',N''80''),(N''27856'',N''Xã Nhơn Ninh'',N''80''),(N''27865'',N''Xã Thạnh Hóa'',N''80''),(N''27868'',N''Xã Bình Thành'',N''80''),(N''27877'',N''Xã Thạnh Phước'',N''80''),(N''27889'',N''Xã Tân Tây'',N''80''),(N''27898'',N''Xã Đông Thành'',N''80''),(N''27907'',N''Xã Mỹ Quý'',N''80''),(N''27925'',N''Xã Đức Huệ'',N''80''),(N''27931'',N''Xã Hậu Nghĩa'',N''80''),(N''27937'',N''Xã Đức Hòa'',N''80''),(N''27943'',N''Xã An Ninh'',N''80''),(N''27952'',N''Xã Hiệp Hòa'',N''80''),(N''27964'',N''Xã Đức Lập'',N''80''),(N''27976'',N''Xã Mỹ Hạnh'',N''80''),(N''27979'',N''Xã Hòa Khánh'',N''80''),(N''27991'',N''Xã Bến Lức'',N''80'')) s(ma,ten,tinh) JOIN dbo.tinh_thanh p ON p.ma_hanh_chinh=s.tinh WHERE NOT EXISTS(SELECT 1 FROM dbo.phuong_xa w WHERE w.ma=s.ma);');
 EXEC(N'INSERT dbo.phuong_xa(ma,ten,tinh_thanh_id) SELECT s.ma,s.ten,p.id FROM (VALUES(N''27994'',N''Xã Thạnh Lợi'',N''80''),(N''28003'',N''Xã Lương Hòa'',N''80''),(N''28015'',N''Xã Bình Đức'',N''80''),(N''28018'',N''Xã Mỹ Yên'',N''80''),(N''28036'',N''Xã Thủ Thừa'',N''80''),(N''28051'',N''Xã Mỹ Thạnh'',N''80''),(N''28066'',N''Xã Mỹ An'',N''80''),(N''28072'',N''Xã Tân Long'',N''80''),(N''28075'',N''Xã Tân Trụ'',N''80''),(N''28087'',N''Xã Nhựt Tảo'',N''80''),(N''28093'',N''Xã Vàm Cỏ'',N''80''),(N''28108'',N''Xã Cần Đước'',N''80''),(N''28114'',N''Xã Rạch Kiến'',N''80''),(N''28126'',N''Xã Long Cang'',N''80''),(N''28132'',N''Xã Mỹ Lệ'',N''80''),(N''28138'',N''Xã Tân Lân'',N''80''),(N''28144'',N''Xã Long Hựu'',N''80''),(N''28159'',N''Xã Cần Giuộc'',N''80''),(N''28165'',N''Xã Phước Lý'',N''80''),(N''28177'',N''Xã Mỹ Lộc'',N''80''),(N''28201'',N''Xã Phước Vĩnh Tây'',N''80''),(N''28207'',N''Xã Tân Tập'',N''80''),(N''28210'',N''Xã Tầm Vu'',N''80''),(N''28222'',N''Xã Vĩnh Công'',N''80''),(N''28225'',N''Xã Thuận Mỹ'',N''80''),(N''28243'',N''Xã An Lục Long'',N''80''),(N''28249'',N''Phường Đạo Thạnh'',N''82''),(N''28261'',N''Phường Mỹ Tho'',N''82''),(N''28270'',N''Phường Thới Sơn'',N''82''),(N''28273'',N''Phường Mỹ Phong'',N''82''),(N''28285'',N''Phường Trung An'',N''82''),(N''28297'',N''Phường Long Thuận'',N''82''),(N''28306'',N''Phường Gò Công'',N''82''),(N''28315'',N''Phường Bình Xuân'',N''82''),(N''28321'',N''Xã Tân Phước 1'',N''82''),(N''28327'',N''Xã Tân Phước 2'',N''82''),(N''28336'',N''Xã Hưng Thạnh'',N''82''),(N''28345'',N''Xã Tân Phước 3'',N''82''),(N''28360'',N''Xã Cái Bè'',N''82''),(N''28366'',N''Xã Hậu Mỹ'',N''82''),(N''28378'',N''Xã Mỹ Thiện'',N''82''),(N''28393'',N''Xã Hội Cư'',N''82''),(N''28405'',N''Xã Mỹ Đức Tây'',N''82''),(N''28414'',N''Xã Mỹ Lợi'',N''82''),(N''28426'',N''Xã Thanh Hưng'',N''82''),(N''28429'',N''Xã An Hữu'',N''82''),(N''28435'',N''Phường Mỹ Phước Tây'',N''82''),(N''28436'',N''Phường Thanh Hòa'',N''82''),(N''28439'',N''Phường Cai Lậy'',N''82''),(N''28444'',N''Xã Thạnh Phú'',N''82''),(N''28456'',N''Xã Mỹ Thành'',N''82''),(N''28468'',N''Xã Tân Phú'',N''82''),(N''28471'',N''Xã Bình Phú'',N''82''),(N''28477'',N''Phường Nhị Quý'',N''82''),(N''28501'',N''Xã Hiệp Đức'',N''82''),(N''28504'',N''Xã Long Tiên'',N''82''),(N''28516'',N''Xã Ngũ Hiệp'',N''82''),(N''28519'',N''Xã Châu Thành'',N''82''),(N''28525'',N''Xã Tân Hương'',N''82''),(N''28537'',N''Xã Long Hưng'',N''82''),(N''28543'',N''Xã Long Định'',N''82''),(N''28564'',N''Xã Bình Trưng'',N''82''),(N''28576'',N''Xã Vĩnh Kim'',N''82''),(N''28582'',N''Xã Kim Sơn'',N''82''),(N''28594'',N''Xã Chợ Gạo'',N''82''),(N''28603'',N''Xã Mỹ Tịnh An'',N''82''),(N''28615'',N''Xã Lương Hòa Lạc'',N''82''),(N''28627'',N''Xã Tân Thuận Bình'',N''82''),(N''28633'',N''Xã An Thạnh Thủy'',N''82''),(N''28648'',N''Xã Bình Ninh'',N''82''),(N''28651'',N''Xã Vĩnh Bình'',N''82''),(N''28660'',N''Xã Đồng Sơn'',N''82''),(N''28663'',N''Xã Phú Thành'',N''82''),(N''28678'',N''Xã Vĩnh Hựu'',N''82''),(N''28687'',N''Xã Long Bình'',N''82''),(N''28693'',N''Xã Tân Thới'',N''82''),(N''28696'',N''Xã Tân Phú Đông'',N''82''),(N''28702'',N''Xã Tân Hòa'',N''82''),(N''28720'',N''Xã Gia Thuận'',N''82''),(N''28723'',N''Xã Tân Đông'',N''82''),(N''28729'',N''Phường Sơn Qui'',N''82''),(N''28738'',N''Xã Tân Điền'',N''82''),(N''28747'',N''Xã Gò Công Đông'',N''82''),(N''29869'',N''Phường Cao Lãnh'',N''82''),(N''29884'',N''Phường Mỹ Ngãi'',N''82''),(N''29888'',N''Phường Mỹ Trà'',N''82''),(N''29905'',N''Phường Sa Đéc'',N''82''),(N''29926'',N''Xã Tân Hồng'',N''82''),(N''29929'',N''Xã Tân Hộ Cơ'',N''82''),(N''29938'',N''Xã Tân Thành'',N''82''),(N''29944'',N''Xã An Phước'',N''82''),(N''29954'',N''Phường An Bình'',N''82''),(N''29955'',N''Phường Hồng Ngự'',N''82''),(N''29971'',N''Xã Thường Phước'',N''82''),(N''29978'',N''Phường Thường Lạc'',N''82''),(N''29983'',N''Xã Long Khánh'',N''82''),(N''29992'',N''Xã Long Phú Thuận'',N''82''),(N''30001'',N''Xã Tràm Chim'',N''82''),(N''30010'',N''Xã Tam Nông'',N''82''),(N''30019'',N''Xã An Hòa'',N''82''),(N''30025'',N''Xã Phú Cường'',N''82''),(N''30028'',N''Xã An Long'',N''82''),(N''30034'',N''Xã Phú Thọ'',N''82''),(N''30037'',N''Xã Tháp Mười'',N''82''),(N''30043'',N''Xã Phương Thịnh'',N''82''),(N''30046'',N''Xã Trường Xuân'',N''82''),(N''30055'',N''Xã Mỹ Quí'',N''82''),(N''30061'',N''Xã Đốc Binh Kiều'',N''82''),(N''30073'',N''Xã Thanh Mỹ'',N''82''),(N''30076'',N''Xã Mỹ Thọ'',N''82''),(N''30085'',N''Xã Ba Sao'',N''82''),(N''30088'',N''Xã Phong Mỹ'',N''82''),(N''30112'',N''Xã Mỹ Hiệp'',N''82''),(N''30118'',N''Xã Bình Hàng Trung'',N''82''),(N''30130'',N''Xã Thanh Bình'',N''82''),(N''30154'',N''Xã Tân Long'',N''82''),(N''30157'',N''Xã Tân Thạnh'',N''82''),(N''30163'',N''Xã Bình Thành'',N''82''),(N''30169'',N''Xã Lấp Vò'',N''82''),(N''30178'',N''Xã Mỹ An Hưng'',N''82''),(N''30184'',N''Xã Tân Khánh Trung'',N''82''),(N''30208'',N''Xã Hòa Long'',N''82''),(N''30214'',N''Xã Tân Dương'',N''82''),(N''30226'',N''Xã Lai Vung'',N''82''),(N''30235'',N''Xã Phong Hòa'',N''82''),(N''30244'',N''Xã Phú Hựu'',N''82''),(N''30253'',N''Xã Tân Nhuận Đông'',N''82''),(N''30259'',N''Xã Tân Phú Trung'',N''82''),(N''28756'',N''Phường Phú Khương'',N''86''),(N''28777'',N''Phường An Hội'',N''86''),(N''28783'',N''Phường Sơn Đông'',N''86''),(N''28789'',N''Phường Bến Tre'',N''86''),(N''28807'',N''Xã Giao Long'',N''86''),(N''28810'',N''Xã Phú Túc'',N''86''),(N''28840'',N''Xã Tân Phú'',N''86''),(N''28858'',N''Phường Phú Tân'',N''86''),(N''28861'',N''Xã Tiên Thủy'',N''86''),(N''28870'',N''Xã Chợ Lách'',N''86''),(N''28879'',N''Xã Phú Phụng'',N''86''),(N''28894'',N''Xã Vĩnh Thành'',N''86''),(N''28901'',N''Xã Hưng Khánh Trung'',N''86''),(N''28903'',N''Xã Mỏ Cày'',N''86''),(N''28915'',N''Xã Phước Mỹ Trung'',N''86''),(N''28921'',N''Xã Tân Thành Bình'',N''86''),(N''28945'',N''Xã Đồng Khởi'',N''86''),(N''28948'',N''Xã Nhuận Phú Tân'',N''86''),(N''28957'',N''Xã An Định'',N''86''),(N''28969'',N''Xã Thành Thới'',N''86''),(N''28981'',N''Xã Hương Mỹ'',N''86''),(N''28984'',N''Xã Giồng Trôm'',N''86''),(N''28987'',N''Xã Lương Hòa'',N''86''),(N''28993'',N''Xã Lương Phú'',N''86''),(N''28996'',N''Xã Châu Hòa'',N''86''),(N''29020'',N''Xã Phước Long'',N''86''),(N''29029'',N''Xã Tân Hào'',N''86''),(N''29044'',N''Xã Hưng Nhượng'',N''86''),(N''29050'',N''Xã Bình Đại'',N''86''),(N''29062'',N''Xã Phú Thuận'',N''86''),(N''29077'',N''Xã Lộc Thuận'',N''86''),(N''29083'',N''Xã Châu Hưng'',N''86''),(N''29089'',N''Xã Thạnh Trị'',N''86''),(N''29104'',N''Xã Thạnh Phước'',N''86''),(N''29107'',N''Xã Thới Thuận'',N''86''),(N''29110'',N''Xã Ba Tri'',N''86''),(N''29122'',N''Xã Mỹ Chánh Hòa'',N''86''),(N''29125'',N''Xã Bảo Thạnh'',N''86''),(N''29137'',N''Xã Tân Xuân'',N''86''),(N''29143'',N''Xã An Ngãi Trung'',N''86''),(N''29158'',N''Xã An Hiệp'',N''86''),(N''29167'',N''Xã Tân Thủy'',N''86''),(N''29182'',N''Xã Thạnh Phú'',N''86''),(N''29191'',N''Xã Quới Điền'',N''86''),(N''29194'',N''Xã Đại Điền'',N''86''),(N''29221'',N''Xã Thạnh Hải'',N''86''),(N''29224'',N''Xã An Qui'',N''86''),(N''29227'',N''Xã Thạnh Phong'',N''86''),(N''29242'',N''Phường Trà Vinh'',N''86''),(N''29254'',N''Phường Nguyệt Hóa'',N''86''),(N''29263'',N''Phường Long Đức'',N''86''),(N''29266'',N''Xã Càng Long'',N''86''),(N''29275'',N''Xã An Trường'',N''86''),(N''29278'',N''Xã Tân An'',N''86''),(N''29287'',N''Xã Bình Phú'',N''86''),(N''29302'',N''Xã Nhị Long'',N''86''),(N''29308'',N''Xã Cầu Kè'',N''86''),(N''29317'',N''Xã An Phú Tân'',N''86''),(N''29329'',N''Xã Phong Thạnh'',N''86''),(N''29335'',N''Xã Tam Ngãi'',N''86''),(N''29341'',N''Xã Tiểu Cần'',N''86''),(N''29362'',N''Xã Hùng Hòa'',N''86''),(N''29365'',N''Xã Tập Ngãi'',N''86''),(N''29371'',N''Xã Tân Hòa'',N''86''),(N''29374'',N''Xã Châu Thành'',N''86''),(N''29386'',N''Xã Song Lộc'',N''86''),(N''29398'',N''Phường Hòa Thuận'',N''86''),(N''29407'',N''Xã Hưng Mỹ'',N''86''),(N''29410'',N''Xã Hòa Minh'',N''86''),(N''29413'',N''Xã Long Hòa'',N''86''),(N''29416'',N''Xã Cầu Ngang'',N''86''),(N''29419'',N''Xã Mỹ Long'',N''86'')) s(ma,ten,tinh) JOIN dbo.tinh_thanh p ON p.ma_hanh_chinh=s.tinh WHERE NOT EXISTS(SELECT 1 FROM dbo.phuong_xa w WHERE w.ma=s.ma);');
 EXEC(N'INSERT dbo.phuong_xa(ma,ten,tinh_thanh_id) SELECT s.ma,s.ten,p.id FROM (VALUES(N''29431'',N''Xã Vinh Kim'',N''86''),(N''29446'',N''Xã Nhị Trường'',N''86''),(N''29455'',N''Xã Hiệp Mỹ'',N''86''),(N''29461'',N''Xã Trà Cú'',N''86''),(N''29467'',N''Xã Tập Sơn'',N''86''),(N''29476'',N''Xã Lưu Nghiệp Anh'',N''86''),(N''29489'',N''Xã Hàm Giang'',N''86''),(N''29491'',N''Xã Đại An'',N''86''),(N''29497'',N''Xã Đôn Châu'',N''86''),(N''29506'',N''Xã Long Hiệp'',N''86''),(N''29512'',N''Phường Duyên Hải'',N''86''),(N''29513'',N''Xã Long Thành'',N''86''),(N''29516'',N''Phường Trường Long Hòa'',N''86''),(N''29518'',N''Xã Long Hữu'',N''86''),(N''29530'',N''Xã Ngũ Lạc'',N''86''),(N''29533'',N''Xã Long Vĩnh'',N''86''),(N''29536'',N''Xã Đông Hải'',N''86''),(N''29551'',N''Phường Long Châu'',N''86''),(N''29557'',N''Phường Phước Hậu'',N''86''),(N''29566'',N''Phường Tân Ngãi'',N''86''),(N''29584'',N''Xã An Bình'',N''86''),(N''29590'',N''Phường Thanh Đức'',N''86''),(N''29593'',N''Phường Tân Hạnh'',N''86''),(N''29602'',N''Xã Long Hồ'',N''86''),(N''29611'',N''Xã Phú Quới'',N''86''),(N''29623'',N''Xã Nhơn Phú'',N''86''),(N''29638'',N''Xã Bình Phước'',N''86''),(N''29641'',N''Xã Cái Nhum'',N''86''),(N''29653'',N''Xã Tân Long Hội'',N''86''),(N''29659'',N''Xã Trung Thành'',N''86''),(N''29668'',N''Xã Quới An'',N''86''),(N''29677'',N''Xã Quới Thiện'',N''86''),(N''29683'',N''Xã Trung Hiệp'',N''86''),(N''29698'',N''Xã Trung Ngãi'',N''86''),(N''29701'',N''Xã Hiếu Phụng'',N''86''),(N''29713'',N''Xã Hiếu Thành'',N''86''),(N''29719'',N''Xã Tam Bình'',N''86''),(N''29728'',N''Xã Cái Ngang'',N''86''),(N''29734'',N''Xã Hòa Hiệp'',N''86''),(N''29740'',N''Xã Song Phú'',N''86''),(N''29767'',N''Xã Ngãi Tứ'',N''86''),(N''29770'',N''Phường Cái Vồn'',N''86''),(N''29771'',N''Phường Bình Minh'',N''86''),(N''29785'',N''Xã Tân Lược'',N''86''),(N''29788'',N''Xã Mỹ Thuận'',N''86''),(N''29800'',N''Xã Tân Quới'',N''86''),(N''29812'',N''Phường Đông Thành'',N''86''),(N''29821'',N''Xã Trà Ôn'',N''86''),(N''29830'',N''Xã Hòa Bình'',N''86''),(N''29836'',N''Xã Trà Côn'',N''86''),(N''29845'',N''Xã Vĩnh Xuân'',N''86''),(N''29857'',N''Xã Lục Sĩ Thành'',N''86''),(N''30292'',N''Phường Bình Đức'',N''91''),(N''30301'',N''Phường Mỹ Thới'',N''91''),(N''30307'',N''Phường Long Xuyên'',N''91''),(N''30313'',N''Xã Mỹ Hòa Hưng'',N''91''),(N''30316'',N''Phường Châu Đốc'',N''91''),(N''30325'',N''Phường Vĩnh Tế'',N''91''),(N''30337'',N''Xã An Phú'',N''91''),(N''30341'',N''Xã Khánh Bình'',N''91''),(N''30346'',N''Xã Nhơn Hội'',N''91''),(N''30352'',N''Xã Phú Hữu'',N''91''),(N''30367'',N''Xã Vĩnh Hậu'',N''91''),(N''30376'',N''Phường Tân Châu'',N''91''),(N''30377'',N''Phường Long Phú'',N''91''),(N''30385'',N''Xã Vĩnh Xương'',N''91''),(N''30388'',N''Xã Tân An'',N''91''),(N''30403'',N''Xã Châu Phong'',N''91''),(N''30406'',N''Xã Phú Tân'',N''91''),(N''30409'',N''Xã Chợ Vàm'',N''91''),(N''30421'',N''Xã Phú Lâm'',N''91''),(N''30430'',N''Xã Hòa Lạc'',N''91''),(N''30436'',N''Xã Phú An'',N''91''),(N''30445'',N''Xã Bình Thạnh Đông'',N''91''),(N''30463'',N''Xã Châu Phú'',N''91''),(N''30469'',N''Xã Mỹ Đức'',N''91''),(N''30478'',N''Xã Vĩnh Thạnh Trung'',N''91''),(N''30481'',N''Xã Thạnh Mỹ Tây'',N''91''),(N''30487'',N''Xã Bình Mỹ'',N''91''),(N''30502'',N''Phường Thới Sơn'',N''91''),(N''30505'',N''Phường Chi Lăng'',N''91''),(N''30520'',N''Phường Tịnh Biên'',N''91''),(N''30526'',N''Xã An Cư'',N''91''),(N''30538'',N''Xã Núi Cấm'',N''91''),(N''30544'',N''Xã Tri Tôn'',N''91''),(N''30547'',N''Xã Ba Chúc'',N''91''),(N''30568'',N''Xã Vĩnh Gia'',N''91''),(N''30577'',N''Xã Ô Lâm'',N''91''),(N''30580'',N''Xã Cô Tô'',N''91''),(N''30589'',N''Xã An Châu'',N''91''),(N''30595'',N''Xã Cần Đăng'',N''91''),(N''30604'',N''Xã Vĩnh An'',N''91''),(N''30607'',N''Xã Bình Hòa'',N''91''),(N''30619'',N''Xã Vĩnh Hanh'',N''91''),(N''30628'',N''Xã Chợ Mới'',N''91''),(N''30631'',N''Xã Long Điền'',N''91''),(N''30643'',N''Xã Cù Lao Giêng'',N''91''),(N''30658'',N''Xã Nhơn Mỹ'',N''91''),(N''30664'',N''Xã Long Kiến'',N''91''),(N''30673'',N''Xã Hội An'',N''91''),(N''30682'',N''Xã Thoại Sơn'',N''91''),(N''30685'',N''Xã Phú Hòa'',N''91''),(N''30688'',N''Xã Óc Eo'',N''91''),(N''30691'',N''Xã Tây Phú'',N''91''),(N''30697'',N''Xã Vĩnh Trạch'',N''91''),(N''30709'',N''Xã Định Mỹ'',N''91''),(N''30742'',N''Phường Rạch Giá'',N''91''),(N''30760'',N''Phường Vĩnh Thông'',N''91''),(N''30766'',N''Phường Tô Châu'',N''91''),(N''30769'',N''Phường Hà Tiên'',N''91''),(N''30781'',N''Xã Tiên Hải'',N''91''),(N''30787'',N''Xã Kiên Lương'',N''91''),(N''30790'',N''Xã Hòa Điền'',N''91''),(N''30793'',N''Xã Vĩnh Điều'',N''91''),(N''30796'',N''Xã Giang Thành'',N''91''),(N''30811'',N''Xã Sơn Hải'',N''91''),(N''30814'',N''Xã Hòn Nghệ'',N''91''),(N''30817'',N''Xã Hòn Đất'',N''91''),(N''30823'',N''Xã Bình Sơn'',N''91''),(N''30826'',N''Xã Bình Giang'',N''91''),(N''30835'',N''Xã Sơn Kiên'',N''91''),(N''30838'',N''Xã Mỹ Thuận'',N''91''),(N''30850'',N''Xã Tân Hiệp'',N''91''),(N''30856'',N''Xã Tân Hội'',N''91''),(N''30874'',N''Xã Thạnh Đông'',N''91''),(N''30880'',N''Xã Châu Thành'',N''91''),(N''30886'',N''Xã Thạnh Lộc'',N''91''),(N''30898'',N''Xã Bình An'',N''91''),(N''30904'',N''Xã Giồng Riềng'',N''91''),(N''30910'',N''Xã Thạnh Hưng'',N''91''),(N''30928'',N''Xã Ngọc Chúc'',N''91''),(N''30934'',N''Xã Hòa Hưng'',N''91''),(N''30943'',N''Xã Long Thạnh'',N''91''),(N''30949'',N''Xã Hòa Thuận'',N''91''),(N''30952'',N''Xã Gò Quao'',N''91''),(N''30958'',N''Xã Định Hòa'',N''91''),(N''30970'',N''Xã Vĩnh Hòa Hưng'',N''91''),(N''30982'',N''Xã Vĩnh Tuy'',N''91''),(N''30985'',N''Xã An Biên'',N''91''),(N''30988'',N''Xã Tây Yên'',N''91''),(N''31006'',N''Xã Đông Thái'',N''91''),(N''31012'',N''Xã Vĩnh Hòa'',N''91''),(N''31018'',N''Xã An Minh'',N''91''),(N''31024'',N''Xã Đông Hòa'',N''91''),(N''31027'',N''Xã U Minh Thượng'',N''91''),(N''31031'',N''Xã Tân Thạnh'',N''91''),(N''31036'',N''Xã Đông Hưng'',N''91''),(N''31042'',N''Xã Vân Khánh'',N''91''),(N''31051'',N''Xã Vĩnh Phong'',N''91''),(N''31064'',N''Xã Vĩnh Bình'',N''91''),(N''31069'',N''Xã Vĩnh Thuận'',N''91''),(N''31078'',N''Đặc khu Phú Quốc'',N''91''),(N''31105'',N''Đặc khu Thổ Châu'',N''91''),(N''31108'',N''Đặc khu Kiên Hải'',N''91''),(N''31120'',N''Phường Cái Khế'',N''92''),(N''31135'',N''Phường Ninh Kiều'',N''92''),(N''31147'',N''Phường Tân An'',N''92''),(N''31150'',N''Phường An Bình'',N''92''),(N''31153'',N''Phường Ô Môn'',N''92''),(N''31157'',N''Phường Thới Long'',N''92''),(N''31162'',N''Phường Phước Thới'',N''92''),(N''31168'',N''Phường Bình Thủy'',N''92''),(N''31174'',N''Phường Thới An Đông'',N''92''),(N''31183'',N''Phường Long Tuyền'',N''92''),(N''31186'',N''Phường Cái Răng'',N''92''),(N''31201'',N''Phường Hưng Phú'',N''92''),(N''31207'',N''Phường Thốt Nốt'',N''92''),(N''31213'',N''Phường Tân Lộc'',N''92''),(N''31217'',N''Phường Trung Nhứt'',N''92''),(N''31228'',N''Phường Thuận Hưng'',N''92''),(N''31231'',N''Xã Thạnh An'',N''92''),(N''31232'',N''Xã Vĩnh Thạnh'',N''92''),(N''31237'',N''Xã Vĩnh Trinh'',N''92''),(N''31246'',N''Xã Thạnh Quới'',N''92''),(N''31249'',N''Xã Thạnh Phú'',N''92''),(N''31255'',N''Xã Trung Hưng'',N''92''),(N''31258'',N''Xã Thới Lai'',N''92''),(N''31261'',N''Xã Cờ Đỏ'',N''92''),(N''31264'',N''Xã Thới Hưng'',N''92''),(N''31273'',N''Xã Đông Hiệp'',N''92''),(N''31282'',N''Xã Đông Thuận'',N''92''),(N''31288'',N''Xã Trường Thành'',N''92''),(N''31294'',N''Xã Trường Xuân'',N''92''),(N''31299'',N''Xã Phong Điền'',N''92''),(N''31309'',N''Xã Trường Long'',N''92''),(N''31315'',N''Xã Nhơn Ái'',N''92''),(N''31321'',N''Phường Vị Thanh'',N''92''),(N''31333'',N''Phường Vị Tân'',N''92''),(N''31338'',N''Xã Hỏa Lựu'',N''92''),(N''31340'',N''Phường Ngã Bảy'',N''92''),(N''31342'',N''Xã Tân Hòa'',N''92''),(N''31348'',N''Xã Trường Long Tây'',N''92''),(N''31360'',N''Xã Thạnh Xuân'',N''92''),(N''31366'',N''Xã Châu Thành'',N''92''),(N''31369'',N''Xã Đông Phước'',N''92''),(N''31378'',N''Xã Phú Hữu'',N''92''),(N''31393'',N''Xã Hòa An'',N''92''),(N''31396'',N''Xã Hiệp Hưng'',N''92''),(N''31399'',N''Xã Tân Bình'',N''92''),(N''31408'',N''Xã Thạnh Hòa'',N''92'')) s(ma,ten,tinh) JOIN dbo.tinh_thanh p ON p.ma_hanh_chinh=s.tinh WHERE NOT EXISTS(SELECT 1 FROM dbo.phuong_xa w WHERE w.ma=s.ma);');
 EXEC(N'INSERT dbo.phuong_xa(ma,ten,tinh_thanh_id) SELECT s.ma,s.ten,p.id FROM (VALUES(N''31411'',N''Phường Đại Thành'',N''92''),(N''31420'',N''Xã Phụng Hiệp'',N''92''),(N''31426'',N''Xã Phương Bình'',N''92''),(N''31432'',N''Xã Tân Phước Hưng'',N''92''),(N''31441'',N''Xã Vị Thủy'',N''92''),(N''31453'',N''Xã Vĩnh Thuận Đông'',N''92''),(N''31459'',N''Xã Vĩnh Tường'',N''92''),(N''31465'',N''Xã Vị Thanh 1'',N''92''),(N''31471'',N''Phường Long Mỹ'',N''92''),(N''31473'',N''Phường Long Bình'',N''92''),(N''31480'',N''Phường Long Phú 1'',N''92''),(N''31489'',N''Xã Vĩnh Viễn'',N''92''),(N''31492'',N''Xã Lương Tâm'',N''92''),(N''31495'',N''Xã Xà Phiên'',N''92''),(N''31507'',N''Phường Sóc Trăng'',N''92''),(N''31510'',N''Phường Phú Lợi'',N''92''),(N''31528'',N''Xã Kế Sách'',N''92''),(N''31531'',N''Xã An Lạc Thôn'',N''92''),(N''31537'',N''Xã Phong Nẫm'',N''92''),(N''31540'',N''Xã Thới An Hội'',N''92''),(N''31552'',N''Xã Nhơn Mỹ'',N''92''),(N''31561'',N''Xã Đại Hải'',N''92''),(N''31567'',N''Xã Mỹ Tú'',N''92''),(N''31569'',N''Xã Phú Tâm'',N''92''),(N''31570'',N''Xã Hồ Đắc Kiện'',N''92''),(N''31579'',N''Xã Long Hưng'',N''92''),(N''31582'',N''Xã Thuận Hòa'',N''92''),(N''31591'',N''Xã Mỹ Hương'',N''92''),(N''31594'',N''Xã An Ninh'',N''92''),(N''31603'',N''Xã Mỹ Phước'',N''92''),(N''31615'',N''Xã An Thạnh'',N''92''),(N''31633'',N''Xã Cù Lao Dung'',N''92''),(N''31639'',N''Xã Long Phú'',N''92''),(N''31645'',N''Xã Đại Ngãi'',N''92''),(N''31654'',N''Xã Trường Khánh'',N''92''),(N''31666'',N''Xã Tân Thạnh'',N''92''),(N''31673'',N''Xã Trần Đề'',N''92''),(N''31675'',N''Xã Liêu Tú'',N''92''),(N''31679'',N''Xã Lịch Hội Thượng'',N''92''),(N''31684'',N''Phường Mỹ Xuyên'',N''92''),(N''31687'',N''Xã Tài Văn'',N''92''),(N''31699'',N''Xã Thạnh Thới An'',N''92''),(N''31708'',N''Xã Nhu Gia'',N''92''),(N''31717'',N''Xã Hòa Tú'',N''92''),(N''31723'',N''Xã Ngọc Tố'',N''92''),(N''31726'',N''Xã Gia Hòa'',N''92''),(N''31732'',N''Phường Ngã Năm'',N''92''),(N''31741'',N''Xã Tân Long'',N''92''),(N''31753'',N''Phường Mỹ Quới'',N''92''),(N''31756'',N''Xã Phú Lộc'',N''92''),(N''31759'',N''Xã Lâm Tân'',N''92''),(N''31777'',N''Xã Vĩnh Lợi'',N''92''),(N''31783'',N''Phường Vĩnh Châu'',N''92''),(N''31789'',N''Phường Khánh Hòa'',N''92''),(N''31795'',N''Xã Vĩnh Hải'',N''92''),(N''31804'',N''Phường Vĩnh Phước'',N''92''),(N''31810'',N''Xã Lai Hòa'',N''92''),(N''31825'',N''Phường Bạc Liêu'',N''96''),(N''31834'',N''Phường Vĩnh Trạch'',N''96''),(N''31840'',N''Phường Hiệp Thành'',N''96''),(N''31843'',N''Xã Hồng Dân'',N''96''),(N''31849'',N''Xã Ninh Quới'',N''96''),(N''31858'',N''Xã Vĩnh Lộc'',N''96''),(N''31864'',N''Xã Ninh Thạnh Lợi'',N''96''),(N''31867'',N''Xã Phước Long'',N''96''),(N''31876'',N''Xã Vĩnh Phước'',N''96''),(N''31882'',N''Xã Vĩnh Thanh'',N''96''),(N''31885'',N''Xã Phong Hiệp'',N''96''),(N''31891'',N''Xã Hòa Bình'',N''96''),(N''31894'',N''Xã Châu Thới'',N''96''),(N''31900'',N''Xã Vĩnh Lợi'',N''96''),(N''31906'',N''Xã Hưng Hội'',N''96''),(N''31918'',N''Xã Vĩnh Mỹ'',N''96''),(N''31927'',N''Xã Vĩnh Hậu'',N''96''),(N''31942'',N''Phường Giá Rai'',N''96''),(N''31951'',N''Phường Láng Tròn'',N''96''),(N''31957'',N''Xã Phong Thạnh'',N''96''),(N''31972'',N''Xã Gành Hào'',N''96''),(N''31975'',N''Xã Đông Hải'',N''96''),(N''31985'',N''Xã Long Điền'',N''96''),(N''31988'',N''Xã An Trạch'',N''96''),(N''31993'',N''Xã Định Thành'',N''96''),(N''32002'',N''Phường An Xuyên'',N''96''),(N''32014'',N''Phường Lý Văn Lâm'',N''96''),(N''32025'',N''Phường Tân Thành'',N''96''),(N''32041'',N''Phường Hòa Thành'',N''96''),(N''32044'',N''Xã Nguyễn Phích'',N''96''),(N''32047'',N''Xã U Minh'',N''96''),(N''32059'',N''Xã Khánh An'',N''96''),(N''32062'',N''Xã Khánh Lâm'',N''96''),(N''32065'',N''Xã Thới Bình'',N''96''),(N''32069'',N''Xã Biển Bạch'',N''96''),(N''32071'',N''Xã Trí Phải'',N''96''),(N''32083'',N''Xã Tân Lộc'',N''96''),(N''32092'',N''Xã Hồ Thị Kỷ'',N''96''),(N''32095'',N''Xã Trần Văn Thời'',N''96''),(N''32098'',N''Xã Sông Đốc'',N''96''),(N''32104'',N''Xã Đá Bạc'',N''96''),(N''32110'',N''Xã Khánh Bình'',N''96''),(N''32119'',N''Xã Khánh Hưng'',N''96''),(N''32128'',N''Xã Cái Nước'',N''96''),(N''32134'',N''Xã Lương Thế Trân'',N''96''),(N''32137'',N''Xã Tân Hưng'',N''96''),(N''32140'',N''Xã Hưng Mỹ'',N''96''),(N''32152'',N''Xã Đầm Dơi'',N''96''),(N''32155'',N''Xã Tạ An Khương'',N''96''),(N''32161'',N''Xã Trần Phán'',N''96''),(N''32167'',N''Xã Tân Thuận'',N''96''),(N''32182'',N''Xã Quách Phẩm'',N''96''),(N''32185'',N''Xã Thanh Tùng'',N''96''),(N''32188'',N''Xã Tân Tiến'',N''96''),(N''32191'',N''Xã Năm Căn'',N''96''),(N''32201'',N''Xã Đất Mới'',N''96''),(N''32206'',N''Xã Tam Giang'',N''96''),(N''32212'',N''Xã Cái Đôi Vàm'',N''96''),(N''32214'',N''Xã Phú Mỹ'',N''96''),(N''32218'',N''Xã Phú Tân'',N''96''),(N''32227'',N''Xã Nguyễn Việt Khái'',N''96''),(N''32236'',N''Xã Tân Ân'',N''96''),(N''32244'',N''Xã Phan Ngọc Hiển'',N''96''),(N''32248'',N''Xã Đất Mũi'',N''96'')) s(ma,ten,tinh) JOIN dbo.tinh_thanh p ON p.ma_hanh_chinh=s.tinh WHERE NOT EXISTS(SELECT 1 FROM dbo.phuong_xa w WHERE w.ma=s.ma);');
 IF COL_LENGTH(N'dbo.dia_chi_nhan_hang',N'phuong_xa_id') IS NULL ALTER TABLE dbo.dia_chi_nhan_hang ADD phuong_xa_id INT NULL;
 IF COL_LENGTH(N'dbo.dia_chi_nhan_hang',N'so_nha_duong') IS NULL ALTER TABLE dbo.dia_chi_nhan_hang ADD so_nha_duong NVARCHAR(255) NULL;
 IF COL_LENGTH(N'dbo.dia_chi_nhan_hang',N'latitude') IS NULL ALTER TABLE dbo.dia_chi_nhan_hang ADD latitude DECIMAL(10,7) NULL;
 IF COL_LENGTH(N'dbo.dia_chi_nhan_hang',N'longitude') IS NULL ALTER TABLE dbo.dia_chi_nhan_hang ADD longitude DECIMAL(10,7) NULL;
 IF COL_LENGTH(N'dbo.dia_chi_nhan_hang',N'nguon_toa_do') IS NULL ALTER TABLE dbo.dia_chi_nhan_hang ADD nguon_toa_do NVARCHAR(20) NULL;
 IF NOT EXISTS(SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_dia_chi_nhan_hang_phuong_xa') EXEC(N'ALTER TABLE dbo.dia_chi_nhan_hang ADD CONSTRAINT FK_dia_chi_nhan_hang_phuong_xa FOREIGN KEY(phuong_xa_id) REFERENCES dbo.phuong_xa(id)');
 IF NOT EXISTS(SELECT 1 FROM sys.check_constraints WHERE name=N'CK_dia_chi_nhan_hang_delivery_point') EXEC(N'ALTER TABLE dbo.dia_chi_nhan_hang ADD CONSTRAINT CK_dia_chi_nhan_hang_delivery_point CHECK((latitude IS NULL AND longitude IS NULL) OR (latitude IS NOT NULL AND longitude IS NOT NULL AND latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180))');
 IF NOT EXISTS(SELECT 1 FROM sys.check_constraints WHERE name=N'CK_dia_chi_nhan_hang_point_source') EXEC(N'ALTER TABLE dbo.dia_chi_nhan_hang ADD CONSTRAINT CK_dia_chi_nhan_hang_point_source CHECK(nguon_toa_do IS NULL OR nguon_toa_do IN(N''GPS'',N''GHIM_TAY'',N''TAM_PHUONG_XA''))');
 IF COL_LENGTH(N'dbo.orders',N'phuong_xa_id') IS NULL ALTER TABLE dbo.orders ADD phuong_xa_id INT NULL;
 IF COL_LENGTH(N'dbo.orders',N'ma_tinh_giao') IS NULL ALTER TABLE dbo.orders ADD ma_tinh_giao NVARCHAR(2) NULL;
 IF COL_LENGTH(N'dbo.orders',N'ma_phuong_giao') IS NULL ALTER TABLE dbo.orders ADD ma_phuong_giao NVARCHAR(5) NULL;
 IF COL_LENGTH(N'dbo.orders',N'ten_phuong_giao') IS NULL ALTER TABLE dbo.orders ADD ten_phuong_giao NVARCHAR(100) NULL;
 IF COL_LENGTH(N'dbo.orders',N'so_nha_duong') IS NULL ALTER TABLE dbo.orders ADD so_nha_duong NVARCHAR(255) NULL;
 IF COL_LENGTH(N'dbo.orders',N'dia_chi_giao_snapshot') IS NULL ALTER TABLE dbo.orders ADD dia_chi_giao_snapshot NVARCHAR(600) NULL;
 IF COL_LENGTH(N'dbo.orders',N'latitude') IS NULL ALTER TABLE dbo.orders ADD latitude DECIMAL(10,7) NULL;
 IF COL_LENGTH(N'dbo.orders',N'longitude') IS NULL ALTER TABLE dbo.orders ADD longitude DECIMAL(10,7) NULL;
 IF COL_LENGTH(N'dbo.orders',N'nguon_toa_do') IS NULL ALTER TABLE dbo.orders ADD nguon_toa_do NVARCHAR(20) NULL;
 IF COL_LENGTH(N'dbo.orders',N'distance_km') IS NULL ALTER TABLE dbo.orders ADD distance_km DECIMAL(12,2) NULL;
 IF COL_LENGTH(N'dbo.orders',N'is_free_ship') IS NULL ALTER TABLE dbo.orders ADD is_free_ship BIT NULL;
 IF COL_LENGTH(N'dbo.orders',N'shipping_policy_version') IS NULL ALTER TABLE dbo.orders ADD shipping_policy_version INT NULL;
 IF COL_LENGTH(N'dbo.orders',N'shipping_note') IS NULL ALTER TABLE dbo.orders ADD shipping_note NVARCHAR(255) NULL;
 IF COL_LENGTH(N'dbo.orders',N'order_source') IS NULL ALTER TABLE dbo.orders ADD order_source NVARCHAR(30) NULL;
 IF COL_LENGTH(N'dbo.orders',N'sales_note') IS NULL ALTER TABLE dbo.orders ADD sales_note NVARCHAR(1000) NULL;
 IF NOT EXISTS(SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_orders_phuong_xa') EXEC(N'ALTER TABLE dbo.orders ADD CONSTRAINT FK_orders_phuong_xa FOREIGN KEY(phuong_xa_id) REFERENCES dbo.phuong_xa(id)');
 IF NOT EXISTS(SELECT 1 FROM sys.check_constraints WHERE name=N'CK_orders_delivery_point') EXEC(N'ALTER TABLE dbo.orders ADD CONSTRAINT CK_orders_delivery_point CHECK((latitude IS NULL AND longitude IS NULL) OR (latitude IS NOT NULL AND longitude IS NOT NULL AND latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180))');
 IF NOT EXISTS(SELECT 1 FROM sys.check_constraints WHERE name=N'CK_orders_point_source') EXEC(N'ALTER TABLE dbo.orders ADD CONSTRAINT CK_orders_point_source CHECK(nguon_toa_do IS NULL OR nguon_toa_do IN(N''GPS'',N''GHIM_TAY'',N''TAM_PHUONG_XA''))');
 IF (SELECT COUNT(*) FROM dbo.phuong_xa)<3321 THROW 51000,N'Chưa nạp đủ danh mục phường/xã; kiểm tra mã tỉnh.',1;
 COMMIT;
 PRINT N'HOÀN TẤT 016_dia_chi_phi_ship_don_online.sql';
END TRY
BEGIN CATCH
 IF @@TRANCOUNT>0 ROLLBACK;
 THROW;
END CATCH;

GO
