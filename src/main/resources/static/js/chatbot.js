/**
 * F6S Chatbot Client - Giai đoạn 25
 */
(() => {
    'use strict';

    const STORAGE_KEY = 'f6s_chatbot_session_id';

    function getSessionId() {
        let sid = localStorage.getItem(STORAGE_KEY);
        if (!sid) {
            sid = 'cs_' + Date.now() + '_' + Math.random().toString(36).substring(2, 9);
            localStorage.setItem(STORAGE_KEY, sid);
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

    document.addEventListener('DOMContentLoaded', () => {
        const container = document.getElementById('f6s-chatbot-container');
        if (!container) return;

        const toggleBtn = document.getElementById('chatbot-toggle-btn');
        const windowEl = document.getElementById('chatbot-window');
        const closeBtn = document.getElementById('chatbot-close-btn');
        const clearBtn = document.getElementById('chatbot-clear-btn');
        const messagesEl = document.getElementById('chatbot-messages');
        const typingEl = document.getElementById('chatbot-typing');
        const inputEl = document.getElementById('chatbot-input');
        const sendBtn = document.getElementById('chatbot-send-btn');

        // Ticket modal elements
        const ticketModal = document.getElementById('chatbot-ticket-modal');
        const ticketForm = document.getElementById('chatbot-ticket-form');
        const ticketClose = document.getElementById('chatbot-ticket-close');
        const ticketCancel = document.getElementById('chatbot-ticket-cancel');

        let isTyping = false;

        // Toggle Chat Window
        toggleBtn.addEventListener('click', () => {
            const isHidden = windowEl.classList.contains('d-none');
            if (isHidden) {
                windowEl.classList.remove('d-none');
                toggleBtn.setAttribute('aria-expanded', 'true');
                inputEl.focus();
                scrollToBottom();
            } else {
                windowEl.classList.add('d-none');
                toggleBtn.setAttribute('aria-expanded', 'false');
            }
        });

        closeBtn.addEventListener('click', () => {
            windowEl.classList.add('d-none');
            toggleBtn.setAttribute('aria-expanded', 'false');
            toggleBtn.focus();
        });

        // Clear Chat
        clearBtn.addEventListener('click', async () => {
            if (!confirm('Bạn có chắc muốn xóa lịch sử cuộc trò chuyện này không?')) return;
            const sid = getSessionId();
            const csrf = getCsrf();
            try {
                await fetch('/api/chatbot/clear', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        [csrf.header]: csrf.token
                    },
                    body: JSON.stringify({ sessionId: sid })
                });
            } catch (ignored) {}

            localStorage.removeItem(STORAGE_KEY);
            // Giữ lại tin chào mặc định
            messagesEl.innerHTML = `
                <div class="chatbot-msg chatbot-msg-bot">
                    <div class="chatbot-msg-bubble">
                        <p class="mb-0">Đã xóa cuộc trò chuyện. Mình có thể giúp gì tiếp cho bạn?</p>
                    </div>
                </div>
                <div id="chatbot-quick-replies" class="chatbot-quick-replies" role="group" aria-label="Gợi ý nhanh">
                    <button type="button" class="chatbot-chip" data-query="Tìm giày">🔍 Tìm giày</button>
                    <button type="button" class="chatbot-chip" data-query="Tư vấn size">📏 Tư vấn size</button>
                    <button type="button" class="chatbot-chip" data-query="Chính sách đổi trả">🔄 Chính sách đổi trả</button>
                    <button type="button" class="chatbot-chip" data-query="Kiểm tra đơn hàng">📦 Kiểm tra đơn hàng</button>
                    <button type="button" class="chatbot-chip" data-query="Nói chuyện với nhân viên">💬 Nói chuyện với nhân viên</button>
                </div>
            `;
            messagesEl.appendChild(typingEl);
        });

        // Quick replies delegation
        messagesEl.addEventListener('click', (e) => {
            const chip = e.target.closest('.chatbot-chip');
            if (chip) {
                const query = chip.getAttribute('data-query');
                if (query) {
                    sendUserMessage(query);
                }
            }
        });

        // Send message event
        sendBtn.addEventListener('click', () => {
            const text = inputEl.value.trim();
            if (text) {
                sendUserMessage(text);
                inputEl.value = '';
                inputEl.style.height = 'auto';
            }
        });

        // Enter to send, Shift+Enter for newline
        inputEl.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                const text = inputEl.value.trim();
                if (text) {
                    sendUserMessage(text);
                    inputEl.value = '';
                    inputEl.style.height = 'auto';
                }
            }
        });

        // Auto resize input
        inputEl.addEventListener('input', () => {
            inputEl.style.height = 'auto';
            inputEl.style.height = Math.min(inputEl.scrollHeight, 80) + 'px';
        });

        // Ticket modal handlers
        function showTicketModal() {
            ticketModal.classList.remove('d-none');
            document.getElementById('ticket-hoten').focus();
        }

        function hideTicketModal() {
            ticketModal.classList.add('d-none');
        }

        if (ticketClose) ticketClose.addEventListener('click', hideTicketModal);
        if (ticketCancel) ticketCancel.addEventListener('click', hideTicketModal);

        if (ticketForm) {
            ticketForm.addEventListener('submit', async (e) => {
                e.preventDefault();
                const hoTen = document.getElementById('ticket-hoten').value.trim();
                const lienHe = document.getElementById('ticket-lienhe').value.trim();
                const noiDung = document.getElementById('ticket-noidung').value.trim();
                if (!hoTen || !lienHe || !noiDung) return;

                const csrf = getCsrf();
                try {
                    const res = await fetch('/api/chatbot/ticket', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                            [csrf.header]: csrf.token
                        },
                        body: JSON.stringify({
                            hoTen: hoTen,
                            lienHe: lienHe,
                            noiDung: noiDung,
                            sessionId: getSessionId()
                        })
                    });
                    const data = await res.json();
                    hideTicketModal();
                    if (data.success) {
                        appendBotMessage(data.message);
                    } else {
                        appendBotMessage('Không thể tạo phiếu hỗ trợ, vui lòng thử lại sau.');
                    }
                } catch (err) {
                    hideTicketModal();
                    appendBotMessage('Có lỗi xảy ra khi tạo phiếu hỗ trợ. Vui lòng thử lại sau.');
                }
            });
        }

        // Core Send function
        async function sendUserMessage(text) {
            if (isTyping) return;

            // Render tin nhắn của User (đã escape)
            appendUserMessage(text);
            showTyping();

            const sid = getSessionId();
            const csrf = getCsrf();

            try {
                const res = await fetch('/api/chatbot/message', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        [csrf.header]: csrf.token
                    },
                    body: JSON.stringify({
                        message: text,
                        sessionId: sid
                    })
                });

                hideTyping();

                if (!res.ok) {
                    appendBotMessage('Xin lỗi, hệ thống đang bận. Vui lòng thử lại sau giây lát!');
                    return;
                }

                const data = await res.json();
                appendBotResponse(data);

                if (data.suggestSupport) {
                    showTicketModal();
                }

            } catch (err) {
                hideTyping();
                appendBotMessage('Không thể kết nối tới máy chủ. Vui lòng kiểm tra lại kết nối!');
            }
        }

        function appendUserMessage(text) {
            const msgEl = document.createElement('div');
            msgEl.className = 'chatbot-msg chatbot-msg-user';
            msgEl.innerHTML = `<div class="chatbot-msg-bubble">${escapeHtml(text)}</div>`;
            messagesEl.insertBefore(msgEl, typingEl);
            scrollToBottom();
        }

        function appendBotMessage(text) {
            const msgEl = document.createElement('div');
            msgEl.className = 'chatbot-msg chatbot-msg-bot';
            msgEl.innerHTML = `<div class="chatbot-msg-bubble"><p class="mb-0" style="white-space: pre-line;">${escapeHtml(text)}</p></div>`;
            messagesEl.insertBefore(msgEl, typingEl);
            scrollToBottom();
        }

        function appendBotResponse(data) {
            const msgEl = document.createElement('div');
            msgEl.className = 'chatbot-msg chatbot-msg-bot';

            let bubbleContent = '';
            if (data.text) {
                bubbleContent += `<p class="mb-1" style="white-space: pre-line;">${escapeHtml(data.text)}</p>`;
            }

            // Render Product Cards
            if (data.products && data.products.length > 0) {
                bubbleContent += '<div class="chatbot-products-carousel">';
                data.products.forEach(p => {
                    const priceFormatted = new Intl.NumberFormat('vi-VN').format(p.finalPrice) + ' đ';
                    const origFormatted = p.discountPercent > 0 ? new Intl.NumberFormat('vi-VN').format(p.originalPrice) + ' đ' : '';

                    bubbleContent += `
                        <a href="${escapeHtml(p.productUrl)}" class="chatbot-product-card" target="_blank" rel="noopener">
                            <img src="${escapeHtml(p.imageUrl)}" alt="${escapeHtml(p.name)}" class="chatbot-product-thumb" onerror="this.src='/images/default-shoe.png'">
                            <div class="chatbot-product-info">
                                <div class="chatbot-product-title" title="${escapeHtml(p.name)}">${escapeHtml(p.name)}</div>
                                <div>
                                    <span class="chatbot-product-price">${priceFormatted}</span>
                                    ${p.discountPercent > 0 ? `<span class="chatbot-product-orig-price">${origFormatted}</span>` : ''}
                                </div>
                                <div class="chatbot-product-stock">Còn ${p.stock} sản phẩm</div>
                            </div>
                        </a>
                    `;
                });
                bubbleContent += '</div>';
            }

            // If require login, render login button
            if (data.requireLogin) {
                bubbleContent += `
                    <div class="mt-2 text-center">
                        <a href="/login" class="btn btn-sm btn-primary">Đăng nhập ngay</a>
                    </div>
                `;
            }

            msgEl.innerHTML = `<div class="chatbot-msg-bubble">${bubbleContent}</div>`;
            messagesEl.insertBefore(msgEl, typingEl);

            // Render Quick Replies
            if (data.quickReplies && data.quickReplies.length > 0) {
                const chipsEl = document.createElement('div');
                chipsEl.className = 'chatbot-quick-replies';
                data.quickReplies.forEach(qr => {
                    const chip = document.createElement('button');
                    chip.type = 'button';
                    chip.className = 'chatbot-chip';
                    chip.setAttribute('data-query', qr);
                    chip.textContent = qr;
                    chipsEl.appendChild(chip);
                });
                messagesEl.insertBefore(chipsEl, typingEl);
            }

            scrollToBottom();
        }

        function showTyping() {
            isTyping = true;
            typingEl.classList.remove('d-none');
            scrollToBottom();
        }

        function hideTyping() {
            isTyping = false;
            typingEl.classList.add('d-none');
        }

        function scrollToBottom() {
            setTimeout(() => {
                messagesEl.scrollTop = messagesEl.scrollHeight;
            }, 50);
        }
    });
})();
