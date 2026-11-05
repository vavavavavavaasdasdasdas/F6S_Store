(() => {
 'use strict';
 const root=document.getElementById('f6s-chatbot-container');if(!root||location.pathname.startsWith('/admin'))return;
 const get=s=>root.querySelector(s),panel=get('#chatbot-window'),toggle=get('#chatbot-toggle-btn'),history=get('[data-chat-history]'),log=get('[data-chat-messages]'),input=get('#chatbot-input'),identity=get('[data-chat-identity]');
 const narrow=matchMedia('(max-width:575.98px)'),messages=new Map(),pending=new Map(),inertBefore=new Map();
 let ready=Promise.resolve(),thread=null,authenticated=false,opened=false,connected=false,client=null,loading=false,refreshAgain=false,hasMore=false,sending=false,lastTyping=0,retryTimer,orderPage=0,ordersMore=false,overflowBefore=null;
 const money=value=>new Intl.NumberFormat('vi-VN').format(Number(value||0))+' ₫';
 let supportCsrf=document.querySelector('meta[name=_csrf]')?.content;
 const csrf=()=>supportCsrf;
 const mask=text=>String(text||'').replace(/\b(?:\d[ -]*?){13,19}\b/g,'****-****-****').replace(/(?:mật khẩu|mat khau|password|pwd)\s*(?:(?:của tôi|cua toi|của mình|cua minh)\s*)?(?:là|la|:|=)?\s*(\S+)/gi,'mật khẩu [ĐÃ BẢO MẬT]');
 const date=value=>new Date(/(?:Z|[+-]\d\d:\d\d)$/.test(value)?value:value+'+07:00');
 const time=value=>new Intl.DateTimeFormat('vi-VN',{hour:'2-digit',minute:'2-digit',timeZone:'Asia/Ho_Chi_Minh'}).format(date(value));
 const day=value=>new Intl.DateTimeFormat('vi-VN',{day:'2-digit',month:'2-digit',year:'numeric',timeZone:'Asia/Ho_Chi_Minh'}).format(date(value));
 function node(tag,cls,text){const el=document.createElement(tag);if(cls)el.className=cls;if(text!=null)el.textContent=text;return el;}
 function error(message){get('[data-chat-error]').textContent=message||'';get('[data-chat-error]').hidden=!message;}
 async function request(url,{method='GET',body,signal}={}){
  const headers={Accept:'application/json'};let data=body;
  if(method!=='GET'){headers['X-CSRF-TOKEN']=csrf();if(!(body instanceof FormData)){headers['Content-Type']='application/json';data=JSON.stringify(body||{});}}
  const response=await fetch(url,{method,headers,body:data,signal,credentials:'same-origin'});
  let result;try{result=await response.json();}catch{result={};}
  if(response.ok&&url==='/api/support/session'&&method==='GET'&&result.csrfToken)supportCsrf=result.csrfToken;
  if(!response.ok){const ex=Error(result.message|| (response.status===403?'Phiên chat không hợp lệ hoặc đã hết hạn.':'Chưa xử lý được tin nhắn. Vui lòng thử lại.'));ex.status=response.status;if(response.status===403)get('[data-chat-reset]').hidden=false;throw ex;}
  return result;
 }
 // IndexedDB keeps the same client id across retries, including image blobs. Text is masked before persistence.
 const storage=new Promise(resolve=>{try{const r=indexedDB.open('f6s_support_outbox_v1',1);r.onupgradeneeded=()=>r.result.createObjectStore('messages',{keyPath:'key'});r.onsuccess=()=>resolve(r.result);r.onerror=()=>resolve(null);}catch{resolve(null);}});
 async function outbox(action,item){const db=await storage;if(!db)return action==='all'?[]:undefined;return new Promise(resolve=>{const tx=db.transaction('messages',action==='all'?'readonly':'readwrite'),store=tx.objectStore('messages');let r;if(action==='put')r=store.put(item);else if(action==='delete')r=store.delete(item.key);else r=store.getAll();r.onsuccess=()=>resolve(r.result);r.onerror=()=>resolve(action==='all'?[]:undefined);});}
 function badge(value){const count=Number(value||0),el=get('[data-chat-badge]');el.hidden=count===0;el.textContent=count>99?'99+':String(count);toggle.setAttribute('aria-label',count?'Mở hỗ trợ khách hàng, '+count+' tin chưa đọc':'Mở hỗ trợ khách hàng');const title=document.title.replace(/^\(\d+\)\s*/,'');document.title=(count?'('+count+') ':'')+title;}
 function status(){get('[data-chat-connection]').textContent=!navigator.onLine?'Mất mạng. Tin nhắn chờ sẽ được gửi lại khi có kết nối.':connected?'Đã kết nối trực tiếp':thread?'Đang dùng kết nối dự phòng, cập nhật mỗi 5 giây.':'Sẵn sàng hỗ trợ';}
 function presence(online){get('[data-chat-presence]').textContent=online?'Đang trực tuyến':'Sẽ phản hồi sớm';get('[data-chat-online-dot]').classList.toggle('online',Boolean(online));}
 function modal(){
  panel.setAttribute('aria-modal',String(opened&&narrow.matches));
  if(opened&&narrow.matches){if(overflowBefore===null){overflowBefore={overflow:document.documentElement.style.overflow,gutter:document.documentElement.style.scrollbarGutter};document.documentElement.style.overflow='hidden';document.documentElement.style.scrollbarGutter='auto';}for(const el of document.body.children){if(el===root||['SCRIPT','STYLE','LINK'].includes(el.tagName))continue;if(!inertBefore.has(el))inertBefore.set(el,el.inert);el.inert=true;}}
  else{for(const [el,value] of inertBefore)el.inert=value;inertBefore.clear();if(overflowBefore!==null){document.documentElement.style.overflow=overflowBefore.overflow;document.documentElement.style.scrollbarGutter=overflowBefore.gutter;overflowBefore=null;}}
 }
 narrow.addEventListener('change',modal);
 function bubble(m,waiting=false){const row=SupportChatView.bubble(m,waiting);const content=row.querySelector('.support-message-content');
  if(waiting&&m.failed){if(m.error)content.append(node('p','support-pending-error',m.error));const retry=node('button','support-retry','Thử gửi lại');retry.type='button';retry.addEventListener('click',()=>{const item=pending.get(m.clientId);if(item)item.permanent=false;flush();});const discard=node('button','support-retry','Bỏ tin chưa gửi');discard.type='button';discard.addEventListener('click',async()=>{const item=pending.get(m.clientId);if(item){pending.delete(m.clientId);await outbox('delete',item);render();}});content.append(retry,discard);}
  return row;
 }
 function render({older=false,previousHeight=0,newMessages=false}={}){
  const rows=[...messages.values()].sort((a,b)=>a.id-b.id),desired=[],existing=new Map([...log.children].map(el=>[el.dataset.chatKey,el]));let current='';
  const keep=(key,signature,build)=>{let el=existing.get(key);if(!el||el._supportSignature!==signature){el=build();el.dataset.chatKey=key;el._supportSignature=signature;}desired.push(el);};
  for(const m of rows){const label=day(m.createdAt);if(label!==current){keep('day:'+m.id,label,()=>node('p','support-day',label));current=label;}keep('message:'+m.id,JSON.stringify(m),()=>bubble(m));}
  for(const p of pending.values()){const m={...p.data,senderType:'KHACH',createdAt:p.createdAt,failed:p.failed,error:p.error};keep('pending:'+p.data.clientId,JSON.stringify(m),()=>bubble(m,true));}
  if(older)log.setAttribute('aria-live','off');
  let anchor=log.firstChild;for(const el of desired){if(el===anchor)anchor=anchor.nextSibling;else log.insertBefore(el,anchor);}while(anchor){const next=anchor.nextSibling;anchor.remove();anchor=next;}
  if(older)requestAnimationFrame(()=>log.setAttribute('aria-live','polite'));
  get('[data-chat-quick]').hidden=rows.length>0||pending.size>0;get('[data-chat-older]').hidden=!hasMore;
  if(older)history.scrollTop=history.scrollHeight-previousHeight;else if(newMessages)history.scrollTop=history.scrollHeight;
 }
 // Read receipts are form parameters so the upper bound is exactly the displayed history snapshot.
 async function readVisible(){if(!thread||!opened)return;const rows=[...messages.values()];if(!rows.some(m=>m.senderType!=='KHACH'&&!m.seenAt))return;const through=Math.max(...rows.map(m=>m.id));const response=await fetch('/api/support/'+thread.id+'/read?through='+through,{method:'POST',headers:{'X-CSRF-TOKEN':csrf(),Accept:'application/json'}});if(response.ok){rows.filter(m=>m.senderType!=='KHACH'&&m.id<=through).forEach(m=>m.seenAt=new Date().toISOString());badge(0);}}
 async function refresh(older=false){
  if(!thread)return;if(loading){if(!older)refreshAgain=true;return;}loading=true;
  const activeId=thread.id,oldIds=[...messages.keys()],oldMax=Math.max(0,...oldIds),previousHeight=history.scrollHeight;
  const before=older&&oldIds.length?'?before='+Math.min(...oldIds):'';
  try{
   const data=await request('/api/support/'+activeId+'/messages'+before);if(thread?.id!==activeId)return;
   if(!older&&oldIds.length&&data.messages.length===30&&data.messages.every(m=>!messages.has(m.id))){messages.clear();hasMore=data.hasMore;}
   if(older||messages.size===0)hasMore=data.hasMore;
   for(const m of data.messages){const prior=messages.get(m.id);if(prior?.seenAt&&!m.seenAt)m.seenAt=prior.seenAt;messages.set(m.id,m);if(m.clientId&&pending.has(m.clientId)){const p=pending.get(m.clientId);pending.delete(m.clientId);await outbox('delete',p);}}
   thread=data.conversation;badge(thread.unread);presence(thread.online);get('[data-chat-typing]').textContent=data.staffTyping?'Nhân viên đang nhập…':'';
   clearTimeout(retryTimer);render({older,previousHeight,newMessages:!older&&Math.max(0,...messages.keys())>oldMax});await readVisible();if(![...pending.values()].some(p=>p.failed))error('');
  }catch(ex){error(ex.message);if(ex.status!==403){clearTimeout(retryTimer);retryTimer=setTimeout(()=>{if(thread?.id===activeId)refresh();},5000);}}finally{loading=false;if(refreshAgain){refreshAgain=false;refresh();}}
 }
 function connect(){
  if(!thread||client)return;
  client=new StompJs.Client({beforeConnect:async()=>{try{const info=await request('/api/support/session');if(!info.conversation||info.conversation.id!==thread?.id){client?.deactivate();connected=false;messages.clear();pending.clear();thread=null;render();if(opened){identity.hidden=false;get('[data-chat-compose]').hidden=true;}return;}client.connectHeaders={'X-CSRF-TOKEN':csrf()};}catch{client?.deactivate();connected=false;status();}},webSocketFactory:()=>new SockJS('/ws/support'),connectHeaders:{'X-CSRF-TOKEN':csrf()},debug:()=>{},heartbeatIncoming:10000,heartbeatOutgoing:10000,reconnectDelay:1000,maxReconnectDelay:30000,reconnectTimeMode:StompJs.ReconnectionTimeMode.EXPONENTIAL,connectionTimeout:8000,
   onConnect:()=>{if(!thread)return;connected=true;status();client.subscribe('/topic/support-presence',frame=>{try{presence(JSON.parse(frame.body).online);}catch{}});client.subscribe('/topic/support/'+thread.id,message=>{try{const event=JSON.parse(message.body);if(event.type==='TYPING'){if(event.senderType==='NHAN_VIEN'){get('[data-chat-typing]').textContent='Nhân viên đang nhập…';setTimeout(()=>get('[data-chat-typing]').textContent='',6000);}}else refresh();}catch{refresh();}});refresh();flush();},
   onWebSocketClose:()=>{connected=false;status();},onWebSocketError:()=>{connected=false;status();},onStompError:()=>{connected=false;status();client?.forceDisconnect();}
  });client.activate();status();
 }
 async function adopt(openedThread){
  if(thread?.id!==openedThread.id){client?.deactivate();client=null;messages.clear();pending.clear();hasMore=false;}
  thread=openedThread;identity.hidden=true;history.hidden=false;get('[data-chat-compose]').hidden=false;get('[data-chat-orders]').hidden=!authenticated;
  get('[data-chat-product]').hidden=!/^\/products\/[0-9]+\/?$/.test(location.pathname);
  for(const p of await outbox('all'))if(p.scope===thread.clientScope)pending.set(p.data.clientId,p);
  presence(thread.online);badge(thread.unread);connect();await refresh();flush();
 }
 async function bootstrap(){
  try{const info=await request('/api/support/session');authenticated=info.authenticated;if(info.mergePending){const target=await request('/api/support/session',{method:'POST',body:{}});for(const item of await outbox('all'))if(item.scope===info.mergeFromScope&&item.scope!==target.clientScope){const migrated={...item,scope:target.clientScope,conversationId:target.id,key:target.clientScope+':'+item.data.clientId};await outbox('put',migrated);await outbox('delete',item);}await adopt(target);}else if(info.conversation)await adopt(info.conversation);else status();}
  catch(ex){error(ex.message);status();}
 }
 async function open(){
  opened=true;panel.hidden=false;toggle.setAttribute('aria-expanded','true');modal();await ready;
  if(!thread){
   if(authenticated){try{await adopt(await request('/api/support/session',{method:'POST',body:{}}));}catch(ex){error(ex.message);}}
   else{identity.hidden=false;history.hidden=true;get('[data-chat-compose]').hidden=true;}
  }else await refresh();
  (identity.hidden?input:get('#support-name')).focus();history.scrollTop=history.scrollHeight;
 }
 function close(){opened=false;panel.hidden=true;toggle.setAttribute('aria-expanded','false');modal();toggle.focus();}
 toggle.addEventListener('click',()=>opened?close():open());get('[data-chat-close]').addEventListener('click',close);
 panel.addEventListener('keydown',event=>{if(event.key==='Escape'){event.preventDefault();close();}if(event.key==='Tab'&&narrow.matches){const controls=[...panel.querySelectorAll('button,input,textarea,a,select,[tabindex="0"]')].filter(e=>!e.disabled&&e.getClientRects().length);const first=controls[0],last=controls.at(-1);if(event.shiftKey&&document.activeElement===first){event.preventDefault();last?.focus();}else if(!event.shiftKey&&document.activeElement===last){event.preventDefault();first?.focus();}}});
 identity.addEventListener('submit',async event=>{event.preventDefault();const button=identity.querySelector('button');button.disabled=true;get('#support-identity-error').textContent='';try{await adopt(await request('/api/support/session',{method:'POST',body:{name:get('#support-name').value.trim(),contact:get('#support-contact').value.trim()}}));input.focus();}catch(ex){get('#support-identity-error').textContent=ex.message;get('#support-name').focus();}finally{button.disabled=false;}});
 async function queue(type,extra={},file=null){
  if(!thread){await open();return;}const content=mask(type==='VAN_BAN'?input.value:extra.content||'').trim();if(type==='VAN_BAN'&&!content)return;
  const data={clientId:crypto.randomUUID(),type,content,...extra},item={key:thread.clientScope+':'+data.clientId,scope:thread.clientScope,conversationId:thread.id,data,file,createdAt:new Date().toISOString()};
  await outbox('put',item);pending.set(data.clientId,item);if(type==='VAN_BAN'){input.value='';resize();}render({newMessages:true});flush();
 }
 async function flush(){
  if(sending||!thread||!navigator.onLine)return;sending=true;
  try{for(const item of [...pending.values()]){if(item.permanent)continue;
   try{let body=item.data;if(item.file){body=new FormData();for(const [key,value] of Object.entries(item.data))if(value!=null)body.set(key,value);body.set('file',item.file);}
    const m=await request('/api/support/'+thread.id+'/messages',{method:'POST',body});messages.set(m.id,m);pending.delete(item.data.clientId);await outbox('delete',item);render({newMessages:true});error('');
   }catch(ex){item.failed=true;item.error=ex.message;item.permanent=[400,403,413,415].includes(ex.status);await outbox('put',item);error(ex.message);render();if(!item.permanent)break;}
  }}finally{sending=false;refresh();}
 }
 function resize(){input.style.height='auto';input.style.height=Math.min(120,input.scrollHeight+2)+'px';input.style.overflowY=input.scrollHeight>118?'auto':'hidden';const counter=get('#support-character-count');counter.hidden=input.value.length<1800;counter.textContent=input.value.length+'/2000 ký tự';}
 input.addEventListener('input',()=>{resize();if(thread&&Date.now()-lastTyping>2000){lastTyping=Date.now();if(connected)client.publish({destination:'/app/support/'+thread.id+'/typing',body:'{}'});else request('/api/support/'+thread.id+'/typing',{method:'POST'}).catch(()=>{});}});
 input.addEventListener('keydown',event=>{if(event.key==='Enter'&&!event.shiftKey&&!event.isComposing){event.preventDefault();queue('VAN_BAN');}});get('#chatbot-send-btn').addEventListener('click',()=>queue('VAN_BAN'));
 root.querySelectorAll('[data-chat-query]').forEach(button=>button.addEventListener('click',()=>{input.value=button.dataset.chatQuery;queue('VAN_BAN');}));
 root.querySelectorAll('[data-chat-human]').forEach(button=>button.addEventListener('click',async()=>{if(!thread){open();return;}try{await request('/api/support/'+thread.id+'/human',{method:'POST'});refresh();}catch(ex){error(ex.message);}}));
 get('[data-chat-older]').addEventListener('click',()=>refresh(true));history.addEventListener('scroll',()=>{if(history.scrollTop<16&&hasMore)refresh(true);});
 get('[data-chat-attach]').addEventListener('click',()=>get('[data-chat-file]').click());get('[data-chat-file]').addEventListener('change',event=>{const file=event.target.files?.[0];if(!file)return;if(file.size>3*1024*1024)error('Ảnh tối đa 3 MB.');else if(!['image/jpeg','image/png','image/webp'].includes(file.type))error('Chỉ chấp nhận ảnh JPG, PNG hoặc WEBP.');else queue('ANH',{},file);event.target.value='';});
 get('[data-chat-product]').addEventListener('click',()=>queue('SAN_PHAM',{productId:Number(location.pathname.split('/')[2])}));
 const picker=get('[data-chat-order-picker]'),order=get('[data-chat-order]');
 async function loadOrders(){try{const data=await request('/api/support/orders?page='+orderPage);order.replaceChildren(new Option('Chọn đơn hàng',''),...data.content.map(o=>new Option(o.code+' · '+money(o.total)+' · '+o.status,String(o.id))));order.dispatchEvent(new Event('combo:sync'));ordersMore=!data.last;get('[data-chat-order-prev]').disabled=orderPage===0;get('[data-chat-order-next]').disabled=!ordersMore;get('[data-chat-order-page]').textContent='Trang '+(orderPage+1);get('[data-chat-order-message]').textContent=data.content.length?'':'Bạn chưa có đơn hàng.';}catch(ex){get('[data-chat-order-message]').textContent=ex.message;}}
 get('[data-chat-orders]').addEventListener('click',()=>{orderPage=0;picker.showModal();loadOrders();});get('[data-chat-order-prev]').addEventListener('click',()=>{if(orderPage>0){orderPage--;loadOrders();}});get('[data-chat-order-next]').addEventListener('click',()=>{if(ordersMore){orderPage++;loadOrders();}});get('[data-chat-order-cancel]').addEventListener('click',()=>picker.close());picker.addEventListener('close',()=>get('[data-chat-orders]').focus());get('[data-chat-order-send]').addEventListener('click',()=>{if(!order.value){get('[data-chat-order-message]').textContent='Vui lòng chọn đơn hàng.';return;}queue('DON_HANG',{orderId:Number(order.value)});picker.close();});
 get('[data-chat-reset]').addEventListener('click',async()=>{await request('/api/support/reset-session',{method:'POST'});client?.deactivate();client=null;thread=null;messages.clear();pending.clear();error('');get('[data-chat-reset]').hidden=true;await bootstrap();if(opened)open();});
 setInterval(()=>{if(thread&&!connected)refresh();if([...pending.values()].some(p=>!p.permanent))flush();},5000);
 window.addEventListener('online',()=>{status();if(thread){refresh();flush();}else{ready=bootstrap();ready.then(()=>{if(opened)open();});}});window.addEventListener('offline',status);
 document.addEventListener('visibilitychange',()=>{if(!document.hidden&&thread)refresh();});
 ready=bootstrap();if(new URLSearchParams(location.search).get('support')==='open')ready.then(open);
})();
