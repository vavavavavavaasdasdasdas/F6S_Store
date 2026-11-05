package com.example.demodatn;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.UUID;

/** Dữ liệu riêng của test trong transaction; không dùng seed khi app khởi động. */
public final class CatalogTestFixtures {
    private CatalogTestFixtures() { }

    public static int variant(JdbcTemplate jdbc) {
        TestDatabaseGuard.check(jdbc);
        if (!TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("Fixture phải nằm trong transaction của test");
        int product = jdbc.queryForObject("INSERT products(code,name,category_id,image_url,status) OUTPUT INSERTED.id VALUES(?,N'Giày kiểm thử độc lập',(SELECT id FROM categories WHERE name=N'Nike'),N'/anh/anh-san-pham-thay-the.svg',1)",
                Integer.class, "FIX" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        return jdbc.queryForObject("INSERT product_variants(product_id,size_id,chat_lieu_id,mau_sac_id,de_giay_id,price,quantity,status) OUTPUT INSERTED.id VALUES(?,(SELECT id FROM sizes WHERE size_value=N'40'),(SELECT id FROM chat_lieu WHERE ten=N'Da thật'),(SELECT id FROM mau_sac WHERE ten=N'Đen'),(SELECT id FROM de_giay WHERE ten=N'Cao su'),200000,50,1)", Integer.class, product);
    }

    public static int soldVariant(JdbcTemplate jdbc) {
        int variant = variant(jdbc);
        int order = jdbc.queryForObject("INSERT orders(order_code,order_type,status,payment_status,payment_method,total_amount,final_amount) OUTPUT INSERTED.id VALUES(?,'ONLINE','DELIVERED','PAID','COD',200000,200000)",
                Integer.class, "FIXO" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        jdbc.update("INSERT order_details(order_id,product_variant_id,price,quantity,total_price,product_name,size_name,ten_chat_lieu,ten_mau_sac,ten_de_giay) VALUES(?,?,200000,1,200000,N'Giày kiểm thử độc lập',N'40',N'Da thật',N'Đen',N'Cao su')", order, variant);
        return variant;
    }
}
