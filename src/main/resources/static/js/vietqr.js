(() => {
    function draw(canvas,payload) {
        const qr=qrcodegen.QrCode.encodeText(payload,qrcodegen.QrCode.Ecc.MEDIUM),border=4;
        const scale=Math.max(4,Math.floor(300/(qr.size+border*2))),pixels=(qr.size+border*2)*scale;
        canvas.width=pixels;canvas.height=pixels;canvas.style.width=pixels+'px';
        const context=canvas.getContext('2d');context.imageSmoothingEnabled=false;
        context.fillStyle='#FFFFFF';context.fillRect(0,0,pixels,pixels);context.fillStyle='#000000';
        for(let y=0;y<qr.size;y++)for(let x=0;x<qr.size;x++)if(qr.getModule(x,y))context.fillRect((x+border)*scale,(y+border)*scale,scale,scale);
        canvas.dataset.payload=payload;
    }
    window.drawVietQr=draw;
    document.querySelectorAll('canvas[data-vietqr][data-payload]').forEach(canvas=>draw(canvas,canvas.dataset.payload));
})();
