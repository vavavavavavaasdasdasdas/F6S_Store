# 8 kịch bản bảo mật

| Mã / kịch bản | Status thực tế | Mong đợi | Kết quả |
|---|---|---|---|
|S01 khách vào quản lý|302|302|PASS|
|S02 user vào quản lý|403|403|PASS|
|S03 admin vào quản lý|200|200|PASS|
|S04 admin POST thiếu CSRF|403|403|PASS|
|S05 admin POST sai CSRF|403|403|PASS|
|S06 user POST có CSRF hợp lệ|403|403|PASS|
|S07 biến thể thiếu CSRF|403|403|PASS|
|S08 chat khách thiếu CSRF|403|403|PASS|
