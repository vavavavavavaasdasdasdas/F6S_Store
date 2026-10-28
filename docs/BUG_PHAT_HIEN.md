# Lỗi phát hiện — kiểm thử 25/10/2026

Không còn lỗi chờ quyết định nghiệp vụ. Người dùng đã chọn chính sách mật khẩu và giữ tên riêng/thương hiệu; kết quả cuối tại [BAO_CAO_KIEM_THU.md](BAO_CAO_KIEM_THU.md).

## BUG-04 — Mật khẩu yếu chưa bị chặn

- Tái hiện Playwright: đăng ký qa_weak_1280, password=123, confirmPassword=123; form chuyển /login và báo thành công.
- Nguồn: src/main/java/com/example/demodatn/dto/nhanvien/RegisterRequest.java (password chỉ NotBlank); service/nhanvien/AuthService.java (không kiểm tra độ mạnh).
- Bằng chứng: verify/full-audit/weak-password-before.json, output/playwright/full-audit-weak-password-before.png.
- Trạng thái: Đã sửa theo quyết định người dùng: ít nhất 8 ký tự, có chữ và số, tối đa 72 byte UTF-8. 127 clean tests PASS; Playwright chặn 123 ở 1280/375px.
- Tài khoản thử chỉ nằm trên shopbangiay_kt_20261025; đã được loại bằng phục hồi checkpoint sau nhóm.

## BUG-05 — Nhãn chức năng tiếng Anh

SALE/Size/variant/voucher/Dashboard/Online/Email và mã quyền/kênh kỹ thuật còn lộ ở các trang/thông báo. Đã Việt hóa phần hiển thị, giữ identifier/route/enum và tên riêng/thương hiệu. Crawl cuối 92 lượt hai viewport và chạy lại 16 trang Admin, thông báo lưu/trùng kích cỡ đều PASS. Bằng chứng: evidence/kiem-thu/final/quality-all-results.json, final-labels-ui.json. Hai assertion chữ cũ được cập nhật; clean test đúng bản cuối 128/128.

## BUG-06 — Truy vấn N+1

ProductFilterService truy vấn biến thể từng sản phẩm; PricingService truy vấn khuyến mãi từng biến thể; POS tính cùng giá hai lần. Đã nạp gộp theo danh sách ID và dùng bản đồ giá trong một lần render, không cache qua request. SQL trang chủ 159→14, danh sách 63→8, thương hiệu 41→9, POS 50→9. HTML tám trang trước/sau giống nhau sau loại riêng token ngẫu nhiên; giá và thứ tự giữ nguyên. Bằng chứng: evidence/kiem-thu/performance. Đã commit 06ae1b0.

## BUG-07 — Hướng dẫn POS chuyển khoản không khớp hành vi

Mô tả cũ nói chờ xác nhận tại chi tiết đơn, trong khi code và test hiện có ghi POS là PAID ngay khi thanh toán. Chỉ sửa hướng dẫn thành xác nhận đã nhận đủ tiền trước khi thanh toán chuyển khoản; không đổi quy tắc thanh toán. Browser kiểm tra lại hai viewport PASS; xác nhận chuyển khoản riêng được kiểm thử bằng đơn Online. Bằng chứng: final-labels-ui.json, admin-orders-sql.json và pos-stock-ledger.json.

BUG-01 đến BUG-03 (schema/checkout, preflight và kiểu SĐT sau chạy lại migration) đã xử lý trong nhóm schema; xem bảng lỗi và metadata trong báo cáo chính. Không thực thi gì trên shopbangiay hoặc shopbangiay_thu.
