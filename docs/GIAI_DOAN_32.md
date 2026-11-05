# Giai đoạn 32 — dọn dữ liệu và dữ liệu mẫu thực tế

Chỉ làm giai đoạn 32 trên nhánh `hoan-thien-chat-thanh-toan`. **Không kết nối hoặc chạy lệnh trên `shopbangiay` và `shopbangiay_thu`.** Không chạy xóa file upload. Không merge/push. Hai script nằm ngoài migration và không được đưa vào chay_tat_ca.sql.

Bản restore nghiệm thu ban đầu: `shopbangiay_kt_gd32_20261105_165907`. Nguồn full backup mới nhất đọc được theo msdb: `C:\Program Files\Microsoft SQL Server\MSSQL17.MSSQLSERVER\MSSQL\Backup\shopbangiay_kt_20261105_gd29_schema.bak`, database nguồn `shopbangiay_kt_20261105_gd29`, hoàn tất `2026-11-05T09:26:45`. Không coi nguồn này là một backup mới của DB chính. RESTORE mới không WITH REPLACE, dừng nếu tên DB đã tồn tại.

Đã lần lượt dùng các bản sao mới hậu tố 162539 (test đầu),164232 (crawl),165400 (chat đã sửa và test cuối),165907 (trạng thái sạch cuối). Không ghi đè DB có sẵn. Sau nhóm ghi dữ liệu, RESTORE thành bản mới; tin nhắn thử không còn trong bản cuối. Các DB thử cũ được để nguyên, không tự DROP.

Tổng 30 hạng mục: **26 PASS / 2 FAIL / 2 NOT TESTED**. Đây không phải nghiệm thu toàn bộ PASS: đăng nhập/đặt đơn/crawl user đã được kiểm tra bổ sung; còn ảnh thật và ngưỡng voucher dưới500.000đ chưa có bằng chứng.

| Hạng mục | Cách kiểm tra | Kết quả thực tế | PASS/FAIL/NOT TESTED |
|---|---|---|---|
| RESTORE riêng | RESTORE HEADERONLY/FILELISTONLY, tên timestamp, không REPLACE | shopbangiay_kt_gd32_20261105_165907; backup 2026-11-05T09:26:45 | PASS |
| Kiểm kê toàn bộ | INFORMATION_SCHEMA + sys.tables + FK | 40 bảng, 354 cột, 50 FK; phân loại bên dưới | PASS |
| Tài khoản giữ | SELECT trước xóa; so mọi cột trước/sau | admin id1 + user id2; giữ nguyên mật khẩu; 2 khách, 1 nhân viên, 1 địa chỉ | PASS |
| Dữ liệu cấu hình/hành chính/FAQ | So toàn bộ nội dung 12 bảng trước/sau cleanup | Không đổi; không có hồ sơ Khách lẻ; POS dùng customer=NULL | PASS |
| Chốt sai DB | Chạy trên DB tạm với TenDB_DuKien=WRONG_DATABASE | THROW 51000 trước thay đổi | PASS |
| Thiếu xác nhận / xem trước | Chạy mặc định; so nội dung cả 40 bảng | ROLLBACK, SHA-256 trước/sau bằng nhau; không reset counter | PASS |
| Xóa xác nhận và FK | SESSION_CONTEXT đúng; DBCC CHECKCONSTRAINTS | 24 bảng xóa sạch =0; 4 bảng xóa có điều kiện; 50 FK không disabled/untrusted | PASS |
| Reset mã/identity | SELECT mã + thử mở chat sau reset | SP001..SP005, BT0001..BT0040, KM001..KM002; chat thử ID1/CH0001 | PASS |
| Seed hai lần | So mọi bản ghi, không chỉ COUNT | 5 SP / 40 BT / 40 log / 5 ảnh tham chiếu / 2 đợt / 4 liên kết / 5 phiếu; không nhân đôi | PASS |
| Giá/tồn/log chuẩn | SELECT từng dòng, join thuộc tính, log trước/sau | Mỗi SP 8 BT; đúng 1 BT tồn3 và 1 BT tồn0, còn lại 10..25; 40 log Khởi tạo | PASS |
| Mẫu dữ liệu thử | SELECT LIKE trên mọi cột tên/mã/email liên quan | 31 bảng được quét; tổng khớp P12-/P7-/P8-/Phase/E2E/kiểm thử/test/@example.* =0 | PASS |
| Không mọc lại dữ liệu | So COUNT 40 bảng sau browser với sau seed | Cả 40 COUNT bằng nhau; DataInitializer chỉ profile test | PASS |
| ddl-auto=validate / khởi động | Cấu hình env bắt buộc trỏ bản sao, chờ log | Initialized JPA EntityManagerFactory; Started; port18032 | PASS |
| Đăng nhập admin | Form thật; mật khẩu hiện có | Chuyển /admin; không đổi mật khẩu | PASS |
| Đăng nhập user | Form đăng nhập thật bằng mật khẩu hiện có | Thành công, chuyển về /; không đổi mật khẩu; user-followup/user-browser.json | PASS |
| Trang chính 1280/375 | Playwright + ảnh + DOM | 20 lượt trang cuối đều200; đếm đúng5/40/2/5, hóa đơn/chat rỗng, Dashboard tồn0 và3; các trang chính không tràn375 | PASS |
| Giá trên cửa hàng | Đối chiếu SELECT độc lập với HTML thật | Nike2929000; Adidas4200000; Converse1260000; Vans1620000; ASICS3900000 | PASS |
| 5 voucher đủ điều kiện | Giỏ thật ASICS3900000, gọi VoucherService qua checkout | 150000/50000/100000/300000/500000; VIP20 Tốt nhất; SELECT khớp | PASS |
| Voucher chưa đủ: GIAM100K/SALE15/VIP20 | Giỏ Converse1260000 | Bị khóa; thiếu240000/740000/1740000 tương ứng | PASS |
| Ngưỡng dưới500000 cho CHAOMUNG10/GIAM50K | Cần tạm tính dưới500000 qua luồng thật | Mọi sản phẩm seed đều trên500000; chưa có bằng chứng ca này trên dữ liệu seed | NOT TESTED |
| Đặt đơn bằng user và xử lý kho | Form thật COD + GIAM50K; bấm xử lý Hóa đơn và SELECT từng bước | Đơn DH000001: 1.260.000−50.000=1.210.000đ; tồn22→22→21→21→21→21; cuối DELIVERED/PAID | PASS |
| Chat khách–admin realtime | Hai browser context, không reload khách, SELECT | Khách gửi; admin trả lời; khách nhận; ID1/CH0001,3 tin. Sau đó RESTORE bản mới, chat cuối0 | PASS |
| Crawl nội bộ | BFS anchor đọc; bỏ hành động xóa/đăng xuất/xuất file | 246 URL Admin +20 URL khách; hàng đợi còn0; xem bảng URL chi tiết | PASS |
| Crawl quyền user | Đăng nhập thật, BFS link đọc + 1280/375 | 22URL duy nhất, queue0; không JS exception/404/tràn; bổ sung 3GETadmin403 và1POSThóađơn403 cóCSRFhợp lệ | PASS |
| 8 kịch bản bảo mật | GET quyền khách; POST thiếu/sai CSRF; admin thiếu CSRF | 8/8 đúng302/403; không thay đổi dữ liệu | PASS |
| JavaScript và 404 trang cuối | pageerror + response404 trên20 lượt | 0 JavaScript exception;0 request404 | PASS |
| Console không lỗi tài nguyên ngoài | console/requestfailed khi mở bản đồ | DNS không phân giải tile.openstreetmap.org; lưu bằng chứng, chưa xử lý trong giai đoạn dữ liệu | FAIL |
| POS không tràn desktop | Playwright đo scrollWidth tại1280 | /admin/pos overflow=true ở1280, false ở375; lỗi ngoài phạm vi dọn/seed | FAIL |
| Ảnh sản phẩm thật | Kiểm tra5 đường dẫn file trên đĩa | Thiếu5/5 ảnh do chủ cửa hàng cung cấp; fallback đã hoạt động,0 request404 | NOT TESTED |
| mvnw clean test | Chạy đầy đủ cuối nhóm sửa | 315 tests,0 failures,0 errors,0 skipped; BUILD SUCCESS; guard giữ nguyên | PASS |

## Kiểm kê trước khi viết script và COUNT trước/sau

Nguồn: INFORMATION_SCHEMA.TABLES, sys.partitions; [COUNT trước](evidence/gd32/counts-before.json), [output xem trước](evidence/gd32/preview.txt), [output xác nhận](evidence/gd32/confirmed.txt), [COUNT sau browser](evidence/gd32/counts-after-browser.json). Cột Sau xóa được ghi trước seed; seed chỉ thêm ASICS vào categories, không sửa danh mục cũ.

| Bảng | Nhóm | Trước | Sau xóa | Sau seed / browser |
|---|---|---|---|---|
| admin_chat_audit | XÓA SẠCH | 0 | 0 | 0 |
| admin_chat_hoi_thoai | XÓA SẠCH | 0 | 0 | 0 |
| admin_chat_tin_nhan | XÓA SẠCH | 0 | 0 | 0 |
| categories | GIỮ | 6 | 6 | 7 |
| cau_hinh_phi_ship | GIỮ | 1 | 1 | 1 |
| chat_action_audit | XÓA SẠCH | 0 | 0 | 0 |
| chat_hoi_thoai | XÓA SẠCH | 0 | 0 | 0 |
| chat_lieu | GIỮ | 8 | 8 | 8 |
| chat_mail_log | XÓA SẠCH | 0 | 0 | 0 |
| chat_tin_nhan | XÓA SẠCH | 0 | 0 | 0 |
| chatbot_faq | GIỮ | 9 | 9 | 9 |
| customers | XÓA THEO ĐIỀU KIỆN | 14 | 2 | 2 |
| danh_gia | XÓA SẠCH | 0 | 0 | 0 |
| de_giay | GIỮ | 8 | 8 | 8 |
| dia_chi_nhan_hang | XÓA THEO ĐIỀU KIỆN | 7 | 1 | 1 |
| employees | XÓA THEO ĐIỀU KIỆN | 5 | 1 | 1 |
| inventory_logs | XÓA SẠCH | 63 | 0 | 40 |
| invoices | XÓA SẠCH | 28 | 0 | 0 |
| mau_sac | GIỮ | 11 | 11 | 11 |
| order_details | XÓA SẠCH | 35 | 0 | 0 |
| order_mail_log | XÓA SẠCH | 0 | 0 | 0 |
| order_status_history | XÓA SẠCH | 28 | 0 | 0 |
| orders | XÓA SẠCH | 28 | 0 | 0 |
| phieu_ho_tro | XÓA SẠCH | 0 | 0 | 0 |
| phuong_xa | GIỮ | 3321 | 3321 | 3321 |
| pos_held_items | XÓA SẠCH | 0 | 0 | 0 |
| pos_held_orders | XÓA SẠCH | 0 | 0 | 0 |
| product_images | XÓA SẠCH | 6 | 0 | 5 |
| product_variants | XÓA SẠCH | 20 | 0 | 40 |
| products | XÓA SẠCH | 10 | 0 | 5 |
| promotion_details | XÓA SẠCH | 7 | 0 | 4 |
| promotions | XÓA SẠCH | 5 | 0 | 2 |
| roles | GIỮ | 3 | 3 | 3 |
| sizes | GIỮ | 8 | 8 | 8 |
| ten_tinh_thanh_cu | GIỮ | 68 | 68 | 68 |
| tinh_thanh | GIỮ | 34 | 34 | 34 |
| users | XÓA THEO ĐIỀU KIỆN | 12 | 2 | 2 |
| vouchers | XÓA SẠCH | 15 | 0 | 5 |
| vung_giao_hang | GIỮ | 3 | 3 | 3 |
| yeu_thich | XÓA SẠCH | 0 | 0 | 0 |

Không có bảng chưa rõ cần tự đoán. Script kiểm tra danh sách bảng đã kiểm kê; thêm bảng mới thì dừng và duyệt lại. 24 bảng xóa sạch;4 bảng theo điều kiện;12 bảng giữ. Danh mục nghi thử: **0 mục** sau kiểm kê tất cả tên và số đang dùng ([danh sách đầy đủ](evidence/gd32/shared-lookups-before.json)). `@DanhMucDuyetXoa` để rỗng.

### Tài khoản/hồ sơ giữ

| ID | Username | Vai trò | Hồ sơ khách | Hồ sơ nhân viên | Trạng thái |
|---|---|---|---|---|---|
|1|admin|ROLE_ADMIN|id11,AD000,Admin|id1,NV001,Lê Quản Trị|Đang hoạt động|
|2|user|ROLE_USER|id9,KH000,nguyen ngoc viet anh|Không có|Đang hoạt động|

Giữ cả địa chỉ thuộc hai hồ sơ. So toàn bộ cột users bao gồm password trước/sau: bằng nhau; không công bố hash/mật khẩu trong evidence. [Kết quả so sánh](evidence/gd32/keepers-check.json). Nhận diện username không phân biệt hoa thường, dừng nếu không có đúng một user/admin.

### Dòng hệ thống và bộ khởi tạo

[rg trước/sau và bằng chứng guard](evidence/gd32/rg-startup-system.txt): chỉ có DataInitializer triển khai CommandLineRunner; không thấy runner/fixture khởi động khác trong src/main. Thêm @Profile("test") và test xác nhận annotation; nội dung bootstrap vẫn giữ cho test. Không bật profile test trên DB chính. ShippingFeeService dùng cấu hình id1: giữ toàn bộ cau_hinh_phi_ship. Toàn bộ FAQ,role,tỉnh/xã,tên tỉnh cũ,vùng giao đều giữ. POS biểu diễn khách lẻ bằng customer=null (PosHoldService), không phụ thuộc ID khách cứng; không có bản ghi Khách lẻ trong bản backup. Script vẫn giữ hồ sơ tên Khách lẻ nếu gặp.

Thứ tự xóa con trước cha được suy từ [50 FK](evidence/gd32/foreign-keys-before.json):

```text
admin_chat_audit, admin_chat_tin_nhan, chat_action_audit, chat_mail_log, chat_tin_nhan, danh_gia, dia_chi_nhan_hang, inventory_logs, invoices, order_details, order_mail_log, order_status_history, pos_held_items, product_images, promotion_details, yeu_thich, admin_chat_hoi_thoai, chat_hoi_thoai, orders, pos_held_orders, product_variants, promotions, phieu_ho_tro, products, vouchers, customers, employees, users
```

Không NOCHECK/disable constraints, không DROP bảng/cột/index/sequence. DBCC CHECKCONSTRAINTS không trả vi phạm;50 FK đều trusted/enabled.

## Dữ liệu mẫu, giá và voucher

[40 biến thể và log](evidence/gd32/seed-products.json), [đợt](evidence/gd32/seed-promotions.json), [liên kết](evidence/gd32/seed-promotion_details.json), [phiếu](evidence/gd32/seed-vouchers.json), [hash chạy seed hai lần](evidence/gd32/seed-idempotent.json). Chạy lại không ghi đè giá/tồn hoặc thêm lại log. Chữ NVARCHAR có dấu giữ nguyên. Mỗi sản phẩm 2 màu ×39/40/41/42,1 chất liệu,1 đế. BT0001 đen39 tồn0;BT0005 trắng39 tồn3;38 dòng khác10..25.

| Mã | Sản phẩm | Giá gốc SELECT | Giá sau giảm SELECT / HTML |
|---|---|---|---|
| SP001 | Nike Air Force 1 '07 | 2,929,000 | 2,929,000 / 2,929,000 |
| SP002 | Adidas Ultraboost Light | 4,500,000 | 4,200,000 / 4,200,000 |
| SP003 | Converse Chuck Taylor All Star Classic | 1,400,000 | 1,260,000 / 1,260,000 |
| SP004 | Vans Old Skool | 1,800,000 | 1,620,000 / 1,620,000 |
| SP005 | ASICS Gel-Kayano 14 | 4,200,000 | 3,900,000 / 3,900,000 |

[SELECT giá độc lập](evidence/gd32/pricing-select.sql) và [HTML/response service](evidence/gd32/browser-extra.json). Ngày bắt đầu05/11/2026 00:00; hết30/11 hoặc31/12 23:59:59.997 giờViệt Nam. Mã SQL: promotion dùng AMOUNT, voucher dùng FIXED theo code sẵn có; không đổi enum hay luật giảm.

| Phiếu | Tạm tính3.900.000 — service | SELECT độc lập | Tạm tính1.260.000 — service |
|---|---|---|---|
| CHAOMUNG10 | 150,000 | 150,000 | 126,000 |
| GIAM100K | 100,000 | 100,000 | Khóa; mua thêm 240,000 |
| GIAM50K | 50,000 | 50,000 | 50,000 |
| SALE15 | 300,000 | 300,000 | Khóa; mua thêm 740,000 |
| VIP20 | 500,000 | 500,000 | Khóa; mua thêm 1,740,000 |

[SELECT voucher](evidence/gd32/voucher-select.sql). Ngưỡng dưới500.000 cho hai phiếu còn NOT TESTED qua catalog seed vì mọi giá đều cao hơn ngưỡng; không giả giá client để tuyên bố đã test.

## Bằng chứng kiểm thử

- [Maven output và số test](evidence/gd32/maven-summary.txt):315/0/0/0. Test tra đơn trước kia phụ thuộc DH000002/ORD-85019F6B: chuyển thành tài khoản/khách/đơn tự tạo có code/phone/email duy nhất trong transaction rollback; giữ nguyên các assertion khách/chủ đơn/người khác. Không xóa test hoặc nới TestDatabaseGuard.
- [Log Started + catalog](evidence/gd32/app-started.txt), [schema](evidence/gd32/schema-check.json), [quét31 bảng không còn mẫu thử](evidence/gd32/test-pattern-scan.json), [SQL quét thô](evidence/gd32/read-only-select.sql).
- [Bảng URL đầy đủ](evidence/gd32/URL_STATUS.md):246URL Admin,20URL khách,không còn link đọc mới trong queue; không GET hành động xóa/đăng xuất/xuất file. Đã bổ sung crawl session user ở user-followup/README.md. [8 bảo mật](evidence/gd32/SECURITY.md).
- [DOM cuối](evidence/gd32/final-browser.json):20lượt200,JS exception0,404=0;5/40/2/5; hóa đơn/chat0; tồn0/3. [Chat nhận không reload](evidence/gd32/chat-realtime.json), [SELECT ID1,CH0001,3tin](evidence/gd32/chat-select-after.json); dữ liệu thử đó đã được loại bằng RESTORE mới.
- [Ảnh1280/375](evidence/gd32/screenshots/):store,products,dashboard,products-crud,variants,promotions,vouchers,invoices,chat,pos; ảnh chat realtime riêng. Có kiểm tra trực quan ảnh cửa hàng/Dashboard375, bảng cuộn trong khung.

## Lỗi tìm được và cách xử lý

| Mã | Vị trí | Nguyên nhân / sửa | Chạy lại |
|---|---|---|---|
|GD32-01|database/seed/du_lieu_mau.sql:74|Bản seed đầu dùng AMOUNT trong khi VoucherService nhận FIXED; sửa dữ liệu seed thành FIXED, không sửa service|5/5 phiếu đủ điều kiện đúng số tiền; xem voucher-before-fix.json và browser-extra.json|
|GD32-02|database/tools/don_du_lieu_thu.sql:258|RESEED0 với bảng chưa từng INSERT sinh ID0; phân biệt identity last_valueNULL để dùng seed1|Chat từ0/CH0001 không mở được → ID1/CH0001 nhắn realtime PASS|
|GD32-03|src/test/java/com/example/demodatn/service/hotro/ChatbotIntegrationTest.java:132|Fixture phụ thuộc đơn cũ đã bị xóa; tự tạo fixture độc lập trong transaction|315test PASS; giữ nguyên assertion quyền|
|GD32-06|src/test/java/com/example/demodatn/service/hotro/AdminChatbotIntegrationTest.java:98|SELECT test coi DATETIME giờViệtNam là UTC rồi cộng7giờ, làm dữ liệu sau17giờ sang ngày mai. Đổi CAST(created_at AS DATE), thêm fixture20:30 trong transaction rollback và assertion bắt buộc tính dòng đó. Không sửa service/luật doanh thu|Chạy lại toàn bộ315test; output lưu ở user-followup|
|GD32-04|src/main/resources/static/css/quan-tri.css:175 và trang POS|Đo thấy tràn ngang ở1280; chưa chốt phần tử CSS gây ra. Ngoài phạm vi dữ liệu, chưa sửa|Tái hiện2lượt;375không tràn|
|GD32-05|src/main/resources/static/js/dia-chi-giao-hang.js:43|Tile OpenStreetMap không phân giải DNS trong môi trường test; không phải404 nội bộ|requestfailed net::ERR_NAME_NOT_RESOLVED; chưa sửa|

Hai lỗi ngoài phạm vi được ghi thêm trong docs/BUG_PHAT_HIEN.md. Không tuyên bố console sạch hoặc toàn bộ UI PASS.

## CẦN TÔI DUYỆT — chưa xóa file

Không có danh mục nghi thử hoặc dòng hệ thống chưa rõ từ dữ liệu đã kiểm kê. Có16file mồ côi sau cleanup/seed, **chỉ là danh sách đề nghị duyệt**, không tự di chuyển/xóa. Kiểm kê lại tham chiếu nếu DB thay đổi trước khi vận hành.

| STT | Đường dẫn | Byte | Đề nghị |
|---|---|---|---|
|1|C:/Users/ADMIN/Documents/DuAnTNDEMO/DuAnTNDEMO/DuAnTNDEMO/DemoDATN/uploads/products/0f636dba-2f2e-49c1-b6a3-81b2818fd74a.png|174676|CẦN DUYỆT|
|2|C:/Users/ADMIN/Documents/DuAnTNDEMO/DuAnTNDEMO/DuAnTNDEMO/DemoDATN/uploads/products/11a7718c-3e08-4f86-a00c-7426c87586d2.png|109157|CẦN DUYỆT|
|3|C:/Users/ADMIN/Documents/DuAnTNDEMO/DuAnTNDEMO/DuAnTNDEMO/DemoDATN/uploads/products/22f8f786-2c37-4902-823e-8cbb7b0a7a1a.png|193637|CẦN DUYỆT|
|4|C:/Users/ADMIN/Documents/DuAnTNDEMO/DuAnTNDEMO/DuAnTNDEMO/DemoDATN/uploads/products/2c1f2f74-eb80-47b4-957b-8545ac6f42a3.jpg|7524|CẦN DUYỆT|
|5|C:/Users/ADMIN/Documents/DuAnTNDEMO/DuAnTNDEMO/DuAnTNDEMO/DemoDATN/uploads/products/2de02991-f9fe-4467-9261-3ad01181844b.png|3763542|CẦN DUYỆT|
|6|C:/Users/ADMIN/Documents/DuAnTNDEMO/DuAnTNDEMO/DuAnTNDEMO/DemoDATN/uploads/products/44b085c1-37d3-4409-bba8-822ae4130c21.png|174676|CẦN DUYỆT|
|7|C:/Users/ADMIN/Documents/DuAnTNDEMO/DuAnTNDEMO/DuAnTNDEMO/DemoDATN/uploads/products/44f4d99c-63ae-464b-9f2e-b61455ddf1ec.png|15067|CẦN DUYỆT|
|8|C:/Users/ADMIN/Documents/DuAnTNDEMO/DuAnTNDEMO/DuAnTNDEMO/DemoDATN/uploads/products/4b06f5d4-5c36-4a84-9b89-82ff556baa41.png|198416|CẦN DUYỆT|
|9|C:/Users/ADMIN/Documents/DuAnTNDEMO/DuAnTNDEMO/DuAnTNDEMO/DemoDATN/uploads/products/91e95ec3-69ac-4d8a-bbde-625a765c253f.png|193637|CẦN DUYỆT|
|10|C:/Users/ADMIN/Documents/DuAnTNDEMO/DuAnTNDEMO/DuAnTNDEMO/DemoDATN/uploads/products/95810a0c-1237-449c-afb5-385c9a4dbdad.png|185307|CẦN DUYỆT|
|11|C:/Users/ADMIN/Documents/DuAnTNDEMO/DuAnTNDEMO/DuAnTNDEMO/DemoDATN/uploads/products/99567ef9-828e-4473-a33d-88b835608a9e.png|77911|CẦN DUYỆT|
|12|C:/Users/ADMIN/Documents/DuAnTNDEMO/DuAnTNDEMO/DuAnTNDEMO/DemoDATN/uploads/products/9a3e3459-d2f2-4aaf-b896-05ddc7936379.png|3763542|CẦN DUYỆT|
|13|C:/Users/ADMIN/Documents/DuAnTNDEMO/DuAnTNDEMO/DuAnTNDEMO/DemoDATN/uploads/products/a26921ee-fc69-4081-9805-d0856cdd7a8c.png|34954|CẦN DUYỆT|
|14|C:/Users/ADMIN/Documents/DuAnTNDEMO/DuAnTNDEMO/DuAnTNDEMO/DemoDATN/uploads/products/a9e29b27-7030-45e0-9fa8-1e342edd5bf5.png|198416|CẦN DUYỆT|
|15|C:/Users/ADMIN/Documents/DuAnTNDEMO/DuAnTNDEMO/DuAnTNDEMO/DemoDATN/uploads/products/cfb59015-0be6-46a5-9100-b77787863ad1.png|15067|CẦN DUYỆT|
|16|C:/Users/ADMIN/Documents/DuAnTNDEMO/DuAnTNDEMO/DuAnTNDEMO/DemoDATN/uploads/products/f8f008a4-d54d-4701-9935-692f875c94d7.jpg|54649|CẦN DUYỆT|

## Ảnh chủ cửa hàng cần bổ sung

| Sản phẩm | Đường dẫn chính xác | Trạng thái |
|---|---|---|
|Nike Air Force 1 '07|C:/Users/ADMIN/Documents/DuAnTNDEMO/DuAnTNDEMO/DuAnTNDEMO/DemoDATN/uploads/products/nike-air-force-1-07.jpg|THIẾU — dùng ảnh thay thế hệ thống|
|Adidas Ultraboost Light|C:/Users/ADMIN/Documents/DuAnTNDEMO/DuAnTNDEMO/DuAnTNDEMO/DemoDATN/uploads/products/adidas-ultraboost-light.jpg|THIẾU — dùng ảnh thay thế hệ thống|
|Converse Chuck Taylor All Star Classic|C:/Users/ADMIN/Documents/DuAnTNDEMO/DuAnTNDEMO/DuAnTNDEMO/DemoDATN/uploads/products/converse-chuck-taylor-classic.jpg|THIẾU — dùng ảnh thay thế hệ thống|
|Vans Old Skool|C:/Users/ADMIN/Documents/DuAnTNDEMO/DuAnTNDEMO/DuAnTNDEMO/DemoDATN/uploads/products/vans-old-skool.jpg|THIẾU — dùng ảnh thay thế hệ thống|
|ASICS Gel-Kayano 14|C:/Users/ADMIN/Documents/DuAnTNDEMO/DuAnTNDEMO/DuAnTNDEMO/DemoDATN/uploads/products/asics-gel-kayano-14.jpg|THIẾU — dùng ảnh thay thế hệ thống|

Không tải/vẽ ảnh bên thứ ba. URL script trỏ /uploads/products/<tên trên>; ProductImageUrlService.source kiểm tra file và trả /anh/anh-san-pham-thay-the.svg khi thiếu, đã thấy trong ảnh và không gây404.

## Chỗ đã thay đổi so với giai đoạn cũ

- Ngoại lệ có chủ đích: chỉ reset SEQUENCE/IDENTITY của bảng đã xóa sạch, cho phép tái dùng SP001/BT0001/KM001/CH0001. Không reset people/danh mục còn dữ liệu. Không DROP đối tượng. Bảng chưa từng ghi có quy tắc RESEED riêng để không sinh ID0.
- DataInitializer không chạy ở profile mặc định/production; chỉ profile test. Dữ liệu mẫu được tạo bằng script riêng, không lồng vào migration.
- Test tra đơn không yêu cầu đơn mẫu cũ tồn tại; tự tạo fixture,không giảm kiểm tra quyền.
- Không thay luật kho/hủy/doanh thu/giá; không đổi route/package/enum; không đổi mật khẩu admin/user. Không sửa cấu hình datasource mặc định/local/IDE.

## Lệnh chủ dự án tự chạy sau sao lưu — CHƯA thực thi trên DB chính

Dừng app, sao lưu và kiểm tra bản cứu hộ trước. Chạy từ thư mục project. Dùng Windows Authentication hoặc SQLCMDPASSWORD, không đưa mật khẩu vào file/Git/dòng lệnh. **Không tự động chạy các lệnh dưới đây.**

```powershell
$dbMucTieu="shopbangiay" # chỉ chủ dự án tự chọn sau khi sao lưu
# 1. XEM TRƯỚC, mặc định ROLLBACK
sqlcmd -S localhost -E -C -b -f 65001 -d $dbMucTieu -v "TenDB_DuKien=$dbMucTieu" -i database/tools/don_du_lieu_thu.sql
```

2. Duyệt tài khoản/count/danh mục. Tạo file local tmp/xac_nhan_gd32.sql:

```sql
EXEC sys.sp_set_session_context @key=N'XAC_NHAN',@value=N'XOA-DU-LIEU-THU';
GO
:r database/tools/don_du_lieu_thu.sql
```

```powershell
# 2. XÁC NHẬN (chỉ sau khi đã duyệt)
sqlcmd -S localhost -E -C -b -f 65001 -d $dbMucTieu -v "TenDB_DuKien=$dbMucTieu" -i tmp/xac_nhan_gd32.sql
# 3. SEED (chạy lại được)
sqlcmd -S localhost -E -C -b -f 65001 -d $dbMucTieu -v "TenDB_DuKien=$dbMucTieu" -i database/seed/du_lieu_mau.sql
# 4. FILE MỒ CÔI — riêng, chỉ XEM TRƯỚC; chưa có file nào được xóa
powershell -NoProfile -File docs/evidence/gd32/xoa_upload_sau_khi_duyet.ps1
```

Bước4 có đúng16 LiteralPath và -WhatIf. Chỉ chủ dự án sau sao lưu/duyệt/recheck tham chiếu mới bỏ -WhatIf ở đúng dòng muốn xóa; không dùng wildcard/LIKE rộng/Remove-Item-Recurse. Không xóa ảnh mẫu mới hoặc file vẫn được DB tham chiếu. Hai script bảo trì không chạy qua migration; giai đoạn này không có migration cần chạy.

Dừng giai đoạn32 tại đây. Đã bổ sung nghiệm thu đăng nhập/đặt đơn/crawl user sau khi được cung cấp mật khẩu. Ảnh thật và ngưỡng voucher dưới500.000đ vẫn NOT TESTED.

App/bộ duyệt kiểm thử đã dừng sau khi chụp bằng chứng; cấu hình local/DB chính không bị thay đổi.

Kiểm tra thêm bằng đúng `sqlcmd -v TenDB_DuKien=... -i database/tools/don_du_lieu_thu.sql` trên bản cuối sau khi dừng app: exit0, ROLLBACK, COUNT40bảng không đổi ([output thô](evidence/gd32/sqlcmd-public-preview.txt)).

## Bổ sung sau khi chủ dự án cung cấp mật khẩu user

[Báo cáo và SQL trước/sau riêng](evidence/gd32/user-followup/README.md), [form/crawl thật](evidence/gd32/user-followup/user-browser.json), [các bước Hóa đơn](evidence/gd32/user-followup/order-processing.json), [ảnh1280/375](evidence/gd32/user-followup/screenshots/).

Ba hạng mục cũ NOT TESTED (đăng nhập user, đặt đơn/xử lý kho, crawl quyền user) đã chuyển PASS bằng bằng chứng mới. Các ghi chú NOT TESTED trong ảnh/output JSON chụp trước khi có mật khẩu được giữ nguyên để không sửa bằng chứng lịch sử; phần bổ sung này thay kết luận cũ về ba hạng mục đó.

User tự đặt một đơn COD, voucher GIAM50K. SELECT sau đặt: total_amount1260000,discount_amount50000,final_amount1210000,customer_id9,variant_id17,quantity1,statusPENDING,paymentUNPAID,da_tru_kho=false,tồn22. Xác nhận ở Hóa đơn: tồn21 và log−1. Chuẩn bị/Giao/Hoàn tất không trừ lần hai, cuốiDELIVERED/PAID. Cả40giá và39tồn khác không đổi. Chỉ thông tin nhận hàng của đơn thử được nhập, không sửa hồ sơ/mật khẩu/sổ địa chỉ. Thử trên bản RESTORE mới201412; không dùng DB chính.

Test đối chiếu doanh thu được cập nhật thêm ở lượt bổ sung vì lộ lỗi sau17giờ: không đổi truy vấn production. Xem [output thất bại trước sửa](evidence/gd32/user-followup/maven-before-date-fix.txt). Fixture20:30 đảm bảo test không còn phụ thuộc giờ chạy; không xóa assertion hoặc nới điều kiện doanh thu.

Kết quả cuối lượt bổ sung: **315test,0failure,0error,0skip,BUILD SUCCESS** ([output sau sửa test ngày](evidence/gd32/user-followup/maven-after-date-fix.txt)). Không đổi code tính doanh thu production. Sau test đã RESTORE mới thành `shopbangiay_kt_gd32_20261105_203055`, không ghi đè DB cũ; dọn/seed lại: orders/invoices/order_details/chat=0,products5,variants40,promotions2,vouchers5,users2,FKkhôngvi phạm. [COUNT40bảng và tài khoản cuối](evidence/gd32/user-followup/final-clean-state.json), [RESTORE output](evidence/gd32/user-followup/final-clean-restore.txt). App kiểm thử đã dừng. Không kết nối shopbangiay/shopbangiay_thu.
