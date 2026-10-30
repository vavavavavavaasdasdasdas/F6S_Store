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
