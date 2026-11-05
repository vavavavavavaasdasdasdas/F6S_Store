package com.example.demodatn.service.sanpham;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ObjectCodeIntegrationTest {
    @Autowired JdbcTemplate jdbc;@Autowired PlatformTransactionManager transactions;
    @BeforeEach void guard(){com.example.demodatn.TestDatabaseGuard.check(jdbc);}
    @Test void twentyConcurrentInsertsPerTypeNeverDuplicateAndRollbackDoesNotReuse() throws Exception {
        var pool=Executors.newFixedThreadPool(20);
        try {
            for(var definition:List.of(new String[]{"categories","TH","name"},new String[]{"sizes","KC","size_value"},new String[]{"chat_lieu","CL","ten"},new String[]{"mau_sac","MS","ten"},new String[]{"de_giay","DG","ten"},new String[]{"promotions","KM","name"})) {
                String table=definition[0],prefix=definition[1],field=definition[2],token=UUID.randomUUID().toString().substring(0,4);var start=new CountDownLatch(1);List<Future<String>> futures=new ArrayList<>();
                for(int i=0;i<20;i++){final int n=i;futures.add(pool.submit(()->{start.await();String columns=field,values="?";if(table.equals("promotions")){columns+=",discount_type,discount_value,start_date,end_date,status";values+=",'PERCENT',1,'2026-01-01','2027-01-01',0";}return jdbc.queryForObject("INSERT dbo."+table+"("+columns+") OUTPUT INSERTED.ma VALUES("+values+")",String.class,"G9"+token+String.format("%02d",n));}));}
                start.countDown();Set<String> codes=new HashSet<>();for(var f:futures){String code=f.get(30,TimeUnit.SECONDS);assertTrue(code.matches(prefix+"[0-9]{3,}"));assertTrue(codes.add(code));}assertEquals(20,codes.size());System.out.println("GD9 CONCURRENT "+table+" requests=20 distinct=20 codes="+codes);
                for(var code:codes)jdbc.update("DELETE dbo."+table+" WHERE ma=?",code);
            }
            String rolled=new TransactionTemplate(transactions).execute(status->{String c=jdbc.queryForObject("INSERT categories(name) OUTPUT INSERTED.ma VALUES(N'G9 rollback')",String.class);status.setRollbackOnly();return c;});
            String next=jdbc.queryForObject("INSERT categories(name) OUTPUT INSERTED.ma VALUES(N'G9 after rollback')",String.class);assertNotEquals(rolled,next);assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM categories WHERE ma=?",Integer.class,rolled));jdbc.update("DELETE categories WHERE ma=?",next);
            var source=jdbc.queryForMap("SELECT TOP 1 size_id,chat_lieu_id,mau_sac_id,de_giay_id FROM product_variants ORDER BY id");var start=new CountDownLatch(1);List<Future<Map<String,Object>>> variants=new ArrayList<>();
            for(int i=0;i<20;i++){final int n=i;variants.add(pool.submit(()->{start.await();String token=UUID.randomUUID().toString();Integer p=jdbc.queryForObject("INSERT products(code,name,status) OUTPUT INSERTED.id VALUES(?,N'G9 concurrent variant',1)",Integer.class,"G9C-"+token);var row=jdbc.queryForMap("INSERT product_variants(product_id,size_id,chat_lieu_id,mau_sac_id,de_giay_id,price,quantity,status) OUTPUT INSERTED.ma,INSERTED.id VALUES(?,?,?,?,?,100000,1,1)",p,source.get("size_id"),source.get("chat_lieu_id"),source.get("mau_sac_id"),source.get("de_giay_id"));row.put("product",p);return row;}));}
            start.countDown();Set<String> codes=new HashSet<>();for(var f:variants){var r=f.get(30,TimeUnit.SECONDS);assertTrue(codes.add(r.get("ma").toString()));assertTrue(r.get("ma").toString().matches("BT[0-9]{4,}"));jdbc.update("DELETE product_variants WHERE id=?",r.get("id"));jdbc.update("DELETE products WHERE id=?",r.get("product"));}assertEquals(20,codes.size());System.out.println("GD9 CONCURRENT product_variants requests=20 distinct=20 codes="+codes);
        }finally{pool.shutdownNow();}
    }
}
