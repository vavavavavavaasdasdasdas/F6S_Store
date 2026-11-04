-- Giai đoạn 24: Bán online qua nút gạt ở POS và email thông báo trạng thái
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('pos_held_orders') AND name = 'la_giao_hang')
BEGIN
    ALTER TABLE pos_held_orders ADD la_giao_hang BIT NOT NULL CONSTRAINT df_pos_held_orders_la_giao_hang DEFAULT 0;
END;

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('pos_held_orders') AND name = 'thong_tin_giao_hang')
BEGIN
    ALTER TABLE pos_held_orders ADD thong_tin_giao_hang NVARCHAR(MAX) NULL;
END;

IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'order_mail_log')
BEGIN
    CREATE TABLE order_mail_log (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        order_id INT NOT NULL CONSTRAINT fk_order_mail_log_orders REFERENCES orders(id) ON DELETE CASCADE,
        status VARCHAR(50) NOT NULL,
        email NVARCHAR(255) NULL,
        sent_at DATETIME2 NOT NULL,
        thanh_cong BIT NOT NULL,
        loi NVARCHAR(MAX) NULL,
        CONSTRAINT uq_order_mail_log_order_status UNIQUE (order_id, status)
    );
    CREATE INDEX ix_order_mail_log_order_id ON order_mail_log(order_id);
END;
