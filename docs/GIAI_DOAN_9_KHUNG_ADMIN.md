# Giai đoạn 9 — Khung giao diện Admin

Nhánh `gop-y-dot-moi`. Chỉ kết nối bản sao `shopbangiay_kt_20261031_gd9`, RESTORE mới từ full backup đọc được gần nhất (30/10/2026 21:48:04), không ghi đè database có sẵn. App cổng 18309, `ddl-auto=validate`; bằng chứng [khởi động](evidence/gd9/startup.txt), [RESTORE](evidence/gd9/restore-new.txt). Không kết nối `shopbangiay` hoặc `shopbangiay_thu`.

| Hạng mục | Cách kiểm tra | Kết quả thực tế | PASS/FAIL/NOT TESTED |
|---|---|---|---|
| Menu hai cấp, bàn phím, nhớ trạng thái | Playwright 1280/375, đóng/mở, tải lại, mục đang xem | Hai viewport qua; native details/summary, active mở sẵn | PASS |
| Tiêu đề, Dashboard, chữ nhỏ, thương hiệu sidebar | Crawl DOM tất cả trang, kiểm tra breadcrumb ngoài Dashboard và không tràn | Dashboard gồm cả alias `/admin`; trang khác không còn phần giới thiệu nhỏ | PASS |
| Nút Thêm có dấu cộng | rg trước/sau và DOM sau render | Dấu cộng trang trí aria-hidden; giữ nhãn chức năng | PASS |
| Mọi cột hành động dùng fragment icon | Quét mọi bảng trong đồ thị link ở hai viewport | Không còn hành động thiếu SVG/title/aria-label; label ẩn; 36px/40px; ngang một hàng | PASS |
| AJAX, xác nhận, focus, icon không cần mạng | Tìm khách theo mã, thêm giỏ POS/lưu chờ, Hủy hộp xác nhận; chặn tài nguyên ngoài | Qua cả hai viewport; không xóa khi Hủy; SVG vẫn hiển thị | PASS |
| Mã 7 loại, tìm mã, form chỉ đọc, chống giả mạo | SELECT, MockMvc, form thật | TH/KC/CL/MS/DG/KM tối thiểu 3 số, BT tối thiểu 4; server không nhận mã giả | PASS |
| Migration 017 hai lần | COUNT/schema và sequence trước/sau | Không đổi COUNT lần 1/2, sequence không lùi; giá/tồn/trạng thái biến thể và đơn cũ giữ nguyên | PASS |
| 20 tạo đồng thời mỗi loại; rollback không tái sử dụng | Test tích hợp trên SQL Server | 7 × 20 mã riêng biệt; rollback tiêu thụ mã, không tái sử dụng | PASS |
| 8 tình huống bảo mật | MockMvc có Spring Security | 302,403,403,403,403,403,403,302; POST mã giả bị bỏ qua | PASS |
| Crawl toàn bộ link tìm được | BFS tới khi hết hàng đợi, ba vai trò | admin: 489 URL, user: 24 URL, guest: 20 URL; không link nội bộ 404/500 | PASS |
| JS và mobile | Playwright mọi trang HTML 1280/375 | Không lỗi JS console/runtime, không tràn ngang trang | PASS |
| `mvnw clean test` | Maven offline, không skip | 211 test; 0 lỗi, 0 thất bại, 0 bỏ qua | PASS |

Bằng chứng: [Maven/concurrency/bảo mật](evidence/gd9/maven-summary.txt), [URL và status](evidence/gd9/URL_STATUS.md), [DOM từng bảng và cột](evidence/gd9/table-columns-and-actions.json), [Playwright](evidence/gd9/browser-results.json), [rg trước](evidence/gd9/them-before.txt), [rg sau](evidence/gd9/them-after.txt). Lần thử cũ có lỗi fixture chọn kích cỡ trước các thuộc tính bắt buộc và kiểm tra nhầm `/admin` là trang thường; đã sửa kịch bản và chạy lại qua. Bản đồ dùng tile giả lập, không đo dịch vụ bản đồ thật ở giai đoạn giao diện này.

## Bảng kiểm cột Mã

| Trang danh sách | Cột Mã sau STT |
|---|---|
| Sản phẩm | Mã sản phẩm hiện có |
| Biến thể, tồn kho, lịch sử kho | Mã biến thể; có riêng Mã SP và Tên SP |
| Thương hiệu, Kích cỡ, Chất liệu, Màu sắc, Đế giày, Đợt giảm giá | Mã tự sinh bằng sequence 017 |
| Khách hàng, Nhân viên, Phiếu giảm giá | Mã nghiệp vụ hiện có |
| Đơn hàng, Hóa đơn, lịch sử đơn khách, đơn User, dòng hàng/bản in | Mã đối tượng chính hiện có; dòng hàng là mã SP |
| Địa chỉ khách | Mã địa chỉ hiển thị DC + id; không đổi schema địa chỉ |
| Thời gian giao hàng | Mã vùng BAC/TRUNG/NAM |
| POS tìm khách/giỏ, giỏ User, checkout, chi tiết sản phẩm | Mã khách/mã SP hoặc mã biến thể tương ứng |
| Tổng hợp doanh thu theo ngày/trạng thái | Bảng tổng hợp theo ngày/trạng thái, không có đối tượng có mã nghiệp vụ; giữ khóa tổng hợp |

Tên cột từng URL ở bằng chứng DOM, gồm bảng trong trang chi tiết, giỏ và bản in. Bộ icon Bootstrap Icons 1.10.5 được giữ đúng phiên bản đang dùng, đưa CSS/font vào static; MIT, LICENSE đi kèm, [nguồn và SHA256](evidence/gd9/vendor.json). Fragment icon dùng chung `fragments/nut-hanh-dong.html`; JS chỉ trang trí điều khiển hiện có, không đổi phương thức HTTP/CSRF/handler.

## COUNT trước và sau migration

SQL độc lập: `SELECT COUNT_BIG(*) FROM dbo.[tên_bảng]`; schema lấy `sys.tables/sys.columns`; sequence lấy `sys.sequences`; toàn bộ kết quả JSON trước/sau trong thư mục bằng chứng. Không ghi CCCD, mật khẩu, cookie hay token vào báo cáo.

| Bảng | Trước 017 | Lần 1 | Lần 2 |
|---|---:|---:|---:|
| categories | 6 | 6 | 6 |
| cau_hinh_phi_ship | 1 | 1 | 1 |
| chat_lieu | 8 | 8 | 8 |
| customers | 14 | 14 | 14 |
| de_giay | 8 | 8 | 8 |
| dia_chi_nhan_hang | 7 | 7 | 7 |
| employees | 5 | 5 | 5 |
| inventory_logs | 63 | 63 | 63 |
| invoices | 28 | 28 | 28 |
| mau_sac | 11 | 11 | 11 |
| order_details | 35 | 35 | 35 |
| order_status_history | 28 | 28 | 28 |
| orders | 28 | 28 | 28 |
| phuong_xa | 3321 | 3321 | 3321 |
| pos_held_items | 0 | 0 | 0 |
| pos_held_orders | 0 | 0 | 0 |
| product_images | 6 | 6 | 6 |
| product_variants | 20 | 20 | 20 |
| products | 10 | 10 | 10 |
| promotion_details | 7 | 7 | 7 |
| promotions | 5 | 5 | 5 |
| roles | 2 | 2 | 2 |
| sizes | 8 | 8 | 8 |
| ten_tinh_thanh_cu | 68 | 68 | 68 |
| tinh_thanh | 34 | 34 | 34 |
| users | 12 | 12 | 12 |
| vouchers | 15 | 15 | 15 |
| vung_giao_hang | 3 | 3 | 3 |

## Lệnh vận hành sau khi sao lưu database thật

Chỉ người vận hành thực hiện. Chưa chạy trên DB thật. Chạy các migration trước 017 theo thứ tự nếu chưa có; migration 017 bổ sung mã, không đổi luật kho/doanh thu/hủy.

Tạo file vận hành riêng gồm hai dòng:

```sql
EXEC sys.sp_set_session_context @key=N'shop.allow-production-migration',@value=1;
:r database/migrations/017_khung_admin_ma_doi_tuong.sql
```

```powershell
sqlcmd -S <SERVER> -E -d shopbangiay -b -f 65001 -i <FILE_VAN_HANH.sql>
```

Sau đó khởi động với `ddl-auto=validate`. File gộp `chay_tat_ca.sql` và `shopgiay.sql` đã cập nhật. Kiểm tra phục hồi sạch sau nhóm test ở [final-restore.json](evidence/gd9/final-restore.json).
