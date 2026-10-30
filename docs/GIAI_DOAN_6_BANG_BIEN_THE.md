# Giai đoạn 6 — Bảng biến thể sửa trực tiếp

Ngày 30/10/2026, nhánh `gop-y-giang-vien`. Bản sao mới `shopbangiay_kt_20261030_gd6` RESTORE từ backup mới nhất `shopbangiay_kt_20261030_gd5_schema.bak` (30/10/2026 11:36:50). App riêng18180, đã đợi Started, ddl-auto=validate sạch. Không đổi schema nên không thêm migration rỗng; migration mới nhất vẫn013. Không kết nối `shopbangiay` hoặc `shopbangiay_thu`.

| Hạng mục | Cách kiểm tra | Kết quả thực tế | PASS/FAIL/NOT TESTED |
|---|---|---|---|
| Bảng và lọc | Playwright, GET phân trang | 9 cột riêng; trang2 STT13, giữ bộ lọc; 375px cuộn trong khung | PASS |
| Sửa AJAX | Form thật1280/375, SELECT | Màu/đế/giá/tồn được lưu; Hủy giữ giá cũ; không điều hướng cả trang | PASS |
| Thêm hàng loạt | Form chọn3size và API14size | Một transaction; có trùng trả400 và không tạo nửa chừng | PASS |
| Giá nguyên đồng | Integration test và shared saveVariant | Giá .50 bị chặn; .00 chấp nhận; giá đơn cũ không đổi | PASS |
| Kho | SELECT product_variants/inventory_logs | 8→11: log trước8, thay đổi+3, sau11, actoradmin; quaInventoryService.adjust | PASS |
| Hai Admin | Hai browser context đăng nhập riêng, gửi cùng lúc | Một200/một409; thông báo Tồn kho đã thay đổi, tải lại; chỉ một log thay đổi | PASS |
| Ngừng bán/Bán lại | Nút thật ở hai viewport | Trạng thái đúng, dùng chung service; không đổi kho | PASS |
| Xóa | Form/API và SELECT | Chưa dùng xóa được; đã bán409; chưa bán nhưng tronghold409, không hiện nútXóa | PASS |
| Giá giỏ đang mở | Sửa giá ở contextAdmin, tải giỏ ở contextKhách | Hiện cảnh báo giá mới; tổng vẫn đúng; SELECT giá đơn cũ giống nhau | PASS |
| Bảo mật | 8nhómMockMvc và HTTP cho4POST | Khách302, User403, CSRFthiếu/sai403; sai chủsảnphẩm404 | PASS |
| Kiểm thử | mvnw clean test khôngskip | 169test, failures0/errors0/skipped0; BUILD SUCCESS | PASS |
| Crawl/Playwright | URLchính1280/375, JS/request/layout | 25caPASS; 48lượtURL; khôngJSruntime error,404 hoặc tràn ngang ở crawl | PASS |
| Khôi phục | RESTORE rồi soCOUNT và20biến thể | Tất cảkhớpbaseline; app18180đãdừng; không kết nối haiDBcấm | PASS |

`AdminCatalogService.saveVariant` dùng chung cho trang Biến thể và bảng sửa trực tiếp; thay đổi số lượng gọi `InventoryService.adjust` trước khi ghi thuộc tính/giá để thao tác refresh không làm mất thay đổi. Batch gọi cùng saveVariant trong một transaction; trùng bất kỳ tổ hợp nào rollback cả lô. Giá lưu vào đơn cũ không cập nhật lại. Biến thể đã có đơn hoặc hóa đơn chờ chỉ gợi ý Ngừng bán, server kiểm tra lại khi nhận POST.

## SQL và bằng chứng thô

```sql
SELECT id,size_id,mau_sac_id,chat_lieu_id,de_giay_id,price,quantity,status
FROM product_variants WHERE product_id=949 ORDER BY id;
SELECT l.variant_id,l.quantity_before,l.quantity_change,l.quantity_after,l.reason,l.actor
FROM inventory_logs l JOIN product_variants v ON v.id=l.variant_id
WHERE v.product_id=949 ORDER BY l.id;
-- Sửa dòng: 8 / +3 / 11 / Chỉnh trên trang sản phẩm / admin.
-- Hai phiên cùng đọc11: HTTP200 +409; kho11→12, không ghi đè bởi phiên thua.
```

[Trước/sau tạo và sửa](evidence/giai-doan-6/created.json), [trạng thái và xóa](evidence/giai-doan-6/actions.json), [hai phiên và SQL/log](evidence/giai-doan-6/concurrency.json), [14biến thể/phântrang](evidence/giai-doan-6/page-proof.json), [biến thể tronghold:0dòng đơn/1hold và kho5giữ nguyên](evidence/giai-doan-6/held-delete-proof.json), [giá dòng đơn trước/sau](evidence/giai-doan-6/old-order-prices.json), [khôi phục](evidence/giai-doan-6/final-restore.json).

## Bảo mật — tám nhóm

| Nhóm | Kết quả thực tế | Trạng thái |
|---|---|---|
| KháchGETAdmin |302|PASS|
| UserGETAdmin |403|PASS|
| AdminGETAdmin |200|PASS|
| KháchPOSTcóCSRF |302|PASS|
| UserPOSTcóCSRF |403 trên cả4endpoint|PASS|
| AdminPOSTthiếuCSRF |403 trên cả4endpoint|PASS|
| AdminPOSTsaiCSRF |403 trên cả4endpoint|PASS|
| DùngIDbiến thể của sản phẩm khác |404|PASS|

POST gồm `/inline-save`, `/batch`, `/{id}/status`, `/{id}/delete` dưới `/admin/products-crud/{productId}/variants`. [RawbrowserHTTP](evidence/giai-doan-6/browser-final.json); regression `EditableVariantIntegrationTest` cũng kiểm tra đủ támnhóm. HTTP400/409 ở ca cố tình gửi dữ liệu trùng/cũ là kết quả dự kiến, giữ trong log; không coi thông báo network400/409 của trình duyệt là lỗi JavaScript.

## URL

| URL | Vai trò | Viewport | Status |
|---|---|---|---:|
| / | guest | 1280px | 200 |
| /products | guest | 1280px | 200 |
| /products/2 | guest | 1280px | 200 |
| /cart | guest | 1280px | 200 |
| /login | guest | 1280px | 200 |
| / | guest | 375px | 200 |
| /products | guest | 375px | 200 |
| /products/2 | guest | 375px | 200 |
| /cart | guest | 375px | 200 |
| /login | guest | 375px | 200 |
| /profile | user | 1280px | 200 |
| /orders | user | 1280px | 200 |
| /cart | user | 1280px | 200 |
| /checkout | user | 1280px | 200 |
| /profile | user | 375px | 200 |
| /orders | user | 375px | 200 |
| /cart | user | 375px | 200 |
| /checkout | user | 375px | 200 |
| /admin | admin | 1280px | 200 |
| /admin/products-crud | admin | 1280px | 200 |
| /admin/products-crud/view/949 | admin | 1280px | 200 |
| /admin/products-crud/view/1 | admin | 1280px | 200 |
| /admin/variants | admin | 1280px | 200 |
| /admin/colors | admin | 1280px | 200 |
| /admin/materials | admin | 1280px | 200 |
| /admin/soles | admin | 1280px | 200 |
| /admin/inventory | admin | 1280px | 200 |
| /admin/pos | admin | 1280px | 200 |
| /admin/customers | admin | 1280px | 200 |
| /admin/employees | admin | 1280px | 200 |
| /admin/invoices | admin | 1280px | 200 |
| /admin/statistics/revenue | admin | 1280px | 200 |
| /admin/statistics/best-sellers | admin | 1280px | 200 |
| /admin | admin | 375px | 200 |
| /admin/products-crud | admin | 375px | 200 |
| /admin/products-crud/view/949 | admin | 375px | 200 |
| /admin/products-crud/view/1 | admin | 375px | 200 |
| /admin/variants | admin | 375px | 200 |
| /admin/colors | admin | 375px | 200 |
| /admin/materials | admin | 375px | 200 |
| /admin/soles | admin | 375px | 200 |
| /admin/inventory | admin | 375px | 200 |
| /admin/pos | admin | 375px | 200 |
| /admin/customers | admin | 375px | 200 |
| /admin/employees | admin | 375px | 200 |
| /admin/invoices | admin | 375px | 200 |
| /admin/statistics/revenue | admin | 375px | 200 |
| /admin/statistics/best-sellers | admin | 375px | 200 |

## Lỗi tìm được và sửa

| Mã | File | Nguyên nhân/cách sửa | Chạy lại |
|---|---|---|---|
|G6-01|CartController.java|Giỏ chỉ cập nhật giá âm thầm; bổ sung cảnh báo một lần và vẫn cộng đúng tổng|Integration + browser375 PASS|
|G6-02|AdminCatalogService.java|adjust refresh entity; chuyển adjust trước bước ghi thuộc tính/giá để giữ cả giá/màu/đế mới|SELECT và regression PASS|

Timeout đọc lỗi batch ban đầu do script kiểm tra ngay khi nhậnresponse trước khi JS hiển thị lỗi; đã chờ đúng trạng thái cạnh ô. [Lịch sử lần chạy](evidence/giai-doan-6/browser-attempts.json).

## Test và ảnh

```text
.\mvnw.cmd -B -o clean test
Tests run:169, Failures:0, Errors:0, Skipped:0
BUILD SUCCESS
```

[Output từng lớp](evidence/giai-doan-6/maven-clean-test.txt). [Ảnh sửa dòng1280](evidence/giai-doan-6/gd6-inline-edit-save-cancel-and-duplicate-batch-1280.png), [375](evidence/giai-doan-6/gd6-inline-edit-save-cancel-and-duplicate-batch-375.png), [trang2STT13](evidence/giai-doan-6/gd6-page-two-starts-thirteen-and-filters-375.png).

Đã restore sau nhóm browser, nhóm test tích hợp và nhóm thử đổi giá cuối. [COUNT/biến thể trước và sau](evidence/giai-doan-6/final-restore.json) khớp hoàn toàn. Không thay luật kho/doanhthu/hủyđơn; không merge/push. Tiếp tục Giai đoạn7 theo yêu cầu mới của người dùng.
