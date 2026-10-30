# Thư viện QR chạy cục bộ

`src/main/resources/static/js/vendor/qrcodegen-1.8.0.js`: QR Code generator của Project Nayuki, phiên bản **1.8.0**, giấy phép **MIT** (giữ nguyên thông báo bản quyền và toàn văn giấy phép ở đầu file).

Nguồn chính thức: [tag v1.8.0](https://github.com/nayuki/QR-Code-generator/tree/v1.8.0/typescript-javascript), [qrcodegen.ts](https://github.com/nayuki/QR-Code-generator/blob/v1.8.0/typescript-javascript/qrcodegen.ts). Bản JavaScript được biên dịch từ nguồn của tag bằng TypeScript 5.7.3: `tsc qrcodegen.ts --target es2015 --outFile qrcodegen-1.8.0.js`.

SHA-256: `6E200897A80FF15A652A5D601667856D807CEC60CFC81A3A351305D69CC9DBBD`.

Thư viện vẽ canvas có viền trắng bốn mô-đun, mức sửa lỗi MEDIUM. Không dùng CDN, không gửi dữ liệu thanh toán tới dịch vụ bên ngoài. Payload được tạo ở server; trình duyệt chỉ vẽ ảnh. Kiểm thử dùng jsQR 1.4.0 đã có trong vendor để giải mã ảnh PNG tải vào trình duyệt.
