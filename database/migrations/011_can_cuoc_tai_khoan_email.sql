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
