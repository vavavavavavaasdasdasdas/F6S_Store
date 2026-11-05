# Giai đoạn 32 — BẢN THAY THẾ: DELETE hẳn, không seed

Báo cáo này thay hoàn toàn bản dọn rồi tạo5sản phẩm mẫu trước đây. **Không kết nối hoặc chạy lệnh trên `shopbangiay`/`shopbangiay_thu`. Không seed. Không xóa file upload.** Các bằng chứng cũ trong `docs/evidence/gd32/` là lịch sử đã bị thay thế, không phải hướng dẫn vận hành hiện tại.

Nhánh: `hoan-thien-chat-thanh-toan`. Bản restore sạch cuối: **`shopbangiay_kt_gd32_20261105_214749`**. Nguồn fullbackup mới nhất đọc được theo msdb: `C:\Program Files\Microsoft SQL Server\MSSQL17.MSSQLSERVER\MSSQL\Backup\shopbangiay_kt_20261105_gd29_schema.bak`, hoàn tất `2026-11-05T09:26:45`, nguồn `shopbangiay_kt_20261105_gd29`. Không coi đây là backup mới của DB chính; agent chỉ đọc file có sẵn và RESTORE tên mới, không WITH REPLACE/không ghi đè DB đã tồn tại. Sau nhóm tạo dữ liệu thử/Maven/UI đã RESTORE thành bản mới, không DROP các DB thử cũ.

Kết quả: **27 PASS / 2 FAIL / 0 NOT TESTED**, tổng29hạng mục. Hai FAIL là lỗi UI/mạng ngoài phạm vi cleanup, được ghi rõ, không tuyên bố mọi thứ đều PASS.

| Hạng mục | Cách kiểm tra | Kết quả thực tế | PASS/FAIL/NOT TESTED |
|---|---|---|---|
| RESTORE an toàn | RESTORE mới; guard tên chưa tồn tại, không REPLACE | shopbangiay_kt_gd32_20261105_212258 | PASS |
| Kiểm kê và phân loại | INFORMATION_SCHEMA + COUNT_BIG từng bảng | 40 bảng: 7 giữ, 24 xóa toàn bộ, 9 xóa ngoài danh sách giữ/gốc | PASS |
| Nguồn danh mục gốc | Đọc shopgiay.sql qua lịch sử Git | 6 thương hiệu, 8 kích cỡ, 8 chất liệu, 11 màu, 8 đế; không lấy bản đã rút gọn | PASS |
| Tài khoản và mật khẩu | So toàn bộ cột users trước/sau + đăng nhập form thật | Chỉ admin id1 và user id2; mật khẩu không thay đổi | PASS |
| Hồ sơ, địa chỉ và cấu hình | So dữ liệu giữ trước/sau | 2 khách, 1 nhân viên, 1 địa chỉ; cấu hình/FAQ/địa giới/roles nguyên vẹn | PASS |
| Sai tên database | TenDB_DuKien=WRONG_DB trên bản sao | THROW trước thay đổi | PASS |
| Preview/thiếu xác nhận | Chạy mặc định rồi so toàn bộ bản ghi 40 bảng | ROLLBACK; SHA-256 nội dung trước/sau bằng nhau | PASS |
| Lệnh sqlcmd công khai | -v TenDB_DuKien + -i script trên bản cuối | Exit0; ROLLBACK; không tạo sản phẩm | PASS |
| DELETE thật, kể cả đang tắt | COUNT mọi bảng + COUNT mọi trạng thái | 24 bảng nghiệp vụ đều0; products/variants/promotions/vouchers ở mọi trạng thái đều0 | PASS |
| Danh mục ngoài gốc | Fixture5tên thử đang ẩn, chạy script trên bản sao | Đã DELETE thật ở cả5bảng, không đổi trạng thái để né xóa | PASS |
| Chốt tên có thể là dữ liệu thật | Fixture ASICS do agent tạo trên bản sao | Mặc định dừng trước DELETE; chỉ giữ sau khi điền rõ @DanhMucGiuThem; đã restore bỏ fixture | PASS |
| FK và schema | DBCC CHECKCONSTRAINTS; sys.foreign_keys | 40 bảng/50FK còn nguyên, không FK disabled/untrusted, không vi phạm | PASS |
| Không đổi status / không lách FK | rg UPDATE/NOCHECK/DISABLE/TRUNCATE/DROP trong script | Exit1, không có kết quả; script chỉ DELETE dữ liệu | PASS |
| Không seed mặc định | Gỡ file seed cũ; rg main/pom/aggregate/migrations; profile | Không gọi seed; DataInitializer chỉ profile test; không thêm route/nút xóa | PASS |
| Khởi động sau dọn | ddl-auto=validate, log Started/cổng riêng | Catalog bản sao, JPA validate sạch; Started cổng18032 | PASS |
| Trang rỗng 1280/375 | 34lượt trang chính bằng Playwright | Tất cả200, không tràn; sản phẩm/biến thể/khuyến mãi/hóa đơn/chat rỗng, Dashboard có empty state | PASS |
| Cửa hàng, giỏ và checkout rỗng | UI + GET checkout không follow redirect | Trang chủ “Cửa hàng chưa có sản phẩm”; danh sách không tìm thấy; giỏ trống; checkout302về/cart, không500 | PASS |
| Crawl ba vai trò | BFS anchor nội bộ đọc, loại link xóa/đăng xuất/xuất file | Khách13,user15,admin154; mỗi queue còn0, không status400/500 ở link đọc đã crawl | PASS |
| 8 kịch bản bảo mật | Khách/user/admin; POST thiếu/sai/đúngCSRF nhưng sai quyền | 302/403/200/403/403/403/403/403 đúng kỳ vọng | PASS |
| Thêm thương hiệu qua UI | Form thật, không gọi POST trực tiếp | Lưu được thương hiệu VERIFY; codeTH007; lỗi lần bấm đầu ghi riêng bên dưới | PASS |
| Thêm sản phẩm + biến thể qua UI | Form, checkbox39, chất liệu/đế/màu, upload logo sẵn có | POST302; SELECT SP001/BT0001,giá200000,tồn7; log0/+7/7/Khởi tạo/admin | PASS |
| Reset SEQUENCE/IDENTITY | SELECT sau nhập mới và probe trên bản sao | SP001/BT0001 và ID1; probeKM001/CH0001; không reset bảng còn dữ liệu | PASS |
| Khôi phục sau nhóm phá dữ liệu | RESTORE bản mới sau test nhập hàng | Bản cuối lại0sản phẩm/biến thể/đơn/hóa đơn/chat/phiếu/đợt; không giữ sản phẩm thử | PASS |
| Quét mẫu thử | SELECT trên cột tên/mã/email của31bảng | P12-/P7-/P8-/Phase/E2E/kiểm thử/test/poly/@example.*: tổng khớp0 | PASS |
| Maven không skip | mvnw.cmd clean test trên bản không seed | 315test,0failure,0error,0skip; giữ nguyên TestDatabaseGuard | PASS |
| Không xóa upload | Kiểm kê đĩa so với DB cuối không còn tham chiếu ảnh | 17file chỉ liệt kê; lệnh riêng mặc định -WhatIf, chưa thực thi | PASS |
| JavaScript và tài nguyên404 | pageerror/response404 trong trình duyệt | 0JS exception,0request404 | PASS |
| Bấm Thêm lần đầu ổn định | Đo nút trước/sau blur và event target | Nút lên22px khi ẩn vùng lỗi trống, click đầu rơi vào FORM; click lại lưu được. Chưa sửa UI ngoài phạm vi | FAIL |
| Tài nguyên bản đồ ngoài | requestfailed khi crawl form địa chỉ | tile.openstreetmap.org: net::ERR_NAME_NOT_RESOLVED. Chưa xử lý mạng ngoài trong giai đoạn dữ liệu | FAIL |

## A. Kiểm kê trước khi viết/chạy

[COUNT trước](evidence/gd32-xoa-han/counts-before.json), [preview thật](evidence/gd32-xoa-han/preview.txt), [xác nhận](evidence/gd32-xoa-han/confirmed.txt), [COUNT_BIG chính xác cuối](evidence/gd32-xoa-han/final-counts-exact.json), [câu SELECT đủ40bảng](evidence/gd32-xoa-han/SELECT_COUNT_TUNG_BANG.sql).

Nhóm “XÓA CÁC DÒNG NGOÀI DANH SÁCH GỐC” gồm5danh mục theo danh sách tên gốc và4bảng tài khoản/hồ sơ theo danh sách admin/user + dòng hệ thống. Không có bảng chưa rõ cần đoán. Bảng mới chưa kiểm kê sẽ làm script dừng, không tự xóa.

| Bảng | Phân loại | COUNT trước | COUNT sau xóa / bản cuối |
|---|---|---|---|
|admin_chat_audit|XÓA HẲN TOÀN BỘ|0|0|
|admin_chat_hoi_thoai|XÓA HẲN TOÀN BỘ|0|0|
|admin_chat_tin_nhan|XÓA HẲN TOÀN BỘ|0|0|
|categories|XÓA CÁC DÒNG NGOÀI DANH SÁCH GỐC|6|6|
|cau_hinh_phi_ship|GIỮ|1|1|
|chat_action_audit|XÓA HẲN TOÀN BỘ|0|0|
|chat_hoi_thoai|XÓA HẲN TOÀN BỘ|0|0|
|chat_lieu|XÓA CÁC DÒNG NGOÀI DANH SÁCH GỐC|8|8|
|chat_mail_log|XÓA HẲN TOÀN BỘ|0|0|
|chat_tin_nhan|XÓA HẲN TOÀN BỘ|0|0|
|chatbot_faq|GIỮ|9|9|
|customers|XÓA CÁC DÒNG NGOÀI DANH SÁCH GỐC|14|2|
|danh_gia|XÓA HẲN TOÀN BỘ|0|0|
|de_giay|XÓA CÁC DÒNG NGOÀI DANH SÁCH GỐC|8|8|
|dia_chi_nhan_hang|XÓA CÁC DÒNG NGOÀI DANH SÁCH GỐC|7|1|
|employees|XÓA CÁC DÒNG NGOÀI DANH SÁCH GỐC|5|1|
|inventory_logs|XÓA HẲN TOÀN BỘ|63|0|
|invoices|XÓA HẲN TOÀN BỘ|28|0|
|mau_sac|XÓA CÁC DÒNG NGOÀI DANH SÁCH GỐC|11|11|
|order_details|XÓA HẲN TOÀN BỘ|35|0|
|order_mail_log|XÓA HẲN TOÀN BỘ|0|0|
|order_status_history|XÓA HẲN TOÀN BỘ|28|0|
|orders|XÓA HẲN TOÀN BỘ|28|0|
|phieu_ho_tro|XÓA HẲN TOÀN BỘ|0|0|
|phuong_xa|GIỮ|3321|3321|
|pos_held_items|XÓA HẲN TOÀN BỘ|0|0|
|pos_held_orders|XÓA HẲN TOÀN BỘ|0|0|
|product_images|XÓA HẲN TOÀN BỘ|6|0|
|product_variants|XÓA HẲN TOÀN BỘ|20|0|
|products|XÓA HẲN TOÀN BỘ|10|0|
|promotion_details|XÓA HẲN TOÀN BỘ|7|0|
|promotions|XÓA HẲN TOÀN BỘ|5|0|
|roles|GIỮ|3|3|
|sizes|XÓA CÁC DÒNG NGOÀI DANH SÁCH GỐC|8|8|
|ten_tinh_thanh_cu|GIỮ|68|68|
|tinh_thanh|GIỮ|34|34|
|users|XÓA CÁC DÒNG NGOÀI DANH SÁCH GỐC|12|2|
|vouchers|XÓA HẲN TOÀN BỘ|15|0|
|vung_giao_hang|GIỮ|3|3|
|yeu_thich|XÓA HẲN TOÀN BỘ|0|0|

Cả24bảng nhóm xóa sạch có COUNT=0, không chỉ status=1. [SELECT mọi trạng thái](evidence/gd32-xoa-han/final-all-statuses-zero.json) cho products,product_variants,promotions,vouchers: tổng/đang bật/đang tắt/NULL đều0. Không có bảng giỏ hàng/token/session trong schema40bảng này: giỏ nằm trong HttpSession, session persistence tắt; không tự bịa câu DELETE bảng không tồn tại. Bảng phụ mới xuất hiện sẽ bị chốt bảng-chưa-kiểm-kê chặn.

### Tài khoản và dòng hệ thống giữ

| ID | Username | Vai trò | Hồ sơ khách | Hồ sơ nhân viên | Trạng thái |
|---|---|---|---|---|---|
|1|admin|ROLE_ADMIN|id11,AD000,Admin|id1,NV001,Lê Quản Trị|Đang hoạt động|
|2|user|ROLE_USER|id9,KH000,nguyen ngoc viet anh|Không có|Đang hoạt động|

Đúng một user/admin, không phân biệt hoa thường. Giữ địa chỉ liên kết. So toàn bộ cột users bao gồm mật khẩu trước/sau bằng nhau; không xuất hash hoặc mật khẩu vào evidence. Đăng nhập cả hai qua form thật đã PASS. [So sánh](evidence/gd32-xoa-han/keepers-check.json), [tài khoản trước](evidence/gd32-xoa-han/accounts-before.json).

Giữ cả3vai trò (kể cả ROLE_EMPLOYEE) theo quy tắc không xóa vai trò và để lần nhập nhân viên mới còn cấu hình quyền. Giữ toàn bộ cấu hình phí ship id1,FAQ,địa giới và vùng giao. [rg hệ thống](evidence/gd32-xoa-han/rg-system.txt): ShippingFeeService tham chiếu id1; POS dùng customer=NULL cho Khách lẻ, không có hồ sơ Khách lẻ riêng trong bản restore. Script vẫn giữ hồ sơ mang đúng tên Khách lẻ nếu có. Không tự đổi người dùng/khách hệ thống.

### Danh mục gốc — bảng đã công bố trước khi chạy

`shopgiay.sql` hiện tại từng bị rút gọn thành3thương hiệu/3kích cỡ. Lấy bản gốc từ lịch sử Git:21f76e6 cho categories/sizes;2229c5b cho chất liệu;ba08a0e cho đế;8410685 cho màu (các lần bổ sung bảng tương ứng). [Đoạn INSERT gốc](evidence/gd32-xoa-han/nguon-danh-muc-goc.txt), [danh sách máy đọc](evidence/gd32-xoa-han/danh-muc-goc.json), [từng ID và số đang dùng trước xóa](evidence/gd32-xoa-han/catalog-before.json).

| Danh mục | Dòng giữ | Dòng sẽ xóa trong bản backup | Cần duyệt thực tế |
|---|---|---|---|
|Thương hiệu|Nike, Adidas, Converse, Vans, Puma, New Balance|Không có|Không có|
|Kích cỡ|36, 37, 38, 39, 40, 41, 42, 43|Không có|Không có|
|Chất liệu|Da thật, Da tổng hợp, Da lộn, Vải canvas, Vải lưới, Vải dệt kim, Cao su, Khác|Không có|Không có|
|Màu sắc|Đen, Trắng, Xám, Đỏ, Xanh dương, Xanh lá, Vàng, Nâu, Be, Hồng, Khác|Không có|Không có|
|Đế giày|Cao su, EVA, PU, TPU, Đế bằng, Đế độn, Đế chống trượt, Khác|Không có|Không có|

So tên bỏ qua hoa thường và khoảng trắng (space/tab/CR/LF/NBSP), giữ phân biệt dấu. Không dùng ID để đoán gốc. Dòng khớp gốc giữ nguyên toàn bộ dữ liệu/status. Tên ngoài gốc rõ là thử được DELETE; tên ngoài gốc có thể là thật được SELECT “CẦN TÔI DUYỆT” rồi THROW trước DELETE. `@DanhMucGiuThem` mặc định rỗng, chủ dữ liệu tự điền tên đã duyệt GIỮ.

Đã thử5fixture ngoài gốc đang ẩn (VERIFY,99test,poly,TESTmàu,Phaseđế): DELETE thật cả5bảng. ASICS trong ca chốt an toàn là fixture agent tự tạo trên bản sao để thử guard, không phải dữ liệu thực đang chờ duyệt. Mặc định bị chặn, điền rõ giữ thêm mới chạy; sau đó RESTORE bỏ tất cả fixture. [Chặn](evidence/gd32-xoa-han/catalog-approval-blocked.txt), [preview](evidence/gd32-xoa-han/catalog-approved-preview.txt), [commit thử](evidence/gd32-xoa-han/catalog-approved-commit.txt), [SELECT kiểm chứng](evidence/gd32-xoa-han/catalog-fixture-check.json).

## B. Script và an toàn

`database/tools/don_du_lieu_thu.sql` nằm ngoài migration. Toàn bộ DELETE trong một transaction; lỗi XACT_ABORT/ROLLBACK và THROW tên bảng/bước. Tính thứ tự con trước cha theo các FK; không tắt constraint/trigger. Hiển thị tài khoản giữ và kế hoạch danh mục trước DELETE. Có chốt tên DB, đúng một admin/user, danh sách bảng đã kiểm kê, app lock và khóa bảng cho đợt bảo trì. Dừng app trước khi chủ dự án tự vận hành.

Chỉ COMMIT khi @XAC_NHAN bằng chính xác XOA-DU-LIEU-THU. Mặc định ROLLBACK và không reset counter. [Hash toàn bộ40bảng sau preview bằng trước](evidence/gd32-xoa-han/preview-check.json), [saiDB bị chặn](evidence/gd32-xoa-han/wrong-db.txt), [lệnh sqlcmd công khai preview](evidence/gd32-xoa-han/public-sqlcmd-preview.txt).

[rg thô](evidence/gd32-xoa-han/rg-no-status-update-no-seed-guard.txt): `UPDATE|NOCHECK|DISABLE|TRUNCATE|DROP` trong script không có kết quả (exit1). Không có UPDATE kể cả để ghi thống kê; bảng đếm là table variable. Không thêm nút/route xóa vào app. Không đổi schema/package/luật kho/hủy/doanh thu. File seed cũ đã gỡ; không có bước seed trong hướng dẫn mới hoặc aggregate.

[Schema cuối](evidence/gd32-xoa-han/final-schema.json):40bảng,50FK,0disabled/untrusted; DBCC CHECKCONSTRAINTS không trả vi phạm. [Counter cuối](evidence/gd32-xoa-han/final-counters.json): chỉ bảng đã rỗng hoàn toàn được reset; khách/nhân viên/users/địa chỉ còn dữ liệu không reset. Danh mục còn dữ liệu giữ nguyên sequence. Trường hợp bảng chưa từng INSERT dùng RESEED phù hợp để không sinh ID0.

## C. Web sạch, nhập mới và test

[Log Started/catalog](evidence/gd32-xoa-han/app-started.txt). Cấu hình kiểm thử dùng biến môi trường bắt buộc trỏ tên bản restore, port18032,ddl-auto=validate; không sửa application-local.properties/IDE/default datasource. [34lượt trang chính + crawl](evidence/gd32-xoa-han/browser-empty.json), [bảng URL đầy đủ](evidence/gd32-xoa-han/URL_STATUS.md), [8bảo mật](evidence/gd32-xoa-han/SECURITY.md).

- Trang chủ: “Cửa hàng chưa có sản phẩm”; /products: “Không tìm thấy sản phẩm nào!”.
- Dashboard: doanh thu0,empty state,không500. Hai dòng cố định Online/POS trong bảng kênh không phải đơn hàng; mọi giá trị là0.
- Chi tiết sản phẩm/biến thể/đợt/phiếu/hóa đơn/chat/tồn kho đều có thông báo rỗng,0dòng dữ liệu. Profile/đơn/yêu thích/giỏ của user vẫn mở được.
- /checkout khi giỏ rỗng chuyển302 đến /cart (Location có thể là URL tuyệt đối), giữ nguyên nghiệp vụ; không giả tạo checkout có sản phẩm.
- 34lượt1280/375không tràn ngang,JS exception0,request404=0. Crawl13khách+15user+154admin,queue0; không chạy link xóa/đăng xuất/xuất file.

Ảnh: [cửa hàng rỗng375](evidence/gd32-xoa-han/screenshots/guest-products-375.png), [sản phẩm Admin rỗng375](evidence/gd32-xoa-han/screenshots/admin-products-crud-375.png), [Dashboard375](evidence/gd32-xoa-han/screenshots/admin-dashboard-375.png), các ảnh khác trong thư mục screenshots.

Sau khi chụp/crawl trạng thái rỗng, thử bằng form thật:

| Thao tác | Bằng chứng thực tế |
|---|---|
|Thêm thương hiệu|Tên VERIFY Thương hiệu nhập mới,codeTH007,giữ nguyên các6thương hiệu gốc|
|Thêm sản phẩm đang bán|SP001; ảnh dùng logo.jpg sẵn có trong project, không vẽ/tải ảnh bên thứ ba|
|Biến thể ban đầu|BT0001,kích cỡ39,màuĐen,chất liệuDa thật,đếCao su,giá200000,tồn7|
|InventoryLog|Khởi tạo,quantity_before0/change+7/after7,actoradmin|
|Probe sequence tiếp|KM001 và CH0001; không tạo đợt/chat mẫu|

[SELECT độc lập](evidence/gd32-xoa-han/created-select.sql), [kết quả giá/tồn/log/mã](evidence/gd32-xoa-han/created-select.json), [probe mã](evidence/gd32-xoa-han/sequence-probe.json), [form thật](evidence/gd32-xoa-han/browser-created.json), [thương hiệu](evidence/gd32-xoa-han/browser-created-brand.json), [ảnh biến thể375](evidence/gd32-xoa-han/screenshots/created-variant-375.png).

Sau nhóm ghi dữ liệu, app đã dừng và RESTORE bản mới rồi chạy chỉ DELETE. Bản cuối không còn sản phẩm/thương hiệu thử vừa nhập, không có seed. [COUNT cuối và context](evidence/gd32-xoa-han/final-copy.json). Quét31bảng có tên/mã/email với cả poly: [output0](evidence/gd32-xoa-han/final-pattern-scan.json), [SELECT thô](evidence/gd32-xoa-han/pattern-scan.sql).

### Bộ test không phụ thuộc dữ liệu vận hành

Lượt đầu trên DB không seed:315test,0failure,28error do TOP1 bản ghi sản phẩm/biến thể/đợt/đơn có sẵn hoặc lấy nhầm fixture thiếu thương hiệu. [Output trước sửa](evidence/gd32-xoa-han/maven-before-fixture-fix.txt). Đã sửa13class phụ thuộc dữ liệu thành fixture riêng trong src/test, dùng helper CatalogTestFixtures có guard DB và yêu cầu transaction; các test đang dùng transaction rollback giữ cách rollback đó. InvoiceManagementIntegrationTest vốn kiểm thử giao dịch commit thì tạo fixture riêng bằng TransactionTemplate, nhóm test được loại bỏ bằng RESTORE mới. ObjectCodeIntegrationTest lấy thuộc tính từ danh mục gốc, không lấy từ biến thể tồn sẵn. Không tạo seed khi ứng dụng khởi động.

Giữ mọi test/assertion và không sửa TestDatabaseGuard/Listener. Kết quả chạy đủ sau sửa trên bản restore không seed: **315test,0failure,0error,0skipped,BUILD SUCCESS**. [Output](evidence/gd32-xoa-han/maven-summary.txt), [số liệu](evidence/gd32-xoa-han/maven-summary.json). Sau Maven lại RESTORE trước UI; sau UI lại RESTORE bản cuối. DataInitializer vốn đã @Profile("test") được giữ nguyên, không bật profile này trên DB chính. [rg startup](evidence/gd32-xoa-han/rg-startup.txt).

### Lỗi ngoài phạm vi được ghi riêng

| Mã | File:dòng | Nguyên nhân và bằng chứng | Trạng thái |
|---|---|---|---|
|GD32R-UI01|src/main/resources/static/js/quan-tri-danh-muc.js:25|Ẩn vùng lỗi trống khi blur làm nút Thêm nhảy22px: Y441.67→419.67,clickđầu targetFORM/khôngPOST. Bấm lại lúc ổn định lưu được. create-diagnostic.json và ảnh cùng tên.|FAIL,chưa sửa UI|
|GD32R-NET01|src/main/resources/static/js/dia-chi-giao-hang.js:43|Tile OpenStreetMap lỗiDNS khi crawl form địa chỉ; không phải404 nội bộ/không phải lỗiSQL. browser-empty.json/requestFailed.|FAIL,chưa sửa mạng ngoài|

Không tuyên bố console tài nguyên ngoài sạch. Thêm thành công được kiểm bằng thao tác UI bình thường sau khi blur hoàn tất/bấm lại, không dùng form.submit()/POST API hoặc gỡ validation. Ghi thêm trong docs/BUG_PHAT_HIEN.md để làm ở phạm vi riêng.

## D. CẦN TÔI DUYỆT — không tự xóa file

Danh mục thực ngoài gốc: **không có** trên backup đã kiểm kê. Dòng hệ thống chưa rõ: **không có**. Có17file upload mồ côi so với DB tạm đã dọn (product_images và chat đều0), trong đó một file do lần thử upload logo tạo. Không xóa/di chuyển file nào. Đối chiếu lại tham chiếu trên DB đích sau khi chủ dự án tự dọn thành công; không xóa file trước bước đó.

| STT | Đường dẫn | Byte | Quyết định |
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
|15|C:/Users/ADMIN/Documents/DuAnTNDEMO/DuAnTNDEMO/DuAnTNDEMO/DemoDATN/uploads/products/cf5f9462-0cd2-4f03-9d99-46a467725c35.jpg|7524|CẦN DUYỆT|
|16|C:/Users/ADMIN/Documents/DuAnTNDEMO/DuAnTNDEMO/DuAnTNDEMO/DemoDATN/uploads/products/cfb59015-0be6-46a5-9100-b77787863ad1.png|15067|CẦN DUYỆT|
|17|C:/Users/ADMIN/Documents/DuAnTNDEMO/DuAnTNDEMO/DuAnTNDEMO/DemoDATN/uploads/products/f8f008a4-d54d-4701-9935-692f875c94d7.jpg|54649|CẦN DUYỆT|

## E. Chỗ đã thay đổi so với bản cũ

- Bỏ hoàn toàn yêu cầu5sản phẩm/40biến thể/2đợt/5phiếu; gỡ database/seed/du_lieu_mau.sql. Database cuối không có dữ liệu mẫu. Chủ cửa hàng tự nhập thật qua UI.
- Danh mục không giữ rộng theo mọi dòng trong DB: dùng danh sách gốc từ lịch sử shopgiay.sql + @DanhMucGiuThem đã được chủ dữ liệu điền. Tên thật chưa duyệt làm script dừng, không tự đoán.
- Ngoại lệ có chủ đích: chỉ SEQUENCE/IDENTITY của bảng đã rỗng hoàn toàn được đặt lại; cho phép nhập tiếpSP001/BT0001/KM001/CH0001. Không reset bảng còn dữ liệu giữ, không DROP đối tượng.
- Test tự tạo fixture thay cho yêu cầu dữ liệu vận hành có sẵn; không khôi phục seed để làm test pass, không xóa test hoặc nới chốt DB.
- Không thêm route/nút xóa,không sửa nghiệp vụ kho/doanh thu/hủy,không đổi mật khẩu,tên bảng/package/enum hoặc cấu hình DB mặc định.

## F. Lệnh chủ dự án tự chạy trên DB chính SAU SAO LƯU

**Agent chưa thực thi các lệnh này trên DB chính.** Dừng ứng dụng, sao lưu và kiểm tra khả năng khôi phục trước. Chạy từ thư mục project, dùng -E hoặc SQLCMDPASSWORD, không đặt mật khẩu trong repo/dòng lệnh.

```powershell
$dbMucTieu="shopbangiay" # Chỉ chủ dự án tự chọn sau sao lưu
# 1. XEM TRƯỚC — ROLLBACK, duyệt tài khoản/danh mục/COUNT
sqlcmd -S localhost -E -C -b -f 65001 -d $dbMucTieu -v "TenDB_DuKien=$dbMucTieu" -i database/tools/don_du_lieu_thu.sql
```

Nếu có danh mục “CẦN TÔI DUYỆT”, dừng, quyết định và tự điền @DanhMucGiuThem trong script rồi xem trước lại. Mặc định danh sách này rỗng.

2. Chỉ sau khi đã duyệt, tạo file cục bộ tmp/xac_nhan_gd32.sql:

```sql
EXEC sys.sp_set_session_context @key=N'XAC_NHAN',@value=N'XOA-DU-LIEU-THU';
GO
:r database/tools/don_du_lieu_thu.sql
```

```powershell
# 2. XÁC NHẬN DELETE — không seed sau bước này
sqlcmd -S localhost -E -C -b -f 65001 -d $dbMucTieu -v "TenDB_DuKien=$dbMucTieu" -i tmp/xac_nhan_gd32.sql
# 3. UPLOAD — riêng, chỉ xem trước từng LiteralPath; chưa chạy xóa
powershell -NoProfile -File docs/evidence/gd32-xoa-han/xoa_upload_sau_khi_duyet.ps1
```

Bước3 có đúng các file đã liệt kê và -WhatIf. Chỉ chủ dự án sau sao lưu/duyệt/đối chiếu lại mới bỏ -WhatIf ở dòng được duyệt. Không wildcard,không -Recurse. Không xóa ảnh tĩnh/logo của project hoặc ảnh vẫn còn tham chiếu.

Không có migration mới,không có bước seed. App/bộ duyệt test đã dừng. Chỉ chốt một commit cho bản thay thế rồi DỪNG; không merge/push hoặc tự vận hành trên DB chính.

Bổ sung đối chiếu tag P12- trên danh mục đang tắt: [preview rollback](evidence/gd32-xoa-han/p12-preview.txt), [DELETE xác nhận](evidence/gd32-xoa-han/p12-delete.txt). Lượt Maven cuối chạy sau sửa tag:315/0/0/0. Sau đó đã RESTORE lại bản cuối nêu đầu báo cáo; không giữ fixture P12 hoặc sản phẩm nhập thử.

Chốt bảo toàn dữ liệu giữ trên bản RESTORE cuối: đã so **toàn bộ cột** của users (kể cả mật khẩu), customers IDs9/11,employeeID1,địa chỉID4 trước/sau DELETE; đều bằng nhau. [Bằng chứng boolean và IDs, không công bố mật khẩu/hash](evidence/gd32-xoa-han/keepers-check.json).
