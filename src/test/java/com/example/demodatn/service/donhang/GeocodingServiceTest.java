package com.example.demodatn.service.donhang;

import com.example.demodatn.config.GeocodingProperties;
import com.example.demodatn.entity.donhang.*;
import com.example.demodatn.repository.donhang.WardRepository;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GeocodingServiceTest {
    HttpServer server;GeocodingProperties config;AddressDirectory directory;WardRepository wards;AtomicInteger calls;
    String response="{}";int status=200;long delay;String userAgent;
    @BeforeEach void setup() throws Exception {
        calls=new AtomicInteger();directory=mock(AddressDirectory.class);wards=mock(WardRepository.class);
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/",e->{calls.incrementAndGet();userAgent=e.getRequestHeaders().getFirst("User-Agent");try{Thread.sleep(delay);}catch(InterruptedException ex){Thread.currentThread().interrupt();}var bytes=response.getBytes(StandardCharsets.UTF_8);try{e.sendResponseHeaders(status,bytes.length);e.getResponseBody().write(bytes);}finally{e.close();}});server.start();
        config=new GeocodingProperties();config.setEnabled(true);config.setBaseUrl("http://127.0.0.1:"+server.getAddress().getPort());config.setUserAgent("F6S-local-test/1.0");config.setTimeoutMillis(1000);config.setMinIntervalMillis(0);config.setRequestsPerMinute(20);config.setCacheSeconds(600);config.setRetrySeconds(300);
    }
    @AfterEach void stop(){server.stop(0);TransactionSynchronizationManager.setActualTransactionActive(false);}
    GeocodingService service(){return new GeocodingService(config,wards,directory,Clock.systemUTC());}
    @Test void reverseUsesLocalStubRecognizesVietnameseAndCaches(){
        var p=new Province();p.setId(1);var w=new Ward();w.setId(2);when(directory.matchProvince("Hà Nội")).thenReturn(p);when(directory.matchWard(p,"Phường Tây Tựu")).thenReturn(w);
        response="{\"address\":{\"state\":\"Hà Nội\",\"suburb\":\"Phường Tây Tựu\"}}";var s=service();
        var result=s.reverse(new BigDecimal("21.03801"),new BigDecimal("105.7469"),"test");assertEquals(1,result.provinceId());assertEquals(2,result.wardId());assertEquals("F6S-local-test/1.0",userAgent);
        s.reverse(new BigDecimal("21.03801"),new BigDecimal("105.7469"),"test");assertEquals(1,calls.get());
    }
    @Test void providerFailureAndTimeoutKeepManualCoordinates(){
        status=503;var s=service();var result=s.reverse(BigDecimal.ONE,BigDecimal.TEN,"test");assertNull(result.wardId());assertEquals(0,BigDecimal.ONE.compareTo(result.latitude()));assertTrue(result.message().contains("chọn tay"));
        status=200;delay=250;config.setTimeoutMillis(50);result=service().reverse(BigDecimal.TEN,BigDecimal.ONE,"test");assertNull(result.wardId());assertNotNull(result.latitude());
    }
    @Test void transactionAndRateLimitNeverTriggerExtraProviderCalls(){
        var s=service();TransactionSynchronizationManager.setActualTransactionActive(true);s.reverse(BigDecimal.ONE,BigDecimal.ONE,"test");assertEquals(0,calls.get());TransactionSynchronizationManager.setActualTransactionActive(false);
        config.setRequestsPerMinute(1);s=service();s.reverse(BigDecimal.TEN,BigDecimal.ONE,"test");s.reverse(BigDecimal.TEN,BigDecimal.TEN,"test");assertEquals(1,calls.get());
    }
    @Test void wardCenterIsValidatedAndPersistedOnce(){
        var p=new Province();p.setName("Hà Nội");var w=new Ward();w.setId(2);w.setName("Phường Tây Tựu");w.setProvince(p);
        response="[{\"display_name\":\"Phường Tây Tựu, Hà Nội, Việt Nam\",\"lat\":\"21.03801\",\"lon\":\"105.7469\"}]";var point=service().wardCenter(w,"test").orElseThrow();assertEquals(0,new BigDecimal("21.03801").compareTo(point.latitude()));verify(wards).storeCoordinates(eq(2),any(),any(),any());
        w.setLatitude(point.latitude());w.setLongitude(point.longitude());service().wardCenter(w,"test");assertEquals(1,calls.get());
    }
}
