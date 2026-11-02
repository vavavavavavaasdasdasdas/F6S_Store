# BÁO CÁO GIAI ĐOẠN 16: GỘP THỐNG KÊ VÀO DASHBOARD VÀ HÓA ĐƠN ĐỦ CHỨC NĂNG CỦA TRANG ĐƠN HÀNG CŨ

- **Nhánh làm việc**: `gop-y-sua-lan-2` (tạo từ `gop-y-dot-moi`)
- **Cơ sở dữ liệu kiểm thử**: `shopbangiay_kt_20261102_gd16_sua` (RESTORE mới từ bản sao lưu gần nhất `shopbangiay_kt_20261101_gd14_schema.bak`; tuyệt đối không chạm vào `shopbangiay` hoặc `shopbangiay_thu`).
- **Trạng thái schema**: Không thêm/xóa bảng hay cột; `spring.jpa.hibernate.ddl-auto=validate` sạch.

---

## 1. Chỗ đã thay đổi so với giai đoạn cũ (Luật 6)

| STT | Nội dung ở giai đoạn 9–15 cũ | Thay đổi ở Giai đoạn 16 (`gop-y-sua-lan-2`) |
|---|---|---|
| 1 | Tồn tại nhóm menu `THỐNG KÊ` và trang riêng `/admin/statistics` song song với `/admin/dashboard`. | Bỏ hoàn toàn nhóm menu `THỐNG KÊ` khỏi `AdminPageAdvice.NAV` và xóa template `templates/admin/thong-ke/thong-ke.html`. Mọi nội dung thống kê được gộp vào `/admin/dashboard`. Các đường dẫn `/admin/statistics`, `/admin/statistics/revenue`, `/admin/statistics/best-sellers`, `/admin/reports/**` chuyển hướng HTTP `302` về `/admin/dashboard` (giữ nguyên query string bộ lọc). |
| 2 | Trang Dashboard cũ bố cục nhiều cột (`row g-4`, `col-lg-6`, `col-xl-4`) đặt các bảng/biểu đồ cạnh nhau, có biểu đồ "Doanh thu theo ngày" và bảng "Đơn hàng gần đây", bộ lọc chỉ có 7/30 ngày. | Đổi sang **bố cục một cột tuyệt đối** (`.dashboard-single-column`): mọi bảng và biểu đồ chiếm 100% chiều ngang, xếp mỗi hàng đúng một khối từ trên xuống ở cả `1280px` và `375px`. Chỉ dải thẻ số KPI (Khối 1) xếp ngang tự xuống dòng. Bỏ hoàn toàn "Doanh thu theo ngày" và "Đơn hàng gần đây". |
| 3 | Bộ lọc ở `/admin/dashboard` và `/admin/statistics` tách rời, phân trang chưa tách độc lập cho đủ 8 bảng trên cùng một trang. | Hợp nhất thanh bộ lọc cố định khi cuộn (`.dashboard-sticky-filter`, `position: sticky`) ở đầu `/admin/dashboard`: `Từ ngày`, `Đến ngày`, `Khoảng nhanh (7 ngày / 30 ngày)`, `Kênh (Tất cả / Online / Tại quầy POS)`, `Thương hiệu`. Áp dụng chung cho mọi khối và ghi chú rõ phạm vi bộ lọc ở các khối không phụ thuộc kênh/thương hiệu. Mỗi bảng có tham số phân trang độc lập (`channelPage`, `statusPage`, `quantityPage`, `revenuePage`, `customerPage`, `reviewPage`, `favoritePage`, `lowStockPage`) giữ nguyên bộ lọc chung. |
| 4 | Trang Hóa đơn (`/admin/invoices`, `/admin/invoices/{id}`) ở Giai đoạn 13 thiếu một số chi tiết của trang Đơn hàng cũ: thiếu combo box lọc khách hàng (`customerId`), từ khóa khách hàng chưa tìm theo `customerCode`, trang chi tiết hóa đơn thiếu hiển thị `salesNote` (ghi chú đơn hàng) và thiếu nạp danh sách `provinces` vào `Model` cho form đổi tỉnh/thành (`#order-province-id`). | Bổ sung đầy đủ vào `AdminInvoiceController`, `templates/admin/hoa-don/danh-sach.html`, `templates/admin/hoa-don/chi-tiet.html`: combo box `#invoice-customerId`, tìm kiếm khách theo `customerCode`/`fullName`/`phone`/`receiverName`/`phoneNumber`, hiển thị `salesNote`, nạp `provinces` cho form cập nhật tỉnh/thành trên trang chi tiết hóa đơn, và hỗ trợ alias POST `/admin/invoices/update-status/{id}`. |
| 5 | Các bài test cũ (`StatisticsIntegrationTest`, `PaginationIntegrationTest`, `Phase3ComboFilterIntegrationTest`, `RouteErrorIntegrationTest`, `PosInvoiceIntegrationTest`, `EmployeeAccessIntegrationTest`) kỳ vọng `/admin/statistics` trả `200`. | Cập nhật test để kiểm tra `/admin/statistics` chuyển hướng `302` về `/admin/dashboard` (với `ROLE_ADMIN`) và vẫn trả `403` (với `ROLE_EMPLOYEE`), đồng thời kiểm tra toàn bộ 9 khối và bộ lọc hợp nhất ngay trên `/admin/dashboard`. |

---

## 2. Bảng đối chiếu chức năng trang Đơn hàng cũ (`trước Giai đoạn 13`) và trang Hóa đơn (Yêu cầu B.1)

Dữ liệu đối chiếu được trích xuất từ lịch sử git trước Giai đoạn 13 (`git show 928b12c~1:src/main/java/com/example/demodatn/controller/admin/donhang/AdminOrderController.java`, `templates/admin/don-hang/danh-sach.html`, `templates/admin/don-hang/chi-tiet.html`):

| STT | Chức năng ở Đơn hàng cũ | Route cũ | Đã có ở Hóa đơn chưa (trước khi sửa GĐ16) | Route mới ở Hóa đơn | Cách kiểm tra |
|---|---|---|---|---|---|
| 1 | Danh sách đơn hàng (Web khách đặt, Bán online, POS tại quầy), phân trang 12 dòng/trang, cột STT, Mã, Khách hàng, Kênh/Nguồn, Tổng tiền, Thanh toán, Trạng thái, Ngày tạo, Hành động icon | `GET /admin/orders` | Đã có | `GET /admin/invoices` | Truy cập `/admin/invoices`, kiểm tra hiển thị đủ đơn Web, Bán online, POS và phân trang 12 dòng. |
| 2 | Lọc theo mã đơn / mã hóa đơn (`q`), khoảng ngày (`from`, `to`), kênh (`channel`), nguồn đơn (`source`), trạng thái đơn (`status`), trạng thái thanh toán (`paymentStatus`), phương thức thanh toán (`paymentMethod`) | `GET /admin/orders?...` | Đã có | `GET /admin/invoices?...` | Lọc từng tiêu chí trên `/admin/invoices` và kiểm tra kết quả khớp SQL. |
| 3 | Lọc theo từ khóa khách hàng (`customer` gồm cả `customerCode`, tên, SĐT) và combo box chọn khách hàng (`customerId`) | `GET /admin/orders?customer=...&customerId=...` | **Thiếu** combo box `customerId` và tìm theo `customerCode` | `GET /admin/invoices?customer=...&customerId=...` | Đã bổ sung `#invoice-customerId` và mở rộng Specification tìm `customerCode`; kiểm tra bằng MockMvc & Playwright. |
| 4 | Xem chi tiết đơn hàng / hóa đơn: thông tin người nhận, SĐT, địa chỉ, kênh, nguồn, thanh toán, nhãn `"Đã trừ kho"` / `"Chưa trừ kho"`, bảng dòng hàng đầy đủ (`STT, Mã, Tên, Kích cỡ, Màu sắc, Chất liệu, Đế giày, Đơn giá, SL, Thành tiền`), tổng hợp tiền hàng / giảm giá / phí ship / tổng thanh toán | `GET /admin/orders/{id}` / `GET /admin/orders/detail/{id}` | Đã có | `GET /admin/invoices/{id}` / `GET /admin/invoices/detail/{id}` | Mở `/admin/invoices/{id}`, kiểm tra đủ cột biến thể và nhãn `"Đã trừ kho"`. |
| 5 | Hiển thị ghi chú đơn hàng (`salesNote`) trên trang chi tiết | `GET /admin/orders/{id}` | **Thiếu** hiển thị `salesNote` trên `hoa-don/chi-tiet.html` | `GET /admin/invoices/{id}` | Đã bổ sung hiển thị `order.salesNote` trong khung thông tin khách hàng/đơn hàng; kiểm tra bằng Playwright. |
| 6 | Thanh tiến trình trạng thái & nút chuyển từng bước hợp lệ (`CONFIRMED`, `PREPARING`, `SHIPPING`, `DELIVERED`) dùng `OrderStatusService` | `POST /admin/orders/{id}/status` & `POST /admin/orders/update-status/{id}` | Đã có `/{id}/status`, **thiếu** alias `/update-status/{id}` | `POST /admin/invoices/{id}/status` & `POST /admin/invoices/update-status/{id}` | Thực hiện chuyển trạng thái từng bước trên UI và kiểm tra DB `orders.status`, `order_status_history`. |
| 7 | Nhập đơn vị vận chuyển (`carrier`) và mã vận đơn (`trackingCode`) khi chuyển sang `SHIPPING`, hiển thị thông tin vận chuyển trên chi tiết | `POST /admin/orders/{id}/status` (`status=SHIPPING`) | Đã có | `POST /admin/invoices/{id}/status` (`status=SHIPPING`) | Nhập `Giao Hàng Nhanh` + `GHN-GD16-999` khi chuyển `SHIPPING`, kiểm tra hiển thị và DB. |
| 8 | Hủy đơn (`CANCELLED`) và Giao hàng thất bại (`DELIVERY_FAILED`) bắt buộc nhập lý do (`reason`), tự động hoàn kho nếu đơn đã trừ kho (`da_tru_kho=1 -> 0`) | `POST /admin/orders/{id}/status` | Đã có | `POST /admin/invoices/{id}/status` | Thực hiện nhánh `CANCELLED` (ở 375px) và `DELIVERY_FAILED` (ở 1280px), đối chiếu SQL `product_variants.quantity` trước/sau. |
| 9 | Xác nhận đã nhận tiền chuyển khoản (`confirmPayment`) cho đơn `BANK_TRANSFER` / `TRANSFER` | `POST /admin/orders/{id}/confirm-payment` | Đã có | `POST /admin/invoices/{id}/confirm-payment` | Bấm `"Xác nhận đã nhận chuyển khoản"`, kiểm tra `payment_status` chuyển từ `UNPAID` sang `PAID`. |
| 10 | Cập nhật Tỉnh/Thành phố giao hàng trước khi giao vận chuyển và ghi lịch sử | `POST /admin/orders/{id}/province` | Đã có route POST nhưng **thiếu** nạp `provinces` vào `Model` ở `detail()` | `POST /admin/invoices/{id}/province` | Đã bổ sung `model.addAttribute("provinces", provinces.findAllByOrderByNameAsc())`; chọn tỉnh mới trên UI và kiểm tra SQL. |
| 11 | Hiển thị mã QR VietQR (tạo nội bộ bằng JS, không gọi dịch vụ ngoài) và hạn thanh toán cho đơn chuyển khoản chưa thanh toán | `GET /admin/orders/{id}` | Đã có | `GET /admin/invoices/{id}` | Kiểm tra `canvas[data-vietqr]` hiển thị khi đơn `BANK_TRANSFER` đang `UNPAID`. |
| 12 | Khung văn bản và nút `"Sao chép tin nhắn xác nhận"` (`[data-copy-confirmation]`) cho đơn Online | `GET /admin/orders/{id}` | Đã có | `GET /admin/invoices/{id}` | Bấm nút sao chép trên `/admin/invoices/{id}`, kiểm tra thông báo trạng thái sao chép. |
| 13 | Bảng lịch sử trạng thái đơn hàng (`STT, Thời gian, Trạng thái, Người thực hiện, Vận chuyển, Ghi chú/Lý do`) | `GET /admin/orders/{id}` | Đã có | `GET /admin/invoices/{id}` | Kiểm tra bảng lịch sử tăng số dòng sau mỗi thao tác chuyển trạng thái/đổi tỉnh. |
| 14 | Bản in hóa đơn (`?print=true` và nút `"In hóa đơn"`) | `GET /admin/orders/{id}?print=true` | Đã có | `GET /admin/invoices/{id}?print=true` | Mở `/admin/invoices/{id}?print=true` ở 1280px và 375px. |
| 15 | Chuyển hướng toàn bộ URL `/admin/orders...` cũ sang `/admin/invoices...` tương ứng (`302` cho GET, `307` cho POST) | `/admin/orders/**` | Đã có trong `AdminOrderController` | `/admin/invoices/**` | Kiểm tra bằng `InvoiceOnlineWorkspaceIntegrationTest` và `RouteErrorIntegrationTest`. |

---

## 3. Bảng kết quả kiểm thử tổng hợp (Luật 1)

| Hạng mục | Cách kiểm tra | Kết quả thực tế | PASS/FAIL/NOT TESTED |
|---|---|---|---|
| **A.1** Bỏ nhóm menu `THỐNG KÊ` và trang `/admin/statistics`; chuyển hướng `302` các URL thống kê/báo cáo cũ về `/admin/dashboard` (giữ query string); xóa mọi liên kết cũ | Kiểm tra `AdminPageAdvice.NAV`, chạy `git grep` toàn bộ `src/main/`, kiểm tra HTTP status + `Location` header cho `/admin/statistics`, `/admin/statistics/revenue`, `/admin/statistics/best-sellers`, `/admin/reports/revenue`, `/admin/reports/best-sellers`, `/admin/statistics?from=2026-01-01&channel=ONLINE` | Menu không còn `THỐNG KÊ`; toàn bộ 6 URL cũ trả `302` về `/admin/dashboard` (giữ nguyên `?from=2026-01-01&channel=ONLINE`); `git grep` xác nhận không còn link cũ ngoài controller redirect | **PASS** |
| **A.2** Bố cục một cột tuyệt đối trên `/admin/dashboard` ở cả `1280px` và `375px`; chỉ dải thẻ KPI xếp ngang tự xuống dòng | Đo bounding box (`top`, `bottom`, `left`, `width`) của toàn bộ `.dashboard-block`, `table`, `canvas` bằng Playwright ở `1280px` và `375px` (`single-column-proof-1280.json`) | Ở cả `1280px` và `375px`: `blocks[i+1].top >= blocks[i].bottom` với mọi khối từ 2 đến 9; `tablesAndCharts[i+1].top >= tablesAndCharts[i].bottom`; `scrollWidth <= viewport` (không tràn ngang) | **PASS** |
| **A.3** Thanh bộ lọc cố định khi cuộn (`Từ ngày`, `Đến ngày`, `Khoảng nhanh`, `Kênh`, `Thương hiệu`) áp dụng chung; ghi chú rõ ở các khối không phụ thuộc `Kênh` | Kiểm tra `getComputedStyle(el).position === 'sticky'` trên `.dashboard-sticky-filter`; kiểm tra ghi chú `"không áp dụng bộ lọc Kênh"` và đối chiếu số liệu 8 chế độ lọc với SQL độc lập | `position: sticky` hoạt động đúng; các khối độc lập kênh có ghi chú `"không áp dụng bộ lọc Kênh"`; số liệu khớp tuyệt đối 100% với SQL độc lập (`dashboard-sql-pairs.json`) | **PASS** |
| **A.4** Đúng thứ tự 9 khối trên `/admin/dashboard`, gộp không trùng lặp, mỗi bảng có phân trang riêng giữ bộ lọc chung, bỏ `"Doanh thu theo ngày"` và `"Đơn hàng gần đây"` | Kiểm tra thứ tự `data-dashboard-block` (`channels`, `statuses`, `top-quantity`, `top-revenue`, `top-customers`, `reviews`, `favorites`, `low-stock` sau dải KPI), kiểm tra cột bảng Top SL (`STT \| Ảnh \| Mã \| Tên \| Số lượng \| Doanh thu`), kiểm tra loại khách vãng lai khỏi Top khách hàng, kiểm tra không còn `"Doanh thu theo ngày"` và `"Đơn hàng gần đây"` | Thứ tự 9 khối chính xác; bảng Top sản phẩm theo SL đủ 6 cột (`STT \| Ảnh \| Mã \| Tên \| Số lượng \| Doanh thu`); khách vãng lai không xếp hạng; 8 tham số phân trang độc lập giữ nguyên query bộ lọc; không còn `"Doanh thu theo ngày"` và `"Đơn hàng gần đây"` | **PASS** |
| **A.5** Phân quyền Dashboard như Giai đoạn 12: Nhân viên (`ROLE_EMPLOYEE`) vào `/admin/dashboard` và `/admin/statistics` bị `403` | Kiểm tra bằng Playwright (`security-role-and-csrf-checks`) và `EmployeeAccessIntegrationTest` | `ROLE_EMPLOYEE` nhận HTTP `403` tại `/admin/dashboard` và `/admin/statistics`, nhận HTTP `200` tại `/admin/invoices` | **PASS** |
| **B.1 & B.2** Trang Hóa đơn (`/admin/invoices`, `/admin/invoices/{id}`) có đầy đủ chức năng của trang Đơn hàng cũ | Chạy kịch bản Playwright tạo đơn `BANK_TRANSFER` (kiểm tra QR, sao chép tin nhắn, `salesNote`, xác nhận thanh toán, đổi tỉnh/thành, chuyển `CONFIRMED -> PREPARING -> SHIPPING -> DELIVERED`, in hóa đơn) và 2 nhánh `CANCELLED` (375px), `DELIVERY_FAILED` (1280px), lọc theo `customerCode` và `#invoice-customerId` | Toàn bộ thao tác chạy thành công với 0 lỗi console; tồn kho biến thể `id=3` giảm từ `18 -> 17` khi đặt chuyển khoản, giảm `17 -> 16` khi xác nhận đơn COD và hoàn lại `16 -> 17` khi hủy hoặc giao thất bại (`invoice-workflows-sql.json`) | **PASS** |
| **B.3** Menu chỉ có `"Hóa đơn"` (không có `"Đơn hàng"`), mọi URL `/admin/orders...` chuyển hướng `302`/`307` sang hóa đơn tương ứng, trang `/orders` của khách giữ nguyên | Kiểm tra menu `.admin-nav`, kiểm tra `git grep` và chạy `InvoiceOnlineWorkspaceIntegrationTest` + `RouteErrorIntegrationTest` | Menu chỉ có `"Hóa đơn"`; `/admin/orders` chuyển hướng `302` sang `/admin/invoices`, POST `/admin/orders/{id}/status` chuyển hướng `307` sang `/admin/invoices/{invoiceId}/status`; `/orders` của khách hoạt động bình thường | **PASS** |
| **Luật 8** Kiểm thử tự động `mvnw clean test`, Playwright `1280px` & `375px`, 8 kịch bản bảo mật, crawl toàn bộ link nội bộ | Chạy `mvnw clean test` (không skip), `verify_gd16.py` và `crawl.py` | `Tests run: 248, Failures: 0, Errors: 0, Skipped: 0`; 18 bước Playwright PASS; 8 kịch bản CSRF/권한 PASS; Crawl `475` link admin, `214` link employee, `22` link user, `20` link guest đều PASS (`0` link hỏng) | **PASS** |

---

## 4. Bằng chứng thô (Raw Evidence)

### 4.1. Đối chiếu số liệu trên trang `/admin/dashboard` với `SELECT` SQL độc lập (`docs/evidence/gd16/dashboard-sql-pairs.json`)

| Chế độ bộ lọc | Khoảng ngày / Tham số | Chỉ số | Số trên trang (`/admin/dashboard`) | Kết quả `SELECT` SQL độc lập | Khớp |
|---|---|---|---|---|---|
| `default_7d` | `2026-10-27` .. `2026-11-02`, `channel=ALL` | Tổng doanh thu / Tiền mặt / Chuyển khoản / Đơn hợp lệ / Tổng đơn / Yêu thích | `1.200.000 đ` / `500.000 đ` / `700.000 đ` / `2` / `2` / `2` | `1200000.00` / `500000.00` / `700000.00` / `2` / `2` / `2` | PASS |
| `quick_7d` | `days=7` (`2026-10-27` .. `2026-11-02`) | Tổng doanh thu / Tiền mặt / Chuyển khoản / Đơn hợp lệ / Tổng đơn / Yêu thích | `1.200.000 đ` / `500.000 đ` / `700.000 đ` / `2` / `2` / `2` | `1200000.00` / `500000.00` / `700000.00` / `2` / `2` / `2` | PASS |
| `quick_30d` | `days=30` (`2026-10-04` .. `2026-11-02`) | Tổng doanh thu / Tiền mặt / Chuyển khoản / Đơn hợp lệ / Tổng đơn / Yêu thích | `28.230.000 đ` / `17.690.000 đ` / `10.540.000 đ` / `13` / `31` / `2` | `28230000.00` / `17690000.00` / `10540000.00` / `13` / `31` / `2` | PASS |
| `empty_range` | `from=2041-01-01&to=2041-01-02` | Tổng doanh thu / Tiền mặt / Chuyển khoản / Đơn hợp lệ / Tổng đơn / Yêu thích | `0 đ` / `0 đ` / `0 đ` / `0` / `0` / `0` | `0.00` / `0.00` / `0.00` / `0` / `0` / `0` | PASS |
| `channel_ONLINE` | `days=30&channel=ONLINE` | Tổng doanh thu / Tiền mặt / Chuyển khoản / Đơn hợp lệ / Tổng đơn | `6.050.000 đ` / `5.150.000 đ` / `900.000 đ` / `5` / `13` | `6050000.00` / `5150000.00` / `900000.00` / `5` / `13` | PASS |
| `channel_POS` | `days=30&channel=POS` | Tổng doanh thu / Tiền mặt / Chuyển khoản / Đơn hợp lệ / Tổng đơn | `22.180.000 đ` / `12.540.000 đ` / `9.640.000 đ` / `8` / `18` | `22180000.00` / `12540000.00` / `9640000.00` / `8` / `18` | PASS |
| `brand_A` | `days=30&categoryId=129` | Doanh thu thương hiệu / Tiền mặt / Chuyển khoản / Đơn hợp lệ / Tổng đơn / Yêu thích | `500.000 đ` / `500.000 đ` / `0 đ` / `1` / `1` / `2` | `500000.00` / `500000.00` / `0.00` / `1` / `1` / `2` | PASS |
| `brand_B` | `days=30&categoryId=130` | Doanh thu thương hiệu / Tiền mặt / Chuyển khoản / Đơn hợp lệ / Tổng đơn / Yêu thích | `1.600.000 đ` / `0 đ` / `1.600.000 đ` / `2` / `2` / `0` | `1600000.00` / `0.00` / `1600000.00` / `2` / `2` / `0` | PASS |

### 4.2. Bằng chứng SQL trước/sau cho toàn bộ luồng xử lý Hóa đơn (`docs/evidence/gd16/invoice-workflows-sql.json`)

- **Biến thể kiểm thử**: `variantId = 3`, tồn kho ban đầu `quantity = 18`.
- **Luồng A (Đơn Online chuyển khoản `DH000674` / Hóa đơn `HD000674`, `invoice_id = 287`, `order_id = 674`)**:
  - Sau khi tạo đơn `BANK_TRANSFER` (`sales_note = "Ghi chú kiểm thử GĐ16 — gọi trước khi giao"`): `status = PENDING`, `payment_status = UNPAID`, `da_tru_kho = true`, `tinh_thanh_id = 1` (Hà Nội), tồn kho giảm `18 -> 17`.
  - Sau `POST /admin/invoices/287/confirm-payment`: `payment_status = PAID`, `status = PENDING`, `historyCount = 1`.
  - Sau `POST /admin/invoices/287/province` (`provinceId = 2`): `tinh_thanh_id = 2`, `ten_tinh_thanh = "Cao Bằng"`, `historyCount = 2`.
  - Sau `POST /admin/invoices/287/status` (`CONFIRMED`): `status = CONFIRMED`, `da_tru_kho = true`, `quantity = 17`, `historyCount = 3`.
  - Sau `POST /admin/invoices/287/status` (`PREPARING`): `status = PREPARING`, `historyCount = 4`.
  - Sau `POST /admin/invoices/287/status` (`SHIPPING`, `carrier = "Giao Hàng Nhanh"`, `trackingCode = "GHN-GD16-999"`): `status = SHIPPING`, `historyCount = 5`.
  - Sau `POST /admin/invoices/287/status` (`DELIVERED`): `status = DELIVERED`, `payment_status = PAID`, `da_tru_kho = true`, `quantity = 17`, `historyCount = 6`.
- **Luồng B (Đơn Online COD xác nhận ngay rồi Hủy đơn ở `375px`, `order_id = 675`)**:
  - Trước khi đặt: `quantity = 17`.
  - Sau khi đặt có tích `confirmNow`: `status = CONFIRMED`, `da_tru_kho = true`, `quantity = 16`.
  - Sau khi hủy đơn (`CANCELLED`, lý do `"Khách đổi ý hủy đơn GĐ16"`): `status = CANCELLED`, `da_tru_kho = false`, tồn kho hoàn lại `quantity = 17`.
- **Luồng C (Đơn Online COD giao vận chuyển rồi Giao hàng thất bại ở `1280px`, `invoice_id = 289`, `order_id = 676`)**:
  - Trước khi đặt: `quantity = 17`.
  - Sau khi chuyển `CONFIRMED -> PREPARING -> SHIPPING -> DELIVERY_FAILED` (lý do `"Không liên lạc được với khách 3 lần"`): `status = DELIVERY_FAILED`, `da_tru_kho = false`, tồn kho hoàn lại `quantity = 17`.
- Sau khi kết thúc kiểm thử trình duyệt, cơ sở dữ liệu `shopbangiay_kt_20261102_gd16_sua` đã được RESTORE lại về checkpoint sạch ban đầu (`Restored gd16_sua checkpoint`).

### 4.3. Bằng chứng `git grep` không còn tham chiếu `/admin/orders`, `/admin/statistics`, `/admin/reports` ngoài controller chuyển hướng

```text
$ git grep -n -E "/admin/orders|/admin/statistics|/admin/reports" src/main/
src/main/java/com/example/demodatn/controller/admin/donhang/AdminOrderController.java:10:@RequestMapping("/admin/orders")
src/main/java/com/example/demodatn/controller/admin/thongke/AdminStatisticsController.java:12:    @GetMapping({"/admin/statistics", "/admin/statistics/**", "/admin/reports", "/admin/reports/**"})
```

### 4.4. Bằng chứng 8 kịch bản bảo mật & Crawl toàn bộ liên kết nội bộ (`docs/evidence/gd16/browser-results.json`, `docs/evidence/gd16/crawl-manifest.json`)

- **Phân quyền Employee (`ROLE_EMPLOYEE`)**:
  - `GET /admin/dashboard` -> HTTP `403`
  - `GET /admin/statistics` -> HTTP `403`
  - `GET /admin/invoices` -> HTTP `200`
- **8 kịch bản CSRF (thiếu token & sai token trên 4 endpoint POST)**:
  - `POST /admin/invoices/{id}/status` (missing CSRF -> `403`, invalid CSRF -> `403`)
  - `POST /admin/invoices/{id}/confirm-payment` (missing CSRF -> `403`, invalid CSRF -> `403`)
  - `POST /admin/invoices/{id}/province` (missing CSRF -> `403`, invalid CSRF -> `403`)
  - `POST /admin/ban-online/add` (missing CSRF -> `403`, invalid CSRF -> `403`)
- **Crawl toàn bộ link nội bộ**:
  - `admin`: `475` URL (`exhausted: true`, `0` lỗi)
  - `employee`: `214` URL (`exhausted: true`, `0` lỗi)
  - `user`: `22` URL (`exhausted: true`, `0` lỗi)
  - `guest`: `20` URL (`exhausted: true`, `0` lỗi)

### 4.5. Kết quả `mvnw clean test` (`docs/evidence/gd16/maven-summary.txt`)

- `Tests run: 248, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS`.
- Không có thay đổi schema DB ở Giai đoạn 16 nên không cần chạy thêm migration SQL nào trên DB thật.
