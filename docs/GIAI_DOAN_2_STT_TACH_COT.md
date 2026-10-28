# Giai đoạn 2 — Số thứ tự và tách cột

Nhánh `gop-y-giang-vien`, ngày 28/10/2026. Chỉ thực hiện giai đoạn 2. Không merge/push.

Kết quả: **12 hạng mục PASS / 0 FAIL / 0 NOT TESTED trong phạm vi nghiệm thu**. Thiết bị/máy in vật lý và trình duyệt khác Chromium không kiểm thử; không suy rộng kết quả PDF sang máy in thật.

| Hạng mục | Cách kiểm tra | Kết quả thực tế | PASS/FAIL/NOT TESTED |
|---|---|---|---|
| Kiểm kê trước sửa | Quét source, mở bản trước ở cổng 18121 | 25 mẫu table / 29 biến thể; 38 bảng trong phạm vi gồm bản in và danh sách chuyển sang bảng | PASS |
| STT toàn bộ bảng | Crawl mọi bảng ở 1280/375; kiểm tra mỗi dòng | STT đứng đầu, liên tục, độc lập ID; trang 2 bắt đầu 13; bảng không phân trang bắt đầu 1 | PASS |
| Tách cột / giữ dữ liệu | Đối chiếu từng ô, thứ tự, ảnh, link của 108 lượt bảng | 108/108 khớp; 9 nhóm danh sách mới giữ nguyên dữ liệu; mỗi trường có cột riêng | PASS |
| Bộ lọc / phân trang | Click Sau với keyword/sort/materialId và hai bảng top | Giữ query; page/historyPage, quantityPage/revenuePage độc lập; STT đúng 13 | PASS |
| Mobile / cuộn ngang | Đo root và frame; dùng phím ArrowRight khi focus khung | Tất cả 49 URL–vai trò mỗi viewport: 1265px/1280, 360px/375; cuộn trong khung | PASS |
| Giỏ User / POS | Sửa SL 1→2→1, xóa, thêm lại, mở hóa đơn chờ | Thao tác thành công; sau xóa 13 dòng đánh lại STT 1…13; mở chờ khôi phục 14 dòng | PASS |
| Chọn khách / sản phẩm giảm giá | AJAX tìm/chọn khách; tick và lưu trang 2 | customerId đúng; lựa chọn lưu đúng và giữ q/page; có SELECT xác nhận | PASS |
| Bản in | Xuất PDF A4 và render cả 2 trang | 14 mã sản phẩm đầy đủ; trang 2 có STT 13,14; lặp tiêu đề, không cắt cột | PASS |
| Crawl và JavaScript | HTTP thực, pageerror/console, tài nguyên | 98 lượt chính + 5 alias; 0 JS exception, 0 request 404/500, không trang trắng | PASS |
| Bảo mật | 8 kịch bản quyền, CSRF, sở hữu | S01–S08 PASS; thiếu/sai CSRF và User POST Admin trả 403; dữ liệu người khác không đổi | PASS |
| Maven | mvnw.cmd clean test, không skip | 133 tests, Failures 0, Errors 0, Skipped 0; BUILD SUCCESS | PASS |
| Database / schema | Validate; so metadata/COUNT; RESTORE sau nhóm thử | Hai bản sao mới; 21 bảng khớp ban đầu sau khôi phục; schema không đổi, không cần migration mới | PASS |

## Kiểm kê và bảng trước/sau

[Bảng kiểm kê trước sửa](GIAI_DOAN_2_KIEM_KE_TRUOC_KHI_SUA.md) đã được công bố trong hội thoại trước khi sửa. Các cột được xác nhận lại từ bản trước chạy cùng fixture. [JSON tên/số cột](evidence/gd2/columns-before-after.json). Các mục trước là danh sách được ghi 0 cột HTML, không giả định chúng đã là table.

Giữ ảnh, chất liệu, tài khoản và ID vốn có dưới cột riêng để không mất thông tin. Thêm Mã sản phẩm vào dòng đơn/giỏ/tồn kho/top nơi cần; thêm Địa chỉ vào bảng khách/nhân viên. `Kích cỡ` là nhãn tiếng Việt tương ứng Size. Mã lớp, route, enum, bảng và cột database không đổi.

| URL / bảng | Số cột trước → sau | Tên cột trước | Tên cột sau | Kết quả |
|---|---|---|---|---|
| /admin/products-crud? — Sản phẩm | 9 → 11 | Ảnh; Mã / Tên sản phẩm; Thương hiệu; Đế giày; Chất liệu; Giá (₫); Tồn kho; Trạng thái; Hành động | STT; Ảnh; Mã; Tên sản phẩm; Thương hiệu; Đế giày; Chất liệu; Giá (₫); Tồn kho; Trạng thái; Hành động | PASS |
| /admin/variants? — Biến thể | 7 → 9 | Sản phẩm; Kích cỡ; Chất liệu; Giá (₫); Tồn kho; Trạng thái; Hành động | STT; Mã; Tên sản phẩm; Kích cỡ; Chất liệu; Giá (₫); Tồn kho; Trạng thái; Hành động | PASS |
| /admin/products-crud/view/{id} — Biến thể trong chi tiết sản phẩm | 5 → 6 | Kích cỡ; Chất liệu; Giá (₫); Tồn kho; Trạng thái | STT; Kích cỡ; Chất liệu; Giá (₫); Tồn kho; Trạng thái | PASS |
| /admin/categories? — Thương hiệu | 4 → 5 | ID; Tên thương hiệu; Trạng thái; Hành động | STT; ID; Tên thương hiệu; Trạng thái; Hành động | PASS |
| /admin/sizes? — Kích cỡ | 3 → 4 | ID; Kích cỡ; Hành động | STT; ID; Kích cỡ; Hành động | PASS |
| /admin/materials? — Chất liệu | 5 → 6 | ID; Tên chất liệu; Trạng thái; Đang dùng; Hành động | STT; ID; Tên chất liệu; Trạng thái; Đang dùng; Hành động | PASS |
| /admin/soles? — Đế giày | 5 → 6 | ID; Tên đế giày; Trạng thái; Đang dùng; Hành động | STT; ID; Tên đế giày; Trạng thái; Đang dùng; Hành động | PASS |
| /admin/inventory? — Tồn kho | 6 → 8 | Sản phẩm; Kích cỡ; Chất liệu; Tồn kho; Trạng thái biến thể; Hành động | STT; Mã; Tên sản phẩm; Kích cỡ; Chất liệu; Tồn kho; Trạng thái biến thể; Hành động | PASS |
| /admin/inventory? — Lịch sử kho | 7 → 11 | Thời gian; Sản phẩm / Kích cỡ; Trước; Thay đổi; Sau; Lý do; Người thực hiện | STT; Thời gian; Mã; Tên sản phẩm; Kích cỡ; Chất liệu; Trước; Thay đổi; Sau; Lý do; Người thực hiện | PASS |
| /admin/customers? — Khách hàng | 5 → 9 | Mã / Họ tên; Liên hệ; Tài khoản; Trạng thái; Hành động | STT; Mã; Họ tên; SĐT; Email; Địa chỉ; Tài khoản; Trạng thái; Hành động | PASS |
| /admin/employees? — Nhân viên | 5 → 9 | Mã / Họ tên; Liên hệ; Tài khoản; Trạng thái; Hành động | STT; Mã; Họ tên; SĐT; Email; Địa chỉ; Tài khoản; Trạng thái; Hành động | PASS |
| /admin/customers/detail/{id} — Lịch sử đơn của khách | 4 → 5 | Mã đơn; Ngày tạo; Trạng thái; Thành tiền | STT; Mã đơn; Ngày tạo; Trạng thái; Thành tiền | PASS |
| /admin/customers/detail/{id} — Địa chỉ khách | 0 → 5 | Người nhận; SĐT; Địa chỉ; Mặc định | STT; Người nhận; SĐT; Địa chỉ; Mặc định | PASS |
| /admin/orders? — Đơn hàng Admin | 7 → 8 | Mã đơn; Kênh; Khách; Trạng thái; Thanh toán; Tổng (đ); Ngày tạo | STT; Mã đơn; Kênh; Khách; Trạng thái; Thanh toán; Tổng (đ); Ngày tạo | PASS |
| /admin/orders/{id}; /admin/orders/detail/{id} — Chi tiết đơn Admin | 6 → 8 | Sản phẩm; Kích cỡ; Chất liệu; SL; Đơn giá (đ); Thành tiền (đ) | STT; Mã; Tên sản phẩm; Kích cỡ; Chất liệu; SL; Đơn giá (đ); Thành tiền (đ) | PASS |
| /admin/invoices? — Hóa đơn | 7 → 8 | Hóa đơn; Đơn hàng; Ngày; Khách; Nhân viên; Tổng (đ); Thanh toán | STT; Hóa đơn; Đơn hàng; Ngày; Khách; Nhân viên; Tổng (đ); Thanh toán | PASS |
| /admin/invoices/{id} — Chi tiết hóa đơn | 6 → 9 | Sản phẩm; Kích cỡ; Chất liệu; SL; Đơn giá (đ); Thành tiền (đ) | STT; Ảnh; Mã; Tên sản phẩm; Kích cỡ; Chất liệu; SL; Đơn giá (đ); Thành tiền (đ) | PASS |
| /admin/invoices/{id} — chế độ in — Bản in hóa đơn | 6 → 9 | Sản phẩm; Kích cỡ; Chất liệu; SL; Đơn giá (đ); Thành tiền (đ) | STT; Ảnh; Mã; Tên sản phẩm; Kích cỡ; Chất liệu; SL; Đơn giá (đ); Thành tiền (đ) | PASS |
| /admin/vouchers? — Phiếu giảm giá | 7 → 13 | Mã / Tên; Mức giảm; Phạm vi; Thời gian; Lượt dùng; Trạng thái; Hành động | STT; Mã; Tên; Loại giảm; Giá trị giảm; Phạm vi; Khách hàng áp dụng; Bắt đầu; Kết thúc; Đã dùng; Giới hạn lượt; Trạng thái; Hành động | PASS |
| /admin/promotions? — Đợt giảm giá | 5 → 8 | Tên; Mức giảm; Thời gian; Trạng thái; Hành động | STT; Tên; Loại giảm; Giá trị giảm; Bắt đầu; Kết thúc; Trạng thái; Hành động | PASS |
| /admin/promotions/{id}/products — Chọn sản phẩm giảm giá | 4 → 5 | Áp dụng; Mã; Sản phẩm; Trạng thái | STT; Áp dụng; Mã; Sản phẩm; Trạng thái | PASS |
| /admin/statistics; /admin/statistics/revenue; /admin/reports/revenue — Doanh thu ngày | 3 → 4 | Ngày; Số đơn; Doanh thu (đ) | STT; Ngày; Số đơn; Doanh thu (đ) | PASS |
| /admin/statistics/best-sellers; /admin/reports/best-sellers — Top số lượng | 4 → 6 | #; Sản phẩm; SL bán; Doanh thu (đ) | STT; Ảnh; Mã; Tên sản phẩm; SL bán; Doanh thu (đ) | PASS |
| /admin/statistics/best-sellers; /admin/reports/best-sellers — Top doanh thu | 4 → 6 | #; Sản phẩm; SL bán; Doanh thu (đ) | STT; Ảnh; Mã; Tên sản phẩm; SL bán; Doanh thu (đ) | PASS |
| /admin/statistics*; /admin/reports/* — Thống kê trạng thái | 2 → 3 | Trạng thái; Số đơn | STT; Trạng thái; Số đơn | PASS |
| /admin/dashboard — Dashboard — ngày | 2 → 3 | Ngày; Doanh thu (₫) | STT; Ngày; Doanh thu (₫) | PASS |
| /admin/dashboard — Dashboard — đơn gần đây | 4 → 6 | Mã đơn / Ngày tạo; Khách hàng; Trạng thái; Tổng tiền | STT; Mã đơn; Ngày tạo; Khách hàng; Trạng thái; Tổng tiền | PASS |
| /admin/dashboard — Dashboard — sắp hết | 5 → 7 | Sản phẩm; Kích cỡ; Chất liệu; Tồn kho; Thao tác | STT; Mã; Tên sản phẩm; Kích cỡ; Chất liệu; Tồn kho; Thao tác | PASS |
| /admin/dashboard — Dashboard — trạng thái | 0 → 3 | Trạng thái; Số đơn | STT; Trạng thái; Số đơn | PASS |
| /admin/dashboard — Dashboard — bán chạy | 0 → 4 | Tên sản phẩm; Số lượng | STT; Mã; Tên sản phẩm; Số lượng | PASS |
| /orders — Đơn User | 5 → 6 | Mã Đơn Hàng; Ngày đặt; Tổng tiền; Trạng thái; Hành động | STT; Mã Đơn Hàng; Ngày đặt; Tổng tiền; Trạng thái; Hành động | PASS |
| /orders/{id} — Chi tiết đơn User | 6 → 9 | Sản phẩm; Kích cỡ; Chất liệu; Số lượng; Đơn giá; Thành tiền | STT; Ảnh; Mã; Tên sản phẩm; Kích cỡ; Chất liệu; Số lượng; Đơn giá; Thành tiền | PASS |
| /cart — Giỏ hàng | 5 → 11 | Sản Phẩm; Đơn Giá; Số Lượng; Thành Tiền; Thao Tác | STT; Ảnh; Mã; Tên sản phẩm; Kích cỡ; Chất liệu; Đơn giá; Giá gốc; Số lượng; Thành tiền; Thao tác | PASS |
| /checkout — Checkout | 0 → 11 | Ảnh; Tên; Kích cỡ; Chất liệu; Số lượng; Giá bán; Giá gốc; Giảm (%) | STT; Ảnh; Mã; Tên sản phẩm; Kích cỡ; Chất liệu; Số lượng; Đơn giá; Giá gốc; Giảm (%); Thành tiền | PASS |
| /orders/success/{id} — Đặt hàng thành công | 0 → 8 | Tên; Kích cỡ; Chất liệu; Số lượng | STT; Mã; Tên sản phẩm; Kích cỡ; Chất liệu; Số lượng; Đơn giá; Thành tiền | PASS |
| /admin/pos — Giỏ POS | 0 → 9 | Tên; Kích cỡ; Chất liệu; Giá; Thành tiền; Số lượng; Hành động | STT; Mã; Tên sản phẩm; Kích cỡ; Chất liệu; Đơn giá; Số lượng; Thành tiền; Hành động | PASS |
| /admin/pos — Hóa đơn chờ POS | 0 → 7 | Mã; Chất liệu; Số món; Tổng tiền; Hết hạn; Hành động | STT; Mã; Chất liệu; Số món; Tổng tiền; Hết hạn; Hành động | PASS |
| /admin/pos; GET /admin/pos/customers — Chọn khách POS | 0 → 4 | Họ tên; SĐT | STT; Họ tên; SĐT; Hành động | PASS |

## Cách đánh STT và giữ hành vi

Spring Page.number bắt đầu từ 0, nên công thức là `Page.number * Page.size + rowIndex.count`. Với cỡ trang 12: URL page=1 là trang hiển thị số 2, dòng đầu STT=13. Danh sách không phân trang dùng rowIndex.count từ 1; chi tiết và bản in không lấy page của bảng cha. ID vẫn là dữ liệu riêng, không dùng làm STT. Hai bảng lịch sử/tồn kho và top số lượng/doanh thu dùng đúng Page tương ứng.

Dùng chung `.data-table` và các khung `.table-wrap` / `.table-responsive` / `.data-table-scroll`, có focus để cuộn bằng bàn phím. `position:relative` giữ nhãn ẩn của input nằm trong vùng cuộn. Không che overflow của toàn trang. In hóa đơn dùng cùng dữ liệu, chỉ thay độ rộng cột khi in.

[108 so sánh bảng đã render](evidence/gd2/table-comparison.json) giữ đúng giá trị, thứ tự, ảnh và link. [9 đối chiếu danh sách](evidence/gd2/logical-comparison.json) so bằng dữ liệu thật trước/sau, có SHA-256. Không coi khác biệt xuống dòng/trình bày là thay đổi dữ liệu. Các ô quantity vẫn giữ input và form cũ.

## URL, vai trò và status

Mỗi URL dưới đây đã chạy ở cả 1280 và 375px. Nguồn raw nằm trong browser-final.json, gồm header, colspan từng dòng, STT, hình học frame, link hành động và status request. Dữ liệu liên hệ gốc được giữ riêng ở verify/gd2; không đưa vào bằng chứng Git. Endpoint fragment khách POS được thử bằng GET và phân trang AJAX trong trang POS có stylesheet, không lấy fragment trần làm trang độc lập.

| URL | Vai trò | 1280 | 375 | Kết quả |
|---|---|---|---|---|
| `/admin/dashboard` | admin | 200 | 200 | PASS |
| `/admin/products-crud?keyword=GD2-P&sort=code&page=0` | admin | 200 | 200 | PASS |
| `/admin/products-crud?keyword=GD2-P&sort=code&page=1` | admin | 200 | 200 | PASS |
| `/admin/variants?productId=949&page=0` | admin | 200 | 200 | PASS |
| `/admin/variants?productId=949&page=1` | admin | 200 | 200 | PASS |
| `/admin/products-crud/view/949?page=0` | admin | 200 | 200 | PASS |
| `/admin/products-crud/view/949?page=1` | admin | 200 | 200 | PASS |
| `/admin/categories?page=0` | admin | 200 | 200 | PASS |
| `/admin/categories?page=1` | admin | 200 | 200 | PASS |
| `/admin/sizes?page=0` | admin | 200 | 200 | PASS |
| `/admin/sizes?page=1` | admin | 200 | 200 | PASS |
| `/admin/materials?page=0` | admin | 200 | 200 | PASS |
| `/admin/materials?page=1` | admin | 200 | 200 | PASS |
| `/admin/soles?page=0` | admin | 200 | 200 | PASS |
| `/admin/soles?page=1` | admin | 200 | 200 | PASS |
| `/admin/customers?page=0` | admin | 200 | 200 | PASS |
| `/admin/customers?page=1` | admin | 200 | 200 | PASS |
| `/admin/employees?page=0` | admin | 200 | 200 | PASS |
| `/admin/employees?page=1` | admin | 200 | 200 | PASS |
| `/admin/orders?page=0` | admin | 200 | 200 | PASS |
| `/admin/orders?page=1` | admin | 200 | 200 | PASS |
| `/admin/invoices?page=0` | admin | 200 | 200 | PASS |
| `/admin/invoices?page=1` | admin | 200 | 200 | PASS |
| `/admin/vouchers?page=0` | admin | 200 | 200 | PASS |
| `/admin/vouchers?page=1` | admin | 200 | 200 | PASS |
| `/admin/promotions?page=0` | admin | 200 | 200 | PASS |
| `/admin/promotions?page=1` | admin | 200 | 200 | PASS |
| `/admin/inventory?page=0&historyPage=0` | admin | 200 | 200 | PASS |
| `/admin/inventory?page=1&historyPage=1` | admin | 200 | 200 | PASS |
| `/admin/customers/detail/100?page=0` | admin | 200 | 200 | PASS |
| `/admin/customers/detail/100?page=1` | admin | 200 | 200 | PASS |
| `/admin/promotions/77/products?page=0` | admin | 200 | 200 | PASS |
| `/admin/promotions/77/products?page=1` | admin | 200 | 200 | PASS |
| `/admin/orders/674` | admin | 200 | 200 | PASS |
| `/admin/orders/detail/674` | admin | 200 | 200 | PASS |
| `/admin/invoices/287` | admin | 200 | 200 | PASS |
| `/admin/statistics/revenue?from=2026-10-01&to=2026-10-28&page=0` | admin | 200 | 200 | PASS |
| `/admin/statistics/revenue?from=2026-10-01&to=2026-10-28&page=1` | admin | 200 | 200 | PASS |
| `/admin/statistics/best-sellers?from=2026-10-01&to=2026-10-28&quantityPage=0&revenuePage=0` | admin | 200 | 200 | PASS |
| `/admin/statistics/best-sellers?from=2026-10-01&to=2026-10-28&quantityPage=1&revenuePage=1` | admin | 200 | 200 | PASS |
| `/admin/pos` | admin | 200 | 200 | PASS |
| `/admin/pos/customers` | admin | 200 | 200 | PASS |
| `/admin/pos/customers?customerPage=1` | admin | 200 | 200 | PASS |
| `/cart` | user | 200 | 200 | PASS |
| `/checkout` | user | 200 | 200 | PASS |
| `/orders` | user | 200 | 200 | PASS |
| `/orders?page=1` | user | 200 | 200 | PASS |
| `/orders/674` | user | 200 | 200 | PASS |
| `/orders/success/674` | user | 200 | 200 | PASS |

Các alias `/admin`, `/admin/`, `/admin/statistics`, `/admin/reports/revenue`, `/admin/reports/best-sellers` cũng đã mở và kiểm tra STT (HTTP 200).

## Bằng chứng SQL và Maven

```text
Tests run: 133, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

[Output đầy đủ](evidence/gd2/clean-test-final.log), [từng lớp test](evidence/gd2/tests-summary.json). Test mới `TablePresentationIntegrationTest` tạo 14 sản phẩm/biến thể trong transaction, kiểm tra trang 2 bắt đầu 13 và cột Mã/Tên/Kích cỡ/Chất liệu riêng; rollback sau test.

Cùng bộ fixture trên bản sao `shopbangiay_kt_20261028_gd2`, ứng dụng trước chạy 18121 và sau chạy 18120, đều chờ Started và dùng ddl-auto=validate. Chạy Maven cuối trên bản sao độc lập `shopbangiay_kt_20261028_gd2_unit` để không làm thay đổi dữ liệu đang so sánh UI. Hai database đều được tạo mới bằng RESTORE có guard không ghi đè database đã tồn tại; chỉ RESTORE WITH REPLACE chính các bản sao do giai đoạn này tạo.

Trong khoảng crawl/đối chiếu, `COUNT_BIG(*)` và `CHECKSUM_AGG(BINARY_CHECKSUM(*))` của cả 21 bảng không đổi: [trước](evidence/gd2/data-before-comparison.json), [sau](evidence/gd2/data-after-comparison.json). Các thao tác kiểm tra checkbox/hóa đơn chờ sau đó chỉ tác động fixture, rồi được loại bằng RESTORE.

```sql
SELECT p.code,v.id,v.price,v.quantity FROM product_variants v JOIN products p ON p.id=v.product_id WHERE p.code LIKE 'GD2-P%' ORDER BY p.code,v.id;
SELECT product_name,size_name,ten_chat_lieu,quantity,price,total_price FROM order_details WHERE order_id=674 ORDER BY id;
SELECT i.invoice_code,o.total_amount,o.final_amount FROM invoices i JOIN orders o ON o.id=i.order_id WHERE i.invoice_code='GD2-I000';
```

[Kết quả SQL thật](evidence/gd2/owned-sql-after-actions.json): 14 dòng của GD2-O000, mỗi dòng quantity=1, price=total_price=123456; tổng hóa đơn **1.728.384** đúng PDF. Dữ liệu này được tạo bằng fixture SQL để kiểm tra trình bày, không dùng làm bằng chứng đã chạy đầy đủ luồng thanh toán. Form giỏ/số lượng/lựa chọn được thao tác bằng trình duyệt thật.

## Khôi phục dữ liệu

| Bảng | Ban đầu | Sau fixture / thao tác | Sau RESTORE |
|---|---:|---:|---:|
| categories | 6 | 14 | 6 |
| chat_lieu | 8 | 14 | 8 |
| customers | 14 | 15 | 14 |
| de_giay | 8 | 14 | 8 |
| dia_chi_nhan_hang | 7 | 9 | 7 |
| employees | 5 | 14 | 5 |
| inventory_logs | 63 | 77 | 63 |
| invoices | 28 | 42 | 28 |
| order_details | 35 | 62 | 35 |
| orders | 28 | 42 | 28 |
| pos_held_items | 0 | 14 | 0 |
| pos_held_orders | 0 | 1 | 0 |
| product_images | 6 | 6 | 6 |
| product_variants | 20 | 47 | 20 |
| products | 10 | 24 | 10 |
| promotion_details | 7 | 8 | 7 |
| promotions | 5 | 14 | 5 |
| roles | 2 | 2 | 2 |
| sizes | 8 | 14 | 8 |
| users | 12 | 22 | 12 |
| vouchers | 15 | 15 | 15 |

[Xác nhận khôi phục](evidence/gd2/cleanup-proof.json): cả hai bản sao khớp toàn bộ COUNT/schema ban đầu; biến thể 596 vẫn tồn 18, đơn 669 vẫn PENDING. Không có upload mới trong giai đoạn này. Các app kiểm thử đã dừng.

## Bảo mật và lỗi phát hiện

| Ca | Kiểm tra | Kết quả HTTP thực |
|---|---|---|
| S01 | Khách vào trang bảo vệ | PASS — [302] |
| S02 | User vào Admin | PASS — [403] |
| S03 | Admin vào trang thuộc quyền | PASS — [200] |
| S04 | POST thiếu CSRF: POS quantity/remove/holds và cart update | PASS — [403] |
| S05 | POST CSRF sai: bốn route trên | PASS — [403] |
| S06 | User POST POS với CSRF hợp lệ | PASS — [403] |
| S07 | User đọc/hủy đơn của người khác | PASS — [403] |
| S08 | User đọc/sửa/xóa/đặt mặc định địa chỉ người khác | PASS — [404] |

| Mã | File:dòng | Nguyên nhân và sửa | Chạy lại |
|---|---|---|---|
| G2-01 | src/main/resources/templates/admin/don-hang/chi-tiet.html:9 | Header đã tách nhưng dòng dữ liệu chưa có ô Mã; ban đầu 7 ô/9 tiêu đề. Sửa còn đúng 8 cột theo dữ liệu thực, thêm ô Mã và giữ Tên riêng | Các URL chi tiết/alias và toàn bộ crawl sau đều khớp số ô/header |
| G2-02 | src/main/resources/static/css/he-thong-thiet-ke.css:166 | Nhãn ẩn position:absolute trong bảng POS dùng containing block ngoài frame, làm root rộng 537px trên 375px. Cho khung cuộn position:relative | Root 360/375 và 1265/1280; phím ArrowRight chỉ cuộn frame |

Lịch sử lần chạy đầu và lần chạy lại giữ trong attempt-history.json. Các lỗi riêng của bộ kiểm tra (bỏ sót cột ảnh khi ánh xạ dữ liệu, đặt tên biến trùng all, nhận diện tiêu đề PDF bị xuống dòng) đã được sửa và chạy lại; không sửa dữ liệu/logic ứng dụng để làm test đạt.

Audit UI strict có 87 phát hiện trước và 87 sau (20 chưa khai báo ownership trong manifest và 67 quy tắc cũ), không phát sinh thêm. Đây không phải kết luận toàn repository sạch audit; giữ phạm vi và không refactor phần cũ. Hai JSON audit đi kèm.

## Ảnh và bản in

[Sản phẩm trang 2 — 375px](evidence/gd2/gd2-products-375-left.png), [cột bên phải](evidence/gd2/gd2-products-375-right.png), [tồn kho](evidence/gd2/gd2-inventory-375-left.png), [giỏ User](evidence/gd2/gd2-cart-375-left.png), [POS sau thao tác](evidence/gd2/gd2-pos-actions-375.png), [top trang 2](evidence/gd2/gd2-top-375-left.png). Có ảnh tương ứng 1280px và các bảng chính khác trong thư mục bằng chứng.

[PDF hóa đơn](evidence/gd2/gd2-invoice-print.pdf), [trang in 1](evidence/gd2/gd2-invoice-print-page-1.png), [trang in 2](evidence/gd2/gd2-invoice-print-page-2.png), [kết quả đọc PDF](evidence/gd2/print-verification.json). Đã kiểm tra hình ảnh hai trang sau render; tiêu đề Kích cỡ xuống hai dòng đúng trong cùng cột. Không gửi lệnh in tới máy in thật.

## Triển khai và phạm vi commit

Giai đoạn 2 không đổi schema, nên **không có migration mới cần chạy**; `chay_tat_ca.sql` và `shopgiay.sql` giữ nguyên. Nếu môi trường chưa nâng cấp giai đoạn 1, làm theo hướng dẫn migration 010 trong báo cáo giai đoạn 1 trước khi khởi động. Không tự chạy gì trên database thật.

Giữ cấu trúc package và mọi route/enum. Các truy vấn chỉ bổ sung code cho cột hiển thị, không đổi WHERE/ORDER BY/giá hay điều kiện doanh thu. JavaScript/Controller phụ thuộc sẵn của POS/Dashboard được giữ đồng bộ với template đang có trong workspace. 115 đường dẫn staged từ trước được bảo toàn, không gom các thao tác dọn dẹp đó vào commit này.

Xác nhận không kết nối `shopbangiay` hoặc `shopbangiay_thu`. Log RESTORE chứa tên logical file lấy từ backup, không phải kết nối tới các database đó. DỪNG sau giai đoạn 2, không merge.
