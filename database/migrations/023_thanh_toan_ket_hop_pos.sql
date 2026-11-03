-- 023_thanh_toan_ket_hop_pos.sql: Thêm cột tiền mặt, tiền chuyển khoản, ghi chú thanh toán cho đơn hàng và dọn hóa đơn chờ online

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.orders') AND name = 'tien_mat')
BEGIN
    ALTER TABLE dbo.orders ADD tien_mat BIGINT NOT NULL CONSTRAINT df_orders_tien_mat DEFAULT 0;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.orders') AND name = 'tien_chuyen_khoan')
BEGIN
    ALTER TABLE dbo.orders ADD tien_chuyen_khoan BIGINT NOT NULL CONSTRAINT df_orders_tien_chuyen_khoan DEFAULT 0;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.orders') AND name = 'ghi_chu_thanh_toan')
BEGIN
    ALTER TABLE dbo.orders ADD ghi_chu_thanh_toan NVARCHAR(500) NULL;
END
GO

-- Bù dữ liệu cũ:
-- CASH/COD: tien_mat = tổng, tien_chuyen_khoan = 0
-- TRANSFER/BANK_TRANSFER: tien_chuyen_khoan = tổng, tien_mat = 0
-- Đơn chưa thanh toán để 0
UPDATE dbo.orders
SET tien_mat = CASE 
        WHEN payment_status = 'PAID' AND payment_method IN ('CASH', 'COD') THEN CAST(ROUND(final_amount, 0) AS BIGINT)
        ELSE 0
    END,
    tien_chuyen_khoan = CASE 
        WHEN payment_status = 'PAID' AND payment_method IN ('TRANSFER', 'BANK_TRANSFER') THEN CAST(ROUND(final_amount, 0) AS BIGINT)
        ELSE 0
    END
WHERE tien_mat = 0 AND tien_chuyen_khoan = 0;
GO

-- Xóa hóa đơn chờ loại Online (chỉ là dữ liệu tạm, giao diện Bán online bị xóa)
DELETE FROM dbo.pos_held_orders WHERE order_type = 'ONLINE';
GO

-- Cho phép phương thức kết hợp COMBINED trên pos_held_orders
IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'ck_pos_hold_type_payment')
BEGIN
    ALTER TABLE dbo.pos_held_orders DROP CONSTRAINT ck_pos_hold_type_payment;
END
GO

ALTER TABLE dbo.pos_held_orders ADD CONSTRAINT ck_pos_hold_type_payment CHECK (
    ([order_type]='POS' AND ([payment_method]='TRANSFER' OR [payment_method]='CASH' OR [payment_method]='COMBINED'))
    OR ([order_type]='ONLINE' AND ([payment_method]='BANK_TRANSFER' OR [payment_method]='COD'))
);
GO
