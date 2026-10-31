IF DB_NAME() NOT IN (N'shopbangiay_kt',N'shop_verify',N'shop_fresh')
 AND DB_NAME() NOT LIKE N'shopbangiay[_]kt[_]%'
 AND NOT (DB_NAME()=N'shopbangiay' AND COALESCE(TRY_CONVERT(INT,SESSION_CONTEXT(N'shop.allow-production-migration')),0)=1)
 THROW 51000,N'Database không được phép chạy migration.',1;
SET XACT_ABORT ON;
BEGIN TRY
BEGIN TRANSACTION;
PRINT N'018: chuyển phiếu giảm giá cũ thành công khai; giữ cột và bộ đếm lịch sử';
UPDATE dbo.vouchers SET is_public=1
OUTPUT deleted.id,deleted.code,deleted.is_public AS truoc,inserted.is_public AS sau,deleted.customer_id
WHERE COALESCE(is_public,0)<>1;
COMMIT;
PRINT N'HOÀN TẤT 018_phieu_cong_khai.sql';
END TRY
BEGIN CATCH
IF @@TRANCOUNT>0 ROLLBACK;
THROW;
END CATCH;
