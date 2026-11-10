-- database/migrations/029_khuyen_mai_bien_the.sql
-- Ap dung dot giam gia theo tung bien the chi tiet
SET QUOTED_IDENTIFIER ON;
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('promotion_details') AND name = 'product_variant_id')
BEGIN
    ALTER TABLE promotion_details ADD product_variant_id INT NULL CONSTRAINT FK_promotion_details_variant FOREIGN KEY REFERENCES product_variants(id);
END;
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_promotion_details_variant' AND object_id = OBJECT_ID('promotion_details'))
BEGIN
    CREATE UNIQUE NONCLUSTERED INDEX UX_promotion_details_variant ON promotion_details(promotion_id, product_variant_id) WHERE product_variant_id IS NOT NULL;
END;
GO
