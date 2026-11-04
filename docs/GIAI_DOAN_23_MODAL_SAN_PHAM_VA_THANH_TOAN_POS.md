# BÁO CÁO KẾT QUẢ GIAI ĐOẠN 23: HỘP THOẠI "DANH SÁCH SẢN PHẨM" VÀ THANH TOÁN CỦA TRANG BÁN HÀNG TẠI QUẦY

## 1. TỔNG QUAN YÊU CẦU ĐÃ HOÀN THÀNH

Giai đoạn 23 tập trung vào việc tinh chỉnh toàn diện hộp thoại "Danh sách sản phẩm" và luồng thanh toán tại POS:

1. **Mục A - Thanh lọc**:
   - Sửa nhãn "Danh mục" thành **"Thương hiệu"**.
   - Mọi combobox đều có placeholder `"Tất cả"`, chiều cao đồng nhất `35px`, chiều rộng bố cục `col-6 col-md-3 col-lg-2` thoáng mắt, không bị che khuất hay cắt chữ ("Tất cả mức giá" -> các mức giá rõ ràng).

2. **Mục B - Bảng sản phẩm**:
   - Bỏ cột gộp "Tên sản phẩm & Mã".
   - Chuẩn hóa đúng 13 cột:
     `STT | Mã SP | Mã biến thể | Tên sản phẩm | Ảnh | Màu sắc | Chất liệu | Đế giày | Kích cỡ | Tồn kho | Giá bán | Khuyến mãi | Thao tác`.
   - Cột **Màu sắc** có ô màu mẫu tròn (`pos-color-dot`) trực quan cạnh tên màu.

3. **Mục C - Nút thao tác**:
   - Đổi icon thành dấu cộng (`+`), có tooltip và `aria-label="Thêm vào giỏ"`.
   - Ngăn chặn triệt để việc icon tự động của hệ thống quản trị (`nut-hanh-dong.js`) ghi đè thành icon checkmark (✓).

4. **Mục D - Tồn kho sau khi thêm & Tồn khả dụng**:
   - "Tồn kho" trong modal hiển thị là **TỒN KHẢ DỤNG** = `tồn thật trong DB - số lượng biến thể đó trong giỏ POS hiện tại`.
   - Bấm `+` thêm vào giỏ: tồn khả dụng trên dòng GIẢM ngay qua AJAX, modal không bị đóng, không reload, giữ nguyên bộ lọc và vị trí cuộn.
   - Khi tồn khả dụng = 0: nút thao tác bị khóa, đổi thành nhãn `Hết hàng`.
   - Xóa dòng trong giỏ hoặc giảm số lượng: tồn khả dụng tự động phục hồi.
   - Chuyển tab hóa đơn chờ: tồn khả dụng tự động tính theo giỏ của tab đang kích hoạt.
   - **Tồn kho thật trong DB KHÔNG bị trừ** khi người dùng thêm vào giỏ hàng (giữ đúng quy tắc nghiệp vụ: chỉ trừ kho thật khi giao dịch thanh toán thành công).
   - Kiểm tra tồn kho trước khi thanh toán: nếu có xung đột (người khác mua trước dẫn tới thiếu hàng trong DB), hệ thống rollback toàn bộ hóa đơn và báo rõ dòng sản phẩm thiếu hàng.

5. **Mục E - Giá bán và Khuyến mãi**:
   - Cột "Giá bán": hiển thị giá ĐÃ TRỪ khuyến mãi (màu đỏ đậm), giá gốc gạch ngang (`<del>`), badge `-X%` hoặc `-X đ`.
   - Cột "Khuyến mãi": hiển thị tên đợt giảm giá (hoặc `—` nếu không có).
   - Giá được đồng bộ từ `PricingService`, khớp hoàn toàn 100% với giá hiển thị trên trang cửa hàng (Storefront) và dữ liệu trong CSDL.
   - Thẻ "Thông tin đơn hàng": hiển thị rõ `Tổng tiền hàng` (giá gốc), `Giảm khuyến mãi` (nếu > 0), `Giảm phiếu giảm giá` (kèm mã nếu áp dụng), `Tổng thanh toán`.

6. **Mục F - Điều hướng sau khi thanh toán**:
   - Khi thanh toán thành công, hệ thống chuyển thẳng đến trang chi tiết hóa đơn `/admin/invoices/{id}` kèm thông báo thành công.
   - Trang chi tiết có sẵn 2 nút: **"In hóa đơn"** và **"Bán đơn mới"** (dẫn về `/admin/pos`).
   - Đường dẫn cũ `/admin/pos/success/{id}` trả về redirect 302 chuyển hướng về `/admin/invoices/{id}`.
   - Tài khoản có vai trò Nhân viên (`ROLE_EMPLOYEE`) được phân quyền truy cập đầy đủ vào trang chi tiết hóa đơn.

7. **Mục G - Kiểm thử tự động (Playwright & Integration Tests)**:
   - Đã thực hiện kiểm thử tự động toàn diện qua Playwright browser và Spring Boot integration tests:
     - Thêm cùng 1 biến thể 3 lần: tồn khả dụng giảm từ 10 -> 7, query DB tồn thật vẫn nguyên 10.
     - Xóa dòng giỏ: tồn khả dụng phục hồi từ 7 -> 10.
     - Hết hàng: nút đổi thành "Hết hàng" và bị disabled.
     - Chuyển hóa đơn chờ: giỏ tab mới trống, tồn khả dụng phản ánh đúng tab mới.
     - Thanh toán thành công: chuyển đúng `/admin/invoices/{id}`, kho thật DB trừ đúng 1 lần từ 10 xuống 9.
     - Khuyến mãi % và số tiền khớp 100% giữa POS, Storefront và CSDL.
     - Thiếu hàng do mua trước: rollback hoàn toàn, hiển thị alert cảnh báo thiếu hàng.
     - Đã chụp ảnh hộp thoại tại độ phân giải Desktop 1280px và Mobile 375px.

---

## 2. KẾT QUẢ KIỂM THỬ PLAYWRIGHT & TRUY VẤN CƠ SỞ DỮ LIỆU

### 2.1. Thêm 3 lần và Tồn kho thật trong DB
- **Tồn ban đầu**: `10`
- **Sau 3 lần bấm `+`**: Tồn khả dụng trên modal hiển thị `7`.
- **Truy vấn DB kiểm tra tồn thật**:
  ```sql
  SELECT id, ma, quantity FROM product_variants WHERE id = 3;
  -- Kết quả: id = 3, ma = BT0003, quantity = 10 (KHÔNG ĐỔI)
  ```

### 2.2. Xóa dòng khỏi giỏ hàng
- Bấm nút xóa dòng giỏ hàng: giỏ hàng trống, tồn khả dụng trên modal tức thì trở về `10`.

### 2.3. Hết hàng khóa nút
- Chọn biến thể ID = 8 (Nike Air Max, tồn kho = 1).
- Bấm `+` thêm vào giỏ: tồn khả dụng hiển thị `0`, nút thao tác đổi thành `Hết hàng` và `disabled = true`.

### 2.4. Chuyển hóa đơn chờ
- Bấm `+ Thêm hóa đơn chờ` để lưu hóa đơn hiện tại và chuyển sang hóa đơn mới:
- Tại tab mới, giỏ hàng trống, biến thể ID = 8 có tồn khả dụng là `1`, nút thao tác là `+` khả dụng.

### 2.5. Thanh toán thành công & Trừ kho thật
- Thực hiện thanh toán tiền mặt (khách đưa 3.000.000 đ cho đơn 2.975.000 đ):
- Chuyển hướng đến: `http://localhost:8080/admin/invoices/920`.
- Thông báo: *"Thanh toán thành công! Mã hóa đơn: HD001406"*.
- Hiển thị đầy đủ nút: `In hóa đơn`, `Bán đơn mới`, `Danh sách`.
- **Truy vấn DB kiểm tra tồn kho thật sau thanh toán**:
  ```sql
  SELECT id, ma, quantity FROM product_variants WHERE id = 3;
  -- Kết quả: quantity = 9 (Đã trừ đúng 1 sản phẩm)
  ```

### 2.6. Khớp giá khuyến mãi với CSDL và Storefront
- **CSDL**:
  ```sql
  SELECT p.name, p.discount_type, p.discount_value, pr.name, pv.price 
  FROM promotions p 
  JOIN promotion_details pd ON pd.promotion_id = p.id 
  JOIN products pr ON pr.id = pd.product_id 
  JOIN product_variants pv ON pv.product_id = pr.id 
  WHERE pv.id = 3;
  -- Kết quả: Đợt "Khuyến mại mùa hè", PERCENT 15%, Giá gốc 3.500.000 đ -> Giảm còn 2.975.000 đ
  ```
- **Storefront (`/products/2`)**:
  - Hiển thị: `2.975.000 ₫ 3.500.000 ₫ GIẢM -15%`
- **POS Modal**:
  - Cột Giá bán: `2.975.000 đ` (đỏ), `<del>3.500.000 đ</del>`, badge `-15%`.
  - Cột Khuyến mãi: `Khuyến mãi mùa hè`.

### 2.7. Xử lý xung đột tồn kho (Rollback)
- Giỏ hàng POS có sản phẩm ID = 8.
- CSDL mô phỏng người khác mua trước: `UPDATE product_variants SET quantity = 0 WHERE id = 8`.
- Bấm thanh toán tại POS:
  - Hệ thống phát hiện thiếu hàng, kích hoạt rollback giao dịch.
  - Chuyển hướng về `/admin/pos` kèm alert: *"Sản phẩm đã ngừng bán hoặc không đủ tồn kho: Nike Air Max"*.
  - Không có hóa đơn nào được tạo.

---

## 3. DANH SÁCH FILE THAY ĐỔI

1. `src/main/java/com/example/demodatn/dto/donhang/PriceInfo.java`: Bổ sung `promotionName`, `discountType`, `discountAmount`, `discountLabel`.
2. `src/main/java/com/example/demodatn/dto/donhang/CartItemDto.java`: Bổ sung các trường giá gốc, khuyến mãi, tính tổng tiền hàng trước và sau giảm.
3. `src/main/java/com/example/demodatn/service/donhang/PricingService.java`: Bổ sung tính toán chi tiết nhãn khuyến mãi (`-X%` / `-X đ`), tên đợt áp dụng.
4. `src/main/java/com/example/demodatn/controller/admin/donhang/AdminPosController.java`:
   - `ModalVariantDto` bổ sung `physicalQuantity`, `availableQuantity`, `promotionName`, `discountLabel` và các getters.
   - Endpoint `/admin/pos/products-modal`: tính toán tồn khả dụng = `v.quantity - qty_in_cart`.
   - Endpoint `/admin/pos/add`, `/admin/pos/quantity`, `/admin/pos/remove`: trả về JSON cập nhật live tồn khả dụng và totals.
   - Endpoint `/admin/pos/checkout`: thanh toán thành công chuyển hướng trực tiếp `/admin/invoices/{id}`.
   - Endpoint `/admin/pos/success/{id}`: chuyển hướng 302 về `/admin/invoices/{id}`.
5. `src/main/resources/templates/admin/ban-tai-quay/modal-san-pham.html`: Thiết kế bảng chuẩn 13 cột, ô màu sắc, badge tồn khả dụng, cột giá bán và khuyến mãi, nút `+` (aria-label="Thêm vào giỏ").
6. `src/main/resources/templates/admin/ban-tai-quay/ban-tai-quay.html`: Sửa nhãn "Thương hiệu", placeholder "Tất cả", layout các combo box đều đẹp, thẻ thông tin đơn hàng hiển thị tổng tiền hàng gốc & giảm khuyến mãi.
7. `src/main/resources/templates/admin/hoa-don/chi-tiet.html`: Thêm nút "Bán đơn mới" bên cạnh "In hóa đơn".
8. `src/main/resources/static/css/quan-tri.css`: Mở rộng kích thước modal POS (`width: 95vw; max-width: 1200px`), min-width bảng modal, căn chỉnh chiều cao combo box `35px`.
9. `src/main/resources/static/js/ban-tai-quay.js`: Tích hợp cập nhật realtime tồn khả dụng trên modal qua AJAX, cập nhật giỏ hàng và totals không reload trang, kẹp số lượng nhập tay.
10. `src/main/resources/static/js/nut-hanh-dong.js`: Loại trừ `.pos-modal-table` để không bị can thiệp biến nút `+` thành icon SVG khác.
11. `src/test/java/com/example/demodatn/service/donhang/PosModalPricingIntegrationTest.java`: Suite 5 test cases tự động kiểm tra tồn khả dụng, khuyến mãi, checkout và rollback.
12. `docs/images/gd23/screenshots/pos_modal_1280.png`: Ảnh chụp màn hình hộp thoại tại 1280px.
13. `docs/images/gd23/screenshots/pos_modal_375.png`: Ảnh chụp màn hình hộp thoại tại 375px.
