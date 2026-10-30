# Nguồn tỉnh/thành và tên cũ

Đối chiếu trực tuyến **30/10/2026**. Danh sách tên hiện hành đối chiếu [hệ thống đơn vị hành chính của cơ quan thống kê](https://danhmuchanhchinh.nso.gov.vn/TongHop.aspx). Việc hợp nhất và ánh xạ tên cũ đối chiếu [Nghị quyết202/2025/QH15 trên Cổng thông tin Chính phủ](https://xaydungchinhsach.chinhphu.vn/toan-van-nghi-quyet-so-202-2025-qh15-ve-sap-xep-don-vi-hanh-chinh-cap-tinh-119250612174148722.htm).

Lưu34 **tên riêng**, không cố định tiền tố Tỉnh/Thành phố; bảng trực tuyến có cập nhật loại đơn vị sau2025 nhưng các tên dùng dưới đây vẫn khớp. Không sao chép các ghi chú nhiễu của trang nguồn vào dữ liệu. Việc chia BAC/TRUNG/NAM dưới đây là **cấu hình giao hàng của project**, không phải khẳng định phân vùng hành chính chính thức. Kho miền Bắc; Bắc3–3, Trung/Nam5–7 ngày lịch theo yêu cầu. Không tính ngày làm việc, ngày lễ hoặc gọi dịch vụ vận chuyển ngoài.

| Vùng giao hàng | Tỉnh/Thành phố |
|---|---|
| BAC | Hà Nội, Cao Bằng, Tuyên Quang, Điện Biên, Lai Châu, Sơn La, Lào Cai, Thái Nguyên, Lạng Sơn, Quảng Ninh, Bắc Ninh, Phú Thọ, Hải Phòng, Hưng Yên, Ninh Bình |
| TRUNG | Thanh Hóa, Nghệ An, Hà Tĩnh, Quảng Trị, Huế, Đà Nẵng, Quảng Ngãi, Gia Lai, Khánh Hòa, Đắk Lắk, Lâm Đồng |
| NAM | Đồng Nai, Hồ Chí Minh, Tây Ninh, Đồng Tháp, Vĩnh Long, An Giang, Cần Thơ, Cà Mau |

## Tên cũ và tên gọi khác

Mỗi tên hiện hành cũng là một alias của chính nó. Tên gọi TP HCM/TPHCM/HCM/Sài Gòn và Thừa Thiên Huế được giữ để nhận diện dữ liệu cũ. Các bản viết không dấu được so qua collation CI_AI; dấu chấm, gạch nối và khoảng trắng được chuẩn hóa trong bảng tạm, không sửa chuỗi địa chỉ gốc.

| Tên cũ/tên gọi | Tên hiện hành |
|---|---|
| Hà Giang | Tuyên Quang |
| Yên Bái | Lào Cai |
| Bắc Kạn | Thái Nguyên |
| Vĩnh Phúc, Hòa Bình | Phú Thọ |
| Bắc Giang | Bắc Ninh |
| Thái Bình | Hưng Yên |
| Hải Dương | Hải Phòng |
| Hà Nam, Nam Định | Ninh Bình |
| Quảng Bình | Quảng Trị |
| Thừa Thiên Huế | Huế |
| Quảng Nam | Đà Nẵng |
| Kon Tum | Quảng Ngãi |
| Bình Định | Gia Lai |
| Phú Yên | Đắk Lắk |
| Ninh Thuận | Khánh Hòa |
| Đắk Nông, Bình Thuận | Lâm Đồng |
| Bà Rịa Vũng Tàu, Bình Dương, TPHCM, TP HCM, HCM, Sài Gòn | Hồ Chí Minh |
| Bình Phước | Đồng Nai |
| Long An | Tây Ninh |
| Sóc Trăng, Hậu Giang | Cần Thơ |
| Bến Tre, Trà Vinh | Vĩnh Long |
| Tiền Giang | Đồng Tháp |
| Bạc Liêu | Cà Mau |
| Kiên Giang | An Giang |

Migration chỉ nhận diện tên ở phần hành chính có tiền tố hoặc phân đoạn cuối sau dấu phẩy/chấm phẩy; nếu có nhiều tỉnh khác nhau thì đểNULL. Không nhận đường Hà Nội thành tỉnh Hà Nội. Địa chỉ không có bằng chứng hành chính rõ đểNULL để Admin/người dùng chọn lại. Tên tỉnh của đơn là bản chụp, lịch sử cũ chỉ bù một trạng thái hiện tại; không bịa thời gian các bước hoặc ngày giao cũ. Xem10ca dữ liệu giả trước/sau trong báo cáo Giai đoạn8.
