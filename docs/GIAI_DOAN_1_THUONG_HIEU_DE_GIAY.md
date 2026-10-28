# Giai đoạn 1 — Menu, thương hiệu và đế giày

Ngày kiểm tra: 28/10/2026. Nhánh `gop-y-giang-vien`. Chỉ thực hiện giai đoạn này; không merge/push.

Tổng bảng nghiệm thu: **17 PASS / 0 FAIL / 1 NOT TESTED**. Playwright có **56 ca theo mã/kích thước, đều PASS** ở lần chạy cuối của mỗi ca. Lịch sử lần đầu và lần chạy lại giữ nguyên trong `browser-attempts.json`; không xóa bằng chứng thất bại.

| Hạng mục | Cách kiểm tra | Kết quả thực tế | PASS/FAIL/NOT TESTED |
|---|---|---|---|
| Cách ly database | RESTORE file backup sang tên mới, không WITH REPLACE ở lần tạo | shopbangiay_kt_20261028_gd1; app cổng 18110; không kết nối shopbangiay/shopbangiay_thu | PASS |
| Menu và hồ sơ Admin | Click sidebar 1280/375; đọc h1, breadcrumb, topbar | Nhóm TÀI KHOẢN; Hồ sơ của tôi; Thương hiệu và Đế giày trong menu con Sản phẩm; chỉ một mục active | PASS |
| Đổi nhãn toàn ứng dụng | rg templates/JS/Java; README/docs; crawl nhãn | Quét trước có kết quả; sau không còn kết quả (rg exit 1). Giữ Category, categories và route cũ | PASS |
| Migration 010 và bộ gộp | sqlcmd -b -I; chạy chay_tat_ca.sql hai lần | Cả hai exit 0; 21 bảng/152 cột sau migration; COUNT và metadata lần hai giống lần một | PASS |
| NVARCHAR và duy nhất tên | Đọc sys.columns; INSERT tên khác hoa/thường; SELECT Unicode | ten NVARCHAR(100), collation Latin1_General_100_CI_AS; SQL 2601 chặn cAO SU; chuỗi Nguyễn Huệ đọc nguyên vẹn | PASS |
| Khởi tạo schema mới | Chỉ làm rỗng bản sao dùng một lần rồi chạy shopgiay.sql; validate | Bootstrap exit 0; seed 8 đế; ứng dụng Started, không lỗi validate; sau đó RESTORE checkpoint | PASS |
| CRUD đế giày | Form thật: thêm/sửa, trùng tên, ẩn/hiện, xóa | Hai kích thước đều đạt; kiểm tra lại CRUD sau thay đổi cuối | PASS |
| Đang dùng / ẩn / xóa | 13 sản phẩm/đế, click số dùng; giả mạo POST có CSRF | Hiện đúng 13; link giữ soleId; cảnh báo 13 sản phẩm; xóa bị chặn; gán đế ẩn cho sản phẩm mới bị chặn | PASS |
| Thêm/sửa sản phẩm có đế | Form thật multipart + SELECT products/product_variants/inventory_logs | Desktop sản phẩm 949, mobile 963: chọn đế riêng → EVA (2) → đế riêng. Một biến thể giá 456789, tồn 4, log Khởi tạo | PASS |
| Đế tùy chọn và dữ liệu cũ | Form thật tạo sản phẩm trống đế; sửa giữ đế đã ẩn | Lưu NULL được; combo mới không chứa đế ẩn; sản phẩm cũ vẫn hiện tên và giữ được lựa chọn khi sửa | PASS |
| Cột/lọc/phân trang Admin | Form lọc soleId + SELECT COUNT + click trang Sau | 13 sản phẩm đúng đế; trang đầu 12, trang hai 1, giữ soleId | PASS |
| Chi tiết và lọc User | Xem sản phẩm, chọn bộ lọc, click trang Sau | Tên đế đúng dữ liệu thật kể cả sau ẩn; lọc 13 sản phẩm, trang 12/1, giữ soleId | PASS |
| Phân trang Đế giày | 13 đế (fixture được ghi rõ là SQL), click Sau | Trang đầu 12, trang hai 1; page -1/abc/99999 không 500 | PASS |
| 8 kịch bản bảo mật | Ma trận quyền, CSRF, quyền sở hữu; SELECT trước/sau | S01–S08 đều PASS; bảng chi tiết phía dưới | PASS |
| Crawl / JS / responsive | 46 URL–vai trò mỗi kích thước; console, response, scrollWidth | 92 lượt: đúng trạng thái/redirect; 0 lỗi JS, 0 request tài nguyên 404, 0 trang trắng, không tràn ngang | PASS |
| Maven không bỏ test | mvnw.cmd clean test trên cây nguồn commit, datasource bản sao, ddl-auto=validate | Tests run: 132, Failures: 0, Errors: 0, Skipped: 0; BUILD SUCCESS | PASS |
| Khôi phục dữ liệu thử | RESTORE checkpoint sau từng nhóm; so COUNT cuối và dọn ảnh sở hữu | 21 bảng khớp checkpoint; biến thể 596 tồn 18; đơn 669 PENDING; đã xóa 2 upload thử không còn tham chiếu | PASS |
| Triển khai database thật | Chỉ cung cấp lệnh để người quản trị chạy | Không chạy migration hoặc test trên database thật | NOT TESTED |

## Bằng chứng thô

[Thư mục bằng chứng](evidence/gd1/): JSON SQL, toàn bộ output Maven, log khởi động/RESTORE/migration, status HTTP và ảnh. Cookie, session, mật khẩu và thông tin liên hệ của khách gốc không đưa vào bằng chứng commit.

```text
Tests run: 132, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Bộ test gồm 4 test tích hợp mới trong `SoleIntegrationTest`: gán/đổi/xóa đế và giữ đế ẩn; Unicode/duy nhất tên tại service và DB; chặn xóa đế đang dùng và lọc hai phía với trang 12; nhãn/route/quyền/CSRF.

[Quét trước](evidence/gd1/labels-before.txt) và [quét sau](evidence/gd1/labels-after.txt): quét sau không có dòng nào, exit 1. Không đổi tên bảng, lớp, enum hoặc route hiện có.

```sql
SELECT p.id,p.code,p.de_giay_id,s.ten,s.trang_thai
FROM products p LEFT JOIN de_giay s ON s.id=p.de_giay_id
WHERE p.code IN ('GD1-UI-1280','GD1-UI-375');
SELECT id,product_id,size_id,chat_lieu_id,price,quantity FROM product_variants WHERE product_id IN (949,963);
SELECT l.* FROM inventory_logs l JOIN product_variants v ON v.id=l.variant_id WHERE v.product_id IN (949,963);
```

[SELECT từng bước](evidence/gd1/product-sql.json), gồm kết quả trước/sau sửa và sau ẩn:

| Màn hình | Sản phẩm | Đế lúc tạo | Sau sửa | Sau đổi lại/ẩn | Giá / tồn / log |
|---|---|---|---|---|---|
| 1280 | 949 | 9 | 2 — EVA | 9, trang_thai=false | 456789 / 4 / Khởi tạo +4 |
| 375 | 963 | 11 | 2 — EVA | 11, trang_thai=false | 456789 / 4 / Khởi tạo +4 |

Các sản phẩm kiểm tra phân trang được chèn bằng SQL trên bản sao (12 sản phẩm bổ sung mỗi kích thước). Hai sản phẩm trên được tạo qua form trình duyệt thật, có upload ảnh và biến thể ban đầu. Tất cả dữ liệu thử đã bị loại bằng RESTORE, nên các ID thử không còn sau nghiệm thu.

## COUNT và schema trước/sau

SELECT COUNT_BIG(*) được thực hiện với mọi bảng trong sys.tables. Hai lần chạy bộ migration có kết quả hoàn toàn giống nhau. Bảng sau là dữ liệu cuối sau RESTORE checkpoint schema 010:

| Bảng | Trước 010 | Sau migration lần 1 và 2 | Cuối sau khôi phục |
|---|---:|---:|---:|
| categories | 6 | 6 | 6 |
| chat_lieu | 8 | 8 | 8 |
| customers | 14 | 14 | 14 |
| de_giay | chưa có bảng | 8 | 8 |
| dia_chi_nhan_hang | 7 | 7 | 7 |
| employees | 5 | 5 | 5 |
| inventory_logs | 63 | 63 | 63 |
| invoices | 28 | 28 | 28 |
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

[Metadata và COUNT lần 1](evidence/gd1/after-010-first.json), [lần 2](evidence/gd1/after-010-second.json), [cuối](evidence/gd1/final-restored.json), [xác nhận dọn dữ liệu](evidence/gd1/cleanup-proof.json). FK không bị disable hoặc untrusted; ten có unique index phân biệt dấu nhưng không phân biệt hoa/thường. [Output SQL 2601 và metadata](evidence/gd1/db-constraints.log).

Trước migration, SchemaCheck báo thiếu `dbo.de_giay` và `products.de_giay_id`, hướng dẫn 010; xem [log validate trước](evidence/gd1/validate-before.log). Sau migration và cả khi khởi tạo schema rỗng, `ddl-auto=validate` sạch, log Started có trong [log bootstrap](evidence/gd1/bootstrap-validate.log) và [log ứng dụng cuối](evidence/gd1/last-ui-app.log).

## URL, vai trò và status

Bảng lấy từ Playwright thực, cả 1280 và 375px. Với redirect, ghi status đầu và URL cuối. Sản phẩm SP001 đang ngừng bán trong bản sao nên trả 404 đúng; lượt crawl chạy lại sử dụng SP004 đang bán từ SELECT, không sửa dữ liệu gốc để ép trang trả 200.

| URL | Vai trò | 1280px | 375px | Kết quả |
|---|---|---|---|---|
| `/` | guest | 200 | 200 | PASS |
| `/products` | guest | 200 | 200 | PASS |
| `/products?promotion=true` | guest | 200 | 200 | PASS |
| `/category/1` | guest | 200 | 200 | PASS |
| `/products/4` | guest | 200 | 200 | PASS |
| `/cart` | guest | 200 | 200 | PASS |
| `/login` | guest | 200 | 200 | PASS |
| `/register` | guest | 200 | 200 | PASS |
| `/policy` | guest | 200 | 200 | PASS |
| `/` | user | 200 | 200 | PASS |
| `/products` | user | 200 | 200 | PASS |
| `/products?promotion=true` | user | 200 | 200 | PASS |
| `/category/1` | user | 200 | 200 | PASS |
| `/products/4` | user | 200 | 200 | PASS |
| `/cart` | user | 200 | 200 | PASS |
| `/login` | user | 200 | 200 | PASS |
| `/register` | user | 200 | 200 | PASS |
| `/policy` | user | 200 | 200 | PASS |
| `/profile` | user | 200 | 200 | PASS |
| `/orders` | user | 200 | 200 | PASS |
| `/checkout` | user | 302 → /cart (200) | 302 → /cart (200) | PASS |
| `/admin/dashboard` | admin | 200 | 200 | PASS |
| `/admin/products-crud` | admin | 200 | 200 | PASS |
| `/admin/products-crud/create` | admin | 200 | 200 | PASS |
| `/admin/products-crud/view/1` | admin | 200 | 200 | PASS |
| `/admin/products-crud/edit/1` | admin | 200 | 200 | PASS |
| `/admin/categories` | admin | 200 | 200 | PASS |
| `/admin/categories/create` | admin | 200 | 200 | PASS |
| `/admin/categories/edit/1` | admin | 200 | 200 | PASS |
| `/admin/sizes` | admin | 200 | 200 | PASS |
| `/admin/materials` | admin | 200 | 200 | PASS |
| `/admin/soles` | admin | 200 | 200 | PASS |
| `/admin/soles/create` | admin | 200 | 200 | PASS |
| `/admin/soles/edit/1` | admin | 200 | 200 | PASS |
| `/admin/variants` | admin | 200 | 200 | PASS |
| `/admin/inventory` | admin | 200 | 200 | PASS |
| `/admin/customers` | admin | 200 | 200 | PASS |
| `/admin/employees` | admin | 200 | 200 | PASS |
| `/admin/vouchers` | admin | 200 | 200 | PASS |
| `/admin/promotions` | admin | 200 | 200 | PASS |
| `/admin/orders` | admin | 200 | 200 | PASS |
| `/admin/invoices` | admin | 200 | 200 | PASS |
| `/admin/pos` | admin | 200 | 200 | PASS |
| `/admin/statistics/revenue` | admin | 200 | 200 | PASS |
| `/admin/statistics/best-sellers` | admin | 200 | 200 | PASS |
| `/admin/account` | admin | 200 | 200 | PASS |

## Bảo mật

| Mã | Kiểm tra thực | Kết quả |
|---|---|---|
| S01 | Khách vào Admin, hồ sơ, đơn | PASS — HTTP [302] |
| S02 | User vào Admin | PASS — HTTP [403] |
| S03 | Admin vào trang thuộc quyền | PASS — HTTP [200] |
| S04 | Thiếu CSRF: save/toggle/delete đế và save sản phẩm | PASS — HTTP [403] |
| S05 | CSRF sai: bốn POST trên | PASS — HTTP [403] |
| S06 | User gửi ba POST đế với CSRF hợp lệ | PASS — HTTP [403] |
| S07 | User đọc/hủy đơn người khác | PASS — HTTP [403] |
| S08 | User đọc/sửa/xóa/đặt mặc định địa chỉ người khác | PASS — HTTP [404] |

SELECT trước/sau xác nhận chủ sở hữu đơn/địa chỉ và nội dung địa chỉ không đổi; raw đầy đủ được giữ cục bộ trong verify/gd1, không xuất thông tin liên hệ của khách gốc lên Git. Status từng yêu cầu nằm trong browser-final.json.

## Các lỗi phát hiện và chạy lại

| Mã | File:dòng | Nguyên nhân / sửa | Bằng chứng chạy lại |
|---|---|---|---|
| G1-01 | templates/admin/danh-muc-kich-co/danh-sach.html:73 | Trong lúc chỉnh template đã lặp th:attr gây 500; gộp data-object và data-consequence vào một thuộc tính | template-failure.log giữ Caused by; crawl categories/soles và CRUD cuối đều 200 |
| G1-02 | templates/fragments/thanh-ben-quan-tri.html:44 | Điều kiện menu tồn kho bị mở rộng nhầm sang đế; trả lại điều kiện inventory riêng | menu/final-menu hai kích thước xác nhận đúng 1 aria-current |
| G1-03 | templates/admin/ban-tai-quay/ban-tai-quay.html; static/js/ban-tai-quay.js:14 | Cây thử ban đầu lấy nhầm template POS đang chỉnh dở ngoài phạm vi, không khớp JS của commit; loại phần dở khỏi cây commit, POS chỉ đổi nhãn | crawl Admin cuối cả hai kích thước: 200, errors=[], consoleErrors=[] |

Các lần thất bại do bộ kiểm tra dùng ID ngừng bán, đọc CSRF từ thẻ meta thay vì input, so thông báo bằng chuỗi khác, hoặc đọc menu trước khi animation kết thúc được giữ trong lịch sử; đã chỉnh bộ kiểm tra và chạy lại. Không sửa nghiệp vụ để làm các ca này đạt.

Kiểm tra tĩnh UI strict: HEAD trước giai đoạn có 85 phát hiện (20 thiếu khai báo quyền sở hữu trong manifest và 65 phát hiện quy tắc); cây commit cuối vẫn 85, không tăng. Bộ quét chưa hiểu đầy đủ Thymeleaf/JS gắn sự kiện. Đây không phải tuyên bố toàn repository sạch kiểm tra tĩnh; các vấn đề cũ ngoài giai đoạn không được refactor. Hai JSON audit được lưu kèm để đối chiếu.

## Ảnh và giới hạn kiểm tra

[Menu desktop](evidence/gd1/gd1-final-menu-viewport-1280.png), [menu mobile](evidence/gd1/gd1-final-menu-viewport-375.png), [form mobile](evidence/gd1/gd1-brand-copy-375.png), [lọc User](evidence/gd1/gd1-product-filters-375.png), [giữ đế đã ẩn](evidence/gd1/gd1-hide-used-375.png). Chỉ kiểm tra Chromium ở hai viewport; trình duyệt/thiết bị thật khác NOT TESTED. Không kiểm thử lại toàn bộ thanh toán/kho vì giai đoạn này không đổi các nghiệp vụ đó.

## Migration cần chạy khi triển khai — chưa thực hiện trên DB thật

Database đã đủ 004–009: chỉ chạy `database/migrations/010_de_giay.sql`. Nếu thiếu bản cũ, thứ tự 004 → 005 → 006 → 007 → 008 → 009 → 010; bộ `chay_tat_ca.sql` đã gộp đúng thứ tự. `shopgiay.sql` dành cho database rỗng, không chạy vào database đã có schema.

Lệnh dưới đây chỉ là hướng dẫn cho người quản trị, **chưa chạy**. Sao lưu trước, chọn đúng database đích. SESSION_CONTEXT chỉ mở guard cho phiên triển khai chủ động; không bỏ guard trong file migration.

```powershell
$OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$migrationPath = 'database/migrations/010_de_giay.sql'
# Nếu cần toàn bộ bản nâng cấp: thay bằng database/migrations/chay_tat_ca.sql
$migrationSql = "EXEC sys.sp_set_session_context @key=N'shop.allow-production-migration', @value=1;`nGO`n" + (Get-Content -LiteralPath $migrationPath -Raw -Encoding UTF8)
$migrationSql | sqlcmd -S 'MAY_CHU_SQL,1433' -E -C -I -b -f 65001 -d 'DATABASE_DICH'
if ($LASTEXITCODE -ne 0) { throw "Migration thất bại" }
```

Sau đó đặt `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`, khởi động và chờ Started. Tài khoản SQL triển khai dùng cơ chế xác thực của môi trường, không đưa mật khẩu vào repository.

Không kết nối hoặc chạy gì trên `shopbangiay` và `shopbangiay_thu` trong giai đoạn này. Tên logical file cũ xuất hiện trong log RESTORE là metadata trong file backup; database đích luôn là bản sao mới. App kiểm thử đã dừng. DỪNG sau giai đoạn 1.
