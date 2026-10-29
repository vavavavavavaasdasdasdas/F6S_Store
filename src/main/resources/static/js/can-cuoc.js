(() => {
  'use strict';
  const dialog = document.getElementById('id-scanner');
  if (!dialog) return;
  const status = document.getElementById('id-scanner-status');
  const video = document.getElementById('id-video');
  const file = document.getElementById('id-image');
  const canvas = document.createElement('canvas');
  let stream, timer, reading = false, generation = 0;
  const invalid = 'Mã QR không phải căn cước công dân';
  const date = value => {
    if (!/^[0-9]{8}$/.test(value)) throw new Error(invalid);
    const d = Number(value.slice(0, 2)), m = Number(value.slice(2, 4)), y = Number(value.slice(4));
    const parsed = new Date(y, m - 1, d);
    if (y < 1900 || parsed.getFullYear() !== y || parsed.getMonth() !== m - 1 || parsed.getDate() !== d || parsed > new Date()) throw new Error(invalid);
    return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
  };
  function parse(text) {
    let parts = text.trim().split('|').map(s => s.trim());
    if (parts.length !== 6 && parts.length !== 7) throw new Error(invalid);
    // Six fields may omit the old identity number or the issue date.
    if (parts.length === 6 && /^[0-9]{8}$/.test(parts[2])) parts.splice(1, 0, '');
    const [id, old, name, birth, gender, address, issued] = parts;
    if (!/^[0-9]{12}$/.test(id) || !name || name.length > 100 || address.length > 255 || !['Nam', 'Nữ', 'Khác'].includes(gender)) throw new Error(invalid);
    const born = date(birth);
    if (issued) { date(issued); if (date(issued) < born) throw new Error(invalid); }
    return { soCccd: id, fullName: name, ngaySinh: born, gioiTinh: gender, address };
  }
  function stop() {
    generation++; clearTimeout(timer); stream?.getTracks().forEach(t => t.stop());
    stream = null; video.srcObject = null; video.hidden = true;
    canvas.width = canvas.height = 0;
  }
  function decoded(text) {
    const fields = parse(text); // Validate every field before modifying anything.
    for (const [key, value] of Object.entries(fields)) {
      const input = document.getElementById(key);
      input.value = value; input.dispatchEvent(new Event('input', { bubbles: true })); input.dispatchEvent(new Event('change', { bubbles: true }));
    }
    stop(); file.value = ''; status.textContent = 'Đã đọc căn cước. Kiểm tra thông tin rồi nhập số điện thoại và thư điện tử.';
  }
  function decode(source, width, height) {
    const scale = Math.min(1, 1800 / Math.max(width, height));
    canvas.width = Math.max(1, Math.round(width * scale)); canvas.height = Math.max(1, Math.round(height * scale));
    const context = canvas.getContext('2d', { willReadFrequently: true });
    context.drawImage(source, 0, 0, canvas.width, canvas.height);
    const pixels = context.getImageData(0, 0, canvas.width, canvas.height);
    return window.jsQR(pixels.data, pixels.width, pixels.height, { inversionAttempts: 'attemptBoth' });
  }
  document.getElementById('open-id-scanner').addEventListener('click', () => { status.textContent = ''; dialog.showModal(); });
  document.getElementById('id-scanner-close').addEventListener('click', () => dialog.close());
  dialog.addEventListener('close', () => { stop(); file.value = ''; });
  window.addEventListener('pagehide', stop);
  file.addEventListener('change', async () => {
    const selected = file.files[0]; if (!selected || reading) return;
    stop(); reading = true; const current = generation;
    let bitmap;
    try {
      if (selected.size > 10 * 1024 * 1024 || !['image/png', 'image/jpeg', 'image/webp'].includes(selected.type)) throw new Error('Chọn ảnh PNG, JPEG hoặc WebP tối đa 10 MB.');
      bitmap = await createImageBitmap(selected);
      if (!dialog.open || generation !== current) return;
      const qr = decode(bitmap, bitmap.width, bitmap.height);
      if (!qr) throw new Error('Không tìm thấy mã QR trong ảnh. Hãy chọn ảnh rõ hơn.');
      decoded(qr.data);
    } catch (error) { if (dialog.open && generation === current) status.textContent = error.message || invalid; }
    finally { bitmap?.close(); canvas.width = canvas.height = 0; file.value = ''; reading = false; }
  });
  document.getElementById('id-camera').addEventListener('click', async () => {
    stop(); const current = generation;
    if (!window.isSecureContext || !navigator.mediaDevices?.getUserMedia) { status.textContent = 'Camera cần HTTPS hoặc localhost. Bạn vẫn có thể tải ảnh mã QR.'; return; }
    try {
      const opened = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' }, audio: false });
      if (!dialog.open || current !== generation) { opened.getTracks().forEach(t => t.stop()); return; }
      stream = opened; video.srcObject = stream; video.hidden = false; await video.play();
      status.textContent = 'Đưa mã QR vào trước camera.';
      const frame = () => {
        if (!stream || !dialog.open || current !== generation) return;
        try { if (video.readyState >= 2) { const qr = decode(video, video.videoWidth, video.videoHeight); if (qr) { decoded(qr.data); return; } } }
        catch (_) { status.textContent = invalid; }
        timer = setTimeout(frame, 200);
      }; frame();
    } catch (_) { stop(); status.textContent = 'Không mở được camera hoặc chưa được cấp quyền. Bạn vẫn có thể tải ảnh mã QR.'; }
  });
  const email = document.getElementById('email'), username = document.getElementById('username');
  let usernameEdited = !!username.value, sequence = 0;
  username.addEventListener('input', () => { usernameEdited = !!username.value; });
  email.addEventListener('change', async () => {
    if (usernameEdited || !email.validity.valid || !email.value) return;
    const form = email.form, token = form.querySelector('input[name="_csrf"]');
    if (!token) return; const current = ++sequence;
    try {
      const response = await fetch('/admin/accounts/username-suggestion', { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: new URLSearchParams({ email: email.value, [token.name]: token.value }) });
      if (response.ok) { const data = await response.json(); if (sequence === current && !usernameEdited) username.value = data.username; }
    } catch (_) { /* The server also suggests a unique name when the form is saved. */ }
  });
})();
