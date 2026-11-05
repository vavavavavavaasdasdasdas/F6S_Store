-- Dữ liệu mẫu giai đoạn 32, idempotent; KHÔNG nằm trong migration.
-- Chỉ dùng sau khi kiểm kê/sao lưu. Tên DB phải được truyền rõ bằng sqlcmd -v.
SET NOCOUNT ON;
SET XACT_ABORT ON;
DECLARE @TenDB_DuKien sysname=N'$(TenDB_DuKien)';
IF DB_NAME()<>@TenDB_DuKien THROW 51100,N'Sai database dự kiến.',1;
IF @@TRANCOUNT<>0 THROW 51100,N'Chạy độc lập ngoài transaction khác.',1;
-- DATETIME lưu giờ địa phương Asia/Ho_Chi_Minh (UTC+7), không phụ thuộc ngày chạy script.
DECLARE @BatDau datetime=CONVERT(datetime,'20261105',112),@HetThang11 datetime='2026-11-30T23:59:59.997',@KetThuc datetime='2026-12-31T23:59:59.997';
DECLARE @SP TABLE(stt int PRIMARY KEY,ten nvarchar(255),thuong_hieu nvarchar(100),chat_lieu nvarchar(100),de_giay nvarchar(100),mau1 nvarchar(100),mau2 nvarchar(100),gia decimal(18,2),tep nvarchar(255),mo_ta nvarchar(2000));
INSERT @SP VALUES
(1,N'Nike Air Force 1 ''07',N'Nike',N'Da thật',N'Cao su',N'Trắng',N'Đen',2929000,N'nike-air-force-1-07.jpg',N'Nike Air Force 1 ''07 có phong cách thể thao cổ điển, phù hợp phối cùng trang phục hằng ngày. Hai lựa chọn trắng và đen giúp bạn dễ chọn màu yêu thích.'),
(2,N'Adidas Ultraboost Light',N'Adidas',N'Vải lưới',N'EVA',N'Trắng',N'Đen',4500000,N'adidas-ultraboost-light.jpg',N'Adidas Ultraboost Light là lựa chọn mang phong cách giày chạy bộ. Sản phẩm có hai màu trắng và đen để bạn lựa chọn theo sở thích.'),
(3,N'Converse Chuck Taylor All Star Classic',N'Converse',N'Vải canvas',N'Cao su',N'Đen',N'Trắng',1400000,N'converse-chuck-taylor-classic.jpg',N'Converse Chuck Taylor All Star Classic mang phong cách giày vải quen thuộc. Thiết kế dễ phối cùng quần jeans hoặc trang phục thường ngày.'),
(4,N'Vans Old Skool',N'Vans',N'Vải canvas',N'Cao su',N'Đen',N'Trắng',1800000,N'vans-old-skool.jpg',N'Vans Old Skool mang phong cách đường phố đặc trưng của thương hiệu. Hai màu đen và trắng phù hợp nhiều cách phối trang phục.'),
(5,N'ASICS Gel-Kayano 14',N'ASICS',N'Vải lưới',N'EVA',N'Xám',N'Trắng',4200000,N'asics-gel-kayano-14.jpg',N'ASICS Gel-Kayano 14 có phong cách thể thao với cảm hứng giày chạy bộ. Các lựa chọn xám và trắng giúp bạn dễ phối cùng trang phục hằng ngày.');
BEGIN TRY
 BEGIN TRANSACTION;
 DECLARE @Lock int;
 EXEC @Lock=sys.sp_getapplock @Resource=N'GD32_MAINTENANCE',@LockMode='Exclusive',@LockOwner='Transaction',@LockTimeout=0;
 IF @Lock<0 THROW 51101,N'Đang có tác vụ bảo trì khác.',1;
 -- Khóa các bảng đích: không có hai tiến trình seed cùng tạo một tên.
 DECLARE @n bigint;
 SELECT @n=COUNT_BIG(*) FROM dbo.products WITH(TABLOCKX,HOLDLOCK);
 SELECT @n=COUNT_BIG(*) FROM dbo.product_variants WITH(TABLOCKX,HOLDLOCK);
 SELECT @n=COUNT_BIG(*) FROM dbo.categories WITH(TABLOCKX,HOLDLOCK);
 SELECT @n=COUNT_BIG(*) FROM dbo.chat_lieu WITH(TABLOCKX,HOLDLOCK);
 SELECT @n=COUNT_BIG(*) FROM dbo.de_giay WITH(TABLOCKX,HOLDLOCK);
 SELECT @n=COUNT_BIG(*) FROM dbo.mau_sac WITH(TABLOCKX,HOLDLOCK);
 SELECT @n=COUNT_BIG(*) FROM dbo.sizes WITH(TABLOCKX,HOLDLOCK);
 INSERT dbo.categories(name,status) SELECT DISTINCT s.thuong_hieu,1 FROM @SP s WHERE NOT EXISTS(SELECT 1 FROM dbo.categories c WHERE c.name COLLATE Latin1_General_100_CI_AI=s.thuong_hieu COLLATE Latin1_General_100_CI_AI);
 INSERT dbo.chat_lieu(ten,trang_thai) SELECT DISTINCT s.chat_lieu,1 FROM @SP s WHERE NOT EXISTS(SELECT 1 FROM dbo.chat_lieu c WHERE c.ten COLLATE Latin1_General_100_CI_AI=s.chat_lieu COLLATE Latin1_General_100_CI_AI);
 INSERT dbo.de_giay(ten,trang_thai) SELECT DISTINCT s.de_giay,1 FROM @SP s WHERE NOT EXISTS(SELECT 1 FROM dbo.de_giay c WHERE c.ten COLLATE Latin1_General_100_CI_AI=s.de_giay COLLATE Latin1_General_100_CI_AI);
 INSERT dbo.mau_sac(ten,ma_mau,trang_thai) SELECT ten,hex,1 FROM (VALUES(N'Đen','#111111'),(N'Trắng','#FFFFFF'),(N'Xám','#808080')) s(ten,hex) WHERE NOT EXISTS(SELECT 1 FROM dbo.mau_sac c WHERE c.ten COLLATE Latin1_General_100_CI_AI=s.ten COLLATE Latin1_General_100_CI_AI);
 INSERT dbo.sizes(size_value,trang_thai) SELECT ten,1 FROM (VALUES(N'39'),(N'40'),(N'41'),(N'42')) s(ten) WHERE NOT EXISTS(SELECT 1 FROM dbo.sizes c WHERE c.size_value=s.ten);
 DECLARE @i int,@Ten nvarchar(255),@TH nvarchar(100),@CL nvarchar(100),@DG nvarchar(100),@M1 nvarchar(100),@M2 nvarchar(100),@Gia decimal(18,2),@Tep nvarchar(255),@MoTa nvarchar(2000),@p int,@Code varchar(20),@seq bigint;
 DECLARE sp CURSOR LOCAL FAST_FORWARD FOR SELECT * FROM @SP ORDER BY stt;
 OPEN sp; FETCH NEXT FROM sp INTO @i,@Ten,@TH,@CL,@DG,@M1,@M2,@Gia,@Tep,@MoTa;
 WHILE @@FETCH_STATUS=0
 BEGIN
  IF (SELECT COUNT(*) FROM dbo.products WHERE name COLLATE Latin1_General_100_CI_AI=@Ten COLLATE Latin1_General_100_CI_AI)>1 THROW 51102,N'Tên sản phẩm mẫu bị trùng: cần kiểm kê lại.',1;
  SET @p=NULL; SELECT @p=id FROM dbo.products WHERE name COLLATE Latin1_General_100_CI_AI=@Ten COLLATE Latin1_General_100_CI_AI;
  IF @p IS NULL
  BEGIN
   SELECT @seq=NEXT VALUE FOR dbo.product_code_seq;
   SET @Code=CONCAT('SP',FORMAT(@seq,'000'));
   INSERT dbo.products(code,name,description,category_id,image_url,status) SELECT @Code,@Ten,@MoTa,id,CONCAT(N'/uploads/products/',@Tep),1 FROM dbo.categories WHERE name COLLATE Latin1_General_100_CI_AI=@TH COLLATE Latin1_General_100_CI_AI;
   SET @p=SCOPE_IDENTITY();
  END;
  IF NOT EXISTS(SELECT 1 FROM dbo.product_images WHERE product_id=@p)
   INSERT dbo.product_images(product_id,duong_dan,thu_tu,la_anh_chinh) VALUES(@p,CONCAT(N'/uploads/products/',@Tep),0,1);
  DECLARE @New TABLE(id int,quantity int);
  INSERT dbo.product_variants(product_id,size_id,chat_lieu_id,mau_sac_id,de_giay_id,price,quantity,status)
  OUTPUT inserted.id,inserted.quantity INTO @New
  SELECT @p,s.id,cl.id,m.id,d.id,@Gia,
   CASE WHEN @i=1 AND s.size_value=N'39' AND m.ten=@M1 THEN 3 WHEN @i=1 AND s.size_value=N'39' AND m.ten=@M2 THEN 0 ELSE 10+((@i*7+TRY_CONVERT(int,s.size_value)+CASE WHEN m.ten=@M1 THEN 0 ELSE 3 END)%16) END,1
  FROM dbo.sizes s CROSS JOIN dbo.chat_lieu cl CROSS JOIN dbo.mau_sac m CROSS JOIN dbo.de_giay d
  WHERE s.size_value IN(N'39',N'40',N'41',N'42') AND cl.ten COLLATE Latin1_General_100_CI_AI=@CL COLLATE Latin1_General_100_CI_AI AND d.ten COLLATE Latin1_General_100_CI_AI=@DG COLLATE Latin1_General_100_CI_AI AND m.ten IN(@M1,@M2)
  AND NOT EXISTS(SELECT 1 FROM dbo.product_variants v WHERE v.product_id=@p AND v.size_id=s.id AND v.chat_lieu_id=cl.id AND v.mau_sac_id=m.id AND v.de_giay_id=d.id);
  INSERT dbo.inventory_logs(variant_id,quantity_change,quantity_before,quantity_after,reason,actor,created_at)
  SELECT id,quantity,0,quantity,N'Khởi tạo',N'admin',@BatDau FROM @New;
  DELETE FROM @New;
  FETCH NEXT FROM sp INTO @i,@Ten,@TH,@CL,@DG,@M1,@M2,@Gia,@Tep,@MoTa;
 END;
 CLOSE sp; DEALLOCATE sp;
 DECLARE @KM TABLE(ten nvarchar(255),loai varchar(20),gia decimal(18,2),het datetime);
 INSERT @KM VALUES(N'Khai trương: Giảm 10%','PERCENT',10,@HetThang11),(N'Giày chạy bộ: Giảm 300.000đ','AMOUNT',300000,@KetThuc);
 INSERT dbo.promotions(name,discount_type,discount_value,start_date,end_date,status)
 SELECT k.ten,k.loai,k.gia,@BatDau,k.het,1 FROM @KM k WHERE NOT EXISTS(SELECT 1 FROM dbo.promotions p WITH(UPDLOCK,HOLDLOCK) WHERE p.name=k.ten);
 INSERT dbo.promotion_details(promotion_id,product_id)
 SELECT k.id,p.id FROM dbo.promotions k JOIN @KM m ON m.ten=k.name CROSS JOIN dbo.products p JOIN @SP s ON s.ten=p.name
 WHERE ((k.name=N'Khai trương: Giảm 10%' AND s.stt IN(3,4)) OR (k.name=N'Giày chạy bộ: Giảm 300.000đ' AND s.stt IN(2,5)))
 AND NOT EXISTS(SELECT 1 FROM dbo.promotion_details d WHERE d.promotion_id=k.id AND d.product_id=p.id);
 INSERT dbo.vouchers(code,name,discount_type,discount_value,min_order_value,max_discount,start_date,end_date,usage_limit,used_count,is_public,customer_id,status)
 SELECT v.code,v.ten,v.loai,v.gia,v.toi_thieu,v.toi_da,@BatDau,@KetThuc,NULL,0,1,NULL,1
 FROM (VALUES('CHAOMUNG10',N'Chào mừng khách mới','PERCENT',10,500000,150000),('GIAM50K',N'Giảm 50.000đ','FIXED',50000,500000,NULL),('GIAM100K',N'Giảm 100.000đ','FIXED',100000,1500000,NULL),('SALE15',N'Ưu đãi 15%','PERCENT',15,2000000,300000),('VIP20',N'Ưu đãi lớn 20%','PERCENT',20,3000000,500000)) v(code,ten,loai,gia,toi_thieu,toi_da)
 WHERE NOT EXISTS(SELECT 1 FROM dbo.vouchers x WITH(UPDLOCK,HOLDLOCK) WHERE x.code=v.code);
 COMMIT;
 SELECT p.code,p.name,c.name thuong_hieu,COUNT(v.id) so_bien_the,MIN(v.price) gia,MIN(v.quantity) ton_min,MAX(v.quantity) ton_max FROM dbo.products p JOIN @SP s ON s.ten=p.name JOIN dbo.categories c ON c.id=p.category_id JOIN dbo.product_variants v ON v.product_id=p.id GROUP BY p.code,p.name,c.name ORDER BY p.code;
 PRINT N'Đã seed. Chạy lại không ghi đè tồn kho/giá và không thêm log cho biến thể đã có.';
END TRY
BEGIN CATCH
 IF XACT_STATE()<>0 ROLLBACK;
 THROW;
END CATCH;
