-- Gộp migrations 004 đến 012. Chạy bằng sqlcmd -b -I -d <database> -i database/migrations/chay_tat_ca.sql.
-- Không tạo/chuyển database. Không tự chạy trên DB thật.
SET NOCOUNT ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET ARITHABORT ON;
SET NUMERIC_ROUNDABORT OFF;
SET XACT_ABORT ON;
IF DB_NAME() NOT IN (N'shopbangiay_kt',N'shop_verify',N'shop_fresh')
    AND DB_NAME() NOT LIKE N'shopbangiay[_]kt[_]%'
    AND NOT (DB_NAME()=N'shopbangiay' AND COALESCE(TRY_CONVERT(INT,SESSION_CONTEXT(N'shop.allow-production-migration')),0)=1)
    THROW 51000,N'Database không được phép chạy bộ migration.',1;
GO

PRINT N'BẮT ĐẦU 004_anh_san_pham.sql';
GO
IF DB_NAME() NOT IN (N'shopbangiay_kt',N'shop_verify',N'shop_fresh',N'shopbangiay_thu')
   AND NOT (DB_NAME()=N'shopbangiay' AND COALESCE(TRY_CONVERT(INT,SESSION_CONTEXT(N'shop.allow-production-migration')),0)=1)
    AND DB_NAME() NOT LIKE N'shopbangiay[_]kt[_]%'
    THROW 51000,N'Database không được phép chạy migration.',1;
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
PRINT N'HOÀN TẤT 004_anh_san_pham.sql';
GO

PRINT N'BẮT ĐẦU 005_chat_lieu.sql';
GO
IF DB_NAME() NOT IN (N'shopbangiay_kt',N'shop_verify',N'shop_fresh',N'shopbangiay_thu')
   AND NOT (DB_NAME()=N'shopbangiay' AND COALESCE(TRY_CONVERT(INT,SESSION_CONTEXT(N'shop.allow-production-migration')),0)=1)
    AND DB_NAME() NOT LIKE N'shopbangiay[_]kt[_]%'
    THROW 51000,N'Database không được phép chạy migration.',1;
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
PRINT N'HOÀN TẤT 005_chat_lieu.sql';
GO

PRINT N'BẮT ĐẦU 006_luat_ton_kho.sql';
GO
IF DB_NAME() NOT IN (N'shopbangiay_kt',N'shop_verify',N'shop_fresh',N'shopbangiay_thu')
   AND NOT (DB_NAME()=N'shopbangiay' AND COALESCE(TRY_CONVERT(INT,SESSION_CONTEXT(N'shop.allow-production-migration')),0)=1)
    AND DB_NAME() NOT LIKE N'shopbangiay[_]kt[_]%'
    THROW 51000,N'Database không được phép chạy migration.',1;
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
PRINT N'HOÀN TẤT 006_luat_ton_kho.sql';
GO

PRINT N'BẮT ĐẦU 007_tieng_viet_unicode.sql';
GO
IF DB_NAME() NOT IN (N'shopbangiay_kt',N'shop_verify',N'shop_fresh',N'shopbangiay_thu')
   AND NOT (DB_NAME()=N'shopbangiay' AND COALESCE(TRY_CONVERT(INT,SESSION_CONTEXT(N'shop.allow-production-migration')),0)=1)
    AND DB_NAME() NOT LIKE N'shopbangiay[_]kt[_]%'
    THROW 51000,N'Database không được phép chạy migration.',1;
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
PRINT N'HOÀN TẤT 007_tieng_viet_unicode.sql';
GO

PRINT N'BẮT ĐẦU 008_nvarchar_con_lai.sql';
GO
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
      -- 011/013: so_cccd và ma_mau là mã ASCII, giữ VARCHAR theo hợp đồng schema.
      -- Migration 009 quy định số điện thoại sổ địa chỉ là VARCHAR(20).
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
PRINT N'HOÀN TẤT 008_nvarchar_con_lai.sql';
GO

PRINT N'BẮT ĐẦU 009_dia_chi_nhan_hang.sql';
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
PRINT N'HOÀN TẤT 009_dia_chi_nhan_hang.sql';
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
