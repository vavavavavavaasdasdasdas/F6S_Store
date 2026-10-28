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
