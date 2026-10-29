# Cửa hàng giày

Ứng dụng Java 17, Spring Boot, Thymeleaf, Spring Security, JPA và SQL Server; gồm cửa hàng trực tuyến, quản trị, POS và thống kê.

## Chuẩn bị

JDK 17, Maven Wrapper trong repository, SQL Server và tài khoản có quyền trên database đích. Trên Windows dùng PowerShell và `curl.exe`. Node/Playwright chỉ phục vụ kiểm thử trình duyệt.

Không chạy kiểm thử trên `shopbangiay`. Dùng `shop_verify`; diễn tập migration trên bản sao riêng. Sao lưu database và thư mục upload trước triển khai.

## Khởi tạo database thử mới

Tạo `shop_verify` bằng SSMS. Chạy `shopgiay.sql` với database đích đã chọn; script chứa schema hiện tại và chặn database ngoài danh sách cho phép. Không chạy trên database đã có bảng `users`. Script không seed khách, sản phẩm, đơn hàng mẫu; ứng dụng khởi tạo vai trò, quản trị viên và thương hiệu cơ bản khi chưa tồn tại.

```powershell
sqlcmd -S 'localhost,1433' -E -C -I -b -d shop_verify -i shopgiay.sql
```

Với database hiện có, làm theo [hướng dẫn triển khai](docs/HUONG_DAN_TRIEN_KHAI.md): kiểm kê trước, migration **004 → 005 → 006 → 007 → 008 → 009 → 010 → 011 → 012**, đối chiếu dữ liệu sau. **003 xóa dữ liệu nghiệp vụ**, không thuộc quy trình nâng cấp thông thường.

## Cấu hình và chạy

Xem `application-example.properties`. Cung cấp đủ URL, tài khoản và mật khẩu datasource qua biến môi trường hoặc cấu hình local; không có mật khẩu thật mặc định trong cấu hình nguồn. Không commit file cấu hình cục bộ.

Nếu chạy nút Run trong IntelliJ: tạo `application-local.properties` từ file mẫu, điền kết nối `shop_verify` và mật khẩu SQL trên máy, chọn working directory là thư mục gốc dự án. Ứng dụng tự đọc file này; file đã được gitignore. Bạn cũng có thể đặt ba biến môi trường datasource trực tiếp trong Run Configuration. Cổng local là `http://localhost:8080`; lệnh kiểm thử bên dưới dùng 8081.

**Chỉ chạy một phiên ở cổng 8080.** Nếu có `APPLICATION FAILED TO START` kèm `Port 8080 was already in use`, hãy dừng phiên dự án đang chạy (nút Stop trong IntelliJ hoặc phiên JAR trước), rồi Run lại. Kiểm tra tiến trình đang giữ cổng bằng `Get-NetTCPConnection -State Listen -LocalPort 8080 | Select-Object OwningProcess`; không tắt tất cả tiến trình Java vì có thể ảnh hưởng IntelliJ hoặc ứng dụng khác. Khi kiểm thử từ terminal xong, dừng phiên kiểm thử trước khi quay lại nút Run.

```powershell
$env:JAVA_HOME = 'DUONG_DAN_JDK_17'
$env:SPRING_DATASOURCE_URL = 'jdbc:sqlserver://localhost:1433;databaseName=shop_verify;encrypt=false;trustServerCertificate=true'
$env:SPRING_DATASOURCE_USERNAME = 'TAI_KHOAN_SQL_SERVER'
$taskSecret = Read-Host 'Mật khẩu SQL Server' -AsSecureString
$env:SPRING_DATASOURCE_PASSWORD = [System.Net.NetworkCredential]::new('', $taskSecret).Password
$env:SPRING_PROFILES_ACTIVE = 'verify'
$env:SPRING_JPA_HIBERNATE_DDL_AUTO = 'validate'
.\mvnw.cmd clean test
.\mvnw.cmd clean package
java -jar target\DemoDATN-0.0.1-SNAPSHOT.jar --spring.profiles.active=verify --server.port=8081
```

Dùng `ddl-auto=validate` sau khi chạy đủ migration; ứng dụng báo rõ schema thiếu trước khi khởi động. Cổng thường là 8080; lệnh trên dùng 8081. Chờ log `Started DemoDatnApplication in ...` trước kiểm tra.

Tài khoản khởi tạo khi chưa có: `admin/admin123`. Đổi mật khẩu trước đưa lên môi trường thật. Khách hàng tự đăng ký tại `/register`; không có cam kết tài khoản mẫu `user/user123` trên database mới.

## Cấu trúc

```text
src/main/java/com/example/demodatn/
  DemoDatnApplication.java
  config/ security/ controller/ service/ repository/
  entity/ dto/ enums/ exception/ util/
src/main/resources/
  templates/     user, admin, auth, loi và fragments
  static/        css, js, anh
src/test/java/   kiểm thử theo lớp và nghiệp vụ
database/        schema bổ sung, migrations, kiểm kê triển khai
docs/            báo cáo và hướng dẫn
```

Xem [cấu trúc dự án](docs/CAU_TRUC_DU_AN.md) và [báo cáo chốt triển khai](docs/BAO_CAO_CHOT_TRIEN_KHAI.md) để biết kết quả thực chạy và giới hạn nghiệm thu. Giá trị enum trong database giữ nguyên; nhãn giao diện dùng tiếng Việt. Văn bản nghiệp vụ mới lưu NVARCHAR; dữ liệu đã mất dấu trước đó cần nhập lại từ nguồn đúng.

Thư mục upload thật, `target/`, `verify/`, log và cấu hình bí mật cục bộ không đưa vào Git. Mật khẩu từng có trong lịch sử Git phải được thay mới; bỏ khỏi file hiện tại không xóa được lịch sử.

Đế giày thuộc sản phẩm, tùy chọn và có thể lọc tại `/products` hoặc `/admin/products-crud`. Quản lý tại `/admin/soles`; migration `010_de_giay.sql` thêm bảng, khóa ngoại và tám giá trị mặc định. `chay_tat_ca.sql` đã gộp 004–012; chạy trên bản sao trước khi triển khai. Thương hiệu giữ nguyên bảng `categories`, lớp `Category` và các route cũ.


## Quét căn cước và gửi tài khoản — Giai đoạn 4

Chạy migration `011_can_cuoc_tai_khoan_email.sql` sau 010 trước khi khởi động; Hibernate mặc định `ddl-auto=validate`, không tự sửa schema. Script gộp hiện gồm 004–012. Xem lệnh triển khai và bằng chứng tại [báo cáo Giai đoạn 4](docs/GIAI_DOAN_4_QR_EMAIL.md).

Form tạo/sửa khách hàng và nhân viên có nút **Quét QR căn cước**. Camera chỉ hoạt động trong HTTPS hoặc localhost; phải cấp quyền camera. Nếu không có camera hoặc từ chối quyền, chọn ảnh PNG/JPEG/WebP tối đa 10 MB. Thư viện jsQR được lưu trong `static/js/vendor`, đọc ảnh trên trình duyệt, không tải lên máy chủ và không lưu ảnh. Kiểm tra lại họ tên, CCCD, ngày sinh, giới tính, địa chỉ trước khi lưu; nhập tay SĐT và thư điện tử. Danh sách chỉ hiện bốn số cuối CCCD; Admin được xem đầy đủ tại chi tiết hồ sơ; hóa đơn không chứa CCCD.

Cấu hình biến môi trường `MAIL_HOST`, `MAIL_PORT` (mặc định 587), `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`, `PUBLIC_BASE_URL` (địa chỉ gốc công khai của website, không kèm `/login`). Mặc định xác thực SMTP và STARTTLS được bật, thời gian chờ kết nối 3 giây, đọc/ghi 5 giây. Không ghi mật khẩu mail vào file theo dõi bởi Git. `application-example.properties` dùng các biến này; nếu chưa cấu hình SMTP, tài khoản vẫn được lưu và trang báo **Chưa gửi được email**. Chỉ với máy chủ mail giả localhost mới đặt `MAIL_AUTH=false`, `MAIL_STARTTLS=false`.

Admin tạo hồ sơ sẽ tạo tài khoản theo quyền hiện có: khách `ROLE_USER`, nhân viên theo `shop.employee-role`. Tên đăng nhập được gợi ý từ email, kiểm tra không trùng, cho phép sửa. Mật khẩu tạm ngẫu nhiên 12 ký tự chỉ được gửi qua email sau khi commit, được lưu bằng BCrypt. Đăng nhập lần đầu bắt buộc đổi mật khẩu (ít nhất 8 ký tự, có chữ và số, tối đa 72 byte UTF-8) trước khi dùng trang khác; vẫn có thể đăng xuất. Hồ sơ cũ chưa có tài khoản dùng **Gửi lại tài khoản** để tạo.

**Gửi lại tài khoản** cần xác nhận: hủy mật khẩu cũ, tạo mật khẩu tạm mới, tối đa 3 lần gửi lại/tài khoản/giờ. Tài khoản đang vô hiệu hóa vẫn bị khóa. Nếu SMTP lỗi, sửa cấu hình rồi gửi lại; không có mật khẩu tạm trong thông báo trang, URL hoặc log.

Thử thủ công camera thật: mở HTTPS/localhost bằng thiết bị có camera → Admin tạo hồ sơ → Quét QR → Mở camera → cấp quyền → đưa căn cước vào khung → đối chiếu năm trường, nhập SĐT/email và sửa trước khi lưu. Thử hộp thư thật: cấu hình SMTP và `PUBLIC_BASE_URL` đúng → tạo hồ sơ bằng email bạn kiểm soát → kiểm tra thư và thư rác → đăng nhập bằng mật khẩu tạm → đổi mật khẩu → kiểm tra gửi lại làm mật khẩu cũ hết hiệu lực. Hai thử nghiệm này **NOT TESTED** trong báo cáo tự động; GreenMail chỉ nhận thư giả trên loopback.


## Ngừng bán và mã sản phẩm tự sinh — bổ sung 012

Danh sách và trang xem sản phẩm có **Ngừng bán / Bán lại**, từng biến thể có cùng thao tác. Hộp xác nhận nêu tác động; POST gửi trạng thái đích nên gửi lặp không đảo trạng thái lần nữa. Ngừng bán sản phẩm giữ nguyên trạng thái từng biến thể, số lượng và lịch sử; chỉ chặn bán mới ở User/POS. Giỏ và hóa đơn chờ có dòng ngừng bán sẽ cảnh báo, chặn thanh toán; Admin vẫn quản lý tồn và xem dữ liệu cũ. Nút Xóa giữ hành vi cũ và gợi ý dùng Ngừng bán khi sản phẩm có đơn.

Mã của luồng tạo sản phẩm Admin được sinh trong transaction bằng SQL SEQUENCE `dbo.product_code_seq`: `SP006`, `SP007`… theo dữ liệu hiện có, tối thiểu ba chữ số; sau `SP999` là `SP1000`. Form tạo chỉ hiện **Tự động tạo khi lưu**, form sửa chỉ đọc; server bỏ qua mã gửi lên. Mã cũ không đổi; rollback có thể tạo khoảng trống số, không tái dùng số. Các fixture tạo trực tiếp entity vẫn tự đặt mã như trước; DataInitializer hiện không seed sản phẩm.

Sau khi sao lưu và dừng ứng dụng, chạy migration **012_ma_san_pham_tu_sinh.sql** sau 011; `chay_tat_ca.sql` đã gộp 004–012, `shopgiay.sql` đã đồng bộ. Lệnh triển khai thủ công, SQL trước/sau và kết quả kiểm thử nằm trong [báo cáo bổ sung 012](docs/BO_SUNG_NGUNG_BAN_MA_TU_SINH.md). Không chạy lại bootstrap `shopgiay.sql` lên DB có dữ liệu.
