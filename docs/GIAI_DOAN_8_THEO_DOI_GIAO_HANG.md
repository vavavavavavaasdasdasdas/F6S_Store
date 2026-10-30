# Giai đoạn 8 — Theo dõi đơn online và ngày giao dự kiến

30/10/2026, nhánh `gop-y-giang-vien`. Tiếp tục Giai đoạn 6–8 theo chỉ đạo mới, mỗi giai đoạn một commit. Bản sao mới `shopbangiay_kt_20261030_gd8` RESTORE từ file backup gần nhất `shopbangiay_kt_20261030_gd7_schema.bak`, hoàn tất12:56:10. Kiểm tra DB chưa tồn tại trước lần tạo; các lần REPLACE sau chỉ phục hồi bản sao gd8 do phiên này tạo. App cổng18200, đợi Started và ddl-auto=validate sạch. **Không kết nối shopbangiay hoặc shopbangiay_thu.** Cấu hình cục bộ/IDE đã trỏ gd8 đủschema; không công bố thông tin đăng nhập.

Tổng nghiệm thu chức năng: **20 PASS, 0 FAIL, 0 NOT TESTED**. Browser cuối **70 ca PASS**, **82 lượt crawl** (bao gồm crawl build cuối sau sửa lỗi kiểu dữ liệu). Kiểm tra tĩnh bổ sung toànrepo còn FAIL, tách riêng bên dưới; không dùng kết quả đó để giả PASS hoặc sửa ngoài phạm vi.

| Hạng mục | Cách kiểm tra | Kết quả thực tế | PASS/FAIL/NOT TESTED |
|---|---|---|---|
| Bản sao và validate | RESTORE mới, logStarted, Hibernatevalidate | gd8 từ backup gd7_schema12:56:10; app18200; validate sạch | PASS |
| Tỉnh hiện hành | Đối chiếu nguồn chính thức30/10/2026, SELECT | 34tỉnh/thành,68tên nhận diện,3vùng; tên có dấu; nguồn và phân vùng giao hàng ghi riêng | PASS |
| Migration015 | Hai lần trên bản sao từ backup gốc | Lần2 schema/COUNT không đổi; giá/tồn/trạng thái/tổng đơn cũ nguyên vẹn;28đơn cũ có28history | PASS |
| Bộ chạy tất cả | aggregate004–015 hai lần | Toàn bộ snapshot bằng nhau; không lỗi; cột chữ mới NVARCHAR | PASS |
| Nhận diện địa chỉ cũ | 10địa chỉ và10đơn giả, SQL trước/sau | Tên không dấu/tên cũ/TP.HCM/quốc gia/255ký tự đúng; tên đường và mâu thuẫn đểNULL; chạy lại chỉ1history | PASS |
| Sổ địa chỉ/checkout | Form thật, đổi từng địa chỉ đã lưu | Combo tỉnh bắt buộc và tìm được; đổi địa chỉ đồng bộ tên/SĐT/chi tiết/tỉnh; sửa tạm không đổi sổ | PASS |
| Khách Admin | Sửa qua form và SELECT | Combo chung; tỉnh lưu ở địa chỉ mặc định; bảng địa chỉ có cột tỉnh riêng | PASS |
| Cấu hình vùng | Form thật, lỗi min/max và offline retry | Ngày nguyên1–30; min<=max; lỗi cạnh ô; shared validator hỗ trợ inputform liên kết trong bảng; không đổi dự kiến cũ | PASS |
| Luồng giao hàng | Playwright1280/375 và SQL từng bước | Đặt→Xác nhận→Chuẩn bị→Giao có vận đơn→Đã giao; history5dòng, actor/thời gian/ghi chú đúng | PASS |
| Hủy/thất bại/kho | Hai nhánh mỗi viewport, SQL từng bước | Lý do bắt buộc; COD chưa xác nhận không trừ, xác nhận trừ1, hủy/thất bại hoàn1; cuối kho đúng | PASS |
| Gửi lặp/sai bước | Hai POST song song, POST nhảy bước | CùngCONFIRMED hai302; history/kho không đổi lần2; nhảy bước bị từ chối | PASS |
| Dự kiến/zone | 9unit cases và SELECT độc lập6đơn | Bắc+3, Trung/Nam5–7; cuối tháng/năm/năm nhuận, xác nhận muộn và Asia/Ho_Chi_Minh; snapshot không đổi khi sửa cấu hình | PASS |
| Sửa tỉnh đơn | Form/AdminPOST, SQLhistory | Trước giao đổi được, có tên trước/sau và actor; service chặn khi đãSHIPPING | PASS |
| Đơn cũ/POS | Browser và integration | Tỉnh không rõ hiển thị chưa xác định; ngày cũ không có không bịa; POS không có tiến trình vận chuyển | PASS |
| Hóa đơn khách/Admin/in | OwnerGET, IDOR, emulatemediaA4 | Đủ người nhận/địa chỉ/tỉnh/ngày/cộtSTT+thuộc tính; che điều hướng khi in; chỉ chủ đơn được xem hóa đơn khách | PASS |
| Tiếng Việt và kiểu dữ liệu | POST provinceIdabc, số ngàyabc/3.5/quá lớn | Trước lỗiEnglish, sau thông báoViệt;6integration test giao hàng gồm regression lỗi kiểu; mới15test so với196tổng | PASS |
| Bảo mật8nhóm | MockMvc và HTTP trên3POST mới/được sửa | Khách302, UserAdmin403, Admin200; ownerinvoice200, khácowner403; thiếu/saiCSRF403; POST hợp lệ đúng quyền | PASS |
| Playwright/JS/mobile | Hai viewport, browser vàcrawl | Không lỗi JS runtime/console,404,trang trắng hoặc tràn trang trong các ca cuối; ESC trảfocus, offline giữ form | PASS |
| Clean test | mvnw clean test khôngskip | 196test; failures0/errors0/skipped0;BUILD SUCCESS | PASS |
| Khôi phục | RESTORE checkpoint sau từng nhóm rồi SELECT | Snapshot cuối bằng sau015lần2; không còn dữ liệu thử của giai đoạn8; appđãdừng; localIDEtrỏ gd8 | PASS |
| Audit tĩnh bổ sung toànrepo | premium strict, soHEAD6fc8cbf |139cảnh báo ởHEAD,143ởbản mới;4cảnh báo mới do không nhận diện3handlerdata và combo chung, đã đối chiếu browser; các cảnh báo cũ nằm ngoài phạm vi | FAIL |

## SQL trước/sau

```sql
SELECT id,status,payment_status,da_tru_kho,tinh_thanh_id,ten_tinh_thanh,
 du_kien_giao_tu,du_kien_giao_den,ngay_giao_thuc_te,shipping_address
FROM orders WHERE id IN (674,675,676,677,678,679);
SELECT order_id,status,changed_at,actor,note,carrier,tracking_code
FROM order_status_history WHERE order_id IN (674,675,676,677,678,679)
ORDER BY order_id,changed_at,id;
SELECT id,quantity FROM product_variants WHERE id=5;
```

Các ID trên thuộc lượt dữ liệu giả và đã RESTORE bỏ đi; output từng bước được giữ trong [flow-proof.json](evidence/giai-doan-8/flow-proof.json), không suy từ dữ liệu sau khi khôi phục. ĐơnCOD trạng tháiPENDING giữ kho; CONFIRMED giảm1, PREPARING/SHIPPING không giảm thêm; DELIVERED giữ phần đã trừ; CANCELLED/DELIVERY_FAILED hoàn1. Kho variant5:97→96 ở đơn giao đầu;96→96 ở hai nhánh hủy/thất bại; lượt375 giao thêm1 còn95, hai nhánh sau vẫn95. Đơn phụ kiểm tra sửa tỉnh xác nhận còn94, haiPOST xác nhận lặp giữ94 và không thêmhistory.

Đối chiếu độc lập ngày bằng Python từ thời điểm đặt trong history và seedngày từng vùng; [raw so sánh](evidence/giai-doan-8/independent-date-stock-checks.json):

| Đơn giả | Tỉnh snapshot | Ngày đặt | Dự kiến từ | Dự kiến đến | Kho trước→cuối |
|---|---|---|---|---|---|
| 674 | Hà Nội | 2026-10-30 | 2026-11-02 | 2026-11-02 | 97→96 |
| 675 | Đà Nẵng | 2026-10-30 | 2026-11-04 | 2026-11-06 | 96→96 |
| 676 | Hồ Chí Minh | 2026-10-30 | 2026-11-04 | 2026-11-06 | 96→96 |
| 677 | Hà Nội | 2026-10-30 | 2026-11-02 | 2026-11-02 | 96→95 |
| 678 | Đà Nẵng | 2026-10-30 | 2026-11-04 | 2026-11-06 | 95→95 |
| 679 | Hồ Chí Minh | 2026-10-30 | 2026-11-04 | 2026-11-06 | 95→95 |

[Sửa tỉnh/gửi lặp trước-sau](evidence/giai-doan-8/province-idempotence.json), [sửa tạm không đổi sổ](evidence/giai-doan-8/temporary-address-book.json), [SQLđịa chỉAdmin và đơn cũ](evidence/giai-doan-8/quality-sql.json). Đổi tỉnh đơn trước giao sẽ ghi lịch sử và chụp lại dự kiến theo tỉnh mới; đổi cấu hình vùng đơn thuần giữ nguyên dự kiến cũ. Ngày cũ không có thông tin thì hiện thiếu dữ liệu, không điền giả.

## Migration, schema và COUNT

015 tạo4bảng: tinh_thanh,ten_tinh_thanh_cu,vung_giao_hang,order_status_history; thêmFK/tên snapshot/ngày giao vào orders vàFK tỉnh vào sổ địa chỉ. Tất cả cột chữ mới làNVARCHAR, gồm cả mã vùng và trạng thái history. Dữ liệu nguồn34tên,68alias,3vùng;28đơn cũ bù28history hiện tại. Không cập nhật trạng thái, tổng tiền, giá hoặc tồn cũ. Bộ aggregate004–015 đã chạy hai lần với snapshot giống nhau hoàn toàn.

| Bảng | Trước015 | Sau015lần2 | SauRESTOREcuối |
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
| order_status_history | 0 | 28 | 28 |
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
| ten_tinh_thanh_cu | 0 | 68 | 68 |
| tinh_thanh | 0 | 34 | 34 |
| users | 12 | 12 | 12 |
| vouchers | 15 | 15 | 15 |
| vung_giao_hang | 0 | 3 | 3 |

[Trước](evidence/giai-doan-8/before-015.json), [sau lần1](evidence/giai-doan-8/after-015-first.json), [sau lần2](evidence/giai-doan-8/after-015-second.json), [output015lần1](evidence/giai-doan-8/015-first.txt), [lần2](evidence/giai-doan-8/015-second.txt), [aggregatelần1](evidence/giai-doan-8/aggregate-first.txt), [lần2](evidence/giai-doan-8/aggregate-second.txt), [cộtNVARCHAR](evidence/giai-doan-8/new-text-columns.json), [FK](evidence/giai-doan-8/foreign-keys.json), [RESTOREcuối](evidence/giai-doan-8/final-restore.json), [outputRESTORE](evidence/giai-doan-8/restore-checkpoint.txt).

10ca địa chỉ/đơn giả: Da Nang→Đà Nẵng; Hải Dương→Hải Phòng; Bắc Giang→Bắc Ninh; Đắk Nông→Lâm Đồng; TP.HCM→Hồ Chí Minh; hậu tốViệtNam và255ký tự vẫn đúng. Đường Hà Nội, địa chỉ thiếu tỉnh, hoặc nhiều tỉnh mâu thuẫn đểNULL. [Input/expected/actual](evidence/giai-doan-8/inference-proof.json), [trước](evidence/giai-doan-8/inference-before.json), [sau1](evidence/giai-doan-8/inference-after-first.json), [sau2](evidence/giai-doan-8/inference-after-second.json). Các history bù luôn1dòng/đơn khi chạy lặp.

## Lỗi phát hiện và xử lý

| Mã | File:dòng | Nguyên nhân | Sửa/kiểm tra lại | Trạng thái |
|---|---|---|---|---|
| GD8-01 | messages.properties:1; AdminShippingRegionController.java:19; AdminOrderController.java:131 | Springbinding provinceIdabc trả thông báo chuyển kiểu tiếngAnh; số ngày không phảiint dễ rơi vào lỗi tham số | Bundle riêng3ô tỉnh; parse tham số mới có thông báoViệt. HTTPtrước200 kèm Failed to convert, sau200kèm Vui lòng chọn…;196test gồm regression | Đã sửa |
| GD8-02 | quan-tri-danh-muc.js:21; theo-doi-don-hang.js:1 | Validator dùngDOMcon trongform sẽ bỏ qua input liên kếtform ở bảng; prototypeform mới chưa dùng cùngowner | Dùngform.elements, novalidate và shared error/busy; Playwright min0/max<min, lý do rỗng, offline/retry, ESCfocus PASS | Đã sửa |
| GD8-03 | 015_theo_doi_giao_hang.sql:40 | Prototype suy tỉnh theo hậu tố bất kỳ có nguy cơ nhầm tên đường; bảng tạm255ký tự không đủ khi thêm dấu phân cách | Chỉ đoạn hành chính/tên cuối theo phân cách; NVARCHAR(MAX)bảngtạm;10caSQL PASS. Không có raw chạy phiên bản lỗi nên không nhận đã tái hiện trước sửa | Đã phòng lỗi, kiểm tra sau PASS |

[Thông báo trước](evidence/giai-doan-8/binding-before.json), [sau](evidence/giai-doan-8/binding-after.json), [browser cuối](evidence/giai-doan-8/browser-final.json), [mọi lần chạy](evidence/giai-doan-8/browser-attempts.json). Probe trước chỉ thu thập thông báo (`localized=false`), chưa khẳng định ngôn ngữ đạt; ca cuối có assertion thông báo tiếng Việt. Lỗi test trong lúc phát triển gồm importOrder trùng, accessorBoolean, cleanup thiếuFKhistory và expectedschema cũ; đã sửafixture/import và fullclean test cuối196PASS, không bỏ qua test.

## Bảo mật tám nhóm

| Nhóm | Kết quả thực tế | Kết luận |
|---|---|---|
| KháchGETAdmin/hoáđơnkhách |302đăngnhập|PASS|
| UserGETAdmin |403|PASS|
| AdminGET cấu hình/đơn |200|PASS|
| Chủ đơn/khác chủ xemhoáđơn |200/403|PASS|
| KháchPOSTcóCSRF |302|PASS|
| UserPOSTAdmincóCSRF |403|PASS|
| AdminPOSTthiếu/saiCSRF |403trênstatus/province/shipping-times|PASS|
| AdminPOSTđúngCSRF |302, service kiểm tra bước/lýdo/province/ngày; không sửa dữ liệu khi sai|PASS|

RawHTTP trong các ca security-* của browser-final.json; integration DeliveryIntegrationTest cũng kiểm tra ma trận. POST bị từ chối doCSRF/quyền không được tính là lỗi JavaScript. Không đưa token/cookie/mật khẩu vào bằng chứng công khai.

## URL và giao diện

| URL | Vai trò | Viewport | Status |
|---|---|---:|---:|
| / | guest | 1280 | 200 |
| /products | guest | 1280 | 200 |
| /products/3 | guest | 1280 | 200 |
| /cart | guest | 1280 | 200 |
| /login | guest | 1280 | 200 |
| / | guest | 375 | 200 |
| /products | guest | 375 | 200 |
| /products/3 | guest | 375 | 200 |
| /cart | guest | 375 | 200 |
| /login | guest | 375 | 200 |
| /profile | user | 1280 | 200 |
| /orders | user | 1280 | 200 |
| /orders/677 | user | 1280 | 200 |
| /orders/677/hoa-don | user | 1280 | 200 |
| /cart | user | 1280 | 200 |
| /profile | user | 375 | 200 |
| /orders | user | 375 | 200 |
| /orders/677 | user | 375 | 200 |
| /orders/677/hoa-don | user | 375 | 200 |
| /cart | user | 375 | 200 |
| /admin | admin | 1280 | 200 |
| /admin/products-crud | admin | 1280 | 200 |
| /admin/products-crud/view/3 | admin | 1280 | 200 |
| /admin/variants | admin | 1280 | 200 |
| /admin/colors | admin | 1280 | 200 |
| /admin/materials | admin | 1280 | 200 |
| /admin/soles | admin | 1280 | 200 |
| /admin/inventory | admin | 1280 | 200 |
| /admin/pos | admin | 1280 | 200 |
| /admin/customers | admin | 1280 | 200 |
| /admin/customers/detail/101 | admin | 1280 | 200 |
| /admin/employees | admin | 1280 | 200 |
| /admin/invoices | admin | 1280 | 200 |
| /admin/invoices/290 | admin | 1280 | 200 |
| /admin/orders/677 | admin | 1280 | 200 |
| /admin/shipping-times | admin | 1280 | 200 |
| /admin/shipping-times?region=TRUNG&page=abc | admin | 1280 | 200 |
| /admin/statistics/revenue | admin | 1280 | 200 |
| /admin/statistics/best-sellers | admin | 1280 | 200 |
| /admin | admin | 375 | 200 |
| /admin/products-crud | admin | 375 | 200 |
| /admin/products-crud/view/3 | admin | 375 | 200 |
| /admin/variants | admin | 375 | 200 |
| /admin/colors | admin | 375 | 200 |
| /admin/materials | admin | 375 | 200 |
| /admin/soles | admin | 375 | 200 |
| /admin/inventory | admin | 375 | 200 |
| /admin/pos | admin | 375 | 200 |
| /admin/customers | admin | 375 | 200 |
| /admin/customers/detail/101 | admin | 375 | 200 |
| /admin/employees | admin | 375 | 200 |
| /admin/invoices | admin | 375 | 200 |
| /admin/invoices/290 | admin | 375 | 200 |
| /admin/orders/677 | admin | 375 | 200 |
| /admin/shipping-times | admin | 375 | 200 |
| /admin/shipping-times?region=TRUNG&page=abc | admin | 375 | 200 |
| /admin/statistics/revenue | admin | 375 | 200 |
| /admin/statistics/best-sellers | admin | 375 | 200 |
| / | guest | 1280 | 200 |
| /products | guest | 1280 | 200 |
| /cart | guest | 1280 | 200 |
| / | guest | 375 | 200 |
| /products | guest | 375 | 200 |
| /cart | guest | 375 | 200 |
| /profile | user | 1280 | 200 |
| /orders | user | 1280 | 200 |
| /profile | user | 375 | 200 |
| /orders | user | 375 | 200 |
| /admin | admin | 1280 | 200 |
| /admin/shipping-times | admin | 1280 | 200 |
| /admin/orders/1 | admin | 1280 | 200 |
| /admin/invoices | admin | 1280 | 200 |
| /admin/pos | admin | 1280 | 200 |
| /admin/products-crud | admin | 1280 | 200 |
| /admin/inventory | admin | 1280 | 200 |
| /admin | admin | 375 | 200 |
| /admin/shipping-times | admin | 375 | 200 |
| /admin/orders/1 | admin | 375 | 200 |
| /admin/invoices | admin | 375 | 200 |
| /admin/pos | admin | 375 | 200 |
| /admin/products-crud | admin | 375 | 200 |
| /admin/inventory | admin | 375 | 200 |

[RawURL](evidence/giai-doan-8/urls.json), [Started/validate cuối](evidence/giai-doan-8/app-final.txt), [Maven196test](evidence/giai-doan-8/maven-clean-test.txt). Ảnh ví dụ: [đơn375](evidence/giai-doan-8/gd8-transition-delivered-shipping-375.png), [lý do/lịch sửUser375](evidence/giai-doan-8/gd8-user-timeline-failed-375.png), [bảninUserA4 từ375](evidence/giai-doan-8/gd8-invoice-user-a4-from-375.png), [AdminA4](evidence/giai-doan-8/gd8-invoice-admin-a4-from-375.png). Bảng rộng và tiến trình cuộn trong khung; bảninA4 giữ đủ cột, không đánh giá độ rộng bảnin bằng viewport điện thoại.

Audit tĩnh bổ sung strict có139cảnh báo trênHEAD và143ởbản mới:3nhóm mới gồm1select đã đượccombo-box.js nâng cấp,2nút data-transition/data-close-transition cóhandler và1nút in dùngin-hoa-don.js. Đây là4cảnh báo nhận diện tĩnh, đã đối chiếu code vàbrowser. Các cảnh báo cũ toànrepo không được sửa vì ngoài phạm vi. **Chưa đạt strict audit toànrepo**; [rawdiff](evidence/giai-doan-8/premium-audit-diff.json), [rawaudit](evidence/giai-doan-8/premium-final.json). Không tuyên bố đã chứng nhận toàn bộWCAG. Không có bộ formatter/accessibility riêng được cấu hình trongrepo; gitdiffcheck sạch.

## Lệnh triển khai thủ công — chưa thực thi trên DB thật

Sao lưu và dừng app trước. Nếu DB đang có004–012 thì thứ tự còn thiếu là **013→014→015**; đã có014 thì chỉ chạy015. Có thể dùng `chay_tat_ca.sql`004–015 đã kiểm tra lặp. `shopgiay.sql`đồng bộ để khởi tạo mới, không dùng để ghi đè DB có dữ liệu.

```powershell
@"
EXEC sys.sp_set_session_context @key=N'shop.allow-production-migration', @value=1;
GO
:r database/migrations/013_mau_sac_bien_the.sql
GO
:r database/migrations/014_tham_chieu_thanh_toan.sql
GO
:r database/migrations/015_theo_doi_giao_hang.sql
"@ | sqlcmd -S <MAY_CHU_SQL> -d <DATABASE_TRIEN_KHAI> -E -C -I -b -f 65001
```

Chỉ giữ các dòng:r còn thiếu, hoặc thay bằng:r database/migrations/chay_tat_ca.sql. Guardproduction cho tênDBđã khai báo trongscript vàsessioncontext rõ ràng; không tắtguard. Sau migration chạy app với ddl-auto=validate, chờStarted. Bản này không thay luật kho/hủy/doanh thu, không merge/push. Đã hoàn tất các phần còn lại6–8 theo chỉ đạo tiếp tục, dừng sau commit8. Quét QR bằng ứng dụng ngân hàng thật vẫn NOT TESTED của Giai đoạn7; hướng dẫn ở README và báo cáo7.
