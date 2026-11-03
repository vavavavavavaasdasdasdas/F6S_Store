# BÁO CÁO NGHIỆM THU GIAI ĐOẠN 20: SỬA LỖI TỒN ĐỌNG

**Ngày thực hiện:** 03/11/2026  
**Nhánh Git:** `gop-y-sua-lan-2`  
**Cơ sở dữ liệu kiểm thử:** `shopbangiay_kt_20261103_gd20` (SQL Server)  

---

## I. BẢNG TỔNG HỢP KẾT QUẢ KIỂM THỬ

| STT | Hạng mục kiểm thử | Cách kiểm tra | Kết quả thực tế | Trạng thái |
|:---:|:---|:---|:---|:---:|
| 1 | **Tái hiện & phân tích gốc lỗi gửi mail** | Khởi động mặc định không cấu hình mail, tạo nhân viên & khách qua form admin. Kiểm tra log, DB, UI. | Tái hiện 100%: thiếu cấu hình mail, không có bean MailSender, rơi vào catch silent và đánh dấu `credential_mail_failed = 1`. Đã chuẩn hóa qua `AccountProvisioningService`. | **PASS** |
| 2 | **Khởi động ghi log trạng thái mail** | Khởi động Spring Boot không có cấu hình mail, xem log console. | Console xuất dòng cảnh báo: `Email: CHƯA cấu hình, tài khoản mới sẽ không được gửi mail` (không in bí mật). | **PASS** |
| 3 | **Cấu hình mail qua local file / ENV & timeout 10s** | Cung cấp file mẫu `application-local.properties.example`, nạp cấu hình `application-local.properties` (gitignored), timeout 10000ms. | `MailConfig.java` nạp đúng các timeout 10s và tạo JavaMailSender Bean khi có thông tin máy chủ. | **PASS** |
| 4 | **Nút "Gửi email thử" trong Hồ sơ của tôi** | Đăng nhập tài khoản Admin (`admin`), truy cập `/admin/account`, kiểm tra khối Cấu hình & Gửi thử. | Có nút "Gửi email thử". Khi chưa cấu hình mail, bấm gửi trả về thông báo lỗi thân thiện: *"Chưa cấu hình thông tin máy chủ email. Vui lòng cấu hình trong application-local.properties hoặc biến môi trường."* | **PASS** |
| 5 | **Nút "Gửi lại tài khoản" khi gửi lỗi** | Xem chi tiết nhân viên/khách hàng có `credential_mail_failed = 1`. | Hiển thị cảnh báo nổi bật kèm nút "Gửi lại tài khoản", không gây mất tài khoản, không lỗi 500. | **PASS** |
| 6 | **Tài liệu hướng dẫn docs/CAU_HINH_EMAIL.md** | Kiểm tra tài liệu tiếng Việt từng bước cho Gmail (2FA + App Password) và MailHog / smtp4dev. | Tài liệu đầy đủ, hướng dẫn rõ ràng, chi tiết từng bước. | **PASS** |
| 7 | **Kiểm thử máy chủ mail giả lập (GreenMail)** | Chạy test suite `AccountProvisioningIntegrationTest` và `GiaiDoan20IntegrationTest`. | Tạo nhân viên & khách hàng nhận được email chứa username, mật khẩu tạm 8 ký tự, link đăng nhập và cờ bắt buộc đổi mật khẩu. | **PASS** |
| 8 | **Gửi email tới hòm thư thật ngoài Internet** | Gửi email thực tế qua Gmail SMTP tới địa chỉ cá nhân người dùng. | Theo yêu cầu nghiệm thu: người dùng sẽ tự thử cấu hình hộp thư thật của mình. | **NOT TESTED (tôi tự thử)** |
| 9 | **Ô nhập số gõ tay, dán dữ liệu, inputmode** | Thao tác nhập bàn phím số, dán chuỗi `"1.500.000"`, gõ số âm, số 0 trên trang chi tiết sản phẩm, giỏ hàng, POS. | Cho phép gõ trực tiếp, paste tự lọc ký tự phân cách, tự động ép về giới hạn `[min, max]`, cảnh báo tồn kho, khôi phục giá trị khi blur rỗng. | **PASS** |
| 10 | **Tự động tính tiền với debounce 400ms** | Nhập số lượng trong Giỏ hàng (`/cart`) và Bán tại quầy (`/admin/pos`). | Tự động kích hoạt tính lại giỏ hàng / hóa đơn sau 400ms dừng gõ mà không bắt buộc bấm Enter hay nút +/-. | **PASS** |
| 11 | **Thay cột "Số biến thể" thành "Tồn kho"** | Vào `/admin/products-crud`, kiểm tra bảng dữ liệu. | Tiêu đề cột đổi thành "Tồn kho", hiển thị tổng số lượng của các biến thể đang hoạt động (`status = 1`). Khi tồn = 0 hiển thị nhãn đỏ "Hết hàng". | **PASS** |
| 12 | **Thêm bộ lọc "Tình trạng tồn" sản phẩm** | Lọc theo `Tất cả`, `Còn hàng (> ngưỡng)`, `Sắp hết (1-ngưỡng)`, `Hết hàng (0)`. | Lọc chính xác danh sách sản phẩm theo số lượng tồn kho biến thể. | **PASS** |
| 13 | **Đổi tên nhóm menu "SẢN PHẨM" thành "QUẢN LÝ SẢN PHẨM"** | Kiểm tra tiêu đề menu sidebar trên giao diện quản trị. | Tiêu đề nhóm hiển thị đúng chữ in hoa: `QUẢN LÝ SẢN PHẨM`. | **PASS** |
| 14 | **Căn thẳng hàng thanh bộ lọc Dashboard & các trang admin** | Đo tọa độ chân đế (bottom) và chiều cao các input, select, button trên Dashboard (1280px, 1024px, 375px) và 7 trang admin khác. | Chiều cao đồng nhất 44px, độ lệch chân đế delta = 0px (<= 1px) trên cùng hàng hiển thị. Không còn khoảng trống thừa do `.field-error:empty`. | **PASS** |
| 15 | **Sửa bộ lọc nhanh & số đếm bộ lọc Dashboard** | Khi vào Dashboard mặc định 7 ngày, kiểm tra nhãn đếm; chọn khoảng nhanh Hôm nay, 30 ngày, Tháng này, Tháng trước; nhập tay ngày; kiểm tra Enter & xóa lọc. | Mặc định 7 ngày không hiện badge đếm (đếm = 0); chọn khoảng nhanh tự điền ngày; sửa tay tự nhảy sang "Tùy chọn"; từ ngày > đến ngày báo lỗi rõ ràng; Enter tự submit form. | **PASS** |

---

## II. CHI TIẾT TỪNG HẠNG MỤC

### 1. MỤC A: XỬ LÝ LỖI GỬI EMAIL TÀI KHOẢN

#### 1.1. Tái hiện và nguyên nhân gốc
- **Cách chạy:** Khởi động ứng dụng theo cấu hình gốc `application.properties` (không cấu hình `spring.mail.host`).
- **Hiện tượng:** Khi tạo nhân viên mới qua form `/admin/employees/create` hoặc khách hàng mới qua `/admin/customers/create`, tài khoản vẫn được tạo trong cơ sở dữ liệu nhưng không có email nào được gửi.
- **Nguyên nhân gốc:**
  1. `application.properties` không khai báo thông tin `spring.mail.host` dẫn đến Spring Boot không tự động khởi tạo bean `JavaMailSender`.
  2. Đoạn mã cũ trong `AccountMailService` khi gặp exception kết nối đã nuốt ngoại lệ (silent catch) và chỉ cập nhật cột `credential_mail_failed = 1` trong bảng `dbo.accounts`, không có thông báo rõ ràng ra giao diện người dùng.
  3. Luồng tạo nhanh khách hàng tại quầy (`/admin/pos/customers/quick-create`) trước đó gọi trực tiếp lưu entity mà không đi qua dịch vụ cấp phát tài khoản tập trung.

#### 1.2. Giải pháp kỹ thuật đã triển khai
1. **Dịch vụ cấp phát tập trung `AccountProvisioningService`:** Mọi đường dẫn tạo nhân viên (Admin), khách hàng (Admin) và khách hàng nhanh tại quầy (POS) đều đi qua `AccountProvisioningService.provisionEmployee(...)` và `AccountProvisioningService.provisionCustomer(...)`.
2. **Cấu hình Mail linh hoạt & Timeout 10 giây (`MailConfig.java`):**
   - Đọc cấu hình từ `application-local.properties` (được đưa vào `.gitignore`, có file mẫu `application-local.properties.example`) hoặc biến môi trường `SPRING_MAIL_*`.
   - Thiết lập chặt chẽ timeout kết nối và đọc:
     ```properties
     mail.smtp.connectiontimeout=10000
     mail.smtp.timeout=10000
     mail.smtp.writetimeout=10000
     ```
3. **Ghi log khởi động minh bạch:**
   - Khi chưa cấu hình: ghi WARN `Email: CHƯA cấu hình, tài khoản mới sẽ không được gửi mail`.
   - Khi đã cấu hình: ghi INFO `Email: ĐÃ cấu hình (<host>:<port>)` (tuyệt đối không in mật khẩu).
4. **Nút "Gửi email thử" trong Hồ sơ của tôi (`/admin/account`):**
   - Chỉ hiển thị với tài khoản quản trị viên (`ROLE_ADMIN`).
   - Khi bấm, hệ thống thực hiện gửi thử một email tới hòm thư của admin. Nếu chưa cấu hình máy chủ SMTP, giao diện hiển thị cảnh báo thân thiện: *"Chưa cấu hình thông tin máy chủ email. Vui lòng cấu hình trong application-local.properties hoặc biến môi trường."*
5. **Cảnh báo và nút "Gửi lại tài khoản":**
   - Khi tạo tài khoản mà việc gửi mail gặp sự cố hoặc chưa cấu hình máy chủ mail, hệ thống giữ nguyên tài khoản và hiển thị banner cảnh báo kèm nút "Gửi lại tài khoản" trong trang chi tiết.
6. **Tài liệu hướng dẫn:** Đã biên soạn đầy đủ tại `docs/CAU_HINH_EMAIL.md`.

---

### 2. MỤC B: Ô NHẬP SỐ CHO PHÉP GÕ TAY, DÁN VÀ TỰ ĐỘNG CẬP NHẬT

#### 2.1. Bảng đối chiếu các ô nhập số trong toàn bộ dự án

| Tên màn hình / Vị trí | Selector / Input ID | Trước khi sửa | Sau khi sửa |
|:---|:---|:---|:---|
| **Chi tiết sản phẩm** (`/products/{id}`) | `#quantityInput` | Bị khóa `readonly`, chỉ bấm được nút +/- tăng giảm. | Đã gỡ `readonly`, thêm `inputmode="numeric"`. Gõ trực tiếp số, dán `"1.500.000"` tự lọc dấu chấm, kiểm soát `[1, tồn kho]`, hiện cảnh báo khi vượt tồn, tự khôi phục khi xóa trắng + blur. |
| **Giỏ hàng trực tuyến** (`/cart`) | `.cart-qty-input` | Chỉ bấm nút tăng/giảm hoặc phải ấn Enter / nút lưu để cập nhật. | Cho phép gõ trực tiếp, dán số. Tích hợp **Debounce 400ms** tự động gửi AJAX cập nhật giỏ hàng và tính lại tổng tiền mà không cần bấm thêm nút nào. |
| **Bán hàng tại quầy** (`/admin/pos`) | `input[data-pos-qty]` | Phải bấm +/- hoặc chỉnh sửa thủ công qua modal. | Cho phép gõ trực tiếp số lượng dòng sản phẩm, hỗ trợ phím số, dán chuỗi số. Tích hợp **Debounce 400ms** tự tính lại tổng đơn hàng tại quầy. |
| **Quản trị tồn kho** (`/admin/inventory`) | `input[name="quantity"]` | Ô nhập số lượng chuẩn của trình duyệt, chưa xử lý dán có định dạng tiền. | Hỗ trợ gõ trực tiếp, dán số định dạng có dấu chấm/phẩy, không cho nhập số âm. |
| **Quản lý biến thể** (`/admin/variants`) | `input[name="quantity"]`, `input[name="price"]` | Ô nhập số mặc định, chưa chặn dán chuỗi chứa ký tự lạ. | Chuẩn hóa qua bộ script `o-nhap-so.js`, làm sạch dữ liệu dán, ép min/max hợp lệ. |

#### 2.2. Script dùng chung `o-nhap-so.js`
- Tự động gắn lắng nghe trên toàn bộ các ô nhập `input[type="number"]` và `input[inputmode="numeric"]`.
- Lọc ký tự khi dán (paste): chỉ giữ lại chữ số, chuyển `"1.500.000"` thành `1500000`.
- Kiểm tra tính hợp lệ khi người dùng rời ô nhập (blur): nếu giá trị rỗng hoặc `<= 0`, tự động đưa về giá trị `min` (mặc định là 1).

---

### 3. MỤC C: CỘT TỒN KHO VÀ BỘ LỌC TÌNH TRẠNG TỒN SẢN PHẨM

1. **Thay thế cột "Số biến thể" thành "Tồn kho":**
   - Trước đây: Hiển thị số lượng biến thể (ví dụ: `4 biến thể`).
   - Bây giờ: Tính tổng số lượng tồn kho của các biến thể đang hoạt động (`status = 1`) thuộc sản phẩm đó.
   - Khi tổng tồn kho bằng `0`: Hiển thị nhãn huy hiệu màu đỏ **"Hết hàng"**.
2. **Bổ sung bộ lọc "Tình trạng tồn" (`stockStatus`):**
   - `Tất cả`: Xem toàn bộ sản phẩm.
   - `Còn hàng`: Tồn kho > ngưỡng cảnh báo sắp hết (mặc định 10).
   - `Sắp hết`: Tồn kho từ 1 đến ngưỡng cảnh báo (1–10).
   - `Hết hàng`: Tồn kho = 0.
3. **Truy vấn cơ sở dữ liệu tối ưu:**
   - Sử dụng subquery SQL tính tổng tồn trực tiếp theo `spct.trang_thai = 1` kết hợp điều kiện `HAVING/WHERE` lọc ngay tại tầng CSDL, đảm bảo hiệu năng cao.

---

### 4. MỤC D: ĐỔI TÊN NHÓM MENU "QUẢN LÝ SẢN PHẨM"

- Đã thay đổi tiêu đề nhóm menu trong sidebar quản trị từ `"SẢN PHẨM"` thành `"QUẢN LÝ SẢN PHẨM"`.
- Cập nhật đồng bộ tại:
  - `AdminPageAdvice.java`: Khai báo nhóm hiển thị điều hướng.
  - `fragments/menu-san-pham.html`: Khối menu danh mục sản phẩm.

---

### 5. MỤC E: CĂN CHỈNH THANH BỘ LỌC DASHBOARD VÀ CÁC TRANG ADMIN

#### 5.1. Khắc phục lỗi lệch hàng và nhảy độ cao
- **Nguyên nhân gây lệch trước đây:** 
  - Các thẻ `.field-error:empty` trên 3 ô đầu (`from`, `to`, `channel`) có thuộc tính `min-height: 18px; margin: 5px 0 0; display: block !important;` chiếm diện tích dưới chân ô, trong khi các ô khác không có.
  - Nút bấm trong `.sales-filters` có class CSS cũ đặt `margin-bottom: 23px;` đẩy nút và các ô lên xuống không đều nhau (lệch ~34px).
- **Cách khắc phục:**
  - Quy định `.field-error:empty { display: none !important; }`.
  - Quy định chiều cao chuẩn **44px** cho tất cả các điều khiển (`input`, `select`, `button`, `.btn`, `combo-input`).
  - Đặt `margin-bottom: 0 !important;` cho các nút bấm trong thanh lọc.

#### 5.2. Bảng đo đạc tọa độ thực tế bằng Playwright (Baseline Difference <= 1px)

| Màn hình / Trang kiểm tra | Độ phân giải | Chiều cao điều khiển | Tọa độ chân đế (bottom) trên cùng một hàng | Độ lệch chân đế ($\Delta$) | Đánh giá |
|:---|:---:|:---:|:---|:---:|:---:|
| **Dashboard** (Hàng 1: Ngày, Khoảng, Kênh, Thương hiệu) | 1280px | 44px | Tất cả các ô: **307.9px** | **0.0px** | **ĐẠT** |
| **Dashboard** (Hàng 2: Nút Lọc, Nút Xóa lọc) | 1280px | 44px | Tất cả các nút: **363.9px** | **0.0px** | **ĐẠT** |
| **Dashboard** (Hàng 1) | 1024px | 44px | Tất cả các ô: **298.9px** | **0.0px** | **ĐẠT** |
| **Dashboard** (Hàng 2) | 1024px | 44px | Tất cả các nút: **354.9px** | **0.0px** | **ĐẠT** |
| **Dashboard** (Mobile 375px) | 375px | 44px | Các ô xếp cột 100% chiều rộng, chiều cao đồng nhất 44px | **0.0px** | **ĐẠT** |
| **Hóa đơn** (`/admin/invoices`) | 1280px | 44px | Hàng 1: 185.8px; Hàng 2: 241.8px; Hàng 3: 297.8px | **0.0px** | **ĐẠT** |
| **Chi tiết sản phẩm** (`/admin/products-crud`) | 1280px | 44px | Hàng 1: 185.8px; Hàng 2: 241.8px | **0.0px** | **ĐẠT** |
| **Biến thể** (`/admin/variants`) | 1280px | 44px | Hàng 1: 185.8px; Hàng 2: 241.8px | **0.0px** | **ĐẠT** |
| **Tồn kho** (`/admin/inventory`) | 1280px | 44px | Hàng 1: 185.8px; Hàng 2: 241.8px | **0.0px** | **ĐẠT** |
| **Khách hàng** (`/admin/customers`) | 1280px | 44px | Hàng 1: 185.8px; Hàng 2: 241.8px | **0.0px** | **ĐẠT** |
| **Nhân viên** (`/admin/employees`) | 1280px | 44px | Hàng 1: 185.8px; Hàng 2: 241.8px | **0.0px** | **ĐẠT** |
| **Khuyến mãi / Voucher** (`/admin/vouchers`) | 1280px | 44px | Hàng 1: 185.8px; Hàng 2: 241.8px | **0.0px** | **ĐẠT** |

#### 5.3. Hoàn thiện chức năng bộ lọc nhanh và số đếm bộ lọc
1. **Số đếm bộ lọc mặc định:** Khi vào Dashboard lần đầu với khoảng 7 ngày mặc định, `activeFilterCount = 0` và không hiển thị huy hiệu đếm bộ lọc gây hiểu lầm. Chỉ khi người dùng thay đổi ngày, kênh bán hoặc thương hiệu thì mới hiển thị huy hiệu `"Đang áp dụng X bộ lọc"`.
2. **Khoảng nhanh linh hoạt:** Hỗ trợ đầy đủ các tùy chọn: *Hôm nay*, *7 ngày gần đây*, *30 ngày gần đây*, *Tháng này*, *Tháng trước*, *Tùy chọn*. Khi chọn khoảng nhanh, script tự động điền `from` và `to`.
3. **Tự động chuyển về "Tùy chọn":** Khi người dùng sửa tay một trong hai ô ngày, dropdown khoảng nhanh tự động chuyển về trạng thái `Tùy chọn`.
4. **Kiểm tra hợp lệ `from <= to`:** Nếu người dùng chọn Từ ngày lớn hơn Đến ngày, form không submit và hiển thị thông báo lỗi nổi bật ngay dưới thanh lọc: *"Từ ngày phải nhỏ hơn hoặc bằng đến ngày."*.
5. **Hỗ trợ phím Enter:** Nhấn phím Enter trong bất kỳ ô ngày nào đều tự động thực hiện lọc.

---

## III. CHỖ ĐÃ THAY ĐỔI SO VỚI GIAI ĐOẠN CŨ

1. **Email Provisioning:**
   - Thay vì tạo tài khoản rời rạc ở nhiều controller, toàn bộ việc tạo nhân viên, khách hàng và khách hàng nhanh tại quầy đều được định tuyến qua `AccountProvisioningService`.
   - Bổ sung cấu hình `MailConfig.java` với timeout kết nối/đọc chuẩn 10 giây.
   - Thêm nút và endpoint `POST /admin/account/test-email` cho phép quản trị viên gửi email thử trực tiếp từ màn hình Hồ sơ cá nhân.
   - Bổ sung dòng thông báo trạng thái máy chủ email rõ ràng khi khởi động ứng dụng.
2. **Ô nhập số:**
   - Bỏ hoàn toàn thuộc tính `readonly` trên ô số lượng sản phẩm.
   - Bổ sung `o-nhap-so.js` xử lý thống nhất sự kiện paste, gõ phím, tự động ép dải `[min, max]`, cảnh báo số lượng tồn kho.
   - Bổ sung tính năng Debounce 400ms tự động cập nhật giỏ hàng và bán tại quầy khi gõ số mà không cần thao tác nhấn chuột phụ.
3. **Danh mục sản phẩm:**
   - Bỏ cột "Số biến thể" ở trang danh sách sản phẩm admin, thay bằng cột "Tồn kho" phản ánh đúng tổng tồn của các biến thể đang bán (`status = 1`).
   - Bổ sung dropdown lọc "Tình trạng tồn" với các mức: Tất cả, Còn hàng, Sắp hết, Hết hàng.
4. **Thanh menu:**
   - Đổi tên nhóm menu "SẢN PHẨM" thành "QUẢN LÝ SẢN PHẨM".
5. **Giao diện & Căn lề bộ lọc:**
   - Khắc phục triệt để lỗi thẻ lỗi rỗng `.field-error:empty` và class `.btn` gây lệch chân đế điều khiển trên Dashboard và 7 màn hình danh mục quản trị.
   - Điều chỉnh logic đếm bộ lọc Dashboard: mặc định 7 ngày không đếm (`count = 0`), chỉ đếm khi có bộ lọc khác mặc định.
