# BÁO CÁO THỰC HIỆN GIAI ĐOẠN 21: THIẾT KẾ LẠI "BÁN HÀNG TẠI QUẦY" VÀ XÓA TRANG "BÁN ONLINE"

Dự án: **F6S Store**  
Nhánh git: `gop-y-sua-lan-2`  
Cơ sở dữ liệu kiểm thử: `shopbangiay_kt_20261103_gd20` (SQL Server)  
Thời gian hoàn thành: 04/11/2026  

---

## MỤC LỤC
1. [Tóm tắt công việc hoàn thành](#1-tóm-tắt-công-việc-hoàn-thành)
2. [Chi tiết Mục A: Xóa trang Bán online](#2-chi-tiết-mục-a-xóa-trang-bán-online)
3. [Chi tiết Mục B: Thiết kế lại giao diện Bán hàng tại quầy (POS)](#3-chi-tiết-mục-b-thiết-kế-lại-giao-diện-bán-hàng-tại-quầy-pos)
4. [Cơ sở dữ liệu, Migration & Báo cáo doanh thu](#4-cơ-sở-dữ-liệu-migration--báo-cáo-doanh-thu)
5. [Kết quả kiểm thử & Minh chứng thực tế](#5-kết-quả-kiểm-thử--minh-chứng-thực-tế)

---

## 1. Tóm tắt công việc hoàn thành

- [x] **Xóa trang Bán online**: Bỏ mục menu "Bán online", cấu hình URL chuyển hướng 302 từ `/admin/ban-online` về `/admin/pos`, xóa hoàn toàn JavaScript chỉ dùng cho trang bán online (`don-online-pos.js`), gỡ các tùy chọn Nguồn đơn/sao chép tin nhắn/loại hóa đơn chờ Online.
- [x] **Thiết kế lại POS 2 cột desktop & 1 cột 375px**:
  - Cột trái (~2/3): Khối "Hóa đơn chờ" + Khối "Giỏ hàng".
  - Cột phải (~1/3): Thẻ "Khách hàng" + Thẻ "Thông tin đơn hàng".
  - Responsive 375px: Xếp dọc đúng thứ tự: Hóa đơn chờ -> Giỏ hàng -> Khách hàng -> Thông tin đơn hàng. Nút hamburger menu hoạt động mượt mà.
- [x] **Hóa đơn chờ**: Nút "+ Thêm hóa đơn chờ", bộ đếm "n/5 hóa đơn", hiển thị tab trực quan với số lượng món, tổng tiền, thời gian hết hạn và nút hủy. Tự động lưu giỏ hiện tại khi chuyển tab.
- [x] **Giỏ hàng**: Quét mã vạch camera (`jsQR`) và đầu đọc mã vạch quét phím (`code + Enter`), nút "+ Chọn sản phẩm" mở modal lọc động đa tiêu chí, phân trang AJAX. Bảng giỏ hàng hiển thị đầy đủ 12 cột, ô số lượng gõ tay trực tiếp được.
- [x] **Khách hàng**: Tìm kiếm nhanh live (tên hoặc SĐT >= 2 ký tự), mặc định "Khách lẻ", nút đổi/bỏ chọn, nút "+" thêm nhanh khách hàng (họ tên, SĐT bắt buộc; email tùy chọn cấp tài khoản).
- [x] **Thanh toán & Phương thức Kết hợp (COMBINED)**:
  - Tiền mặt: Ô khách đưa tiền gõ tay định dạng nghìn, tự động tính tiền thừa trả khách.
  - Chuyển khoản: Mã VietQR chuẩn cho số tiền chính xác, checkbox xác nhận đã nhận đủ tiền.
  - Kết hợp: Nhập số tiền mặt, số tiền chuyển khoản tự động tính phần còn lại; tạo mã VietQR chuyển khoản đúng phần tiền chuyển khoản; checkbox xác nhận đã nhận đủ tiền.
  - Ghi chú thanh toán lưu vào đơn hàng (`orders.ghi_chu_thanh_toan`).
- [x] **Dữ liệu & Doanh thu**: Thêm cột `tien_mat`, `tien_chuyen_khoan`, `ghi_chu_thanh_toan` vào `orders`; bù đắp số liệu lịch sử; cập nhật thống kê doanh thu Dashboard tách biệt chính xác tiền mặt và tiền chuyển khoản.
- [x] **Kiểm thử tự động**: 100% test tích hợp Maven xanh (`InvoiceOnlineWorkspaceIntegrationTest`), Playwright test tự động toàn bộ 14 kịch bản tạo đơn thành công, chụp 7 ảnh màn hình minh chứng.

---

## 2. Chi tiết Mục A: Xóa trang Bán online

### Những thành phần đã được gỡ bỏ:
1. **Menu điều hướng**: Gỡ mục menu "Bán online" khỏi thanh điều hướng quản trị và `AdminPageAdvice.java`.
2. **Nút bấm**: Gỡ nút "Bán online" khỏi giao diện Danh sách hóa đơn (`hoa-don/danh-sach.html`).
3. **Mã nguồn Frontend**: Xóa file `src/main/resources/static/js/don-online-pos.js`.
4. **Controller & Endpoint**: Gỡ logic xử lý riêng của Bán online trong `AdminPosController.java`, thay thế URL `/admin/ban-online` bằng mã chuyển hướng HTTP 302 về `/admin/pos`.
5. **Dữ liệu tạm**: Migration SQL xóa toàn bộ hóa đơn chờ loại Online (`DELETE FROM pos_held_orders WHERE order_type = 'ONLINE'`).
6. **Bảo toàn dữ liệu lịch sử**: Tất cả đơn hàng cũ có nguồn gốc Online/Bán online và hóa đơn liên quan được giữ nguyên vẹn 100%.

---

## 3. Chi tiết Mục B: Thiết kế lại giao diện Bán hàng tại quầy (POS)

### 3.1. Bố cục 2 cột Desktop & Thứ tự hiển thị 375px Mobile
- **Desktop (>= 992px)**:
  - Cột trái chiếm ~65% chiều rộng màn hình, chứa:
    1. **Khối Hóa đơn chờ**: Quản lý đa tab hóa đơn chờ.
    2. **Khối Giỏ hàng**: Bảng sản phẩm, công cụ quét mã và chọn sản phẩm.
  - Cột phải chiếm ~35% chiều rộng màn hình, dạng sticky gọn gàng, chứa:
    1. **Thẻ Khách hàng**: Định danh khách mua.
    2. **Thẻ Thông tin đơn hàng & Thanh toán**: Tính tiền, áp mã giảm giá, lựa chọn phương thức thanh toán.
- **Mobile (375px)**:
  - Áp dụng cấu trúc Flexbox `display: contents` trên container phụ kết hợp thứ tự tường minh:
    - `order: 1`: Khối Hóa đơn chờ
    - `order: 2`: Khối Giỏ hàng
    - `order: 3`: Thẻ Khách hàng
    - `order: 4`: Thẻ Thông tin đơn hàng & Thanh toán
  - Nút hamburger mở menu quản trị cố định góc trên cùng bên trái.

### 3.2. Chức năng chi tiết từng khối

#### 1. Hóa đơn chờ (Held Orders)
- Nút `+ Thêm hóa đơn chờ` và nhãn đếm `n/5 hóa đơn`.
- Giới hạn tối đa 5 hóa đơn chờ trên mỗi tài khoản nhân viên.
- Mỗi tab hiển thị STT (`#1`, `#2`), mã hóa đơn chờ, số lượng món, tổng tiền, đếm ngược thời gian hết hạn và nút `×` để hủy.
- Bấm vào tab để chuyển đổi giỏ hàng (hệ thống tự động lưu giỏ hiện tại trước khi chuyển).
- Cơ chế kiểm tra tồn kho và xác nhận lại giá khi mở lại hóa đơn chờ.

#### 2. Giỏ hàng & Hộp thoại Chọn sản phẩm
- Tiêu đề "Giỏ hàng" kèm badge số lượng sản phẩm.
- Nút icon `Quét mã`: Hỗ trợ camera (qua thư viện `jsQR`) và đầu đọc mã vạch quét phím bấm `Enter`. Nếu mã biến thể hoặc sản phẩm có đúng 1 biến thể thì tự thêm vào giỏ; nếu sản phẩm nhiều biến thể thì thông báo rõ và hướng dẫn mở hộp thoại chọn.
- Nút `+ Chọn sản phẩm`: Mở modal danh sách sản phẩm với các bộ lọc linh hoạt (Từ khóa, Danh mục, Màu sắc, Chất liệu, Đế giày, Kích cỡ, Khoảng giá) và phân trang động AJAX. Dữ liệu render qua `ModalVariantDto` đảm bảo an toàn tuyệt đối, không dính lỗi SPEL.
- Bảng giỏ hàng hiển thị chuẩn 12 cột: `STT | Mã SP | Mã biến thể | Tên sản phẩm | Màu sắc | Chất liệu | Đế giày | Kích cỡ | Số lượng | Đơn giá | Thành tiền | Thao tác (xóa)`.
- Ô số lượng cho phép **gõ tay trực tiếp**, tự động cập nhật giỏ hàng và kiểm tra tồn kho.

#### 3. Thẻ Khách hàng
- Ô tìm kiếm khách hàng bằng Tên hoặc Số điện thoại (tìm kiếm live khi gõ từ 2 ký tự trở lên).
- Mặc định là "Khách lẻ". Khi đã chọn khách hàng, hiển thị khung thông tin gọn gàng với nút "Đổi / Bỏ chọn".
- Nút `+` mở hộp thoại **Thêm khách hàng nhanh**:
  - Họ tên, Số điện thoại (bắt buộc).
  - Thư điện tử (tùy chọn): Nếu có nhập email thì tự động cấp tài khoản đăng nhập và gửi email mật khẩu tạm qua `AccountProvisioningService`; nếu để trống email thì chỉ lưu thông tin khách hàng mà không tạo tài khoản người dùng (`user_id = NULL`, `email = NULL`).

#### 4. Thẻ Thông tin đơn hàng & Phương thức Thanh toán
- Dòng tổng tiền hàng (n sản phẩm).
- Ô nhập mã giảm giá và nút "Áp dụng", hiển thị dòng giảm giá màu xanh khi hợp lệ.
- Tổng thanh toán hiển thị to, nổi bật.
- 3 nút chọn phương thức thanh toán dạng nút bấm phẳng:
  - **Tiền mặt**: Ô nhập "Khách thanh toán" gõ tay định dạng hàng nghìn, tự động tính "Tiền thừa trả khách", khóa nút thanh toán nếu tiền khách đưa chưa đủ.
  - **Chuyển khoản**: Hiển thị khung VietQR với mã QR thanh toán động cho đúng tổng tiền, checkbox bắt buộc "Đã nhận đủ tiền chuyển khoản".
  - **Kết hợp**:
    - Nhập số tiền mặt (gõ tay có định dạng).
    - Ô tiền chuyển khoản tự động tính phần còn lại (`Tổng - Tiền mặt`). Cả 2 số tiền phải > 0 và tổng bằng đúng tổng thanh toán.
    - Hiển thị mã VietQR chuyển khoản tương ứng đúng số tiền chuyển khoản cần thu.
    - Checkbox bắt buộc "Đã nhận đủ tiền chuyển khoản".
- Ô "Ghi chú thanh toán" đa dòng, lưu vào `orders.ghi_chu_thanh_toan`.
- Hai nút chân thẻ: "Hủy hóa đơn" (xóa giỏ hiện tại có xác nhận) và "Thanh toán" (tự động khóa khi giỏ trống hoặc thanh toán chưa hợp lệ).

---

## 4. Cơ sở dữ liệu, Migration & Báo cáo doanh thu

### 4.1. File migration `database/migrations/023_thanh_toan_ket_hop_pos.sql`
- Thêm cột `tien_mat BIGINT NOT NULL DEFAULT 0` vào bảng `orders`.
- Thêm cột `tien_chuyen_khoan BIGINT NOT NULL DEFAULT 0` vào bảng `orders`.
- Thêm cột `ghi_chu_thanh_toan NVARCHAR(500) NULL` vào bảng `orders`.
- Cập nhật ràng buộc kiểm tra `ck_pos_hold_type_payment` trên `pos_held_orders` cho phép phương thức `COMBINED`.
- Bù dữ liệu lịch sử đơn hàng đã thanh toán:
  - Đơn `CASH`/`COD`: `tien_mat = final_amount`, `tien_chuyen_khoan = 0`.
  - Đơn `TRANSFER`/`BANK_TRANSFER`: `tien_chuyen_khoan = final_amount`, `tien_mat = 0`.
  - Đơn chưa thanh toán: để 0.
- Xóa sạch hóa đơn chờ loại `ONLINE`.

### 4.2. Báo cáo doanh thu Dashboard
- Cập nhật `DashboardRepository.java` và `StatisticsService.java`:
  - Doanh thu tiền mặt tính bằng: `SUM(o.tien_mat)` của các đơn `PAID`.
  - Doanh thu chuyển khoản tính bằng: `SUM(o.tien_chuyen_khoan)` của các đơn `PAID`.
  - Đảm bảo các đơn thanh toán kết hợp phân bổ chính xác doanh thu theo từng kênh.

---

## 5. Kết quả kiểm thử & Minh chứng thực tế

### 5.1. Kiểm thử tích hợp tự động (Maven Test)
- Kiểm thử `InvoiceOnlineWorkspaceIntegrationTest`: **5/5 tests passed** (0 failures, 0 errors).
- Kiểm tra tính bảo mật và mã chuyển hướng HTTP 302 của `/admin/ban-online` về `/admin/pos`.

### 5.2. Minh chứng thực thi đơn hàng trong Cơ sở dữ liệu (Order ID: 1351)
```sql
SELECT id, order_code, order_type, status, total_amount, final_amount, payment_method, payment_status, tien_mat, tien_chuyen_khoan, ghi_chu_thanh_toan 
FROM orders WHERE id = 1351;
```
**Kết quả thực tế từ DB:**
- `id`: `1351`
- `order_code`: `DH001351`
- `order_type`: `POS`
- `status`: `CONFIRMED`
- `total_amount`: `4.000.000 đ`
- `final_amount`: `4.000.000 đ`
- `payment_method`: `COMBINED`
- `payment_status`: `PAID`
- `tien_mat`: `2.000.000 đ`
- `tien_chuyen_khoan`: `2.000.000 đ`
- `ghi_chu_thanh_toan`: `Ghi chú đơn quầy test kết hợp GĐ21`
- `da_tru_kho`: `1` (Tồn kho sản phẩm giảm chính xác 2 đôi, từ 3 xuống 1).

### 5.3. Danh sách ảnh chụp màn hình minh chứng (`docs/images/gd21/screenshots/`)
1. `pos_desktop_1280.png`: Giao diện Bán hàng tại quầy hoàn chỉnh 2 cột trên desktop 1280px (4 thẻ bo góc hiện đại).
2. `pos_modal_products_1280.png`: Hộp thoại Chọn sản phẩm với bộ lọc động và bảng phân trang.
3. `pos_mobile_375.png`: Bố cục dọc trên màn hình di động 375px (Hóa đơn chờ -> Giỏ hàng -> Khách hàng -> Thông tin đơn hàng).
4. `pos_payment_cash.png`: Phương thức Tiền mặt với tính năng định dạng tiền và tính tiền thừa tự động.
5. `pos_payment_transfer.png`: Phương thức Chuyển khoản với mã VietQR tạo động.
6. `pos_payment_combined.png`: Phương thức Kết hợp tự động phân bổ tiền mặt và tiền chuyển khoản kèm mã QR cho phần chuyển khoản.
7. `pos_success_page.png`: Trang thanh toán thành công chuyển hướng sau khi hoàn tất đơn hàng POS.
