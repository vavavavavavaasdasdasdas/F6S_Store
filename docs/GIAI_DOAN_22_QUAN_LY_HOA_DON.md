# BÁO CÁO KẾT QUẢ GIAI ĐOẠN 22: LÀM LẠI TRANG "QUẢN LÝ HÓA ĐƠN" ĐẦY ĐỦ CHỨC NĂNG CỦA ĐƠN HÀNG VÀ HÓA ĐƠN

## I. TỔNG QUAN
Giai đoạn 22 hoàn thiện việc hợp nhất không gian xử lý đơn hàng và hóa đơn:
- Đổi tên toàn diện mục menu, tiêu đề trang H1 và thẻ `<title>` thành **"Quản lý hóa đơn"** (`/admin/invoices`).
- Gỡ bỏ hoàn toàn mục "Đơn hàng" và "Bán online" khỏi thanh sidebar admin.
- Chuyển hướng tương thích toàn bộ route cũ `/admin/orders/**` sang `/admin/invoices/**` (302 đối với GET, 307 đối với POST kèm body).
- Trang danh sách Quản lý hóa đơn: Bổ sung dải tab trạng thái kèm số lượng đếm động theo bộ lọc hiện tại, thanh lọc đa tiêu chí chuẩn hóa, bảng 11 cột kèm nút "Chuyển bước tiếp theo" nhanh cho đơn trực tuyến có bước hợp lệ, cố định phân trang 12 đơn/trang sắp xếp mới nhất lên trước.
- Trang chi tiết hợp nhất: Bố cục trực quan kết hợp thông tin hóa đơn, đơn hàng, người mua, người nhận có tỉnh/thành; tiến trình đơn online 5 bước trực quan; modal chuyển trạng thái bắt buộc lý do khi hủy/giao thất bại và cho phép nhập đơn vị vận chuyển/mã vận đơn; mã VietQR khi chưa thanh toán; huy hiệu trạng thái tồn kho; nút xác nhận nhận chuyển khoản; bảng 10 cột sản phẩm; khối tổng tiền và chi tiết dòng tiền; đơn tại quầy POS hoàn toàn không hiển thị các bước vận chuyển; bảo lưu bản in hóa đơn chuyên dụng.

---

## II. BẢNG KIỂM TOÁN TRƯỚC KHI SỬA (AUDIT MATRIX)
*(Đối chiếu từ git commit `b14d335` và `928b12c` trước/sau khi sáp nhập Đơn hàng và Hóa đơn)*

| STT | Chức năng | Route cũ | Hiện có ở Quản lý hóa đơn chưa | Sai / thiếu ở đâu & Giải pháp thực hiện |
|---|---|---|:---:|---|
| **1** | **Mục menu sidebar** | `/admin/invoices` | Có | Đổi tên từ "Hóa đơn" $\rightarrow$ **"Quản lý hóa đơn"** trên sidebar, tiêu đề H1 và thẻ `<title>`. |
| **2** | **Mục Đơn hàng & Bán online** | `/admin/orders`, `/admin/ban-online` | Đã gỡ | Đã loại bỏ hoàn toàn khỏi sidebar trong `AdminPageAdvice.java`. URL cũ chuyển hướng 302 về trang tương ứng. |
| **3** | **Dải tab trạng thái kèm số lượng** | `/admin/orders` (dropdown cũ) | Chưa có | **Đã bổ sung**: 8 tab trạng thái (`Tất cả`, `Chờ xử lý`, `Đã xác nhận`, `Đang chuẩn bị`, `Đang giao`, `Đã giao/Hoàn tất`, `Đã hủy`, `Giao thất bại`). Đếm số lượng động theo các tiêu chí lọc đang chọn; bấm tab thì lọc theo trạng thái đó; cuộn ngang mượt mà trên mobile 375px. |
| **4** | **Thanh lọc từ khóa tổng hợp** | Dropdown rời rạc | Có 1 phần | **Đã nâng cấp**: Ô từ khóa `q` tìm kiếm đa trường: mã hóa đơn, mã đơn hàng, tên khách hàng, SĐT khách, người nhận, SĐT người nhận, mã khách hàng. |
| **5** | **Thanh lọc tiêu chí nghiệp vụ** | Lọc theo Kênh, Thanh toán, Phương thức, Khoảng ngày | Có | Chuẩn hóa các bộ lọc: Kênh bán (Tại quầy / Trực tuyến), Thanh toán (Đã thanh toán / Chưa thanh toán), Phương thức (Tiền mặt, COD, Chuyển khoản, Kết hợp), Từ ngày, Đến ngày. Gỡ bỏ dropdown Nguồn đơn thừa. |
| **6** | **Cấu trúc bảng danh sách** | Bảng 12 cột cũ | Chưa chuẩn | **Đã chuẩn hóa 11 cột**: `STT | Mã hóa đơn | Mã đơn | Ngày | Khách | Kênh bán | Trạng thái | Thanh toán | Phương thức | Tổng tiền (đ) | Hành động`. |
| **7** | **Nút chuyển bước tiếp theo nhanh** | N/A (chỉ có ở chi tiết) | Chưa có | **Đã bổ sung**: Trên cột Hành động của danh sách, đơn trực tuyến có bước tuyến tính tiếp theo hợp lệ (Xác nhận đơn, Chuẩn bị hàng, Giao hàng, Hoàn tất) hiển thị nút chuyển bước kèm hộp thoại xác nhận `data-confirm`. |
| **8** | **Quy chuẩn phân trang** | Default page size | Chưa cố định | **Cố định 12 dòng/trang** (`shop.page-size=12`), sắp xếp mới nhất lên trước (`issuedAt DESC, id DESC`). |
| **9** | **Khối thông tin hóa đơn & đơn hàng** | 2 trang riêng | Đã hợp nhất | Hiển thị mã HĐ, mã đơn, ngày tạo, kênh bán, nhân viên bán hàng (tại quầy) hoặc Đơn trực tuyến, ghi chú thanh toán, ghi chú bán hàng, mã đối soát. |
| **10** | **Khối khách hàng & người nhận** | Thiếu địa chỉ đầy đủ | Đã hoàn thiện | Hiển thị tên khách, SĐT, email, người nhận, địa chỉ giao hàng, tỉnh/thành phố; hỗ trợ sửa tỉnh/thành trước khi giao. |
| **11** | **Tiến trình đơn trực tuyến (Timeline)** | `fragments/giao-hang` | Đã nâng cấp | Hiển thị 5 bước: `Đã đặt` $\rightarrow$ `Đã xác nhận` $\rightarrow$ `Đang chuẩn bị` $\rightarrow$ `Đang giao` $\rightarrow$ `Đã giao` kèm mốc thời gian, người thực hiện. Cảnh báo rõ ràng nếu đơn Hủy hoặc Giao thất bại. |
| **12** | **Xử lý trạng thái & hoàn kho** | Dialog | Đã hoàn thiện | Hủy đơn (`CANCELLED`) hoặc Giao hàng thất bại (`DELIVERY_FAILED`) bắt buộc nhập lý do (`reason`), tự động hoàn kho `da_tru_kho = 0`. Nhập đơn vị vận chuyển (`carrier`) và mã vận đơn (`trackingCode`) khi giao. |
| **13** | **Xác nhận chuyển khoản & VietQR** | Có | Đã củng cố | Nút "Xác nhận đã nhận chuyển khoản" cho đơn chưa thanh toán; mã VietQR thanh toán chuẩn số tiền đơn hàng không gồm phí ship. |
| **14** | **Phân biệt đơn tại quầy (POS)** | Lẫn giao diện | Đã phân tách | Đơn tại quầy POS **KHÔNG** hiển thị các bước vận chuyển, timeline giao hàng, thông tin bưu tá, địa chỉ giao hàng. |
| **15** | **Bảng sản phẩm chi tiết** | 10 cột | Đã chuẩn hóa | `STT | Mã SP | Ảnh | Tên sản phẩm | Kích cỡ | Màu sắc | Chất liệu | Đế giày | Số lượng | Đơn giá | Thành tiền`. |
| **16** | **Khối tổng tiền & dòng tiền** | Cũ có phí ship | Đã cập nhật | Tạm tính, Giảm giá, Voucher, Tổng thanh toán; phân bổ Tiền mặt (`tien_mat`), Chuyển khoản (`tien_chuyen_khoan`), Khách đưa, Tiền thừa. Ghi chú nhỏ: *"Phí vận chuyển do đơn vị vận chuyển tính và thu riêng."* (không có dòng phí ship). |
| **17** | **Bản in hóa đơn** | `data-print-invoice` | Đã giữ nguyên | Layout in `.invoice-paper` ẩn khi xem web thông thường, hiển thị đầy đủ tiêu chuẩn khi bấm "In hóa đơn". |
| **18** | **Bảo mật & Phân quyền** | Có | Đã kiểm chứng | Tất cả form POST đều có CSRF token. `ROLE_ADMIN` và `ROLE_EMPLOYEE` được phép thao tác; `ROLE_USER` bị từ chối 403 Forbidden. |

---

## III. CHI TIẾT CÁC THAY ĐỔI MÃ NGUỒN

### 1. Điều hướng & Menu Sidebar
- `AdminPageAdvice.java`: Cập nhật danh sách điều hướng quản trị, đổi `"Hóa đơn"` thành `"Quản lý hóa đơn"` (`/admin/invoices`), xóa bỏ mục menu `"Bán online"` (`/admin/ban-online`).

### 2. Controller & Nghiệp vụ
- `OrderStatusService.java`:
  - Thêm phương thức `nextLinearStatus(Order order)` xác định chính xác bước tiếp theo tuyến tính của đơn trực tuyến (`PENDING` $\rightarrow$ `CONFIRMED` $\rightarrow$ `PREPARING` $\rightarrow$ `SHIPPING` $\rightarrow$ `DELIVERED`).
  - Thêm phương thức `nextLinearStatusLabel(String nextStatus)` sinh nhãn tiếng Việt tương ứng ("Xác nhận đơn", "Chuẩn bị hàng", "Giao hàng", "Hoàn tất").
- `AdminInvoiceController.java`:
  - Thêm logic sinh 8 tab trạng thái (`StatusTab`) kèm tính toán số lượng đơn thực tế theo các bộ lọc kích hoạt.
  - Mở rộng Specification tìm kiếm `q` đa trường (mã HĐ, mã đơn, khách, SĐT, người nhận, mã khách).
  - Chuẩn hóa bộ lọc Phương thức thanh toán hỗ trợ `CASH`, `COD`, `TRANSFER` (gồm cả chuyển khoản POS và trực tuyến), `COMBINED`.
  - Hỗ trợ tham số `returnTo` trong các endpoint POST để sau khi thao tác từ bảng danh sách, chuyển hướng quay lại đúng URL và trang hiện tại kèm thông báo flash.
  - Bổ sung nạp thông tin vận chuyển mới nhất (`latestCarrier`, `latestTrackingCode`) phục vụ hiển thị và sửa đổi.

### 3. Giao diện Thymeleaf & CSS
- `src/main/resources/templates/admin/hoa-don/danh-sach.html`:
  - Bổ sung dải tab trạng thái cuộn ngang `.status-tabs-container` với 8 tab và badge đếm số lượng.
  - Thiết kế lại form lọc tinh gọn, dễ sử dụng.
  - Bảng 11 cột hiển thị đầy đủ thông tin: STT, Mã hóa đơn, Mã đơn, Ngày, Khách, Kênh bán, Trạng thái, Thanh toán, Phương thức, Tổng tiền, Hành động.
  - Cột Hành động có nút "Xem / xử lý", "In", và nút "Chuyển bước tiếp theo" nhanh kèm popup xác nhận `data-confirm`.
- `src/main/resources/templates/admin/hoa-don/chi-tiet.html`:
  - Thiết kế lại thành 1 trang duy nhất, bố cục 2 cột cho thông tin hóa đơn & đơn hàng (kèm huy hiệu tồn kho `"Đã trừ kho" / "Chưa trừ kho"`) và thông tin khách hàng & người nhận có tỉnh/thành.
  - Đơn trực tuyến: hiển thị timeline tiến trình 5 bước, dải nút chuyển bước, nút hủy đơn và giao thất bại mở dialog bắt buộc lý do, khối VietQR khi chưa thanh toán, lịch sử đổi trạng thái chi tiết.
  - Đơn tại quầy: ẩn hoàn toàn các bước vận chuyển và thông tin bưu tá.
  - Bảng 10 cột sản phẩm trong đơn kèm ảnh đại diện.
  - Khối tổng kết tài chính hiển thị đầy đủ tạm tính, giảm giá, voucher, tổng thanh toán, dòng tiền mặt và chuyển khoản đối với đơn kết hợp, ghi chú nhỏ về phí vận chuyển.
  - Modal `#order-transition-dialog` tích hợp sẵn xử lý lý do và vận đơn.
- `src/main/resources/static/css/quan-tri.css`:
  - Thêm CSS cho `.status-tabs-container`, `.status-tabs`, `.invoice-detail-grid`.

---

## IV. BẰNG CHỨNG KIỂM THỬ

### 1. Kiểm thử tích hợp tự động (Maven Integration Tests)
- **`InvoiceManagementIntegrationTest`**: Đạt **7/7 ca kiểm thử (100% PASS)**:
  1. `statusTabsMatchDatabaseCountAndFiltersWork`: Dải tab trạng thái khớp chính xác `COUNT(*)` từ SQL và bộ lọc hoạt động chuẩn.
  2. `onlineOrderProgressionFromPendingToDelivered`: Tiến trình đầy đủ từ PENDING $\rightarrow$ CONFIRMED $\rightarrow$ PREPARING $\rightarrow$ SHIPPING $\rightarrow$ DELIVERED, trừ kho đúng thời điểm.
  3. `cancelledOrderRestoresStockAndRequiresReason`: Hủy đơn bắt buộc nhập lý do, tự động hoàn trả tồn kho vào database.
  4. `deliveryFailedRestoresStockAndRequiresReason`: Giao hàng thất bại bắt buộc nhập lý do, tự động hoàn trả tồn kho.
  5. `confirmBankTransferPaymentSuccess`: Xác nhận chuyển khoản cập nhật `paymentStatus = PAID`.
  6. `securityNonAdminNonEmployeeGetsForbidden`: Người dùng thông thường `ROLE_USER` bị từ chối 403 Forbidden trên tất cả thao tác GET/POST quản trị.
  7. `employeeHasAccessToInvoiceListAndDetail`: Nhân viên `ROLE_EMPLOYEE` được phép truy cập xem và xử lý.
- **`InvoiceOnlineWorkspaceIntegrationTest`**: Đạt **5/5 ca kiểm thử (100% PASS)**.
- **`PosInvoiceIntegrationTest`**: Đạt **10/10 ca kiểm thử (100% PASS)**.

### 2. Kiểm thử tự động trên trình duyệt thật (Playwright)
Kịch bản `verify_gd22.js` chạy trên Microsoft Edge với đầy đủ các bước kiểm tra:
1. Đăng nhập tài khoản admin thành công.
2. Kiểm tra route cũ `/admin/orders` $\rightarrow$ chuyển hướng 302 chính xác về `/admin/invoices`.
3. Kiểm tra tiêu đề H1 `"Quản lý hóa đơn"`, sidebar có `"Quản lý hóa đơn"`, không còn `"Đơn hàng"`, không còn `"Bán online"`.
4. 8 tab trạng thái hiển thị đầy đủ số lượng: Tất cả (39), Chờ xử lý (6), Đã xác nhận (16), Đang chuẩn bị (0), Đang giao (1), Đã giao/Hoàn tất (8), Đã hủy (7), Giao thất bại (1).
5. Bảng 11 cột hiển thị chính xác.
6. Responsive 375px: Dải tab cuộn ngang mượt mà, bảng cuộn trong khung không làm vỡ giao diện.
7. Chi tiết hóa đơn online: Có timeline 5 bước, có huy hiệu kho "Đã trừ kho", mở modal Hủy đơn bắt buộc nhập lý do thành công.
8. Chi tiết hóa đơn POS: Ẩn hoàn toàn tiến trình vận chuyển, hiển thị đúng nhân viên và thông tin thu tiền.
9. Bản in hóa đơn (`?print=true`): Hiển thị bản in `.invoice-paper` chuẩn hóa.

### 3. Danh sách ảnh chụp màn hình lưu trữ
Tất cả ảnh chụp màn hình đã được lưu tại thư mục `docs/images/gd22/screenshots/`:
1. `invoices_desktop_1280.png`: Giao diện danh sách Quản lý hóa đơn 1280px (dải tab trạng thái, thanh lọc, bảng 11 cột).
2. `invoices_mobile_375.png`: Giao diện danh sách trên thiết bị di động 375px (tab cuộn ngang mượt mà).
3. `invoice_detail_online_1280.png`: Chi tiết hóa đơn trực tuyến (timeline 5 bước, thông tin khách, nút chuyển bước, bảng sản phẩm, tổng tiền).
4. `invoice_detail_pos_1280.png`: Chi tiết hóa đơn tại quầy (không hiện vận chuyển, hiển thị nhân viên bán và dòng tiền).
5. `invoice_transition_modal.png`: Hộp thoại chuyển trạng thái (bắt buộc nhập lý do khi hủy).
6. `invoice_print_preview.png`: Bố cục in hóa đơn bán hàng chuyên dụng.
