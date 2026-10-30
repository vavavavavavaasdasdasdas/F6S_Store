# Giai đoạn 5 — Màu sắc và bộ năm biến thể

Ngày kiểm thử: 30/10/2026. Nhánh: `gop-y-giang-vien`. Chỉ thực hiện Giai đoạn 5; Giai đoạn 6–8 chưa thực hiện trong commit này.

## Kết quả nghiệm thu

| Hạng mục | Cách kiểm tra | Kết quả thực tế | PASS/FAIL/NOT TESTED |
|---|---|---|---|
| RESTORE bản sao mới | RESTORE FILELISTONLY, log RESTORE, guard DB chưa có | shopbangiay_kt_20261030_gd5; backup mới nhất 29/10/2026 09:50:28 | PASS |
| Migration 013 chạy lặp | Hai lần sqlcmd; đối chiếu COUNT, metadata | 22 bảng; lần 2 COUNT/schema không đổi | PASS |
| chay_tat_ca chạy lặp | Hai lần trên dữ liệu có các biến thể cùng bộ ba | 004–013 hoàn tất; dữ liệu/schema trước = sau 1 = sau 2 | PASS |
| Dữ liệu cũ | SELECT trước/sau giá, tồn, trạng thái, chi tiết đơn | 20 biến thể và 35 dòng đơn giữ nguyên; màu Khác, kế thừa đế; snapshot mới NULL | PASS |
| Ràng buộc DB | Chèn trùng bộ năm, tên màu khác dấu, mã hex sai; rollback | Bị chặn lần lượt 2601 / 2627 / 547 | PASS |
| CRUD Màu sắc | Form thật hai viewport; picker/mã, trùng/sai, sửa/ẩn/xóa | Đang dùng dẫn bộ lọc; trang 2 STT=13; xóa chưa dùng xong COUNT 11→11 | PASS |
| Form sản phẩm/biến thể | Tạo sản phẩm kèm 2 kích cỡ; tạo cùng kích cỡ khác màu/đế | Mỗi sản phẩm ban đầu 2 dòng; cùng kích cỡ khác màu/đế được; bộ năm trùng báo cạnh ô | PASS |
| Combo tạo nhanh | Tạo màu/đế ngay trong form; test gấp chữ/dấu | Tạo được, không tạo trùng; hidden không xuất hiện cho tạo mới | PASS |
| Danh sách admin, nhãn và lọc | SELECT EXISTS độc lập so trang; >3 tên | Tối đa 3 nhãn rồi +1; lọc từ biến thể đang bán; giữ phân trang | PASS |
| Chi tiết sản phẩm | Chọn 4 tổ hợp thực ở 1280/375, đối chiếu SELECT | Đúng ID/giá/tồn; một giá trị tự chọn; tổ hợp hết hàng disabled; mẫu màu có tên | PASS |
| Thêm nhanh | Thao tác combo trên thẻ và POST AJAX thật | 200; đúng màu/đế trong giỏ | PASS |
| Giỏ, checkout, thành công, đơn User | Mua 2 màu bằng form trên hai viewport | Hai dòng riêng; chuyển khoản trừ đúng 2 ID biến thể | PASS |
| POS, hóa đơn chờ | Chọn đủ thuộc tính; lưu/mở/duyệt lại/thanh toán | Hai dòng giữ đúng; POS trừ đúng 2 ID; snapshot đúng | PASS |
| Đổi tên/ẩn thuộc tính | Đổi/ẩn màu đang dùng rồi xem đơn, hóa đơn và form | Đơn/hóa đơn giữ tên cũ; sản phẩm cũ giữ thông tin; combo tạo mới bỏ màu ẩn | PASS |
| Kho, lịch sử, Dashboard | Chỉnh tồn về 0 bằng form; SELECT inventory_logs; crawl | Log 7 / -7 / 0, actor admin; cột màu/đế riêng trong kho và Dashboard | PASS |
| Hóa đơn và bản in | Giả lập CSS print, ảnh 1280/375; đo 11 cột | Đủ STT và cột màu/đế riêng; hai cột tiền không bị ép mất độ rộng | PASS |
| mvnw clean test | JDK 17, clean test không skip | 160 test; failures=0; errors=0; skipped=0; BUILD SUCCESS | PASS |
| Playwright và crawl | Lần chạy cuối từng mã/viewport; status/JS/request/layout | 41 ca PASS; 76 lượt URL; 0 JS error; 0 request 404; không tràn ngang các trang đã crawl | PASS |
| 8 nhóm bảo mật | HTTP thật và MockMvc, gồm CSRF thiếu/sai và truy cập chéo | Khách 302, User 403, Admin 200; POST CSRF sai/thiếu 403; đơn người khác 404 | PASS |
| Khôi phục sau test | RESTORE checkpoint, so toàn bộ COUNT/schema/biến thể/chi tiết đơn | Khớp hoàn toàn checkpoint; app validate sạch, Started; app kiểm thử đã dừng | PASS |
| In bằng máy in vật lý | Chưa kết nối máy in | Chỉ kiểm thử CSS print và ảnh chụp, chưa in giấy | NOT TESTED |

## Bằng chứng và môi trường

Bản sao mới `shopbangiay_kt_20261030_gd5`, app cổng `18170`, `ddl-auto=validate`. Backup nguồn mới nhất tìm được: `shopbangiay_kt_20261029_bs012` lúc 29/10/2026 09:50:28. Chỉ đọc metadata backup qua master/msdb và kết nối bản sao. Không kết nối `shopbangiay` hoặc `shopbangiay_thu`; không chạy migration trên hai DB đó. Script RESTORE ban đầu từ chối nếu tên DB đã tồn tại. Sau từng nhóm test phá dữ liệu đã RESTORE lại checkpoint riêng của bản sao; chỉ các lần khôi phục bản sao do phiên này tạo mới dùng REPLACE.

Raw RESTORE: [restore-new.txt](evidence/giai-doan-5/restore-new.txt). Raw migration: [013 lần 1](evidence/giai-doan-5/migration-first.txt), [013 lần 2](evidence/giai-doan-5/migration-second.txt), [aggregate lần 1](evidence/giai-doan-5/aggregate-first.txt), [aggregate lần 2](evidence/giai-doan-5/aggregate-second.txt). Metadata/COUNT/biến thể trước sau và SHA256 của 35 dòng đơn được giữ trong các file `before-013`, `after-013-*`, `final-restored` ở thư mục bằng chứng.

Raw Started cuối:

```text
2026-10-30 12:07:19.252  INFO c.example.demodatn.DemoDatnApplication : Started DemoDatnApplication in 6.762 seconds (process running for 7.082)
runtimeSessions = [{"database_name": "shopbangiay_kt_20261030_gd5", "host_process_id": 37908}]
countsAndSchemaAndOldVariantsAndOrderDetailsMatchCheckpoint = true
```

[Startup log](evidence/giai-doan-5/startup.txt), [đối chiếu restore cuối](evidence/giai-doan-5/final-proof.json). App kiểm thử đã dừng. Cấu hình local/IntelliJ đã trỏ bản sao mới có 013; các file cấu hình riêng/cookie/mật khẩu không được commit. Cần mở lại project trong IntelliJ để IDE nạp cấu hình đã lưu.

## COUNT trước/sau migration

| Bảng | Trước 013 | Sau lần 1 | Sau lần 2 |
|---|---:|---:|---:|
| categories | 6 | 6 | 6 |
| chat_lieu | 8 | 8 | 8 |
| customers | 14 | 14 | 14 |
| de_giay | 8 | 8 | 8 |
| dia_chi_nhan_hang | 7 | 7 | 7 |
| employees | 5 | 5 | 5 |
| inventory_logs | 63 | 63 | 63 |
| invoices | 28 | 28 | 28 |
| mau_sac | 0 | 11 | 11 |
| order_details | 35 | 35 | 35 |
| orders | 28 | 28 | 28 |
| pos_held_items | 0 | 0 | 0 |
| pos_held_orders | 0 | 0 | 0 |
| product_images | 6 | 6 | 6 |
| product_variants | 20 | 20 | 20 |
| products | 10 | 10 | 10 |
| promotion_details | 7 | 7 | 7 |
| promotions | 5 | 5 | 5 |
| roles | 2 | 2 | 2 |
| sizes | 8 | 8 | 8 |
| users | 12 | 12 | 12 |
| vouchers | 15 | 15 | 15 |

Biến thể cũ: [SELECT backfill](evidence/giai-doan-5/backfill.json). Giá/tồn/trạng thái và các dòng đơn giữ nguyên. `ten_mau_sac`, `ten_de_giay` của đơn cũ NULL, view hiển thị `—`.

## SELECT độc lập và giao dịch mua hàng

Các câu SQL đã dùng (ID test thực tế trước khi restore):

```sql
SELECT v.id,v.product_id,v.size_id,v.chat_lieu_id,v.mau_sac_id,v.de_giay_id,
       v.price,v.quantity,v.status
FROM product_variants v WHERE v.product_id IN (949,950) ORDER BY v.id;

SELECT p.id,p.code FROM products p WHERE p.status=1
AND EXISTS(SELECT 1 FROM product_variants v WHERE v.product_id=p.id
 AND v.status=1 AND v.mau_sac_id=12
 AND v.de_giay_id=(SELECT id FROM de_giay WHERE ten=N'Cao su'))
ORDER BY p.id DESC;
-- SELECT: id=949, code=SP006. Admin và /products cùng khớp kết quả này.

SELECT product_variant_id,ten_mau_sac,ten_de_giay,quantity,price
FROM order_details WHERE order_id IN (674,675,676,677)
ORDER BY order_id,product_variant_id;

SELECT variant_id,quantity_before,quantity_change,quantity_after,reason,actor
FROM inventory_logs WHERE variant_id=601 ORDER BY id;
```

Output SQL trước/sau: [sales.json](evidence/giai-doan-5/sales.json), [rename-filter.json](evidence/giai-doan-5/rename-filter.json), [inventory-before-after.json](evidence/giai-doan-5/inventory-before-after.json).

```text
SP006: variant 598 = Màu Ánh 1280 / Cao su / 125000; tồn 9 -> 8 (online) -> 7 (POS)
       variant 600 = Đỏ / Cao su / 175000; tồn 7 -> 6 (online) -> 5 (POS)
SP007: variant 602 = Màu Ánh 375 / Cao su / 125000; tồn 9 -> 8 -> 7
       variant 604 = Đỏ / Cao su / 175000; tồn 7 -> 6 -> 5
Điều chỉnh variant 601: quantity_before=7, quantity_change=-7, quantity_after=0, actor=admin
Ràng buộc SQL: duplicate tuple=2601; duplicate accent name=2627; invalid hex=547
```

Order 674/675 giữ `Màu Ánh 1280`, order 676/677 giữ `Màu Ánh 375` sau khi đổi tên lookup thành `Màu đã đổi ...`. Các ID và giao dịch test đã mất sau restore; bằng chứng lưu từ lúc kiểm thử.

## Nơi hiển thị thuộc tính

| Nơi hiển thị | Cách kiểm tra | Kết quả |
|---|---|---|
| Sản phẩm admin | Nhãn màu/đế tối đa 3+n; bộ lọc EXISTS và trang xem | PASS |
| Biến thể | Form, danh sách và bộ năm được kiểm thử | PASS |
| Tồn kho/điều chỉnh/lịch sử | Cột riêng; đổi về 0, SELECT log | PASS |
| Dashboard sắp hết hàng | Header/cột riêng và route 200 hai viewport | PASS |
| Chi tiết User | Chọn mọi tổ hợp so SQL; giá/tồn và mẫu màu | PASS |
| Thêm nhanh | Combo bốn thuộc tính, AJAX 200 và giỏ đúng dòng | PASS |
| Giỏ/checkout/thành công | Mua hai màu tại cả hai viewport | PASS |
| Chi tiết đơn User/Admin | Cột snapshot, giữ tên sau đổi tên | PASS |
| Hóa đơn/bản in | 11 cột, ảnh cuối và giá đúng | PASS |
| POS/hóa đơn chờ | Chọn bốn thuộc tính; lưu/mở/thanh toán | PASS |
| Fixture/DataInitializer/test | Fixture gán màu/đế; DataInitializer tạo lookup Khác khi cần; 160 test PASS | PASS |

## Bảo mật

| Nhóm | Cách kiểm tra HTTP | Kết quả thực tế | Trạng thái |
|---|---|---|---|
| 1 | Khách GET các route Admin | 302 | PASS |
| 2 | User GET các route Admin | 403 | PASS |
| 3 | Admin GET các route Admin | 200 | PASS |
| 4 | Khách POST với CSRF hợp lệ | 302 tới đăng nhập | PASS |
| 5 | User POST tạo màu/biến thể có CSRF | 403 | PASS |
| 6 | Admin POST thiếu CSRF | 403 | PASS |
| 7 | Admin POST sai CSRF | 403 | PASS |
| 8 | User xem chi tiết/thành công đơn của người khác | 404 | PASS |

[Output HTTP thật](evidence/giai-doan-5/security-http.json). Kiểm tra trên `/admin/colors`, `/admin/variants`, `/admin/products-crud`, `/admin/inventory`; POST `/admin/colors/save`, `/admin/variants/save`. Mọi form POST trong 76 lượt crawl đều có input CSRF.

## URL thực tế

| URL | Vai trò | Viewport | Status | Kết quả |
|---|---|---|---:|---|
| / | guest | 1280px | 200 | PASS |
| /products | guest | 1280px | 200 | PASS |
| /products?promotion=true | guest | 1280px | 200 | PASS |
| /products/950 | guest | 1280px | 200 | PASS |
| /category/1 | guest | 1280px | 200 | PASS |
| /cart | guest | 1280px | 200 | PASS |
| /login | guest | 1280px | 200 | PASS |
| /register | guest | 1280px | 200 | PASS |
| / | guest | 375px | 200 | PASS |
| /products | guest | 375px | 200 | PASS |
| /products?promotion=true | guest | 375px | 200 | PASS |
| /products/950 | guest | 375px | 200 | PASS |
| /category/1 | guest | 375px | 200 | PASS |
| /cart | guest | 375px | 200 | PASS |
| /login | guest | 375px | 200 | PASS |
| /register | guest | 375px | 200 | PASS |
| /profile | user | 1280px | 200 | PASS |
| /cart | user | 1280px | 200 | PASS |
| /checkout | user | 1280px | 200 | PASS |
| /orders | user | 1280px | 200 | PASS |
| /orders/676 | user | 1280px | 200 | PASS |
| /orders/success/676 | user | 1280px | 200 | PASS |
| /profile | user | 375px | 200 | PASS |
| /cart | user | 375px | 200 | PASS |
| /checkout | user | 375px | 200 | PASS |
| /orders | user | 375px | 200 | PASS |
| /orders/676 | user | 375px | 200 | PASS |
| /orders/success/676 | user | 375px | 200 | PASS |
| /admin | admin | 1280px | 200 | PASS |
| /admin/customers | admin | 1280px | 200 | PASS |
| /admin/employees | admin | 1280px | 200 | PASS |
| /admin/products-crud | admin | 1280px | 200 | PASS |
| /admin/products-crud/view/950 | admin | 1280px | 200 | PASS |
| /admin/products-crud/edit/950 | admin | 1280px | 200 | PASS |
| /admin/variants | admin | 1280px | 200 | PASS |
| /admin/variants/create | admin | 1280px | 200 | PASS |
| /admin/variants/edit/602 | admin | 1280px | 200 | PASS |
| /admin/colors | admin | 1280px | 200 | PASS |
| /admin/colors?page=1 | admin | 1280px | 200 | PASS |
| /admin/colors/create | admin | 1280px | 200 | PASS |
| /admin/materials | admin | 1280px | 200 | PASS |
| /admin/categories | admin | 1280px | 200 | PASS |
| /admin/soles | admin | 1280px | 200 | PASS |
| /admin/inventory | admin | 1280px | 200 | PASS |
| /admin/inventory/adjust/602 | admin | 1280px | 200 | PASS |
| /admin/pos | admin | 1280px | 200 | PASS |
| /admin/orders/676 | admin | 1280px | 200 | PASS |
| /admin/invoices | admin | 1280px | 200 | PASS |
| /admin/invoices/290 | admin | 1280px | 200 | PASS |
| /admin/statistics | admin | 1280px | 200 | PASS |
| /admin/statistics/revenue | admin | 1280px | 200 | PASS |
| /admin/statistics/best-sellers | admin | 1280px | 200 | PASS |
| /admin | admin | 375px | 200 | PASS |
| /admin/customers | admin | 375px | 200 | PASS |
| /admin/employees | admin | 375px | 200 | PASS |
| /admin/products-crud | admin | 375px | 200 | PASS |
| /admin/products-crud/view/950 | admin | 375px | 200 | PASS |
| /admin/products-crud/edit/950 | admin | 375px | 200 | PASS |
| /admin/variants | admin | 375px | 200 | PASS |
| /admin/variants/create | admin | 375px | 200 | PASS |
| /admin/variants/edit/602 | admin | 375px | 200 | PASS |
| /admin/colors | admin | 375px | 200 | PASS |
| /admin/colors?page=1 | admin | 375px | 200 | PASS |
| /admin/colors/create | admin | 375px | 200 | PASS |
| /admin/materials | admin | 375px | 200 | PASS |
| /admin/categories | admin | 375px | 200 | PASS |
| /admin/soles | admin | 375px | 200 | PASS |
| /admin/inventory | admin | 375px | 200 | PASS |
| /admin/inventory/adjust/602 | admin | 375px | 200 | PASS |
| /admin/pos | admin | 375px | 200 | PASS |
| /admin/orders/676 | admin | 375px | 200 | PASS |
| /admin/invoices | admin | 375px | 200 | PASS |
| /admin/invoices/290 | admin | 375px | 200 | PASS |
| /admin/statistics | admin | 375px | 200 | PASS |
| /admin/statistics/revenue | admin | 375px | 200 | PASS |
| /admin/statistics/best-sellers | admin | 375px | 200 | PASS |

[Raw URL/forms/header/layout](evidence/giai-doan-5/urls.json). Lần chạy cuối: 41 ca Playwright PASS. [Kết quả cuối](evidence/giai-doan-5/browser-final.json); [các lần chạy, gồm FAIL trước sửa](evidence/giai-doan-5/browser-attempts.json). Các timeout selector/chế độ ADD=0/header bảng thu gọn là lỗi script test, đã chỉnh thao tác và chạy lại; lỗi JS POS và bản in là lỗi sản phẩm, ghi dưới đây. Không đưa tên/SĐT khách gốc từ bảng POS vào artifact crawl.

## Lỗi đã sửa

| Mã | Mô tả | File:dòng | Nguyên nhân/cách sửa | Bằng chứng chạy lại |
|---|---|---|---|---|
| GD5-01 | Khóa bộ ba không cho cùng kích cỡ khác màu/đế | database/migrations/013_mau_sac_bien_the.sql:39 | Bỏ khóa bộ 2/3, tạo khóa bộ năm; 005 không tạo lại khóa cũ | SQL 2601 và form trùng PASS |
| GD5-02 | 008 đổi mã màu/CCCD sang Unicode khi chạy lại bộ tổng hợp | database/migrations/008_nvarchar_con_lai.sql:18 | Giữ VARCHAR của mã ASCII theo 011/013; đồng bộ các bản gộp | Hai lần aggregate, converted_columns=0; metadata không đổi |
| GD5-03 | Script POS lỗi Identifier fields has already been declared | src/main/resources/static/js/ban-tai-quay.js:4 | Tên biến của bộ chọn trùng phần thanh toán; đổi thành variantFields | POS/hold/thanh toán hai viewport và crawl PASS |
| GD5-04 | Thêm 2 cột làm cột tiền bản in bị ép chiều ngang | src/main/resources/static/css/he-thong-thiet-ke.css:186 | CSS vẫn chia cho 9 cột; chia lại đủ 11 cột tổng 100% | Ảnh in cuối và đo độ rộng hai viewport PASS |

## Build và ảnh chụp

```text
COMMAND: .\mvnw.cmd -B -o clean test
[INFO] Tests run: 160, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

[Output theo từng lớp test](evidence/giai-doan-5/maven-clean-test.txt).

| Màn | 1280px | 375px |
|---|---|---|
| Chi tiết, tổ hợp | [Ảnh](evidence/giai-doan-5/gd5-detail-all-real-combinations-1280.png) | [Ảnh](evidence/giai-doan-5/gd5-detail-all-real-combinations-375.png) |
| Sản phẩm tạo kèm biến thể | [Ảnh](evidence/giai-doan-5/gd5-create-product-five-attributes-1280.png) | [Ảnh](evidence/giai-doan-5/gd5-create-product-five-attributes-375.png) |
| Bản in đủ cột | [Ảnh](evidence/giai-doan-5/gd5-invoice-print-final-1280.png) | [Ảnh](evidence/giai-doan-5/gd5-invoice-print-final-375.png) |

Kiểm tra tĩnh bổ sung từ skill giao diện: strict audit có 125 cảnh báo trên HEAD cũ và 130 trên bản mới; công cụ không tự nhận diện select được `combo-box.js` nâng cấp và nút gắn handler `data-clear-search` / `data-confirm` / `data-delete`. Năm cảnh báo phát sinh đã đối chiếu với primitive hiện có và browser (combo, ẩn/xóa, xóa tìm kiếm PASS), không coi chúng là bằng chứng lỗi runtime. Chưa đạt toàn bộ bộ luật tĩnh của công cụ trên cả repo; không ghi PASS cho strict audit. [Raw diff](evidence/giai-doan-5/premium-audit-diff.json).

## Lệnh triển khai thật — chỉ hướng dẫn, chưa thực thi

Sao lưu trước. Database đã có 004–012: chạy 013. Database chưa đầy đủ: dùng `database/migrations/chay_tat_ca.sql` mới (004–013), đã kiểm thử chạy lặp. `shopgiay.sql` đã đồng bộ 013; không dùng script khởi tạo để ghi đè DB đang có.

PowerShell, từ thư mục project, thay máy chủ và database cần triển khai:

```powershell
@"
EXEC sys.sp_set_session_context @key=N'shop.allow-production-migration', @value=1;
GO
:r database/migrations/013_mau_sac_bien_the.sql
"@ | sqlcmd -S <MAY_CHU_SQL> -d <DATABASE_TRIEN_KHAI> -E -C -I -b -f 65001
```

Không đổi luật kho, doanh thu hoặc chuyển trạng thái/hủy đơn; chỉ bổ sung thuộc tính và snapshot. Không merge, không push; dừng sau commit Giai đoạn 5.
