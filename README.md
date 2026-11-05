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


## Giai đoạn 32 — dọn dữ liệu và seed (thao tác riêng, không phải migration)

Giữ `database/tools/` và `database/seed/`. Hai script này không được đưa vào `chay_tat_ca.sql` và không tự chạy khi khởi động ứng dụng. `DataInitializer` chỉ chạy khi bật profile `test`; không bật profile này trên DB chính.

Dừng ứng dụng và sao lưu trước khi tự vận hành trên database cần dọn. Tên DB phải được truyền tường minh; script dừng khi `DB_NAME()` khác `TenDB_DuKien`. Dùng Windows Authentication (`-E`) hoặc tài khoản SQL qua `SQLCMDPASSWORD`, không đặt mật khẩu trong dòng lệnh/repository.

1. Xem trước (toàn bộ thay đổi được ROLLBACK; không reset bộ đếm):

```powershell
sqlcmd -S localhost -E -C -b -f 65001 -d "<DB_DA_SAO_LUU>" -v TenDB_DuKien="<DB_DA_SAO_LUU>" -i database/tools/don_du_lieu_thu.sql
```

2. Xác nhận sau khi duyệt bảng đếm/tài khoản giữ lại. Tạo file cục bộ `tmp/xac_nhan_gd32.sql` có nội dung dưới đây rồi chạy sqlcmd cùng các tham số trên, thay `-i` thành `-i tmp/xac_nhan_gd32.sql`:

```sql
EXEC sys.sp_set_session_context @key=N'XAC_NHAN', @value=N'XOA-DU-LIEU-THU';
GO
:r database/tools/don_du_lieu_thu.sql
```

`@DanhMucDuyetXoa` mặc định rỗng: không xóa thương hiệu/kích cỡ/chất liệu/màu/đế khi chưa duyệt ID. Tài khoản phải có đúng một `admin` và một `user`; script giữ nguyên mật khẩu cùng hồ sơ/địa chỉ liên kết. Script từ chối schema có bảng chưa kiểm kê. Không tắt khóa ngoại.

3. Tạo 5 sản phẩm, 40 biến thể, 2 đợt giảm giá, 5 phiếu:

```powershell
sqlcmd -S localhost -E -C -b -f 65001 -d "<DB_DA_SAO_LUU>" -v TenDB_DuKien="<DB_DA_SAO_LUU>" -i database/seed/du_lieu_mau.sql
```

Seed chạy lặp không tăng số dòng, không ghi đè tồn kho đã bán và không thêm log khởi tạo lần hai. Ngày hiệu lực cố định 05/11/2026–31/12/2026 (đợt khai trương kết thúc 30/11), giờ Việt Nam. Ngoại lệ có chủ đích: reset bộ đếm chỉ cho bảng đã xóa sạch, nên mã bắt đầu SP001/BT0001/KM001/CH0001; không reset bộ đếm danh mục/hồ sơ còn dữ liệu.

Ảnh do chủ cửa hàng cung cấp, đặt trong `uploads/products/`:

| Sản phẩm | Tên file |
|---|---|
| Nike Air Force 1 '07 | nike-air-force-1-07.jpg |
| Adidas Ultraboost Light | adidas-ultraboost-light.jpg |
| Converse Chuck Taylor All Star Classic | converse-chuck-taylor-classic.jpg |
| Vans Old Skool | vans-old-skool.jpg |
| ASICS Gel-Kayano 14 | asics-gel-kayano-14.jpg |

Chưa có ảnh thì dùng cơ chế ảnh thay thế hiện có (`/anh/anh-san-pham-thay-the.svg`). Không tự tải/vẽ ảnh. Không tự xóa upload; danh sách file mồ côi, hướng dẫn kiểm tra và kết quả thực tế nằm trong `docs/GIAI_DOAN_32.md`.
