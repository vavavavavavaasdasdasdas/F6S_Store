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
