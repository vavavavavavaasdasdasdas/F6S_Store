# Thư viện bổ sung — Giai đoạn 4

| Thư viện | Phiên bản | Giấy phép | Vị trí / mục đích |
|---|---|---|---|
| jsQR | 1.4.0 | Apache-2.0 | `src/main/resources/static/js/vendor/jsQR-1.4.0.js`, giải mã camera/ảnh trong trình duyệt |
| Spring Boot Starter Mail | 4.1.1 (theo parent Spring Boot) | Apache-2.0 | Dependency Maven, gửi MIME UTF-8 bằng JavaMailSender |
| GreenMail | 2.1.15, chỉ scope test | Apache-2.0 | SMTP giả trong test tích hợp, không gửi thư tới hộp thư thật |

jsQR được tải từ gói npm phiên bản cố định, đã kiểm tra SHA-512 tarball khớp integrity của registry. Runtime tham chiếu file local, không dùng CDN. Bản giấy phép đầy đủ nằm cạnh JS tại `jsQR-1.4.0.LICENSE`; thông tin tác giả giữ nguyên trong bản phân phối.

- [Nguồn jsQR](https://github.com/cozmo/jsQR)
- [Metadata npm 1.4.0](https://registry.npmjs.org/jsqr/1.4.0)
- [Giấy phép jsQR](https://github.com/cozmo/jsQR/blob/master/LICENSE)
- [Tài liệu Spring Boot Mail](https://docs.spring.io/spring-boot/reference/io/email.html)
- [GreenMail](https://greenmail-mail-test.github.io/greenmail/); giấy phép được đối chiếu POM `greenmail-parent:2.1.15`.

SHA-256 của file JS đã đưa vào repo:

```
bc40c8a15196236b2314db0856f72ca0b49980cd5413b8c852a7349f5fee0859
```

Integrity SHA-512 của tarball:

```
sha512-dxLob7q65Xg2DvstYkRpkYtmKm2sPJ9oFhrhmudT1dZvNFFTlroai3AWSpLey/w5vMcLBXRgOJsbXpdN9HzU/A==
```

qrcode 8.2/Pillow 13.0.0 chỉ được cài trong thư mục kiểm thử bị ignore để tạo ảnh dữ liệu GIẢ, không thêm dependency runtime hay triển khai lên website.
