# Báo cáo kiểm thử và sửa lỗi — 25/10/2026

Nhánh hoan-thien-chat-lieu. Hoàn tất **97 mục: 97 PASS, 0 FAIL, 0 NOT TESTED** trong [ma trận có bước thực hiện](evidence/kiem-thu/MA_TRAN_CHUC_NANG.md) và [manifest](evidence/kiem-thu/cases.json). Số mục đếm chức năng/kịch bản, không đếm lại mỗi viewport hoặc mỗi tổ hợp URL.

## Môi trường

Chỉ dùng bản sao mới **shopbangiay_kt_20261025**, không ghi đè shopbangiay_kt có sẵn. Không có backup shopbangiay_thu; theo trả lời cho phép dùng dữ liệu chính qua bản sao, đã restore file có sẵn shopbangiay_release_20261023.bak. Đây là dữ liệu của file ngày 23/10, không khẳng định là dữ liệu mới hơn trên máy chủ.

App chạy cổng riêng **18100**, ddl-auto=validate, ép datasource vào DB audit và tắt import cấu hình cá nhân trong phiên kiểm thử. Có log **Started DemoDatnApplication** trước khi test. Playwright Chromium: **1280×900 và 375×812**. Sau từng nhóm phá dữ liệu, dừng app và restore checkpoint sạch của chính DB audit.

**Không kết nối hoặc thực thi lệnh trên shopbangiay và shopbangiay_thu.** Master/msdb chỉ dùng metadata backup/RESTORE. App audit đã dừng sau kiểm tra cuối.

## Tổng hợp

| Hạng mục | Cách kiểm tra | Kết quả thực tế | PASS/FAIL/NOT TESTED |
|---|---|---|---|
| Schema/migration/phục hồi — 7 mục | Validate, metadata SQL, runner hai lần | Schema sạch; COUNT/schema lần 1 = lần 2 | PASS |
| Khách — 19 mục | Browser thật, hai viewport | 38/38 lượt chức năng đạt | PASS |
| User — 23 mục | Browser/HTTP và SQL trước/sau | 46/46 lượt chức năng đạt | PASS |
| Admin — 34 mục | CRUD, POS, đơn, tài khoản, thống kê, SQL | Đủ 34 mục, có bằng chứng theo thao tác/viewport | PASS |
| Bảo mật/kho/chất lượng/hiệu năng/cleanup — 14 mục | Ma trận quyền/CSRF, SQL, crawl, đếm truy vấn | Đủ 14 mục | PASS |
| **Tổng manifest** | [cases.json](evidence/kiem-thu/cases.json) | **97 / 0 / 0** | **PASS** |

Không lấy kết quả các giai đoạn trước để đánh PASS cho đợt này. Các lần harness sai locator, cảnh báo rời form, trùng biến id hoặc thiếu tồn fixture được giữ dấu vết và chạy lại; không sửa ứng dụng theo lỗi script.

## Bảng lỗi

Đường dẫn Java dưới đây bắt đầu từ src/main/java/com/example/demodatn; template bắt đầu từ src/main/resources. Dòng theo working tree đã kiểm thử.

| Mã | Mô tả/nguyên nhân gốc | File:dòng | Cách sửa / bằng chứng chạy lại |
|---|---|---|---|
| BUG-01 | Thiếu dia_chi_nhan_hang, gây checkout lỗi theo báo cáo người dùng; validate xác nhận schema thiếu | controller/user/OrderController.java:81; database/migrations/009_dia_chi_nhan_hang.sql:1 | Runner 004–009 và bootstrap đầy đủ; checkout có giỏ trả 200, combo địa chỉ hoạt động |
| BUG-02 | Startup chưa liệt kê đủ bảng/cột bắt buộc còn thiếu | config/SchemaCheck.java:23 | Preflight chỉ đọc trước EntityManagerFactory, log tất cả phần thiếu và migration; schema đủ thì Started bình thường |
| BUG-03 | Chạy lại 008 sau 009 có thể đổi cột SĐT nhận hàng sang NVARCHAR | database/migrations/008_nvarchar_con_lai.sql:21 | Loại đúng cột VARCHAR(20) theo đặc tả; schema/COUNT hai lần giống nhau |
| BUG-04 | Đăng ký bằng mật khẩu 123 vẫn thành công | util/PasswordPolicy.java:6; dto/nhanvien/RegisterRequest.java | Chính sách dùng chung ít nhất 8 ký tự, có chữ/số, tối đa BCrypt 72 byte UTF-8. Đăng ký/đổi mật khẩu User/Admin/tạo nhân viên đều kiểm tra; browser và test entry point đạt |
| BUG-05 | Nhãn Size/variant/voucher/Dashboard/Online/Email và mã quyền/kênh kỹ thuật còn hiển thị | controller/AdminPageAdvice.java:12; controller/admin/sanpham/AdminLookupController.java:43; templates/admin/don-hang/chi-tiet.html:3; các template/thông báo liên quan | Việt hóa phần hiển thị; giữ tên riêng, identifier, route, enum và dữ liệu nghiệp vụ. Crawl 92 lượt + chạy lại 16 trang Admin/thông báo sau POST đạt |
| BUG-06 | N+1 biến thể/khuyến mãi; POS tính cùng giá hai lần | service/sanpham/ProductFilterService.java:55; service/donhang/PricingService.java:42; controller/admin/donhang/AdminPosController.java:72 | Nạp gộp theo danh sách ID, dùng lại bản đồ giá trong một render; tất cả dưới 40 SQL, HTML/giá/thứ tự giữ nguyên |
| BUG-07 | Hướng dẫn POS nói chờ xác nhận trong khi luồng sẵn có ghi PAID ngay | templates/admin/ban-tai-quay/ban-tai-quay.html:32 | Sửa hướng dẫn xác nhận đã nhận đủ tiền trước khi thanh toán; giữ hành vi POS. Xác nhận chuyển khoản riêng được test bằng đơn Online |

Hai quyết định đã nhận: chính sách mật khẩu và giữ tên riêng/thương hiệu. Không còn lỗi chờ quyết định nghiệp vụ.

## Schema

Validate ban đầu dừng ở missing table chat_lieu. Preflight liệt kê: chat_lieu (005), dia_chi_nhan_hang (009), product_images (004), order_details.ten_chat_lieu (005), orders.da_tru_kho (006), product_variants.chat_lieu_id (005).

[SCHEMA_TRUOC_SAU.md](evidence/kiem-thu/schema/SCHEMA_TRUOC_SAU.md) và metadata thô liệt kê 28 mục schema được bổ sung/chuyển kiểu. SchemaCheck liệt kê phần thiếu; Hibernate validate kiểm tra kiểu và đã sạch sau migration.

Runner chay_tat_ca.sql gộp vật lý 004–009, có DB_NAME guard và PRINT từng bước. Chạy hai lần trên bản sao: lần 2 không lỗi, không đổi COUNT/schema. shopgiay.sql có cấu trúc 009. Bằng chứng tại [schema](evidence/kiem-thu/schema). Các log preflight đầu tiên bị charset Windows làm mất dấu thông báo; tên bảng/cột/migration vẫn rõ. Các lần sau dùng UTF-8; Unicode được kiểm tra thêm bằng browser/SQL.

## Chức năng và luật kho

| Hạng mục | Cách kiểm tra | Kết quả thực tế | Trạng thái |
|---|---|---|---|
| Sản phẩm/ảnh/biến thể | Form thật: 2 kích cỡ, 1 chất liệu, upload 3 PNG, đổi ảnh chính; SELECT | Đúng 2 dòng, giá 123456, tồn 7 mỗi dòng, 2 log Khởi tạo; 3 ảnh tải được, ảnh chính đổi đúng ID | PASS |
| Validation/thương hiệu/chất liệu | Thiếu chất liệu, trùng bộ ba; CRUD; ẩn/xóa chất liệu đang dùng | Lỗi cạnh ô; không tạo trùng; combo loại chất liệu ẩn, biến thể/đơn cũ giữ tên; xóa chất liệu dùng bị chặn | PASS |
| Khách/nhân viên | CRUD, vô hiệu hóa, sổ địa chỉ chỉ xem, đăng nhập tài khoản nhân viên, tự khóa | SQL đúng; vô hiệu hóa khách chặn tài khoản liên kết; tự khóa bị chặn | PASS |
| Địa chỉ/checkout | Thêm đủ 3/chặn thứ 4, sửa/xóa/default, chọn combo, sửa tạm | Điền đúng; default chuyển đúng; customers.address khớp; sửa tạm không đổi sổ | PASS |
| Giỏ/phiếu giảm giá | Cộng dồn, số lượng, vượt tồn, xóa/rỗng; hợp lệ và 7 lý do từ chối | Giảm hợp lệ 5000; không tồn tại/khóa/chưa tới/hết hạn/hết lượt/tối thiểu/riêng tư đều bị chặn | PASS |
| COD | SQL từng bước, hai viewport | 20 → 20 đặt → 19 xác nhận → 20 hủy → 20 hủy lại; không lặp log hoàn | PASS |
| Chuyển khoản Online | SQL đặt/hủy, Admin xác nhận thanh toán/hoàn tất | 20 → 19 đặt → 20 hủy; chuyển trạng thái không trừ lần hai; PAID trước hoàn tất | PASS |
| Thiếu hàng | Tồn đổi sau khi mở checkout, rồi submit | Đơn/voucher/log/kho không đổi; rollback | PASS |
| Tranh chấp tồn | Hai request xác nhận khi còn 1 đôi | 1 CONFIRMED, 1 PENDING; đúng 1 log trừ, tồn 0 | PASS |
| Luồng đơn Admin | Hoàn tất, hủy, giao thất bại, đơn 2 chất liệu | Trạng thái/PAID/trừ/hoàn kho và snapshot đúng sau từng bước | PASS |
| POS | Tìm/chọn kích cỡ/chất liệu; khách lẻ/có tài khoản; phiếu; tiền mặt/chuyển khoản | Mỗi thanh toán trừ 1 đôi, giảm 5000; POS đều PAID theo hành vi hiện có | PASS |
| Hóa đơn/in | Xem, kiểm tra gọi window.print, render CSS print trong Chromium | Có chất liệu/số tiền đúng; ảnh chụp bản in hai viewport | PASS |
| Hóa đơn chờ | Lưu/mở/xóa; 5 rồi thử thứ 6; fixture hết hạn | Giới hạn 5; mở hết hạn 404; lưu chờ không trừ kho | PASS |
| Phiếu/đợt giảm giá | Tạo/sửa/xóa, chọn sản phẩm, SELECT | Dữ liệu/lựa chọn giữ đúng; giá khuyến mãi hiển thị | PASS |
| Tài khoản Admin | Hồ sơ, đổi mật khẩu, đăng xuất/đăng nhập bằng fixture | User/employee đồng bộ; mật khẩu cũ bị từ chối, mới đăng nhập được | PASS |
| Unicode | Browser và SELECT | Số 12, đường Nguyễn Huệ, Đà Nẵng giữ nguyên từng ký tự | PASS |

Ledger SQL có số trước/sau và ID thật: [user-security](evidence/kiem-thu/user-security), [admin](evidence/kiem-thu/admin), [final](evidence/kiem-thu/final).

## Doanh thu/bán chạy/Dashboard

Số liệu cùng thời điểm nhóm POS/thống kê, trước restore:

| Kênh | Doanh thu SELECT / trang | Số đơn SELECT / trang | Trung bình SELECT / trang |
|---|---:|---:|---:|
| Tất cả | 26510000 / 26510000 | 14 / 14 | 1893571.43 / 1893571.43 |
| Online | 4650000 / 4650000 | 3 / 3 | 1550000 / 1550000 |
| POS | 21860000 / 21860000 | 11 / 11 | 1987272.73 / 1987272.73 |

Top 10 số lượng/doanh thu ở ba kênh đặt SELECT cạnh số trên trang tại [SO_LIEU_DOI_CHIEU.md](evidence/kiem-thu/admin/SO_LIEU_DOI_CHIEU.md), kèm SQL độc lập đầy đủ. Không dùng tổng từ service làm số kỳ vọng. Chỉ PAID, loại CANCELLED/DELIVERY_FAILED. Sáu chỉ số Dashboard đối chiếu SELECT riêng.

600 tổ hợp URL ở hai viewport đều 200, ngày sai/quá dài báo rõ, không dữ liệu có empty state, không lỗi JS biểu đồ. Bổ sung **4800/4800 PASS** cho page/quantityPage/revenuePage nhận độc lập {0, -1, abc, 99999}, 5 bộ ngày × 3 kênh × 5 URL. [CSV đầy đủ](evidence/kiem-thu/final/statistics-cross-pages.csv). Không phát sinh 500 ở doanh thu/bán chạy trong đợt này; không bịa stacktrace nguyên nhân khác.

## Tám kịch bản bảo mật

| Mã | Kịch bản | Kết quả thực tế | Trạng thái |
|---|---|---|---|
| SEC-01 | Khách vào route cần đăng nhập | 302 tới đăng nhập | PASS |
| SEC-02 | User vào Admin | 403 | PASS |
| SEC-03 | Admin vào trang Admin với ID thật | 200 hoặc redirect alias hợp lệ | PASS |
| SEC-04 | Thiếu CSRF mọi POST mapping và login/logout | 403 | PASS |
| SEC-05 | CSRF sai mọi POST | 403 | PASS |
| SEC-06 | CSRF hợp lệ nhưng khách/User POST Admin | 96 kiểm tra: 302 / 403 | PASS |
| SEC-07 | Đơn/trang thành công của người khác | 403 ở hai viewport | PASS |
| SEC-08 | Sửa/xóa/default địa chỉ, checkout bằng địa chỉ người khác | 404; SQL không đổi | PASS |

Ma trận mọi mapping/quyền/CSRF cuối: **639 PASS, 0 FAIL**, có phương thức và status tại [all-route-security.csv](evidence/kiem-thu/final/all-route-security.csv). Valid-CSRF bổ sung và ownership/SQL tại user-security.

## URL và chất lượng

| URL | Vai trò | Status | Kết quả |
|---|---|---:|---|
| /, /products, /category/{id}, chi tiết, lọc/khuyến mãi, login/register | Khách | 200 | PASS |
| /profile, /cart, /checkout có giỏ, /orders, đơn sở hữu | User | 200 | PASS |
| CRUD, kho, POS, đơn, hóa đơn, thống kê, tài khoản | Admin | 200 | PASS |
| Route bảo vệ / route Admin | Khách / User | 302 / 403 | PASS |
| Đơn / địa chỉ người khác | User | 403 / 404 | PASS |
| POST thiếu/sai CSRF | Các vai trò | 403 | PASS |

Chi tiết 92 lượt crawl: [URL_STATUS.md](evidence/kiem-thu/final/URL_STATUS.md), [CSV](evidence/kiem-thu/final/url-role-status.csv). Thêm 16 trang Admin và thông báo sau POST: final-labels-ui.json. Không pageerror, request 404, trang trắng hoặc tràn ngang 375px trong lượt crawl cuối. Việt hóa nhãn chức năng; giữ tên riêng/thương hiệu và tên dữ liệu người dùng đặt.

## SQL trước/sau

Đếm prepareStatement của JPA và JdbcTemplate qua proxy DataSource, MockMvc render đủ Thymeleaf/Spring Security; có ít nhất 14 sản phẩm fixture bổ sung. Không tính câu đăng nhập vào câu của trang.

| URL | Trước | Sau | Kết quả |
|---|---:|---:|---|
| / | 159 | 14 | PASS |
| /products | 63 | 8 | PASS |
| /products/949 | 22 | 22 | PASS |
| /category/1 | 41 | 9 | PASS |
| /cart có 1 dòng | 3 | 3 | PASS |
| /admin/pos | 50 | 9 | PASS |
| /admin/products-crud | 6 | 6 | PASS |
| /admin/inventory | 6 | 6 | PASS |

HTML cả 8 trang giống từng ký tự sau khi thay riêng token CSRF và checkoutToken ngẫu nhiên bằng placeholder; sản phẩm/giá/thứ tự không đổi. Không cache qua request. Có regression test giá đơn/batch và cập nhật khi đợt giảm giá bị tắt. Mặc định show-sql=false; profile debug bật true. [Bằng chứng](evidence/kiem-thu/performance).

## Clean test và commit

| Nhóm | Run | Failures | Errors | Skipped | Kết quả |
|---|---:|---:|---:|---:|---|
| Schema — 2229c5b | 123 | 0 | 0 | 0 | BUILD SUCCESS |
| Mật khẩu/nhãn ban đầu — 761cd4e | 127 | 0 | 0 | 0 | BUILD SUCCESS |
| N+1 — 06ae1b0 | 128 | 0 | 0 | 0 | BUILD SUCCESS |
| Working tree cuối | 128 | 0 | 0 | 0 | BUILD SUCCESS |
| Đúng nội dung commit Việt hóa cuối | 128 | 0 | 0 | 0 | BUILD SUCCESS |

Các lần cuối chạy **mvnw clean test, không skip**. Log final-clean-test-working-tree-v2.log và final-clean-test-exact-commit-v2.log tại final. Một lần sau Việt hóa có 2 assertion còn đòi chữ variant/size cũ; đã cập nhật kỳ vọng và chạy lại toàn bộ. Nội dung commit xuất từ HEAD qua index riêng để không gom thay đổi sẵn của người dùng; test riêng cũng 128/128. Commit nhóm cuối là commit chứa báo cáo này. Không merge/push.

## Cleanup

[cleanup.json](evidence/kiem-thu/final/cleanup.json), [after-final-cross-pages.json](evidence/kiem-thu/final/after-final-cross-pages.json), log RESTORE chứng minh dữ liệu sau cùng bằng checkpoint:

| Bảng | COUNT checkpoint / sau cùng |
|---|---|
| categories, chat_lieu, sizes | 6=6, 8=8, 8=8 |
| customers, dia_chi_nhan_hang, employees, users | 14=14, 7=7, 5=5, 12=12 |
| products, product_variants, product_images | 10=10, 20=20, 6=6 |
| orders, order_details, invoices, inventory_logs | 28=28, 35=35, 28=28, 63=63 |
| vouchers, promotions, promotion_details | 15=15, 5=5, 7=7 |
| pos_held_orders, pos_held_items, roles | 0=0, 0=0, 2=2 |

Toàn bộ 20 bảng có COUNT/schema bằng checkpoint. Mã QA/tài khoản thử không còn. Đơn gốc 669 vẫn PENDING, biến thể 596 tồn 18. Xóa đúng **9 ảnh audit**, từng đường dẫn được kiểm tra không có DB tham chiếu; giữ ảnh gốc. Bản sao audit sạch, app 18100 đã dừng.

## Migration triển khai thủ công

Thứ tự **004_anh_san_pham.sql → 005_chat_lieu.sql → 006_luat_ton_kho.sql → 007_tieng_viet_unicode.sql → 008_nvarchar_con_lai.sql → 009_dia_chi_nhan_hang.sql**. Có thể dùng runner chay_tat_ca.sql.

Lệnh dành cho người triển khai trên DB thật, từ thư mục project, thay server và cách xác thực phù hợp. Đây chỉ là hướng dẫn, **chưa được thực thi**. Context mở guard và runner phải cùng một kết nối:

~~~powershell
@'
EXEC sys.sp_set_session_context @key=N'shop.allow-production-migration', @value=1;
GO
:r database/migrations/chay_tat_ca.sql
'@ | sqlcmd -S <SQL_SERVER> -E -C -I -b -f 65001 -d shopbangiay
~~~

Sau migration dùng ddl-auto=validate và kiểm tra log Started. **Đợt audit không chạy gì trên shopbangiay hoặc shopbangiay_thu.**
