# F6S Store - H? Th?ng Qu?n L? B?n Gi?y Th? Thao

H? th?ng b?n h?ng ?a k?nh (t?i qu?y POS v? tr?c tuy?n Storefront) x?y d?ng tr?n n?n t?ng Spring Boot v? SQL Server.

---

## 1. Y?u C?u M?i Tr??ng
- **JDK**: Java 17 tr? l?n (khuy?n ngh? Eclipse Adoptium Temurin JDK 17).
- **C? s? d? li?u**: Microsoft SQL Server 2019 / 2022.
- **Node.js** (t?y ch?n, ch? khi ch?y ki?m th? Playwright E2E): Node.js 18+.

---

## 2. C?u H?nh Database Duy Nh?t
D? ?n s? d?ng **duy nh?t m?t database ch?nh th?c** c? t?n l?: `shopbangiay`.

- C?u h?nh m?c ??nh trong `src/main/resources/application.properties` tr? t?i:
  `jdbc:sqlserver://${DB_HOST:localhost}:${DB_PORT:1433};databaseName=shopbangiay;encrypt=true;trustServerCertificate=true`
- ?? t?y ch?nh k?t n?i t?i m?y ph?t tri?n m? kh?ng ?nh h??ng m? ngu?n (file n?y ?? ???c `.gitignore`), sao ch?p file m?u:
  ```bash
  cp application-local.properties.example application-local.properties
  ```
- Ch?nh s?a `application-local.properties` v?i th?ng tin k?t n?i th?c t?:
  ```properties
  spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=shopbangiay;encrypt=true;trustServerCertificate=true
  spring.datasource.username=sa
  spring.datasource.password=123456789
  ```

---

## 3. Kh?i T?o C? S? D? Li?u & Ch?y Migration
> **L?u ? quan tr?ng**: Tr??c khi ch?y tr?n database ch?nh th?c, h?y th?c hi?n sao l?u (backup) database.

### B??c 1: Kh?i t?o schema c? s? (cho database m?i)
T?o database r?ng `shopbangiay` tr?n SQL Server, sau ?? ch?y script `shopgiay.sql`:
```bash
sqlcmd -S localhost,1433 -U sa -P 123456789 -C -I -b -d shopbangiay -i shopgiay.sql
```

### B??c 2: C?p nh?t c?c migration phi?n b?n m?i
Ch?y script migration t?ng h?p `chay_tat_ca.sql`:
```bash
sqlcmd -S localhost,1433 -U sa -P 123456789 -C -I -b -d shopbangiay -i chay_tat_ca.sql
```
Script `chay_tat_ca.sql` ???c thi?t k? c? t?nh l?y k? (idempotent), c? th? ch?y nhi?u l?n m? kh?ng g?y l?i ho?c tr?ng l?p d? li?u.

---

## 4. Ch?y ?ng D?ng
S? d?ng Maven Wrapper ?? kh?i ch?y ?ng d?ng Spring Boot:
- **Windows**:
  ```powershell
  .\mvnw.cmd spring-boot:run
  ```
- **Linux/macOS**:
  ```bash
  ./mvnw spring-boot:run
  ```
?ng d?ng s? kh?i ??ng t?i: `http://localhost:8080`.

---

## 5. Ch?y Ki?m Th? (Unit & Integration Tests)
D? ?n ???c trang b? **CH?T CH?N B?O V? (`TestDatabaseGuard`)**: To?n b? b? test s? **t? ??ng t? ch?i ch?y (fail ngay l?p t?c)** n?u ph?t hi?n URL k?t n?i tr? t?i database ch?nh th?c `shopbangiay`.

?? ch?y ki?m th?, h?y c?u h?nh database t?m (v? d? `shopbangiay_kt_temp`):
```powershell
.\mvnw.cmd test "-Dspring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=shopbangiay_kt_temp;encrypt=true;trustServerCertificate=true"
```

?? ch?y ki?m th? End-to-End v?i Playwright:
```bash
cd e2e
npm install
npm test
```

---

## 6. C?u H?nh Email
H? th?ng s? d?ng Spring Mail ?? g?i th? x?c nh?n ??i m?t kh?u, th?ng b?o h?a ??n v? tr?ng th?i giao h?ng.
- Xem chi ti?t h??ng d?n t?i: [docs/CAU_HINH_EMAIL.md](docs/CAU_HINH_EMAIL.md).

---

## 7. T?i Kho?n Qu?n Tr? M?c ??nh
- **Qu?n tr? vi?n (Admin)**: `admin` / `admin123` (to?n quy?n truy c?p Dashboard, POS, Qu?n l? h?a ??n, S?n ph?m, Khuy?n m?i, T?i kho?n, H? tr? kh?ch h?ng).
- **Nh?n vi?n (Employee)**: ph?n quy?n theo ma tr?n chi ti?t t?i [docs/PHAN_QUYEN.md](docs/PHAN_QUYEN.md).

---

## 8. C?u Tr?c C?y Th? M?c D? ?n
```
DemoDATN/
??? .mvn/                               # C?u h?nh Maven Wrapper
??? database/                           # Th? m?c database
?   ??? data/                           # D? li?u ngu?n (JSON h?nh ch?nh)
?   ??? migrations/                     # 26 file migration SQL l?y k?
??? docs/                               # T?i li?u h??ng d?n ch?nh th?c
?   ??? CAU_HINH_EMAIL.md               # H??ng d?n c?u h?nh d?ch v? email
?   ??? PHAN_QUYEN.md                   # Ma tr?n ph?n quy?n Admin & Nh?n vi?n
??? e2e/                                # B? ki?m th? t? ??ng Playwright E2E
?   ??? package.json
?   ??? README.md
?   ??? e2e-suite.js
??? src/
?   ??? main/                           # M? ngu?n ch?nh (Java, Templates, Static Assets)
?   ??? test/                           # B? ki?m th? t? ??ng k?m TestDatabaseGuard
??? .gitattributes                      # C?u h?nh git line endings
??? .gitignore                          # C?u h?nh lo?i b? file r?c & local
??? application-local.properties.example# M?u c?u h?nh database local
??? chay_tat_ca.sql                     # Script ch?y t?t c? migration
??? mvnw & mvnw.cmd                     # Maven wrapper executable
??? pom.xml                             # C?u h?nh dependencies Maven
??? README.md                           # T?i li?u d? ?n
??? shopgiay.sql                        # Script kh?i t?o schema c? s?
```


## Giai đoạn 32 — BẢN THAY THẾ: xóa hẳn dữ liệu, không seed

Yêu cầu này thay hoàn toàn bản dọn rồi seed trước đây. Script duy nhất: `database/tools/don_du_lieu_thu.sql`, chạy một lần ngoài migration. File seed sản phẩm của bản cũ đã được gỡ; không có bước tạo dữ liệu mẫu. Sau dọn, chủ cửa hàng tự nhập dữ liệu thật qua giao diện.

Chỉ chủ dự án tự vận hành trên DB chính sau khi dừng app, sao lưu và kiểm tra bản cứu hộ. Agent chỉ thử trên DB mới RESTORE, không ghi đè DB có sẵn, không kết nối `shopbangiay`/`shopbangiay_thu`.

Script dùng DELETE theo khóa ngoại, kể cả dòng đang ẩn/ngừng bán/tắt. Không đổi status thay cho xóa, không tắt ràng buộc. Giữ admin/user cùng hồ sơ/địa chỉ, các vai trò/cấu hình/FAQ/tỉnh-phường-xã; DataInitializer chỉ ở profile test (không bật profile test trên DB chính).

Danh mục gốc đối chiếu lịch sử `shopgiay.sql`: 6 thương hiệu từ commit21f76e6 (Nike, Adidas, Converse, Vans, Puma, New Balance), kích cỡ36–43; chất liệu8tên từ2229c5b; đế giày8tên từba08a0e; màu sắc11tên từ8410685. So tên bỏ qua hoa thường và khoảng trắng, giữ phân biệt dấu. Không lấy danh sách3thương hiệu/3kích cỡ đã bị rút gọn về sau làm gốc.

`@DanhMucGiuThem` mặc định rỗng. Tên ngoài gốc có thể là dữ liệu thật sẽ được SELECT rồi THROW để chờ chủ dữ liệu điền thêm tên cần GIỮ (ví dụ categories/ASICS). Các tên thử rõ ràng ngoài gốc được DELETE. Không tự quyết định giữ/xóa tên thật chưa duyệt.

1. Xem trước bằng đúng tên DB; mọi thay đổi ROLLBACK, không reset bộ đếm:

```powershell
sqlcmd -S localhost -E -C -b -f 65001 -d "<DB_DA_SAO_LUU>" -v TenDB_DuKien="<DB_DA_SAO_LUU>" -i database/tools/don_du_lieu_thu.sql
```

2. Sau khi duyệt bảng đếm/tài khoản/danh mục, tạo file cục bộ `tmp/xac_nhan_gd32.sql`:

```sql
EXEC sys.sp_set_session_context @key=N'XAC_NHAN',@value=N'XOA-DU-LIEU-THU';
GO
:r database/tools/don_du_lieu_thu.sql
```

```powershell
sqlcmd -S localhost -E -C -b -f 65001 -d "<DB_DA_SAO_LUU>" -v TenDB_DuKien="<DB_DA_SAO_LUU>" -i tmp/xac_nhan_gd32.sql
```

Dùng Windows Authentication hoặc SQLCMDPASSWORD; không ghi mật khẩu vào lệnh/file/repository. Script dừng nếu DB_NAME khác TenDB_DuKien, thiếu đúng một admin/user hoặc gặp bảng chưa kiểm kê. Toàn bộ một transaction; lỗi rollback và nêu bảng/bước.

Ngoại lệ có chủ đích: chỉ reset IDENTITY/SEQUENCE của bảng đã rỗng hoàn toàn, để nhập tiếp SP001/BT0001/KM001/CH0001. Không reset bộ đếm bảng còn dữ liệu. Không DROP đối tượng/schema.

3. File ảnh mồ côi: chỉ kiểm kê, không tự xóa. Xem bảng cần duyệt và lệnh riêng trong `docs/GIAI_DOAN_32.md` (bản thay thế). Không chạy lại hướng dẫn seed hoặc báo cáo cũ trong `docs/evidence/gd32/`; đó chỉ là bằng chứng lịch sử của yêu cầu đã bị thay thế.

Kiểm thử lại cần đặt `SPRING_DATASOURCE_URL` trỏ rõ tới database RESTORE riêng trước khi chạy `mvnw clean test` hoặc app cổng khác. Giữ nguyên chốt TestDatabaseGuard. Test tự tạo fixture trong src/test, không cần chạy seed sản phẩm. Sau nhóm test ghi dữ liệu, RESTORE lại một bản mới và chạy chỉ script dọn; không đưa bản test còn fixture vào cấu hình vận hành.
