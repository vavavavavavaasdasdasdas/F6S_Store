# Thư viện chat — Giai đoạn 28

Lấy từ gói npm đúng phiên bản; chạy cục bộ, không CDN. Giữ tệp giấy phép cạnh bundle trong `src/main/resources/static/js/vendor/`.

| Tệp | Gói | Phiên bản | Giấy phép | SHA-256 |
|---|---|---|---|---|
| stomp-7.2.1.min.js | [@stomp/stompjs](https://github.com/stomp-js/stompjs) | 7.2.1 | Apache-2.0 | `e5d3a5f14d400048501f502f4ebe8eb48dcee6a61eb3fae0c4791ce6c97469d3` |
| sockjs-1.6.1.min.js | [sockjs-client](https://github.com/sockjs/sockjs-client) | 1.6.1 | MIT | `1e0f37d073666196b0848fd7d80d525268d5adf47ea1ec459632f05bbf8e1f54` |

Spring WebSocket/Security dùng phiên bản do Spring Boot trong pom quản lý; bổ sung starter-websocket và security-messaging. CSRF của CONNECT dùng interceptor chuẩn Spring Security (XOR token), quyền kênh kiểm tra riêng. [Tài liệu chính thức](https://docs.spring.io/spring-security/reference/servlet/integrations/websocket.html) giải thích SockJS transport cần ngoại lệ CSRF hẹp trong khi CONNECT vẫn có CSRF.

Không đưa ảnh chat vào static. Không tải ảnh lên nhà cung cấp AI. Kiểm thử chỉ RULES và mock ChatbotEngine, không gọi AI thật.
