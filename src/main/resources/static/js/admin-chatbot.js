/**
 * Admin Chatbot Client - Trợ lý cửa hàng F6S Store (Giai đoạn 26)
 */
(() => {
    'use strict';

    const STORAGE_KEY = 'f6s_admin_chatbot_session_id';

    function getSessionId() {
        let sid = sessionStorage.getItem(STORAGE_KEY);
        if (!sid) {
            sid = 'adm_' + Date.now() + '_' + Math.random().toString(36).substring(2, 9);
            sessionStorage.setItem(STORAGE_KEY, sid);
        }
        return sid;
    }

    function getCsrf() {
        const tokenMeta = document.querySelector('meta[name="_csrf"]');
        const headerMeta = document.querySelector('meta[name="_csrf_header"]');
        return {
            token: tokenMeta ? tokenMeta.getAttribute('content') : '',
            header: headerMeta ? headerMeta.getAttribute('content') : 'X-CSRF-TOKEN'
        };
    }

    function escapeHtml(str) {
        if (!str) return '';
        const div = document.createElement('div');
        div.textContent = str;
        return div.innerHTML;
    }

    /**
     * Chuyển đổi Markdown đơn giản sang HTML an toàn
     */
    function formatMarkdown(text) {
        if (!text) return '';
        let escaped = escapeHtml(text);

        // 1. Format code: `code`
        escaped = escaped.replace(/`([^`]+)`/g, '<code>$1</code>');

        // 2. Format bold: **text**
        escaped = escaped.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');

        // 3. Format italic: *text*
        escaped = escaped.replace(/\*([^*]+)\*/g, '<em>$1</em>');

        // 4. Format links: [text](url)
        escaped = escaped.replace(/\[([^\]]+)\]\(([^)]+)\)/g, (match, linkText, url) => {
            // An toàn URL
            let safeUrl = url.trim();
            if (safeUrl.startsWith('/admin') || safeUrl.startsWith('http://') || safeUrl.startsWith('https://')) {
                return `<a href="${safeUrl}" class="admin-chat-link">${linkText}</a>`;
            }
            return linkText;
        });

        // 5. Format lists and paragraphs
        const lines = escaped.split('\n');
        let html = '';
        let inList = false;

        for (let line of lines) {
            line = line.trim();
            if (line.startsWith('- ') || line.startsWith('+ ') || line.startsWith('* ')) {
                if (!inList) {
                    html += '<ul class="mb-2 ps-3">';
                    inList = true;
                }
                html += `<li>${line.substring(2)}</li>`;
            } else if (/^\d+\.\s/.test(line)) {
                if (!inList) {
                    html += '<ol class="mb-2 ps-3">';
                    inList = true;
                }
                const content = line.replace(/^\d+\.\s/, '');
                html += `<li>${content}</li>`;
            } else {
                if (inList) {
                    html += inList === 'ol' ? '</ol>' : '</ul>';
                    inList = false;
                }
                if (line.length > 0) {
                    html += `<p class="mb-1">${line}</p>`;
                }
            }
        }
        if (inList) {
            html += inList === 'ol' ? '</ol>' : '</ul>';
        }

        return html;
    }

    document.addEventListener('DOMContentLoaded', () => {
        const container = document.getElementById('f6s-admin-chatbot-container');
        if (!container) return;

        const toggleBtn = document.getElementById('admin-chatbot-toggle-btn');
        const windowEl = document.getElementById('admin-chatbot-window');
        const closeBtn = document.getElementById('admin-chatbot-close-btn');
        const clearBtn = document.getElementById('admin-chatbot-clear-btn');
        const messagesEl = document.getElementById('admin-chatbot-messages');
        const quickRepliesEl = document.getElementById('admin-chatbot-quick-replies');
        const typingEl = document.getElementById('admin-chatbot-typing');
        const formEl = document.getElementById('admin-chatbot-form');
        const inputEl = document.getElementById('admin-chatbot-input');
        const sendBtn = document.getElementById('admin-chatbot-send-btn');

        let isSending = false;

        // Toggle mở/đóng cửa sổ chat
        toggleBtn.addEventListener('click', () => {
            const isHidden = windowEl.classList.contains('d-none');
            if (isHidden) {
                windowEl.classList.remove('d-none');
                toggleBtn.setAttribute('aria-expanded', 'true');
                scrollToBottom();
                setTimeout(() => inputEl.focus(), 150);
            } else {
                windowEl.classList.add('d-none');
                toggleBtn.setAttribute('aria-expanded', 'false');
            }
        });

        // Đóng chat bằng nút X
        closeBtn.addEventListener('click', () => {
            windowEl.classList.add('d-none');
            toggleBtn.setAttribute('aria-expanded', 'false');
            toggleBtn.focus();
        });

        // Đóng bằng phím Escape
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && !windowEl.classList.contains('d-none')) {
                windowEl.classList.add('d-none');
                toggleBtn.setAttribute('aria-expanded', 'false');
                toggleBtn.focus();
            }
        });

        // Xóa hội thoại
        clearBtn.addEventListener('click', async () => {
            if (!confirm('Bạn có chắc muốn xóa lịch sử trò chuyện trong phiên này?')) return;

            const sid = getSessionId();
            const csrf = getCsrf();

            try {
                await fetch('/admin/chatbot/clear', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        [csrf.header]: csrf.token
                    },
                    body: JSON.stringify({ sessionId: sid })
                });
            } catch (err) {
                console.error('Lỗi khi xóa phiên chat:', err);
            }

            // Tạo session id mới
            sessionStorage.removeItem(STORAGE_KEY);

            // Làm sạch tin nhắn về mặc định
            messagesEl.innerHTML = `
                <div class="admin-chat-msg admin-chat-msg-bot">
                    <div class="admin-chat-bubble">
                        <p class="mb-0">Hội thoại đã được làm mới. Tôi là Trợ lý cửa hàng F6S Store 🤖. Tôi có thể hỗ trợ tra cứu doanh thu, đơn hàng, tồn kho, sản phẩm bán chạy, khách VIP và hướng dẫn thao tác.</p>
                    </div>
                </div>
                <div id="admin-chatbot-quick-replies" class="admin-chatbot-chips" role="group" aria-label="Gợi ý nhanh">
                    <button type="button" class="admin-chat-chip" data-query="Doanh thu hôm nay">📊 Doanh thu hôm nay</button>
                    <button type="button" class="admin-chat-chip" data-query="Đơn chờ xử lý">⏳ Đơn chờ xử lý</button>
                    <button type="button" class="admin-chat-chip" data-query="Sản phẩm sắp hết hàng">⚠️ Sản phẩm sắp hết hàng</button>
                    <button type="button" class="admin-chat-chip" data-query="Top bán chạy tuần này">🏆 Top bán chạy tuần này</button>
                    <button type="button" class="admin-chat-chip" data-query="Khách chi tiêu nhiều nhất">🌟 Khách chi tiêu nhiều nhất</button>
                    <button type="button" class="admin-chat-chip" data-query="Tin nhắn mới">📩 Tin nhắn mới</button>
                    <button type="button" class="admin-chat-chip" data-query="Hướng dẫn thao tác POS">📖 Hướng dẫn thao tác POS</button>
                </div>
                <div id="admin-chatbot-typing" class="admin-chat-msg admin-chat-msg-bot d-none" aria-hidden="true">
                    <div class="admin-chat-bubble admin-chat-typing-bubble">
                        <span class="admin-dot"></span>
                        <span class="admin-dot"></span>
                        <span class="admin-dot"></span>
                    </div>
                </div>
            `;
            bindChipEvents();
        });

        // Bắt sự kiện form submit gửi tin nhắn
        formEl.addEventListener('submit', (e) => {
            e.preventDefault();
            sendMessage(inputEl.value);
        });

        // Bắt sự kiện click vào các chip gợi ý nhanh
        function bindChipEvents() {
            const chips = messagesEl.querySelectorAll('.admin-chat-chip');
            chips.forEach(chip => {
                chip.onclick = () => {
                    const query = chip.getAttribute('data-query');
                    if (query) {
                        sendMessage(query);
                    }
                };
            });
        }
        bindChipEvents();

        // Gửi tin nhắn
        async function sendMessage(text) {
            const trimmed = text ? text.trim() : '';
            if (!trimmed || isSending) return;

            isSending = true;
            inputEl.value = '';
            inputEl.disabled = true;
            sendBtn.disabled = true;

            // 1. Thêm bubble tin nhắn của người dùng
            appendUserMessage(trimmed);

            // 2. Hiện typing indicator
            showTyping(true);
            scrollToBottom();

            const sid = getSessionId();
            const csrf = getCsrf();

            try {
                const response = await fetch('/admin/chatbot/message', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        [csrf.header]: csrf.token
                    },
                    body: JSON.stringify({
                        message: trimmed,
                        sessionId: sid
                    })
                });

                if (response.status === 403) {
                    showTyping(false);
                    appendBotMessage("⛔ Bạn không có quyền truy cập vào Trợ lý quản trị cửa hàng (HTTP 403 Forbidden).", []);
                    return;
                }

                if (!response.ok) {
                    throw new Error('Lỗi máy chủ: ' + response.status);
                }

                const data = await response.json();
                showTyping(false);
                appendBotMessage(data.text, data.quickReplies || []);
            } catch (err) {
                console.error('Lỗi khi gọi Trợ lý cửa hàng:', err);
                showTyping(false);
                appendBotMessage("⚠️ Đã có sự cố kết nối tới hệ thống. Vui lòng thử lại sau giây lát.", []);
            } finally {
                isSending = false;
                inputEl.disabled = false;
                sendBtn.disabled = false;
                inputEl.focus();
                scrollToBottom();
            }
        }

        function appendUserMessage(text) {
            const msgDiv = document.createElement('div');
            msgDiv.className = 'admin-chat-msg admin-chat-msg-user';
            msgDiv.innerHTML = `<div class="admin-chat-bubble"><p class="mb-0">${escapeHtml(text)}</p></div>`;
            const currentTyping = document.getElementById('admin-chatbot-typing');
            messagesEl.insertBefore(msgDiv, currentTyping);
        }

        function appendBotMessage(text, quickReplies) {
            const msgDiv = document.createElement('div');
            msgDiv.className = 'admin-chat-msg admin-chat-msg-bot';
            msgDiv.innerHTML = `<div class="admin-chat-bubble">${formatMarkdown(text)}</div>`;
            const currentTyping = document.getElementById('admin-chatbot-typing');
            messagesEl.insertBefore(msgDiv, currentTyping);

            // Cập nhật gợi ý nhanh nếu có
            if (quickReplies && quickReplies.length > 0) {
                const chipsDiv = document.createElement('div');
                chipsDiv.className = 'admin-chatbot-chips';
                chipsDiv.setAttribute('role', 'group');
                chipsDiv.setAttribute('aria-label', 'Gợi ý nhanh');

                chipsDiv.innerHTML = quickReplies.map(q => {
                    return `<button type="button" class="admin-chat-chip" data-query="${escapeHtml(q)}">${escapeHtml(q)}</button>`;
                }).join('');

                messagesEl.insertBefore(chipsDiv, currentTyping);
                bindChipEvents();
            }
        }

        function showTyping(show) {
            const currentTyping = document.getElementById('admin-chatbot-typing');
            if (currentTyping) {
                if (show) {
                    currentTyping.classList.remove('d-none');
                } else {
                    currentTyping.classList.add('d-none');
                }
            }
        }

        function scrollToBottom() {
            setTimeout(() => {
                messagesEl.scrollTop = messagesEl.scrollHeight;
            }, 50);
        }
    });
})();
