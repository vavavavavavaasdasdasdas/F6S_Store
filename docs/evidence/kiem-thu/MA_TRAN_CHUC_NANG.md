# Ma trận chức năng

Mỗi mục UI chạy ở 1280px và 375px; ma trận HTTP/SQL được lưu riêng. PASS là kết quả sau sửa/chạy lại.

| Mã | Chức năng | Vai trò | Các bước | Kết quả | Bằng chứng |
|---|---|---|---|---|---|
| SC-01 | Restore bản sao mới, không ghi đè DB cũ | Hệ thống | Đọc metadata backup; restore vào tên/MDF/LDF mới; đối chiếu COUNT | PASS | schema/restore-initial.log |
| SC-02 | Validate trước migration | Hệ thống | Khởi động ddl-auto=validate trên DB audit thiếu migration; lấy lỗi gốc | PASS | schema/validate-before.log; schema-check-before.log |
| SC-03 | Migration gộp chạy hai lần | Hệ thống | Chạy runner 004–009 hai lần; SELECT COUNT/schema sau mỗi lần | PASS | schema/migrations-first.log; migrations-second.log; after-first.json; after-second.json |
| SC-04 | SchemaCheck và validate sau migration | Hệ thống | Khởi động lại với SchemaCheck và validate; đọc log Started | PASS | schema/validate-after.log |
| SC-05 | Checkout không còn thiếu bảng 009 | User | Đăng nhập, thêm sản phẩm vào giỏ rồi mở GET /checkout | PASS | schema/schema-checkout-http.json |
| SC-06 | Clean test nhóm schema | Hệ thống | Chạy mvnw clean test trên đúng nội dung commit schema | PASS | schema/schema-clean-test.log:123 tests |
| SC-07 | Phục hồi sau test phá dữ liệu | Hệ thống | Dừng app; restore checkpoint; so sánh toàn bộ COUNT/schema | PASS | schema/reset-schema-tests.log; after-schema-tests-reset.json |
| G-01 | Trang chủ | Khách | Mở trang chủ, đọc các khối sản phẩm | PASS | guest-password/guest-results.json (1280 và 375px) |
| G-02 | Lọc thương hiệu | Khách | Chọn thương hiệu và áp dụng bộ lọc | PASS | guest-password/guest-results.json (1280 và 375px) |
| G-03 | Lọc kích cỡ | Khách | Chọn kích cỡ và áp dụng bộ lọc | PASS | guest-password/guest-results.json (1280 và 375px) |
| G-04 | Lọc chất liệu | Khách | Chọn chất liệu và kiểm tra sản phẩm hiển thị | PASS | guest-password/guest-results.json (1280 và 375px) |
| G-05 | Lọc giá | Khách | Nhập khoảng giá, áp dụng và đọc giá trên thẻ | PASS | guest-password/guest-results.json (1280 và 375px) |
| G-06 | Sắp xếp | Khách | Đổi sắp xếp, đọc giá/thứ tự các thẻ | PASS | guest-password/guest-results.json (1280 và 375px) |
| G-07 | Tìm kiếm | Khách | Nhập từ khóa tên/mã, submit và đọc kết quả | PASS | guest-password/guest-results.json (1280 và 375px) |
| G-08 | Phân trang 12 | Khách | Tạo đủ dữ liệu, chuyển trang và kiểm tra giới hạn 12 | PASS | guest-password/guest-results.json (1280 và 375px) |
| G-09 | Chọn kích cỡ và chất liệu | Khách | Mở chi tiết; chọn chất liệu rồi kích cỡ; đọc biến thể được chọn | PASS | guest-password/guest-results.json (1280 và 375px) |
| G-10 | Tồn kho sản phẩm | Khách | Đọc tồn và thử biến thể hết hàng/không cho đặt | PASS | guest-password/guest-results.json (1280 và 375px) |
| G-11 | Gallery nhiều góc | Khách | Bấm lần lượt ảnh nhiều góc, kiểm tra ảnh chính thay đổi | PASS | guest-password/guest-results.json (1280 và 375px) |
| G-12 | Khuyến mãi | Khách | Mở bộ lọc khuyến mãi và thẻ sản phẩm đang giảm | PASS | guest-password/guest-results.json (1280 và 375px) |
| G-13 | Thêm nhanh giỏ | Khách | Mở thêm nhanh, chọn chất liệu/kích cỡ rồi thêm giỏ | PASS | guest-password/guest-results.json (1280 và 375px) |
| G-14 | Đăng ký hợp lệ | Khách | Gửi form đăng ký hợp lệ và kiểm tra tài khoản được tạo | PASS | guest-password/guest-results.json (1280 và 375px) |
| G-15 | Đăng ký trùng username | Khách | Gửi form với tên đăng nhập đã có, đọc lỗi | PASS | guest-password/guest-results.json (1280 và 375px) |
| G-16 | Đăng ký trùng email | Khách | Gửi form với thư điện tử đã có, đọc lỗi | PASS | guest-password/guest-results.json (1280 và 375px) |
| G-17 | Mật khẩu yếu | Khách | Gửi mật khẩu 123, kiểm tra form bị chặn sau sửa | PASS | guest-password/guest-results.json (1280 và 375px) |
| G-18 | Đăng nhập sai/đúng | Khách | Thử mật khẩu sai rồi đúng bằng tài khoản fixture | PASS | guest-password/guest-results.json (1280 và 375px) |
| G-19 | Đăng xuất | Khách | Gửi form đăng xuất và kiểm tra trạng thái khách | PASS | guest-password/guest-results.json (1280 và 375px) |
| U-01 | Sửa hồ sơ | User | Sửa họ tên/thông tin hồ sơ, tải lại và SELECT | PASS | user-security/browser-results.json; stock-before-after.json; all-route-security.json |
| U-02 | Đổi mật khẩu | User | Đổi mật khẩu; đăng nhập bằng mật khẩu cũ/mới | PASS | user-security/browser-results.json; stock-before-after.json; all-route-security.json |
| U-03 | Thêm 3 địa chỉ | User | Thêm lần lượt ba địa chỉ bằng form hồ sơ | PASS | user-security/browser-results.json; stock-before-after.json; all-route-security.json |
| U-04 | Địa chỉ thứ 4 bị chặn | User | Thử thêm địa chỉ thứ tư với CSRF hợp lệ, SELECT vẫn ba | PASS | user-security/browser-results.json; stock-before-after.json; all-route-security.json |
| U-05 | Sửa địa chỉ | User | Chọn địa chỉ trong combo, sửa và tải lại | PASS | user-security/browser-results.json; stock-before-after.json; all-route-security.json |
| U-06 | Xóa địa chỉ | User | Xóa địa chỉ mặc định, kiểm tra địa chỉ còn lại được chọn mặc định | PASS | user-security/browser-results.json; stock-before-after.json; all-route-security.json |
| U-07 | Đặt mặc định | User | Đặt địa chỉ thứ hai làm mặc định, SELECT customers.address | PASS | user-security/browser-results.json; stock-before-after.json; all-route-security.json |
| U-08 | Giỏ cộng dồn | User | Thêm cùng biến thể hai lần, kiểm tra gộp số lượng | PASS | user-security/browser-results.json; stock-before-after.json; all-route-security.json |
| U-09 | Giỏ sửa số lượng | User | Sửa số lượng giỏ rồi tải lại | PASS | user-security/browser-results.json; stock-before-after.json; all-route-security.json |
| U-10 | Giỏ vượt tồn | User | Nhập vượt tồn ở UI và gửi trực tiếp AJAX, kiểm tra từ chối | PASS | user-security/browser-results.json; stock-before-after.json; all-route-security.json |
| U-11 | Xóa giỏ và rỗng | User | Xóa dòng cuối, kiểm tra giỏ rỗng | PASS | user-security/browser-results.json; stock-before-after.json; all-route-security.json |
| U-12 | Checkout chọn địa chỉ | User | Chọn từng địa chỉ đã lưu ở checkout, đọc ba trường được điền | PASS | user-security/browser-results.json; stock-before-after.json; all-route-security.json |
| U-13 | Phiếu giảm giá hợp lệ | User | Nhập mã hợp lệ, áp dụng và kiểm tra giảm 5000 | PASS | user-security/browser-results.json; stock-before-after.json; all-route-security.json |
| U-14 | Phiếu giảm giá: bảy trường hợp từ chối | User | Thử khóa/chưa tới/hết hạn/hết lượt/tối thiểu/riêng tư/không tồn tại | PASS | user-security/browser-results.json; stock-before-after.json; all-route-security.json |
| U-15 | COD | User | Đặt COD bằng form; SELECT đơn, snapshot tạm và tồn kho | PASS | user-security/browser-results.json; stock-before-after.json; all-route-security.json |
| U-16 | Chuyển khoản | User | Đặt chuyển khoản bằng form; SELECT đơn và tồn kho | PASS | user-security/browser-results.json; stock-before-after.json; all-route-security.json |
| U-17 | Thiếu hàng rollback | User | Mở checkout rồi giảm tồn fixture, submit và so SQL trước/sau | PASS | user-security/browser-results.json; stock-before-after.json; all-route-security.json |
| U-18 | Trang thành công | User | Sau đặt đơn, mở trang thành công và đọc mã/nội dung | PASS | user-security/browser-results.json; stock-before-after.json; all-route-security.json |
| U-19 | Danh sách đơn | User | Mở danh sách đơn của tài khoản fixture | PASS | user-security/browser-results.json; stock-before-after.json; all-route-security.json |
| U-20 | Chi tiết đơn | User | Mở chi tiết đơn vừa đặt, đọc dữ liệu đơn | PASS | user-security/browser-results.json; stock-before-after.json; all-route-security.json |
| U-21 | Hủy đơn | User | Hủy đơn đã xác nhận, SELECT tồn/log/trạng thái | PASS | user-security/browser-results.json; stock-before-after.json; all-route-security.json |
| U-22 | Hủy lần hai | User | Gửi yêu cầu hủy lần hai, kiểm tra không hoàn kho thêm | PASS | user-security/browser-results.json; stock-before-after.json; all-route-security.json |
| U-23 | Chặn truy cập đơn người khác | User | Đăng nhập người khác, mở chi tiết và trang thành công của đơn | PASS | user-security/browser-results.json; stock-before-after.json; all-route-security.json |
| A-01 | Bảng điều khiển khớp SELECT | Admin | Mở Dashboard, so sáu data-value với SELECT độc lập | PASS | admin/*-results.json; admin/pos-stock-ledger.json; admin/statistics-independent-sql.json |
| A-02 | Tạo sản phẩm và biến thể ban đầu | Admin | Form chọn hai kích cỡ; thử thiếu chất liệu; bổ sung và lưu; SELECT biến thể/log | PASS | admin/*-results.json; admin/pos-stock-ledger.json; admin/statistics-independent-sql.json |
| A-03 | Upload 3 ảnh | Admin | Upload ba PNG bằng input file; tải lại gallery và kiểm tra ảnh tải được | PASS | admin/*-results.json; admin/pos-stock-ledger.json; admin/statistics-independent-sql.json |
| A-04 | Đổi ảnh chính | Admin | Đặt ảnh thứ ba làm ảnh chính; lưu; SELECT ID ảnh chính | PASS | admin/*-results.json; admin/pos-stock-ledger.json; admin/statistics-independent-sql.json |
| A-05 | Sửa sản phẩm | Admin | Đổi tên sản phẩm trong form sửa, lưu rồi tải lại | PASS | admin/*-results.json; admin/pos-stock-ledger.json; admin/statistics-independent-sql.json |
| A-06 | Xóa sản phẩm có/không đơn | Admin | Xóa sản phẩm trống và sản phẩm đã có đơn; kiểm tra xóa/vô hiệu hóa | PASS | final/*-results.json; final/admin-orders-sql.json; final/quality-all-results.json |
| A-07 | Chặn trùng bộ ba biến thể | Admin | Tạo lại cùng sản phẩm+kích cỡ+chất liệu, đọc lỗi và SELECT | PASS | admin/*-results.json; admin/pos-stock-ledger.json; admin/statistics-independent-sql.json |
| A-08 | Thương hiệu | Admin | Thêm/sửa thương hiệu rồi ẩn, tải lại kiểm tra trạng thái | PASS | admin/*-results.json; admin/pos-stock-ledger.json; admin/statistics-independent-sql.json |
| A-09 | kích cỡ | Admin | Thêm/sửa/xóa kích cỡ; thử trùng và đọc thông báo sau lưu | PASS | admin/*-results.json; admin/pos-stock-ledger.json; admin/statistics-independent-sql.json |
| A-10 | Chất liệu ẩn/xóa | Admin | CRUD chất liệu chưa dùng; ẩn chất liệu dùng; kiểm tra combo/đơn cũ/xóa bị chặn | PASS | admin/*-results.json; admin/pos-stock-ledger.json; admin/statistics-independent-sql.json |
| A-11 | Điều chỉnh tồn | Admin | Nhập thêm bốn đơn vị kèm lý do, SELECT tồn | PASS | admin/*-results.json; admin/pos-stock-ledger.json; admin/statistics-independent-sql.json |
| A-12 | Lịch sử kho | Admin | Mở lịch sử theo biến thể, đọc chất liệu/lý do/số trước-sau | PASS | admin/*-results.json; admin/pos-stock-ledger.json; admin/statistics-independent-sql.json |
| A-13 | Khách hàng CRUD | Admin | Thêm/sửa/xem/xóa khách chưa được tham chiếu, SELECT | PASS | admin/*-results.json; admin/pos-stock-ledger.json; admin/statistics-independent-sql.json |
| A-14 | Vô hiệu hóa khách | Admin | Vô hiệu hóa khách; với khách liên kết kiểm tra status tài khoản và đăng nhập bị chặn | PASS | admin/*-results.json; admin/pos-stock-ledger.json; admin/statistics-independent-sql.json |
| A-15 | Sổ địa chỉ admin | Admin | Mở chi tiết khách có địa chỉ, đọc mặc định và kiểm tra chỉ xem | PASS | admin/*-results.json; admin/pos-stock-ledger.json; admin/statistics-independent-sql.json |
| A-16 | Nhân viên CRUD và liên kết tài khoản | Admin | Tạo nhân viên có tài khoản, sửa và đăng nhập; vô hiệu hóa/xóa, SELECT | PASS | admin/*-results.json; admin/pos-stock-ledger.json; admin/statistics-independent-sql.json |
| A-17 | Chặn tự khóa | Admin | Đăng nhập nhân viên fixture; thử tự vô hiệu hóa ở UI và POST | PASS | admin/*-results.json; admin/pos-stock-ledger.json; admin/statistics-independent-sql.json |
| A-18 | Phiếu giảm giá | Admin | Tạo/sửa/xóa phiếu giảm giá; SELECT điều kiện lưu | PASS | final/*-results.json; final/admin-orders-sql.json; final/quality-all-results.json |
| A-19 | Đợt giảm giá và chọn sản phẩm | Admin | Tạo/lưu đợt giảm giá; tìm và chọn sản phẩm; tải lại/SELECT; xóa | PASS | final/*-results.json; final/admin-orders-sql.json; final/quality-all-results.json |
| A-20 | Luồng đơn đến hoàn tất | Admin | Tạo COD hai chất liệu; chuyển xác nhận→chuẩn bị→đang giao→hoàn tất; SQL từng bước | PASS | final/*-results.json; final/admin-orders-sql.json; final/quality-all-results.json |
| A-21 | Hủy đơn | Admin | Tạo COD, xác nhận rồi hủy; SELECT hoàn kho | PASS | final/*-results.json; final/admin-orders-sql.json; final/quality-all-results.json |
| A-22 | Giao thất bại | Admin | Tạo COD, xác nhận→chuẩn bị→đang giao→giao thất bại; SELECT hoàn kho | PASS | final/*-results.json; final/admin-orders-sql.json; final/quality-all-results.json |
| A-23 | Xác nhận chuyển khoản | Admin | Tạo chuyển khoản Online, Admin xác nhận nhận tiền rồi hoàn tất; SELECT PAID | PASS | final/*-results.json; final/admin-orders-sql.json; final/quality-all-results.json |
| A-24 | Hóa đơn xem/in | Admin | Mở hóa đơn, kiểm tra chất liệu/số tiền; gọi print handler và render media print | PASS | admin/*-results.json; admin/pos-stock-ledger.json; admin/statistics-independent-sql.json |
| A-25 | POS tìm và chọn biến thể | Admin | Tìm mã sản phẩm POS; chọn chất liệu/kích cỡ trong dialog; thêm giỏ | PASS | admin/*-results.json; admin/pos-stock-ledger.json; admin/statistics-independent-sql.json |
| A-26 | POS khách lẻ/có tài khoản | Admin | Chọn khách lẻ cho tiền mặt và khách có tài khoản cho chuyển khoản | PASS | admin/*-results.json; admin/pos-stock-ledger.json; admin/statistics-independent-sql.json |
| A-27 | POS phiếu giảm giá | Admin | Áp dụng mã POS hợp lệ, kiểm tra giảm 5000 | PASS | admin/*-results.json; admin/pos-stock-ledger.json; admin/statistics-independent-sql.json |
| A-28 | POS tiền mặt/chuyển khoản | Admin | Thanh toán lần lượt tiền mặt/chuyển khoản, kiểm tra đơn và hóa đơn | PASS | admin/*-results.json; admin/pos-stock-ledger.json; admin/statistics-independent-sql.json |
| A-29 | POS thanh toán | Admin | SELECT tồn/log trước/sau từng lần POS thanh toán | PASS | admin/*-results.json; admin/pos-stock-ledger.json; admin/statistics-independent-sql.json |
| A-30 | Hóa đơn chờ lưu/mở/xóa | Admin | Lưu giỏ chờ, mở lại, kiểm tra ghi chú rồi xóa | PASS | admin/*-results.json; admin/pos-stock-ledger.json; admin/statistics-independent-sql.json |
| A-31 | Tối đa 5 hóa đơn chờ | Admin | Lưu năm hóa đơn chờ rồi thử thứ sáu, SELECT/count vẫn năm | PASS | admin/*-results.json; admin/pos-stock-ledger.json; admin/statistics-independent-sql.json |
| A-32 | Hóa đơn chờ hết hạn | Admin | Đặt đúng fixture hết hạn; kiểm tra biến mất khỏi danh sách, mở trả 404 | PASS | admin/*-results.json; admin/pos-stock-ledger.json; admin/statistics-independent-sql.json |
| A-33 | Thống kê mọi kênh/ngày/trang | Admin | Chạy ma trận URL kênh/ngày/trang và phân trang chéo; so SELECT độc lập | PASS | admin/*-results.json; admin/pos-stock-ledger.json; admin/statistics-independent-sql.json |
| A-34 | Tài khoản admin hồ sơ/mật khẩu | Admin | Đăng nhập hai nhân viên fixture, sửa hồ sơ/đổi mật khẩu; kiểm tra SQL và đăng nhập cũ/mới | PASS | final/*-results.json; final/admin-orders-sql.json; final/quality-all-results.json |
| S-01 | Luật kho SQL COD | Hệ thống | SELECT tồn/log trước đặt COD, xác nhận, hủy và hủy lại | PASS | user-security/browser-results.json; stock-before-after.json; all-route-security.json |
| S-02 | Luật kho SQL chuyển khoản | Hệ thống | SELECT tồn/log trước đặt chuyển khoản và sau hủy | PASS | user-security/browser-results.json; stock-before-after.json; all-route-security.json |
| S-03 | Luật kho SQL POS | Hệ thống | SELECT tồn/log trước/sau thanh toán POS | PASS | admin/*-results.json; admin/pos-stock-ledger.json; admin/statistics-independent-sql.json |
| S-04 | Thiếu hàng rollback | Hệ thống | So đơn/voucher/log/kho trước/sau checkout thiếu tồn | PASS | user-security/browser-results.json; stock-before-after.json; all-route-security.json |
| S-05 | Hai request đồng thời không âm | Hệ thống | Gửi đồng thời hai xác nhận khi còn một đôi, SELECT hai đơn và kho | PASS | user-security/browser-results.json; stock-before-after.json; all-route-security.json |
| S-06 | Ma trận quyền mọi route | Hệ thống | Lấy mappings Spring; gọi GET với Khách/User/Admin và ID thật | PASS | user-security/browser-results.json; stock-before-after.json; all-route-security.json |
| S-07 | CSRF thiếu/sai mọi POST | Hệ thống | Gửi mọi POST mapping/login/logout thiếu và sai CSRF ở ba vai trò | PASS | user-security/browser-results.json; stock-before-after.json; all-route-security.json |
| S-08 | Ownership dữ liệu User | Hệ thống | Thử đọc đơn, sửa/xóa/default địa chỉ, checkout bằng dữ liệu người khác; so SQL | PASS | user-security/browser-results.json; stock-before-after.json; all-route-security.json |
| S-09 | Unicode ghi/đọc | Hệ thống | Ghi rồi đọc đúng chuỗi Số 12, đường Nguyễn Huệ, Đà Nẵng bằng browser/SQL | PASS | user-security/browser-results.json; stock-before-after.json; all-route-security.json |
| S-10 | Không lỗi JS/404/trắng/tràn375 | Hệ thống | Crawl 92 lượt hai viewport và chạy lại 16 trang; ghi pageerror/status/scrollWidth | PASS | final/*-results.json; final/admin-orders-sql.json; final/quality-all-results.json |
| S-11 | Không tiếng Anh ngoài tên riêng | Hệ thống | Đọc nhãn trên trang và thông báo sau POST; giữ tên riêng, Việt hóa mã quyền/kênh hiển thị | PASS | final/*-results.json; final/admin-orders-sql.json; final/quality-all-results.json |
| S-12 | SQL count 8 trang trước/sau | Hệ thống | Đếm prepareStatement tám trang có ít nhất 14 sản phẩm bổ sung, trước/sau sửa | PASS | performance/before/counts.txt; performance/after/counts.txt; performance/performance-compare.json |
| S-13 | HTML/giá/thứ tự không đổi | Hệ thống | Lưu HTML trước/sau; thay riêng CSRF/checkoutToken; so từng ký tự | PASS | performance/before/counts.txt; performance/after/counts.txt; performance/performance-compare.json |
| S-14 | Cleanup dữ liệu kiểm thử cuối | Hệ thống | Dừng app, restore checkpoint, so 20 bảng/schema; xóa đúng chín ảnh không có tham chiếu | PASS | final/cleanup.json; final/after-final-cleanup.json; final/reset-final-cleanup.log |
