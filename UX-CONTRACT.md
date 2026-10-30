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

Nút cột bảng dùng class chung, icon aria-hidden kèm chữ, cao ≥36px, vẫn dùng confirm và CSRF. MutationObserver chỉ trang trí trình bày cho dòng AJAX; không thay endpoint/quyền/nghiệp vụ. In hóa đơn có logo và số liệu snapshot, ẩn điều hướng/map.
