# Sửa lỗi khởi động thiếu sizes.trang_thai

Nhánh `gop-y-sua-lan-2`. Log người dùng dừng tại `SchemaCheck.java:39`: `Thiếu cột dbo.sizes.trang_thai`. Đây là lỗi thiếu migration trên database đang dùng. Entity Size cần cột này; giữ nguyên mapping và ddl-auto=validate.

Đã sửa: SchemaCheck chỉ đúng `022_kich_co_trang_thai.sql` và hướng dẫn áp dụng script được liệt kê; migration 022 có DB_NAME guard, transaction/rollback và giá trị mặc định 1; chay_tat_ca.sql và shopgiay.sql đồng bộ. Không thay luật kho/đơn/doanh thu hoặc giao diện.

| Hạng mục | Cách kiểm tra | Kết quả thực tế | PASS/FAIL/NOT TESTED |
|---|---|---|---|
| Nguyên nhân từ log | Đọc stacktrace người dùng | Thiếu sizes.trang_thai; SchemaCheck dừng trước Hibernate, không phải lỗi biên dịch | PASS |
| Tái hiện trên bản sao | RESTORE mới rồi bỏ riêng cột trên bản sao, khởi động 18422 | Không có Started; log báo đúng thiếu cột và migration 022 | PASS |
| Migration 022 hai lần | sqlcmd -b; metadata và COUNT trước/sau | Cột BIT NOT NULL, tám kích cỡ cũ nhận 1; lần hai schema/COUNT không đổi | PASS |
| Bảo toàn dữ liệu | So variant/giá/tồn/đơn trước/sau | Snapshot biến thể và đơn bằng nhau; không thay dòng dữ liệu ngoài cột trạng thái mới | PASS |
| Không bật lại kích cỡ đã ẩn | Đặt một kích cỡ 0 trên bản sao rồi chạy lại 022 | Vẫn 0 | PASS |
| Khởi động sau sửa | Bản build cuối, app 18422, ddl-auto=validate | Tomcat started; Started DemoDatnApplication; không còn SCHEMA_CHECK ERROR | PASS |
| Test hồi quy | mvnw clean test không skip | 240 test, 0 thất bại, 0 lỗi, 0 bỏ qua | PASS |
| URL User công khai/Admin chính | Playwright, 16 URL ở 1280 và 375 | 32 lượt kiểm tra đạt status cuối 200, không trang trắng/JS lỗi/tài nguyên 404/tràn ngang | PASS |
| Tám tình huống CSRF | Thiếu/sai token trên bốn POST thật khi đã đăng nhập Admin | Cả tám HTTP 403 | PASS |
| Phục hồi sau thử nghiệm | Dừng app riêng, RESTORE checkpoint và so snapshot | COUNT/schema/biến thể/đơn/kích cỡ bằng bản sao ban đầu | PASS |
| Database đang được IDE của người dùng sử dụng | Không kết nối DB thật | Chưa áp dụng migration ở đó; cần người vận hành chạy lệnh bên dưới | NOT TESTED |
| Đăng nhập User thật/crawl mọi link nội bộ | Ngoài phép thử tập trung lỗi khởi động | Đã kiểm tra User công khai và Admin chính; không tuyên bố đã crawl toàn bộ hoặc kiểm tra lại mọi chức năng | NOT TESTED |

## Bằng chứng thô

[Log người dùng](evidence/fix_sizes_20261102/user-log-excerpt.txt), [RESTORE mới](evidence/fix_sizes_20261102/restore-new.txt), [tái hiện lỗi](evidence/fix_sizes_20261102/startup-missing.txt), [022 lần 1](evidence/fix_sizes_20261102/022-first.txt), [lần 2](evidence/fix_sizes_20261102/022-second.txt), [SQL trước/sau](evidence/fix_sizes_20261102/migration-proof.json), [trạng thái ẩn giữ nguyên](evidence/fix_sizes_20261102/preserve-hidden.json), [Started](evidence/fix_sizes_20261102/startup-after.txt), [Maven](evidence/fix_sizes_20261102/maven-summary.txt), [Playwright/CSRF](evidence/fix_sizes_20261102/browser-results.json), [URL/status](evidence/fix_sizes_20261102/URL_STATUS.md), [phục hồi cuối](evidence/fix_sizes_20261102/final-restore.json).

Ảnh kích cỡ: [1280px](evidence/fix_sizes_20261102/admin-sizes-1280.png), [375px](evidence/fix_sizes_20261102/admin-sizes-375.png).

Bản sao mới `shopbangiay_kt_20261102_fix_sizes`, không ghi đè database có sẵn. Backup gần nhất đã có cột, nên tái hiện bằng cách bỏ cột chỉ trong bản sao riêng có checkpoint trước đó. Không kết nối `shopbangiay` hoặc `shopbangiay_thu`. Các tên logical file trong RESTORE không phải database nguồn được kết nối.

Một lượt chạy lại test trên dữ liệu chưa phục hồi gặp trùng CCCD fixture của AccountProvisioningIntegrationTest; đã RESTORE checkpoint trước lượt cuối và đạt 240/240. Không sửa logic tài khoản để bỏ qua kiểm tra trùng.

SELECT đối chiếu: `SELECT id,ma,size_value,trang_thai FROM dbo.sizes ORDER BY id`; metadata `sys.columns` cho bảng sizes; `COUNT_BIG(*)` từng bảng. JSON giữ COUNT toàn bộ và kết quả từng kích cỡ; variant/order snapshot được so bằng nhau trong script kiểm chứng.

## COUNT trước/sau migration trên bản sao thiếu cột

| Bảng | Trước | Lần 1 | Lần 2 |
|---|---:|---:|---:|
| categories | 6 | 6 | 6 |
| cau_hinh_phi_ship | 1 | 1 | 1 |
| chat_lieu | 8 | 8 | 8 |
| customers | 14 | 14 | 14 |
| danh_gia | 0 | 0 | 0 |
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
| roles | 3 | 3 | 3 |
| sizes | 8 | 8 | 8 |
| ten_tinh_thanh_cu | 68 | 68 | 68 |
| tinh_thanh | 34 | 34 | 34 |
| users | 12 | 12 | 12 |
| vouchers | 15 | 15 | 15 |
| vung_giao_hang | 3 | 3 | 3 |
| yeu_thich | 0 | 0 | 0 |

## Lệnh cho database đang dùng — thao tác thủ công sau sao lưu

Từ thư mục gốc project, tạo file vận hành riêng:

```sql
EXEC sys.sp_set_session_context @key=N'shop.allow-production-migration', @value=1;
:r database/migrations/022_kich_co_trang_thai.sql
```

```powershell
sqlcmd -S <SERVER> -E -d <TEN_DB_DANG_DUNG> -b -f 65001 -i <FILE_VAN_HANH.sql>
```

Guard vẫn từ chối các tên DB không được cho phép. Cờ session dùng cho DB sản xuất đúng tên trong guard. Sau khi script báo HOÀN TẤT, kiểm tra SELECT kích cỡ rồi mở lại app với validate. Chưa chạy lệnh trên DB thật. Không push/merge.
