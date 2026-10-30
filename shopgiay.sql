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
