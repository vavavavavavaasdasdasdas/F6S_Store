# Kiểm kê bảng trước khi sửa — Giai đoạn 2

Kiểm kê đã được dán trong hội thoại trước khi chỉnh source. Dữ liệu cột dưới đây được đối chiếu lại bằng ứng dụng trước sửa tại cổng 18121, dùng cùng bộ dữ liệu thử với ứng dụng sau sửa.

Có 25 mẫu table trước sửa, tương ứng 29 bảng khi mở đủ các biến thể template. Sau khi bổ sung các danh sách có cấu trúc và tính thêm bản in: phạm vi nghiệm thu 38 bảng. Bảng trước dạng danh sách có số cột HTML bằng 0; tên trường vẫn liệt kê đầy đủ.

| URL / bảng | Số cột trước | Các cột / trường trước | Cột đang gộp |
|---|---:|---|---|
| /admin/products-crud? — Sản phẩm | 9 | Ảnh; Mã / Tên sản phẩm; Thương hiệu; Đế giày; Chất liệu; Giá (₫); Tồn kho; Trạng thái; Hành động | Mã / Tên |
| /admin/variants? — Biến thể | 7 | Sản phẩm; Kích cỡ; Chất liệu; Giá (₫); Tồn kho; Trạng thái; Hành động | Mã / Tên |
| /admin/products-crud/view/{id} — Biến thể trong chi tiết sản phẩm | 5 | Kích cỡ; Chất liệu; Giá (₫); Tồn kho; Trạng thái | Không |
| /admin/categories? — Thương hiệu | 4 | ID; Tên thương hiệu; Trạng thái; Hành động | Không |
| /admin/sizes? — Kích cỡ | 3 | ID; Kích cỡ; Hành động | Không |
| /admin/materials? — Chất liệu | 5 | ID; Tên chất liệu; Trạng thái; Đang dùng; Hành động | Không |
| /admin/soles? — Đế giày | 5 | ID; Tên đế giày; Trạng thái; Đang dùng; Hành động | Không |
| /admin/inventory? — Tồn kho | 6 | Sản phẩm; Kích cỡ; Chất liệu; Tồn kho; Trạng thái biến thể; Hành động | Mã / Tên |
| /admin/inventory? — Lịch sử kho | 7 | Thời gian; Sản phẩm / Kích cỡ; Trước; Thay đổi; Sau; Lý do; Người thực hiện | Tên / Kích cỡ / Chất liệu |
| /admin/customers? — Khách hàng | 5 | Mã / Họ tên; Liên hệ; Tài khoản; Trạng thái; Hành động | Mã / Họ tên; SĐT / Email |
| /admin/employees? — Nhân viên | 5 | Mã / Họ tên; Liên hệ; Tài khoản; Trạng thái; Hành động | Mã / Họ tên; SĐT / Email |
| /admin/customers/detail/{id} — Lịch sử đơn của khách | 4 | Mã đơn; Ngày tạo; Trạng thái; Thành tiền | Không |
| /admin/customers/detail/{id} — Địa chỉ khách | 0 (danh sách) | Người nhận; SĐT; Địa chỉ; Mặc định | Các trường nằm trên cùng dòng danh sách |
| /admin/orders? — Đơn hàng Admin | 7 | Mã đơn; Kênh; Khách; Trạng thái; Thanh toán; Tổng (đ); Ngày tạo | Không |
| /admin/orders/{id}; /admin/orders/detail/{id} — Chi tiết đơn Admin | 6 | Sản phẩm; Kích cỡ; Chất liệu; SL; Đơn giá (đ); Thành tiền (đ) | Không |
| /admin/invoices? — Hóa đơn | 7 | Hóa đơn; Đơn hàng; Ngày; Khách; Nhân viên; Tổng (đ); Thanh toán | Không |
| /admin/invoices/{id} — Chi tiết hóa đơn | 6 | Sản phẩm; Kích cỡ; Chất liệu; SL; Đơn giá (đ); Thành tiền (đ) | Ảnh / Tên |
| /admin/invoices/{id} — chế độ in — Bản in hóa đơn | 6 | Sản phẩm; Kích cỡ; Chất liệu; SL; Đơn giá (đ); Thành tiền (đ) | Ảnh / Tên |
| /admin/vouchers? — Phiếu giảm giá | 7 | Mã / Tên; Mức giảm; Phạm vi; Thời gian; Lượt dùng; Trạng thái; Hành động | Mã / Tên; Loại / Giá trị giảm; Phạm vi / Khách; Bắt đầu / Kết thúc; Đã dùng / Giới hạn |
| /admin/promotions? — Đợt giảm giá | 5 | Tên; Mức giảm; Thời gian; Trạng thái; Hành động | Loại / Giá trị giảm; Bắt đầu / Kết thúc |
| /admin/promotions/{id}/products — Chọn sản phẩm giảm giá | 4 | Áp dụng; Mã; Sản phẩm; Trạng thái | Không |
| /admin/statistics; /admin/statistics/revenue; /admin/reports/revenue — Doanh thu ngày | 3 | Ngày; Số đơn; Doanh thu (đ) | Không |
| /admin/statistics/best-sellers; /admin/reports/best-sellers — Top số lượng | 4 | #; Sản phẩm; SL bán; Doanh thu (đ) | Ảnh / Tên |
| /admin/statistics/best-sellers; /admin/reports/best-sellers — Top doanh thu | 4 | #; Sản phẩm; SL bán; Doanh thu (đ) | Ảnh / Tên |
| /admin/statistics*; /admin/reports/* — Thống kê trạng thái | 2 | Trạng thái; Số đơn | Không |
| /admin/dashboard — Dashboard — ngày | 2 | Ngày; Doanh thu (₫) | Không |
| /admin/dashboard — Dashboard — đơn gần đây | 4 | Mã đơn / Ngày tạo; Khách hàng; Trạng thái; Tổng tiền | Mã đơn / Ngày tạo |
| /admin/dashboard — Dashboard — sắp hết | 5 | Sản phẩm; Kích cỡ; Chất liệu; Tồn kho; Thao tác | Không |
| /admin/dashboard — Dashboard — trạng thái | 0 (danh sách) | Trạng thái; Số đơn | Các trường nằm trên cùng dòng danh sách |
| /admin/dashboard — Dashboard — bán chạy | 0 (danh sách) | Tên sản phẩm; Số lượng | Các trường nằm trên cùng dòng danh sách |
| /orders — Đơn User | 5 | Mã Đơn Hàng; Ngày đặt; Tổng tiền; Trạng thái; Hành động | Không |
| /orders/{id} — Chi tiết đơn User | 6 | Sản phẩm; Kích cỡ; Chất liệu; Số lượng; Đơn giá; Thành tiền | Ảnh / Tên |
| /cart — Giỏ hàng | 5 | Sản Phẩm; Đơn Giá; Số Lượng; Thành Tiền; Thao Tác | Ảnh / Tên / Kích cỡ / Chất liệu; Giá bán / Giá gốc |
| /checkout — Checkout | 0 (danh sách) | Ảnh; Tên; Kích cỡ; Chất liệu; Số lượng; Giá bán; Giá gốc; Giảm (%) | Các trường nằm trên cùng dòng danh sách |
| /orders/success/{id} — Đặt hàng thành công | 0 (danh sách) | Tên; Kích cỡ; Chất liệu; Số lượng | Các trường nằm trên cùng dòng danh sách |
| /admin/pos — Giỏ POS | 0 (danh sách) | Tên; Kích cỡ; Chất liệu; Giá; Thành tiền; Số lượng; Hành động | Các trường nằm trên cùng dòng danh sách |
| /admin/pos — Hóa đơn chờ POS | 0 (danh sách) | Mã; Chất liệu; Số món; Tổng tiền; Hết hạn; Hành động | Các trường nằm trên cùng dòng danh sách |
| /admin/pos; GET /admin/pos/customers — Chọn khách POS | 0 (danh sách) | Họ tên; SĐT | Các trường nằm trên cùng dòng danh sách |

Đính chính khi đối chiếu nguồn chi tiết: bảng chi tiết đơn Admin trước sửa chỉ có tên sản phẩm, không có ảnh; phần kiểm kê đầu hội thoại đã gộp nhầm trường hợp này với hóa đơn/User. Bản sau giữ đúng 8 cột theo ví dụ người dùng; hóa đơn/User giữ ảnh trong cột riêng.
