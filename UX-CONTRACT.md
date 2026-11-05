# UX Contract

## Product context

Cửa hàng giày Việt Nam; khách mua online, nhân viên dùng POS, Admin quản trị. Ngôn ngữ chức năng tiếng Việt có dấu; giữ tên riêng/thương hiệu. Ngày lịch Gregorian, thời gian nghiệp vụ Asia/Ho_Chi_Minh. Mục tiêu truy cập WCAG 2.2 AA; chưa tuyên bố đã chứng nhận toàn bộ website.

## Business-context sources

Yêu cầu Giai đoạn 5–8 của chủ project là nguồn quyết định. Báo cáo từng giai đoạn lưu trong docs/GIAI_DOAN_*.md; nguồn tỉnh hiện hành trong docs/NGUON_TINH_THANH_2026.md. OrderStatusService giữ quyền chuyển trạng thái; InventoryService giữ luật kho; không suy luận quyền từ nút giao diện. Không đổi enum/route/bảng cũ chỉ vì đổi chữ hiển thị.

## Visual contract

DESIGN.md ghi ý định giao diện. Runtime canonical là static/css/he-thong-thiet-ke.css, bổ sung Admin trong quan-tri.css. Dùng Bootstrap hiện có, không thêm bộ component mới. Không đổi token thương hiệu khi bổ sung giao hàng.

## Canonical UI Map

| Capability | Canonical owner | Source of truth | Allowed variants | Verification |
|---|---|---|---|---|
| Select/Listbox | static/js/combo-box.js | select trong Thymeleaf, ID server | authored searchable; creatable chỉ các thuộc tính được phép, tỉnh không creatable | Playwright popup/chọn địa chỉ/tỉnh ở1280/375 |
| Date | Trình bày server #temporals/DeliveryService | DATE snapshot trong orders | dd/MM/yyyy; các input date có sẵn giữ native của trình duyệt/hệ điều hành, không bổ sung date-picker ở Giai đoạn8 | unit ngày/tháng/năm/zone; browser bản in |
| Form | quan-tri-danh-muc.js, giao-dien-cua-hang.js và BindingResult | metadata field + validation service | novalidate, lỗi văn bản cạnh ô, form-associated inputs trong bảng dùng form.elements | test lỗi trường và POST giả mạo |
| Scrollbar | he-thong-thiet-ke.css: data-table-scroll, order-progress-scroll | khung cuộn của bảng/tiến trình | cuộn ngang trong khung, giữ toàn bộ cột | đo scrollWidth ở375 |
| Toast | fragments/thong-bao-quan-tri và flash của layout User | RedirectAttributes | success/warning/error theo kết quả server, không hiện bí mật | browser submit/flash |
| CRUD | Controller/service hiện có | phân quyền Spring Security và transaction service | lưu khách→chi tiết, lưu cấu hình→danh sách, chuyển trạng thái→chi tiết đơn | clean test và Playwright |

## Dataset navigation

Danh sách quản trị có STT, cột tách riêng, bộ lọc GET và phân trang12. PaginationAdvice giữ bộ lọc trong link. POS tìm khách là trạng thái riêng của giỏ: không nạp khi mở, từ2ký tự, debounce300ms, hủy request cũ, không đưa tên/SĐT khách vào URL trang chính.

## Flow ledger

| Operation | Trigger | Pending | Success destination | Success feedback | Failure recovery |
|---|---|---|---|---|---|
| Chuyển đơn online | Nút bước hợp lệ→dialog | khóa gửi lặp, aria-busy | chi tiết đơn | flash + tiến trình/lịch sử mới | lỗi trường hoặc flash, server không đổi bước/kho khi sai |
| Hủy/giao thất bại | Dialog có lý do bắt buộc | như chuyển bước | chi tiết đơn | lý do trong lịch sử | giữ ô lý do khi validation chưa đạt |
| Sửa tỉnh đơn | Combo bắt buộc→Lưu | shared form busy | chi tiết đơn | flash + lịch sử đổi tỉnh | server chặn sau giao cho vận chuyển |
| Sửa vùng | Ngày min/max nguyên1–30→Lưu | shared form busy | danh sách thời gian | flash, đơn đã xác nhận giữ snapshot | lỗi cạnh ô trước submit; server kiểm tra lại |
| Chọn địa chỉ checkout | Combo địa chỉ đã lưu | thay thông tin cục bộ | giữ checkout | họ tên/SĐT/chi tiết/tỉnh đồng bộ | được sửa tạm, không ghi sổ khi đặt |

## Navigation and responsive behavior

Trang mới dùng layout/menu/breadcrumb hiện có. Bảng và thanh tiến trình có khung cuộn riêng ở375px. Hóa đơn khách chỉ chủ đơn được xem; in A4 ẩn menu/nút, giữ người nhận, tỉnh và ngày giao; bảng đủ STT và thuộc tính riêng. Ngày cũ chưa lưu không được bịa ra.

## Overlays and feedback

Dialog chuyển đơn là biến thể nghiệp vụ của native dialog hiện có, dùng cùng field validation và trạng thái busy. ESC/Quay lại đóng và trả focus nút mở; lý do thiếu hiển thị cạnh ô, không dùng native validation bubble. Hủy/giao thất bại dùng nút viền nguy hiểm. Guard rời form chưa lưu tiếp tục thuộc quan-tri-danh-muc.js. Tiến trình có chữ và thời gian, không dùng màu làm dấu hiệu duy nhất.

## Async and resilience

Chuyển bước là pessimistic transaction có khóa đơn, gửi trạng thái đích; gửi lặp cùng trạng thái không trừ/hoàn kho hoặc ghi lịch sử lần nữa. Server quyết định bước hợp lệ. Form dùng shared offline feedback. Không tự xác nhận tiền từ QR. Camera thật, SMTP thật và ứng dụng ngân hàng thật có hướng dẫn thử riêng và không được suy từ test giả.

## Verification

Từng báo cáo ghi output test, HTTP, SQL, screenshots và mục NOT TESTED. Audit tĩnh của premium là kiểm tra bổ sung, không thay browser; các cảnh báo toàn repo có từ trước phải được ghi rõ, không mở rộng giai đoạn để sửa giao diện không liên quan.

## Địa chỉ và phí vận chuyển

Địa chỉ dùng fragment/service chung cho profile, checkout, khách hàng Admin và đơn online POS. Chỉ chọn trong dữ liệu hành chính; đổi tỉnh tải lại phường và hủy kết quả cũ. Ghim thủ công/GPS được giữ ưu tiên khi sửa địa chỉ; nút Dùng tâm phường/xã bỏ ghim để tính theo phường. Có thông báo rõ nguồn điểm và khoảng cách đường chim bay. Không nhận phí/khoảng cách từ client làm số tiền thanh toán. API bận/lỗi/timeout không chặn nhập tay; fallback phí có ghi chú.

GPS có trạng thái đang lấy, bị từ chối, chưa hỗ trợ, timeout. Bấm bản đồ hoặc Enter tại tâm thay thế thao tác kéo. Nút tạo đơn bận khi đang nhận báo phí; server vẫn xác nhận lại khi lưu. Trường hợp request lỗi giữ nội dung form và cho biết phí sẽ tính lại. Voucher thay đổi luôn bỏ kết quả giảm cũ cho đến khi áp dụng lại.

Đơn giao hàng tại POS là ONLINE; không coi là đã thu tiền chỉ vì có QR. QR theo mã đơn sau khi tạo, rồi nhân viên xác nhận tiền theo luồng cũ. Tin nhắn chỉ sao chép clipboard, không tự gửi mạng xã hội. Clipboard không được cấp quyền thì hiện nội dung để chọn/copy tay. Sổ địa chỉ giữ cơ chế mặc định/xóa và ba địa chỉ.

Nút cột bảng dùng fragment chung, icon aria-hidden kèm nhãn chữ ẩn dành cho trình đọc màn hình, title/aria-label tiếng Việt, cao 36px desktop và 40px mobile, vẫn dùng confirm và CSRF. MutationObserver chỉ trang trí trình bày cho dòng AJAX; không thay endpoint/quyền/nghiệp vụ. In hóa đơn có logo và số liệu snapshot, ẩn điều hướng/map.


## Không gian sản phẩm và biến thể (giai đoạn 9–10)

Menu hai cấp thuộc quan-tri.js và native details/summary; nhóm đang xem luôn mở. Trang biến thể riêng /admin/products-crud/{id}/variants giữ combo-box.js làm chủ sở hữu mọi select, gồm select gõ tạo mới. Các nút data-batch-open/data-batch-close và data-row-* thuộc sua-bien-the-san-pham.js, không phải nút thiếu hành vi; chuyển trang chỉ thay vị trí bảng, không tạo bộ điều khiển thứ hai. Lỗi cạnh ô/busy/CSRF/khóa tồn tiếp tục dùng cơ chế đã có.

Form ban đầu bố trí hai cột và dùng bien-the-ban-dau.js chỉ đếm lựa chọn; server xác nhận tổ hợp và transaction. Bộ ảnh vẫn do quan-ly-anh-san-pham.js quản lý. Liên kết bỏ qua nội dung chỉ hiển thị khi có focus, không che ô kích cỡ trong ảnh chụp trang dài.


## Khuyến mãi (giai đoạn 11)

quan-tri-giam-gia.js sở hữu data-discount-switch: POST CSRF, trạng thái bận, phản hồi lỗi và hoàn nguyên công tắc. Fragment cong-tac-khuyen-mai cung cấp markup chung; nhãn chữ phân biệt Sắp diễn ra/Đang chạy/Hết hạn/Tạm dừng. data-selection-all là hai nút có handler bật/tắt các dòng nhìn thấy; sessionStorage theo ID đợt giữ lựa chọn qua trang, chỉ xóa sau lưu thành công, lỗi lưu trữ yêu cầu lưu trước khi chuyển trang. Server xác nhận sản phẩm đang bán. Các select còn lại thuộc combo-box.js; validation form và guard dùng quan-tri-danh-muc.js. Phiếu không có hành động Xóa ở UI và endpoint luôn từ chối.


Giai đoạn 12: AdminAccess sở hữu quyền route/menu; ShippingAddressService sở hữu khóa và mặc định sổ khách. Fragment xu-ly-don-hang và theo-doi-don-hang.js dùng chung trên đơn/hóa đơn. nut-hanh-dong.js sở hữu icon bảng và data-icon-action (gửi lại tài khoản), giữ confirm/CSRF và 36px desktop/40px mobile.


Giai đoạn 13: AdminPosController dùng hai base route, key giỏ/draft theo loại; PosHoldService khóa user, hạn mức 5 mỗi loại, snapshot giao hàng không chứa checkoutToken. ShippingFeeService/PricingService/PosOnlineOrderService/OrderStatusService dùng chung. Khách chọn từ tìm kiếm 300ms; don-online-pos.js sở hữu chọn sổ và điền địa chỉ qua delivery:select-address của module chung. in-hoa-don.js sở hữu data-copy-confirmation (Clipboard API, lỗi chọn văn bản và hướng dẫn Ctrl+C) và data-print-invoice. Textarea readonly của tin nhắn dùng resize-none. Legacy AdminOrderController chỉ chuyển hướng, POST 307 giữ body/CSRF; server kiểm tra quyền lại ở endpoint hóa đơn.


Giai đoạn 14: ProductFeedbackService sở hữu điều kiện đơn DELIVERED/chủ tài khoản, một đánh giá mỗi khách/sản phẩm và giới hạn 5 lần ghi/phút. Khóa khách rồi refresh trước đọc bộ đếm vì advice có thể đã nạp entity; hai cột bộ đếm chỉ service ghi bằng SQL trong transaction, thao tác hồ sơ không ghi đè. ProductReview DynamicUpdate giữ cờ ẩn của Admin khi khách sửa. favorite toggle khóa khách, khóa DB duy nhất theo cặp; không đếm tổng trong transaction ghi. yeu-thich.js sở hữu nút trái tim (busy, CSRF, đồng bộ mọi thẻ trùng sản phẩm, lỗi giữ trạng thái); trang Yêu thích nạp lại sau bỏ để phân trang đúng. Guest đi qua GET /favorites/return được bảo vệ để Spring lưu trang quay lại, kiểm tra URL nội bộ ở server. Review DTO/Thymeleaf escape nội dung; không dùng innerHTML cho nội dung đánh giá. Map tổng hợp nạp gộp, fragment đọc bằng get(productId). Công tắc switch-field là biến thể của fragment/handler dùng cho khuyến mãi, nhãn Hiển thị riêng; không thêm widget chọn mới.

Giai đoạn 15: DashboardRepository và StatisticsService dùng chung điều kiện doanh thu RevenueCondition (PAID, loại CANCELLED/DELIVERY_FAILED, trừ phí vận chuyển khi shipping_policy_version>=1); tiền mặt (CASH, COD) + chuyển khoản (BANK_TRANSFER, TRANSFER) = tổng doanh thu kỳ. Bảng sắp hết hàng lọc qua EXISTS inventory_logs trong khoảng lọc. AdminStatisticsController sở hữu /admin/statistics với 3 tham số phân trang độc lập (quantityPage, revenuePage, customerPage) và chuyển hướng 302 cho /admin/statistics/revenue, /admin/statistics/best-sellers, /admin/reports/*. AdminAccess chặn ROLE_EMPLOYEE truy cập Dashboard và Thống kê (HTTP 403).


## Checkout — Giai đoạn 27

- `thanh-toan.js` sở hữu radio địa chỉ/thanh toán, dialog voucher, validation và busy của checkout; dùng `combo-box.js` và `dia-chi-giao-hang.js` cho địa chỉ phụ thuộc/định vị. Shared delivery phát `delivery:changed` ở checkout thay vì tự ghi tổng song song.
- `VoucherService.checkoutOptions` dùng cùng hiệu lực/công thức `calculate` với checkout/POS; chỉ hiện voucher RUNNING, công thức trần giảm và minimum dùng server. `ShippingPreviewService` vẫn tính tổng. `VietQrService` vẫn tạo payload.
- `CheckoutService` chỉ bọc lưu địa chỉ tùy chọn + `OrderService.placeOrder` trong cùng transaction; `ShippingAddressService` giữ giới hạn/khóa/chủ sở hữu. Không đổi sổ khi sửa tạm.
- POST cũ validate-voucher-ajax giữ nguyên; không bỏ route. API mới /checkout/vouchers (GET) và /checkout/preview (POST CSRF), mọi số tiền client bị bỏ qua. Không lưu tham chiếu QR xem trước vào đơn.
- Giỏ lỗi được giữ trên checkout với cảnh báo và khóa hai nút đặt; server giữ kiểm tra `PricingService`/`OrderService`.


## Hỗ trợ khách hàng — Giai đoạn 28

- `chatbot.js` sở hữu mở/đóng, focus, inert/scroll lock, bàn phím, outbox IndexedDB và STOMP/polling. Desktop là panel không modal, mobile là dialog modal. Escape đóng và trả focus cho nút nổi. Dialog chọn đơn dùng primitive native và combo dùng chung.
- `SupportChatService` sở hữu danh tính/chủ hội thoại, mã tin chống trùng, khóa/counter, hạn mức, masking và thẻ sản phẩm/đơn. Chỉ đọc giá từ PricingService, không ghi kho/đơn. Ảnh qua SupportChatImages tái sử dụng bộ kiểm tra ảnh hiện có, thư mục riêng không public.
- `SupportChatSocketConfig` sở hữu CSRF CONNECT và quyền SUBSCRIBE/SEND; `SupportChatEvents` chỉ phát sau commit. Payload realtime là tín hiệu, client lấy nội dung bằng API có kiểm tra quyền. `SupportChatAutomation` gọi engine ngoài transaction, kiểm tra lại việc nhân viên đã nhận trước khi lưu.
- REST là đường ghi bền vững; STOMP/SockJS là đường báo tin chính, mất kết nối dùng polling 5 giây. Cùng clientId được giữ trong IndexedDB qua reload/mất mạng. Lỗi tin có thử lại/bỏ, không mất draft; chuyển tài khoản không gửi nhầm outbox.
- Phiên ẩn danh dùng cookie HttpOnly/SameSite=Lax; DB chỉ lưu hash trong cột riêng, session_id cũ không dùng làm quyền. Khi đăng nhập gộp lịch sử/outbox vào hội thoại hiện tại, đóng rồi gửi lại mở cùng hội thoại.
- Đồng hồ server Asia/Ho_Chi_Minh; tự động 30 ngày, có nhân viên/hàng chờ 180 ngày cấu hình. Chỉ thông báo lưu cố định, thay checkbox đồng ý cũ. Màn nhận tin nhân viên thuộc giai đoạn 29.
