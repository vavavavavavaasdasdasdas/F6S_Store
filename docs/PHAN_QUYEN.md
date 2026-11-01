# Phân quyền — Giai đoạn 12

Chính sách duy nhất: `security/AdminAccess.java`, được SecurityConfig và menu dùng chung. Người chưa đăng nhập chuyển /login; User bị 403 ở /admin/**. Admin toàn quyền. Nhân viên đăng nhập vào /admin/pos. Đổi vai trò có hiệu lực ở request tiếp theo; bắt đổi mật khẩu lần đầu vẫn được ưu tiên.

| Khu vực | Admin | Nhân viên |
|---|---|---|
| Bán hàng tại quầy, Bán online | Toàn quyền | Được phép (Bán online triển khai ở Giai đoạn 13) |
| Hóa đơn, xử lý trạng thái, xác nhận thanh toán, in | Toàn quyền | Được phép |
| Khách hàng, sổ địa chỉ, gửi lại tài khoản khách | Toàn quyền | Xem/thêm/sửa; không xóa hoặc vô hiệu hóa; không sửa/gửi lại tài khoản gắn quyền quản trị |
| Tồn kho | Toàn quyền | Chỉ GET danh sách, không điều chỉnh/lịch sử |
| Hồ sơ của tôi | Toàn quyền | Được phép |
| Dashboard, sản phẩm/thuộc tính, nhân viên, khuyến mãi, thống kê, hệ thống | Toàn quyền | Ẩn menu và server 403 |

API POST /admin/accounts/username-suggestion được phép cho Nhân viên để tạo khách; không cấp quyền /admin/accounts/** khác. Nhân viên chỉ thấy CCCD che; ô sửa trống giữ CCCD cũ. Chi tiết Admin hiện đủ theo yêu cầu; không đưa CCCD vào log/URL/in.

Chức vụ chỉ Admin/ Nhân viên, ánh xạ ROLE_ADMIN/ROLE_EMPLOYEE, không gõ tạo mới. Chặn tự hạ quyền hoặc tự khóa. Nhân viên có một địa chỉ trong form. Sổ khách dùng ShippingAddressService chung, khóa theo khách, tối đa ba ở service và DB, đổi mặc định đồng bộ customers.address.

## Các dòng migration 019 trên bản sao — để duyệt trước khi triển khai thật

| ID | Mã | Tên đăng nhập | Chức vụ trước → sau | Quyền trước → sau |
|---|---|---|---|---|
| 1 | NV001 | admin | Quản lý → Admin | ROLE_ADMIN → ROLE_ADMIN |
| 40 | NV-P7-1007 | p7.staff.1007 | Nhân viên bán hàng → Nhân viên | ROLE_ADMIN → ROLE_EMPLOYEE |
| 442 | P12-0845 | employee.p120845 | QA → Nhân viên | ROLE_ADMIN → ROLE_EMPLOYEE |
| 443 | P12-0850 | employee.p120850 | QA → Nhân viên | ROLE_ADMIN → ROLE_EMPLOYEE |
| 501 | P12-0900 | employee.p120900 | QA → Nhân viên | ROLE_ADMIN → ROLE_EMPLOYEE |

Chưa áp dụng trên DB thật. Ma trận này là tài liệu để xác nhận; bằng chứng tự động nằm trong báo cáo Giai đoạn 12.
