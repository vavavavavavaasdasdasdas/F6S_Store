# Địa chỉ hành chính, bản đồ và logo

Đối chiếu ngày **30/10/2026** tại [Danh mục đơn vị hành chính, Cục Thống kê](https://danhmuchanhchinh.nso.gov.vn/Default.aspx). Chọn cấp xã hiện hành, xuất Excel: **3.321 phường/xã/đặc khu thuộc 34 tỉnh/thành phố**. Nguồn hiện hành dùng hai cấp; form không tạo thêm cấp quận/huyện cũ. Chỉ dùng mã và tên tiếng Việt hiện hành; không dùng cột tên tiếng Anh cũ trong Excel.

File nguồn xuất `Danh sách cấp xã ___30_10_2026.xls`, 866.784 byte, SHA-256 `d4a8ece8c02334bd428b661d835357c6736ad2646ac32f473707b0842fe6895a`. Bản chuẩn hóa nằm ở `database/data/don_vi_hanh_chinh_20261030.json`; migration 016 chứa SQL seed từ cùng dữ liệu. Mã tỉnh 2 chữ số, mã phường/xã 5 chữ số được giữ cả số 0 đầu. Chạy ứng dụng chỉ đọc DB, không gọi API hành chính ngoài. Tọa độ tâm chưa biết để NULL; không bịa tọa độ hàng loạt.

Kho mặc định: Cao đẳng FPT Polytechnic, Trịnh Văn Bô, Hà Nội; [địa chỉ do trường công bố](https://caodang.fpt.edu.vn/tin-noi-bat/sinh-vien-fpt-polytechnic-ha-noi-chinh-thuc-hoc-tap-tai-co-so-moi.html). Điểm xấp xỉ **21.03801, 105.74690** đối chiếu tại [bản đồ Mapcarta, đối tượng OSM N9593623734](https://mapcarta.com/N9593623734). Truy vấn trực tiếp OSM API không thành công trong môi trường này; không coi đây là điểm đo GPS hay cổng giao nhận đã được khảo sát. Admin có thể chỉnh tại `/admin/shipping-times`.

## Thư viện và dịch vụ

- **Leaflet 1.9.4**, BSD-2-Clause, [bản phát hành chính thức](https://leafletjs.com/download.html), [nguồn v1.9.4](https://github.com/Leaflet/Leaflet/tree/v1.9.4). JS, CSS, ảnh và toàn văn giấy phép được đặt tại `src/main/resources/static/js/vendor/leaflet/`. Không dùng CDN cho Leaflet. SHA-256 leaflet.js: `db49d009c841f5ca34a888c96511ae936fd9f5533e90d8b2c4d57596f4e5641a`.
- Tile bản đồ OpenStreetMap được tải khi dùng bản đồ, có attribution và liên kết bản quyền; tuân thủ [chính sách tile](https://operations.osmfoundation.org/policies/tiles/). Không tải hàng loạt, không cung cấp chế độ tải bản đồ offline.
- Geocoding/reverse qua server Nominatim, [chính sách sử dụng](https://operations.osmfoundation.org/policies/nominatim/). User-Agent định danh có URL dự án, cấu hình được qua `SHOP_GEOCODER_USER_AGENT`; khi triển khai nên cung cấp liên hệ vận hành thích hợp. Một request đang xử lý trên mỗi instance; mặc định cách nhau 1.100ms, không cho cấu hình dưới 1.000ms đối với host Nominatim công cộng; tối đa 20 lần/phút/IP. Không dùng dịch vụ này để tìm gợi ý theo từng ký tự. Chỉ tìm tâm sau khi chọn phường/xã, không gửi số nhà/tên người/SĐT tới geocoder.
- Tâm phường lưu DB; reverse cache 600 giây theo tọa độ làm tròn; lỗi/bận lưu thời điểm thử lại sau 300 giây. Timeout mặc định 2 giây; không giữ transaction DB khi gọi HTTP. Lỗi trả lại điểm ghim và thông báo chọn tay; thiếu điểm giao dùng phí tạm tính theo cấu hình.
- Test tự động dùng HTTP server giả trên localhost và mock tile; **không dùng Nominatim/OSM tile thật để nghiệm thu tự động**. Việc tải dataset/thư viện và đối chiếu nguồn được thực hiện riêng, không nằm trong các bài test ứng dụng.

## Logo

Dùng đúng `src/main/resources/static/anh/logo.jpg` do chủ dự án xác nhận: ảnh 221×166. `static/images/logo.jpg` là bản sao nguyên byte; favicon PNG 32×32 và 180×180 được thu theo tỷ lệ, không kéo méo. Không tự vẽ lại biểu trưng. Email dùng ảnh đính kèm inline `cid:f6s-logo`, đồng thời giữ bản thư plain text. Các bản chứng cứ HTML lịch sử trong docs giữ nguyên nội dung lúc chụp, có thể vẫn ghi đường dẫn logo cũ; source đang chạy không còn tham chiếu file SVG cũ.

Định vị trình duyệt cần **HTTPS hoặc localhost**. Dữ liệu vị trí gửi qua POST có CSRF, không đưa tọa độ nhận từ khách vào URL nội bộ. Trong triển khai nhiều instance, cần dùng geocoder riêng hoặc giới hạn tổng tốc độ ở hạ tầng vì bộ giới hạn/cache reverse hiện thuộc từng instance.
