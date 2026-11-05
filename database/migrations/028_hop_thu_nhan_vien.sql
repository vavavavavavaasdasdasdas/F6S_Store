-- Giai đoạn 29: inbox nhân viên, chuyển phiếu cũ, audit và email nền.
IF DB_NAME() NOT IN (N'shopbangiay_kt',N'shop_verify',N'shop_fresh') AND DB_NAME() NOT LIKE N'shopbangiay[_]kt[_]%'
 AND NOT (DB_NAME()=N'shopbangiay' AND COALESCE(TRY_CONVERT(INT,SESSION_CONTEXT(N'shop.allow-production-migration')),0)=1)
 THROW 51000,N'Database không được phép chạy migration.',1;
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF COL_LENGTH('dbo.chat_hoi_thoai','ma') IS NULL THROW 51000,N'Cần chạy 027 trước 028.',1;
IF COL_LENGTH('dbo.chat_hoi_thoai','assigned_user_id') IS NULL EXEC(N'ALTER TABLE dbo.chat_hoi_thoai ADD assigned_user_id INT NULL CONSTRAINT FK_chat_assigned_user REFERENCES dbo.users(id) ON DELETE SET NULL');
IF COL_LENGTH('dbo.chat_hoi_thoai','legacy_ticket_id') IS NULL EXEC(N'ALTER TABLE dbo.chat_hoi_thoai ADD legacy_ticket_id INT NULL REFERENCES dbo.phieu_ho_tro(id)');
IF COL_LENGTH('dbo.chat_hoi_thoai','last_customer_seen_at') IS NULL EXEC(N'ALTER TABLE dbo.chat_hoi_thoai ADD last_customer_seen_at DATETIME2 NULL');
IF COL_LENGTH('dbo.chat_hoi_thoai','last_mail_at') IS NULL EXEC(N'ALTER TABLE dbo.chat_hoi_thoai ADD last_mail_at DATETIME2 NULL');
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.chat_hoi_thoai') AND name='UX_chat_legacy_ticket') EXEC(N'CREATE UNIQUE INDEX UX_chat_legacy_ticket ON dbo.chat_hoi_thoai(legacy_ticket_id) WHERE legacy_ticket_id IS NOT NULL');
IF OBJECT_ID('dbo.chat_action_audit','U') IS NULL CREATE TABLE dbo.chat_action_audit(id BIGINT IDENTITY PRIMARY KEY,hoi_thoai_id BIGINT NOT NULL REFERENCES dbo.chat_hoi_thoai(id) ON DELETE CASCADE,user_id INT NOT NULL,actor_name NVARCHAR(100) NOT NULL,hanh_dong NVARCHAR(30) NOT NULL,created_at DATETIME2 NOT NULL);
-- Audit chụp tên người thực hiện, không làm thay đổi luồng xóa tài khoản cũ.
IF COL_LENGTH('dbo.chat_action_audit','actor_name') IS NULL EXEC(N'ALTER TABLE dbo.chat_action_audit ADD actor_name NVARCHAR(100) NOT NULL CONSTRAINT DF_chat_audit_actor DEFAULT N''Tài khoản cũ'' WITH VALUES');
EXEC(N'UPDATE a SET actor_name=u.username FROM dbo.chat_action_audit a JOIN dbo.users u ON u.id=a.user_id WHERE a.actor_name=N''Tài khoản cũ''');
DECLARE @fk SYSNAME,@dropSql NVARCHAR(MAX);
DECLARE audit_fks CURSOR LOCAL FAST_FORWARD FOR SELECT name FROM sys.foreign_keys WHERE parent_object_id=OBJECT_ID('dbo.chat_action_audit') AND referenced_object_id=OBJECT_ID('dbo.users');
OPEN audit_fks; FETCH NEXT FROM audit_fks INTO @fk; WHILE @@FETCH_STATUS=0 BEGIN SET @dropSql=N'ALTER TABLE dbo.chat_action_audit DROP CONSTRAINT '+QUOTENAME(@fk); EXEC sys.sp_executesql @dropSql; FETCH NEXT FROM audit_fks INTO @fk; END; CLOSE audit_fks; DEALLOCATE audit_fks;
SELECT @fk=f.name FROM sys.foreign_keys f JOIN sys.foreign_key_columns c ON c.constraint_object_id=f.object_id WHERE f.parent_object_id=OBJECT_ID('dbo.chat_hoi_thoai') AND COL_NAME(c.parent_object_id,c.parent_column_id)='assigned_user_id' AND f.delete_referential_action<>2;
IF @fk IS NOT NULL AND EXISTS(SELECT 1 FROM sys.foreign_keys WHERE name=@fk AND parent_object_id=OBJECT_ID('dbo.chat_hoi_thoai')) BEGIN SET @dropSql=N'ALTER TABLE dbo.chat_hoi_thoai DROP CONSTRAINT '+QUOTENAME(@fk); EXEC sys.sp_executesql @dropSql; END;
IF NOT EXISTS(SELECT 1 FROM sys.foreign_key_columns c JOIN sys.foreign_keys f ON f.object_id=c.constraint_object_id WHERE c.parent_object_id=OBJECT_ID('dbo.chat_hoi_thoai') AND COL_NAME(c.parent_object_id,c.parent_column_id)='assigned_user_id') EXEC(N'ALTER TABLE dbo.chat_hoi_thoai ADD CONSTRAINT FK_chat_assigned_user FOREIGN KEY(assigned_user_id) REFERENCES dbo.users(id) ON DELETE SET NULL');
IF OBJECT_ID('dbo.chat_mail_log','U') IS NULL CREATE TABLE dbo.chat_mail_log(id BIGINT IDENTITY PRIMARY KEY,hoi_thoai_id BIGINT NOT NULL REFERENCES dbo.chat_hoi_thoai(id) ON DELETE CASCADE,message_id BIGINT NOT NULL UNIQUE,trang_thai NVARCHAR(20) NOT NULL,created_at DATETIME2 NOT NULL);
EXEC(N'UPDATE c SET assigned_user_id=e.user_id FROM dbo.chat_hoi_thoai c JOIN dbo.employees e ON e.id=c.nhan_vien_phu_trach_id WHERE c.assigned_user_id IS NULL');
EXEC(N'
DECLARE @ticket INT,@owner INT,@name NVARCHAR(100),@contact NVARCHAR(100),@body NVARCHAR(MAX),@reply NVARCHAR(MAX),@state NVARCHAR(50),@employee INT,@created DATETIME2,@replied DATETIME2,@chat BIGINT,@text NVARCHAR(MAX),@side NVARCHAR(20),@part INT,@length INT,@sent DATETIME2,@count INT=0;
DECLARE tickets CURSOR LOCAL FAST_FORWARD FOR SELECT t.id,t.khach_id,t.ho_ten,t.lien_he,N''[Phiếu ''+t.ma+N''] ''+t.noi_dung+CASE WHEN NULLIF(t.trich_doan_chat,N'''') IS NULL THEN N'''' ELSE NCHAR(10)+N''Trích đoạn chat:''+NCHAR(10)+t.trich_doan_chat END,t.tra_loi,t.trang_thai,t.xu_ly_boi,t.created_at,t.tra_loi_luc FROM dbo.phieu_ho_tro t WHERE NOT EXISTS(SELECT 1 FROM dbo.chat_hoi_thoai c WHERE c.legacy_ticket_id=t.id) ORDER BY t.id;
OPEN tickets; FETCH NEXT FROM tickets INTO @ticket,@owner,@name,@contact,@body,@reply,@state,@employee,@created,@replied;
WHILE @@FETCH_STATUS=0 BEGIN
 INSERT dbo.chat_hoi_thoai(session_id,khach_id,ho_ten,lien_he,trang_thai,nhan_vien_phu_trach_id,assigned_user_id,legacy_ticket_id,created_at,updated_at,last_message_at,da_co_nhan_vien,cho_nhan_vien,chua_doc_khach,chua_doc_nhan_vien)
 SELECT CONVERT(VARCHAR(36),NEWID()),@owner,@name,@contact,CASE WHEN @state IN(N''Đã xong'',N''Đã xử lý'',N''Đã đóng'') THEN N''DA_DONG'' WHEN @state=N''Đang xử lý'' THEN N''DANG_XU_LY'' ELSE N''MOI'' END,@employee,(SELECT user_id FROM dbo.employees WHERE id=@employee),@ticket,@created,COALESCE(@replied,@created),COALESCE(@replied,@created),1,1,0,0;
 SET @chat=SCOPE_IDENTITY(); SET @count+=1; SET @side=N''KHACH''; SET @text=@body; SET @sent=@created;
 WHILE 1=1 BEGIN
  SET @part=1;
  WHILE LEN(@text)>0 BEGIN
   SET @length=2000; IF UNICODE(SUBSTRING(@text,@length,1)) BETWEEN 55296 AND 56319 SET @length=1999;
   INSERT dbo.chat_tin_nhan(hoi_thoai_id,sender,nguoi_gui_loai,nguoi_gui_id,loai,noi_dung,created_at,da_xem_luc,client_id)
   VALUES(@chat,CASE WHEN @side=N''KHACH'' THEN ''USER'' ELSE ''STAFF'' END,@side,CASE WHEN @side=N''KHACH'' THEN @owner ELSE @employee END,N''VAN_BAN'',SUBSTRING(@text,1,@length),@sent,CASE WHEN @state=N''Mới'' AND @side=N''KHACH'' THEN NULL ELSE @sent END,''ticket-''+CONVERT(VARCHAR(12),@ticket)+''-''+CASE WHEN @side=N''KHACH'' THEN ''c'' ELSE ''s'' END+''-''+CONVERT(VARCHAR(12),@part));
   SET @text=SUBSTRING(@text,@length+1,2147483647); SET @part+=1;
  END;
  IF @side=N''NHAN_VIEN'' OR NULLIF(@reply,N'''') IS NULL BREAK;
  SET @side=N''NHAN_VIEN''; SET @text=@reply; SET @sent=COALESCE(@replied,@created);
 END;
 UPDATE dbo.chat_hoi_thoai SET chua_doc_nhan_vien=(SELECT COUNT(*) FROM dbo.chat_tin_nhan WHERE hoi_thoai_id=@chat AND nguoi_gui_loai=N''KHACH'' AND da_xem_luc IS NULL) WHERE id=@chat;
 FETCH NEXT FROM tickets INTO @ticket,@owner,@name,@contact,@body,@reply,@state,@employee,@created,@replied;
END;
CLOSE tickets; DEALLOCATE tickets; PRINT N''Số phiếu đã chuyển lần này: ''+CONVERT(NVARCHAR(20),@count);
SELECT legacy_ticket_id,id,ma,trang_thai FROM dbo.chat_hoi_thoai WHERE legacy_ticket_id IS NOT NULL ORDER BY legacy_ticket_id;
');
COMMIT;
PRINT N'HOÀN TẤT 028_hop_thu_nhan_vien.sql';
