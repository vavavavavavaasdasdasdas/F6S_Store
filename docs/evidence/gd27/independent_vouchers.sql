DECLARE @subtotal DECIMAL(19,2)=10000000;
SELECT code, name, discount_type, min_order_value, max_discount,
 CASE WHEN @subtotal>=COALESCE(min_order_value,0) THEN 1 ELSE 0 END eligible,
 CASE WHEN @subtotal<COALESCE(min_order_value,0) THEN min_order_value-@subtotal ELSE 0 END missing_amount,
 CASE WHEN @subtotal<COALESCE(min_order_value,0) THEN 0
 ELSE (SELECT MIN(n) FROM (VALUES(@subtotal),(CASE WHEN discount_type='PERCENT' THEN ROUND(@subtotal*discount_value/100,0) ELSE discount_value END),
 (CASE WHEN discount_type='PERCENT' AND max_discount IS NOT NULL THEN max_discount ELSE @subtotal END)) x(n)) END discount_amount
FROM vouchers
WHERE status=1 AND start_date<=GETDATE() AND DATEADD(day,1,CAST(CAST(end_date AS DATE) AS DATETIME2))>GETDATE()
ORDER BY eligible DESC, discount_amount DESC, code;
