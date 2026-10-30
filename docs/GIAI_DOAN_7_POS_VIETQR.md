# Giai đoạn 7 — Hóa đơn, tìm khách POS, VietQR

30/10/2026, nhánh gop-y-giang-vien. Bản sao mới `shopbangiay_kt_20261030_gd7` RESTORE từ backup gần nhất `shopbangiay_kt_20261030_gd6_schema.bak` (12:27:46), không ghi đè DB có sẵn. App18190 chờ Started, ddl-auto=validate sạch. Không kết nối shopbangiay hoặc shopbangiay_thu. Bản sao đã được khôi phục sau nhóm browser và clean test, app đã dừng.

| Hạng mục | Cách kiểm tra | Kết quả thực tế | PASS/FAIL/NOT TESTED |
|---|---|---|---|
| Migration014 | Chạy014 hai lần; aggregate004–014 hai lần | Snapshot schema/COUNT/giá/tồn/đơn không đổi khi chạy lặp | PASS |
| Hóa đơn | Browser, integration và SELECT | Cột Kênh bán riêng cạnh Đơn hàng; chi tiết/in có Tại quầy hoặc Trực tuyến | PASS |
| POS mở trang | DOM và request log ở1280/375 | Không request tìm khách; không tên khách trong DOM; mặc định Khách lẻ | PASS |
| Tìm khách | Gõ1ký tự, gõ nhanh, phân trang, chọn/đổi/bỏ | 1ký tự không gọi; debounce305/306ms;12dòng, trang2STT13; empty state đúng | PASS |
| Hủy truy vấn | Trì hoãn truy vấn cũ, gõ truy vấn mới | Request cũ bị hủy; kết quả mới được giữ | PASS |
| Payload/CRC | Unit test, TLV độc lập và jsQR đọc PNG tải lên | BIN970436, tài khoản giả123456789,704,VN,QRIBFTTA; CRC hợp lệ | PASS |
| Tổng POS | Đổi số lượng, áp/bỏ voucher ở1280/375 | 10.000.000→20.000.000→19.950.000→20.000.000; không sai số | PASS |
| Xác nhận nhận tiền | Browser và POST cố tình giả tổng | Chưa tích thì khóa; đổi tổng xóa tích; server từ chối tổng giả; PAID và ref khớp SELECT | PASS |
| QR Online | Mua thật qua form, user/admin, PNGdecode | 125.000+30.000 phí=155.000; nội dung mã đơn; hạn chuyển khoản còn hiển thị | PASS |
| Ẩn QR | Xác nhận chuyển khoản/hủy rồi GET lại | Không còn canvas cho đơn đã trả/hủy | PASS |
| Cấu hình thiếu | App chạy không có4biến ngân hàng | Hiện Chưa cấu hình tài khoản nhận tiền, không QR; không500 | PASS |
| Bảo mật8nhóm | MockMvc và HTTP | KháchGET302, UserGET403, AdminGET200; kháchPOST302; UserPOST403; CSRFthiếu/sai403; AdminPOST hợp lệ200 | PASS |
| Build/test | mvnw clean test khôngskip |181test,0fail,0error,0skip,BUILD SUCCESS | PASS |
| Crawl/JS/mobile | 44lượtURL,1280/375 | Không JS runtime/console error,404,trang trắng hoặc tràn ngang ở các ca cuối | PASS |
| Khôi phục | RESTORE checkpoint rồi so toàn bộ snapshot | COUNT/schema/variants/orders khớp trước kiểm thử | PASS |
| Ứng dụng ngân hàng thật | Chưa có giao dịch ngân hàng thật | Chỉ dùng cấu hình và dữ liệu giả trên bản sao | NOT TESTED |

## Bằng chứng và lỗi đã sửa

[Raw browser cuối](evidence/giai-doan-7/browser-final.json), [mọi lần chạy gồm lỗi bộ định vị test](evidence/giai-doan-7/browser-attempts.json), [POS/QR/SELECT](evidence/giai-doan-7/pos-proof.json), [Online/QR/SELECT](evidence/giai-doan-7/online-proof.json), [SQL kho/đơn cuối](evidence/giai-doan-7/sql-final.json), [clean test](evidence/giai-doan-7/maven-clean-test.txt), [Started](evidence/giai-doan-7/app-final.txt).

| Mã | Nguyên nhân/file | Cách sửa và chạy lại |
|---|---|---|
| GD7-01 | quan-tri-danh-muc.js: input tìm khách nằm trong DOMform thanh toán nhưng có form owner riêng, guard nhầm dirty; linkAJAX nổi lên document | Kiểm tra event.target.form; bỏ qua event đãpreventDefault; xử lý pagination ở khung khách. Browser chọn trang2/chọn khách PASS cả2viewport |
| GD7-02 | POS cũ nạp danh sách khách ngay GET | Empty page ban đầu, endpoint chỉ tìm từ2ký tự, debounce/abort/card; network vàDOM PASS |
| GD7-03 | Thông tin ngân hàng success cũ ghép SĐT vào nội dung chuyển tiền | Dùng cùng payload/fragment server cho user/admin; jsQR vàSELECT khớp mã đơn |

Test lần đầu lỗi fixturecustomer_code quá20ký tự, đã rút gọn fixture và clean test181PASS. Hai lần browser lỗi selector trùng nút và kiểm tra chữ Hạn trong khi UI ghi Vui lòng chuyển khoản trước; sửa bộ định vị, giữ raw attempts, không tính thành lỗi ứng dụng.

CRC tính độc lập: `binascii.crc_hqx(payload_tru_4_ky_tu_cuoi, 0xffff)` = `3E70` với payload ví dụ trong VietQrServiceTest; vector123456789=29B1. Hai ảnh PNG lấy trực tiếp canvas được tải qua inputfile vào browser và jsQR đọc lại, so từng trường/CRC. Không gọi dịch vụ tạo QR bên ngoài. Thư viện/giấy phép: [THU_VIEN_QR](THU_VIEN_QR.md).

SQL sử dụng:
```sql
SELECT id,order_code,order_type,payment_reference,final_amount,payment_status,status FROM orders WHERE id IN (674,675,677,678);
SELECT id,quantity FROM product_variants WHERE product_id=949;
-- POS674: F6SMATL7O /19950000/PAID; POS675: F6SLTOHO6 /19950000/PAID.
-- Online677:155000, phí30000; nội dungDH000677; Online678 hủy, QRẩn.
```

## COUNT trước/sau và khôi phục

| Bảng | Trước014 | Sau014lần2 | SauRESTOREcuối |
|---|---:|---:|---:|
| categories | 6 | 6 | 6 |
| chat_lieu | 8 | 8 | 8 |
| customers | 14 | 14 | 14 |
| de_giay | 8 | 8 | 8 |
| dia_chi_nhan_hang | 7 | 7 | 7 |
| employees | 5 | 5 | 5 |
| inventory_logs | 63 | 63 | 63 |
| invoices | 28 | 28 | 28 |
| mau_sac | 11 | 11 | 11 |
| order_details | 35 | 35 | 35 |
| orders | 28 | 28 | 28 |
| pos_held_items | 0 | 0 | 0 |
| pos_held_orders | 0 | 0 | 0 |
| product_images | 6 | 6 | 6 |
| product_variants | 20 | 20 | 20 |
| products | 10 | 10 | 10 |
| promotion_details | 7 | 7 | 7 |
| promotions | 5 | 5 | 5 |
| roles | 2 | 2 | 2 |
| sizes | 8 | 8 | 8 |
| users | 12 | 12 | 12 |
| vouchers | 15 | 15 | 15 |

[Snapshot trước](evidence/giai-doan-7/before-014.json), [sau hai lần](evidence/giai-doan-7/after-014-second.json), [RESTOREcuối](evidence/giai-doan-7/final-restore.json), [aggregate lần1](evidence/giai-doan-7/aggregate-first.txt), [lần2](evidence/giai-doan-7/aggregate-second.txt). Hai nguồn stock20: variant lớn16 sau2POS×2; variant nhỏ18 vì có1đơn từ lần test selector hạn thất bại,1đơn đã thanh toán và1đơn hủy hoàn lại. Toàn bộ dữ liệu này đã được RESTORE bỏ khỏi bản sao.

## URL

| URL | Vai trò | Viewport | Status |
|---|---|---:|---:|
| / | guest | 1280 | 200 |
| /products | guest | 1280 | 200 |
| /products/949 | guest | 1280 | 200 |
| /cart | guest | 1280 | 200 |
| /login | guest | 1280 | 200 |
| / | guest | 375 | 200 |
| /products | guest | 375 | 200 |
| /products/949 | guest | 375 | 200 |
| /cart | guest | 375 | 200 |
| /login | guest | 375 | 200 |
| /profile | user | 1280 | 200 |
| /orders | user | 1280 | 200 |
| /cart | user | 1280 | 200 |
| /profile | user | 375 | 200 |
| /orders | user | 375 | 200 |
| /cart | user | 375 | 200 |
| /admin | admin | 1280 | 200 |
| /admin/products-crud | admin | 1280 | 200 |
| /admin/products-crud/view/949 | admin | 1280 | 200 |
| /admin/variants | admin | 1280 | 200 |
| /admin/colors | admin | 1280 | 200 |
| /admin/materials | admin | 1280 | 200 |
| /admin/soles | admin | 1280 | 200 |
| /admin/inventory | admin | 1280 | 200 |
| /admin/pos | admin | 1280 | 200 |
| /admin/customers | admin | 1280 | 200 |
| /admin/employees | admin | 1280 | 200 |
| /admin/invoices | admin | 1280 | 200 |
| /admin/statistics/revenue | admin | 1280 | 200 |
| /admin/statistics/best-sellers | admin | 1280 | 200 |
| /admin | admin | 375 | 200 |
| /admin/products-crud | admin | 375 | 200 |
| /admin/products-crud/view/949 | admin | 375 | 200 |
| /admin/variants | admin | 375 | 200 |
| /admin/colors | admin | 375 | 200 |
| /admin/materials | admin | 375 | 200 |
| /admin/soles | admin | 375 | 200 |
| /admin/inventory | admin | 375 | 200 |
| /admin/pos | admin | 375 | 200 |
| /admin/customers | admin | 375 | 200 |
| /admin/employees | admin | 375 | 200 |
| /admin/invoices | admin | 375 | 200 |
| /admin/statistics/revenue | admin | 375 | 200 |
| /admin/statistics/best-sellers | admin | 375 | 200 |

## Triển khai thủ công, chưa chạy trên DB thật

Sao lưu và dừng app trước. Đã có004–013: chạy014; chưa đủ: aggregate004–014. Rootshopgiay.sql đã đồng bộ, không chạy bootstrap lên DB có dữ liệu.

```powershell
@"
EXEC sys.sp_set_session_context @key=N'shop.allow-production-migration', @value=1;
GO
:r database/migrations/014_tham_chieu_thanh_toan.sql
"@ | sqlcmd -S <MAY_CHU_SQL> -d <DATABASE_TRIEN_KHAI> -E -C -I -b -f 65001
```

Thử ngân hàng thật: cấu hình tài khoản của bạn; tạo đơn giá trị nhỏ; quét và đối chiếu tài khoản/số tiền/nội dung trước khi chuyển; kiểm tra giao dịch thực tế rồi mới tích nhận tiền. Mục này NOT TESTED. Không đổi luật kho/doanh thu/hủy đơn; không merge/push.
