# Bổ sung 012 — Ngừng bán / Bán lại và mã sản phẩm tự sinh

Ngày 29/10/2026, nhánh `gop-y-giang-vien`, nền `81feeeb`. Chỉ làm bổ sung A/B này; không push, không merge.

**Kết quả: 24 hạng mục PASS, 0 FAIL, 0 NOT TESTED trong phạm vi yêu cầu.** `mvnw clean test`: **153 test, 0 lỗi, 0 bỏ qua**. Playwright: **41 kịch bản cuối** ở 1280/375px; crawl **52 lượt URL chính**. Các lần harness sai giả định về Location tuyệt đối, flash chưa tiêu thụ, form action bị input trùng tên che khuất hoặc phiên trình duyệt đã đóng đã được sửa và chạy lại; không tính chúng là lỗi ứng dụng hoặc PASS nếu chưa chạy lại.

## Phạm vi và an toàn

Không kết nối, đọc, ghi hoặc chạy migration trên **shopbangiay** và **shopbangiay_thu**. Chỉ datasource `shopbangiay_kt_20261029_bs012`; `master` chỉ dùng đọc metadata backup và điều phối RESTORE/BACKUP của bản sao này. Backup gần nhất của project tại thời điểm bắt đầu (đối chiếu metadata msdb, không kết nối DB nguồn): `shopbangiay_kt_20261029_gd4_schema_cb9adbb157e24a3281de3630c664062b.bak`, hoàn tất 29/10/2026 09:03:10. [Metadata backup toàn project trước thời điểm bắt đầu](evidence/bo-sung-012/all-backups-before-start.json).

RESTORE ban đầu có guard DB chưa tồn tại, không REPLACE. Tên logical file `shopbangiay` trong [output RESTORE](evidence/bo-sung-012/restore-new.txt) là metadata trong file backup, không phải kết nối DB đó. Chỉ RESTORE có REPLACE lên bản sao do lượt này tạo sau từng nhóm phá dữ liệu, với kiểm tra đường dẫn MDF và checkpoint riêng.

App chạy cổng **18150**, phải có Started trước kiểm thử; Hibernate `ddl-auto=validate`, show-sql=false; các lượt app dùng upload riêng `verify/bs012/uploads`. [Log startup validate](evidence/bo-sung-012/startup-validate.txt). Tất cả tiến trình và phiên Playwright của lượt này đã đóng; bản sao đã hoàn nguyên.

## Bảng nghiệm thu

| Hạng mục | Cách kiểm tra | Kết quả thực tế | PASS/FAIL/NOT TESTED |
|---|---|---|---|
| An toàn database | RESTORE mới, guard DB_NAME, log Started | Chỉ shopbangiay_kt_20261029_bs012; app 18150; không ghi đè DB có sẵn | PASS |
| Migration 012 | Chạy lặp, đối chiếu COUNT và sequence | 21 bảng, 163 cột; COUNT trước/sau 012 không đổi; counter không lùi | PASS |
| Unique index | DB có sẵn index; thử thiếu index trong transaction có rollback | Giữ index cũ; tạo UX_products_code khi thiếu; chạy lần hai không lỗi; mã trùng bị 2627 | PASS |
| Script gộp/khởi tạo | 004–012 chạy hai lần; so nội dung 012 trong shopgiay.sql | Script gộp thành công, COUNT không đổi; bootstrap đã đồng bộ | PASS |
| Schema validate | Khởi động trước từng nhóm, chờ Started | Hibernate validate sạch; sequence/index kiểm tra độc lập bằng SQL | PASS |
| Nút trạng thái sản phẩm | Bấm danh sách/trang xem, xác nhận, gửi lặp, 1280/375 | Ngừng bán/Bán lại đúng nhãn; POST lặp giữ nguyên trạng thái đích | PASS |
| Nút trạng thái biến thể | Bấm Bán lại/Ngừng bán và POST hai lần, hai viewport | Chỉ đổi trạng thái biến thể, giữ nguyên tồn; trạng thái sản phẩm không đổi | PASS |
| Bảo toàn biến thể/tồn kho | SELECT trước/off/on | SP006: tồn 9/12; SP007: 11/12; trạng thái true/false giữ nguyên qua off/on | PASS |
| User ẩn sản phẩm | Trang chủ, products, search, thương hiệu, khuyến mãi, liên quan | Có trước, không còn link sau stop, trở lại sau resume; chi tiết dừng bán 404 thân thiện | PASS |
| Giỏ/checkout | Giỏ đã chứa trước; thêm thường/AJAX, GET/POST checkout sau stop | Cảnh báo dòng; AJAX 400; checkout 302 về cart; server không tạo đơn; bán lại checkout 200 | PASS |
| POS/hóa đơn chờ | Tìm mã, mở hold đã lưu trước, thử trả tiền | Ẩn khỏi tìm kiếm; cảnh báo dòng; nút pay disabled; server từ chối, không trừ kho/tạo đơn | PASS |
| Admin và Xóa | Xem/sửa/biến thể/tồn khi dừng bán; điều chỉnh +1 rồi hoàn lại; Xóa sản phẩm có đơn | 200; điều chỉnh được; Xóa giữ sản phẩm và gợi ý Ngừng bán; hành vi xóa cũ giữ nguyên | PASS |
| Lịch sử và thống kê | SQL đơn/invoice/DT trước-off-on; so bảng/metrics doanh thu và bán chạy ba kênh | Dữ liệu lịch sử, số đơn và doanh thu giữ nguyên; các trang thống kê không đổi sau stop | PASS |
| Đợt giảm giá/voucher | COUNT và CHECKSUM trước/off/on | Không đổi dữ liệu đợt giảm giá, chi tiết đợt hoặc voucher | PASS |
| Lọc trạng thái | Admin keyword mã + status=false | Sản phẩm dừng bán vẫn hiện đúng bộ lọc | PASS |
| Mã tự sinh và readonly | Form thật, giả mạo mã tạo/sửa; SQL | Mã lớn nhất hợp lệ SP005 → SP006 → SP007; bỏ qua mã khác dạng; form không dự đoán số | PASS |
| 20 yêu cầu đồng thời | 20 HTTP POST cùng code giả; đối chiếu ID/code bằng SELECT | 20 ID, 20 mã khác nhau SP008–SP027; không nhận SAME-FORGED-CODE | PASS |
| Rollback và khoảng trống | POST ảnh hỏng sau khi cấp mã, SELECT COUNT/counter, tạo tiếp | COUNT products 32 → 32; sequence 27 → 28; không có SP028; mã tiếp SP029 | PASS |
| Qua 999 | Fixture SP999 trực tiếp, chạy 012, tạo qua form | Form tạo SP1000, không cắt còn ba số; unique index chặn chèn trùng | PASS |
| Mã ở các nơi cũ | Admin/POS tìm mã, xem/sửa, giỏ, hóa đơn và print media | Mã mới hiện đúng; mã cũ readonly không đổi; fixture entity trực tiếp vẫn hoạt động trong test cũ | PASS |
| Bảo mật | 8 nhóm bên dưới | Guest/User/Admin đúng quyền; thiếu/sai CSRF 403; GET không thực hiện POST; không đọc đơn người khác | PASS |
| Maven | mvnw.cmd clean test, không skip | 153 test; 0 Failures; 0 Errors; 0 Skipped; BUILD SUCCESS | PASS |
| Playwright/crawl | 1280px và 375px; console, pageerror, request, kích thước | 41 kịch bản cuối PASS; 52 lượt URL chính; 0 lỗi JS/console, 0 static 404, không tràn ngang | PASS |
| Hoàn nguyên | RESTORE sau từng nhóm phá dữ liệu, đối chiếu metadata/count/mã cũ | 21 bảng khớp checkpoint: products 10, variants 20, orders/invoices 28; sequence trở về 6 của bản sao | PASS |

## SQL và COUNT trước/sau

Mọi helper SQL dùng guard `DB_NAME()=N'shopbangiay_kt_20261029_bs012'` rồi mới truy vấn. COUNT lấy từ `sys.tables` và `SELECT COUNT_BIG(*) FROM dbo.[tên_bảng]`; schema đối chiếu `sys.columns`. Không thêm cột chữ mới trong 012; các tên/thông báo tiếng Việt dùng Unicode, không sửa mã cũ.

| Bảng | Trước 012 | 012 lần 1 | 012 lần 2 | Sau nhóm UI đầy đủ | Cuối sau RESTORE |
|---|---:|---:|---:|---:|---:|
| categories | 6 | 6 | 6 | 6 | 6 |
| chat_lieu | 8 | 8 | 8 | 8 | 8 |
| customers | 14 | 14 | 14 | 15 | 14 |
| de_giay | 8 | 8 | 8 | 8 | 8 |
| dia_chi_nhan_hang | 7 | 7 | 7 | 7 | 7 |
| employees | 5 | 5 | 5 | 5 | 5 |
| inventory_logs | 63 | 63 | 63 | 73 | 63 |
| invoices | 28 | 28 | 28 | 32 | 28 |
| order_details | 35 | 35 | 35 | 39 | 35 |
| orders | 28 | 28 | 28 | 32 | 28 |
| pos_held_items | 0 | 0 | 0 | 3 | 0 |
| pos_held_orders | 0 | 0 | 0 | 3 | 0 |
| product_images | 6 | 6 | 6 | 8 | 6 |
| product_variants | 20 | 20 | 20 | 24 | 20 |
| products | 10 | 10 | 10 | 33 | 10 |
| promotion_details | 7 | 7 | 7 | 9 | 7 |
| promotions | 5 | 5 | 5 | 6 | 5 |
| roles | 2 | 2 | 2 | 2 | 2 |
| sizes | 8 | 8 | 8 | 8 | 8 |
| users | 12 | 12 | 12 | 13 | 12 |
| vouchers | 15 | 15 | 15 | 15 | 15 |

[Toàn bộ COUNT và số cột từng mốc](evidence/bo-sung-012/counts-before-after.json); [hoàn nguyên cuối, sequence và mã cũ](evidence/bo-sung-012/final-restore-proof.json). Script gộp 004–012 có COUNT bằng nhau hai lần, trong nhóm thử mốc 999 nên products=12 (10 gốc + fixture SP999 + SP1000), sau đó đã RESTORE về 10.

Truy vấn bảo toàn dữ liệu (chạy theo ID của từng sản phẩm giả):

```sql
SELECT id,code,status FROM products WHERE id=@product_id;
SELECT id,status,quantity,price FROM product_variants
WHERE product_id=@product_id ORDER BY id;
SELECT o.id,o.status,o.payment_status,o.final_amount,i.id invoice_id
FROM orders o JOIN invoices i ON i.order_id=o.id WHERE o.id=@order_id;
SELECT COUNT_BIG(*) order_count,COALESCE(SUM(final_amount),0) revenue
FROM orders WHERE payment_status='PAID'
AND status NOT IN ('CANCELLED','DELIVERY_FAILED');
```

| Sản phẩm giả | Trước stop | Sau stop | Sau bán lại | Biến thể/tồn cả ba mốc |
|---|---|---|---|---|
| SP006, ID 949 | products.status=true | false | true | ID 598: true, 9; ID 599: false, 12 |
| SP007, ID 950 | products.status=true | false | true | ID 600: true, 11; ID 601: false, 12 |

Số tồn trước stop đã bao gồm các giao dịch POS chuẩn bị giỏ/hóa đơn; không giả định tồn còn 12 sau khi bán. [SQL thực tế before/stopped/afterRejectedSales/resumed](evidence/bo-sung-012/status-sql.json) so bằng nhau mọi phần ngoài `products.status`, gồm đơn cũ, invoice, doanh thu, COUNT/CHECKSUM promotion/voucher. Kiểm tra điều chỉnh kho khi ngừng bán là thao tác riêng: 12 → 13 → 12, có lý do/log; [proof](evidence/bo-sung-012/admin-maintenance-proof.json). Không quy thay đổi chủ động này cho nút trạng thái.

Hóa đơn đã thanh toán dùng sản phẩm mới trước stop vẫn xem/in được sau stop, còn mã sản phẩm đúng. Dữ liệu đơn không bị sửa bởi stop/resume. `ProductSalePolicy` chỉ áp dụng bán mới, không áp dụng lên query lịch sử. Query doanh thu/bán chạy không đổi; [Playwright historical-statistics](evidence/bo-sung-012/playwright-final.json) so metrics và bảng của cả ALL/ONLINE/POS trước/sau stop sản phẩm có dòng đơn PAID.

## Mã và sequence: bằng chứng thô

[Trạng thái trước tạo](evidence/bo-sung-012/initial-state.json): các mã đúng dạng chỉ có SP001…SP005; các mã `P6-E2E-1007`, `P12-*`, `NA_LCD100` không làm tăng điểm bắt đầu. Sequence BIGINT bắt đầu **6**, increment=1, NO CYCLE, NO CACHE. Hai form thật tạo **SP006, SP007**, bất chấp POST code giả; sửa gửi code khác vẫn giữ mã đã lưu. [Proof form và SELECT biến thể](evidence/bo-sung-012/created.json).

```sql
SELECT name,CONVERT(BIGINT,current_value) current_value,
       CONVERT(BIGINT,last_used_value) last_used_value
FROM sys.sequences WHERE name='product_code_seq';
-- Allocation duy nhất trong repository, nằm trong transaction tạo sản phẩm:
SELECT NEXT VALUE FOR dbo.product_code_seq;
```

- [20 HTTP POST đồng thời và SELECT](evidence/bo-sung-012/code-http-proof.json): 20 response 302, 20 ID riêng, 20 mã riêng SP008–SP027. Mọi request cùng gửi `SAME-FORGED-CODE`, đều bị bỏ qua.
- Cùng file proof: ảnh không hợp lệ bị từ chối 200 ở form, products **32 → 32**, sequence **27 → 28**, không lưu bản ghi rollback; lần kế tiếp response 302, ID 972, mã **SP029**, không tái dùng SP028.
- [Mốc 999](evidence/bo-sung-012/boundary-proof.json): thêm fixture trực tiếp SP999 trên bản sao, chạy 012 rồi tạo form nhận **SP1000**. Chèn trùng SP1000 bị SQL **2627** và rollback.
- [Chạy lặp sequence](evidence/bo-sung-012/sequence-repeat.json): current_value **1001 → 1001 → 1001**, không đặt lùi. Test tích hợp còn kiểm chạy lại sau số đã tiêu thụ bởi rollback không tái dùng số đó.
- [Thử nhánh chưa có unique index](evidence/bo-sung-012/index-branch.txt): tạm bỏ constraint của chính bản sao trong một outer transaction; 012 tạo `UX_products_code is_unique=1`; lần hai giữ một index; rollback và RESTORE lại checkpoint, products=10. Với index unique sẵn có, migration giữ nguyên, không tạo trùng.

MAX chỉ dùng trong migration để khởi tạo/tiến sequence theo dữ liệu hiện có, không dùng để cấp mã lúc tạo. Counter không CYCLE, không reset xuống; SQL unique index bảo vệ cuối cùng. Số có thể bị bỏ qua khi rollback — hành vi của [SQL Server sequence](https://learn.microsoft.com/en-us/sql/relational-databases/sequence-numbers/sequence-numbers). Khôi phục counter về 6 ở cuối là **RESTORE bản sao kiểm thử theo yêu cầu**, không phải logic runtime đặt lùi sequence.

DataInitializer không seed sản phẩm; không có seed sản phẩm profile demo để thay đổi. Luồng fixture/entity repository trực tiếp vẫn tự đặt mã; các test UnicodePersistence/TablePresentation/Material và các fixture cũ vẫn chạy trong suite 153 test. Chỉ AdminCatalogService tạo sản phẩm mới gọi ProductCodeRepository; khi sửa không gọi bộ sinh.

## Playwright và URL chính

[Raw status, request, JS console/pageerror, proof, viewport và scrollWidth](evidence/bo-sung-012/playwright-final.json). Các response 400/403/404/405 do test từ chối có chủ đích được giữ nguyên trong raw proof; không nhầm 404 chi tiết sản phẩm đã dừng bán với 404 tài nguyên tĩnh. Không có CSS/JS/ảnh 404 ở lượt cuối.

| Kịch bản | Viewport | Vai trò | Kết quả |
|---|---:|---|---|
| login-admin | 1280 | guest | PASS |
| create-product | 1280 | admin | PASS |
| create-product | 375 | admin | PASS |
| register-user | 1280 | guest | PASS |
| prepare-pos | 1280 | admin | PASS |
| cart-before-stop | 1280 | user | PASS |
| prepare-pos | 375 | admin | PASS |
| cart-before-stop | 375 | user | PASS |
| selling-before | 1280 | user | PASS |
| review-hold-before | 1280 | admin | PASS |
| stop-product | 1280 | admin | PASS |
| user-stopped | 1280 | user | PASS |
| admin-stopped | 1280 | admin | PASS |
| resume-product | 1280 | admin | PASS |
| user-resumed | 1280 | user | PASS |
| selling-before | 375 | user | PASS |
| review-hold-before | 375 | admin | PASS |
| stop-product | 375 | admin | PASS |
| user-stopped | 375 | user | PASS |
| admin-stopped | 375 | admin | PASS |
| resume-product | 375 | admin | PASS |
| user-resumed | 375 | user | PASS |
| twenty-concurrent-http | 1280 | admin | PASS |
| http-rollback | 1280 | admin | PASS |
| next-after-rollback | 1280 | admin | PASS |
| security-guest | 1280 | guest | PASS |
| security-user | 1280 | user | PASS |
| security-admin | 1280 | admin | PASS |
| admin-inventory-and-delete | 1280 | admin | PASS |
| variant-repeat-and-product-buttons | 1280 | admin | PASS |
| variant-repeat-and-product-buttons | 375 | admin | PASS |
| crawl-guest | 1280 | guest | PASS |
| crawl-guest | 375 | guest | PASS |
| crawl-user | 1280 | user | PASS |
| crawl-user | 375 | user | PASS |
| crawl-admin | 1280 | admin | PASS |
| crawl-admin | 375 | admin | PASS |
| code-beyond-999 | 1280 | admin | PASS |
| readonly-forms-final | 1280 | admin | PASS |
| readonly-forms-final | 375 | admin | PASS |
| historical-statistics | 1280 | admin | PASS |

Crawl 26 URL-vai trò × 2 viewport = 52 lượt, ngoài các URL trong kịch bản chức năng:

| URL | Vai trò | Status 1280/375 | Kết quả |
|---|---|---|---|
| `/` | guest | 200/200 | PASS, đích `/` |
| `/products` | guest | 200/200 | PASS, đích `/products` |
| `/products/2` | guest | 200/200 | PASS, đích `/products/2` |
| `/category/1` | guest | 200/200 | PASS, đích `/category/1` |
| `/cart` | guest | 200/200 | PASS, đích `/cart` |
| `/login` | guest | 200/200 | PASS, đích `/login` |
| `/register` | guest | 200/200 | PASS, đích `/register` |
| `/profile` | user | 200/200 | PASS, đích `/profile` |
| `/cart` | user | 200/200 | PASS, đích `/cart` |
| `/checkout` | user | 200/200 | PASS, đích `/checkout` |
| `/orders` | user | 200/200 | PASS, đích `/orders` |
| `/admin` | admin | 200/200 | PASS, đích `/admin` |
| `/admin/customers` | admin | 200/200 | PASS, đích `/admin/customers` |
| `/admin/customers/create` | admin | 200/200 | PASS, đích `/admin/customers/create` |
| `/admin/employees` | admin | 200/200 | PASS, đích `/admin/employees` |
| `/admin/employees/create` | admin | 200/200 | PASS, đích `/admin/employees/create` |
| `/admin/products-crud` | admin | 200/200 | PASS, đích `/admin/products-crud` |
| `/admin/variants` | admin | 200/200 | PASS, đích `/admin/variants` |
| `/admin/materials` | admin | 200/200 | PASS, đích `/admin/materials` |
| `/admin/categories` | admin | 200/200 | PASS, đích `/admin/categories` |
| `/admin/soles` | admin | 200/200 | PASS, đích `/admin/soles` |
| `/admin/inventory` | admin | 200/200 | PASS, đích `/admin/inventory` |
| `/admin/pos` | admin | 200/200 | PASS, đích `/admin/pos` |
| `/admin/statistics` | admin | 200/200 | PASS, đích `/admin/statistics` |
| `/admin/statistics/revenue` | admin | 200/200 | PASS, đích `/admin/statistics/revenue` |
| `/admin/statistics/best-sellers` | admin | 200/200 | PASS, đích `/admin/statistics/best-sellers` |

Ảnh: [ngừng bán desktop](evidence/bo-sung-012/bs012-stopped-product-final-1280.png), [ngừng bán mobile](evidence/bo-sung-012/bs012-stopped-product-final-375.png), [form mã readonly desktop](evidence/bo-sung-012/bs012-create-readonly-1280.png), [form mã readonly mobile](evidence/bo-sung-012/bs012-create-readonly-375.png), [giỏ User bị chặn](evidence/bo-sung-012/bs012-user-stopped-375.png), [POS/hóa đơn chờ bị chặn](evidence/bo-sung-012/bs012-admin-stopped-375.png). Ảnh và tài khoản nghiệp vụ trong các màn thử này là fixture giả trên bản sao.

## Tám nhóm bảo mật

| Mã | Cách kiểm tra | Status thực tế | Kết quả |
|---|---|---|---|
| SEC01 | Guest GET admin/products-crud | 302 | PASS |
| SEC02 | User GET admin/products-crud | 403 | PASS |
| SEC03 | Guest POST với CSRF hợp lệ | 302, không thực hiện | PASS |
| SEC04 | User POST status sản phẩm/biến thể và save | 403 | PASS |
| SEC05 | Admin POST thiếu CSRF | 403 ở cả ba route | PASS |
| SEC06 | Admin POST CSRF sai | 403 ở cả ba route | PASS |
| SEC07 | Admin GET route chỉ nhận POST trạng thái | 405, không đổi dữ liệu | PASS |
| SEC08 | User đọc đơn không thuộc tài khoản | 403 | PASS |

Các form mới dùng th:action (Spring tự thêm CSRF), hộp confirm hiện có; server không dựa vào readonly HTML để bảo vệ mã. POST trạng thái gửi `selling=true/false`, giữ tính idempotent ngay khi client gửi lại; không dùng thao tác toggle ngược trạng thái hiện tại.

## Phạm vi mã sửa và test

- `service/sanpham/AdminCatalogService.java`: cấp mã khi tạo, giữ mã khi sửa, hai phương thức cập nhật trạng thái bằng khóa dòng; không cập nhật tồn/biến thể khi đổi trạng thái sản phẩm.
- `repository/sanpham/ProductCodeRepository.java`: NEXT VALUE FOR trong transaction đang tạo, định dạng tối thiểu ba chữ số, không cắt mã lớn.
- `service/sanpham/ProductSalePolicy.java`: điều kiện sản phẩm và biến thể được bán; dùng ở User, giỏ/giá hiện tại, POS, tồn kho khi xuất cho đơn. Giữ SQL pushdown StatusTrue có sẵn để không tải dư dữ liệu; không đưa policy vào lịch sử.
- `InventoryService.lock`: làm mới/giữ khóa đọc trạng thái sản phẩm trong transaction xuất hàng để không dùng trạng thái cha đã cache trước đó; điều chỉnh kho Admin vẫn được phép khi dừng bán.
- `fragments/trang-thai-ban.html`: nút dùng chung, xác nhận nêu hậu quả, form POST có CSRF. Form mã readonly, lỗi/placeholder tiếng Việt.

Bổ sung 7 test tích hợp: mã tạo/sửa chống giả mạo; bảo toàn biến thể/tồn/lịch sử và chặn bán; trạng thái biến thể idempotent; 20 POST đồng thời; rollback không tái sử dụng; migration không đặt lùi; phân quyền/CSRF. Cập nhật các test cũ còn giả định mã nhập tay sang ID/mã thực trả về, giữ các kiểm tra ảnh, phân trang, khởi tạo kho, chất liệu, đế giày.

Lượt full test đầu phát hiện hai test còn truy vấn theo mã client và một lỗi cô lập test: chạy migration SET XACT_ABORT ON trên connection pool làm ảnh hưởng test unique đế giày sau đó. Đã sửa truy vấn test và khôi phục tùy chọn session ngay trong ConnectionCallback; không sửa nghiệp vụ đế giày. Lượt full cuối:

```text
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0 s -- in com.example.demodatn.util.PasswordPolicyTest
[INFO]
[INFO] Results:
[INFO]
[INFO] Tests run: 153, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  34.336 s
[INFO] Finished at: 2026-10-29T10:25:11+07:00
[INFO] ------------------------------------------------------------------------
```

[Output đầy đủ](evidence/bo-sung-012/clean-test.txt), [34 suite và số test](evidence/bo-sung-012/junit-suites.json). Không skip.

## Lệnh triển khai 012 — chỉ hướng dẫn, chưa chạy DB thật

Sau khi **sao lưu** và **dừng ứng dụng**, với DB đã có đến 011 chạy 012. Nếu thiếu các migration trước, dùng đúng thứ tự 004 → 005 → 006 → 007 → 008 → 009 → 010 → 011 → 012 qua `chay_tat_ca.sql`; không chạy 003 để nâng cấp. `shopgiay.sql` chỉ dùng DB rỗng, không chạy lên DB có dữ liệu.

Ví dụ PowerShell từ gốc project, thay server/database bằng giá trị triển khai và dùng tài khoản Windows có quyền. SESSION_CONTEXT là opt-in rõ ràng cho guard khi người vận hành chủ động chạy trên DB sản xuất; **lệnh này chưa được thực thi trên shopbangiay hoặc shopbangiay_thu**:

```powershell
@"
EXEC sys.sp_set_session_context @key=N'shop.allow-production-migration', @value=1;
GO
:r database/migrations/012_ma_san_pham_tu_sinh.sql
"@ | sqlcmd -S "<SERVER>" -d "<DATABASE_TRIEN_KHAI>" -E -I -b -f 65001
```

Nếu cần gộp, thay dòng `:r` bằng `:r database/migrations/chay_tat_ca.sql`. Kiểm tra unique index và sequence sau migration; không tự đặt lùi hoặc bật CYCLE. Chạy app với `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`, chờ log Started sạch trước khi mở bán.

Dừng sau bổ sung này. Không merge.
