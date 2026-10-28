# Ngữ cảnh giao diện bộ ảnh sản phẩm

Cửa hàng tiếng Việt, Thymeleaf và Bootstrap. Giữ nguyên giao diện hiện có; không đổi màu thương hiệu trong nhiệm vụ ảnh.

- Token và component chuẩn: `static/css/he-thong-thiet-ke.css`; primary hiện tại `--bs-primary: #b94312`, nền `#f8f9fa`, chữ `#272d2d`, font Inter/system, bo góc `--bs-border-radius`.
- Admin dùng layout quản trị, field/error và confirm hiện có; bộ ảnh dùng input file native nhiều lựa chọn. POST form multipart có CSRF do Thymeleaf/Spring Security.
- Gallery có khung vuông cố định, ảnh object-fit contain, mũi tên tròn trắng và thumbnail cuộn ngang; focus rõ bằng primary. Đổi ảnh không thay kích thước khung.
- Ảnh chính đồng bộ qua `products.image_url` cho các view cũ; bảng product_images là nguồn bộ ảnh, không lưu giá.
- Quản lý ảnh là thay đổi chưa lưu cho tới khi lưu sản phẩm; xóa có confirm. Server kiểm tra lại số lượng, loại thực, dung lượng và chủ sở hữu ảnh, không tin hidden field.
- Bảo mật/transaction thuộc ProductGalleryService và AdminCatalogService. File mới được dọn khi rollback, file bị xóa chỉ dọn sau commit; không xóa file dùng chung còn được ảnh khác tham chiếu.
- Trên mobile gallery dùng vuốt ngang, phím trái/phải khi focus gallery; 1 ảnh thì không render điều hướng/thumbnail.

## Phân trang dùng chung

- Nguồn cỡ trang duy nhất: `shop.page-size` trong application.properties. Helper `core/Pagination` tạo PageRequest và chuẩn hóa page; fragment duy nhất `fragments/phan-trang` hiển thị Trước/Sau, trang rút gọn và khoảng bản ghi. Giữ màu/component hiện có.
- URL do PaginationAdvice tạo từ toàn bộ query params, giữ giá trị lặp, Unicode và dấu +; mỗi bảng độc lập dùng tên tham số riêng khi cần. Chỉ render điều hướng khi có hơn một trang.
- Lựa chọn promotion chỉ cập nhật trang đã lưu, không làm mất các trang khác. POS tìm khách phía server có loading/error và giữ khách đã chọn khi đổi trang kết quả.

### Biến thể theo size và chất liệu

Chất liệu được quản lý riêng. Trang chi tiết và thêm nhanh dùng hai thẻ select có nhãn Size và Chất liệu; chỉ kết hợp có biến thể còn hàng mới chọn được. Khi chỉ có một chất liệu, chọn sẵn và vẫn hiển thị tên. POS dùng cùng cách chọn hai trường. Giỏ và kho phân biệt bằng ID biến thể; đơn hàng và hóa đơn dùng tên chất liệu đã chụp tại thời điểm mua, không đọc lại tên hiện tại.

### Luồng trạng thái và nhãn tồn kho của đơn

Chi tiết đơn quản trị hiển thị “Đã trừ kho / Chưa trừ kho”. COD chưa xác nhận chưa giữ hàng; khi xác nhận thiếu hàng phải giữ nguyên trạng thái và hiện rõ sản phẩm, size, chất liệu, số cần và số còn. Các bước là Chờ xử lý → Đã xác nhận → Đang chuẩn bị → Đang giao → Hoàn tất. Hủy trước giao và Giao hàng thất bại là các nhánh kết thúc khác nhau. User hủy đơn qua hộp thoại xác nhận, dùng cùng dịch vụ trạng thái với Admin và job quá hạn.

### Thời hạn chuyển khoản

Fragment `fragments/han-chuyen-khoan` dùng alert hiện có, không có CSS riêng. User thấy “Vui lòng chuyển khoản trước HH:mm dd/MM/yyyy” cho đơn online chuyển khoản chưa trả tiền và chưa kết thúc. Admin chỉ thấy “Tự hủy lúc ...” khi đơn còn Chờ xử lý, đúng điều kiện job hiện có. Thời hạn dùng created_at cộng cấu hình timeout, không tính lại từ lúc tải trang; nhãn biến mất khi đã thanh toán/hủy/thất bại.

### Dashboard chưa có dữ liệu

Không render khung canvas cho trạng thái đơn và sản phẩm bán chạy khi danh sách tương ứng rỗng; dùng thông báo trống hiện có. Điều này tránh kích thước canvas mặc định gây tràn ngang trên tablet sau khi dọn dữ liệu thử. Khi có dữ liệu, biểu đồ giữ hành vi cũ.

### Chất liệu — Giai đoạn A

Form tạo sản phẩm dùng select native và checkbox size, giữ component/error Bootstrap hiện có. Biến thể ban đầu là tùy chọn; chọn size thì bắt buộc chất liệu, giá và tồn kho. Cột chất liệu admin chỉ lấy biến thể đang bán, tối đa ba tên và +n; thẻ user dùng fragment chung. Ẩn chất liệu dùng dialog xác nhận chung, nêu số biến thể đang dùng; chỉ loại khỏi lựa chọn tạo mới, giữ tên trên dữ liệu cũ. Phân trang giữ materialId qua PaginationAdvice hiện có.

### Sổ địa chỉ nhận hàng — Giai đoạn C

Dùng select native của Bootstrap (popup do hệ điều hành sở hữu), nhãn/lỗi cạnh trường và dialog store-confirm hiện có. Tối đa ba địa chỉ, lựa chọn hiện nhãn Mặc định; admin xem danh sách, chủ tài khoản quản lý. Checkout dùng select Giao tới, điền recipient/phone/address nhưng cho sửa tạm; POST đơn giữ snapshot, không ghi ngược sổ. Dữ liệu và quyền sở hữu kiểm tra lại phía service; mọi thao tác khóa customer rồi mới đọc/ghi địa chỉ.

### Kiểm tra đường dẫn — phase 1

Ảnh sản phẩm dùng resolver productImageUrl chung: kiểm tra file theo shop.upload-dir hoặc classpath trước khi render URL. Thiếu file thì dùng /anh/anh-san-pham-thay-the.svg hiện có; giữ nguyên khung ảnh, alt, kích thước và màu sắc. Không sửa đường dẫn trong DB và không thay ảnh gốc bằng dữ liệu giả. ID không chuyển được sang số dùng trang 404 tiếng Việt có sẵn.

### Chính sách mật khẩu và ngôn ngữ — 25/10/2026

Người dùng chọn mật khẩu mới ít nhất 8 ký tự, có chữ và số; giữ trần 72 byte UTF-8 của BCrypt. PasswordPolicy là nguồn kiểm tra dùng chung cho đăng ký/đổi mật khẩu User/Admin/tài khoản nhân viên. Hiển thị hướng dẫn cạnh trường bằng primitive form-text/field-help có sẵn; giữ tham chiếu aria-describedby hiện có khi thêm lỗi. Nhãn chức năng dùng GIẢM/Kích cỡ; giữ tên riêng/thương hiệu SHOES, F6S Store, Nike, Adidas theo lựa chọn người dùng. Không đổi màu hoặc bố cục.

### Góp ý giảng viên — Giai đoạn 1

Categories chỉ đổi nhãn thành Thương hiệu, giữ route và mô hình. Đế giày thuộc sản phẩm, là lựa chọn không bắt buộc; dùng select native và cùng component field/error, dialog xác nhận, bảng và phân trang hiện có. Đế ẩn không chọn cho sản phẩm mới, sản phẩm đang dùng vẫn giữ tên và có thể giữ lựa chọn cũ khi sửa. Cột Đang dùng dẫn sang bộ lọc sản phẩm. Nhóm khách/nhân viên mang tên TÀI KHOẢN; trang cá nhân admin mang tên Hồ sơ của tôi. Không đổi màu hoặc bố cục chuẩn.
