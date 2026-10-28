# Repository Guidelines

## Project Structure & Module Organization

This Java 17 Spring Boot application serves a shoe storefront and admin/POS workflows using Thymeleaf, Spring Security, JPA, and SQL Server.

- `src/main/java/com/example/demodatn/`: feature packages including `banhang` (sales), `sanpham` (products), `khachhang` (customers), `nhanvien` (employees), `khuyenmai` (promotions), and `thongke` (reporting); shared code lives in `core`, `config`, and `security`.
- `src/main/resources/templates/`: user/admin views and shared layouts/fragments. Edit these templates for application pages.
- `src/main/resources/static/`: CSS, JavaScript, and images.
- `src/test/java/`: tests mirroring application packages.
- `shopgiay.sql`: database schema and sample data. `target/` contains generated build output.

## Build, Test, and Development Commands

Use JDK 17 and the checked-in Maven wrapper. From PowerShell:

- `.\mvnw.cmd spring-boot:run`: start the application at `http://localhost:8080`; admin pages use `/admin`.
- `.\mvnw.cmd clean package`: run tests and build the application JAR in `target/`.
- `.\mvnw.cmd test`: run all automated tests.
- `.\mvnw.cmd "-Dtest=PricingServiceTest" test`: run isolated pricing unit tests.

On Unix-like systems, use `./mvnw` instead. Initialize a local SQL Server database using `shopgiay.sql` before running the application or context-loading tests.

## Coding Style & Naming Conventions

Use four-space Java indentation, PascalCase class names, and camelCase methods/fields. Retain lowercase feature package names and existing suffixes such as `Controller`, `Service`, `Repository`, and `Dto`. Keep business logic in services and persistence access in repositories. Follow surrounding Lombok and constructor-injection conventions. Preserve UTF-8 Vietnamese text. No dedicated formatter or lint plugin is configured; match nearby code and avoid unrelated formatting changes.

## Testing Guidelines

Tests use JUnit Jupiter and Mockito through Spring Boot Test. Name classes `*Test` or `*Tests` and methods after the behavior tested. Add regression coverage for pricing, voucher, stock, and checkout changes. No coverage threshold is configured. Root `test_*.ps1` scripts are manual integration helpers; inspect them before running because some modify database records and write local artifacts.

## Commit & Pull Request Guidelines

The limited history uses short task descriptions, such as `Complete Phase 4B Checkout`; no formal prefix convention is established. Use concise, action-oriented subjects. For PRs, describe changed behavior, link relevant issues, report test results, and include screenshots for UI changes. Call out schema or configuration changes.

## Security & Configuration

Override datasource settings through environment variables such as `SPRING_DATASOURCE_PASSWORD`. Do not add real credentials, session cookies, or generated test captures to commits. Schema auto-update is disabled; coordinate database changes with SQL scripts.

# Independent verification rules (current task)

1. KHÔNG SỬA CODE, template, CSS, JS, application.properties, DataInitializer. Chỉ được: tạo VERIFICATION_REPORT.md, tạo application-verify.properties, tạo thư mục verify/ chứa script test của bạn, tạo AGENTS.md. Nếu thấy lỗi thì ghi vào báo cáo, không tự sửa.
2. KHÔNG đụng database thật shopbangiay. Mọi test chạy trên database riêng shop_verify.
3. Mỗi hạng mục có đúng một trạng thái: VERIFIED (đã chạy và thấy đúng, có bằng chứng) | PARTIAL (có nhưng thiếu/sai một phần) | BROKEN (có nhưng chạy sai hoặc lỗi 4xx/5xx) | MISSING (không có trong code) | NOT TESTED (không test được; ghi rõ vì sao). Không có bằng chứng thì chỉ được ghi NOT TESTED. Không được ghi VERIFIED chỉ vì thấy code tồn tại.
4. Bằng chứng gồm: lệnh chạy, HTTP status thật, đoạn output ngắn, kết quả SQL (trước và sau nếu có đổi dữ liệu), tên file:dòng code. Không tóm tắt chung chung.
5. Test phá dữ liệu (đổi tồn kho, đặt hàng, hủy đơn...) chỉ chạy trên shop_verify.
6. Trên Windows dùng PowerShell và curl.exe (không dùng alias curl). Khi gọi POST có CSRF, tự lấy token từ form/cookie. Đăng nhập bằng cookie jar.
7. Chạy app ở chế độ nền, chờ log "Started ... in ... seconds" rồi mới test. Lệnh nào chạy quá 2 phút không có output thì dừng và báo. Khi build dùng mvnw clean package (KHÔNG dùng -DskipTests); ghi lại số test run/fail.
8. Ghi báo cáo vào VERIFICATION_REPORT.md và cập nhật sau từng phase, không đợi đến cuối. Lưu mục 1-8 này thành AGENTS.md ở thư mục gốc.


