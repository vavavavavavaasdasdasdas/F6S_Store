IF DB_NAME() NOT IN (N'shopbangiay_kt',N'shop_verify',N'shop_fresh')
 AND DB_NAME() NOT LIKE N'shopbangiay[_]kt[_]%'
 AND NOT (DB_NAME()=N'shopbangiay' AND COALESCE(TRY_CONVERT(INT,SESSION_CONTEXT(N'shop.allow-production-migration')),0)=1)
 THROW 51000,N'Database không được phép chạy migration.',1;
SET XACT_ABORT ON;
BEGIN TRY
BEGIN TRANSACTION;
IF OBJECT_ID(N'dbo.danh_gia_ma_seq',N'SO') IS NULL
 CREATE SEQUENCE dbo.danh_gia_ma_seq AS BIGINT START WITH 1 MAXVALUE 999999999999999999 NO CYCLE;
IF OBJECT_ID(N'dbo.danh_gia',N'U') IS NULL
 CREATE TABLE dbo.danh_gia(
  id INT IDENTITY PRIMARY KEY,
  ma VARCHAR(20) NOT NULL CONSTRAINT df_danh_gia_ma DEFAULT(CONCAT('DG',FORMAT(NEXT VALUE FOR dbo.danh_gia_ma_seq,'0000'))) CONSTRAINT uq_danh_gia_ma UNIQUE,
  product_id INT NOT NULL REFERENCES dbo.products(id),
  customer_id INT NOT NULL REFERENCES dbo.customers(id),
  order_id INT NOT NULL REFERENCES dbo.orders(id),
  so_sao INT NOT NULL CONSTRAINT ck_danh_gia_sao CHECK(so_sao BETWEEN 1 AND 5),
  noi_dung NVARCHAR(1000) NOT NULL,
  hien_thi BIT NOT NULL CONSTRAINT df_danh_gia_hien_thi DEFAULT(1),
  created_at DATETIME2 NOT NULL,updated_at DATETIME2 NOT NULL,
  CONSTRAINT uq_danh_gia_customer_product UNIQUE(customer_id,product_id)
 );
IF OBJECT_ID(N'dbo.yeu_thich',N'U') IS NULL
 CREATE TABLE dbo.yeu_thich(
  customer_id INT NOT NULL REFERENCES dbo.customers(id) ON DELETE CASCADE,
  product_id INT NOT NULL REFERENCES dbo.products(id) ON DELETE CASCADE,
  created_at DATETIME2 NOT NULL,
  CONSTRAINT pk_yeu_thich PRIMARY KEY(customer_id,product_id)
 );
DECLARE @favoriteFk SYSNAME,@parent SYSNAME,@command NVARCHAR(1000);
DECLARE favoriteKeys CURSOR LOCAL FAST_FORWARD FOR SELECT fk.name,OBJECT_NAME(fk.referenced_object_id) FROM sys.foreign_keys fk WHERE fk.parent_object_id=OBJECT_ID(N'dbo.yeu_thich') AND fk.delete_referential_action<>1 AND fk.referenced_object_id IN (OBJECT_ID(N'dbo.customers'),OBJECT_ID(N'dbo.products'));
OPEN favoriteKeys;FETCH NEXT FROM favoriteKeys INTO @favoriteFk,@parent;
WHILE @@FETCH_STATUS=0
BEGIN
 SET @command=N'ALTER TABLE dbo.yeu_thich DROP CONSTRAINT '+QUOTENAME(@favoriteFk);EXEC(@command);
 SET @command=N'ALTER TABLE dbo.yeu_thich WITH CHECK ADD CONSTRAINT '+QUOTENAME(N'fk_yeu_thich_'+@parent)+N' FOREIGN KEY('+CASE WHEN @parent=N'customers' THEN N'customer_id' ELSE N'product_id' END+N') REFERENCES dbo.'+QUOTENAME(@parent)+N'(id) ON DELETE CASCADE';EXEC(@command);
 FETCH NEXT FROM favoriteKeys INTO @favoriteFk,@parent;
END;
CLOSE favoriteKeys;DEALLOCATE favoriteKeys;
IF COL_LENGTH(N'customers',N'danh_gia_cua_so') IS NULL ALTER TABLE customers ADD danh_gia_cua_so DATETIME2 NULL;
IF COL_LENGTH(N'customers',N'danh_gia_so_lan') IS NULL ALTER TABLE customers ADD danh_gia_so_lan INT NOT NULL CONSTRAINT df_customer_review_count DEFAULT(0) WITH VALUES;
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'danh_gia') AND name=N'ix_danh_gia_product_visible_date')
 CREATE INDEX ix_danh_gia_product_visible_date ON danh_gia(product_id,hien_thi,created_at);
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'yeu_thich') AND name=N'ix_yeu_thich_product_date')
 CREATE INDEX ix_yeu_thich_product_date ON yeu_thich(product_id,created_at);
COMMIT;
PRINT N'HOÀN TẤT 021_danh_gia_yeu_thich.sql';
END TRY
BEGIN CATCH
IF @@TRANCOUNT>0 ROLLBACK;
THROW;
END CATCH;
