-- Giai đoạn 32 BẢN THAY THẾ: DELETE hẳn; KHÔNG seed. Ngoài migration. Sao lưu trước khi vận hành.
-- sqlcmd -v TenDB_DuKien="<database đã kiểm tra>" -i database/tools/don_du_lieu_thu.sql
-- Mặc định XEM TRƯỚC. Xác nhận bằng SESSION_CONTEXT như hướng dẫn README.
SET NOCOUNT ON;
SET XACT_ABORT ON;
DECLARE @TenDB_DuKien sysname=N'$(TenDB_DuKien)';
DECLARE @XAC_NHAN nvarchar(100)=COALESCE(CONVERT(nvarchar(100),SESSION_CONTEXT(N'XAC_NHAN')),N'');
IF DB_NAME()<>@TenDB_DuKien THROW 51000,N'Sai database dự kiến. Dừng trước khi thay đổi.',1;
IF @@TRANCOUNT<>0 THROW 51000,N'Phải chạy độc lập, không nằm trong transaction khác.',1;
DECLARE @BangLoi sysname=N'Kiểm tra đầu vào';
DECLARE @DanhMucGoc TABLE(bang sysname NOT NULL,ten nvarchar(100) NOT NULL);
DECLARE @DanhMucGiuThem TABLE(bang sysname NOT NULL,ten nvarchar(100) NOT NULL);
-- Chỉ chủ dữ liệu điền các tên ngoài danh sách gốc đã duyệt GIỮ. Mặc định RỖNG.
-- INSERT @DanhMucGiuThem VALUES(N'categories',N'ASICS');
INSERT @DanhMucGoc(bang,ten) VALUES
(N'categories',N'Nike'),
(N'categories',N'Adidas'),
(N'categories',N'Converse'),
(N'categories',N'Vans'),
(N'categories',N'Puma'),
(N'categories',N'New Balance'),
(N'sizes',N'36'),
(N'sizes',N'37'),
(N'sizes',N'38'),
(N'sizes',N'39'),
(N'sizes',N'40'),
(N'sizes',N'41'),
(N'sizes',N'42'),
(N'sizes',N'43'),
(N'chat_lieu',N'Da thật'),
(N'chat_lieu',N'Da tổng hợp'),
(N'chat_lieu',N'Da lộn'),
(N'chat_lieu',N'Vải canvas'),
(N'chat_lieu',N'Vải lưới'),
(N'chat_lieu',N'Vải dệt kim'),
(N'chat_lieu',N'Cao su'),
(N'chat_lieu',N'Khác'),
(N'mau_sac',N'Đen'),
(N'mau_sac',N'Trắng'),
(N'mau_sac',N'Xám'),
(N'mau_sac',N'Đỏ'),
(N'mau_sac',N'Xanh dương'),
(N'mau_sac',N'Xanh lá'),
(N'mau_sac',N'Vàng'),
(N'mau_sac',N'Nâu'),
(N'mau_sac',N'Be'),
(N'mau_sac',N'Hồng'),
(N'mau_sac',N'Khác'),
(N'de_giay',N'Cao su'),
(N'de_giay',N'EVA'),
(N'de_giay',N'PU'),
(N'de_giay',N'TPU'),
(N'de_giay',N'Đế bằng'),
(N'de_giay',N'Đế độn'),
(N'de_giay',N'Đế chống trượt'),
(N'de_giay',N'Khác');
DECLARE @DanhMuc TABLE(bang sysname,id int,ten nvarchar(100),khoa nvarchar(100) COLLATE Latin1_General_100_CI_AS,nhom nvarchar(60));
DECLARE @Sau TABLE(bang sysname PRIMARY KEY,sau bigint);
DECLARE @ThongKe TABLE(bang sysname PRIMARY KEY,truoc bigint);
BEGIN TRY
 BEGIN TRANSACTION;
 DECLARE @Khoa int;
 EXEC @Khoa=sys.sp_getapplock @Resource=N'GD32_MAINTENANCE',@LockMode='Exclusive',@LockOwner='Transaction',@LockTimeout=0;
 IF @Khoa<0 THROW 51005,N'Đang có tác vụ bảo trì khác.',1;
 IF (SELECT COUNT(*) FROM dbo.users WITH(UPDLOCK,HOLDLOCK) WHERE LOWER(username)=N'user')<>1
  OR (SELECT COUNT(*) FROM dbo.users WHERE LOWER(username)=N'admin')<>1
  THROW 51001,N'Phải có đúng một admin và một user. Dừng và chọn tài khoản với chủ dữ liệu.',1;
 SELECT u.id,u.username,r.name vai_tro,u.status,c.id customer_id,c.customer_code,e.id employee_id,e.employee_code
 FROM dbo.users u JOIN dbo.roles r ON r.id=u.role_id LEFT JOIN dbo.customers c ON c.user_id=u.id
 LEFT JOIN dbo.employees e ON e.user_id=u.id WHERE LOWER(u.username) IN(N'admin',N'user');
 DECLARE @GiuUser TABLE(id int PRIMARY KEY);
 DECLARE @GiuKhach TABLE(id int PRIMARY KEY);
 INSERT @GiuUser SELECT id FROM dbo.users WHERE LOWER(username) IN(N'admin',N'user');
 INSERT @GiuKhach SELECT id FROM dbo.customers WHERE user_id IN(SELECT id FROM @GiuUser)
 OR full_name COLLATE Latin1_General_100_CI_AI=N'Khách lẻ';
 -- POS dùng customer=NULL cho khách lẻ; giữ thêm mọi hồ sơ có đúng nhãn hệ thống nếu có.
 IF EXISTS(SELECT 1 FROM sys.tables t JOIN sys.schemas s ON s.schema_id=t.schema_id WHERE s.name<>N'dbo' OR t.name NOT IN(N'admin_chat_audit',N'admin_chat_hoi_thoai',N'admin_chat_tin_nhan',N'categories',N'cau_hinh_phi_ship',N'chat_action_audit',N'chat_hoi_thoai',N'chat_lieu',N'chat_mail_log',N'chat_tin_nhan',N'chatbot_faq',N'customers',N'danh_gia',N'de_giay',N'dia_chi_nhan_hang',N'employees',N'inventory_logs',N'invoices',N'mau_sac',N'order_details',N'order_mail_log',N'order_status_history',N'orders',N'phieu_ho_tro',N'phuong_xa',N'pos_held_items',N'pos_held_orders',N'product_images',N'product_variants',N'products',N'promotion_details',N'promotions',N'roles',N'sizes',N'ten_tinh_thanh_cu',N'tinh_thanh',N'users',N'vouchers',N'vung_giao_hang',N'yeu_thich')) THROW 51002,N'Có bảng chưa kiểm kê. Phải duyệt lại kế hoạch, không tự xóa.',1;
 INSERT @ThongKe(bang,truoc) SELECT N'admin_chat_audit',COUNT_BIG(*) FROM dbo.[admin_chat_audit] WITH(TABLOCKX,HOLDLOCK);
 INSERT @ThongKe(bang,truoc) SELECT N'admin_chat_hoi_thoai',COUNT_BIG(*) FROM dbo.[admin_chat_hoi_thoai] WITH(TABLOCKX,HOLDLOCK);
 INSERT @ThongKe(bang,truoc) SELECT N'admin_chat_tin_nhan',COUNT_BIG(*) FROM dbo.[admin_chat_tin_nhan] WITH(TABLOCKX,HOLDLOCK);
 INSERT @ThongKe(bang,truoc) SELECT N'categories',COUNT_BIG(*) FROM dbo.[categories] WITH(TABLOCKX,HOLDLOCK);
 INSERT @ThongKe(bang,truoc) SELECT N'cau_hinh_phi_ship',COUNT_BIG(*) FROM dbo.[cau_hinh_phi_ship] WITH(TABLOCKX,HOLDLOCK);
 INSERT @ThongKe(bang,truoc) SELECT N'chat_action_audit',COUNT_BIG(*) FROM dbo.[chat_action_audit] WITH(TABLOCKX,HOLDLOCK);
 INSERT @ThongKe(bang,truoc) SELECT N'chat_hoi_thoai',COUNT_BIG(*) FROM dbo.[chat_hoi_thoai] WITH(TABLOCKX,HOLDLOCK);
 INSERT @ThongKe(bang,truoc) SELECT N'chat_lieu',COUNT_BIG(*) FROM dbo.[chat_lieu] WITH(TABLOCKX,HOLDLOCK);
 INSERT @ThongKe(bang,truoc) SELECT N'chat_mail_log',COUNT_BIG(*) FROM dbo.[chat_mail_log] WITH(TABLOCKX,HOLDLOCK);
 INSERT @ThongKe(bang,truoc) SELECT N'chat_tin_nhan',COUNT_BIG(*) FROM dbo.[chat_tin_nhan] WITH(TABLOCKX,HOLDLOCK);
 INSERT @ThongKe(bang,truoc) SELECT N'chatbot_faq',COUNT_BIG(*) FROM dbo.[chatbot_faq] WITH(TABLOCKX,HOLDLOCK);
 INSERT @ThongKe(bang,truoc) SELECT N'customers',COUNT_BIG(*) FROM dbo.[customers] WITH(TABLOCKX,HOLDLOCK);
 INSERT @ThongKe(bang,truoc) SELECT N'danh_gia',COUNT_BIG(*) FROM dbo.[danh_gia] WITH(TABLOCKX,HOLDLOCK);
 INSERT @ThongKe(bang,truoc) SELECT N'de_giay',COUNT_BIG(*) FROM dbo.[de_giay] WITH(TABLOCKX,HOLDLOCK);
 INSERT @ThongKe(bang,truoc) SELECT N'dia_chi_nhan_hang',COUNT_BIG(*) FROM dbo.[dia_chi_nhan_hang] WITH(TABLOCKX,HOLDLOCK);
 INSERT @ThongKe(bang,truoc) SELECT N'employees',COUNT_BIG(*) FROM dbo.[employees] WITH(TABLOCKX,HOLDLOCK);
 INSERT @ThongKe(bang,truoc) SELECT N'inventory_logs',COUNT_BIG(*) FROM dbo.[inventory_logs] WITH(TABLOCKX,HOLDLOCK);
 INSERT @ThongKe(bang,truoc) SELECT N'invoices',COUNT_BIG(*) FROM dbo.[invoices] WITH(TABLOCKX,HOLDLOCK);
 INSERT @ThongKe(bang,truoc) SELECT N'mau_sac',COUNT_BIG(*) FROM dbo.[mau_sac] WITH(TABLOCKX,HOLDLOCK);
 INSERT @ThongKe(bang,truoc) SELECT N'order_details',COUNT_BIG(*) FROM dbo.[order_details] WITH(TABLOCKX,HOLDLOCK);
 INSERT @ThongKe(bang,truoc) SELECT N'order_mail_log',COUNT_BIG(*) FROM dbo.[order_mail_log] WITH(TABLOCKX,HOLDLOCK);
 INSERT @ThongKe(bang,truoc) SELECT N'order_status_history',COUNT_BIG(*) FROM dbo.[order_status_history] WITH(TABLOCKX,HOLDLOCK);
 INSERT @ThongKe(bang,truoc) SELECT N'orders',COUNT_BIG(*) FROM dbo.[orders] WITH(TABLOCKX,HOLDLOCK);
 INSERT @ThongKe(bang,truoc) SELECT N'phieu_ho_tro',COUNT_BIG(*) FROM dbo.[phieu_ho_tro] WITH(TABLOCKX,HOLDLOCK);
 INSERT @ThongKe(bang,truoc) SELECT N'phuong_xa',COUNT_BIG(*) FROM dbo.[phuong_xa] WITH(TABLOCKX,HOLDLOCK);
 INSERT @ThongKe(bang,truoc) SELECT N'pos_held_items',COUNT_BIG(*) FROM dbo.[pos_held_items] WITH(TABLOCKX,HOLDLOCK);
 INSERT @ThongKe(bang,truoc) SELECT N'pos_held_orders',COUNT_BIG(*) FROM dbo.[pos_held_orders] WITH(TABLOCKX,HOLDLOCK);
 INSERT @ThongKe(bang,truoc) SELECT N'product_images',COUNT_BIG(*) FROM dbo.[product_images] WITH(TABLOCKX,HOLDLOCK);
 INSERT @ThongKe(bang,truoc) SELECT N'product_variants',COUNT_BIG(*) FROM dbo.[product_variants] WITH(TABLOCKX,HOLDLOCK);
 INSERT @ThongKe(bang,truoc) SELECT N'products',COUNT_BIG(*) FROM dbo.[products] WITH(TABLOCKX,HOLDLOCK);
 INSERT @ThongKe(bang,truoc) SELECT N'promotion_details',COUNT_BIG(*) FROM dbo.[promotion_details] WITH(TABLOCKX,HOLDLOCK);
 INSERT @ThongKe(bang,truoc) SELECT N'promotions',COUNT_BIG(*) FROM dbo.[promotions] WITH(TABLOCKX,HOLDLOCK);
 INSERT @ThongKe(bang,truoc) SELECT N'roles',COUNT_BIG(*) FROM dbo.[roles] WITH(TABLOCKX,HOLDLOCK);
 INSERT @ThongKe(bang,truoc) SELECT N'sizes',COUNT_BIG(*) FROM dbo.[sizes] WITH(TABLOCKX,HOLDLOCK);
 INSERT @ThongKe(bang,truoc) SELECT N'ten_tinh_thanh_cu',COUNT_BIG(*) FROM dbo.[ten_tinh_thanh_cu] WITH(TABLOCKX,HOLDLOCK);
 INSERT @ThongKe(bang,truoc) SELECT N'tinh_thanh',COUNT_BIG(*) FROM dbo.[tinh_thanh] WITH(TABLOCKX,HOLDLOCK);
 INSERT @ThongKe(bang,truoc) SELECT N'users',COUNT_BIG(*) FROM dbo.[users] WITH(TABLOCKX,HOLDLOCK);
 INSERT @ThongKe(bang,truoc) SELECT N'vouchers',COUNT_BIG(*) FROM dbo.[vouchers] WITH(TABLOCKX,HOLDLOCK);
 INSERT @ThongKe(bang,truoc) SELECT N'vung_giao_hang',COUNT_BIG(*) FROM dbo.[vung_giao_hang] WITH(TABLOCKX,HOLDLOCK);
 INSERT @ThongKe(bang,truoc) SELECT N'yeu_thich',COUNT_BIG(*) FROM dbo.[yeu_thich] WITH(TABLOCKX,HOLDLOCK);
 SET @BangLoi=N'Kiểm kê danh mục';
 IF EXISTS(SELECT 1 FROM @DanhMucGiuThem WHERE bang NOT IN(N'categories',N'sizes',N'chat_lieu',N'mau_sac',N'de_giay')) THROW 51006,N'Bảng giữ thêm không hợp lệ',1;
 INSERT @DanhMuc(bang,id,ten,khoa,nhom) SELECT N'categories',d.id,d.[name],UPPER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(d.name,N' ',N''),NCHAR(9),N''),NCHAR(10),N''),NCHAR(13),N''),NCHAR(160),N'')) COLLATE Latin1_General_100_CI_AS,CASE WHEN EXISTS(SELECT 1 FROM @DanhMucGoc g WHERE g.bang=N'categories' AND UPPER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(g.ten,N' ',N''),NCHAR(9),N''),NCHAR(10),N''),NCHAR(13),N''),NCHAR(160),N'')) COLLATE Latin1_General_100_CI_AS=UPPER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(d.name,N' ',N''),NCHAR(9),N''),NCHAR(10),N''),NCHAR(13),N''),NCHAR(160),N'')) COLLATE Latin1_General_100_CI_AS) OR EXISTS(SELECT 1 FROM @DanhMucGiuThem g WHERE g.bang=N'categories' AND UPPER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(g.ten,N' ',N''),NCHAR(9),N''),NCHAR(10),N''),NCHAR(13),N''),NCHAR(160),N'')) COLLATE Latin1_General_100_CI_AS=UPPER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(d.name,N' ',N''),NCHAR(9),N''),NCHAR(10),N''),NCHAR(13),N''),NCHAR(160),N'')) COLLATE Latin1_General_100_CI_AS) THEN N'GIỮ' WHEN d.[name] COLLATE Latin1_General_100_CI_AI LIKE N'%test%' OR d.[name] COLLATE Latin1_General_100_CI_AI LIKE N'%phase%' OR d.[name] COLLATE Latin1_General_100_CI_AI LIKE N'%e2e%' OR d.[name] COLLATE Latin1_General_100_CI_AI LIKE N'%kiểm thử%' OR d.[name] LIKE N'%VERIFY%' OR d.[name] LIKE N'%P12-%' OR d.[name] LIKE N'%P7-%' OR d.[name] LIKE N'%P8-%' OR UPPER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(d.name,N' ',N''),NCHAR(9),N''),NCHAR(10),N''),NCHAR(13),N''),NCHAR(160),N'')) COLLATE Latin1_General_100_CI_AS=N'POLY' THEN N'XÓA NGOÀI DANH SÁCH GỐC' ELSE N'CẦN TÔI DUYỆT' END FROM dbo.[categories] d;
 INSERT @DanhMuc(bang,id,ten,khoa,nhom) SELECT N'sizes',d.id,d.[size_value],UPPER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(d.size_value,N' ',N''),NCHAR(9),N''),NCHAR(10),N''),NCHAR(13),N''),NCHAR(160),N'')) COLLATE Latin1_General_100_CI_AS,CASE WHEN EXISTS(SELECT 1 FROM @DanhMucGoc g WHERE g.bang=N'sizes' AND UPPER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(g.ten,N' ',N''),NCHAR(9),N''),NCHAR(10),N''),NCHAR(13),N''),NCHAR(160),N'')) COLLATE Latin1_General_100_CI_AS=UPPER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(d.size_value,N' ',N''),NCHAR(9),N''),NCHAR(10),N''),NCHAR(13),N''),NCHAR(160),N'')) COLLATE Latin1_General_100_CI_AS) OR EXISTS(SELECT 1 FROM @DanhMucGiuThem g WHERE g.bang=N'sizes' AND UPPER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(g.ten,N' ',N''),NCHAR(9),N''),NCHAR(10),N''),NCHAR(13),N''),NCHAR(160),N'')) COLLATE Latin1_General_100_CI_AS=UPPER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(d.size_value,N' ',N''),NCHAR(9),N''),NCHAR(10),N''),NCHAR(13),N''),NCHAR(160),N'')) COLLATE Latin1_General_100_CI_AS) THEN N'GIỮ' WHEN d.[size_value] COLLATE Latin1_General_100_CI_AI LIKE N'%test%' OR d.[size_value] COLLATE Latin1_General_100_CI_AI LIKE N'%phase%' OR d.[size_value] COLLATE Latin1_General_100_CI_AI LIKE N'%e2e%' OR d.[size_value] COLLATE Latin1_General_100_CI_AI LIKE N'%kiểm thử%' OR d.[size_value] LIKE N'%VERIFY%' OR d.[size_value] LIKE N'%P12-%' OR d.[size_value] LIKE N'%P7-%' OR d.[size_value] LIKE N'%P8-%' OR UPPER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(d.size_value,N' ',N''),NCHAR(9),N''),NCHAR(10),N''),NCHAR(13),N''),NCHAR(160),N'')) COLLATE Latin1_General_100_CI_AS=N'POLY' THEN N'XÓA NGOÀI DANH SÁCH GỐC' ELSE N'CẦN TÔI DUYỆT' END FROM dbo.[sizes] d;
 INSERT @DanhMuc(bang,id,ten,khoa,nhom) SELECT N'chat_lieu',d.id,d.[ten],UPPER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(d.ten,N' ',N''),NCHAR(9),N''),NCHAR(10),N''),NCHAR(13),N''),NCHAR(160),N'')) COLLATE Latin1_General_100_CI_AS,CASE WHEN EXISTS(SELECT 1 FROM @DanhMucGoc g WHERE g.bang=N'chat_lieu' AND UPPER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(g.ten,N' ',N''),NCHAR(9),N''),NCHAR(10),N''),NCHAR(13),N''),NCHAR(160),N'')) COLLATE Latin1_General_100_CI_AS=UPPER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(d.ten,N' ',N''),NCHAR(9),N''),NCHAR(10),N''),NCHAR(13),N''),NCHAR(160),N'')) COLLATE Latin1_General_100_CI_AS) OR EXISTS(SELECT 1 FROM @DanhMucGiuThem g WHERE g.bang=N'chat_lieu' AND UPPER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(g.ten,N' ',N''),NCHAR(9),N''),NCHAR(10),N''),NCHAR(13),N''),NCHAR(160),N'')) COLLATE Latin1_General_100_CI_AS=UPPER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(d.ten,N' ',N''),NCHAR(9),N''),NCHAR(10),N''),NCHAR(13),N''),NCHAR(160),N'')) COLLATE Latin1_General_100_CI_AS) THEN N'GIỮ' WHEN d.[ten] COLLATE Latin1_General_100_CI_AI LIKE N'%test%' OR d.[ten] COLLATE Latin1_General_100_CI_AI LIKE N'%phase%' OR d.[ten] COLLATE Latin1_General_100_CI_AI LIKE N'%e2e%' OR d.[ten] COLLATE Latin1_General_100_CI_AI LIKE N'%kiểm thử%' OR d.[ten] LIKE N'%VERIFY%' OR d.[ten] LIKE N'%P12-%' OR d.[ten] LIKE N'%P7-%' OR d.[ten] LIKE N'%P8-%' OR UPPER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(d.ten,N' ',N''),NCHAR(9),N''),NCHAR(10),N''),NCHAR(13),N''),NCHAR(160),N'')) COLLATE Latin1_General_100_CI_AS=N'POLY' THEN N'XÓA NGOÀI DANH SÁCH GỐC' ELSE N'CẦN TÔI DUYỆT' END FROM dbo.[chat_lieu] d;
 INSERT @DanhMuc(bang,id,ten,khoa,nhom) SELECT N'mau_sac',d.id,d.[ten],UPPER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(d.ten,N' ',N''),NCHAR(9),N''),NCHAR(10),N''),NCHAR(13),N''),NCHAR(160),N'')) COLLATE Latin1_General_100_CI_AS,CASE WHEN EXISTS(SELECT 1 FROM @DanhMucGoc g WHERE g.bang=N'mau_sac' AND UPPER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(g.ten,N' ',N''),NCHAR(9),N''),NCHAR(10),N''),NCHAR(13),N''),NCHAR(160),N'')) COLLATE Latin1_General_100_CI_AS=UPPER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(d.ten,N' ',N''),NCHAR(9),N''),NCHAR(10),N''),NCHAR(13),N''),NCHAR(160),N'')) COLLATE Latin1_General_100_CI_AS) OR EXISTS(SELECT 1 FROM @DanhMucGiuThem g WHERE g.bang=N'mau_sac' AND UPPER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(g.ten,N' ',N''),NCHAR(9),N''),NCHAR(10),N''),NCHAR(13),N''),NCHAR(160),N'')) COLLATE Latin1_General_100_CI_AS=UPPER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(d.ten,N' ',N''),NCHAR(9),N''),NCHAR(10),N''),NCHAR(13),N''),NCHAR(160),N'')) COLLATE Latin1_General_100_CI_AS) THEN N'GIỮ' WHEN d.[ten] COLLATE Latin1_General_100_CI_AI LIKE N'%test%' OR d.[ten] COLLATE Latin1_General_100_CI_AI LIKE N'%phase%' OR d.[ten] COLLATE Latin1_General_100_CI_AI LIKE N'%e2e%' OR d.[ten] COLLATE Latin1_General_100_CI_AI LIKE N'%kiểm thử%' OR d.[ten] LIKE N'%VERIFY%' OR d.[ten] LIKE N'%P12-%' OR d.[ten] LIKE N'%P7-%' OR d.[ten] LIKE N'%P8-%' OR UPPER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(d.ten,N' ',N''),NCHAR(9),N''),NCHAR(10),N''),NCHAR(13),N''),NCHAR(160),N'')) COLLATE Latin1_General_100_CI_AS=N'POLY' THEN N'XÓA NGOÀI DANH SÁCH GỐC' ELSE N'CẦN TÔI DUYỆT' END FROM dbo.[mau_sac] d;
 INSERT @DanhMuc(bang,id,ten,khoa,nhom) SELECT N'de_giay',d.id,d.[ten],UPPER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(d.ten,N' ',N''),NCHAR(9),N''),NCHAR(10),N''),NCHAR(13),N''),NCHAR(160),N'')) COLLATE Latin1_General_100_CI_AS,CASE WHEN EXISTS(SELECT 1 FROM @DanhMucGoc g WHERE g.bang=N'de_giay' AND UPPER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(g.ten,N' ',N''),NCHAR(9),N''),NCHAR(10),N''),NCHAR(13),N''),NCHAR(160),N'')) COLLATE Latin1_General_100_CI_AS=UPPER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(d.ten,N' ',N''),NCHAR(9),N''),NCHAR(10),N''),NCHAR(13),N''),NCHAR(160),N'')) COLLATE Latin1_General_100_CI_AS) OR EXISTS(SELECT 1 FROM @DanhMucGiuThem g WHERE g.bang=N'de_giay' AND UPPER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(g.ten,N' ',N''),NCHAR(9),N''),NCHAR(10),N''),NCHAR(13),N''),NCHAR(160),N'')) COLLATE Latin1_General_100_CI_AS=UPPER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(d.ten,N' ',N''),NCHAR(9),N''),NCHAR(10),N''),NCHAR(13),N''),NCHAR(160),N'')) COLLATE Latin1_General_100_CI_AS) THEN N'GIỮ' WHEN d.[ten] COLLATE Latin1_General_100_CI_AI LIKE N'%test%' OR d.[ten] COLLATE Latin1_General_100_CI_AI LIKE N'%phase%' OR d.[ten] COLLATE Latin1_General_100_CI_AI LIKE N'%e2e%' OR d.[ten] COLLATE Latin1_General_100_CI_AI LIKE N'%kiểm thử%' OR d.[ten] LIKE N'%VERIFY%' OR d.[ten] LIKE N'%P12-%' OR d.[ten] LIKE N'%P7-%' OR d.[ten] LIKE N'%P8-%' OR UPPER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(d.ten,N' ',N''),NCHAR(9),N''),NCHAR(10),N''),NCHAR(13),N''),NCHAR(160),N'')) COLLATE Latin1_General_100_CI_AS=N'POLY' THEN N'XÓA NGOÀI DANH SÁCH GỐC' ELSE N'CẦN TÔI DUYỆT' END FROM dbo.[de_giay] d;
 SELECT bang,id,ten,nhom FROM @DanhMuc ORDER BY bang,id;
 IF EXISTS(SELECT 1 FROM @DanhMuc WHERE nhom=N'CẦN TÔI DUYỆT') THROW 51007,N'Có danh mục ngoài gốc có thể là dữ liệu thật. Dừng; chủ dữ liệu phải điền @DanhMucGiuThem trước khi chạy.',1;
 SET @BangLoi=N'admin_chat_audit'; DELETE FROM dbo.[admin_chat_audit];
 SET @BangLoi=N'admin_chat_tin_nhan'; DELETE FROM dbo.[admin_chat_tin_nhan];
 SET @BangLoi=N'chat_action_audit'; DELETE FROM dbo.[chat_action_audit];
 SET @BangLoi=N'chat_mail_log'; DELETE FROM dbo.[chat_mail_log];
 SET @BangLoi=N'chat_tin_nhan'; DELETE FROM dbo.[chat_tin_nhan];
 SET @BangLoi=N'danh_gia'; DELETE FROM dbo.[danh_gia];
 SET @BangLoi=N'dia_chi_nhan_hang'; DELETE FROM dbo.[dia_chi_nhan_hang] WHERE customer_id NOT IN(SELECT id FROM @GiuKhach);
 SET @BangLoi=N'inventory_logs'; DELETE FROM dbo.[inventory_logs];
 SET @BangLoi=N'invoices'; DELETE FROM dbo.[invoices];
 SET @BangLoi=N'order_details'; DELETE FROM dbo.[order_details];
 SET @BangLoi=N'order_mail_log'; DELETE FROM dbo.[order_mail_log];
 SET @BangLoi=N'order_status_history'; DELETE FROM dbo.[order_status_history];
 SET @BangLoi=N'pos_held_items'; DELETE FROM dbo.[pos_held_items];
 SET @BangLoi=N'product_images'; DELETE FROM dbo.[product_images];
 SET @BangLoi=N'promotion_details'; DELETE FROM dbo.[promotion_details];
 SET @BangLoi=N'yeu_thich'; DELETE FROM dbo.[yeu_thich];
 SET @BangLoi=N'admin_chat_hoi_thoai'; DELETE FROM dbo.[admin_chat_hoi_thoai];
 SET @BangLoi=N'chat_hoi_thoai'; DELETE FROM dbo.[chat_hoi_thoai];
 SET @BangLoi=N'orders'; DELETE FROM dbo.[orders];
 SET @BangLoi=N'pos_held_orders'; DELETE FROM dbo.[pos_held_orders];
 SET @BangLoi=N'product_variants'; DELETE FROM dbo.[product_variants];
 SET @BangLoi=N'promotions'; DELETE FROM dbo.[promotions];
 SET @BangLoi=N'phieu_ho_tro'; DELETE FROM dbo.[phieu_ho_tro];
 SET @BangLoi=N'products'; DELETE FROM dbo.[products];
 SET @BangLoi=N'vouchers'; DELETE FROM dbo.[vouchers];
 SET @BangLoi=N'customers'; DELETE FROM dbo.[customers] WHERE id NOT IN(SELECT id FROM @GiuKhach);
 SET @BangLoi=N'employees'; DELETE FROM dbo.[employees] WHERE user_id NOT IN(SELECT id FROM @GiuUser);
 SET @BangLoi=N'users'; DELETE FROM dbo.[users] WHERE id NOT IN(SELECT id FROM @GiuUser);
 SET @BangLoi=N'categories'; DELETE d FROM dbo.[categories] d JOIN @DanhMuc a ON a.bang=N'categories' AND a.id=d.id WHERE a.nhom=N'XÓA NGOÀI DANH SÁCH GỐC';
 SET @BangLoi=N'sizes'; DELETE d FROM dbo.[sizes] d JOIN @DanhMuc a ON a.bang=N'sizes' AND a.id=d.id WHERE a.nhom=N'XÓA NGOÀI DANH SÁCH GỐC';
 SET @BangLoi=N'chat_lieu'; DELETE d FROM dbo.[chat_lieu] d JOIN @DanhMuc a ON a.bang=N'chat_lieu' AND a.id=d.id WHERE a.nhom=N'XÓA NGOÀI DANH SÁCH GỐC';
 SET @BangLoi=N'mau_sac'; DELETE d FROM dbo.[mau_sac] d JOIN @DanhMuc a ON a.bang=N'mau_sac' AND a.id=d.id WHERE a.nhom=N'XÓA NGOÀI DANH SÁCH GỐC';
 SET @BangLoi=N'de_giay'; DELETE d FROM dbo.[de_giay] d JOIN @DanhMuc a ON a.bang=N'de_giay' AND a.id=d.id WHERE a.nhom=N'XÓA NGOÀI DANH SÁCH GỐC';
 INSERT @Sau(bang,sau) SELECT N'admin_chat_audit',COUNT_BIG(*) FROM dbo.[admin_chat_audit];
 INSERT @Sau(bang,sau) SELECT N'admin_chat_hoi_thoai',COUNT_BIG(*) FROM dbo.[admin_chat_hoi_thoai];
 INSERT @Sau(bang,sau) SELECT N'admin_chat_tin_nhan',COUNT_BIG(*) FROM dbo.[admin_chat_tin_nhan];
 INSERT @Sau(bang,sau) SELECT N'categories',COUNT_BIG(*) FROM dbo.[categories];
 INSERT @Sau(bang,sau) SELECT N'cau_hinh_phi_ship',COUNT_BIG(*) FROM dbo.[cau_hinh_phi_ship];
 INSERT @Sau(bang,sau) SELECT N'chat_action_audit',COUNT_BIG(*) FROM dbo.[chat_action_audit];
 INSERT @Sau(bang,sau) SELECT N'chat_hoi_thoai',COUNT_BIG(*) FROM dbo.[chat_hoi_thoai];
 INSERT @Sau(bang,sau) SELECT N'chat_lieu',COUNT_BIG(*) FROM dbo.[chat_lieu];
 INSERT @Sau(bang,sau) SELECT N'chat_mail_log',COUNT_BIG(*) FROM dbo.[chat_mail_log];
 INSERT @Sau(bang,sau) SELECT N'chat_tin_nhan',COUNT_BIG(*) FROM dbo.[chat_tin_nhan];
 INSERT @Sau(bang,sau) SELECT N'chatbot_faq',COUNT_BIG(*) FROM dbo.[chatbot_faq];
 INSERT @Sau(bang,sau) SELECT N'customers',COUNT_BIG(*) FROM dbo.[customers];
 INSERT @Sau(bang,sau) SELECT N'danh_gia',COUNT_BIG(*) FROM dbo.[danh_gia];
 INSERT @Sau(bang,sau) SELECT N'de_giay',COUNT_BIG(*) FROM dbo.[de_giay];
 INSERT @Sau(bang,sau) SELECT N'dia_chi_nhan_hang',COUNT_BIG(*) FROM dbo.[dia_chi_nhan_hang];
 INSERT @Sau(bang,sau) SELECT N'employees',COUNT_BIG(*) FROM dbo.[employees];
 INSERT @Sau(bang,sau) SELECT N'inventory_logs',COUNT_BIG(*) FROM dbo.[inventory_logs];
 INSERT @Sau(bang,sau) SELECT N'invoices',COUNT_BIG(*) FROM dbo.[invoices];
 INSERT @Sau(bang,sau) SELECT N'mau_sac',COUNT_BIG(*) FROM dbo.[mau_sac];
 INSERT @Sau(bang,sau) SELECT N'order_details',COUNT_BIG(*) FROM dbo.[order_details];
 INSERT @Sau(bang,sau) SELECT N'order_mail_log',COUNT_BIG(*) FROM dbo.[order_mail_log];
 INSERT @Sau(bang,sau) SELECT N'order_status_history',COUNT_BIG(*) FROM dbo.[order_status_history];
 INSERT @Sau(bang,sau) SELECT N'orders',COUNT_BIG(*) FROM dbo.[orders];
 INSERT @Sau(bang,sau) SELECT N'phieu_ho_tro',COUNT_BIG(*) FROM dbo.[phieu_ho_tro];
 INSERT @Sau(bang,sau) SELECT N'phuong_xa',COUNT_BIG(*) FROM dbo.[phuong_xa];
 INSERT @Sau(bang,sau) SELECT N'pos_held_items',COUNT_BIG(*) FROM dbo.[pos_held_items];
 INSERT @Sau(bang,sau) SELECT N'pos_held_orders',COUNT_BIG(*) FROM dbo.[pos_held_orders];
 INSERT @Sau(bang,sau) SELECT N'product_images',COUNT_BIG(*) FROM dbo.[product_images];
 INSERT @Sau(bang,sau) SELECT N'product_variants',COUNT_BIG(*) FROM dbo.[product_variants];
 INSERT @Sau(bang,sau) SELECT N'products',COUNT_BIG(*) FROM dbo.[products];
 INSERT @Sau(bang,sau) SELECT N'promotion_details',COUNT_BIG(*) FROM dbo.[promotion_details];
 INSERT @Sau(bang,sau) SELECT N'promotions',COUNT_BIG(*) FROM dbo.[promotions];
 INSERT @Sau(bang,sau) SELECT N'roles',COUNT_BIG(*) FROM dbo.[roles];
 INSERT @Sau(bang,sau) SELECT N'sizes',COUNT_BIG(*) FROM dbo.[sizes];
 INSERT @Sau(bang,sau) SELECT N'ten_tinh_thanh_cu',COUNT_BIG(*) FROM dbo.[ten_tinh_thanh_cu];
 INSERT @Sau(bang,sau) SELECT N'tinh_thanh',COUNT_BIG(*) FROM dbo.[tinh_thanh];
 INSERT @Sau(bang,sau) SELECT N'users',COUNT_BIG(*) FROM dbo.[users];
 INSERT @Sau(bang,sau) SELECT N'vouchers',COUNT_BIG(*) FROM dbo.[vouchers];
 INSERT @Sau(bang,sau) SELECT N'vung_giao_hang',COUNT_BIG(*) FROM dbo.[vung_giao_hang];
 INSERT @Sau(bang,sau) SELECT N'yeu_thich',COUNT_BIG(*) FROM dbo.[yeu_thich];
 SELECT t.bang,t.truoc,s.sau,t.truoc-s.sau da_xoa_trong_transaction FROM @ThongKe t JOIN @Sau s ON s.bang=t.bang ORDER BY t.bang;
 IF @XAC_NHAN=N'XOA-DU-LIEU-THU'
 BEGIN
  SET @BangLoi=N'admin_chat_audit'; IF NOT EXISTS(SELECT 1 FROM dbo.[admin_chat_audit])
  BEGIN
   -- Bảng chưa từng có dòng: lần INSERT đầu dùng thẳng seed; bảng đã DELETE thì cộng increment.
   IF EXISTS(SELECT 1 FROM sys.identity_columns WHERE object_id=OBJECT_ID(N'dbo.admin_chat_audit') AND last_value IS NULL)
    DBCC CHECKIDENT('dbo.admin_chat_audit', RESEED, 1) WITH NO_INFOMSGS;
   ELSE DBCC CHECKIDENT('dbo.admin_chat_audit', RESEED, 0) WITH NO_INFOMSGS;
  END;
  SET @BangLoi=N'admin_chat_tin_nhan'; IF NOT EXISTS(SELECT 1 FROM dbo.[admin_chat_tin_nhan])
  BEGIN
   -- Bảng chưa từng có dòng: lần INSERT đầu dùng thẳng seed; bảng đã DELETE thì cộng increment.
   IF EXISTS(SELECT 1 FROM sys.identity_columns WHERE object_id=OBJECT_ID(N'dbo.admin_chat_tin_nhan') AND last_value IS NULL)
    DBCC CHECKIDENT('dbo.admin_chat_tin_nhan', RESEED, 1) WITH NO_INFOMSGS;
   ELSE DBCC CHECKIDENT('dbo.admin_chat_tin_nhan', RESEED, 0) WITH NO_INFOMSGS;
  END;
  SET @BangLoi=N'chat_action_audit'; IF NOT EXISTS(SELECT 1 FROM dbo.[chat_action_audit])
  BEGIN
   -- Bảng chưa từng có dòng: lần INSERT đầu dùng thẳng seed; bảng đã DELETE thì cộng increment.
   IF EXISTS(SELECT 1 FROM sys.identity_columns WHERE object_id=OBJECT_ID(N'dbo.chat_action_audit') AND last_value IS NULL)
    DBCC CHECKIDENT('dbo.chat_action_audit', RESEED, 1) WITH NO_INFOMSGS;
   ELSE DBCC CHECKIDENT('dbo.chat_action_audit', RESEED, 0) WITH NO_INFOMSGS;
  END;
  SET @BangLoi=N'chat_mail_log'; IF NOT EXISTS(SELECT 1 FROM dbo.[chat_mail_log])
  BEGIN
   -- Bảng chưa từng có dòng: lần INSERT đầu dùng thẳng seed; bảng đã DELETE thì cộng increment.
   IF EXISTS(SELECT 1 FROM sys.identity_columns WHERE object_id=OBJECT_ID(N'dbo.chat_mail_log') AND last_value IS NULL)
    DBCC CHECKIDENT('dbo.chat_mail_log', RESEED, 1) WITH NO_INFOMSGS;
   ELSE DBCC CHECKIDENT('dbo.chat_mail_log', RESEED, 0) WITH NO_INFOMSGS;
  END;
  SET @BangLoi=N'chat_tin_nhan'; IF NOT EXISTS(SELECT 1 FROM dbo.[chat_tin_nhan])
  BEGIN
   -- Bảng chưa từng có dòng: lần INSERT đầu dùng thẳng seed; bảng đã DELETE thì cộng increment.
   IF EXISTS(SELECT 1 FROM sys.identity_columns WHERE object_id=OBJECT_ID(N'dbo.chat_tin_nhan') AND last_value IS NULL)
    DBCC CHECKIDENT('dbo.chat_tin_nhan', RESEED, 1) WITH NO_INFOMSGS;
   ELSE DBCC CHECKIDENT('dbo.chat_tin_nhan', RESEED, 0) WITH NO_INFOMSGS;
  END;
  SET @BangLoi=N'danh_gia'; IF NOT EXISTS(SELECT 1 FROM dbo.[danh_gia])
  BEGIN
   -- Bảng chưa từng có dòng: lần INSERT đầu dùng thẳng seed; bảng đã DELETE thì cộng increment.
   IF EXISTS(SELECT 1 FROM sys.identity_columns WHERE object_id=OBJECT_ID(N'dbo.danh_gia') AND last_value IS NULL)
    DBCC CHECKIDENT('dbo.danh_gia', RESEED, 1) WITH NO_INFOMSGS;
   ELSE DBCC CHECKIDENT('dbo.danh_gia', RESEED, 0) WITH NO_INFOMSGS;
  END;
  SET @BangLoi=N'inventory_logs'; IF NOT EXISTS(SELECT 1 FROM dbo.[inventory_logs])
  BEGIN
   -- Bảng chưa từng có dòng: lần INSERT đầu dùng thẳng seed; bảng đã DELETE thì cộng increment.
   IF EXISTS(SELECT 1 FROM sys.identity_columns WHERE object_id=OBJECT_ID(N'dbo.inventory_logs') AND last_value IS NULL)
    DBCC CHECKIDENT('dbo.inventory_logs', RESEED, 1) WITH NO_INFOMSGS;
   ELSE DBCC CHECKIDENT('dbo.inventory_logs', RESEED, 0) WITH NO_INFOMSGS;
  END;
  SET @BangLoi=N'invoices'; IF NOT EXISTS(SELECT 1 FROM dbo.[invoices])
  BEGIN
   -- Bảng chưa từng có dòng: lần INSERT đầu dùng thẳng seed; bảng đã DELETE thì cộng increment.
   IF EXISTS(SELECT 1 FROM sys.identity_columns WHERE object_id=OBJECT_ID(N'dbo.invoices') AND last_value IS NULL)
    DBCC CHECKIDENT('dbo.invoices', RESEED, 1) WITH NO_INFOMSGS;
   ELSE DBCC CHECKIDENT('dbo.invoices', RESEED, 0) WITH NO_INFOMSGS;
  END;
  SET @BangLoi=N'order_details'; IF NOT EXISTS(SELECT 1 FROM dbo.[order_details])
  BEGIN
   -- Bảng chưa từng có dòng: lần INSERT đầu dùng thẳng seed; bảng đã DELETE thì cộng increment.
   IF EXISTS(SELECT 1 FROM sys.identity_columns WHERE object_id=OBJECT_ID(N'dbo.order_details') AND last_value IS NULL)
    DBCC CHECKIDENT('dbo.order_details', RESEED, 1) WITH NO_INFOMSGS;
   ELSE DBCC CHECKIDENT('dbo.order_details', RESEED, 0) WITH NO_INFOMSGS;
  END;
  SET @BangLoi=N'order_mail_log'; IF NOT EXISTS(SELECT 1 FROM dbo.[order_mail_log])
  BEGIN
   -- Bảng chưa từng có dòng: lần INSERT đầu dùng thẳng seed; bảng đã DELETE thì cộng increment.
   IF EXISTS(SELECT 1 FROM sys.identity_columns WHERE object_id=OBJECT_ID(N'dbo.order_mail_log') AND last_value IS NULL)
    DBCC CHECKIDENT('dbo.order_mail_log', RESEED, 1) WITH NO_INFOMSGS;
   ELSE DBCC CHECKIDENT('dbo.order_mail_log', RESEED, 0) WITH NO_INFOMSGS;
  END;
  SET @BangLoi=N'order_status_history'; IF NOT EXISTS(SELECT 1 FROM dbo.[order_status_history])
  BEGIN
   -- Bảng chưa từng có dòng: lần INSERT đầu dùng thẳng seed; bảng đã DELETE thì cộng increment.
   IF EXISTS(SELECT 1 FROM sys.identity_columns WHERE object_id=OBJECT_ID(N'dbo.order_status_history') AND last_value IS NULL)
    DBCC CHECKIDENT('dbo.order_status_history', RESEED, 1) WITH NO_INFOMSGS;
   ELSE DBCC CHECKIDENT('dbo.order_status_history', RESEED, 0) WITH NO_INFOMSGS;
  END;
  SET @BangLoi=N'pos_held_items'; IF NOT EXISTS(SELECT 1 FROM dbo.[pos_held_items])
  BEGIN
   -- Bảng chưa từng có dòng: lần INSERT đầu dùng thẳng seed; bảng đã DELETE thì cộng increment.
   IF EXISTS(SELECT 1 FROM sys.identity_columns WHERE object_id=OBJECT_ID(N'dbo.pos_held_items') AND last_value IS NULL)
    DBCC CHECKIDENT('dbo.pos_held_items', RESEED, 1) WITH NO_INFOMSGS;
   ELSE DBCC CHECKIDENT('dbo.pos_held_items', RESEED, 0) WITH NO_INFOMSGS;
  END;
  SET @BangLoi=N'product_images'; IF NOT EXISTS(SELECT 1 FROM dbo.[product_images])
  BEGIN
   -- Bảng chưa từng có dòng: lần INSERT đầu dùng thẳng seed; bảng đã DELETE thì cộng increment.
   IF EXISTS(SELECT 1 FROM sys.identity_columns WHERE object_id=OBJECT_ID(N'dbo.product_images') AND last_value IS NULL)
    DBCC CHECKIDENT('dbo.product_images', RESEED, 1) WITH NO_INFOMSGS;
   ELSE DBCC CHECKIDENT('dbo.product_images', RESEED, 0) WITH NO_INFOMSGS;
  END;
  SET @BangLoi=N'promotion_details'; IF NOT EXISTS(SELECT 1 FROM dbo.[promotion_details])
  BEGIN
   -- Bảng chưa từng có dòng: lần INSERT đầu dùng thẳng seed; bảng đã DELETE thì cộng increment.
   IF EXISTS(SELECT 1 FROM sys.identity_columns WHERE object_id=OBJECT_ID(N'dbo.promotion_details') AND last_value IS NULL)
    DBCC CHECKIDENT('dbo.promotion_details', RESEED, 1) WITH NO_INFOMSGS;
   ELSE DBCC CHECKIDENT('dbo.promotion_details', RESEED, 0) WITH NO_INFOMSGS;
  END;
  SET @BangLoi=N'admin_chat_hoi_thoai'; IF NOT EXISTS(SELECT 1 FROM dbo.[admin_chat_hoi_thoai])
  BEGIN
   -- Bảng chưa từng có dòng: lần INSERT đầu dùng thẳng seed; bảng đã DELETE thì cộng increment.
   IF EXISTS(SELECT 1 FROM sys.identity_columns WHERE object_id=OBJECT_ID(N'dbo.admin_chat_hoi_thoai') AND last_value IS NULL)
    DBCC CHECKIDENT('dbo.admin_chat_hoi_thoai', RESEED, 1) WITH NO_INFOMSGS;
   ELSE DBCC CHECKIDENT('dbo.admin_chat_hoi_thoai', RESEED, 0) WITH NO_INFOMSGS;
  END;
  SET @BangLoi=N'chat_hoi_thoai'; IF NOT EXISTS(SELECT 1 FROM dbo.[chat_hoi_thoai])
  BEGIN
   -- Bảng chưa từng có dòng: lần INSERT đầu dùng thẳng seed; bảng đã DELETE thì cộng increment.
   IF EXISTS(SELECT 1 FROM sys.identity_columns WHERE object_id=OBJECT_ID(N'dbo.chat_hoi_thoai') AND last_value IS NULL)
    DBCC CHECKIDENT('dbo.chat_hoi_thoai', RESEED, 1) WITH NO_INFOMSGS;
   ELSE DBCC CHECKIDENT('dbo.chat_hoi_thoai', RESEED, 0) WITH NO_INFOMSGS;
  END;
  SET @BangLoi=N'orders'; IF NOT EXISTS(SELECT 1 FROM dbo.[orders])
  BEGIN
   -- Bảng chưa từng có dòng: lần INSERT đầu dùng thẳng seed; bảng đã DELETE thì cộng increment.
   IF EXISTS(SELECT 1 FROM sys.identity_columns WHERE object_id=OBJECT_ID(N'dbo.orders') AND last_value IS NULL)
    DBCC CHECKIDENT('dbo.orders', RESEED, 1) WITH NO_INFOMSGS;
   ELSE DBCC CHECKIDENT('dbo.orders', RESEED, 0) WITH NO_INFOMSGS;
  END;
  SET @BangLoi=N'pos_held_orders'; IF NOT EXISTS(SELECT 1 FROM dbo.[pos_held_orders])
  BEGIN
   -- Bảng chưa từng có dòng: lần INSERT đầu dùng thẳng seed; bảng đã DELETE thì cộng increment.
   IF EXISTS(SELECT 1 FROM sys.identity_columns WHERE object_id=OBJECT_ID(N'dbo.pos_held_orders') AND last_value IS NULL)
    DBCC CHECKIDENT('dbo.pos_held_orders', RESEED, 1) WITH NO_INFOMSGS;
   ELSE DBCC CHECKIDENT('dbo.pos_held_orders', RESEED, 0) WITH NO_INFOMSGS;
  END;
  SET @BangLoi=N'product_variants'; IF NOT EXISTS(SELECT 1 FROM dbo.[product_variants])
  BEGIN
   -- Bảng chưa từng có dòng: lần INSERT đầu dùng thẳng seed; bảng đã DELETE thì cộng increment.
   IF EXISTS(SELECT 1 FROM sys.identity_columns WHERE object_id=OBJECT_ID(N'dbo.product_variants') AND last_value IS NULL)
    DBCC CHECKIDENT('dbo.product_variants', RESEED, 1) WITH NO_INFOMSGS;
   ELSE DBCC CHECKIDENT('dbo.product_variants', RESEED, 0) WITH NO_INFOMSGS;
  END;
  SET @BangLoi=N'promotions'; IF NOT EXISTS(SELECT 1 FROM dbo.[promotions])
  BEGIN
   -- Bảng chưa từng có dòng: lần INSERT đầu dùng thẳng seed; bảng đã DELETE thì cộng increment.
   IF EXISTS(SELECT 1 FROM sys.identity_columns WHERE object_id=OBJECT_ID(N'dbo.promotions') AND last_value IS NULL)
    DBCC CHECKIDENT('dbo.promotions', RESEED, 1) WITH NO_INFOMSGS;
   ELSE DBCC CHECKIDENT('dbo.promotions', RESEED, 0) WITH NO_INFOMSGS;
  END;
  SET @BangLoi=N'phieu_ho_tro'; IF NOT EXISTS(SELECT 1 FROM dbo.[phieu_ho_tro])
  BEGIN
   -- Bảng chưa từng có dòng: lần INSERT đầu dùng thẳng seed; bảng đã DELETE thì cộng increment.
   IF EXISTS(SELECT 1 FROM sys.identity_columns WHERE object_id=OBJECT_ID(N'dbo.phieu_ho_tro') AND last_value IS NULL)
    DBCC CHECKIDENT('dbo.phieu_ho_tro', RESEED, 1) WITH NO_INFOMSGS;
   ELSE DBCC CHECKIDENT('dbo.phieu_ho_tro', RESEED, 0) WITH NO_INFOMSGS;
  END;
  SET @BangLoi=N'products'; IF NOT EXISTS(SELECT 1 FROM dbo.[products])
  BEGIN
   -- Bảng chưa từng có dòng: lần INSERT đầu dùng thẳng seed; bảng đã DELETE thì cộng increment.
   IF EXISTS(SELECT 1 FROM sys.identity_columns WHERE object_id=OBJECT_ID(N'dbo.products') AND last_value IS NULL)
    DBCC CHECKIDENT('dbo.products', RESEED, 1) WITH NO_INFOMSGS;
   ELSE DBCC CHECKIDENT('dbo.products', RESEED, 0) WITH NO_INFOMSGS;
  END;
  SET @BangLoi=N'vouchers'; IF NOT EXISTS(SELECT 1 FROM dbo.[vouchers])
  BEGIN
   -- Bảng chưa từng có dòng: lần INSERT đầu dùng thẳng seed; bảng đã DELETE thì cộng increment.
   IF EXISTS(SELECT 1 FROM sys.identity_columns WHERE object_id=OBJECT_ID(N'dbo.vouchers') AND last_value IS NULL)
    DBCC CHECKIDENT('dbo.vouchers', RESEED, 1) WITH NO_INFOMSGS;
   ELSE DBCC CHECKIDENT('dbo.vouchers', RESEED, 0) WITH NO_INFOMSGS;
  END;
  SET @BangLoi=N'users'; IF NOT EXISTS(SELECT 1 FROM dbo.[users])
  BEGIN
   IF EXISTS(SELECT 1 FROM sys.identity_columns WHERE object_id=OBJECT_ID(N'dbo.users') AND last_value IS NULL)
    DBCC CHECKIDENT('dbo.users', RESEED, 1) WITH NO_INFOMSGS;
   ELSE DBCC CHECKIDENT('dbo.users', RESEED, 0) WITH NO_INFOMSGS;
  END;
  SET @BangLoi=N'customers'; IF NOT EXISTS(SELECT 1 FROM dbo.[customers])
  BEGIN
   IF EXISTS(SELECT 1 FROM sys.identity_columns WHERE object_id=OBJECT_ID(N'dbo.customers') AND last_value IS NULL)
    DBCC CHECKIDENT('dbo.customers', RESEED, 1) WITH NO_INFOMSGS;
   ELSE DBCC CHECKIDENT('dbo.customers', RESEED, 0) WITH NO_INFOMSGS;
  END;
  SET @BangLoi=N'employees'; IF NOT EXISTS(SELECT 1 FROM dbo.[employees])
  BEGIN
   IF EXISTS(SELECT 1 FROM sys.identity_columns WHERE object_id=OBJECT_ID(N'dbo.employees') AND last_value IS NULL)
    DBCC CHECKIDENT('dbo.employees', RESEED, 1) WITH NO_INFOMSGS;
   ELSE DBCC CHECKIDENT('dbo.employees', RESEED, 0) WITH NO_INFOMSGS;
  END;
  SET @BangLoi=N'dia_chi_nhan_hang'; IF NOT EXISTS(SELECT 1 FROM dbo.[dia_chi_nhan_hang])
  BEGIN
   IF EXISTS(SELECT 1 FROM sys.identity_columns WHERE object_id=OBJECT_ID(N'dbo.dia_chi_nhan_hang') AND last_value IS NULL)
    DBCC CHECKIDENT('dbo.dia_chi_nhan_hang', RESEED, 1) WITH NO_INFOMSGS;
   ELSE DBCC CHECKIDENT('dbo.dia_chi_nhan_hang', RESEED, 0) WITH NO_INFOMSGS;
  END;
  SET @BangLoi=N'categories'; IF NOT EXISTS(SELECT 1 FROM dbo.[categories])
  BEGIN
   IF EXISTS(SELECT 1 FROM sys.identity_columns WHERE object_id=OBJECT_ID(N'dbo.categories') AND last_value IS NULL)
    DBCC CHECKIDENT('dbo.categories', RESEED, 1) WITH NO_INFOMSGS;
   ELSE DBCC CHECKIDENT('dbo.categories', RESEED, 0) WITH NO_INFOMSGS;
  END;
  SET @BangLoi=N'sizes'; IF NOT EXISTS(SELECT 1 FROM dbo.[sizes])
  BEGIN
   IF EXISTS(SELECT 1 FROM sys.identity_columns WHERE object_id=OBJECT_ID(N'dbo.sizes') AND last_value IS NULL)
    DBCC CHECKIDENT('dbo.sizes', RESEED, 1) WITH NO_INFOMSGS;
   ELSE DBCC CHECKIDENT('dbo.sizes', RESEED, 0) WITH NO_INFOMSGS;
  END;
  SET @BangLoi=N'chat_lieu'; IF NOT EXISTS(SELECT 1 FROM dbo.[chat_lieu])
  BEGIN
   IF EXISTS(SELECT 1 FROM sys.identity_columns WHERE object_id=OBJECT_ID(N'dbo.chat_lieu') AND last_value IS NULL)
    DBCC CHECKIDENT('dbo.chat_lieu', RESEED, 1) WITH NO_INFOMSGS;
   ELSE DBCC CHECKIDENT('dbo.chat_lieu', RESEED, 0) WITH NO_INFOMSGS;
  END;
  SET @BangLoi=N'mau_sac'; IF NOT EXISTS(SELECT 1 FROM dbo.[mau_sac])
  BEGIN
   IF EXISTS(SELECT 1 FROM sys.identity_columns WHERE object_id=OBJECT_ID(N'dbo.mau_sac') AND last_value IS NULL)
    DBCC CHECKIDENT('dbo.mau_sac', RESEED, 1) WITH NO_INFOMSGS;
   ELSE DBCC CHECKIDENT('dbo.mau_sac', RESEED, 0) WITH NO_INFOMSGS;
  END;
  SET @BangLoi=N'de_giay'; IF NOT EXISTS(SELECT 1 FROM dbo.[de_giay])
  BEGIN
   IF EXISTS(SELECT 1 FROM sys.identity_columns WHERE object_id=OBJECT_ID(N'dbo.de_giay') AND last_value IS NULL)
    DBCC CHECKIDENT('dbo.de_giay', RESEED, 1) WITH NO_INFOMSGS;
   ELSE DBCC CHECKIDENT('dbo.de_giay', RESEED, 0) WITH NO_INFOMSGS;
  END;
  SET @BangLoi=N'product_code_seq'; IF NOT EXISTS(SELECT 1 FROM dbo.[products]) ALTER SEQUENCE dbo.[product_code_seq] RESTART WITH 1;
  SET @BangLoi=N'product_variants_ma_seq'; IF NOT EXISTS(SELECT 1 FROM dbo.[product_variants]) ALTER SEQUENCE dbo.[product_variants_ma_seq] RESTART WITH 1;
  SET @BangLoi=N'promotions_ma_seq'; IF NOT EXISTS(SELECT 1 FROM dbo.[promotions]) ALTER SEQUENCE dbo.[promotions_ma_seq] RESTART WITH 1;
  SET @BangLoi=N'danh_gia_ma_seq'; IF NOT EXISTS(SELECT 1 FROM dbo.[danh_gia]) ALTER SEQUENCE dbo.[danh_gia_ma_seq] RESTART WITH 1;
  SET @BangLoi=N'seq_chat_ma'; IF NOT EXISTS(SELECT 1 FROM dbo.[chat_hoi_thoai]) ALTER SEQUENCE dbo.[seq_chat_ma] RESTART WITH 1;
  IF NOT EXISTS(SELECT 1 FROM dbo.[categories]) ALTER SEQUENCE dbo.[categories_ma_seq] RESTART WITH 1;
  IF NOT EXISTS(SELECT 1 FROM dbo.[sizes]) ALTER SEQUENCE dbo.[sizes_ma_seq] RESTART WITH 1;
  IF NOT EXISTS(SELECT 1 FROM dbo.[chat_lieu]) ALTER SEQUENCE dbo.[chat_lieu_ma_seq] RESTART WITH 1;
  IF NOT EXISTS(SELECT 1 FROM dbo.[mau_sac]) ALTER SEQUENCE dbo.[mau_sac_ma_seq] RESTART WITH 1;
  IF NOT EXISTS(SELECT 1 FROM dbo.[de_giay]) ALTER SEQUENCE dbo.[de_giay_ma_seq] RESTART WITH 1;
  COMMIT; PRINT N'COMMIT: đã xác nhận. Không tự xóa file upload.';
 END
 ELSE BEGIN ROLLBACK; PRINT N'XEM TRƯỚC: ROLLBACK. Không đặt lại sequence/identity.'; END;
END TRY
BEGIN CATCH
 IF XACT_STATE()<>0 ROLLBACK;
 DECLARE @Loi nvarchar(2048)=CONCAT(N'Bảng/bước lỗi: ',@BangLoi,N'. ',ERROR_MESSAGE());
 THROW 51004,@Loi,1;
END CATCH;
