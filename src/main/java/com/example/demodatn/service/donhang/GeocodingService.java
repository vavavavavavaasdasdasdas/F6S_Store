package com.example.demodatn.service.donhang;
import com.example.demodatn.config.GeocodingProperties;
import com.example.demodatn.entity.donhang.*;
import com.example.demodatn.repository.donhang.WardRepository;
import com.example.demodatn.util.VietnameseText;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.math.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

@Service
public class GeocodingService {
    public record Point(BigDecimal latitude,BigDecimal longitude) {}
    public record ReverseResult(BigDecimal latitude,BigDecimal longitude,Integer provinceId,Integer wardId,String message) {}
    private record Cached(ReverseResult value,long expires) {}
    private record Window(long start,int count) {}
    private final GeocodingProperties config;private final WardRepository wards;private final AddressDirectory directory;private final Clock clock;
    private final JsonMapper json=JsonMapper.builder().build();private final Semaphore active=new Semaphore(1);
    private final Map<String,Cached> reverseCache=new ConcurrentHashMap<>();private final Map<String,Window> clients=new ConcurrentHashMap<>();
    private volatile long nextRequestNanos;
    public GeocodingService(GeocodingProperties config,WardRepository wards,AddressDirectory directory,Clock clock){this.config=config;this.wards=wards;this.directory=directory;this.clock=clock;}
    public static Point point(BigDecimal lat,BigDecimal lng){
        if(lat==null || lng==null || lat.compareTo(BigDecimal.valueOf(-90))<0 || lat.compareTo(BigDecimal.valueOf(90))>0 || lng.compareTo(BigDecimal.valueOf(-180))<0 || lng.compareTo(BigDecimal.valueOf(180))>0)throw new IllegalArgumentException("Tọa độ không hợp lệ.");
        return new Point(lat.setScale(7,RoundingMode.HALF_UP),lng.setScale(7,RoundingMode.HALF_UP));
    }
    private synchronized boolean permit(String client){
        long now=clock.millis();if(clients.size()>1000)clients.clear();
        var window=clients.get(client);if(window==null || now-window.start()>=60000)window=new Window(now,0);
        if(window.count()>=config.getRequestsPerMinute())return false;
        clients.put(client,new Window(window.start(),window.count()+1));return true;
    }
    private Optional<String> request(String path,String query,String client){
        if(!config.isEnabled() || TransactionSynchronizationManager.isActualTransactionActive() || config.getBaseUrl()==null || config.getUserAgent()==null || config.getUserAgent().isBlank() || !permit(client))return Optional.empty();
        if(!active.tryAcquire())return Optional.empty();
        try {
            long now=System.nanoTime();if(now<nextRequestNanos)return Optional.empty();
            URI base=URI.create(config.getBaseUrl());if(!Set.of("http","https").contains(base.getScheme()) || base.getHost()==null)return Optional.empty();
            long interval="nominatim.openstreetmap.org".equalsIgnoreCase(base.getHost())?Math.max(1000,config.getMinIntervalMillis()):Math.max(0,config.getMinIntervalMillis());
            nextRequestNanos=now+TimeUnit.MILLISECONDS.toNanos(interval);
            var uri=URI.create(config.getBaseUrl().replaceAll("/+$","")+path+"?"+query);
            var http=HttpClient.newBuilder().connectTimeout(Duration.ofMillis(config.getTimeoutMillis())).followRedirects(HttpClient.Redirect.NEVER).build();
            var call=HttpRequest.newBuilder(uri).timeout(Duration.ofMillis(config.getTimeoutMillis())).header("User-Agent",config.getUserAgent()).header("Accept-Language","vi").header("Accept","application/json").GET().build();
            var result=http.send(call,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return result.statusCode()==200 && result.body().length()<=200000?Optional.of(result.body()):Optional.empty();
        } catch(InterruptedException ex){Thread.currentThread().interrupt();return Optional.empty();}
        catch(Exception ex){return Optional.empty();}
        finally{active.release();}
    }
    private static String encode(String s){return URLEncoder.encode(s,StandardCharsets.UTF_8);}
    public Optional<Point> wardCenter(Ward ward,String client){
        if(ward.getLatitude()!=null && ward.getLongitude()!=null)return Optional.of(point(ward.getLatitude(),ward.getLongitude()));
        var now=LocalDateTime.now(clock);
        if(!config.isEnabled() || ward.getGeocodeRetryAfter()!=null && ward.getGeocodeRetryAfter().isAfter(now))return Optional.empty();
        var body=request("/search","format=jsonv2&limit=3&addressdetails=1&countrycodes=vn&q="+encode(ward.getName()+", "+ward.getProvince().getName()+", Việt Nam"),client);
        if(body.isPresent())try{
            var rows=json.readValue(body.get(),List.class);
            for(Object value:rows){var row=(Map<?,?>)value;String display=VietnameseText.fold(Objects.toString(row.get("display_name"),""));
                if(!display.contains(AddressDirectory.normalizedName(ward.getName())) || !display.contains(AddressDirectory.normalizedName(ward.getProvince().getName())))continue;
                var p=point(new BigDecimal(row.get("lat").toString()),new BigDecimal(row.get("lon").toString()));
                wards.storeCoordinates(ward.getId(),p.latitude(),p.longitude(),now);return Optional.of(p);
            }
        }catch(Exception ignored){}
        // A failed/busy provider is cached briefly; normal checkout still uses the configured fallback tier.
        if(!TransactionSynchronizationManager.isActualTransactionActive())wards.retryAfter(ward.getId(),now.plusSeconds(config.getRetrySeconds()));
        return Optional.empty();
    }
    public ReverseResult reverse(BigDecimal latitude,BigDecimal longitude,String client){
        var p=point(latitude,longitude);String key=p.latitude().setScale(5,RoundingMode.HALF_UP)+":"+p.longitude().setScale(5,RoundingMode.HALF_UP);long now=clock.millis();
        var cached=reverseCache.get(key);if(cached!=null && cached.expires()>now)return cached.value();
        ReverseResult result=new ReverseResult(p.latitude(),p.longitude(),null,null,"Chưa tra được địa chỉ. Bạn vẫn có thể chọn tay và giữ vị trí đã ghim.");
        var body=request("/reverse","format=jsonv2&addressdetails=1&zoom=18&lat="+p.latitude()+"&lon="+p.longitude(),client);
        if(body.isPresent())try{
            var row=json.readValue(body.get(),Map.class);var a=(Map<?,?>)row.get("address");
            if(a!=null){Province province=null;for(String field:List.of("state","city","province","municipality")){province=directory.matchProvince(Objects.toString(a.get(field),""));if(province!=null)break;}
                Ward ward=null;for(String field:List.of("suburb","quarter","village","town","municipality","city_district")){ward=directory.matchWard(province,Objects.toString(a.get(field),""));if(ward!=null)break;}
                result=new ReverseResult(p.latitude(),p.longitude(),province==null?null:province.getId(),ward==null?null:ward.getId(),ward==null?"Đã ghim vị trí; vui lòng kiểm tra và chọn Phường/Xã.":"Đã gợi ý địa chỉ từ vị trí. Bạn có thể sửa lại trước khi lưu.");
            }
        }catch(Exception ignored){}
        if(reverseCache.size()>1000)reverseCache.clear();reverseCache.put(key,new Cached(result,now+config.getCacheSeconds()*1000));return result;
    }
}
