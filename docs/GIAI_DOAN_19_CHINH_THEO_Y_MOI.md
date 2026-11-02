# BÁO CÁO NGHIỆM THU GIAI ĐOẠN 19: CHỈNH THEO Ý MỚI

**Nhánh**: `gop-y-sua-lan-2`  
**Database kiểm thử**: `shopbangiay_kt_20261102_gd18_sua` (bản sao restore từ file backup checkpoint; không ghi đè DB chính)  
**Thời gian hoàn thành**: 02/11/2026  

---

## 1. BẢNG TỔNG HỢP KẾT QUẢ NGHIỆM THU

| STT | Hạng mục | Cách kiểm tra | Kết quả thực tế | Đánh giá |
|:---:|:---|:---|:---|:---:|
| 1 | **Xóa breadcrumb "Quản trị / ..."** | Duyệt qua toàn bộ 15 trang admin (`/admin/dashboard`, `/admin/products-crud`, `/admin/categories`, v.v.) tại 1280px & 375px; tìm selector `.admin-breadcrumb`, `nav[aria-label="Đường dẫn"]` và chuỗi text `"Quản trị /"`. | 0 breadcrumb xuất hiện trên toàn bộ các trang admin, kể cả Dashboard. Nội dung dịch lên sát đầu trang. | **PASS** |
| 2 | **Header admin chỉ còn ở Dashboard** | Kiểm tra sự tồn tại của `#admin-header` (`.admin-topbar`) trên `/admin/dashboard` và tất cả các trang admin khác. | Header với text "Không gian quản trị", tên admin, "Hồ sơ của tôi", "Đăng xuất" CHỈ xuất hiện tại `/admin/dashboard`. Các trang khác không có header, không để khoảng trống thừa. | **PASS** |
| 3 | **Sidebar footer & Hamburger 375px** | Kiểm tra chân sidebar `.admin-sidebar-footer` có link "Hồ sơ của tôi" và form "Đăng xuất". Kiểm tra màn hình 375px trên các trang không có header có nút hamburger cố định `#admin-menu-toggle-fixed`. | Sidebar footer hiển thị cố định ở chân sidebar không bị che khuất; click logout hoạt động. Nút hamburger `☰` cố định hiển thị trên mobile, click mở/đóng sidebar mượt mà. | **PASS** |
| 4 | **Bỏ cấu hình vận chuyển shop (302 redirect)** | Truy cập GET `/admin/shipping-config` và `/admin/shipping-times`. | Cả hai URL trả về mã HTTP `302 Found`, chuyển hướng về `/admin/dashboard`. Mục menu "Cấu hình vận chuyển" đã bỏ khỏi sidebar. | **PASS** |
| 5 | **Đơn mới miễn phí ship shop (0đ) & dòng ghi chú** | Kiểm tra giỏ hàng (`/cart`), thanh toán (`/checkout`), chi tiết đơn khách, hóa đơn admin đối với đơn mới. | Đơn mới có `shipping_fee = 0`, tổng tiền bằng tiền hàng trừ giảm giá. Dòng "Phí giao hàng" không còn hiển thị; hiển thị ghi chú `"Phí vận chuyển do đơn vị vận chuyển tính và thu riêng"`. QR chuyển khoản đúng bằng tổng đơn. | **PASS** |
| 6 | **Dữ liệu lịch sử đơn cũ giữ nguyên phí** | Kiểm tra hóa đơn lịch sử có `shipping_fee > 0` trong database. | Hóa đơn lịch sử (ví dụ hóa đơn ID 478) vẫn hiển thị đầy đủ dòng phí giao hàng và tổng tiền gốc không đổi. Bảng và cột `orders.shipping_fee` được giữ nguyên. | **PASS** |
| 7 | **Công tắc bật/tắt thay cho nút xóa (Danh mục & Kích cỡ)** | Kiểm tra trang Thương hiệu (`/admin/categories`), Kích cỡ (`/admin/sizes`), Chất liệu, Màu sắc, Đế giày. Thao tác bật/tắt bằng AJAX. | Nút xóa đã chuyển thành công tắc dạng `<input type="checkbox" role="switch">`. Bấm tắt hiển thị hộp thoại xác nhận hậu quả thực tế; xác nhận xong chuyển sang trạng thái đã tắt (`row-muted`), badge "Đã tắt". Bấm bật lại lập tức kích hoạt lại. | **PASS** |
| 8 | **Công tắc bật/tắt (Biến thể & Sản phẩm & Khuyến mại)** | Kiểm tra trang Sản phẩm, Biến thể, Đợt giảm giá, Phiếu giảm giá. | Sử dụng fragment `fragments/trang-thai-ban.html` đồng nhất, hỗ trợ khóa bấm đúp (double-click lock), CSRF đầy đủ, cập nhật DOM qua AJAX không cần reload trang. | **PASS** |
| 9 | **Công tắc Khách hàng & Nhân viên** | Kiểm tra danh sách khách hàng (`/admin/customers`) và nhân viên (`/admin/employees`). | Sử dụng switch toggle. Hỗ trợ kích hoạt/vô hiệu hóa tức thì qua AJAX. | **PASS** |
| 10 | **Bảo vệ tài khoản bản thân & Quản trị viên duy nhất** | Admin đang đăng nhập thực hiện tắt chính mình; tắt admin khi chỉ còn 1 admin hoạt động. | Giao diện tự vô hiệu hóa switch (`disabled`) đối với chính tài khoản đang đăng nhập. Gửi request POST ép tắt trả về `400 Bad Request`. Backend chặn không cho tắt admin cuối cùng. | **PASS** |
| 11 | **Endpoint xóa cũ trả 405 MethodNotAllowed** | Gửi request POST tới các endpoint xóa cũ (`/admin/categories/delete/{id}`, `/admin/sizes/delete/{id}`, `/admin/materials/delete/{id}`, `/admin/colors/delete/{id}`, `/admin/soles/delete/{id}`, `/admin/variants/delete/{id}`, `/admin/promotions/delete/{id}`, `/admin/vouchers/delete/{id}`, `/admin/customers/delete/{id}`, `/admin/employees/delete/{id}`). | Toàn bộ 10 endpoint xóa cũ trả về mã HTTP `405 METHOD NOT ALLOWED`. | **PASS** |
| 12 | **Tái cấu trúc bảng Nhân viên** | Kiểm tra danh sách nhân viên (`/admin/employees`): thứ tự cột và bộ lọc Chức vụ. | Cột hiển thị đúng chuẩn: `STT \| Mã \| Họ tên \| SĐT \| Email \| Địa chỉ \| Chức vụ \| Trạng thái \| Hành động`. Cột CCCD và Tài khoản đã được loại bỏ. Cột Chức vụ suy từ role (`ROLE_ADMIN` -> "Admin", `ROLE_EMPLOYEE` -> "Nhân viên"). Bộ lọc Chức vụ lọc chính xác theo role. | **PASS** |
| 13 | **8 Kịch bản bảo mật** | Chạy kịch bản bảo mật: phân quyền truy cập, CSRF, SQL Injection, XSS, bảo vệ tài khoản, chặn MethodNotAllowed. | Toàn bộ 8 kịch bản bảo mật PASS 100%. | **PASS** |
| 14 | **Kiểm thử tự động Maven Test Suite** | Chạy `.\mvnw.cmd -B -o clean test` (không skip bất kỳ test nào). | **239/239 tests run, 0 failures, 0 errors, 0 skipped**. BUILD SUCCESS. | **PASS** |
| 15 | **Crawl toàn bộ link nội bộ (Playwright)** | Thu thập và truy cập toàn bộ link cho 4 role (Admin: 453 URLs, Employee: 199 URLs, User: 23 URLs, Guest: 20 URLs). | Không có link chết (0 lỗi 404/500), 100% form POST có token CSRF, không lỗi JavaScript console, không tràn ngang ở 1280px và 375px. | **PASS** |

---

## 2. CHỖ ĐÃ THAY ĐỔI SO VỚI GIAI ĐOẠN CŨ

| Khu vực | Giai đoạn cũ (GĐ 18 trở về trước) | Giai đoạn 19 (Ý mới) | Lý do thay đổi / Ghi chú |
|:---|:---|:---|:---|
| **Breadcrumb** | Xuất hiện dòng `Quản trị / ...` ở đầu các trang quản trị. | **Xóa bỏ hoàn toàn** trên tất cả các trang admin (kể cả Dashboard). | Tiết kiệm diện tích màn hình, giao diện gọn gàng. |
| **Thanh Header Admin** | Xuất hiện trên tất cả các trang admin, chứa nút mở menu, "Không gian quản trị", avatar và nút Đăng xuất. | **Chỉ còn xuất hiện tại `/admin/dashboard`**. Tất cả các trang admin khác không còn thanh header này, nội dung dịch lên sát mép trên. | Theo yêu cầu GĐ19 Mục A.2. |
| **Menu Mobile (<1024px, 375px)** | Dùng nút hamburger trên thanh topbar để mở sidebar. | Các trang không có header trang bị nút hamburger cố định `#admin-menu-toggle-fixed` (biểu tượng `☰`) góc trái. | Đảm bảo thao tác trên mobile ở mọi trang. |
| **Sidebar Footer** | Chân sidebar chỉ có phân nhóm thông thường. | Bổ sung khu vực `.admin-sidebar-footer` cố định ở chân sidebar với link "Hồ sơ của tôi" và nút "Đăng xuất". | Người dùng luôn truy cập được tài khoản và đăng xuất. |
| **Phí vận chuyển** | Cửa hàng tự tính theo khoảng cách GPS/tọa độ kho, phân bậc km, freeship từ 2 sản phẩm; có trang cấu hình `/admin/shipping-config`. | **Bỏ hoàn toàn việc tính phí vận chuyển của cửa hàng**. Đơn mới phí ship = 0đ; dòng phí giao hàng thay bằng ghi chú *"Phí vận chuyển do đơn vị vận chuyển tính và thu riêng"*. URL cấu hình 302 về Dashboard. | Chuyển giao tính và thu phí cho đơn vị vận chuyển thứ 3. |
| **Nút Xóa danh mục/kích cỡ/khách hàng/nhân viên/khuyến mại** | Nút "Xóa" thùng rác với form submit xóa vĩnh viễn. | **Chuyển thành công tắc bật/tắt (Toggle Switch)** có xác nhận hậu quả khi tắt, cập nhật trạng thái AJAX, dòng đã tắt hiển thị mờ (`row-muted`) và nhãn "Đã tắt". | Tránh mất dữ liệu, đồng nhất với trang Sản phẩm. |
| **Endpoint Xóa (DELETE/POST delete)** | Cho phép xóa hoặc báo lỗi khóa dữ liệu nếu có ràng buộc. | **Trả về mã HTTP `405 METHOD NOT ALLOWED`**. | Ngăn chặn hoàn toàn thao tác xóa từ client/API cũ. |
| **Bảng Nhân viên** | Có cột CCCD và cột Tài khoản. | **Bỏ cột CCCD và Tài khoản**. Thứ tự cột chuẩn: `STT \| Mã \| Họ tên \| SĐT \| Email \| Địa chỉ \| Chức vụ \| Trạng thái \| Hành động`. | Tối ưu hiển thị, tránh lộ thông tin nhạy cảm. |
| **Cột Chức vụ Nhân viên** | Lưu text tự do `position` trong bảng `employees`. | **Suy ra hoàn toàn từ quyền hệ thống (`Role`)**: `ROLE_ADMIN` -> "Admin", `ROLE_EMPLOYEE` -> "Nhân viên". Bộ lọc lọc theo role. | Chuẩn hóa vai trò hệ thống, tránh sai lệch phân quyền. |

---

## 3. BẰNG CHỨNG THỰC TẾ

### 3.1. Kết quả chạy Maven Test Suite (239 tests, 0 skipped)
```
[INFO] Results:
[INFO] 
[INFO] Tests run: 239, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  52.385 s
[INFO] Finished at: 2026-11-02T21:02:02+07:00
```

### 3.2. Kết quả Playwright & Bảo mật & Crawl
```
login-admin 1280 PASS 
login-employee 1280 PASS 
login-user 1280 PASS 
section-a-header-and-breadcrumb 1280 PASS 
section-a-header-and-breadcrumb 375 PASS 
section-a-mobile-hamburger-toggle 375 PASS 
section-b-shipping-config-redirect 1280 PASS 
section-b-cart-and-checkout-no-fee 1280 PASS 
section-b-historical-order-fee-retained 1280 PASS 
section-c-categories-switch 1280 PASS 
section-c-sizes-switch 1280 PASS 
section-c-customers-switch 1280 PASS 
section-d-employees-table-and-filter 1280 PASS 
security-s1-employee-access-dashboard-403 1280 PASS 
security-s2-employee-access-employees-403 1280 PASS 
security-s3-csrf-protection 1280 PASS 
security-s4-sql-injection-resilience 1280 PASS 
security-s5-xss-protection 1280 PASS 
security-s6-protect-self-account 1280 PASS 
security-s7-deprecated-delete-405 1280 PASS 
security-s8-protect-last-admin 1280 PASS 
All GD19 verification scenarios PASSED!
exhaustive-links-admin 1280 PASS 
exhaustive-links-employee 1280 PASS 
exhaustive-links-user 1280 PASS 
exhaustive-links-guest 1280 PASS 
CRAWL SUMMARY: [
  {"role": "admin", "exhausted": true, "count": 453},
  {"role": "employee", "exhausted": true, "count": 199},
  {"role": "user", "exhausted": true, "count": 23},
  {"role": "guest", "exhausted": true, "count": 20}
]
```

### 3.3. Bằng chứng Endpoint Xóa cũ trả 405 MethodNotAllowed
```json
[
  {"endpoint": "/admin/categories/delete/1", "status": 405},
  {"endpoint": "/admin/sizes/delete/1", "status": 405},
  {"endpoint": "/admin/materials/delete/1", "status": 405},
  {"endpoint": "/admin/colors/delete/1", "status": 405},
  {"endpoint": "/admin/soles/delete/1", "status": 405},
  {"endpoint": "/admin/variants/delete/1", "status": 405},
  {"endpoint": "/admin/promotions/delete/1", "status": 405},
  {"endpoint": "/admin/vouchers/delete/1", "status": 405},
  {"endpoint": "/admin/customers/delete/1", "status": 405},
  {"endpoint": "/admin/employees/delete/1", "status": 405}
]
```

### 3.4. Bằng chứng đơn hàng mới có phí ship = 0 và không hiển thị phí giao hàng
```sql
SELECT TOP 1 id, order_code, order_type, total_amount, discount_amount, shipping_fee, final_amount, shipping_note 
FROM orders ORDER BY id DESC;
-- Kết quả đơn mới:
-- shipping_fee = 0.00, final_amount = total_amount - discount_amount, shipping_note = N'Phí vận chuyển do đơn vị vận chuyển tính và thu riêng'
```

### 3.5. Danh sách ảnh chụp bằng chứng (Screenshots)
Toàn bộ ảnh chụp màn hình độ phân giải 1280px và 375px được lưu trữ tại `docs/evidence/gd19/`:
- `gd19-section-a-header-and-breadcrumb-1280.png`
- `gd19-section-a-header-and-breadcrumb-375.png`
- `gd19-section-a-mobile-hamburger-toggle-375.png`
- `gd19-section-b-shipping-config-redirect-1280.png`
- `gd19-section-b-cart-and-checkout-no-fee-1280.png`
- `gd19-section-b-historical-order-fee-retained-1280.png`
- `gd19-section-c-categories-switch-1280.png`
- `gd19-section-c-sizes-switch-1280.png`
- `gd19-section-c-customers-switch-1280.png`
- `gd19-section-d-employees-table-and-filter-1280.png`
- `gd19-security-s1-employee-access-dashboard-403-1280.png`
- `gd19-security-s2-employee-access-employees-403-1280.png`
- `gd19-security-s3-csrf-protection-1280.png`
- `gd19-security-s4-sql-injection-resilience-1280.png`
- `gd19-security-s5-xss-protection-1280.png`
- `gd19-security-s6-protect-self-account-1280.png`
- `gd19-security-s7-deprecated-delete-405-1280.png`
- `gd19-security-s8-protect-last-admin-1280.png`
- `gd19-exhaustive-links-admin-1280.png`
- `gd19-exhaustive-links-employee-1280.png`
- `gd19-exhaustive-links-user-1280.png`
- `gd19-exhaustive-links-guest-1280.png`

---

## 4. LỆNH CHẠY MIGRATION TRÊN DATABASE THẬT

> [!IMPORTANT]
> Không tự chạy trên DB thật trong quá trình kiểm thử. Khi triển khai lên database chính thức (`shopbangiay`), người quản trị chạy script sau:

```sql
-- Migration 022: Thêm cột trang_thai cho bảng sizes (idempotent)
IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_NAME = 'sizes' AND COLUMN_NAME = 'trang_thai'
)
BEGIN
    ALTER TABLE sizes ADD trang_thai BIT NOT NULL CONSTRAINT DF_sizes_trang_thai DEFAULT 1;
END;
GO
```
Script này đã được tích hợp sẵn vào file `database/migrations/022_kich_co_trang_thai.sql`, `database/migrations/chay_tat_ca.sql` và `shopgiay.sql`.
