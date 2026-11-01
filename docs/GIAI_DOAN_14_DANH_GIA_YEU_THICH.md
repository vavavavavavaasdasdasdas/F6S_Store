# Giai đoạn 14 — Đánh giá sản phẩm và Sản phẩm yêu thích

Nhánh gop-y-dot-moi. Đã thực hiện đầy đủ chức năng Đánh giá sản phẩm và Sản phẩm yêu thích theo yêu cầu kỹ thuật.

| Hạng mục | Cách kiểm tra | Kết quả thực tế | PASS/FAIL/NOT TESTED |
|---|---|---|---|
| Migration 021 | Chạy migration hai lần trên bản sao kiểm thử | Bảng `danh_gia` (id, code, product_id, customer_id, order_id, so_sao, noi_dung, hien_thi, created_at, updated_at) và `yeu_thich` (customer_id, product_id, created_at) được tạo sạch; hai cột rate limit `danh_gia_cua_so`, `danh_gia_so_lan` gắn vào `customers`; chạy lần hai idempotent | PASS |
| Điều kiện đánh giá | Đơn hàng trạng thái DELIVERED | Khách chỉ được đánh giá sản phẩm khi đã có đơn hàng Đã giao (DELIVERED) chứa sản phẩm đó; đơn chưa giao hoặc không có sản phẩm bị chặn 403 | PASS |
| Một đánh giá mỗi khách / sản phẩm | Ràng buộc duy nhất DB và Service | Mỗi khách chỉ gửi 1 đánh giá cho mỗi sản phẩm; cố tình tạo trùng bị chặn bởi UNIQUE constraint (mã lỗi 2601/2627) và validation | PASS |
| Sửa / xóa đánh giá | Khách sở hữu đánh giá | Khách sửa hoặc xóa được đánh giá của chính mình; người dùng khác sửa/xóa bị từ chối 403; xóa xong có thể đánh giá lại với mã mới | PASS |
| Chi tiết đơn hàng | Nút đánh giá | Chi tiết đơn hàng DELIVERED hiển thị nút Đánh giá cho từng dòng sản phẩm | PASS |
| Trang chi tiết sản phẩm | Điểm trung bình, phân bố sao, danh sách | Hiển thị điểm TB, tổng lượt, phân bố sao 1..5, danh sách đánh giá phân trang 12 (tên che dạng `Ng*** An`, ngày, nội dung, nhãn "Đã mua hàng"); thẻ sản phẩm hiện sao trung bình | PASS |
| Chống XSS & Giới hạn | Escape HTML, max 1000 ký tự, rate limit | Chuỗi `<script>` được escape thành văn bản an toàn; giới hạn độ dài 1000 ký tự; rate limit tối đa 5 lần gửi/sửa mỗi phút | PASS |
| Quản trị Đánh giá | Menu Sản phẩm -> Đánh giá (/admin/reviews) | Bảng chuẩn: STT \| Mã \| Sản phẩm \| Khách \| Số sao \| Nội dung \| Ngày \| Hiển thị \| Hành động (icon xem); bộ lọc sản phẩm, số sao, hiển thị, khoảng ngày; công tắc AJAX ẩn/hiện; Admin không có quyền xóa | PASS |
| Đánh giá ẩn | Loại trừ khỏi điểm trung bình | Khi Admin ẩn đánh giá, điểm TB và số lượt trên trang chi tiết tự động cập nhật loại bỏ đánh giá ẩn; khách sửa đánh giá đang ẩn vẫn giữ trạng thái ẩn | PASS |
| Sản phẩm yêu thích | Nút trái tim thẻ & chi tiết | Nút trái tim 40px hỗ trợ AJAX + CSRF; khách chưa đăng nhập bấm tim chuyển sang `/login` và sau đăng nhập tự quay lại trang; danh sách `/favorites` phân trang 12 có lọc | PASS |
| Sản phẩm ngừng bán | Hiển thị thẻ yêu thích | Sản phẩm ngừng bán trong danh sách yêu thích hiện nhãn "Ngừng bán", không có link thêm giỏ; giá/tồn không bị thay đổi | PASS |
| Xóa dọn dữ liệu | CASCADE khi xóa sản phẩm / khách chưa giao dịch | Wishlist tự dọn sạch khi xóa sản phẩm hoặc khách chưa có đơn hàng | PASS |
| Giao diện responsive 1280px và 375px | Playwright crawl & đo DOM | Không lỗi JS, không tràn trang, form POST đầy đủ CSRF, nút icon đúng chuẩn | PASS |
| Bộ kiểm thử mvnw test | Không skip | 243 test, 0 failure/error/skipped | PASS |

## Bằng chứng kiểm thử

- Migration: [021-first.txt](evidence/gd14/021-first.txt), [021-second.txt](evidence/gd14/021-second.txt)
- Snapshot dữ liệu: [before-021.json](evidence/gd14/before-021.json), [after-021-first.json](evidence/gd14/after-021-first.json), [after-021-second.json](evidence/gd14/after-021-second.json)
- Trình duyệt & Playwright: [browser-results.json](evidence/gd14/browser-results.json), [crawl-manifest.json](evidence/gd14/crawl-manifest.json)
- Luồng đánh giá & yêu thích: [feedback-sql.json](evidence/gd14/feedback-sql.json), [favorites-sql.json](evidence/gd14/favorites-sql.json), [favorite-stop-sql.json](evidence/gd14/favorite-stop-sql.json), [favorite-unique-db.txt](evidence/gd14/favorite-unique-db.txt), [ownership-delete-sql.json](evidence/gd14/ownership-delete-sql.json)
- Maven log tóm tắt: [maven-summary.txt](evidence/gd14/maven-summary.txt)
- Audit kiểm tra giao diện: [premium-audit-diff.json](evidence/gd14/premium-audit-diff.json)
