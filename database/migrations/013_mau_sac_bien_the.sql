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
