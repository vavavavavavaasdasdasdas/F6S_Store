-- Giai đoạn 28. Mở rộng hai bảng chat hiện có, không xóa dữ liệu cũ.
IF DB_NAME() NOT IN (N'shopbangiay_kt',N'shop_verify',N'shop_fresh')
 AND DB_NAME() NOT LIKE N'shopbangiay[_]kt[_]%'
 AND NOT (DB_NAME()=N'shopbangiay' AND COALESCE(TRY_CONVERT(INT,SESSION_CONTEXT(N'shop.allow-production-migration')),0)=1)
 THROW 51000,N'Database không được phép chạy migration.',1;
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF OBJECT_ID(N'dbo.chat_hoi_thoai',N'U') IS NULL OR OBJECT_ID(N'dbo.chat_tin_nhan',N'U') IS NULL
 THROW 51000,N'Cần chạy migration 025 trước 027.',1;
IF OBJECT_ID(N'dbo.seq_chat_ma',N'SO') IS NULL EXEC(N'CREATE SEQUENCE dbo.seq_chat_ma AS BIGINT START WITH 1 INCREMENT BY 1 NO CYCLE');
IF COL_LENGTH(N'dbo.chat_hoi_thoai',N'session_token_hash') IS NULL EXEC(N'ALTER TABLE dbo.chat_hoi_thoai ADD session_token_hash VARCHAR(72) NULL');
IF COL_LENGTH(N'dbo.chat_hoi_thoai',N'ma') IS NULL EXEC(N'ALTER TABLE dbo.chat_hoi_thoai ADD ma VARCHAR(20) NULL');
IF COL_LENGTH(N'dbo.chat_hoi_thoai',N'customer_id') IS NULL EXEC(N'ALTER TABLE dbo.chat_hoi_thoai ADD customer_id INT NULL');
IF COL_LENGTH(N'dbo.chat_hoi_thoai',N'ho_ten') IS NULL EXEC(N'ALTER TABLE dbo.chat_hoi_thoai ADD ho_ten NVARCHAR(100) NULL');
IF COL_LENGTH(N'dbo.chat_hoi_thoai',N'lien_he') IS NULL EXEC(N'ALTER TABLE dbo.chat_hoi_thoai ADD lien_he NVARCHAR(100) NULL');
IF COL_LENGTH(N'dbo.chat_hoi_thoai',N'trang_thai') IS NULL EXEC(N'ALTER TABLE dbo.chat_hoi_thoai ADD trang_thai NVARCHAR(20) NOT NULL CONSTRAINT DF_chat_state DEFAULT N''MOI'' WITH VALUES');
IF COL_LENGTH(N'dbo.chat_hoi_thoai',N'nhan_vien_phu_trach_id') IS NULL EXEC(N'ALTER TABLE dbo.chat_hoi_thoai ADD nhan_vien_phu_trach_id INT NULL');
IF COL_LENGTH(N'dbo.chat_hoi_thoai',N'last_message_at') IS NULL EXEC(N'ALTER TABLE dbo.chat_hoi_thoai ADD last_message_at DATETIME2 NULL');
IF COL_LENGTH(N'dbo.chat_hoi_thoai',N'chua_doc_khach') IS NULL EXEC(N'ALTER TABLE dbo.chat_hoi_thoai ADD chua_doc_khach INT NOT NULL CONSTRAINT DF_chat_unread_customer DEFAULT 0 WITH VALUES');
IF COL_LENGTH(N'dbo.chat_hoi_thoai',N'chua_doc_nhan_vien') IS NULL EXEC(N'ALTER TABLE dbo.chat_hoi_thoai ADD chua_doc_nhan_vien INT NOT NULL CONSTRAINT DF_chat_unread_staff DEFAULT 0 WITH VALUES');
IF COL_LENGTH(N'dbo.chat_hoi_thoai',N'da_co_nhan_vien') IS NULL EXEC(N'ALTER TABLE dbo.chat_hoi_thoai ADD da_co_nhan_vien BIT NOT NULL CONSTRAINT DF_chat_human DEFAULT 0 WITH VALUES');
IF COL_LENGTH(N'dbo.chat_hoi_thoai',N'cho_nhan_vien') IS NULL EXEC(N'ALTER TABLE dbo.chat_hoi_thoai ADD cho_nhan_vien BIT NOT NULL CONSTRAINT DF_chat_wait DEFAULT 0 WITH VALUES');
IF COL_LENGTH(N'dbo.chat_tin_nhan',N'nguoi_gui_loai') IS NULL EXEC(N'ALTER TABLE dbo.chat_tin_nhan ADD nguoi_gui_loai NVARCHAR(20) NOT NULL CONSTRAINT DF_chat_sender DEFAULT N''HE_THONG'' WITH VALUES');
IF COL_LENGTH(N'dbo.chat_tin_nhan',N'nguoi_gui_id') IS NULL EXEC(N'ALTER TABLE dbo.chat_tin_nhan ADD nguoi_gui_id INT NULL');
IF COL_LENGTH(N'dbo.chat_tin_nhan',N'loai') IS NULL EXEC(N'ALTER TABLE dbo.chat_tin_nhan ADD loai NVARCHAR(20) NOT NULL CONSTRAINT DF_chat_type DEFAULT N''VAN_BAN'' WITH VALUES');
IF COL_LENGTH(N'dbo.chat_tin_nhan',N'tep_dinh_kem') IS NULL EXEC(N'ALTER TABLE dbo.chat_tin_nhan ADD tep_dinh_kem NVARCHAR(255) NULL');
IF COL_LENGTH(N'dbo.chat_tin_nhan',N'da_xem_luc') IS NULL EXEC(N'ALTER TABLE dbo.chat_tin_nhan ADD da_xem_luc DATETIME2 NULL');
IF COL_LENGTH(N'dbo.chat_tin_nhan',N'client_id') IS NULL EXEC(N'ALTER TABLE dbo.chat_tin_nhan ADD client_id VARCHAR(64) NULL');
IF COL_LENGTH(N'dbo.chat_tin_nhan',N'ip_address') IS NULL EXEC(N'ALTER TABLE dbo.chat_tin_nhan ADD ip_address VARCHAR(45) NULL');
EXEC(N'
DECLARE @numbered TABLE(id BIGINT, n BIGINT);
INSERT @numbered SELECT id,NEXT VALUE FOR dbo.seq_chat_ma OVER(ORDER BY id) FROM dbo.chat_hoi_thoai WHERE ma IS NULL;
UPDATE c SET ma=''CH''+CASE WHEN n<10000 THEN RIGHT(''0000''+CONVERT(VARCHAR(20),n),4) ELSE CONVERT(VARCHAR(20),n) END
FROM dbo.chat_hoi_thoai c JOIN @numbered a ON a.id=c.id;
UPDATE c SET last_message_at=COALESCE((SELECT MAX(m.created_at) FROM dbo.chat_tin_nhan m WHERE m.hoi_thoai_id=c.id),c.updated_at,c.created_at) FROM dbo.chat_hoi_thoai c WHERE last_message_at IS NULL;
;WITH ranked AS(SELECT id,khach_id,ROW_NUMBER() OVER(PARTITION BY khach_id ORDER BY updated_at DESC,id DESC) rn FROM dbo.chat_hoi_thoai WHERE khach_id IS NOT NULL)
UPDATE c SET customer_id=r.khach_id FROM dbo.chat_hoi_thoai c JOIN ranked r ON r.id=c.id AND r.rn=1 WHERE c.customer_id IS NULL AND NOT EXISTS(SELECT 1 FROM dbo.chat_hoi_thoai x WHERE x.customer_id=r.khach_id);
UPDATE c SET ho_ten=COALESCE(c.ho_ten,cu.full_name,N''Khách cũ''),lien_he=COALESCE(c.lien_he,cu.email,cu.phone) FROM dbo.chat_hoi_thoai c LEFT JOIN dbo.customers cu ON cu.id=c.customer_id WHERE c.ho_ten IS NULL;
UPDATE dbo.chat_tin_nhan SET nguoi_gui_loai=CASE WHEN sender=''USER'' THEN N''KHACH'' WHEN sender=''BOT'' THEN N''TRO_LY'' ELSE N''HE_THONG'' END WHERE nguoi_gui_loai=N''HE_THONG'' AND sender IN(''USER'',''BOT'');
UPDATE dbo.chat_tin_nhan SET da_xem_luc=created_at WHERE client_id IS NULL AND nguoi_gui_loai IN(N''KHACH'',N''TRO_LY'') AND da_xem_luc IS NULL;
ALTER TABLE dbo.chat_hoi_thoai ALTER COLUMN ma VARCHAR(20) NOT NULL;
');
IF NOT EXISTS(SELECT 1 FROM sys.default_constraints WHERE parent_object_id=OBJECT_ID(N'dbo.chat_hoi_thoai') AND parent_column_id=COLUMNPROPERTY(OBJECT_ID(N'dbo.chat_hoi_thoai'),N'ma','ColumnId'))
 EXEC(N'ALTER TABLE dbo.chat_hoi_thoai ADD CONSTRAINT DF_chat_ma DEFAULT (''CH''+FORMAT(NEXT VALUE FOR dbo.seq_chat_ma,''0000'')) FOR ma');
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.chat_hoi_thoai') AND name=N'UX_chat_ma') EXEC(N'CREATE UNIQUE INDEX UX_chat_ma ON dbo.chat_hoi_thoai(ma)');
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.chat_hoi_thoai') AND name=N'UX_chat_token') EXEC(N'CREATE UNIQUE INDEX UX_chat_token ON dbo.chat_hoi_thoai(session_token_hash) WHERE session_token_hash IS NOT NULL');
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.chat_hoi_thoai') AND name=N'UX_chat_customer') EXEC(N'CREATE UNIQUE INDEX UX_chat_customer ON dbo.chat_hoi_thoai(customer_id) WHERE customer_id IS NOT NULL');
IF NOT EXISTS(SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_chat_customer') EXEC(N'ALTER TABLE dbo.chat_hoi_thoai ADD CONSTRAINT FK_chat_customer FOREIGN KEY(customer_id) REFERENCES dbo.customers(id)');
IF NOT EXISTS(SELECT 1 FROM sys.foreign_keys WHERE name=N'FK_chat_employee') EXEC(N'ALTER TABLE dbo.chat_hoi_thoai ADD CONSTRAINT FK_chat_employee FOREIGN KEY(nhan_vien_phu_trach_id) REFERENCES dbo.employees(id)');
IF NOT EXISTS(SELECT 1 FROM sys.check_constraints WHERE name=N'CK_chat_state') EXEC(N'ALTER TABLE dbo.chat_hoi_thoai ADD CONSTRAINT CK_chat_state CHECK(trang_thai IN(N''MOI'',N''DANG_XU_LY'',N''DA_DONG'') AND chua_doc_khach>=0 AND chua_doc_nhan_vien>=0)');
IF NOT EXISTS(SELECT 1 FROM sys.check_constraints WHERE name=N'CK_chat_message_kind') EXEC(N'ALTER TABLE dbo.chat_tin_nhan ADD CONSTRAINT CK_chat_message_kind CHECK(nguoi_gui_loai IN(N''KHACH'',N''NHAN_VIEN'',N''TRO_LY'',N''HE_THONG'') AND loai IN(N''VAN_BAN'',N''ANH'',N''SAN_PHAM'',N''DON_HANG''))');
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.chat_hoi_thoai') AND name=N'IX_chat_state_last') EXEC(N'CREATE INDEX IX_chat_state_last ON dbo.chat_hoi_thoai(trang_thai,last_message_at DESC)');
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.chat_tin_nhan') AND name=N'IX_chat_thread_id') EXEC(N'CREATE INDEX IX_chat_thread_id ON dbo.chat_tin_nhan(hoi_thoai_id,id)');
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.chat_tin_nhan') AND name=N'UX_chat_client') EXEC(N'CREATE UNIQUE INDEX UX_chat_client ON dbo.chat_tin_nhan(hoi_thoai_id,nguoi_gui_loai,client_id) WHERE client_id IS NOT NULL');
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.chat_tin_nhan') AND name=N'IX_chat_ip_date') EXEC(N'CREATE INDEX IX_chat_ip_date ON dbo.chat_tin_nhan(ip_address,created_at) INCLUDE(nguoi_gui_loai)');
COMMIT;
PRINT N'HOÀN TẤT 027_chat_ho_tro_thoi_gian_thuc.sql';
