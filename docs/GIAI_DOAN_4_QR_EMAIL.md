# Giai đoạn 4 — QR căn cước và gửi tài khoản qua email

Ngày kiểm thử: 29/10/2026 (Asia/Bangkok). Nhánh `gop-y-giang-vien`, nền `13e7481`. Chỉ thực hiện Giai đoạn 4; không push, không merge.

**Kết quả: 22 hạng mục PASS, 0 FAIL, 2 NOT TESTED** (camera vật lý và hộp thư thật do người dùng tự thử). Maven: **146 test, không bỏ qua test nào**. Bằng chứng trình duyệt cuối: **35 lượt**, gồm desktop 1280 và mobile 375; crawl 52 lượt trang chính. Bảng dưới phân biệt kết quả chạy thực tế với phần cần thử thủ công.

## Phạm vi database và hoàn nguyên

Chỉ kết nối database ứng dụng `shopbangiay_kt_20261029_gd4`; các lệnh RESTORE/BACKUP điều phối qua `master` chỉ nhắm bản sao này. Không kết nối, đọc, ghi hoặc chạy migration trên **shopbangiay** và **shopbangiay_thu**.

Bản sao được tạo bằng RESTORE file backup có sẵn `shopbangiay_kt_20261028_gd2_schema_16e6aa575bf04de99c5d3e5e1f29b32c.bak`, có guard DB chưa tồn tại, không dùng REPLACE ở lần tạo. Tên logical file `shopbangiay` trong output RESTORE là metadata trong backup, không phải kết nối database đó. Migration chỉ chạy trên bản sao. Sau 011 tạo checkpoint riêng, sau từng nhóm test phá dữ liệu RESTORE checkpoint của chính bản sao; không hoàn kho/xóa dữ liệu bằng phỏng đoán.

Ứng dụng chạy riêng cổng **18140**, SMTP giả **127.0.0.1:25254**, API receipt chỉ loopback **18142**, dữ liệu mail giữ trong RAM. Không thử camera vật lý hay gửi mail ra Internet. Bản sao đã được hoàn nguyên và các tiến trình test do đợt này tạo đã dừng.

Bằng chứng: [RESTORE mới](evidence/giai-doan-4/restore-new.txt), [log startup validate](evidence/giai-doan-4/startup-validate.txt), [metadata/count cuối](evidence/giai-doan-4/final-restored.json).

## Bảng nghiệm thu

| Hạng mục | Cách kiểm tra | Kết quả thực tế | PASS/FAIL/NOT TESTED |
|---|---|---|---|
| R01 · Nhánh và database | git branch, RESTORE mới, guard DB | gop-y-giang-vien; bản sao riêng shopbangiay_kt_20261029_gd4; RESTORE ban đầu không REPLACE | PASS |
| R02 · Migration 011 | Chạy hai lần, COUNT/metadata trước-sau | 21 bảng; 152 → 163 cột; COUNT không đổi; lần hai thành công | PASS |
| R03 · Script gộp và bootstrap | chay_tat_ca 004–011 hai lần; đối chiếu nguồn shopgiay.sql | Hai lần thành công, COUNT không đổi; shopgiay.sql đã bổ sung cùng 011 | PASS |
| R04 · Schema validate | Khởi động app, chờ Started, Hibernate validate | App cổng 18140 Started; EntityManagerFactory khởi tạo; không lỗi schema | PASS |
| R05 · Chức vụ | Combo mặc định và gõ chức vụ mới | Có 5 gợi ý; lưu Giám sát ca giả / Chuyên viên kiểm thử | PASS |
| R06 · QR hai biểu mẫu | Playwright tải ảnh QR ở 1280/375 | Khách hàng và nhân viên điền đúng 5 trường, giữ SĐT/email nhập tay | PASS |
| R07 · Định dạng và ngày QR | 7 trường, 6 thiếu CMND cũ, 6 thiếu ngày cấp; QR sai/ngày 31-02 | 3 định dạng hợp lệ đọc được; QR sai không đổi trường, báo đúng thông báo | PASS |
| R08 · Riêng tư ảnh và camera lỗi | Theo dõi POST; mô phỏng NotAllowedError rồi tải ảnh | 0 POST ảnh; thông báo thân thiện; tải ảnh tiếp tục hoạt động | PASS |
| R09 · Unicode và sửa trước lưu | Form thật, đổi địa chỉ trước lưu; SELECT | Nguyễn Văn Ánh; Số 12, đường Nguyễn Huệ, Đà Nẵng đọc lại nguyên vẹn | PASS |
| R10 · CCCD và bản in | POST trùng/sai; SQL duplicate; danh sách, chi tiết và print media | Lỗi rõ ràng; DB chặn 2601; danh sách ***1234; Admin đọc đủ; hóa đơn/đơn/bản in không chứa CCCD giả | PASS |
| R11 · Email/tên đăng nhập | POST email sai/trùng, username admin trùng; gợi ý admin@... | Báo lỗi cạnh trường; gợi ý admin1; cho sửa username | PASS |
| R12 · Cấp tài khoản | Form thật + SELECT + đọc thư GreenMail | Tạo ROLE_USER hoặc quyền nhân viên hiện có; mật khẩu tạm 12 ký tự, BCrypt, must_change_password=1 | PASS |
| R13 · SMTP sau commit | Test tích hợp gửi thư thật vào GreenMail; guard transaction trong sender | Thư nhận được; sender không chạy khi transaction database còn hoạt động | PASS |
| R14 · SMTP lỗi | Tắt SMTP, tạo khách/nhân viên bằng form | Tài khoản vẫn tồn tại; Chưa gửi được email; nút gửi lại hiện | PASS |
| R15 · Gửi lại và giới hạn | Bật SMTP, gửi lại; test 3 lần/giờ và lần thứ 4 | Đổi BCrypt, mật khẩu cũ mất hiệu lực; lần thứ 4 bị chặn, hash giữ nguyên | PASS |
| R16 · Hồ sơ cũ và xác nhận | Khách chưa tài khoản; tài khoản khóa/đã đổi mật khẩu | Tạo tài khoản được; cần xác nhận khi khóa/đã đổi; không tự mở khóa | PASS |
| R17 · Đổi mật khẩu bắt buộc | Đăng nhập bằng mật khẩu thư; GET /, /profile; POST hồ sơ; đổi qua form | Chặn GET 302, POST 303; đổi thành công; vào trang đúng quyền | PASS |
| R18 · Toàn bộ Maven test | mvnw.cmd clean test, không skip | 146 test; 0 Failures; 0 Errors; 0 Skipped; BUILD SUCCESS | PASS |
| R19 · Bảo mật | 8 nhóm kịch bản bên dưới, CSRF 5 POST mới/đã đổi | Guest/user/admin đúng quyền; CSRF thiếu/sai 403; truy cập chéo bị chặn | PASS |
| R20 · Crawl hồi quy | 26 URL-vai trò × 2 kích thước | 52 lượt trang chính; không 500/trang trắng; checkout giỏ rỗng chuyển /cart | PASS |
| R21 · Chất lượng UI và log | Console, pageerror, request, kích thước; so mật khẩu thực nhận với log | Các lượt cuối không lỗi JS/console, không tài nguyên 404, không tràn 375px; mẫu mật khẩu kiểm tra không khớp log | PASS |
| R22 · Hoàn nguyên bản sao | RESTORE checkpoint sau từng nhóm phá dữ liệu; so COUNT và metadata cuối | Trở về 14 khách, 5 nhân viên, 12 tài khoản; 21 bảng khớp checkpoint | PASS |
| R23 · Camera thật | Cần thiết bị/camera và quyền do người dùng cấp | Chỉ mô phỏng camera bị từ chối, chưa quét bằng camera vật lý; hướng dẫn trong README | NOT TESTED |
| R24 · Hộp thư thật | Cần SMTP và hộp thư do người dùng kiểm soát | Chỉ dùng GreenMail loopback; chưa gửi thư ra ngoài; hướng dẫn trong README | NOT TESTED |

## SQL trước/sau

Các snapshot dùng guard `DB_NAME()` trước mọi truy vấn. Query COUNT tương đương `SELECT COUNT_BIG(*) FROM dbo.[tên_bảng]` cho toàn bộ bảng từ `sys.tables`. Metadata dùng `sys.columns`, `TYPE_NAME`, `max_length`, `is_nullable`, `collation_name`.

| Bảng | Trước 011 | 011 lần 1 | 011 lần 2 | Sau nhóm UI đầy đủ | Cuối sau RESTORE |
|---|---:|---:|---:|---:|---:|
| categories | 6 | 6 | 6 | 6 | 6 |
| chat_lieu | 8 | 8 | 8 | 8 | 8 |
| customers | 14 | 14 | 14 | 17 | 14 |
| de_giay | 8 | 8 | 8 | 8 | 8 |
| dia_chi_nhan_hang | 7 | 7 | 7 | 7 | 7 |
| employees | 5 | 5 | 5 | 8 | 5 |
| inventory_logs | 63 | 63 | 63 | 63 | 63 |
| invoices | 28 | 28 | 28 | 28 | 28 |
| order_details | 35 | 35 | 35 | 35 | 35 |
| orders | 28 | 28 | 28 | 28 | 28 |
| pos_held_items | 0 | 0 | 0 | 0 | 0 |
| pos_held_orders | 0 | 0 | 0 | 0 | 0 |
| product_images | 6 | 6 | 6 | 6 | 6 |
| product_variants | 20 | 20 | 20 | 20 | 20 |
| products | 10 | 10 | 10 | 10 | 10 |
| promotion_details | 7 | 7 | 7 | 7 | 7 |
| promotions | 5 | 5 | 5 | 5 | 5 |
| roles | 2 | 2 | 2 | 2 | 2 |
| sizes | 8 | 8 | 8 | 8 | 8 |
| users | 12 | 12 | 12 | 18 | 12 |
| vouchers | 15 | 15 | 15 | 15 | 15 |

Nguồn thô: [trước 011](evidence/giai-doan-4/before-011.json), [011 lần 1](evidence/giai-doan-4/after-011-first.json), [011 lần 2](evidence/giai-doan-4/after-011-second.json), [sau UI](evidence/giai-doan-4/after-ui-all.json), [cuối](evidence/giai-doan-4/final-restored.json). COUNT của script gộp [lần 1](evidence/giai-doan-4/aggregate-first.json) và [lần 2](evidence/giai-doan-4/aggregate-second.json) cũng bằng nhau.

SQL đối chiếu hồ sơ mới (tên bảng chỉ nhận `customers`/`employees`; không xuất mật khẩu/hash hoặc CCCD đủ vào báo cáo):

```sql
SELECT p.id, p.full_name, p.address, RIGHT(p.so_cccd,4) cccd_last4,
       CONVERT(VARCHAR(10),p.ngay_sinh,23) ngay_sinh, p.gioi_tinh,
       u.username, u.must_change_password, u.credential_mail_failed,
       CASE WHEN u.password LIKE '$2%' THEN 1 ELSE 0 END bcrypt
FROM customers p JOIN users u ON u.id=p.user_id
WHERE p.id=@id;
-- Chạy tương tự với employees.
```

Kết quả thật của bốn form đầu ở hai kích thước được lưu trong [ui-created.json](evidence/giai-doan-4/ui-created.json): họ tên `Nguyễn Văn Ánh`, DOB `1995-01-01`, giới tính `Nam`, địa chỉ sửa tay `Số 12, đường Nguyễn Huệ, Đà Nẵng`, `must_change_password=true`, `credential_mail_failed=false`, `bcrypt=1`. Dữ liệu này hoàn toàn giả.

Ràng buộc trùng CCCD được thử UPDATE trong transaction có ROLLBACK; [output](evidence/giai-doan-4/db-unique-proof.json): `[{"duplicate_error":2601}]`. Không xuất thông báo SQL chứa số CCCD đầy đủ. SMTP lỗi rồi gửi lại: [SQL và status thực tế](evidence/giai-doan-4/smtp-failure.json); sau gửi lại `credential_mail_count=1`, `credential_mail_failed=false`.

## Các bước Playwright và kết quả

Dữ liệu QR GIẢ dùng để tạo PNG rồi tải qua input file trong dialog:

```text
001234567890|123456789|Nguyễn Văn Ánh|01011995|Nam|Số 12, đường Lê Lợi, Đà Nẵng|01012021
```

Không dùng ảnh căn cước thật. Kiểm tra thêm bản 6 trường thiếu CMND cũ, bản 6 trường thiếu ngày cấp, QR là URL và ngày `31021995`. Payload sai báo đúng **Mã QR không phải căn cước công dân**, không điền gì.

| Mã | Chức năng | Vai trò | Các bước | Kết quả | Bằng chứng |
|---|---|---|---|---|---|
| PW-01 | login-admin, 1280px | guest | Thao tác theo proof, lưu request/status và ảnh chụp | PASS | playwright-final.json: id=login-admin, width=1280 |
| PW-02 | qr-customers, 1280px | admin | Thao tác theo proof, lưu request/status và ảnh chụp | PASS | playwright-final.json: id=qr-customers, width=1280 |
| PW-03 | qr-employees, 1280px | admin | Thao tác theo proof, lưu request/status và ảnh chụp | PASS | playwright-final.json: id=qr-employees, width=1280 |
| PW-04 | qr-customers, 375px | admin | Thao tác theo proof, lưu request/status và ảnh chụp | PASS | playwright-final.json: id=qr-customers, width=375 |
| PW-05 | qr-employees, 375px | admin | Thao tác theo proof, lưu request/status và ảnh chụp | PASS | playwright-final.json: id=qr-employees, width=375 |
| PW-06 | create-customers, 1280px | admin | Thao tác theo proof, lưu request/status và ảnh chụp | PASS | playwright-final.json: id=create-customers, width=1280 |
| PW-07 | create-employees, 1280px | admin | Thao tác theo proof, lưu request/status và ảnh chụp | PASS | playwright-final.json: id=create-employees, width=1280 |
| PW-08 | create-customers, 375px | admin | Thao tác theo proof, lưu request/status và ảnh chụp | PASS | playwright-final.json: id=create-customers, width=375 |
| PW-09 | create-employees, 375px | admin | Thao tác theo proof, lưu request/status và ảnh chụp | PASS | playwright-final.json: id=create-employees, width=375 |
| PW-10 | first-login-customers, 1280px | guest | Thao tác theo proof, lưu request/status và ảnh chụp | PASS | playwright-final.json: id=first-login-customers, width=1280 |
| PW-11 | first-login-employees, 1280px | guest | Thao tác theo proof, lưu request/status và ảnh chụp | PASS | playwright-final.json: id=first-login-employees, width=1280 |
| PW-12 | first-login-customers, 375px | guest | Thao tác theo proof, lưu request/status và ảnh chụp | PASS | playwright-final.json: id=first-login-customers, width=375 |
| PW-13 | first-login-employees, 375px | guest | Thao tác theo proof, lưu request/status và ảnh chụp | PASS | playwright-final.json: id=first-login-employees, width=375 |
| PW-14 | validation-privacy, 1280px | admin | Thao tác theo proof, lưu request/status và ảnh chụp | PASS | playwright-final.json: id=validation-privacy, width=1280 |
| PW-15 | validation-privacy, 375px | admin | Thao tác theo proof, lưu request/status và ảnh chụp | PASS | playwright-final.json: id=validation-privacy, width=375 |
| PW-16 | crawl-guest, 1280px | guest | Đi từng URL trong proof, kiểm status/nội dung/kích thước | PASS | playwright-final.json: id=crawl-guest, width=1280 |
| PW-17 | crawl-guest, 375px | guest | Đi từng URL trong proof, kiểm status/nội dung/kích thước | PASS | playwright-final.json: id=crawl-guest, width=375 |
| PW-18 | crawl-user, 1280px | user | Đi từng URL trong proof, kiểm status/nội dung/kích thước | PASS | playwright-final.json: id=crawl-user, width=1280 |
| PW-19 | crawl-user, 375px | user | Đi từng URL trong proof, kiểm status/nội dung/kích thước | PASS | playwright-final.json: id=crawl-user, width=375 |
| PW-20 | crawl-admin, 1280px | admin | Đi từng URL trong proof, kiểm status/nội dung/kích thước | PASS | playwright-final.json: id=crawl-admin, width=1280 |
| PW-21 | crawl-admin, 375px | admin | Đi từng URL trong proof, kiểm status/nội dung/kích thước | PASS | playwright-final.json: id=crawl-admin, width=375 |
| PW-22 | security-guest, 1280px | guest | Thao tác theo proof, lưu request/status và ảnh chụp | PASS | playwright-final.json: id=security-guest, width=1280 |
| PW-23 | security-user, 1280px | user | Thao tác theo proof, lưu request/status và ảnh chụp | PASS | playwright-final.json: id=security-user, width=1280 |
| PW-24 | security-admin, 1280px | admin | Thao tác theo proof, lưu request/status và ảnh chụp | PASS | playwright-final.json: id=security-admin, width=1280 |
| PW-25 | resend-confirmation, 1280px | admin | Thao tác theo proof, lưu request/status và ảnh chụp | PASS | playwright-final.json: id=resend-confirmation, width=1280 |
| PW-26 | smtp-failed-customers, 1280px | admin | Thao tác theo proof, lưu request/status và ảnh chụp | PASS | playwright-final.json: id=smtp-failed-customers, width=1280 |
| PW-27 | smtp-failed-employees, 375px | admin | Thao tác theo proof, lưu request/status và ảnh chụp | PASS | playwright-final.json: id=smtp-failed-employees, width=375 |
| PW-28 | smtp-retry-customers, 1280px | admin | Thao tác theo proof, lưu request/status và ảnh chụp | PASS | playwright-final.json: id=smtp-retry-customers, width=1280 |
| PW-29 | smtp-retry-employees, 375px | admin | Thao tác theo proof, lưu request/status và ảnh chụp | PASS | playwright-final.json: id=smtp-retry-employees, width=375 |
| PW-30 | qr-dialog-final, 1280px | admin | Thao tác theo proof, lưu request/status và ảnh chụp | PASS | playwright-final.json: id=qr-dialog-final, width=1280 |
| PW-31 | qr-dialog-final, 375px | admin | Thao tác theo proof, lưu request/status và ảnh chụp | PASS | playwright-final.json: id=qr-dialog-final, width=375 |
| PW-32 | print-privacy, 1280px | admin | Thao tác theo proof, lưu request/status và ảnh chụp | PASS | playwright-final.json: id=print-privacy, width=1280 |
| PW-33 | print-privacy, 375px | admin | Thao tác theo proof, lưu request/status và ảnh chụp | PASS | playwright-final.json: id=print-privacy, width=375 |
| PW-34 | validation-final, 1280px | admin | Thao tác theo proof, lưu request/status và ảnh chụp | PASS | playwright-final.json: id=validation-final, width=1280 |
| PW-35 | validation-final, 375px | admin | Thao tác theo proof, lưu request/status và ảnh chụp | PASS | playwright-final.json: id=validation-final, width=375 |

[Raw request/status, console, pageerror, proof và kích thước cuối](evidence/giai-doan-4/playwright-final.json). Những lần harness chọn sai ID sản phẩm ẩn, sai nhãn lựa chọn hoặc xử lý form chưa lưu được sửa trong harness và chạy lại; không tính các lần đó là lỗi website hay PASS cuối. Lỗi website phát hiện trong quá trình triển khai được ghi riêng bên dưới.

Ảnh: [QR desktop](evidence/giai-doan-4/gd4-qr-dialog-final-1280.png), [QR mobile](evidence/giai-doan-4/gd4-qr-dialog-final-375.png), [đổi mật khẩu mobile](evidence/giai-doan-4/gd4-forced-password-g4fail375.png), [nhân viên mới](evidence/giai-doan-4/gd4-create-employees-1280.png), [SMTP lỗi mobile](evidence/giai-doan-4/gd4-smtp-failed-employees-375.png), [gửi lại thành công](evidence/giai-doan-4/gd4-smtp-retry-customers-1280.png).

## URL chính và status cuối

Status 302 → 200 thể hiện chuyển hướng hợp lệ; đối chiếu từng response gốc trong JSON. Không coi trang 404 của ID sản phẩm ẩn là sản phẩm đang bán.

| URL | Vai trò | Status | Kết quả |
|---|---|---|---|
| `/` | guest | 200 | PASS; đích `/`, cũng chạy 375px |
| `/products` | guest | 200 | PASS; đích `/products`, cũng chạy 375px |
| `/products/2` | guest | 200 | PASS; đích `/products/2`, cũng chạy 375px |
| `/category/1` | guest | 200 | PASS; đích `/category/1`, cũng chạy 375px |
| `/cart` | guest | 200 | PASS; đích `/cart`, cũng chạy 375px |
| `/login` | guest | 200 | PASS; đích `/login`, cũng chạy 375px |
| `/register` | guest | 200 | PASS; đích `/register`, cũng chạy 375px |
| `/profile` | user | 200 | PASS; đích `/profile`, cũng chạy 375px |
| `/cart` | user | 200 | PASS; đích `/cart`, cũng chạy 375px |
| `/checkout` | user | 302 → 200 | PASS; đích `/cart`, cũng chạy 375px |
| `/orders` | user | 200 | PASS; đích `/orders`, cũng chạy 375px |
| `/admin` | admin | 200 | PASS; đích `/admin`, cũng chạy 375px |
| `/admin/customers` | admin | 200 | PASS; đích `/admin/customers`, cũng chạy 375px |
| `/admin/customers/create` | admin | 200 | PASS; đích `/admin/customers/create`, cũng chạy 375px |
| `/admin/employees` | admin | 200 | PASS; đích `/admin/employees`, cũng chạy 375px |
| `/admin/employees/create` | admin | 200 | PASS; đích `/admin/employees/create`, cũng chạy 375px |
| `/admin/products-crud` | admin | 200 | PASS; đích `/admin/products-crud`, cũng chạy 375px |
| `/admin/variants` | admin | 200 | PASS; đích `/admin/variants`, cũng chạy 375px |
| `/admin/materials` | admin | 200 | PASS; đích `/admin/materials`, cũng chạy 375px |
| `/admin/categories` | admin | 200 | PASS; đích `/admin/categories`, cũng chạy 375px |
| `/admin/soles` | admin | 200 | PASS; đích `/admin/soles`, cũng chạy 375px |
| `/admin/inventory` | admin | 200 | PASS; đích `/admin/inventory`, cũng chạy 375px |
| `/admin/pos` | admin | 200 | PASS; đích `/admin/pos`, cũng chạy 375px |
| `/admin/statistics` | admin | 200 | PASS; đích `/admin/statistics`, cũng chạy 375px |
| `/admin/statistics/revenue` | admin | 200 | PASS; đích `/admin/statistics/revenue`, cũng chạy 375px |
| `/admin/statistics/best-sellers` | admin | 200 | PASS; đích `/admin/statistics/best-sellers`, cũng chạy 375px |

## Tám nhóm bảo mật

| Mã | Kịch bản | Kết quả thực tế | Trạng thái |
|---|---|---|---|
| SEC01 | Guest GET trang tạo Admin | 302 tới đăng nhập | PASS |
| SEC02 | User GET trang tạo Admin | 403 | PASS |
| SEC03 | Guest POST Admin với CSRF hợp lệ | 302, không tạo/sửa | PASS |
| SEC04 | User POST Admin với CSRF hợp lệ | 403 | PASS |
| SEC05 | Admin POST thiếu CSRF | 403 ở save customer, save employee, hai send-account và username-suggestion | PASS |
| SEC06 | Admin POST CSRF sai | 403 ở cùng năm route | PASS |
| SEC07 | Tài khoản phải đổi mật khẩu tìm cách bỏ qua | GET / và /profile → 302; POST hồ sơ → 303; không ghi dữ liệu | PASS |
| SEC08 | User đọc đơn/sửa địa chỉ người khác | 403, không đọc/sửa được | PASS |

Proof: `security-guest`, `security-user`, `security-admin`, `first-login-*` trong JSON; test tích hợp cũng kiểm thiếu/sai CSRF và quyền. Mọi form POST mới dùng `th:action` để Spring thêm token; fetch gợi ý username gửi token cùng body, không đưa CCCD hay mật khẩu vào URL.

## Thư nhận thật trong GreenMail

Nội dung dưới được lấy từ receipt SMTP thực tế, chỉ che giá trị mật khẩu tạm khi ghi bằng chứng. Bản rõ chỉ ở RAM của sender/SMTP giả; test đối chiếu độ dài 12, PasswordPolicy và BCrypt, không ghi ra log/flash/URL/database dạng rõ.

```text
Xin chào Nguyễn Văn Ánh,

Tài khoản của bạn đã sẵn sàng.
Tên đăng nhập: g4c1280
Mật khẩu tạm: [ĐÃ CHE]
Đăng nhập: http://localhost:18140/login

Bạn phải đổi mật khẩu ngay sau lần đăng nhập đầu tiên. Không chia sẻ mật khẩu này.
Nếu đây là thư gửi lại, mật khẩu tạm trước đó đã hết hiệu lực.

Trân trọng,
F6S Store
```

Sender từ chối chạy khi có transaction đang hoạt động. Lưu tài khoản xong mới gọi SMTP; lỗi SMTP trả false, lưu cờ thất bại và cảnh báo; gửi lại tạo BCrypt mới, khóa dòng user để đếm tối đa 3 lần/giờ, không tự kích hoạt tài khoản bị khóa. Metadata gửi mail trong users không chứa mật khẩu rõ. Giữ API PeopleService cũ cho các luồng nội bộ/test hiện có; hai form Admin dùng AccountProvisioningService sinh mật khẩu phía server, bỏ input password.

Quét log thực tế: [Mẫu SMTP kiểm thử bảo mật](evidence/giai-doan-4/log-secret-scan.json), [mẫu SMTP gửi lại](evidence/giai-doan-4/final-log-privacy.json), [mẫu chạy lại sau sửa regex](evidence/giai-doan-4/retest-log-privacy.json). Không tìm thấy chuỗi mật khẩu thực nhận được trong các file đã quét; log app không có chuỗi CCCD 12 số. `mail.debug=false`, `show-sql=false`; converter che CCCD trong message/stacktrace, không log nội dung mail. Không công bố log SMTP kèm credential fixture hoặc receipt dạng rõ.

## Lỗi phát hiện và đã sửa trong đợt này

| Mã | File:dòng | Nguyên nhân | Sửa | Bằng chứng chạy lại |
|---|---|---|---|---|
| BUG4-01 | src/main/resources/templates/admin/khach-hang/chi-tiet.html:7–9 | Chèn fragment gửi lại vào cả section con và section ngoài, xuất hiện 2 nút | Chỉ render một fragment ở cuối trang | resend-confirmation và print-privacy: count nút=1, gửi lại thành công |
| BUG4-02 | src/main/java/com/example/demodatn/security/AccountStatusFilter.java:29 | servletPath rỗng trong một số dispatch/test làm redirect lặp trang bắt buộc đổi | So request URI sau context path | Test POST đổi mật khẩu và first-login-* đều PASS |
| BUG4-03 | src/main/java/com/example/demodatn/service/nhanvien/AccountProvisioningService.java:139 | Hồ sơ cũ bị khóa chưa có user có thể bỏ qua xác nhận vì user mới mặc định bật | Kiểm tra thêm trạng thái hồ sơ trước khi cấp/gửi lại | existingCustomerWithoutAccountCanReceiveCredentialsAndDisabledNeedsConfirmation PASS, vẫn khóa |
| BUG4-04 | src/main/resources/templates/admin/khach-hang/bieu-mau.html:14 | Pattern HTML username có dấu - chưa escape; Chromium regex v báo Invalid character in character class | Escape thành \- giống form nhân viên | Chạy lại create-customers 1280/375, smtp-failed-customers; console/pageerror đều rỗng |

CCCD trùng hiện báo riêng **Số căn cước đã tồn tại.**, sai định dạng báo **Số căn cước phải gồm đúng 12 chữ số.**; `validation-final` chứng minh cả hai form. Không đổi bảng/lớp Java/route/enum hiện có, không đổi quyền nhân viên ngoài cấu hình hiện tại.

## Output Maven

```text
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.001 s -- in com.example.demodatn.util.PasswordPolicyTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 146, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  34.677 s
[INFO] Finished at: 2026-10-29T09:32:33+07:00
[INFO] ------------------------------------------------------------------------
```

[Output đầy đủ](evidence/giai-doan-4/clean-test.txt), [33 suite và số test](evidence/giai-doan-4/junit-suites.json). Không dùng skip trong lệnh nghiệm thu.

## Migration cần triển khai thủ công

Database đã có 004–010: chạy **011_can_cuoc_tai_khoan_email.sql**. Database cũ hơn: chạy đúng thứ tự **004 → 005 → 006 → 007 → 008 → 009 → 010 → 011**, có thể dùng `database/migrations/chay_tat_ca.sql`. **Không chạy 003** trong quy trình nâng cấp. `shopgiay.sql` dành cho database rỗng; đã đồng bộ 011, không dùng để nâng cấp database có dữ liệu.

Các script có guard DB_NAME; database triển khai tên shopbangiay cần opt-in SESSION_CONTEXT trong chính phiên sqlcmd. Ví dụ PowerShell dưới chỉ là hướng dẫn, **chưa chạy trên database thật**; thay server/database và dùng tài khoản Windows có quyền tương ứng, backup trước khi người vận hành chạy:

```powershell
@"
EXEC sys.sp_set_session_context @key=N'shop.allow-production-migration', @value=1;
GO
:r database/migrations/011_can_cuoc_tai_khoan_email.sql
"@ | sqlcmd -S "<SERVER>" -d "<DATABASE_TRIEN_KHAI>" -E -I -b -f 65001
```

Nếu cần toàn bộ 004–011, thay dòng `:r` bằng `:r database/migrations/chay_tat_ca.sql`. Chọn đúng DB bằng `-d`, không sửa script để tự USE database. Sau đó chạy app với `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`; phải có log Started sạch trước khi mở cho người dùng.

Cấu hình mail và cách thử camera/hộp thư thật ở [README](../README.md#quét-căn-cước-và-gửi-tài-khoản--giai-đoạn-4). [Thư viện, phiên bản, giấy phép và checksum](THIRD_PARTY_GIAI_DOAN_4.md). Dừng ở Giai đoạn 4, không merge.
