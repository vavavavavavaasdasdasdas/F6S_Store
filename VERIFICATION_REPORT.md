# Giai đoạn A — Hoàn thiện chất liệu

Ngày kiểm tra: 24/10/2026 (Asia/Bangkok). Nhánh `hoan-thien-chat-lieu`, tạo từ nhánh đang mở `don-dep` vì nhánh yêu cầu chưa tồn tại. Database duy nhất được dùng: `shopbangiay_thu`, bản sao được người dùng cho phép. Port kiểm thử: **18081**. Không kết nối hay chạy lệnh vào `shopbangiay`.

Kết quả nghiệm thu chức năng chất liệu: **PASS**. Dừng sau Giai đoạn A.

Workspace ban đầu có nhiều sửa/xóa và sáu tệp đã stage từ công việc trước. Không reset, stash, xóa hoặc ghi đè các thay đổi đó. Commit Giai đoạn A bao gồm phần chất liệu mới và các phần hiển thị chất liệu đã có trong workspace cần thiết để các luồng trong commit hoạt động; các thay đổi dọn dẹp/chức năng khác vẫn được giữ ngoài commit. Các thay đổi không liên quan trong controller danh mục, JavaScript POS và các tệp dùng chung được giữ ngoài commit bằng index tạm; bản làm việc hiện tại vẫn giữ chúng. Cây code xuất từ index tạm được chạy clean test riêng.

## Thay đổi và nghiệm thu

| Hạng mục | Cách kiểm tra | Kết quả thực tế | PASS/FAIL/NOT TESTED |
|---|---|---|---|
| Biến thể ban đầu tùy chọn | Playwright đăng nhập, nhập form thật, chọn 39 + 40 và Vải canvas, upload PNG fixture, lưu; SELECT products/variants/log | Product 610; variant 645, 646; đúng hai size, giá 321000.50, mỗi size khởi tạo 7 | PASS |
| Thiếu chất liệu/giá | Click lưu trên form thật, kiểm tra aria-invalid và lỗi cạnh ô; MockMvc gửi thiếu cả hai | Lỗi cạnh combo, focus về chất liệu; HTTP 200 có field errors initialMaterialId/initialPrice; SQL sản phẩm bị chặn = 0 | PASS |
| Không chọn size | InitialMaterialIntegrationTest.emptySizesKeepProductOnlyAndZeroStockStillWritesInitialization | Tạo sản phẩm thành công, biến thể rỗng | PASS |
| Transaction và log | rollbackUndoesProductVariantsAndLogsTogether; formPostCreatesTwoVariantsAndExactInitializationLogs | Rollback xóa đồng thời product/variants/log; log trước/sau không đổi. Tồn 0 vẫn có log Khởi tạo 0→0 | PASS |
| Form sửa, đi tới quản lý variant | GET edit/610 và create?productId=610; test kiểm tra model | Có nhãn Vải canvas, link Quản lý variant; productId được chọn sẵn | PASS |
| Cột/lọc chất liệu admin | GET products-crud?materialId=4&keyword=VERIFY-A; SELECT DISTINCT material; test +n | HTTP 200, Vải canvas; chỉ các variant status=1. Bốn chất liệu đang bán rút gọn ba tên +1; variant ngừng bán không góp tên | PASS |
| Giữ lọc qua phân trang | Tạo 14 product trong transaction rollback, GET danh sách có materialId | 14 kết quả, trang tiếp có page=1 và materialId=7; không trùng dòng do dùng EXISTS | PASS |
| Thẻ User | GET /, /products, /category/1, /products?promotion=true | Cả bốn HTTP 200; HTML có dòng Chất liệu từ VariantDto, không trùng tên | PASS |
| Chi tiết và bộ lọc User | GET /products/610 và /products?materialId=4; SELECT variants; test lọc hiện có | HTTP 200; combo và thông tin chất liệu thật; cả thẻ sản phẩm liên quan có dòng chất liệu | PASS |
| Đang dùng và liên kết | GET /admin/materials, bấm/liên kết /admin/variants?materialId=4; SELECT COUNT(*) | 18 biến thể Vải canvas, link mở đúng danh sách đã lọc | PASS |
| Ẩn đang dùng | Playwright mở cảnh báo; curl POST có CSRF toggle-status/4, đọc combo và đơn/hóa đơn; SELECT trước/sau | Cảnh báo 18 biến thể. Status 1→0; combo tạo product/variant không có Vải canvas. Variant/đơn/hóa đơn cũ vẫn có tên; đã khôi phục status=1 | PASS |
| Sửa variant dùng chất liệu ẩn | hiddenMaterialIsAbsentForCreationButExistingVariantCanBeEdited | Combo sửa giữ chất liệu hiện tại; đổi giá/lưu vẫn được, không cho tạo mới bằng chất liệu ẩn | PASS |
| Xóa | POST delete/4 và SELECT tồn tại; test xóa chất liệu chưa dùng | Đang dùng bị chặn (HTTP 302 + thông báo, COUNT=1); chưa dùng xóa được | PASS |
| Tiếng Việt/NVARCHAR | SELECT metadata; test UnicodePersistenceIntegrationTest; render form/PDF | Tên có dấu; metadata xác nhận chat_lieu.ten=nvarchar(100), order_details.ten_chat_lieu=nvarchar(100), inventory_logs.reason=nvarchar(500) | PASS |
| CSRF | Mọi POST nghiệm thu lấy token từ form/cookie jar; các POST thiếu/sai token bên dưới | Thiếu/sai CSRF trả 403; các form Thymeleaf dùng th:action, token do Spring Security chèn | PASS |
| Build/test cuối | .\mvnw.cmd clean test, không skip; URL datasource cố định bản sao | Tests run: 109, Failures: 0, Errors: 0, Skipped: 0; BUILD SUCCESS | PASS |
| Crawl User/Admin chính | 33 URL trong bảng sau, curl.exe có phiên phù hợp | 33/33 HTTP 200 | PASS |
| Mobile và bản in | Playwright 390×844; export PDF, trích text và render PNG để xem | Form không tràn ngang (scrollWidth=375, innerWidth=390); PDF 1 trang có Vải canvas, cột không bị cắt | PASS |
| Audit UI toàn project (bổ sung, ngoài tiêu chí nghiệm thu chất liệu) | audit_project.py . --mode strict; đối chiếu snapshot trước sửa | Baseline 89 cảnh báo lỗi, cuối 90. Thêm một cảnh báo static actionless-button ở nút data-confirm; thực tế listener chung mở dialog đã được kiểm tra. Các mục còn lại là tồn tại trước giai đoạn | FAIL |

## Bằng chứng SQL và form thật

Ảnh tạo hai size: [phase-a-created-two-sizes.png](docs/evidence/phase-a/phase-a-created-two-sizes.png). Lỗi cạnh ô ở viewport 390px: [phase-a-missing-material.png](docs/evidence/phase-a/phase-a-missing-material.png).

Lệnh đọc trước tạo (kết nối bắt buộc chọn database kiểm thử):

```powershell
sqlcmd -S tcp:localhost,1433 -U sa -C -d shopbangiay_thu -b -Q "SELECT DB_NAME() AS db; SELECT id,code FROM products WHERE code=N'VERIFY-A-20261024';"
```

Output trước tạo:

```text
DB_NAME() = shopbangiay_thu
SELECT products WHERE code=N'VERIFY-A-20261024': (0 rows affected)
```

Playwright thực thi lần lượt thao tác thật với textbox, combobox, checkbox, file chooser và nút Lưu sản phẩm. Sau lưu chuyển tới `/admin/products-crud/view/610`. SELECT ngay sau tạo:

```text
id  code              variant_id size material    price     quantity before change after reason
610 VERIFY-A-20261024  645        39   Vải canvas  321000.50 7        0      7      7     Khởi tạo
610 VERIFY-A-20261024  646        40   Vải canvas  321000.50 7        0      7      7     Khởi tạo
(2 rows affected)
```

Tệp SQL ngay sau tạo có số đúng nhưng console sqlcmd lúc đầu dùng code page cũ nên một số chữ thành dấu `?`; kiểm tra cuối dùng `-f 65001` và so sánh `l.reason=N'Khởi tạo'` đã xác nhận Unicode thực trong database. Output SQL thô cuối (tồn 645 còn 5 vì đã bán hai đơn POS khi chạy nghiệm thu, log khởi tạo vẫn là 7):

```text
﻿test_database
-------------
shopbangiay_thu

(1 rows affected)
code id size_value material price current_quantity quantity_before quantity_change quantity_after reason
---- -- ---------- -------- ----- ---------------- --------------- --------------- -------------- ------
VERIFY-A-20261024 645 39 Vải canvas 321000.50 5 0 7 7 Khởi tạo
VERIFY-A-20261024 646 40 Vải canvas 321000.50 7 0 7 7 Khởi tạo

(2 rows affected)
id ten trang_thai used_by
-- --- ---------- -------
4 Vải canvas 1 18

(1 rows affected)
order_id ten_chat_lieu
-------- -------------
513 Vải canvas
514 Vải canvas

(2 rows affected)
blocked_products
----------------
0

(1 rows affected)
```

Câu SELECT đối chiếu:

```sql
SELECT p.code,v.id,s.size_value,m.ten,v.price,v.quantity,
       l.quantity_before,l.quantity_change,l.quantity_after,l.reason
FROM products p JOIN product_variants v ON v.product_id=p.id
JOIN sizes s ON s.id=v.size_id JOIN chat_lieu m ON m.id=v.chat_lieu_id
JOIN inventory_logs l ON l.variant_id=v.id
WHERE p.code=N'VERIFY-A-20261024' AND l.reason=N'Khởi tạo';
SELECT m.id,m.ten,m.trang_thai,COUNT(v.id) AS used_by
FROM chat_lieu m LEFT JOIN product_variants v ON v.chat_lieu_id=m.id
WHERE m.id=4 GROUP BY m.id,m.ten,m.trang_thai;
SELECT order_id,ten_chat_lieu FROM order_details WHERE order_id IN(513,514);
```

Ẩn và giữ lịch sử — trích output thực từ phase-a-live.log:

```text
BEFORE_HIDE: id=4, ten=Vải canvas, trang_thai=1; COUNT(product_variants)=18
POST /admin/materials/toggle-status/4 -> 302
GET /admin/products-crud/create -> 200, không có option Vải canvas
GET /admin/variants/create -> 200, không có option Vải canvas
GET /admin/products-crud/view/610 -> 200, material_visible=true
GET /orders/513 -> 200, material_visible=true
GET /admin/orders/513 -> 200, material_visible=true
GET /admin/invoices/448 -> 200, material_visible=true
AFTER_HIDE: id=4, ten=Vải canvas, trang_thai=0
order_details: Vải canvas; Vải canvas (2 rows affected)
POST /admin/materials/delete/4 -> 302; SELECT COUNT(*) WHERE id=4 -> 1
POST /admin/materials/toggle-status/4 -> 302 (khôi phục hiển thị)
```

Log đầy đủ: [phase-a-live.log](docs/evidence/phase-a/phase-a-live.log), [SQL trước](docs/evidence/phase-a/phase-a-sql-before.txt), [SQL ngay sau](docs/evidence/phase-a/phase-a-sql-after.txt), [SQL cuối UTF-8](docs/evidence/phase-a/phase-a-sql-final.txt). Cookies/CSRF không đưa vào Git.

## Nơi hiển thị chất liệu

| Hạng mục | Cách kiểm tra | Kết quả thực tế | PASS/FAIL/NOT TESTED |
|---|---|---|---|
| Trang chủ | GET /, kiểm tra nội dung card | 200, dòng Chất liệu | PASS |
| Danh sách /products | GET /products | 200, dòng Chất liệu | PASS |
| Danh mục | GET /category/1 | 200, dòng Chất liệu | PASS |
| Khuyến mãi | GET /products?promotion=true | 200, card có Chất liệu | PASS |
| Chi tiết sản phẩm | GET /products/610, Playwright desktop/mobile | 200; Vải canvas từ variants, bỏ placeholder chất liệu | PASS |
| Sản phẩm liên quan | GET /products/610 sau build cuối | 200; các card có Da thật/Da tổng hợp/Vải lưới từ dữ liệu | PASS |
| Giỏ | POST /cart/add, GET /cart bằng phiên user | 302 rồi 200; Vải canvas | PASS |
| Checkout | GET /checkout với giỏ có hàng | 200; Vải canvas | PASS |
| Thành công | POST checkout/place, GET /orders/success/513 | 302 rồi 200; Vải canvas | PASS |
| Chi tiết đơn User | GET /orders/513 trước và sau ẩn | 200; snapshot Vải canvas giữ nguyên | PASS |
| Chi tiết đơn Admin | GET /admin/orders/513 trước và sau ẩn | 200; snapshot Vải canvas giữ nguyên | PASS |
| Hóa đơn | GET /admin/invoices/448 trước và sau ẩn | 200; snapshot Vải canvas giữ nguyên | PASS |
| Bản in hóa đơn | Playwright PDF, PdfReader extract và pdftoppm render | 1 trang; tên Vải canvas, cột chất liệu rõ | PASS |
| POS | POST /admin/pos/add, GET /admin/pos | 302 rồi 200; Vải canvas trên giỏ và lựa chọn | PASS |
| Hóa đơn chờ | POST holds, GET POS, POST open/review | 302/200; tên trước khi mở và sau khi mở đúng | PASS |
| Tồn kho | GET /admin/inventory?variantId=645 | 200; Vải canvas | PASS |
| Điều chỉnh tồn | GET /admin/inventory/adjust/645 | 200; Size 39 · Vải canvas | PASS |
| Lịch sử kho | GET /admin/inventory?variantId=645 | 200; Vải canvas cạnh size, log Khởi tạo | PASS |
| Dashboard sắp hết hàng | GET /admin/dashboard khi variant 645 còn 5 | 200; cột Chất liệu có Vải canvas | PASS |
| Xem sản phẩm Admin | GET /admin/products-crud/view/610 | 200; hai dòng size/chất liệu | PASS |

Ảnh: [cột/lọc Admin](docs/evidence/phase-a/phase-a-admin-filter.png), [số đang dùng](docs/evidence/phase-a/phase-a-material-usage.png), [cảnh báo ẩn](docs/evidence/phase-a/phase-a-hide-warning.png), [lọc User](docs/evidence/phase-a/phase-a-user-filter.png), [chi tiết mobile](docs/evidence/phase-a/phase-a-product-mobile.png), [hóa đơn](docs/evidence/phase-a/phase-a-invoice.png), [PDF in](docs/evidence/phase-a/phase-a-invoice-print.pdf), [PNG bản in](docs/evidence/phase-a/phase-a-invoice-print.png).

## Tám kịch bản bảo mật

Đã chạy cả qua Spring Security MockMvc lẫn HTTP thật trên bản sao. HTTP 200 ở trường hợp nhập sai là render lại form có lỗi, không phải lưu thành công; SQL xác nhận không tạo product.

| Hạng mục | Cách kiểm tra | Kết quả thực tế | PASS/FAIL/NOT TESTED |
|---|---|---|---|
| 1. Khách chưa đăng nhập vào Admin | GET /admin/materials, không cookie đăng nhập | 302 → /login | PASS |
| 2. USER truy cập Admin | GET /admin/products-crud với cookie USER | 403 | PASS |
| 3. POST thiếu CSRF | POST /admin/products-crud/save bằng admin, không token | 403 | PASS |
| 4. Token CSRF sai | POST toggle-status/4, token invalid | 403, không đổi status | PASS |
| 5. USER tạo variant | POST /admin/variants/save, token hợp lệ của USER | 403 | PASS |
| 6. Giả mạo size ID | POST save với initialSizeIds=-1 | 200 + field error initialSizeIds, không có sản phẩm | PASS |
| 7. Giá/tồn âm | POST save initialPrice=-1, initialQuantity=-1 | 200 + hai field errors; không có sản phẩm | PASS |
| 8. SQL injection vào lọc/sort | keyword=%' OR 1=1--; sort=p.id;DROP TABLE products | 200, không tìm thấy sản phẩm; bảng vẫn tồn tại | PASS |

Kiểm tra bổ sung: Admin đọc đơn User 513 qua /orders/513 nhận **403** (quyền sở hữu vẫn được giữ). Chất liệu ẩn bị service từ chối khi tạo variant mới; chất liệu đang dùng bị chặn xóa.

## Crawl URL chính

| Hạng mục | Cách kiểm tra | Kết quả thực tế | PASS/FAIL/NOT TESTED |
|---|---|---|---|
| `/login` | curl.exe + cookie jar trên localhost:18081 | HTTP 200 | PASS |
| `/` | curl.exe + cookie jar trên localhost:18081 | HTTP 200 | PASS |
| `/products` | curl.exe + cookie jar trên localhost:18081 | HTTP 200 | PASS |
| `/products?promotion=true` | curl.exe + cookie jar trên localhost:18081 | HTTP 200 | PASS |
| `/category/1` | curl.exe + cookie jar trên localhost:18081 | HTTP 200 | PASS |
| `/products/610` | curl.exe + cookie jar trên localhost:18081 | HTTP 200 | PASS |
| `/products?materialId=4` | curl.exe + cookie jar trên localhost:18081 | HTTP 200 | PASS |
| `/cart` | curl.exe + cookie jar trên localhost:18081 | HTTP 200 | PASS |
| `/login` | curl.exe + cookie jar trên localhost:18081 | HTTP 200 | PASS |
| `/register` | curl.exe + cookie jar trên localhost:18081 | HTTP 200 | PASS |
| `/policy` | curl.exe + cookie jar trên localhost:18081 | HTTP 200 | PASS |
| `/admin` | curl.exe + cookie jar trên localhost:18081 | HTTP 200 | PASS |
| `/admin/dashboard` | curl.exe + cookie jar trên localhost:18081 | HTTP 200 | PASS |
| `/admin/products-crud` | curl.exe + cookie jar trên localhost:18081 | HTTP 200 | PASS |
| `/admin/products-crud/create` | curl.exe + cookie jar trên localhost:18081 | HTTP 200 | PASS |
| `/admin/products-crud/edit/610` | curl.exe + cookie jar trên localhost:18081 | HTTP 200 | PASS |
| `/admin/products-crud/view/610` | curl.exe + cookie jar trên localhost:18081 | HTTP 200 | PASS |
| `/admin/products-crud/610/variants` | curl.exe + cookie jar trên localhost:18081 | HTTP 200 | PASS |
| `/admin/variants` | curl.exe + cookie jar trên localhost:18081 | HTTP 200 | PASS |
| `/admin/variants/create` | curl.exe + cookie jar trên localhost:18081 | HTTP 200 | PASS |
| `/admin/materials` | curl.exe + cookie jar trên localhost:18081 | HTTP 200 | PASS |
| `/admin/categories` | curl.exe + cookie jar trên localhost:18081 | HTTP 200 | PASS |
| `/admin/sizes` | curl.exe + cookie jar trên localhost:18081 | HTTP 200 | PASS |
| `/admin/inventory` | curl.exe + cookie jar trên localhost:18081 | HTTP 200 | PASS |
| `/admin/inventory/adjust/645` | curl.exe + cookie jar trên localhost:18081 | HTTP 200 | PASS |
| `/admin/pos` | curl.exe + cookie jar trên localhost:18081 | HTTP 200 | PASS |
| `/admin/orders` | curl.exe + cookie jar trên localhost:18081 | HTTP 200 | PASS |
| `/admin/invoices` | curl.exe + cookie jar trên localhost:18081 | HTTP 200 | PASS |
| `/admin/customers` | curl.exe + cookie jar trên localhost:18081 | HTTP 200 | PASS |
| `/admin/employees` | curl.exe + cookie jar trên localhost:18081 | HTTP 200 | PASS |
| `/admin/vouchers` | curl.exe + cookie jar trên localhost:18081 | HTTP 200 | PASS |
| `/admin/promotions` | curl.exe + cookie jar trên localhost:18081 | HTTP 200 | PASS |
| `/admin/statistics` | curl.exe + cookie jar trên localhost:18081 | HTTP 200 | PASS |

Status của các URL có ID đơn/hóa đơn và POST nằm trong [phase-a-live-results.json](docs/evidence/phase-a/phase-a-live-results.json); [kiểm tra đọc sau build cuối](docs/evidence/phase-a/phase-a-final-readonly.log).

## Output Maven cuối

Lệnh thực thi (JDK 17, không skip):

```powershell
$env:JAVA_HOME='C:/Program Files/Eclipse Adoptium/jdk-17.0.17.10-hotspot'
$env:PATH="$env:JAVA_HOME/bin;$env:PATH"
$env:SPRING_DATASOURCE_URL='jdbc:sqlserver://localhost:1433;databaseName=shopbangiay_thu;encrypt=true;trustServerCertificate=true'
# Khi máy chưa có cấu hình local, nhập tài khoản SQL kiểm thử qua biến môi trường.
$env:SPRING_DATASOURCE_USERNAME=Read-Host 'Tài khoản SQL kiểm thử'
$env:SPRING_DATASOURCE_PASSWORD=[System.Net.NetworkCredential]::new('', (Read-Host 'Mật khẩu SQL kiểm thử' -AsSecureString)).Password
$env:SPRING_JPA_HIBERNATE_DDL_AUTO='none'
$env:SPRING_JPA_SHOW_SQL='false'
.\mvnw.cmd clean test
```

```text
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.300 s -- in com.example.demodatn.service.sanpham.InitialMaterialIntegrationTest
[INFO] Tests run: 109, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time:  01:20 min
```

109 gồm 101 test có sẵn và 8 test hồi quy Giai đoạn A. Raw output: [maven-clean-test.txt](docs/evidence/phase-a/maven-clean-test.txt); tóm lược không bỏ số test từng lớp: [maven-output.txt](docs/evidence/phase-a/maven-output.txt).

## Kiểm thử chính cây sẽ commit

Đã xuất index tạm sang `verify/phase-a-commit-tree` và chạy lại **mvnw clean test**, với URL/username/password của SQL kiểm thử truyền qua môi trường, `ddl-auto=none`.

```text
[INFO] Tests run: 109, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time:  01:20 min
```

Raw output: [maven-exact-commit.txt](docs/evidence/phase-a/maven-exact-commit.txt).

Các lần dựng cây tách trước đó chưa đạt vì: chưa truyền thông tin SQL qua môi trường (84 lỗi kết nối); guard phân trang cũ chỉ chấp nhận shop_verify (4 assertion); fragment POS khach-hang có sẵn trong workspace nhưng chưa tracked (1 lỗi template). Đã truyền đủ môi trường, giữ guard cho đúng hai DB kiểm thử được phép, đưa kèm fragment mà controller hiện tại đã tham chiếu. Không skip test, không sửa để bỏ assertion; lần cuối **109/109 PASS**. Trích output các lần chưa đạt: [isolated-build-attempts.txt](docs/evidence/phase-a/isolated-build-attempts.txt).

## Vị trí code chính

- `src/main/java/com/example/demodatn/controller/admin/sanpham/AdminProductController.java:58`: xử lý lỗi cạnh ô và lưu form.
- `src/main/java/com/example/demodatn/service/sanpham/AdminCatalogService.java:47`: transaction tạo sản phẩm cùng size/chất liệu ban đầu.
- `src/main/java/com/example/demodatn/service/sanpham/InventoryService.java:105`: ghi Khởi tạo, kể cả tồn 0.
- `src/main/java/com/example/demodatn/repository/sanpham/AdminCatalogQuery.java:43`: nhãn tối đa ba tên và +n, truy vấn chỉ variant đang bán.
- `src/main/java/com/example/demodatn/repository/sanpham/ProductVariantRepository.java:10`: đếm số variant dùng chất liệu.
- `src/main/java/com/example/demodatn/dto/sanpham/ProductDto.java:38`: tên chất liệu thật cho card dùng chung.
- `src/main/resources/templates/admin/san-pham/bieu-mau.html:22`: khối Biến thể ban đầu.
- `src/test/java/com/example/demodatn/service/sanpham/InitialMaterialIntegrationTest.java`: 8 hồi quy mới, transaction/zero stock/validation/material paging/hidden/security.

## Schema và lệnh chạy

Giai đoạn A **không đổi schema**: sử dụng `chat_lieu`, `product_variants.chat_lieu_id`, `order_details.ten_chat_lieu` và `inventory_logs` đã có. Không có migration mới để chạy; không chạy DDL hoặc migration nào trong giai đoạn này. Các cột tên/mô tả/lý do cần tiếng Việt đã là NVARCHAR; Hibernate `ddl-auto=none`. Metadata thô: [nvarchar-metadata.txt](docs/evidence/phase-a/nvarchar-metadata.txt).

Nếu dựng một bản sao chưa có phần chất liệu, kiểm tra và chạy migration idempotent đã có, chỉ vào database kiểm thử đã tạo đúng schema nền. SQLCMDPASSWORD do người vận hành đặt, không đưa mật khẩu vào Git:

```powershell
# Chỉ cần nếu bản sao chưa áp dụng migration 005; database kiểm thử phải tồn tại.
sqlcmd -S tcp:localhost,1433 -U sa -C -d shop_verify -b -f 65001 -i database/migrations/005_chat_lieu.sql
# Không cần chạy lại migration khi tiếp tục dùng shopbangiay_thu đã nghiệm thu.
```

Chạy app bản sao sau khi đặt datasource như lệnh Maven ở trên:

```powershell
$env:SERVER_PORT='18081'
$env:SHOP_UPLOAD_DIR='verify/uploads'
.\mvnw.cmd spring-boot:run
```

Script HTTP tái nghiệm thu ở `verify/phase_a_live.py` yêu cầu `PHASE_A_ADMIN_PASSWORD`, `SQLCMDPASSWORD`, `PHASE_A_TEST_DB=shopbangiay_thu` (hoặc shop_verify); script bắt buộc kiểm tra DB_NAME và dùng curl.exe + CSRF. Nó tìm sản phẩm fixture `VERIFY-A-20261024`, tạo khách/đơn test và bán tồn kho trên bản sao. Không chạy lại script tùy tiện vì tạo thêm đơn và trừ tồn POS; xem log đã lưu để đọc bằng chứng không ghi dữ liệu.

Giới hạn bổ sung: audit UI toàn project còn các mục ngoài giai đoạn, xem [ui-audit-summary.json](docs/evidence/phase-a/ui-audit-summary.json). Không kiểm thử máy in vật lý; bản PDF và CSS print đã được kiểm tra. Không triển khai hay chạy migration lên database thật.


# Giai đoạn B — Đang dừng ở bước tái hiện

Ngày 24/10/2026: Chưa tái hiện được lỗi trên shop_verify hoặc bản sao shopbangiay_thu. 6306 GET trả HTTP 200; browser 424 tổ hợp không lỗi JS sau khi cấu hình đúng thư mục ảnh test. Tổng/số đơn/trung bình và top 10 hai cách ở cả ba kênh khớp SELECT độc lập. **Chưa sửa code/thêm test/commit sửa lỗi. Dừng theo mục 1, cần stacktrace từ người dùng.**

[Báo cáo B, bảng URL/status, SQL cạnh số trên trang và bằng chứng thô](docs/GIAI_DOAN_B_TAI_HIEN.md).


## Giai đoạn C — Sổ địa chỉ nhận hàng (24/10/2026)

Hoàn tất tối đa 3 địa chỉ bằng service có khóa customer và ràng buộc SQL; hồ sơ/checkout dùng combo box, admin chỉ xem, snapshot đơn và Unicode đã đối chiếu SQL. Chỉ chạy shop_verify. Migration 009 chạy hai lần (thêm 1 rồi 0); 117 test PASS, 0 lỗi/bỏ qua; 23 URL chính HTTP 200; 8 ca bảo mật PASS; browser 0 lỗi JS. Audit UI toàn repo còn 90 findings, ghi rõ trong báo cáo.

[Bảng nghiệm thu C, SQL trước/sau, status, ảnh và lệnh migration](docs/GIAI_DOAN_C_DIA_CHI.md). Dừng sau C.


## Phase 1 kiểm tra đường dẫn — hoàn tất 25/10/2026

Đã restore backup có sẵn thành shopbangiay_kt, chạy migration 004–009 và app cổng 18086 có log Started. Chỉ dùng DB test; không mở shopbangiay. Bản chuẩn bị commit: 108 mapping Spring, 710 request crawl (không link render hỏng/500), 2103 form đúng route/method, 57 ID sai trả 404 thân thiện, 4800 tổ hợp thống kê HTTP 200, Playwright 232 lượt 1280/375px, 8 ca bảo mật PASS. mvnw clean test: 121 run, 0 failures/errors/skipped, BUILD SUCCESS.

Đã sửa xử lý numeric path ID sai và fallback ảnh thiếu file. Hai file gốc vẫn thiếu (6 tham chiếu DB). Test job có sẵn đã tác động đơn DH000669 trên bản sao và để lại 1 log; lần fixture lỗi để lại 1 user thử. Ghi rõ trong docs/BUG_PHAT_HIEN.md, không tự xóa hoặc điều chỉnh tồn. Các trường hợp không có bằng chứng đánh NOT TESTED trong báo cáo.

[Bảng URL/vai trò/status, bảng lỗi, COUNT và bằng chứng thô](docs/PHASE1_KIEM_TRA_DUONG_DAN.md). DỪNG trước phase 2/3; không merge/push.
