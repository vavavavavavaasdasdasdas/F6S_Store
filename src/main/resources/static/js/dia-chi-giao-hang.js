(() => {
 const money=value=>new Intl.NumberFormat('vi-VN',{maximumFractionDigits:2}).format(Number(value||0))+' đ';
 const text=(root,selector,value)=>root.querySelectorAll(selector).forEach(el=>el.textContent=value||'');
 let configuration;
 async function settings(){if(!configuration)configuration=fetch('/shipping/config',{headers:{Accept:'application/json'}}).then(r=>{if(!r.ok)throw Error('Chưa tải được cấu hình bản đồ.');return r.json();}).catch(error=>{configuration=null;throw error;});return configuration;}
 function bindDeliveryAddress(root){
  if(!root||root.dataset.deliveryBound)return;root.dataset.deliveryBound='true';
  const form=root.closest('form');if(!form)return;
  const province=root.querySelector('[data-delivery-province]'),ward=root.querySelector('[data-delivery-ward]'),street=root.querySelector('[data-delivery-street]');
  const lat=root.querySelector('[data-delivery-latitude]'),lng=root.querySelector('[data-delivery-longitude]'),source=root.querySelector('[data-delivery-source]'),message=root.querySelector('[data-location-message]');
  const mapBox=root.querySelector('[data-delivery-map]'),locate=root.querySelector('[data-use-location]');
  const voucher=form.elements.voucherCode,submits=[...form.querySelectorAll('[data-delivery-submit]')].map(button=>({button,disabled:button.disabled}));
  let map,marker,quoteRequest,wardRequest,reverseRequest,timer,revision=0,wardRevision=0,reverseRevision=0,appliedVoucher='';
  const csrf=()=>form.querySelector('[name=_csrf]')?.value||document.querySelector('meta[name=_csrf]')?.content;
  const sync=field=>field.dispatchEvent(new Event('combo:sync'));
  const dirty=()=>street.dispatchEvent(new Event('input',{bubbles:true}));
  function pointLabel(){text(root,'[data-delivery-point]',lat.value&&lng.value?'Điểm giao: '+lat.value+', '+lng.value+(source.value==='TAM_PHUONG_XA'?' · Tâm phường/xã':' · Vị trí ghim; chọn Dùng tâm phường/xã nếu muốn tính theo địa chỉ.'):'Chưa có vị trí ghim.');}
  function busy(value){root.setAttribute('aria-busy',String(value));submits.forEach(({button,disabled})=>button.disabled=value||disabled||Boolean(form.dataset.submitting));}
  async function jsonPost(url,data,signal){const token=csrf();if(token)data.set('_csrf',token);const response=await fetch(url,{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded',Accept:'application/json'},body:data,signal});if(!response.headers.get('content-type')?.includes('json'))throw Error('Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.');const result=await response.json();if(!response.ok)throw Error(result.message||'Chưa xử lý được địa chỉ. Vui lòng kiểm tra lại.');return result;}
  async function loadWards(selected=''){
   wardRequest?.abort();const current=++wardRevision;wardRequest=new AbortController();
   ward.replaceChildren(new Option(province.value?'Đang tải Phường/Xã…':'Chọn Tỉnh/Thành phố trước',''));sync(ward);
   if(!province.value)return;
   try{const response=await fetch('/shipping/wards?provinceId='+encodeURIComponent(province.value),{signal:wardRequest.signal,headers:{Accept:'application/json'}});if(!response.ok)throw Error('Chưa tải được Phường/Xã. Vui lòng chọn lại tỉnh.');const list=await response.json();if(current!==wardRevision)return;
    ward.replaceChildren(new Option('Chọn Phường/Xã',''),...list.map(w=>new Option(w.name,String(w.id))));ward.value=String(selected);sync(ward);
   }catch(error){if(error.name!=='AbortError'&&current===wardRevision){ward.replaceChildren(new Option('Chọn Phường/Xã',''));sync(ward);message.textContent=error.message;}}
  }
  function drawPoint(){
   pointLabel();if(!map)return;
   if(!lat.value||!lng.value){if(marker){marker.remove();marker=null;}return;}
   const p=[Number(lat.value),Number(lng.value)];
   if(!marker){marker=L.marker(p,{draggable:true,keyboard:true,title:'Vị trí giao hàng, kéo để chỉnh',alt:'Ghim vị trí giao hàng'}).addTo(map);marker.on('dragend',()=>{const p=marker.getLatLng();setPoint(p.lat,p.lng,'GHIM_TAY');reverse();});}
   else marker.setLatLng(p);
   map.panTo(p,{animate:false});
  }
  function setPoint(latitude,longitude,kind){lat.value=Number(latitude).toFixed(7);lng.value=Number(longitude).toFixed(7);source.value=kind;drawPoint();dirty();}
  function clearCenter(){if(!['GPS','GHIM_TAY'].includes(source.value)){lat.value='';lng.value='';source.value='';drawPoint();}}
  async function createMap(){
   if(map||form.hidden)return;
   try{const config=await settings();if(!window.L)throw Error('Chưa tải được bản đồ. Bạn vẫn chọn địa chỉ bằng tay được.');
    map=L.map(mapBox,{zoomControl:false}).setView([Number(config.originLatitude),Number(config.originLongitude)],12);
    L.control.zoom({zoomInTitle:'Phóng to',zoomOutTitle:'Thu nhỏ'}).addTo(map);
    L.tileLayer(config.tileUrl,{maxZoom:19,attribution:'© <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener">OpenStreetMap</a> — người đóng góp'}).addTo(map);
    map.on('click',event=>{setPoint(event.latlng.lat,event.latlng.lng,'GHIM_TAY');reverse();});
    mapBox.addEventListener('keydown',event=>{if(event.key==='Enter'){event.preventDefault();const p=map.getCenter();setPoint(p.lat,p.lng,'GHIM_TAY');reverse();}});
    drawPoint();
   }catch(error){message.textContent=error.message||'Chưa tải được bản đồ. Bạn vẫn chọn địa chỉ bằng tay được.';}
  }
  async function refreshQuote(){
   clearTimeout(timer);quoteRequest?.abort();const current=++revision;quoteRequest=new AbortController();busy(true);
   if(!province.value||!ward.value||!street.value.trim()){text(root,'[data-delivery-note]','Chọn đầy đủ tỉnh/thành, phường/xã và số nhà, tên đường để tính phí.');busy(false);return;}
   const data=new URLSearchParams(new FormData(form));
   data.set('provinceId',province.value);data.set('wardId',ward.value);data.set('streetAddress',street.value);
   data.set('latitude',lat.value);data.set('longitude',lng.value);data.set('coordinateSource',source.value);
   data.set('scope',root.dataset.scope);data.set('voucherCode',appliedVoucher);
   try{const result=await jsonPost('/shipping/quote',data,quoteRequest.signal);if(current!==revision)return;const q=result.shipping;
    text(form,'[data-delivery-fee]',q.freeShip?'Miễn phí vận chuyển':money(q.shippingFee));text(root,'[data-delivery-distance]',q.distanceKm==null?'Chưa xác định':new Intl.NumberFormat('vi-VN').format(q.distanceKm)+' km (đường chim bay)');text(root,'[data-delivery-note]',q.note);text(root,'[data-delivery-hint]',q.hint);
    form.querySelectorAll('[data-subtotal]').forEach(el=>{el.textContent=money(result.subtotal);el.dataset.subtotal=result.subtotal;});
    form.querySelectorAll('[data-total]').forEach(el=>{el.textContent=money(result.total);el.dataset.amount=result.total;});
    form.querySelectorAll('[data-discount-row]').forEach(el=>el.hidden=!result.valid||Number(result.discountAmount)===0);text(form,'[data-discount]','− '+money(result.discountAmount));text(form,'[data-voucher-message]',result.message);
    if(!['GPS','GHIM_TAY'].includes(source.value)){lat.value=q.latitude??'';lng.value=q.longitude??'';source.value=q.coordinateSource||'';drawPoint();}
   }catch(error){if(error.name!=='AbortError'&&current===revision){text(root,'[data-delivery-note]',error.message||'Chưa báo được phí. Cửa hàng sẽ tính lại khi đặt đơn.');}}
   finally{if(current===revision)busy(false);}
  }
  const schedule=()=>{clearTimeout(timer);timer=setTimeout(refreshQuote,250);};
  async function reverse(){
   reverseRequest?.abort();const current=++reverseRevision;reverseRequest=new AbortController();message.textContent='Đang gợi ý địa chỉ từ vị trí…';
   try{const result=await jsonPost('/shipping/reverse',new URLSearchParams({latitude:lat.value,longitude:lng.value}),reverseRequest.signal);if(current!==reverseRevision)return;
    if(result.provinceId){province.value=String(result.provinceId);sync(province);await loadWards(result.wardId||'');}
    if(current!==reverseRevision)return;message.textContent=result.message;
   }catch(error){if(error.name!=='AbortError'&&current===reverseRevision)message.textContent='Chưa tra được địa chỉ. Vị trí ghim vẫn được giữ; bạn chọn địa chỉ bằng tay được.';}
   if(current===reverseRevision)refreshQuote();
  }
  province.addEventListener('change',async()=>{reverseRequest?.abort();++reverseRevision;clearCenter();await loadWards();refreshQuote();});
  ward.addEventListener('change',()=>{reverseRequest?.abort();++reverseRevision;clearCenter();refreshQuote();});
  street.addEventListener('input',schedule);form.elements.phone?.addEventListener('input',schedule);
  root.querySelector('[data-clear-location]').addEventListener('click',()=>{reverseRequest?.abort();++reverseRevision;lat.value='';lng.value='';source.value='';drawPoint();dirty();refreshQuote();});
  locate.addEventListener('click',()=>{
   if(!navigator.geolocation){message.textContent='Trình duyệt chưa hỗ trợ định vị. Bạn vẫn chọn địa chỉ bằng tay được.';return;}
   locate.disabled=true;locate.setAttribute('aria-busy','true');message.textContent='Đang lấy vị trí…';
   navigator.geolocation.getCurrentPosition(position=>{locate.disabled=false;locate.removeAttribute('aria-busy');setPoint(position.coords.latitude,position.coords.longitude,'GPS');createMap().then(reverse);},error=>{locate.disabled=false;locate.removeAttribute('aria-busy');message.textContent=error.code===1?'Bạn chưa cấp quyền định vị. Vui lòng chọn địa chỉ bằng tay.':'Chưa lấy được vị trí trong thời gian chờ. Bạn vẫn chọn địa chỉ bằng tay được.';},{enableHighAccuracy:false,timeout:8000,maximumAge:60000});
  });
  voucher?.addEventListener('input',()=>{appliedVoucher='';schedule();});
  form.querySelectorAll('[data-apply-voucher]').forEach(button=>button.addEventListener('click',()=>{appliedVoucher=voucher?.value.trim()||'';refreshQuote();}));
  root.addEventListener('delivery:select-address',async event=>{reverseRequest?.abort();++reverseRevision;const a=event.detail;province.value=String(a.provinceId||'');sync(province);street.value=a.streetAddress||'';lat.value=a.latitude||'';lng.value=a.longitude||'';source.value=a.coordinateSource||'';await loadWards(a.wardId||'');drawPoint();dirty();refreshQuote();});
  document.addEventListener('delivery:visible',()=>{if(!form.hidden)createMap().then(()=>{map?.invalidateSize();refreshQuote();});});
  pointLabel();if(!form.hidden){createMap();refreshQuote();}
 }
 document.querySelectorAll('[data-delivery-address]').forEach(bindDeliveryAddress);

 document.querySelectorAll('[data-customer-addresses-wrapper]').forEach(wrapper=>{
  const container=wrapper.querySelector('[data-customer-addresses]');
  const addBtn=wrapper.querySelector('[data-add-customer-address]');
  const limitNote=wrapper.querySelector('[data-address-limit-note]');
  const template=wrapper.querySelector('#customer-address-template');
  if(!container||!addBtn||!template)return;
  function reindex(){
   const blocks=[...container.querySelectorAll('[data-customer-address-block]')];
   let hasChecked=blocks.some(b=>b.querySelector('[data-address-default]')?.checked);
   blocks.forEach((b,idx)=>{
    const title=b.querySelector('[data-address-title]');if(title)title.textContent='Địa chỉ nhận hàng '+(idx+1);
    const radio=b.querySelector('[data-address-default]'),radioLabel=b.querySelector('[data-address-default-label]');
    if(radio){radio.value=String(idx);radio.id='defaultAddress-'+idx;if(!hasChecked&&idx===0){radio.checked=true;hasChecked=true;}}
    if(radioLabel)radioLabel.setAttribute('for','defaultAddress-'+idx);
    const removeBtn=b.querySelector('[data-remove-address]');if(removeBtn)removeBtn.hidden=blocks.length<=1;
    const idInput=b.querySelector('[data-address-id]');if(idInput)idInput.name=`addresses[${idx}].id`;
    const prov=b.querySelector('[data-delivery-province]'),provLbl=b.querySelector('[data-province-label]'),provErr=b.querySelector('[data-province-error]');
    if(prov){prov.name=`addresses[${idx}].provinceId`;prov.id=`customer-addr-${idx}-province`;prov.setAttribute('aria-describedby',`customer-addr-${idx}-province-error`);if(prov._comboWrapper)prov._comboWrapper.dataset.comboFor=prov.id;}
    if(provLbl){provLbl.setAttribute('for',`customer-addr-${idx}-province`);provLbl.id=`customer-addr-${idx}-province-combo-label`;prov?._comboInput?.setAttribute('aria-labelledby',provLbl.id);}
    if(provErr)provErr.id=`customer-addr-${idx}-province-error`;
    const ward=b.querySelector('[data-delivery-ward]'),wardLbl=b.querySelector('[data-ward-label]'),wardErr=b.querySelector('[data-ward-error]');
    if(ward){ward.name=`addresses[${idx}].wardId`;ward.id=`customer-addr-${idx}-ward`;ward.setAttribute('aria-describedby',`customer-addr-${idx}-ward-error`);if(ward._comboWrapper)ward._comboWrapper.dataset.comboFor=ward.id;}
    if(wardLbl){wardLbl.setAttribute('for',`customer-addr-${idx}-ward`);wardLbl.id=`customer-addr-${idx}-ward-combo-label`;ward?._comboInput?.setAttribute('aria-labelledby',wardLbl.id);}
    if(wardErr)wardErr.id=`customer-addr-${idx}-ward-error`;
    const street=b.querySelector('[data-delivery-street]'),streetLbl=b.querySelector('[data-street-label]'),streetErr=b.querySelector('[data-street-error]');
    if(street){street.name=`addresses[${idx}].streetAddress`;street.id=`customer-addr-${idx}-street`;street.setAttribute('aria-describedby',`customer-addr-${idx}-street-error`);}
    if(streetLbl)streetLbl.setAttribute('for',`customer-addr-${idx}-street`);
    if(streetErr)streetErr.id=`customer-addr-${idx}-street-error`;
    const lat=b.querySelector('[data-delivery-latitude]');if(lat){lat.name=`addresses[${idx}].latitude`;lat.id=`customer-addr-${idx}-latitude`;}
    const lng=b.querySelector('[data-delivery-longitude]');if(lng){lng.name=`addresses[${idx}].longitude`;lng.id=`customer-addr-${idx}-longitude`;}
    const src=b.querySelector('[data-delivery-source]');if(src){src.name=`addresses[${idx}].coordinateSource`;src.id=`customer-addr-${idx}-source`;}
    const mapBox=b.querySelector('[data-delivery-map]');if(mapBox)mapBox.id=`customer-addr-${idx}-map`;
   });
   addBtn.disabled=blocks.length>=3;
   if(limitNote)limitNote.textContent=blocks.length>=3?'Mỗi khách chỉ lưu tối đa 3 địa chỉ':'Tối đa 3 địa chỉ nhận hàng.';
  }
  addBtn.addEventListener('click',()=>{
   const count=container.querySelectorAll('[data-customer-address-block]').length;
   if(count>=3)return;
   const html=template.innerHTML.replace(/__INDEX__/g,String(count));
   const temp=document.createElement('div');temp.innerHTML=html.trim();
   const block=temp.firstElementChild;
   container.appendChild(block);
   reindex();
   bindDeliveryAddress(block);
  });
  container.addEventListener('click',event=>{
   const removeBtn=event.target.closest('[data-remove-address]');
   if(!removeBtn)return;
   const blocks=container.querySelectorAll('[data-customer-address-block]');
   if(blocks.length<=1)return;
   removeBtn.closest('[data-customer-address-block]')?.remove();
   reindex();
  });
  reindex();
 });
})();
