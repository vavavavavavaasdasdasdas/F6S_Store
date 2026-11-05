# Lỗi phát hiện ngoài phạm vi


## Giai đoạn 32 — chỉ ghi nhận

| Mã | Trang / vị trí liên quan | Bằng chứng | Trạng thái |
|---|---|---|---|
|GD32-04|/admin/pos; static/css/quan-tri.css:175|docs/evidence/gd32/final-browser.json:1280overflow=true,375false; screenshots/final-pos-1280.png|Chưa sửa; chưa chốt nguyên nhân CSS. Ngoài dọn dữ liệu/seed.|
|GD32-05|static/js/dia-chi-giao-hang.js:43|browser-extra.json requestfailed: https://tile.openstreetmap.org/12/3251/1803.png,net::ERR_NAME_NOT_RESOLVED|Chưa sửa; lỗi mạng tài nguyên ngoài,không phải404 nội bộ.|


## Giai đoạn32 BẢN THAY THẾ — kiểm thử DB rỗng

| Mã | File:dòng | Bằng chứng | Trạng thái |
|---|---|---|---|
|GD32R-UI01|src/main/resources/static/js/quan-tri-danh-muc.js:25|error() ẩn slot trống khi blur; nút Thêm Y441.67→419.67,clickđầu targetFORM/khôngPOST; clicklại lưu được. docs/evidence/gd32-xoa-han/create-diagnostic.json|Chưa sửa UI,ngoài cleanup|
|GD32R-NET01|src/main/resources/static/js/dia-chi-giao-hang.js:43|Tile OpenStreetMap net::ERR_NAME_NOT_RESOLVED trong browser-empty.json/requestFailed|Chưa sửa mạng ngoài|
