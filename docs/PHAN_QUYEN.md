# Phân quyền — Giai đoạn 12 & 30

Chính sách duy nhất: `security/AdminAccess.java`, được SecurityConfig và menu dùng chung. Người chưa đăng nhập chuyển /login; User bị 403 ở /admin/**. Admin toàn quyền. Nhân viên đăng nhập vào /admin/pos. Đổi vai trò có hiệu lực ở request tiếp theo; bắt đổi mật khẩu lần đầu vẫn được ưu tiên.

| Khu vực | Admin | Nhân viên |
|---|---|---|
| Bán hàng tại quầy | Toàn quyền | Được phép |
| Quản lý hóa đơn, xử lý trạng thái, xác nhận thanh toán, in | Toàn quyền | Được phép |
| Khách hàng, sổ địa chỉ, gửi lại tài khoản khách | Toàn quyền | Xem/thêm/sửa; không xóa hoặc vô hiệu hóa; không sửa/gửi lại tài khoản gắn quyền quản trị |
| Hỗ trợ khách hàng: Chat với khách | Toàn quyền | Được phép |
| Hỗ trợ khách hàng: Đánh giá | Toàn quyền | Ẩn menu và server 403 |
| Dashboard, Quản lý sản phẩm (Sản phẩm, Biến thể, Kích cỡ, Đế giày, Chất liệu, Màu sắc), Nhân viên, Khuyến mãi, Thống kê | Toàn quyền | Ẩn menu và server 403 |

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

## Giai đoạn 29 — Chat với khách

Nguồn quyền duy nhất vẫn là `security/AdminAccess.java`; cả menu và SecurityConfig dùng chung. `/admin/chat/**`, API cũ `/admin/support-chat/**` và kênh STOMP `/topic/support-staff` chỉ ROLE_ADMIN/ROLE_EMPLOYEE. Khách và ROLE_USER bị 403 ở REST và bị ERROR khi đăng ký kênh nhân viên. Mọi nhân viên được đọc/trả lời mọi hội thoại; phụ trách dùng assigned_user_id, không giới hạn quyền trả lời theo người nhận. Nhận/trả về/đóng/mở lại ghi audit người/thời gian. Không có xóa.

## Giai đoạn 30 — Gọn menu admin & Tích hợp Tồn kho

1. **Xóa mục Tồn kho**: Chức năng quản lý tồn kho và lịch sử kho được tích hợp trực tiếp vào **Danh sách biến thể** (`/admin/variants`) và trang xem chi tiết sản phẩm (`/admin/products-crud/view/{id}`). Toàn bộ các route `/admin/inventory/**` chuyển hướng 302 về `/admin/variants`.
2. **Chuyển "Đánh giá" vào nhóm "HỖ TRỢ KHÁCH HÀNG"**: Nhóm HỖ TRỢ KHÁCH HÀNG gồm "Chat với khách" và "Đánh giá". Nhân viên chỉ được xem "Chat với khách" (Đánh giá bị ẩn khỏi menu và chặn 403).
3. **Bỏ "Hồ sơ của tôi"**: Xóa mục khỏi sidebar và header Dashboard; giữ nguyên route `/admin/account` phục vụ đổi mật khẩu lần đầu / test email và nút "Đăng xuất" ở cuối sidebar / topbar.
4. **Menu accordion đồng bộ**: Tất cả các nhóm đều thu gọn độc quyền; mở nhóm này thì nhóm cùng cấp tự đóng; server-side render mở theo trang hiện tại (không khôi phục từ localStorage). Khi đóng hết nhóm, sidebar cao <= 720px không cần cuộn.
