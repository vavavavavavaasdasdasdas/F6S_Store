(() => {
'use strict';
 const money=value=>new Intl.NumberFormat('vi-VN').format(Number(value||0))+' ₫';
 const date=value=>new Date(/(?:Z|[+-]\d\d:\d\d)$/.test(value)?value:value+'+07:00');
 const time=value=>new Intl.DateTimeFormat('vi-VN',{hour:'2-digit',minute:'2-digit',timeZone:'Asia/Ho_Chi_Minh'}).format(date(value));
 const day=value=>new Intl.DateTimeFormat('vi-VN',{day:'2-digit',month:'2-digit',year:'numeric',timeZone:'Asia/Ho_Chi_Minh'}).format(date(value));
 function node(tag,cls,text){const el=document.createElement(tag);if(cls)el.className=cls;if(text!=null)el.textContent=text;return el;}
 function card(data,staff=false){
  const a=node('a','support-card');let target=data.url||data.productUrl;
  if(!/^\/(?:products|orders)\/[0-9]+$/.test(target||''))return node('span','','Thông tin tham khảo');a.href=staff&&target.startsWith('/orders/')?target.replace('/orders/','/admin/chat/order/'):target;
  const image=data.image||data.imageUrl;if(image&&/^\/(?:images|anh|uploads\/products)\//.test(image)){const img=node('img');img.src=image;img.alt=data.name||'Sản phẩm';img.loading='lazy';a.append(img);}
  a.append(node('strong','',data.name||data.code||'Thông tin'),node('span','support-card-price',money(data.price??data.finalPrice)));
  if(data.status)a.append(node('span','',data.status));return a;
 }
 function bubble(m,waiting=false,ownSender='KHACH'){
  const own=m.senderType===ownSender,row=node('article','support-message'+(own?' own':''));row.dataset.messageId=m.id||m.clientId;
  if(!own){const avatar=node('img','support-avatar');avatar.src='/images/logo.jpg';avatar.alt='';row.append(avatar);}
  const content=node('div','support-message-content');
  if(!own)content.append(node('p','support-sender',m.senderType==='TRO_LY'?'Trợ lý tự động':m.senderType==='NHAN_VIEN'?'Nhân viên cửa hàng':m.senderType==='KHACH'?'Khách hàng':'Hệ thống'));
  const body=node('div','support-bubble',m.content||'');
  if(m.attachmentUrl){const img=node('img','support-attachment');img.src=m.attachmentUrl;img.alt='Ảnh trong cuộc trò chuyện';img.loading='lazy';body.append(img);}
  if(m.payload){for(const data of Array.isArray(m.payload)?m.payload:[m.payload])body.append(card(data,ownSender==='NHAN_VIEN'));}
  content.append(body,node('time','support-message-meta',time(m.createdAt)+(own?' · '+(waiting?(m.failed?'Chưa gửi được':'Đang gửi'):m.seenAt?'Đã xem':'Đã gửi'):'')));
  row.append(content);return row;
 }
window.SupportChatView={node,card,bubble,money,time,day};
})();
