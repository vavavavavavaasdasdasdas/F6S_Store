# Giai đoạn 10 — Chi tiết sản phẩm và biến thể

Nhánh `gop-y-dot-moi`. RESTORE mới `shopbangiay_kt_20261031_gd10` từ full backup đọc được gần nhất; không ghi đè DB có sẵn. App cổng 18310 đã có log Started với `ddl-auto=validate`. Không kết nối `shopbangiay` hoặc `shopbangiay_thu`. Giai đoạn này không đổi schema nên không thêm migration.

| Hạng mục | Cách kiểm tra | Kết quả thực tế | PASS/FAIL/NOT TESTED |
|---|---|---|---|
| Danh sách sản phẩm tám cột, bỏ lọc biến thể | MockMvc và Playwright/crawl DOM | STT, Ảnh, Mã, Tên sản phẩm, Thương hiệu, Số biến thể, Trạng thái, Hành động; còn từ khóa/thương hiệu/trạng thái/sắp xếp | PASS |
| Trang biến thể riêng | Theo nút Chi tiết biến thể và kiểm tra 12 cột | `/admin/products-crud/{id}/variants`, AJAX dùng service cũ, có Mã SP/Tên SP/Mã biến thể riêng | PASS |
| Xem/sửa sản phẩm không có bảng biến thể | DOM và MockMvc | Trang xem có thông tin, ảnh, số biến thể/tổng tồn và nút tới trang biến thể; trang sửa không chứa bảng | PASS |
| Form chia đôi và xem trước | Playwright 1280/375 | Desktop hai cột, mobile xếp dọc; ba kích cỡ hiện Sẽ tạo 3 biến thể | PASS |
| Tạo bằng form thật, một transaction | SQL sau gửi form ở mỗi viewport | Mỗi sản phẩm đúng ba biến thể, giá 321000, tồn 7; ba log Khởi tạo; quay về danh sách có nút Chi tiết biến thể | PASS |
| Sửa giá/tồn trong dòng | Playwright và SELECT logs | Một dòng giá 333000, tồn 9; log trước 7/thay đổi +2/sau 9/người admin; tổng tồn 23 | PASS |
| Chặn trùng ở mọi điểm vào | Form ban đầu, form Biến thể, inline, batch; SELECT trước/sau | Lần lượt 200 lỗi cạnh ô, 200 lỗi cạnh ô, 400 JSON, 400 JSON; không thêm hàng hay đổi thuộc tính | PASS |
| Hai request tạo trùng và gõ tạo màu mới | Test tích hợp SQL Server chạy đồng thời | Batch HTTP [200,400]; form gõ tạo mới [200,302]; mỗi trường hợp chỉ một biến thể, màu mới không nhân đôi | PASS |
| Khóa kho, toàn vẹn batch, đơn cũ | Bộ test EditableVariantIntegrationTest và toàn bộ test | Tiếp tục dùng InventoryService.adjust/expectedQuantity; batch lỗi rollback; snapshot đơn cũ giữ nguyên | PASS |
| Bảo mật tám nhóm | Guest/User/Admin; bốn POST thiếu/sai CSRF; sai product owner | Guest 302, User 403; tám request thiếu/sai token đều 403; sai sản phẩm 404; Admin hợp lệ 200 | PASS |
| Crawl toàn bộ link nội bộ | BFS hết hàng đợi ở ba vai trò | admin: 600 URL, user: 21 URL, guest: 20 URL; status và tên cột trong bằng chứng | PASS |
| Desktop/mobile, JS, icon, overflow | Quét mọi trang HTML và mọi bảng ở 1280/375 | Không lỗi JS/runtime, không 404 tài nguyên nội bộ, không tràn trang; bảng cuộn trong khung | PASS |
| mvnw clean test không skip | Maven offline clean test | 215 test, 0 thất bại, 0 lỗi, 0 bỏ qua | PASS |

## Bằng chứng thô

[RESTORE](evidence/gd10/restore-new.txt), [Started](evidence/gd10/startup.txt), [Maven](evidence/gd10/maven-summary.txt), [Playwright](evidence/gd10/browser-results.json), [SQL form 1280](evidence/gd10/form-sql-1280.json), [SQL form 375](evidence/gd10/form-sql-375.json), [SQL chống trùng trước/sau](evidence/gd10/duplicate-sql.json), [URL/status](evidence/gd10/URL_STATUS.md), [cột từng bảng](evidence/gd10/table-columns.json), [phục hồi sạch](evidence/gd10/final-restore.json).

Các SELECT độc lập: `SELECT id,product_id,size_id,ma,price,quantity FROM product_variants WHERE product_id=<ID> ORDER BY id`; `SELECT variant_id,quantity_before,quantity_change,quantity_after,reason,actor FROM inventory_logs WHERE variant_id IN (SELECT id FROM product_variants WHERE product_id=<ID>) ORDER BY id`. ID thật và kết quả đầy đủ nằm trong JSON.

Luồng tạo ban đầu gọi chung `AdminCatalogService.saveVariant` trong transaction sản phẩm; khóa theo sản phẩm để hai request cùng bộ năm nhận kết quả rõ ràng, DB unique index vẫn là ràng buộc cuối. Yêu cầu chứa kích cỡ lặp bị từ chối trước khi ghi. Giá nguyên đồng thống nhất với trang Biến thể. Test cũ đã cập nhật theo redirect về danh sách và chuyển bộ lọc thuộc tính sang Biến thể.

Lần chạy test đầu phát hiện biểu thức bean ở `th:src` bị Thymeleaf chặn trong ngữ cảnh URL; đã dùng `th:with` như quy ước có sẵn và kiểm tra lại bằng RouteErrorIntegrationTest/crawl. Kịch bản Playwright cũ chưa chờ xong điều hướng nên đếm hàng quá sớm; đã chờ và đối chiếu SQL, không sửa dữ liệu để làm qua test. Premium strict audit được đối chiếu với HEAD; tổng vẫn 150; hai loại cảnh báo được chuyển nguyên từ chi-tiet.html sang bien-the.html, xem JSON và chủ sở hữu tương tác trong UX-CONTRACT.md, không coi audit toàn dự án đã sạch.

Không có lệnh migration mới ở giai đoạn 10. Khi triển khai từ bản cũ, sao lưu rồi chạy migration đến 017 theo báo cáo giai đoạn 9 và khởi động `ddl-auto=validate`. Bản đồ vẫn dùng tile giả lập trong kiểm thử giao diện, không đo dịch vụ bản đồ thật. Chưa push hoặc merge.
