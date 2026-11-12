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
