-- database/migrations/030_drop_old_promotion_product_unique_index.sql
-- Drop unique index tren (promotion_id, product_id) de cho phep nhieu bien the cua cung mot san pham tham gia dot giam gia
SET QUOTED_IDENTIFIER ON;
IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_promotion_details_product_phase8' AND object_id = OBJECT_ID('promotion_details'))
BEGIN
    DROP INDEX UX_promotion_details_product_phase8 ON promotion_details;
END;
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_promotion_details_promo_prod' AND object_id = OBJECT_ID('promotion_details'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_promotion_details_promo_prod ON promotion_details(promotion_id, product_id);
END;
GO
