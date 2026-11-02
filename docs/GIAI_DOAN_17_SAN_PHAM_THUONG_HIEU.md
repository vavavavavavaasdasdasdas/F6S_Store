# BÁO CÁO GIAI ĐOẠN 17: NÚT BẬT/TẮT, GỘP XEM CHI TIẾT, FORM THÊM SẢN PHẨM

- **Nhánh làm việc**: `gop-y-sua-lan-2` (tạo từ `gop-y-dot-moi`)
- **Cơ sở dữ liệu kiểm thử**: `shopbangiay_kt_20261102_gd17_sua` (RESTORE mới từ bản sao lưu `shopbangiay_kt_20261101_gd14_schema.bak`; tuyệt đối không chạm vào `shopbangiay` hoặc `shopbangiay_thu`).
- **Trạng thái schema**: Không thêm/xóa bảng hay cột; `spring.jpa.hibernate.ddl-auto=validate` sạch.

---

## 1. Chỗ đã thay đổi so với giai đoạn cũ (Luật 6)

| STT | Nội dung ở giai đoạn 9–15 cũ | Thay đổi ở Giai đoạn 17 (`gop-y-sua-lan-2`) |
|---|---|---|
| 1 | Trang Chi tiết sản phẩm (`/admin/products-crud`) có nút `Ngừng bán` / `Bán lại` dạng nút icon rời và có hai nút xem (`Xem` và `Chi tiết biến thể`) dẫn tới hai trang tách rời (`/admin/products-crud/view/{id}` và `/admin/products-crud/{id}/variants`). | Thay nút `Ngừng bán` / `Bán lại` bằng **công tắc bật/tắt** (`role="switch"`, fragment `productSwitch`), gửi POST AJAX có CSRF tới `/admin/products-crud/status/{id}`, có khóa chống bấm đúp, hiện hộp xác nhận nêu rõ hệ quả khi chuyển sang tắt. Gộp `Xem` và `Chi tiết biến thể` thành **một nút mắt duy nhất `"Xem chi tiết"`** (`/admin/products-crud/view/{id}`). Cột Hành động chỉ còn đúng **3 thành phần** (`Xem chi tiết`, `Sửa`, công tắc bật/tắt) xếp ngang một hàng. Mọi request xóa sản phẩm (`/admin/products-crud/delete/{id}` qua `GET`/`POST`/`DELETE`) bị từ chối với HTTP `405` (Admin) hoặc `403` (Employee/thiếu CSRF). |
| 2 | Trang `/admin/products-crud/view/{id}` chỉ hiển thị thông tin sản phẩm và bảng biến thể chỉ đọc; thao tác sửa/thêm biến thể nằm ở trang riêng `/admin/products-crud/{id}/variants`. Sau khi tạo sản phẩm, banner thông báo hiện nút `"Mở biến thể sản phẩm"` trỏ sang `/variants`. | Hợp nhất toàn bộ bảng biến thể có chỉnh sửa trực tiếp (12 cột `STT \| Mã SP \| Tên SP \| Mã biến thể \| Kích cỡ \| Màu sắc \| Chất liệu \| Đế giày \| Giá (₫) \| Tồn kho \| Trạng thái \| Hành động`, bộ lọc biến thể, phân trang 12 dòng, thêm 1 biến thể, thêm nhiều kích cỡ cùng lúc, sửa tại chỗ có ghi `inventory_logs` qua `InventoryService`, bật/tắt từng biến thể) vào ngay dưới phần thông tin sản phẩm trên `/admin/products-crud/view/{id}`. URL cũ `GET /admin/products-crud/{id}/variants` chuyển hướng HTTP `302` về `/admin/products-crud/view/{id}` (giữ nguyên query string). Nút thông báo sau khi tạo sản phẩm đổi thành `"Xem chi tiết"` trỏ tới `/admin/products-crud/view/{createdProductId}`. |
| 3 | Trang Thương hiệu (`/admin/categories`) dùng nút `Ẩn`/`Hiện` và vẫn cho phép gọi route xóa `/admin/categories/delete/{id}`. | Bỏ hoàn toàn nút `Ẩn`/`Hiện` và nút `Xóa` trên `/admin/categories`; thay bằng **công tắc bật/tắt `Hiển thị`/`Ẩn`** (`categorySwitch`), gửi POST AJAX có CSRF tới `/admin/categories/toggle-status/{id}`, hiện hộp xác nhận khi tắt (`Ẩn`). Cột Hành động trên `/admin/categories` chỉ còn đúng **2 thành phần** (`Sửa` và công tắc bật/tắt). Mọi request xóa thương hiệu (`/admin/categories/delete/{id}`, `/admin/danh-muc/delete/{id}`) bị từ chối HTTP `405` (Admin) hoặc `403` (Employee/thiếu CSRF). |
| 4 | Form Thêm sản phẩm (`/admin/products-crud/create`) đặt `Chất liệu` và `Đế giày` ở dưới hoặc cho phép `data-creatable="true"` (gõ tên tự do để tạo mới nhanh). | Đưa `Chất liệu` (`#initialMaterialId`) và `Đế giày` (`#initialSoleId`) lên **ngay trước `Mô tả` (`#description`)**, sau `Tên sản phẩm, Mã sản phẩm, Thương hiệu, Trạng thái`; bên dưới `Mô tả` mới đến `Bộ ảnh sản phẩm`, rồi khối `Biến thể ban đầu` (`Màu sắc, Kích cỡ chọn nhiều, Giá, Tồn kho ban đầu, dòng "Sẽ tạo N biến thể"`). Đổi `Chất liệu` và `Đế giày` thành combo box **chọn 1 bắt buộc** từ danh sách đang hoạt động (bỏ `data-creatable`, không có nút tạo nhanh; server từ chối mảng nhiều giá trị, ID ẩn/không tồn tại hoặc chuỗi tự do và báo lỗi cạnh ô). Giữ bố cục 2 cột trên màn hình rộng (`1280px`) và tự xếp dọc 1 cột ở `375px`. |
| 5 | Các bài test cũ (`PaginationIntegrationTest`, `Phase3ComboFilterIntegrationTest`, `ProductWorkspaceIntegrationTest`, `InitialMaterialIntegrationTest`, `SaleStatusAndCodeIntegrationTest`) kỳ vọng `GET /admin/products-crud/{id}/variants` trả `200` hoặc cho phép `newInitialMaterialName`/`newSoleName` trên form tạo sản phẩm. | Cập nhật test để kiểm tra `GET /admin/products-crud/{id}/variants` chuyển hướng `302` về `/admin/products-crud/view/{id}` (giữ tham số lọc/phân trang), kiểm tra bảng biến thể hợp nhất trên `/admin/products-crud/view/{id}`, và kiểm tra server từ chối tạo nhanh chất liệu/đế giày trên form thêm sản phẩm. |

---

## 2. Bảng kết quả kiểm thử tổng hợp (Luật 1)

| Hạng mục | Cách kiểm tra | Kết quả thực tế | PASS/FAIL/NOT TESTED |
|---|---|---|---|
| **A.1** Trang `/admin/products-crud`: bỏ nút Xóa, thay nút Ngừng bán/Bán lại bằng công tắc bật/tắt AJAX có CSRF, chống bấm đúp, hiện hộp xác nhận khi tắt, giữ nguyên biến thể & tồn kho; từ chối `/admin/products-crud/delete/{id}` | Kiểm tra bằng Playwright (`product-toggle-switch-ajax`, `product-toggle-on-and-delete-405`) và đối chiếu SQL `products`, `product_variants` trước/sau (`gd17-sql-evidence.json`) | Khi tắt sản phẩm `SP002` (`id=2`): hiện `#admin-confirm` nêu rõ hệ quả, gửi POST AJAX trả `200`, `products.status` chuyển `true -> false`, 2 biến thể (`id=3, 4`) giữ nguyên `status=true` và `quantity=18, 4`. Bấm đúp khi bật lại chỉ gửi đúng `1` request POST. `GET/POST/DELETE /admin/products-crud/delete/2` trả HTTP `405` (Admin) và `403` (Employee/thiếu CSRF) | **PASS** |
| **A.2** Gộp `"Xem"` và `"Chi tiết biến thể"` thành một nút mắt `"Xem chi tiết"`; cột Hành động trên `/admin/products-crud` chỉ còn đúng 3 phần tử xếp ngang một hàng | Kiểm tra DOM & bounding box cột Hành động ở `1280px` và `375px` (`products-list-actions-and-layout`) | Mỗi dòng trên `/admin/products-crud` có `0` thẻ `<button>`, đúng `2` nút icon link (`Xem chi tiết` -> `/admin/products-crud/view/{id}`, `Sửa` -> `/admin/products-crud/edit/{id}`) và `1` công tắc `input[role="switch"]`; ở `1280px` cả 3 phần tử nằm trên cùng 1 hàng ngang (`Δtop <= 2px`) và không bị cắt | **PASS** |
| **A.3** Trang `/admin/products-crud/view/{id}` gộp thông tin sản phẩm và bảng biến thể chỉnh sửa đầy đủ (12 cột); `/admin/products-crud/{id}/variants` chuyển hướng `302`; sau khi tạo sản phẩm hiện nút `"Xem chi tiết"`; trang sửa sản phẩm chỉ có thông tin sản phẩm | Kiểm tra bằng Playwright ở `1280px` & `375px` (`product-view-merged-detail-and-variants`, `product-view-variant-operations`) và đối chiếu SQL `product_variants`, `inventory_logs` | `GET /admin/products-crud/949/variants?page=0&variantStatus=true` trả `302` về `/admin/products-crud/view/949?page=0&variantStatus=true`; trang `view/949` hiển thị đủ thông tin SP + bảng 12 cột; sửa tại chỗ biến thể `BT0021` (`id=598`) từ tồn `7 -> 12`, giá `680.000 -> 720.000` ghi `inventory_logs` (`quantity_change=+5, quantity_before=7, quantity_after=12`) và cập nhật tổng tồn `14 -> 19`; trang `/admin/products-crud/edit/949` chỉ có thông tin sản phẩm | **PASS** |
| **B.1** Trang Thương hiệu (`/admin/categories`): bỏ nút Ẩn/Hiện và nút Xóa; thay bằng công tắc `Hiển thị`/`Ẩn` AJAX có CSRF, xác nhận khi tắt; cột Hành động chỉ còn `Sửa` + công tắc; từ chối xóa | Kiểm tra bằng Playwright ở `1280px` & `375px` (`categories-list-actions-and-layout`, `category-toggle-off-and-on`, `category-toggle-on-and-delete-405`) và đối chiếu SQL `categories` | Cột Hành động trên `/admin/categories` chỉ có `1` nút icon `Sửa` và `1` công tắc `role="switch"`. Khi tắt thương hiệu `TH001` (`Nike`, `id=1`): hiện hộp xác nhận, `categories.status` chuyển `true -> false`, nhãn đổi thành `"Đang ẩn"`, bật lại chuyển `false -> true`. `GET/POST /admin/categories/delete/1` và `POST /admin/danh-muc/delete/1` trả `405` (Admin) và `403` (Employee/thiếu CSRF) | **PASS** |
| **C.1, C.2, C.3** Form Thêm sản phẩm (`/admin/products-crud/create`): `Chất liệu` và `Đế giày` chọn 1 bắt buộc (không tạo nhanh), đặt ngay trước `Mô tả`; bố cục 2 cột ở `1280px`, 1 cột ở `375px`; áp dụng cho mọi biến thể ban đầu | Kiểm tra thứ tự DOM, tọa độ hình học ở `1280px` & `375px`, kiểm tra 4 kịch bản validate server (thiếu, nhiều giá trị, chuỗi tự do, ID không tồn tại/ẩn) và tạo sản phẩm mới với 2 kích cỡ (`product-create-*`) | Thứ tự trường: `name, code, categoryId, status, initialMaterialId, initialSoleId, description, images, initialColorId, initialPrice, initialQuantity`; `initialMaterialId` và `initialSoleId` nằm ngay trên `description` (2 cột ngang ở `1280px`, xếp dọc 1 cột ở `375px`), không có `multiple` hay `data-creatable`. Server chặn đủ 4 trường hợp dữ liệu sai. Tạo mới `SP006` (`id=949`) với 2 kích cỡ sinh 2 biến thể (`BT0021`, `BT0022`) mang đúng `chat_lieu_id=1, de_giay_id=1, price=680000, quantity=7` | **PASS** |
| **Luật 8** Kiểm thử tự động `mvnw clean test`, Playwright `1280px` & `375px`, 8 kịch bản bảo mật, crawl toàn bộ link nội bộ | Chạy `mvnw clean test` (không skip), `verify_gd17.py` và `crawl.py` | `Tests run: 248, Failures: 0, Errors: 0, Skipped: 0`; 19 bước Playwright PASS; 8 kịch bản CSRF + phân quyền Employee PASS; Crawl `467` link admin, `205` link employee, `23` link user, `21` link guest đều PASS (`0` link chết) | **PASS** |

---

## 3. Bằng chứng thô (Raw Evidence)

### 3.1. Bằng chứng SQL trước/sau (`docs/evidence/gd17/gd17-sql-evidence.json`)

1. **Bật/tắt sản phẩm `SP002` (`id = 2`) trên `/admin/products-crud`**:
   - **Trước khi tắt**: `products(id=2, code='SP002', status=true)`; `product_variants`: `[{id: 3, status: true, quantity: 18}, {id: 4, status: true, quantity: 4}]`.
   - **Sau khi tắt qua công tắc AJAX (`POST /admin/products-crud/status/2`)**: `products(id=2, code='SP002', status=false)`; `product_variants` giữ nguyên `[{id: 3, status: true, quantity: 18}, {id: 4, status: true, quantity: 4}]`.
   - **Sau khi bật lại (bấm đúp)**: chỉ gửi `postCount = 1` request, `products(id=2, code='SP002', status=true)`.
   - **Thử gọi xóa sản phẩm**: `GET /admin/products-crud/delete/2 -> 405`, `POST /admin/products-crud/delete/2 -> 405`, `DELETE /admin/products-crud/delete/2 -> 405`.
2. **Bật/tắt thương hiệu `TH001` (`Nike`, `id = 1`) trên `/admin/categories`**:
   - **Trước khi tắt**: `categories(id=1, code='TH001', name='Nike', status=true)`.
   - **Sau khi tắt qua công tắc AJAX (`POST /admin/categories/toggle-status/1`)**: `categories(id=1, code='TH001', name='Nike', status=false)`, badge hiển thị `"Đang ẩn"`.
   - **Sau khi bật lại**: `categories(id=1, code='TH001', name='Nike', status=true)`.
   - **Thử gọi xóa thương hiệu**: `POST /admin/categories/delete/1 -> 405`, `GET /admin/categories/delete/1 -> 405`, `POST /admin/danh-muc/delete/1 -> 405`.
3. **Tạo sản phẩm mới trên `/admin/products-crud/create` và chỉnh sửa biến thể trên `/admin/products-crud/view/{id}`**:
   - **Sau khi tạo sản phẩm**: `products(id=949, code='SP006', name='Giày Chạy Bộ GĐ17 Mới', category_id=1, status=true)`; sinh 2 biến thể `BT0021` (`id=598, size_id=1`) và `BT0022` (`id=599, size_id=2`) cùng có `chat_lieu_id=1, de_giay_id=1, mau_sac_id=1, price=680000.0, quantity=7, status=true`. Nút thông báo flash là `"Xem chi tiết"` trỏ tới `/admin/products-crud/view/949`.
   - **Sau khi sửa tại chỗ biến thể `BT0021` (`id=598`) và ngừng bán trên `/admin/products-crud/view/949`**:
     - `product_variants(id=598)`: `price = 720000.0`, `quantity = 12`, `status = false`.
     - `inventory_logs`: `{quantity_change: 5, quantity_before: 7, quantity_after: 12, reason: "Nhập bổ sung tại trang Xem chi tiết GĐ17", actor: "admin"}`.
     - Tổng tồn kho trên đầu trang chi tiết cập nhật tức thời lên `19`.
4. Sau khi kết thúc kiểm thử trình duyệt, database `shopbangiay_kt_20261102_gd17_sua` đã được RESTORE lại về checkpoint sạch ban đầu (`Restored gd17_sua checkpoint`).

### 3.2. Bằng chứng `git grep` kiểm tra tham chiếu route cũ

```text
$ git grep -n -E "/admin/products-crud/.*/variants|/admin/products-crud/delete|/admin/categories/delete|/admin/danh-muc/delete" src/main/
src/main/java/com/example/demodatn/controller/admin/sanpham/AdminProductVariantController.java:20:@RequestMapping("/admin/products-crud/{productId}/variants")
src/main/resources/static/js/sua-bien-the-san-pham.js:4:    const productId=panel.dataset.productId,base=`/admin/products-crud/${productId}/variants`;
src/main/resources/templates/admin/san-pham/chi-tiet.html:22:<form data-batch-variants method="post" th:action="@{/admin/products-crud/{id}/variants/batch(id=${product.id})}" novalidate>
src/main/resources/templates/admin/san-pham/dong-bien-the.html:13:<form data-inline-variant method="post" th:id="'variant-form-'+${v.id}" th:action="@{/admin/products-crud/{productId}/variants/inline-save(productId=${v.product_id})}" novalidate>
```
*(Không còn bất kỳ liên kết `<a href>` nào trỏ tới trang `/admin/products-crud/{id}/variants` cũ hay form xóa sản phẩm/thương hiệu; các endpoint `/variants/batch`, `/variants/inline-save`, `/variants/{id}/status`, `/variants/row/{id}` là các endpoint AJAX phục vụ bảng biến thể ngay trên `/admin/products-crud/view/{id}`).*

### 3.3. Bằng chứng 8 kịch bản bảo mật & Crawl toàn bộ liên kết nội bộ (`docs/evidence/gd17/browser-results.json`, `docs/evidence/gd17/crawl-manifest.json`)

- **Phân quyền Employee (`ROLE_EMPLOYEE`)**:
  - `GET /admin/products-crud` -> HTTP `403`
  - `GET /admin/products-crud/view/2` -> HTTP `403`
  - `GET /admin/categories` -> HTTP `403`
  - `GET /admin/products-crud/delete/2` -> HTTP `403`
  - `GET /admin/categories/delete/1` -> HTTP `403`
- **8 kịch bản CSRF (thiếu token & sai token trên 4 endpoint POST)**:
  - `POST /admin/products-crud/status/2` (missing CSRF -> `403`, invalid CSRF -> `403`)
  - `POST /admin/categories/toggle-status/1` (missing CSRF -> `403`, invalid CSRF -> `403`)
  - `POST /admin/products-crud/save` (missing CSRF -> `403`, invalid CSRF -> `403`)
  - `POST /admin/products-crud/2/variants/inline-save` (missing CSRF -> `403`, invalid CSRF -> `403`)
- **Crawl toàn bộ link nội bộ**:
  - `admin`: `467` URL (`exhausted: true`, `0` lỗi)
  - `employee`: `205` URL (`exhausted: true`, `0` lỗi)
  - `user`: `23` URL (`exhausted: true`, `0` lỗi)
  - `guest`: `21` URL (`exhausted: true`, `0` lỗi)

### 3.4. Kết quả `mvnw clean test` (`docs/evidence/gd17/mvnw-clean-test.txt`)

- `Tests run: 248, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS`.
- Không có thay đổi schema DB ở Giai đoạn 17 nên không cần chạy thêm migration SQL nào trên DB thật.
