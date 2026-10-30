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
