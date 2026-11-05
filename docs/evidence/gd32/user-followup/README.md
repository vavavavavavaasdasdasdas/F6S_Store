# Bổ sung kiểm thử bằng tài khoản user — giai đoạn 32

Mật khẩu do chủ dự án cung cấp chỉ truyền qua biến môi trường cho trình duyệt. Không đổi mật khẩu, không đưa giá trị vào evidence hoặc Git.

| Hạng mục | Cách kiểm tra | Kết quả thực tế | PASS/FAIL/NOT TESTED |
|---|---|---|---|
|Đăng nhập user|Form đăng nhập thật|Thành công, chuyển về /|PASS|
|Crawl user|BFS mọi anchor đọc nội bộ, bỏ link xóa/đăng xuất/xuất file|22URL duy nhất; queue0;32lượt gồm viewport;không404/JS exception|PASS|
|Giao diện user|Profile,đơn,yêu thích,checkout,chi tiết đơn và hóa đơn tại1280/375|Tất cả trang được đo không tràn ngang; có ảnh|PASS|
|Đặt COD có voucher|Form checkout thật,GIAM50K|Đơn1 thuộc customer9;1dòng variant17;số tiền server1260000-50000=1210000|PASS|
|Kho đúng biến thể|SELECT trước/sau từng bước và so40biến thể|22→22khi đặt→21khi xác nhận;ba bước sau21;39biến thể khác và40giá không đổi|PASS|
|Xử lý ở Hóa đơn|Bấm nút xác nhận hộp thoại|CONFIRMED→PREPARING→SHIPPING→DELIVERED;POST302;PAIDởcuối|PASS|
|Quyền user|3GETadmin và1POSTcóCSRFhợp lệ|Đều403|PASS|

## SQL trước/sau

| Bước | Trạng thái | Thanh toán | Tồn variant17 | Đã trừ kho |
|---|---|---|---|---|
|Trước đặt|Không có đơn|—|22|—|
|placed|PENDING|UNPAID|22|False|
|CONFIRMED|CONFIRMED|UNPAID|21|True|
|PREPARING|PREPARING|UNPAID|21|True|
|SHIPPING|SHIPPING|UNPAID|21|True|
|DELIVERED|DELIVERED|PAID|21|True|

Câu SELECT: [select-order-stock.sql](select-order-stock.sql). Output thô mỗi bước: stock-*.json; inventory_logs gồm trước/thay đổi/sau/người thực hiện. [So40biến thể](all-variants-after.json). [Form/crawl](user-browser.json), [chuyển bước + hóa đơn user](order-processing.json).

## URL bổ sung

| URL | Vai trò | Viewport | Status | Tràn ngang |
|---|---|---|---|---|
|/profile|user|1280|200|False|
|/orders|user|1280|200|False|
|/favorites|user|1280|200|False|
|/cart|user|1280|200|False|
|/products|user|1280|200|False|
|/|user|1280|200|False|
|/category/1|user|1280|200|False|
|/category/2|user|1280|200|False|
|/category/3|user|1280|200|False|
|/category/4|user|1280|200|False|
|/category/5|user|1280|200|False|
|/category/6|user|1280|200|False|
|/category/8|user|1280|200|False|
|/products?promotion=true|user|1280|200|False|
|/profile?newAddress=true|user|1280|200|False|
|/policy|user|1280|200|False|
|/products/5|user|1280|200|False|
|/products/4|user|1280|200|False|
|/products/3|user|1280|200|False|
|/products/2|user|1280|200|False|
|/products/1|user|1280|200|False|
|/products?sort=newest|user|1280|200|False|
|/profile|user|1280|200|False|
|/orders|user|1280|200|False|
|/favorites|user|1280|200|False|
|/profile|user|375|200|False|
|/orders|user|375|200|False|
|/favorites|user|375|200|False|
|/checkout|user|1280|200|False|
|/checkout|user|375|200|False|
|/orders/1|user|1280|200|False|
|/orders/1|user|375|200|False|
|/orders|user|1280|200|False|
|/orders/1|user|1280|200|False|
|/orders/1/hoa-don|user|1280|200|False|
|/orders|user|375|200|False|
|/orders/1|user|375|200|False|
|/orders/1/hoa-don|user|375|200|False|

## Quyền bổ sung

| Yêu cầu | Vai trò | Status thực tế |
|---|---|---|
|GET /admin/products-crud|user|403|
|GET /admin/employees|user|403|
|GET /admin/statistics|user|403|
|POST /admin/invoices/1/status,CSRFhợp lệ|user|403|

Mật khẩu được giữ nguyên. Không gửi thư thật vì SMTP chưa cấu hình. Ảnh bản đồ trắng do DNS là lỗi đã ghi ở GD32-05; không coi đây là đã kiểm thử bản đồ mạng thành công.

## Chốt dữ liệu sau nhóm kiểm thử

`mvnw clean test`:315test,0failure,0error,0skip. Một phép SELECT sai múi giờ trong test đã được sửa, thêm fixture20:30 để không còn phụ thuộc giờ chạy; không đổi luật/service doanh thu. [Trước sửa](maven-before-date-fix.txt), [sau sửa](maven-after-date-fix.txt).

Đã RESTORE bản mới `shopbangiay_kt_gd32_20261105_203055`,cleanup/seed lại; đơn/hóa đơn/chat0,5sản phẩm/40biến thể/2đợt/5phiếu,giữ2tài khoản. [Trạng thái cuối](final-clean-state.json). Không xóa file upload hoặc kết nối database chính; app/browser thử đã đóng.
