IF DB_NAME() NOT IN (N'shopbangiay_kt',N'shop_verify',N'shop_fresh')
 AND DB_NAME() NOT LIKE N'shopbangiay[_]kt[_]%'
 AND NOT (DB_NAME()=N'shopbangiay' AND COALESCE(TRY_CONVERT(INT,SESSION_CONTEXT(N'shop.allow-production-migration')),0)=1)
 THROW 51000,N'Database không được phép chạy migration.',1;
SET XACT_ABORT ON;
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
BEGIN TRY
BEGIN TRANSACTION;
IF COL_LENGTH(N'dbo.categories',N'ma') IS NULL ALTER TABLE dbo.categories ADD ma VARCHAR(20) NULL;
EXEC(N';WITH missing AS (SELECT id,ROW_NUMBER() OVER(ORDER BY id) n FROM dbo.categories WHERE ma IS NULL), base AS (SELECT COALESCE(MAX(TRY_CONVERT(BIGINT,SUBSTRING(ma,3,18))),0) n FROM dbo.categories) UPDATE t SET ma=''TH''+FORMAT(m.n+b.n,''000'') FROM dbo.categories t JOIN missing m ON m.id=t.id CROSS JOIN base b;');
IF OBJECT_ID(N'dbo.categories_ma_seq',N'SO') IS NULL
BEGIN
 DECLARE @next_categories BIGINT; EXEC sys.sp_executesql N'SELECT @n=COALESCE(MAX(TRY_CONVERT(BIGINT,SUBSTRING(ma,3,18))),0)+1 FROM dbo.categories',N'@n BIGINT OUTPUT',@n=@next_categories OUTPUT;
 DECLARE @create_categories NVARCHAR(MAX)=N'CREATE SEQUENCE dbo.categories_ma_seq AS BIGINT START WITH '+CONVERT(NVARCHAR(20),@next_categories)+N' MAXVALUE 999999999999999999 NO CYCLE'; EXEC(@create_categories);
END;
IF NOT EXISTS(SELECT 1 FROM sys.default_constraints d JOIN sys.columns c ON c.object_id=d.parent_object_id AND c.column_id=d.parent_column_id WHERE d.parent_object_id=OBJECT_ID(N'dbo.categories') AND c.name=N'ma')
 EXEC(N'ALTER TABLE dbo.categories ADD CONSTRAINT DF_categories_ma DEFAULT(CONCAT(''TH'',FORMAT(NEXT VALUE FOR dbo.categories_ma_seq,''000''))) FOR ma');
IF EXISTS(SELECT 1 FROM sys.columns WHERE object_id=OBJECT_ID(N'dbo.categories') AND name=N'ma' AND is_nullable=1) EXEC(N'ALTER TABLE dbo.categories ALTER COLUMN ma VARCHAR(20) NOT NULL');
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.categories') AND name=N'UX_categories_ma') EXEC(N'CREATE UNIQUE INDEX UX_categories_ma ON dbo.categories(ma)');
PRINT N'017: categories — ma TH, SEQUENCE không đặt lùi';
IF COL_LENGTH(N'dbo.sizes',N'ma') IS NULL ALTER TABLE dbo.sizes ADD ma VARCHAR(20) NULL;
EXEC(N';WITH missing AS (SELECT id,ROW_NUMBER() OVER(ORDER BY id) n FROM dbo.sizes WHERE ma IS NULL), base AS (SELECT COALESCE(MAX(TRY_CONVERT(BIGINT,SUBSTRING(ma,3,18))),0) n FROM dbo.sizes) UPDATE t SET ma=''KC''+FORMAT(m.n+b.n,''000'') FROM dbo.sizes t JOIN missing m ON m.id=t.id CROSS JOIN base b;');
IF OBJECT_ID(N'dbo.sizes_ma_seq',N'SO') IS NULL
BEGIN
 DECLARE @next_sizes BIGINT; EXEC sys.sp_executesql N'SELECT @n=COALESCE(MAX(TRY_CONVERT(BIGINT,SUBSTRING(ma,3,18))),0)+1 FROM dbo.sizes',N'@n BIGINT OUTPUT',@n=@next_sizes OUTPUT;
 DECLARE @create_sizes NVARCHAR(MAX)=N'CREATE SEQUENCE dbo.sizes_ma_seq AS BIGINT START WITH '+CONVERT(NVARCHAR(20),@next_sizes)+N' MAXVALUE 999999999999999999 NO CYCLE'; EXEC(@create_sizes);
END;
IF NOT EXISTS(SELECT 1 FROM sys.default_constraints d JOIN sys.columns c ON c.object_id=d.parent_object_id AND c.column_id=d.parent_column_id WHERE d.parent_object_id=OBJECT_ID(N'dbo.sizes') AND c.name=N'ma')
 EXEC(N'ALTER TABLE dbo.sizes ADD CONSTRAINT DF_sizes_ma DEFAULT(CONCAT(''KC'',FORMAT(NEXT VALUE FOR dbo.sizes_ma_seq,''000''))) FOR ma');
IF EXISTS(SELECT 1 FROM sys.columns WHERE object_id=OBJECT_ID(N'dbo.sizes') AND name=N'ma' AND is_nullable=1) EXEC(N'ALTER TABLE dbo.sizes ALTER COLUMN ma VARCHAR(20) NOT NULL');
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.sizes') AND name=N'UX_sizes_ma') EXEC(N'CREATE UNIQUE INDEX UX_sizes_ma ON dbo.sizes(ma)');
PRINT N'017: sizes — ma KC, SEQUENCE không đặt lùi';
IF COL_LENGTH(N'dbo.chat_lieu',N'ma') IS NULL ALTER TABLE dbo.chat_lieu ADD ma VARCHAR(20) NULL;
EXEC(N';WITH missing AS (SELECT id,ROW_NUMBER() OVER(ORDER BY id) n FROM dbo.chat_lieu WHERE ma IS NULL), base AS (SELECT COALESCE(MAX(TRY_CONVERT(BIGINT,SUBSTRING(ma,3,18))),0) n FROM dbo.chat_lieu) UPDATE t SET ma=''CL''+FORMAT(m.n+b.n,''000'') FROM dbo.chat_lieu t JOIN missing m ON m.id=t.id CROSS JOIN base b;');
IF OBJECT_ID(N'dbo.chat_lieu_ma_seq',N'SO') IS NULL
BEGIN
 DECLARE @next_chat_lieu BIGINT; EXEC sys.sp_executesql N'SELECT @n=COALESCE(MAX(TRY_CONVERT(BIGINT,SUBSTRING(ma,3,18))),0)+1 FROM dbo.chat_lieu',N'@n BIGINT OUTPUT',@n=@next_chat_lieu OUTPUT;
 DECLARE @create_chat_lieu NVARCHAR(MAX)=N'CREATE SEQUENCE dbo.chat_lieu_ma_seq AS BIGINT START WITH '+CONVERT(NVARCHAR(20),@next_chat_lieu)+N' MAXVALUE 999999999999999999 NO CYCLE'; EXEC(@create_chat_lieu);
END;
IF NOT EXISTS(SELECT 1 FROM sys.default_constraints d JOIN sys.columns c ON c.object_id=d.parent_object_id AND c.column_id=d.parent_column_id WHERE d.parent_object_id=OBJECT_ID(N'dbo.chat_lieu') AND c.name=N'ma')
 EXEC(N'ALTER TABLE dbo.chat_lieu ADD CONSTRAINT DF_chat_lieu_ma DEFAULT(CONCAT(''CL'',FORMAT(NEXT VALUE FOR dbo.chat_lieu_ma_seq,''000''))) FOR ma');
IF EXISTS(SELECT 1 FROM sys.columns WHERE object_id=OBJECT_ID(N'dbo.chat_lieu') AND name=N'ma' AND is_nullable=1) EXEC(N'ALTER TABLE dbo.chat_lieu ALTER COLUMN ma VARCHAR(20) NOT NULL');
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.chat_lieu') AND name=N'UX_chat_lieu_ma') EXEC(N'CREATE UNIQUE INDEX UX_chat_lieu_ma ON dbo.chat_lieu(ma)');
PRINT N'017: chat_lieu — ma CL, SEQUENCE không đặt lùi';
IF COL_LENGTH(N'dbo.mau_sac',N'ma') IS NULL ALTER TABLE dbo.mau_sac ADD ma VARCHAR(20) NULL;
EXEC(N';WITH missing AS (SELECT id,ROW_NUMBER() OVER(ORDER BY id) n FROM dbo.mau_sac WHERE ma IS NULL), base AS (SELECT COALESCE(MAX(TRY_CONVERT(BIGINT,SUBSTRING(ma,3,18))),0) n FROM dbo.mau_sac) UPDATE t SET ma=''MS''+FORMAT(m.n+b.n,''000'') FROM dbo.mau_sac t JOIN missing m ON m.id=t.id CROSS JOIN base b;');
IF OBJECT_ID(N'dbo.mau_sac_ma_seq',N'SO') IS NULL
BEGIN
 DECLARE @next_mau_sac BIGINT; EXEC sys.sp_executesql N'SELECT @n=COALESCE(MAX(TRY_CONVERT(BIGINT,SUBSTRING(ma,3,18))),0)+1 FROM dbo.mau_sac',N'@n BIGINT OUTPUT',@n=@next_mau_sac OUTPUT;
 DECLARE @create_mau_sac NVARCHAR(MAX)=N'CREATE SEQUENCE dbo.mau_sac_ma_seq AS BIGINT START WITH '+CONVERT(NVARCHAR(20),@next_mau_sac)+N' MAXVALUE 999999999999999999 NO CYCLE'; EXEC(@create_mau_sac);
END;
IF NOT EXISTS(SELECT 1 FROM sys.default_constraints d JOIN sys.columns c ON c.object_id=d.parent_object_id AND c.column_id=d.parent_column_id WHERE d.parent_object_id=OBJECT_ID(N'dbo.mau_sac') AND c.name=N'ma')
 EXEC(N'ALTER TABLE dbo.mau_sac ADD CONSTRAINT DF_mau_sac_ma DEFAULT(CONCAT(''MS'',FORMAT(NEXT VALUE FOR dbo.mau_sac_ma_seq,''000''))) FOR ma');
IF EXISTS(SELECT 1 FROM sys.columns WHERE object_id=OBJECT_ID(N'dbo.mau_sac') AND name=N'ma' AND is_nullable=1) EXEC(N'ALTER TABLE dbo.mau_sac ALTER COLUMN ma VARCHAR(20) NOT NULL');
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.mau_sac') AND name=N'UX_mau_sac_ma') EXEC(N'CREATE UNIQUE INDEX UX_mau_sac_ma ON dbo.mau_sac(ma)');
PRINT N'017: mau_sac — ma MS, SEQUENCE không đặt lùi';
IF COL_LENGTH(N'dbo.de_giay',N'ma') IS NULL ALTER TABLE dbo.de_giay ADD ma VARCHAR(20) NULL;
EXEC(N';WITH missing AS (SELECT id,ROW_NUMBER() OVER(ORDER BY id) n FROM dbo.de_giay WHERE ma IS NULL), base AS (SELECT COALESCE(MAX(TRY_CONVERT(BIGINT,SUBSTRING(ma,3,18))),0) n FROM dbo.de_giay) UPDATE t SET ma=''DG''+FORMAT(m.n+b.n,''000'') FROM dbo.de_giay t JOIN missing m ON m.id=t.id CROSS JOIN base b;');
IF OBJECT_ID(N'dbo.de_giay_ma_seq',N'SO') IS NULL
BEGIN
 DECLARE @next_de_giay BIGINT; EXEC sys.sp_executesql N'SELECT @n=COALESCE(MAX(TRY_CONVERT(BIGINT,SUBSTRING(ma,3,18))),0)+1 FROM dbo.de_giay',N'@n BIGINT OUTPUT',@n=@next_de_giay OUTPUT;
 DECLARE @create_de_giay NVARCHAR(MAX)=N'CREATE SEQUENCE dbo.de_giay_ma_seq AS BIGINT START WITH '+CONVERT(NVARCHAR(20),@next_de_giay)+N' MAXVALUE 999999999999999999 NO CYCLE'; EXEC(@create_de_giay);
END;
IF NOT EXISTS(SELECT 1 FROM sys.default_constraints d JOIN sys.columns c ON c.object_id=d.parent_object_id AND c.column_id=d.parent_column_id WHERE d.parent_object_id=OBJECT_ID(N'dbo.de_giay') AND c.name=N'ma')
 EXEC(N'ALTER TABLE dbo.de_giay ADD CONSTRAINT DF_de_giay_ma DEFAULT(CONCAT(''DG'',FORMAT(NEXT VALUE FOR dbo.de_giay_ma_seq,''000''))) FOR ma');
IF EXISTS(SELECT 1 FROM sys.columns WHERE object_id=OBJECT_ID(N'dbo.de_giay') AND name=N'ma' AND is_nullable=1) EXEC(N'ALTER TABLE dbo.de_giay ALTER COLUMN ma VARCHAR(20) NOT NULL');
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.de_giay') AND name=N'UX_de_giay_ma') EXEC(N'CREATE UNIQUE INDEX UX_de_giay_ma ON dbo.de_giay(ma)');
PRINT N'017: de_giay — ma DG, SEQUENCE không đặt lùi';
IF COL_LENGTH(N'dbo.product_variants',N'ma') IS NULL ALTER TABLE dbo.product_variants ADD ma VARCHAR(20) NULL;
EXEC(N';WITH missing AS (SELECT id,ROW_NUMBER() OVER(ORDER BY id) n FROM dbo.product_variants WHERE ma IS NULL), base AS (SELECT COALESCE(MAX(TRY_CONVERT(BIGINT,SUBSTRING(ma,3,18))),0) n FROM dbo.product_variants) UPDATE t SET ma=''BT''+FORMAT(m.n+b.n,''0000'') FROM dbo.product_variants t JOIN missing m ON m.id=t.id CROSS JOIN base b;');
IF OBJECT_ID(N'dbo.product_variants_ma_seq',N'SO') IS NULL
BEGIN
 DECLARE @next_product_variants BIGINT; EXEC sys.sp_executesql N'SELECT @n=COALESCE(MAX(TRY_CONVERT(BIGINT,SUBSTRING(ma,3,18))),0)+1 FROM dbo.product_variants',N'@n BIGINT OUTPUT',@n=@next_product_variants OUTPUT;
 DECLARE @create_product_variants NVARCHAR(MAX)=N'CREATE SEQUENCE dbo.product_variants_ma_seq AS BIGINT START WITH '+CONVERT(NVARCHAR(20),@next_product_variants)+N' MAXVALUE 999999999999999999 NO CYCLE'; EXEC(@create_product_variants);
END;
IF NOT EXISTS(SELECT 1 FROM sys.default_constraints d JOIN sys.columns c ON c.object_id=d.parent_object_id AND c.column_id=d.parent_column_id WHERE d.parent_object_id=OBJECT_ID(N'dbo.product_variants') AND c.name=N'ma')
 EXEC(N'ALTER TABLE dbo.product_variants ADD CONSTRAINT DF_product_variants_ma DEFAULT(CONCAT(''BT'',FORMAT(NEXT VALUE FOR dbo.product_variants_ma_seq,''0000''))) FOR ma');
IF EXISTS(SELECT 1 FROM sys.columns WHERE object_id=OBJECT_ID(N'dbo.product_variants') AND name=N'ma' AND is_nullable=1) EXEC(N'ALTER TABLE dbo.product_variants ALTER COLUMN ma VARCHAR(20) NOT NULL');
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.product_variants') AND name=N'UX_product_variants_ma') EXEC(N'CREATE UNIQUE INDEX UX_product_variants_ma ON dbo.product_variants(ma)');
PRINT N'017: product_variants — ma BT, SEQUENCE không đặt lùi';
IF COL_LENGTH(N'dbo.promotions',N'ma') IS NULL ALTER TABLE dbo.promotions ADD ma VARCHAR(20) NULL;
EXEC(N';WITH missing AS (SELECT id,ROW_NUMBER() OVER(ORDER BY id) n FROM dbo.promotions WHERE ma IS NULL), base AS (SELECT COALESCE(MAX(TRY_CONVERT(BIGINT,SUBSTRING(ma,3,18))),0) n FROM dbo.promotions) UPDATE t SET ma=''KM''+FORMAT(m.n+b.n,''000'') FROM dbo.promotions t JOIN missing m ON m.id=t.id CROSS JOIN base b;');
IF OBJECT_ID(N'dbo.promotions_ma_seq',N'SO') IS NULL
BEGIN
 DECLARE @next_promotions BIGINT; EXEC sys.sp_executesql N'SELECT @n=COALESCE(MAX(TRY_CONVERT(BIGINT,SUBSTRING(ma,3,18))),0)+1 FROM dbo.promotions',N'@n BIGINT OUTPUT',@n=@next_promotions OUTPUT;
 DECLARE @create_promotions NVARCHAR(MAX)=N'CREATE SEQUENCE dbo.promotions_ma_seq AS BIGINT START WITH '+CONVERT(NVARCHAR(20),@next_promotions)+N' MAXVALUE 999999999999999999 NO CYCLE'; EXEC(@create_promotions);
END;
IF NOT EXISTS(SELECT 1 FROM sys.default_constraints d JOIN sys.columns c ON c.object_id=d.parent_object_id AND c.column_id=d.parent_column_id WHERE d.parent_object_id=OBJECT_ID(N'dbo.promotions') AND c.name=N'ma')
 EXEC(N'ALTER TABLE dbo.promotions ADD CONSTRAINT DF_promotions_ma DEFAULT(CONCAT(''KM'',FORMAT(NEXT VALUE FOR dbo.promotions_ma_seq,''000''))) FOR ma');
IF EXISTS(SELECT 1 FROM sys.columns WHERE object_id=OBJECT_ID(N'dbo.promotions') AND name=N'ma' AND is_nullable=1) EXEC(N'ALTER TABLE dbo.promotions ALTER COLUMN ma VARCHAR(20) NOT NULL');
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.promotions') AND name=N'UX_promotions_ma') EXEC(N'CREATE UNIQUE INDEX UX_promotions_ma ON dbo.promotions(ma)');
PRINT N'017: promotions — ma KM, SEQUENCE không đặt lùi';
COMMIT;
PRINT N'HOÀN TẤT 017_khung_admin_ma_doi_tuong.sql';
END TRY
BEGIN CATCH
IF @@TRANCOUNT>0 ROLLBACK;
THROW;
END CATCH;
