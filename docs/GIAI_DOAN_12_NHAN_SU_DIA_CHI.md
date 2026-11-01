# Giai đoạn 12 — Nhân sự và địa chỉ

Nhánh gop-y-dot-moi. RESTORE mới shopbangiay_kt_20261031_gd12; app cổng 18312 có Started, ddl-auto=validate sạch. Không kết nối shopbangiay hoặc shopbangiay_thu. Sau kiểm thử đã dừng app và RESTORE checkpoint riêng: COUNT/schema/giá/tồn/đơn bằng baseline sau 019, dữ liệu thử không còn.

| Hạng mục | Cách kiểm tra | Kết quả thực tế | PASS/FAIL/NOT TESTED |
|---|---|---|---|
| Hai chức vụ | Form thật 1280/375, integration tạo hai vai trò, giả mạo chức vụ | Chỉ Admin/Nhân viên, không creatable; ROLE_ADMIN/ROLE_EMPLOYEE đúng; giá trị khác bị từ chối | PASS |
| Ma trận quyền dùng chung | Guest/User/Employee/Admin; menu và URL trực tiếp | Nhân viên vào POS, hóa đơn, khách, tồn kho chỉ xem, hồ sơ; sản phẩm/nhân viên/khuyến mãi/thống kê/Dashboard/hệ thống 403; Admin truy cập được | PASS |
| Quyền tài khoản khách | Nhân viên tạo/sửa khách, gọi API tên đăng nhập; thử tài khoản đặc quyền | Helper 200; không sửa/gửi lại tài khoản quản trị; CCCD che, sửa để trống giữ dữ liệu gốc | PASS |
| Nhân viên một địa chỉ | Form thật, SELECT | Một ô địa chỉ, không sổ địa chỉ nhiều | PASS |
| Sổ khách tối đa ba | Form thật/API, SQL từng bước, direct SQL slot 4 | Số dòng 1→2→3→3; lần bốn báo lỗi; CHECK DB chặn chèn slot 4; không sửa địa chỉ khách khác | PASS |
| Mặc định và Unicode | Đặt mặc định số 2 rồi xóa, SELECT customers/address book | customers.address đồng bộ; xóa mặc định chuyển lại địa chỉ đầu; Số 12, đường Nguyễn Huệ, Đà Nẵng đọc đúng | PASS |
| Tài khoản và email | GreenMail integration, thư nhận thật trong mail giả | Hai vai trò đúng, BCrypt, mật khẩu tạm mạnh, ép đổi lần đầu, đổi xong vào đúng khu vực; gửi lại và SMTP lỗi có test | PASS |
| Hóa đơn cho nhân viên | Đặt COD thật, xử lý qua hóa đơn, SELECT trước/sau | Đặt không trừ, xác nhận trừ một lần, bấm lại không trừ; đi hết DELIVERED và lịch sử, in được | PASS |
| Tám kịch bản bảo mật | Browser POST thật và MockMvc | Guest chuyển login, User 403; quyền Nhân viên hạn chế; thiếu/sai CSRF 403; địa chỉ khác 404; tự hạ quyền/tài khoản quản trị bị chặn | PASS |
| Crawl hết link nội bộ | BFS bốn vai trò | Admin 510, Employee 185, User 23, Guest 20 URL; hết hàng đợi, không link chết/tài nguyên nội bộ 404 | PASS |
| Playwright 1280/375 | Trang chính, form, icon, CSRF, console, DOM | 36 trường hợp cuối PASS; không lỗi JS, không tràn trang; icon 36/40px có title/aria-label; mọi form POST có token | PASS |
| Migration 019 hai lần | COUNT/schema/variant/order, OUTPUT thay đổi | Tạo một role: roles 2→3; các bảng khác giữ COUNT; lần hai không đổi snapshot; năm dòng nhân viên được liệt kê | PASS |
| mvnw clean test | Maven clean test không skip | 229 test, 0 failure/error/skipped | PASS |
| Gửi mail hộp thư thật | Cần cấu hình SMTP thật | Chỉ dùng GreenMail và SMTP tắt trong kiểm thử; chưa gửi tới hộp thư thật | NOT TESTED |

## Bằng chứng thô

[Maven](evidence/gd12/maven-summary.txt), [Started](evidence/gd12/startup.txt), [RESTORE mới](evidence/gd12/restore-new.txt), [RESTORE cuối](evidence/gd12/final-restore.json), [URL/status](evidence/gd12/URL_STATUS.md), [Playwright](evidence/gd12/browser-results.json), [SQL địa chỉ từng bước](evidence/gd12/final-address-sql-steps.json), [SQL hóa đơn/kho](evidence/gd12/invoice-stock-sql.json), [thư Nhân viên](evidence/gd12/mail-employee.txt), [thư Admin](evidence/gd12/mail-admin.txt), [ma trận và từng dòng migration](PHAN_QUYEN.md).

SELECT độc lập: SELECT id,position,user_id,address FROM employees; SELECT id,customer_id,thu_tu,la_mac_dinh,dia_chi FROM dia_chi_nhan_hang ORDER BY customer_id,thu_tu; SELECT id,address FROM customers WHERE id=<ID>; SELECT id,quantity FROM product_variants WHERE id=<ID>; SELECT order_id,status FROM order_status_history WHERE order_id=<ID> ORDER BY id. Kết quả và ID thực nằm trong các JSON; không công bố cookie, token, mật khẩu tạm hay CCCD đầy đủ. Thư GreenMail chỉ che mật khẩu tạm. Đã quét app.log/app-error.log không có CCCD thử.

Ảnh: [sổ địa chỉ 1280](evidence/gd12/address-book-1280.png), [375](evidence/gd12/address-book-375.png), [menu Nhân viên](evidence/gd12/employee-menu-375.png), [form nhân viên](evidence/gd12/employee-form-375.png), [hóa đơn](evidence/gd12/invoice-workflow-375.png), [bản in](evidence/gd12/invoice-print.png).

Các lỗi phát hiện đã sửa và chạy lại: link mã đơn ở danh sách hóa đơn/POS và lịch sử khách trước đây vào trang đơn bị cấm với Nhân viên, nay vào hóa đơn; URI customerId tránh tự bind nhầm DTO.id của địa chỉ; bảo toàn CCCD khi Nhân viên để trống ô sửa; quyền API gợi ý tên đăng nhập giới hạn đúng một endpoint. Trạng thái đơn dùng lại OrderStatusService và ba fragment chung, không nhân đôi luật kho. Audit tĩnh toàn repo vẫn có cảnh báo lịch sử (xem premium-audit-diff.json), không tuyên bố audit toàn repo sạch.

## COUNT trước/sau 019

| Bảng | Trước | Lần 1 | Lần 2 |
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
| roles | 2 | 3 | 3 |
| sizes | 8 | 8 | 8 |
| ten_tinh_thanh_cu | 68 | 68 | 68 |
| tinh_thanh | 34 | 34 | 34 |
| users | 12 | 12 | 12 |
| vouchers | 15 | 15 | 15 |
| vung_giao_hang | 3 | 3 | 3 |

## Lệnh triển khai sau sao lưu — chưa chạy trên DB thật

Chạy các migration còn thiếu theo thứ tự tới 018 trước. Tạo file vận hành riêng:

```sql
EXEC sys.sp_set_session_context @key=N'shop.allow-production-migration',@value=1;
:r database/migrations/019_chuc_vu_phan_quyen.sql
```

```powershell
sqlcmd -S <SERVER> -E -d shopbangiay -b -f 65001 -i <FILE_VAN_HANH.sql>
```

chay_tat_ca.sql và shopgiay.sql đã cập nhật. Khởi động ddl-auto=validate. Muốn thử mail thật: cấu hình MAIL_HOST/PORT/USERNAME/PASSWORD/FROM bằng biến môi trường, tạo khách/nhân viên bằng email do bạn sở hữu, kiểm tra thư, đăng nhập mật khẩu tạm và đổi ngay. Không lưu bí mật vào repo. Không push/merge.
