# F6S Store - Hệ Thống Quản Lý Bán Giày Thể Thao Sneaker

Hệ thống bán hàng đa kênh (Bán tại quầy POS và Cửa hàng trực tuyến Storefront) xây dựng trên nền tảng Spring Boot và Microsoft SQL Server.

---

## 1. Yêu Cầu Môi Trường
- **JDK**: Java 17 trở lên (khuyến nghị OpenJDK 17 hoặc Microsoft Build of OpenJDK 17).
- **Cơ sở dữ liệu**: Microsoft SQL Server 2019 / 2022.
- **Trình duyệt**: Google Chrome / Microsoft Edge phiên bản mới nhất.

---

## 2. Cấu Hình Cơ Sở Dữ Liệu Duy Nhất
Dự án sử dụng **duy nhất một database chính thức** có tên là: `shopbangiay`.

- Cấu hình mặc định trong `src/main/resources/application.properties`:
  ```properties
  spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=shopbangiay;encrypt=true;trustServerCertificate=true
  spring.datasource.username=sa
  spring.datasource.password=123456789
  spring.jpa.hibernate.ddl-auto=validate
  ```
- Để tùy chỉnh thông tin tài khoản kết nối trên máy cá nhân mà không làm ảnh hưởng mã nguồn (file này đã được `.gitignore`), sao chép file mẫu:
  ```powershell
  copy application-local.properties.example application-local.properties
  ```
- Mở `application-local.properties` và nhập tài khoản sa/password của máy bạn.

---

## 3. Khởi Tạo Cơ Sở Dữ Liệu (1 File Duy Nhất)
Toàn bộ cấu trúc bảng, khóa ngoại, chỉ mục, chuỗi tự tăng (sequences) và dữ liệu mẫu chuẩn của hệ thống được đóng gói trọn gói trong **1 file SQL duy nhất**:

📁 **`database/shopbangiay.sql`**

### Cách cài đặt:
1. Mở **SQL Server Management Studio (SSMS)** hoặc **Azure Data Studio**.
2. Mở file `database/shopbangiay.sql` và nhấn **Execute** (`F5`).
3. Hoặc chạy nhanh qua cửa sổ dòng lệnh (Terminal):
   ```powershell
   sqlcmd -S localhost -U sa -P 123456789 -C -I -i database/shopbangiay.sql
   ```
Script sẽ tự động tạo cơ sở dữ liệu `shopbangiay` với đầy đủ 40 bảng dữ liệu và thiết lập sẵn sàng.

---

## 4. Hướng Dẫn Chạy Ứng Dụng

Sử dụng Maven Wrapper có sẵn trong thư mục dự án để khởi chạy:
- **Windows (PowerShell / Command Prompt)**:
  ```powershell
  .\mvnw.cmd spring-boot:run
  ```
- **Linux / macOS**:
  ```bash
  ./mvnw spring-boot:run
  ```

Sau khi ứng dụng khởi động thành công:
- **Trang mua hàng (Storefront)**: [http://localhost:8080](http://localhost:8080)
- **Trang đăng nhập**: [http://localhost:8080/login](http://localhost:8080/login)
- **Trang quản trị (Admin/POS)**: [http://localhost:8080/admin/pos](http://localhost:8080/admin/pos)

---

## 5. Tài Khoản Đăng Nhập Mẫu

| Tài khoản | Mật khẩu | Vai trò | Quyền hạn chính |
| :--- | :--- | :--- | :--- |
| **admin** | `admin123` | Quản trị viên (ADMIN) | Toàn quyền quản trị Dashboard, Bán tại quầy (POS), Quản lý đơn hàng & hóa đơn, Sản phẩm & Biến thể, Đợt giảm giá & Phiếu giảm giá, Nhân viên, Khách hàng, Trợ lý & Hộp thư hỗ trợ. |
| **vietanhnv** | `12345678` | Nhân viên (STAFF) | Bán hàng tại quầy POS, xử lý đơn hàng, tư vấn khách hàng. |
| **reydathu** | `12345678` | Khách hàng (USER) | Xem sản phẩm, đặt hàng online, theo dõi đơn hàng, quản lý sổ địa chỉ, đánh giá sản phẩm. |

---

## 6. Cấu Trúc Thư Mục Dự Án

```
DemoDATN/
├── .mvn/                               # Cấu hình Maven Wrapper
├── database/                           # Cơ sở dữ liệu
│   ├── shopbangiay.sql                 # File script khởi tạo trọn gói database duy nhất
│   ├── data/                           # Dữ liệu hành chính (tỉnh/phường)
│   └── migrations/                     # Lịch sử các bước migration
├── docs/                               # Tài liệu hướng dẫn
│   ├── CAU_HINH_EMAIL.md               # Hướng dẫn cấu hình gửi mail SMTP
│   └── PHAN_QUYEN.md                   # Bảng phân quyền chi tiết các chức năng
├── src/                                # Mã nguồn ứng dụng (Spring Boot + Thymeleaf)
├── uploads/                            # Thư mục lưu trữ hình ảnh sản phẩm
├── pom.xml                             # Cấu hình Maven Dependencies
├── application-local.properties.example# Mẫu cấu hình database cá nhân
└── README.md                           # Tài liệu hướng dẫn dự án
```
