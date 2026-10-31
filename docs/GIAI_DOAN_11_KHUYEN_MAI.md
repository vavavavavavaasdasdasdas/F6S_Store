# Giai đoạn 11 — Khuyến mãi

Nhánh `gop-y-dot-moi`. RESTORE mới `shopbangiay_kt_20261031_gd11`, app cổng 18311 có Started và `ddl-auto=validate` sạch. Không kết nối `shopbangiay` hoặc `shopbangiay_thu`. Migration 018 chỉ đổi dữ liệu phạm vi phiếu; giữ các cột cũ, liên kết khách cũ và bộ đếm lịch sử. Mã phiếu được chuyển: **ID 17 — P8-PRIVATE — riêng tư → công khai**. Có OUTPUT ID/mã/trước/sau trong [018-first.txt](evidence/gd11/018-first.txt); lần hai không có dòng đổi.

| Hạng mục | Cách kiểm tra | Kết quả thực tế | PASS/FAIL/NOT TESTED |
|---|---|---|---|
| Phiếu luôn công khai, bỏ phạm vi/khách/giới hạn | Form thật, DOM, server gửi tham số giả | Các trường biến mất; service không kiểm tra sai khách/hết lượt; tham số cũ kể cả abc bị bỏ qua trước bind; is_public=true, used_count=0 | PASS |
| Không xóa phiếu | DOM cột Hành động và POST trực tiếp có CSRF | Chỉ icon Sửa; POST xóa 405, dòng vẫn tồn tại; thiếu quyền/token vẫn 403 | PASS |
| Hiệu lực theo giờ Việt Nam | Clock giả trước bắt đầu, lúc bắt đầu, cuối ngày kết thúc, ngày kế tiếp | UPCOMING → RUNNING → RUNNING tại 23:59:59.999999999 → EXPIRED lúc 00:00; tắt luôn INACTIVE | PASS |
| Công tắc phiếu | Playwright hai viewport, AJAX/SELECT | Bật phiếu tương lai vẫn Sắp diễn ra; tắt Tạm dừng; bật lại Sắp diễn ra; không tải lại trang | PASS |
| Công tắc và loại giảm đợt | Form thật chọn Số tiền đồng, nhập 15000, bật/tắt và SELECT | discount_type=AMOUNT, discount_value=15000; trạng thái AJAX khớp DB | PASS |
| Chọn sản phẩm nhiều trang | Bật tất cả 12 ở trang 1, chọn một ở trang 2, Lưu thay đổi, quay lại | SELECT đúng 13 product_id, trang 1 còn đủ 12 lựa chọn; có nút bật/tắt toàn trang | PASS |
| Chỉ liệt kê sản phẩm đang bán | Ngừng bán bằng giao diện rồi tải lại màn chọn; bán lại | Ẩn đúng sản phẩm, bán lại xuất hiện; quantity/status biến thể trước/sau bằng nhau | PASS |
| Công thức PricingService | Unit phần trăm/số tiền/hỗn hợp/vượt giá/hết hạn/tắt, batch và checkout/POS integration | 100000 với 10% và 15000 → 85000; với 20% và 15000 → 80000; giảm 500000 → 0; không tổng âm, snapshot đơn giữ nguyên | PASS |
| Voucher sau khuyến mãi/đếm đã dùng | Integration online/POS/rollback/hai request | Dùng chung công thức; bỏ giới hạn cũ vẫn tăng used_count chính xác cho hai đơn; lỗi rollback không tiêu thụ | PASS |
| Bộ lọc, rỗng, page lạ | Playwright cả hai viewport | RUNNING/UPCOMING/EXPIRED/INACTIVE, hai loại giảm, không kết quả, -1/abc/99999 đều HTTP 200, không JS lỗi | PASS |
| Tám tình huống bảo mật | Spring Security MockMvc | 302,403,200,302,403,403,403,200; xóa có quyền/token vẫn 405 | PASS |
| Crawl toàn bộ link nội bộ | BFS đến hết hàng đợi, ba vai trò | admin: 548 URL, user: 23 URL, guest: 22 URL; không link nội bộ chết hay tài nguyên nội bộ 404 | PASS |
| Playwright 1280/375 và trang chính | Luồng thật, DOM và ảnh | Không lỗi JS/runtime, không tràn ngang trang; bảng cuộn trong khung, hành động icon có nhãn | PASS |
| Migration hai lần | COUNT/schema/biến thể/đơn trước/sau | Không đổi COUNT/schema; giá/tồn/trạng thái biến thể và số liệu đơn giữ nguyên; lần hai không đổi dữ liệu | PASS |
| mvnw clean test không skip | Maven offline clean test | 221 test, 0 lỗi, 0 thất bại, 0 bỏ qua | PASS |

## Bằng chứng

[Maven/Clock/bảo mật](evidence/gd11/maven-summary.txt), [Playwright](evidence/gd11/browser-results.json), [SQL 1280](evidence/gd11/flow-sql-1280.json), [SQL 375](evidence/gd11/flow-sql-375.json), [SQL trạng thái sản phẩm/tồn](evidence/gd11/stop-selection-sql.json), [bind tham số cũ](evidence/gd11/retired-binding-sql.json), [URL/status](evidence/gd11/URL_STATUS.md), [Started](evidence/gd11/startup.txt), [RESTORE mới](evidence/gd11/restore-new.txt), [phục hồi cuối](evidence/gd11/final-restore.json).

SELECT độc lập sau form: `SELECT id,code,is_public,used_count,status,usage_limit,customer_id FROM vouchers WHERE id=<ID>`; `SELECT id,ma,discount_type,discount_value,status FROM promotions WHERE id=<ID>`; `SELECT product_id FROM promotion_details WHERE promotion_id=<ID> ORDER BY product_id`. Kết quả thật và ID trong JSON, không ghi cookie/token/mật khẩu.

`DiscountEffectivity` sở hữu điều kiện thời gian, dùng Clock có sẵn, ép múi giờ Asia/Ho_Chi_Minh. Phiếu lưu ngày kết thúc ở 00:00 và kiểm tra tới trước 00:00 ngày kế tiếp, tránh DATETIME làm tròn cuối ngày. Đợt giảm giá giữ thời điểm kết thúc chính xác như luồng cũ. PricingService vẫn hỗ trợ dữ liệu AMOUNT đang có; lựa chọn theo số đồng thực tế, giới hạn giảm bằng giá gốc. Không thay luật kho, hủy hoặc điều kiện doanh thu.

`quan-tri-giam-gia.js` sở hữu công tắc AJAX (khóa khi gửi, lỗi giữ giá trị trước, CSRF) và lựa chọn phân trang (sessionStorage theo ID đợt; khi lưu gửi cả thay đổi ngoài trang, server kiểm tra lại chỉ sản phẩm đang bán). Không gọi CDN mới. Lỗi fixture Playwright ban đầu do bộ chọn input/nút submit không phân biệt token ẩn và nút Lọc liên kết form khác; đã giới hạn selector và chạy lại. Không sửa dữ liệu để ép kết quả.

Audit strict là bổ sung: cảnh báo toàn repo cũ vẫn được ghi trong JSON. Hai nút bật/tắt toàn trang có handler trong quan-tri-giam-gia.js và đã được bấm thật; audit tĩnh không phát hiện thuộc tính data-selection-all. Không tuyên bố audit toàn dự án sạch.

## COUNT trước/sau 018

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
| roles | 2 | 2 | 2 |
| sizes | 8 | 8 | 8 |
| ten_tinh_thanh_cu | 68 | 68 | 68 |
| tinh_thanh | 34 | 34 | 34 |
| users | 12 | 12 | 12 |
| vouchers | 15 | 15 | 15 |
| vung_giao_hang | 3 | 3 | 3 |

## Lệnh chạy migration thật sau khi sao lưu

Chưa chạy trên DB thật. Tạo file vận hành riêng:

```sql
EXEC sys.sp_set_session_context @key=N'shop.allow-production-migration',@value=1;
:r database/migrations/018_phieu_cong_khai.sql
```

```powershell
sqlcmd -S <SERVER> -E -d shopbangiay -b -f 65001 -i <FILE_VAN_HANH.sql>
```

Chạy các migration trước đó còn thiếu theo thứ tự tới 017 trước khi chạy 018; chay_tat_ca.sql và shopgiay.sql đã cập nhật. Khởi động lại `ddl-auto=validate`. Không push/merge.
