-- Bổ sung trạng thái kích cỡ; giữ nguyên mã và dữ liệu đang có.
IF DB_NAME() NOT IN (N'shopbangiay_kt',N'shop_verify',N'shop_fresh')
 AND DB_NAME() NOT LIKE N'shopbangiay[_]kt[_]%'
 AND NOT (DB_NAME()=N'shopbangiay' AND COALESCE(TRY_CONVERT(INT,SESSION_CONTEXT(N'shop.allow-production-migration')),0)=1)
 THROW 51000,N'Database không được phép chạy migration.',1;
SET XACT_ABORT ON;
BEGIN TRY
BEGIN TRANSACTION;
IF OBJECT_ID(N'dbo.sizes',N'U') IS NULL
 THROW 51000,N'Thiếu bảng dbo.sizes. Cần tạo schema gốc trước migration 022.',1;
IF COL_LENGTH(N'dbo.sizes', N'trang_thai') IS NULL
    ALTER TABLE dbo.sizes ADD trang_thai BIT NOT NULL CONSTRAINT DF_sizes_trang_thai DEFAULT(1) WITH VALUES;
COMMIT;
PRINT N'HOÀN TẤT 022_kich_co_trang_thai.sql';
END TRY
BEGIN CATCH
IF @@TRANCOUNT>0 ROLLBACK;
THROW;
END CATCH;
