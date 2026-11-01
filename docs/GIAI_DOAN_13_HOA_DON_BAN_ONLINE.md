# Giai đoạn 13 — Hóa đơn và Bán online

Nhánh gop-y-dot-moi. RESTORE mới shopbangiay_kt_20261101_gd13 từ full backup mới nhất đọc được; app cổng 18313 có Started và ddl-auto=validate sạch. Không kết nối shopbangiay hoặc shopbangiay_thu. Các nhóm kiểm thử phá dữ liệu kết thúc bằng RESTORE checkpoint riêng. Phục hồi cuối bằng baseline sau 020, không còn tài khoản/địa chỉ/hóa đơn thử.

| Hạng mục | Cách kiểm tra | Kết quả thực tế | PASS/FAIL/NOT TESTED |
|---|---|---|---|
| Một hóa đơn mỗi đơn | SELECT GROUP BY đơn, trước migration | 28 đơn / 28 hóa đơn, không có đơn thiếu hoặc nhiều hóa đơn; dữ liệu gốc không cần bù | PASS |
| Bù thiếu idempotent | Xóa đúng một hóa đơn của đơn thử rồi chạy 020 hai lần | Đơn thử 676 được bù HD-BU-676, lần hai cùng ID 294, không thêm dòng | PASS |
| Migration 020 | COUNT/schema/giá/tồn/đơn hai lần | Thêm hai cột hóa đơn chờ; CHECK phương thức theo loại được bật và trusted; COUNT/giá/tồn/đơn không đổi, lần hai không đổi schema | PASS |
| URL quản trị đơn cũ | Browser GET, integration POST, rg src/main | GET 302 giữ query / sang hóa đơn đúng; POST 307 giữ body, không tự đổi kho; chỉ controller tương thích còn literal /admin/orders | PASS |
| Danh sách hóa đơn hợp nhất | DOM, bộ lọc nhiều điều kiện, SELECT độc lập | 12 cột tách riêng, STT, icon Xem/xử lý và In; kênh/nguồn/trạng thái/thanh toán/phương thức/khách/mã/ngày đúng; page -1/abc/99999 không 500 | PASS |
| Chi tiết hóa đơn | Playwright cả hai viewport, bản in, SQL | Có người nhận/địa chỉ/các thuộc tính dòng hàng/tổng/timeline/kho/ngày dự kiến; xử lý bằng OrderStatusService dùng chung; in được | PASS |
| POS chỉ tại quầy | DOM 1280/375 và integration cùng session | Không có công tắc bán online hoặc form/khối giao hàng; giỏ quầy không bị thay đổi bởi giỏ online | PASS |
| Bán online riêng | Form thật 1280/375, guest không tạo user | Tạo đơn Facebook/Zalo cho khách vãng lai, giữ tiếng Việt; tính ship qua service cũ; tạo xong sang chi tiết hóa đơn | PASS |
| Tìm khách/sổ địa chỉ | Browser chọn khách, chọn địa chỉ, lưu/mở chờ | Không request tìm khách hoặc tên khách trong DOM lúc mở; gõ SĐT chọn khách được; mở lại hiện sổ và giữ snapshot | PASS |
| Sửa tạm địa chỉ | Playwright thay người nhận/địa chỉ, lưu/mở chờ, SELECT sổ | Địa chỉ tạm và người nhận thay thế giữ nguyên; sổ địa chỉ trước/sau bằng nhau, không bị tải sổ ghi đè | PASS |
| Xác nhận ngay | Integration tạo đơn confirmNow=true, SQL | CONFIRMED, trừ đúng một đơn vị và hai dòng lịch sử; checkbox có trên form | PASS |
| Tin nhắn xác nhận | Click thật, kiểm tra nội dung và phản hồi | Đủ hàng/thuộc tính/tổng/ship/địa chỉ; Clipboard API có phản hồi, lỗi có hướng dẫn chọn văn bản và Ctrl+C | PASS |
| QR chuyển khoản | Tạo BANK_TRANSFER thật, giải mã canvas bằng jsQR | Payload giải mã bằng payload server; QR hiện sau tạo đơn với tổng đơn và mã đơn, ẩn khi hủy; không tự xác nhận thu tiền | PASS |
| Luật kho COD | SQL trước/đặt/xác nhận/giao/hủy/thất bại | Đặt không trừ, xác nhận trừ; giao giữ mức đã trừ; hủy và giao thất bại hoàn lại đúng tồn ban đầu | PASS |
| Luật kho chuyển khoản | SQL trước/đặt/hủy | Đặt trừ ngay, hủy hoàn đúng mức trước; điều kiện doanh thu và shipping policy cũ giữ nguyên, test hồi quy PASS | PASS |
| Hóa đơn chờ tách loại | Form lưu/mở, SQL, thử lần sáu | Cùng nhân viên lưu 5 Quầy + 5 Online; mỗi lần sáu bị chặn; mở chờ phục hồi dữ liệu Unicode; lưu chờ không đổi kho; token phiên không được lưu vào snapshot mới | PASS |
| Tám kịch bản CSRF | POST thiếu/sai token ở add/online-orders/holds/invoice-status | Tám HTTP 403; User thường 403, Guest 302, Nhân viên và Admin 200 ở workspace được phép | PASS |
| Crawl toàn bộ link nội bộ | BFS bốn vai trò tới hết hàng đợi | admin: 487 URL, employee: 199 URL, user: 21 URL, guest: 20 URL; không link chết hoặc tài nguyên nội bộ 404 | PASS |
| Giao diện 1280/375 | Browser, đo DOM, ảnh kiểm tra trực quan | Không lỗi JS/console, không tràn trang; bảng/timeline cuộn trong khung; mọi form POST có CSRF | PASS |
| mvnw clean test | Không skip | 234 test, 0 failure/error/skipped | PASS |
| Quét QR bằng ngân hàng thật | Cần thiết bị/tài khoản thật | Chỉ giải mã QR tự động với tài khoản ngân hàng giả | NOT TESTED |
| GPS/bản đồ ngoài thật | Cần thiết bị và dịch vụ ngoài | Browser dùng tile giả, geocoding tắt; chọn tỉnh/phường/địa chỉ bằng tay được kiểm thử | NOT TESTED |

## Bằng chứng

[Maven](evidence/gd13/maven-summary.txt), [Started](evidence/gd13/startup.txt), [RESTORE mới](evidence/gd13/restore-new.txt), [RESTORE cuối](evidence/gd13/final-restore.json), [URL/status](evidence/gd13/URL_STATUS.md), [Playwright](evidence/gd13/browser-results.json), [coverage hóa đơn](evidence/gd13/invoice-coverage.json), [bù hóa đơn](evidence/gd13/backfill-sql.json), [luồng giao thành công](evidence/gd13/flow-sql.json), [nhánh hủy/thất bại](evidence/gd13/branches-sql.json), [lọc/SELECT](evidence/gd13/filter-sql.json), [giới hạn chờ](evidence/gd13/holds-sql.json), [snapshot cuối không token](evidence/gd13/final-hold-sql.json), [địa chỉ tạm](evidence/gd13/temporary-address-sql.json).

Ảnh: [hóa đơn 1280](evidence/gd13/invoice-1280.png), [375](evidence/gd13/invoice-375.png), [bản in](evidence/gd13/print-1280.png), [Bán online mở lại hóa đơn chờ](evidence/gd13/online-open-hold-and-separated-counter-375.png), [địa chỉ tạm](evidence/gd13/held-temporary-address-not-overwritten-375.png).

SELECT độc lập: `SELECT o.id,COUNT(i.id) FROM orders o LEFT JOIN invoices i ON i.order_id=o.id GROUP BY o.id HAVING COUNT(i.id)<>1`; `SELECT id,quantity FROM product_variants WHERE id=<ID>`; `SELECT id,status,payment_status,da_tru_kho,shipping_fee,final_amount FROM orders WHERE id=<ID>`; `SELECT status,actor,note,carrier,tracking_code FROM order_status_history WHERE order_id=<ID> ORDER BY id`; `SELECT order_type,COUNT(*) FROM pos_held_orders WHERE staff_user_id=<ID> GROUP BY order_type`. ID và kết quả từng bước trong JSON. Bản công bố bỏ token phiên trong snapshot hạn mức cũ, không công bố cookie/CSRF/mật khẩu; snapshot cuối không lưu token ở DB.

## Lỗi đã phát hiện và chạy lại

| Mã | Nguyên nhân | Nơi sửa | Bằng chứng sau sửa |
|---|---|---|---|
| G13-01 | CHECK hóa đơn chờ cũ chỉ nhận CASH/TRANSFER | 020_hoa_don_ban_online.sql; PosHoldService | COD/BANK_TRANSFER được lưu theo loại, Quầy vẫn giới hạn đúng; test 5+5 PASS |
| G13-02 | ID online-source trùng giữa nguồn đơn và nguồn tọa độ | ban-tai-quay.html | Nguồn đơn dùng online-order-source; form chọn nguồn và mở lại chờ PASS |
| G13-03 | Tải sổ sau mở chờ có thể thiếu combo hoặc ghi đè dữ liệu tạm | don-online-pos.js; ban-tai-quay.js | Chế độ khôi phục chỉ tải lựa chọn; tên/địa chỉ tạm và SELECT sổ giữ đúng |

Các test cũ kiểm tra nội dung trang đơn được chuyển sang Hóa đơn; có test riêng khẳng định redirect 302/307 và không thay dữ liệu trước khi endpoint đích xử lý. Không sao chép luật trạng thái/kho/giá/phí. Kiểm tra tĩnh premium còn lỗi lịch sử toàn repo (152 → 150); cảnh báo mới về nút Sao chép là do audit không nhận ra handler data-copy-confirmation, đã click thật PASS. Không tuyên bố audit toàn repo sạch.

## COUNT trước/sau 020

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
| roles | 3 | 3 | 3 |
| sizes | 8 | 8 | 8 |
| ten_tinh_thanh_cu | 68 | 68 | 68 |
| tinh_thanh | 34 | 34 | 34 |
| users | 12 | 12 | 12 |
| vouchers | 15 | 15 | 15 |
| vung_giao_hang | 3 | 3 | 3 |

## Triển khai sau sao lưu — chưa chạy trên DB thật

Áp dụng các migration còn thiếu theo thứ tự tới 019. Tạo file vận hành riêng:

```sql
EXEC sys.sp_set_session_context @key=N'shop.allow-production-migration',@value=1;
:r database/migrations/020_hoa_don_ban_online.sql
```

```powershell
sqlcmd -S <SERVER> -E -d shopbangiay -b -f 65001 -i <FILE_VAN_HANH.sql>
```

020 in danh sách hóa đơn được bù; nếu có đơn nhiều hóa đơn thì THROW và rollback để kiểm tra, không tự xóa. chay_tat_ca.sql và shopgiay.sql đã cập nhật. Khởi động lại ddl-auto=validate. Thử ngân hàng thật: cấu hình biến môi trường tài khoản do bạn sở hữu, tạo đơn chuyển khoản số tiền nhỏ, quét bằng ứng dụng ngân hàng và kiểm tra tiền/mã đơn trước xác nhận đã nhận tiền. Thử GPS trên HTTPS hoặc localhost với thiết bị có định vị. Không push/merge.
