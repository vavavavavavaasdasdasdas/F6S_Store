-- Chỉ SELECT trên bản sao kiểm thử được phép
SELECT o.id,o.order_code,o.customer_id,o.status,o.payment_status,o.payment_method,o.total_amount,o.discount_amount,o.shipping_fee,o.final_amount,o.da_tru_kho,i.id invoice_id,i.invoice_code FROM orders o JOIN invoices i ON i.order_id=o.id WHERE o.id=1;
SELECT id,order_id,product_variant_id,price,quantity,total_price,product_name,size_name,ten_chat_lieu,ten_mau_sac,ten_de_giay FROM order_details WHERE order_id=1;
SELECT id,ma,quantity,price,status FROM product_variants WHERE id=17;
SELECT variant_id,quantity_change,quantity_before,quantity_after,reason,actor,created_at FROM inventory_logs WHERE variant_id=17 ORDER BY id;
SELECT status,actor,note,carrier,tracking_code FROM order_status_history WHERE order_id=1 ORDER BY id;
