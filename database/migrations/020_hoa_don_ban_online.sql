IF DB_NAME() NOT IN (N'shopbangiay_kt',N'shop_verify',N'shop_fresh')
 AND DB_NAME() NOT LIKE N'shopbangiay[_]kt[_]%'
 AND NOT (DB_NAME()=N'shopbangiay' AND COALESCE(TRY_CONVERT(INT,SESSION_CONTEXT(N'shop.allow-production-migration')),0)=1)
 THROW 51000,N'Database không được phép chạy migration.',1;
SET XACT_ABORT ON;
BEGIN TRY
BEGIN TRANSACTION;
IF EXISTS(SELECT order_id FROM invoices GROUP BY order_id HAVING COUNT(*)>1)
 THROW 51000,N'Có đơn có nhiều hóa đơn. Cần kiểm tra trước khi tiếp tục.',1;
INSERT invoices(order_id,invoice_code,issued_at,customer_name,customer_phone,employee_name)
OUTPUT inserted.id,inserted.order_id,inserted.invoice_code
SELECT o.id,'HD-BU-'+CONVERT(VARCHAR(20),o.id),o.created_at,
 COALESCE(NULLIF(o.receiver_name,N''),c.full_name,N'Khách lẻ'),COALESCE(o.phone_number,c.phone),COALESCE(e.full_name,N'Đơn trực tuyến')
FROM orders o LEFT JOIN customers c ON c.id=o.customer_id LEFT JOIN employees e ON e.id=o.employee_id
WHERE NOT EXISTS(SELECT 1 FROM invoices i WHERE i.order_id=o.id);
IF NOT EXISTS(SELECT 1 FROM sys.indexes i WHERE i.object_id=OBJECT_ID(N'invoices') AND i.is_unique=1 AND i.has_filter=0 AND (SELECT COUNT(*) FROM sys.index_columns ic WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal>0)=1 AND EXISTS(SELECT 1 FROM sys.index_columns ic JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id WHERE ic.object_id=i.object_id AND ic.index_id=i.index_id AND ic.key_ordinal=1 AND c.name='order_id'))
 CREATE UNIQUE INDEX ux_invoice_order ON invoices(order_id);
IF COL_LENGTH(N'pos_held_orders',N'order_type') IS NULL
 ALTER TABLE pos_held_orders ADD order_type VARCHAR(10) NOT NULL CONSTRAINT df_pos_hold_type DEFAULT('POS') WITH VALUES;
IF COL_LENGTH(N'pos_held_orders',N'delivery_data') IS NULL
 ALTER TABLE pos_held_orders ADD delivery_data NVARCHAR(MAX) NULL;
DECLARE @oldCheck SYSNAME,@dropCheck NVARCHAR(500);
DECLARE paymentChecks CURSOR LOCAL FAST_FORWARD FOR SELECT name FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(N'pos_held_orders') AND definition LIKE N'%payment_method%' AND name<>N'ck_pos_hold_type_payment';
OPEN paymentChecks; FETCH NEXT FROM paymentChecks INTO @oldCheck;
WHILE @@FETCH_STATUS=0
BEGIN
 SET @dropCheck=N'ALTER TABLE pos_held_orders DROP CONSTRAINT '+QUOTENAME(@oldCheck); EXEC(@dropCheck);
 FETCH NEXT FROM paymentChecks INTO @oldCheck;
END;
CLOSE paymentChecks; DEALLOCATE paymentChecks;
IF NOT EXISTS(SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(N'pos_held_orders') AND name=N'ck_pos_hold_type_payment')
 EXEC(N'ALTER TABLE pos_held_orders WITH CHECK ADD CONSTRAINT ck_pos_hold_type_payment CHECK ((order_type=''POS'' AND payment_method IN (''CASH'',''TRANSFER'')) OR (order_type=''ONLINE'' AND payment_method IN (''COD'',''BANK_TRANSFER'')))');
COMMIT;
PRINT N'HOÀN TẤT 020_hoa_don_ban_online.sql';
END TRY
BEGIN CATCH
IF @@TRANCOUNT>0 ROLLBACK;
THROW;
END CATCH;
