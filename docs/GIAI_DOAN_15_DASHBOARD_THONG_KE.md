# Giai đoạn 15 – Dashboard và Thống kê

Nhánh `gop-y-dot-moi`. Đã thực hiện đầy đủ các hạng mục của Giai đoạn 15 cho màn hình **Dashboard** (`/admin/dashboard`) và **Thống kê** (`/admin/statistics`).

| Hạng mục | Cách kiểm tra | Kết quả thực tế | PASS/FAIL/NOT TESTED |
|---|---|---|---|
| Luật doanh thu & phương thức thanh toán | Truy vấn DB `SELECT DISTINCT payment_method FROM orders` và kiểm chứng `cashRevenue + transferRevenue = totalRevenue` | Các phương thức thanh toán trong DB gồm 4 giá trị: `CASH`, `COD` (nhóm Tiền mặt) và `BANK_TRANSFER`, `TRANSFER` (nhóm Chuyển khoản). Điều kiện doanh thu giữ nguyên (`o.payment_status='PAID' AND o.status NOT IN ('CANCELLED','DELIVERY_FAILED')`, trừ phí ship khi `shipping_policy_version >= 1`). Luôn thỏa mãn `Tiền mặt + Chuyển khoản = Tổng doanh thu kỳ` trên mọi bộ lọc | PASS |
| Bộ lọc ngày trên Dashboard | Lọc mặc định (7 ngày), 30 ngày, khoảng ngày tùy chọn và khoảng trống | Bộ lọc ngày (`from`, `to`, `days`) áp dụng cho toàn bộ các thẻ số và mọi khối dữ liệu trên trang Dashboard; liên kết đơn hàng trỏ sang `/admin/invoices` | PASS |
| Lược bỏ khối thừa trên Dashboard | Kiểm tra DOM `/admin/dashboard` | Đã bỏ hoàn toàn biểu đồ/bảng "Doanh thu theo ngày" và bảng "Đơn hàng gần đây" khỏi Dashboard | PASS |
| Top sản phẩm bán chạy (Dashboard) | Đối chiếu SQL `SELECT TOP (5)` theo bộ lọc ngày | Hiển thị Top 5 sản phẩm bán chạy theo số lượng trong kỳ lọc với đầy đủ cột: `STT \| Ảnh \| Mã \| Tên sản phẩm \| Số lượng \| Doanh thu`; tự động thay đổi theo bộ lọc ngày | PASS |
| Khách hàng tiềm năng (Top khách hàng) | Đối chiếu SQL `JOIN customers` theo tổng chi tiêu trong kỳ | Hiển thị Top 5 khách hàng theo tổng chi tiêu trong kỳ lọc: `STT \| Mã KH \| Họ tên \| Số đơn \| Tổng chi tiêu \| Hành động` (nút icon xem chi tiết khách hàng); khách vãng lai (`customer_id IS NULL`) không xếp hạng | PASS |
| Khối Đánh giá sản phẩm (Dashboard) | Đối chiếu SQL trên bảng `danh_gia` (`hien_thi = 1`) | Hiển thị điểm trung bình toàn cửa hàng, tổng số đánh giá, Top 5 sản phẩm điểm cao (có từ 3 đánh giá trở lên), 5 đánh giá mới nhất và liên kết tới `/admin/reviews` | PASS |
| Khối Sản phẩm yêu thích (Dashboard) | Đối chiếu SQL trên bảng `yeu_thich` trong kỳ lọc | Hiển thị tổng lượt yêu thích và Top 5 sản phẩm được yêu thích nhiều nhất theo ngày thêm trong kỳ lọc | PASS |
| Sản phẩm sắp hết hàng (Dashboard) | Kiểm tra biến thể `quantity <= threshold` kèm `EXISTS inventory_logs` trong kỳ | Chỉ hiển thị biến thể đang bán có tồn hiện tại `<= ngưỡng` và có phát sinh biến động kho (`inventory_logs`) trong khoảng ngày lọc; giữ khối Đơn hàng theo trạng thái | PASS |
| Gộp trang Thống kê duy nhất (`/admin/statistics`) | Kiểm tra menu THỐNG KÊ và bố cục `/admin/statistics` | Menu THỐNG KÊ chỉ còn 1 mục duy nhất dẫn tới `/admin/statistics`. Trang hiển thị đúng thứ tự 7 khối từ trên xuống: (1) Bộ lọc chung cố định đầu trang, (2) Tóm tắt doanh thu (Tổng, Số đơn, Trung bình, Tiền mặt, Chuyển khoản), (3) Doanh thu theo kênh (Online/POS), (4) Đơn theo trạng thái, (5) Top sản phẩm theo số lượng, (6) Top sản phẩm theo doanh thu, (7) Top khách hàng. Đã bỏ biểu đồ/bảng Doanh thu theo ngày | PASS |
| Phân trang độc lập & Chuyển hướng 302 | Kiểm tra `quantityPage`, `revenuePage`, `customerPage`, tham số lạ và URL cũ | Mỗi bảng xếp hạng có phân trang riêng giữ nguyên bộ lọc chung; truyền `page=-1`, `abc`, `999999` không lỗi 500; các URL cũ (`/admin/statistics/revenue`, `/admin/statistics/best-sellers`, `/admin/reports/*`) chuyển hướng 302 về `/admin/statistics` | PASS |
| Phân quyền Nhân viên (Giai đoạn 12) | Đăng nhập / MockMvc với `ROLE_EMPLOYEE` và `ROLE_USER` | Nhân viên (`ROLE_EMPLOYEE`) và khách hàng (`ROLE_USER`) truy cập `/admin/dashboard` hoặc `/admin/statistics` đều bị chặn HTTP 403 | PASS |
| Giao diện responsive 1280px & 375px, không lỗi JS | Kiểm tra Chart.js & UI audit strict | Không có lỗi JS khi có hoặc không có dữ liệu biểu đồ; đầy đủ empty state cho khoảng không có dữ liệu; 0 lỗi phát sinh trong UI audit (`new: 0`) | PASS |
| Bộ kiểm thử `./mvnw test` | Chạy toàn bộ test suite trên DB kiểm thử sạch | 248 test, 0 failure, 0 error, 0 skipped | PASS |

## Đối chiếu số liệu độc lập (`Số trên trang | SELECT độc lập`)

Chi tiết JSON đối chiếu tự động được lưu tại [dashboard-statistics-sql.json](evidence/gd15/dashboard-statistics-sql.json):

- **Dashboard (7 ngày: `2026-10-27` → `2026-11-02`)**:
  - Tổng doanh thu kỳ: `1.200.000 ₫` | `SELECT COALESCE(SUM(...),0) FROM orders o WHERE ...` = `1200000.00`
  - Doanh thu tiền mặt (`CASH`, `COD`): `500.000 ₫` | `SELECT COALESCE(SUM(...),0) ... AND o.payment_method IN ('CASH','COD')` = `500000.00`
  - Doanh thu chuyển khoản (`BANK_TRANSFER`, `TRANSFER`): `700.000 ₫` | `SELECT COALESCE(SUM(...),0) ... AND o.payment_method IN ('BANK_TRANSFER','TRANSFER')` = `700000.00`
  - Số đơn hàng: `2` | `SELECT COUNT_BIG(*) FROM orders WHERE ...` = `2`
  - Kiểm chứng: `500.000 + 700.000 = 1.200.000 ₫` (Khớp 100%)
- **Dashboard (30 ngày: `2026-10-04` → `2026-11-02`)**:
  - Tổng doanh thu kỳ: `28.230.000 ₫` | `SELECT` độc lập = `28230000.00`
  - Doanh thu tiền mặt: `17.690.000 ₫` | `SELECT` độc lập = `17690000.00`
  - Doanh thu chuyển khoản: `10.540.000 ₫` | `SELECT` độc lập = `10540000.00`
  - Số đơn hàng: `31` | `SELECT` độc lập = `31`
  - Kiểm chứng: `17.690.000 + 10.540.000 = 28.230.000 ₫` (Khớp 100%)
- **Thống kê theo từng kênh (`ALL`, `ONLINE`, `POS`) & từng thương hiệu**:
  - Kênh `ALL`: Tổng `28.230.000 ₫` = Tiền mặt `17.690.000 ₫` + Chuyển khoản `10.540.000 ₫` (13 đơn đủ điều kiện) | `SELECT` độc lập = `28230000.00`
  - Kênh `ONLINE`: Tổng `6.050.000 ₫` = Tiền mặt `5.150.000 ₫` + Chuyển khoản `900.000 ₫` (5 đơn đủ điều kiện) | `SELECT` độc lập = `6050000.00`
  - Kênh `POS`: Tổng `22.180.000 ₫` = Tiền mặt `12.540.000 ₫` + Chuyển khoản `9.640.000 ₫` (8 đơn đủ điều kiện) | `SELECT` độc lập = `22180000.00`
  - Thương hiệu A: Tổng `500.000 ₫` (`500.000 ₫` tiền mặt + `0 ₫` chuyển khoản, 1 đơn)
  - Thương hiệu B: Tổng `1.600.000 ₫` (`0 ₫` tiền mặt + `1.600.000 ₫` chuyển khoản, 2 đơn)

## Bằng chứng kiểm thử

- Đối chiếu SQL & trang: [dashboard-statistics-sql.json](evidence/gd15/dashboard-statistics-sql.json)
- Maven test log & tóm tắt: [maven-test.log](evidence/gd15/maven-test.log), [maven-summary.txt](evidence/gd15/maven-summary.txt)
- UI Audit strict diff: [premium-audit-diff.json](evidence/gd15/premium-audit-diff.json)
