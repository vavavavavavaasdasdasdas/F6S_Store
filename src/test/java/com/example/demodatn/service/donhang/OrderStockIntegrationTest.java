package com.example.demodatn.service.donhang;
import com.example.demodatn.dto.donhang.CartItemDto;
import com.example.demodatn.dto.donhang.CheckoutRequest;
import com.example.demodatn.entity.donhang.Order;
import com.example.demodatn.entity.sanpham.Product;
import com.example.demodatn.entity.sanpham.ProductVariant;
import com.example.demodatn.repository.donhang.OrderRepository;
import com.example.demodatn.repository.nhanvien.EmployeeRepository;
import com.example.demodatn.repository.sanpham.MaterialRepository;
import com.example.demodatn.repository.sanpham.ProductRepository;
import com.example.demodatn.repository.sanpham.ProductVariantRepository;
import com.example.demodatn.repository.sanpham.SizeRepository;
import com.example.demodatn.service.sanpham.InventoryService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest @WithMockUser(username="admin",roles="ADMIN")
class OrderStockIntegrationTest {
    @org.springframework.beans.factory.annotation.Autowired com.example.demodatn.repository.sanpham.ColorRepository phase5Colors;
    @org.springframework.beans.factory.annotation.Autowired com.example.demodatn.repository.sanpham.SoleRepository phase5Soles;

    @Autowired OrderService orders;
    @Autowired OrderStatusService statuses;
    @Autowired OrderRepository repository;
    @Autowired InventoryService inventory;
    @Autowired ProductRepository products;
    @Autowired ProductVariantRepository variants;
    @Autowired SizeRepository sizes;
    @Autowired MaterialRepository materials;
    @Autowired EmployeeRepository employees;
    @Autowired BankTransferExpiryJob expiry;
    @Autowired PlatformTransactionManager manager;
    @Autowired JdbcTemplate jdbc;
    int pid,vid;
    <T>T tx(java.util.function.Supplier<T> operation){return new TransactionTemplate(manager).execute(s->operation.get());}
    @BeforeEach void fixture(){
        assertTrue((Set.of("shopbangiay_kt","shop_verify","shopbangiay_thu").contains(jdbc.queryForObject("SELECT DB_NAME()",String.class)) || jdbc.queryForObject("SELECT DB_NAME()",String.class).startsWith("shopbangiay_kt_")));
        tx(()->{var p=new Product();p.setCode("STOCK-"+UUID.randomUUID());p.setName("Kiểm thử luật kho");products.saveAndFlush(p);pid=p.getId();var v=new ProductVariant();v.setProduct(p);v.setSize(sizes.findAll().get(0));v.setMaterial(materials.findAll().get(0));v.setColor(phase5Colors.findAll().get(0));v.setSole(phase5Soles.findAll().get(0));v.setPrice(BigDecimal.valueOf(100000));v.setQuantity(5);variants.saveAndFlush(v);vid=v.getId();return null;});
    }
    @AfterEach void cleanup(){
        tx(()->{String ids="SELECT order_id FROM order_details WHERE product_variant_id="+vid;
            jdbc.update("DELETE invoices WHERE order_id IN ("+ids+")");
            jdbc.update("DELETE order_details WHERE product_variant_id=?",vid);
            // Orders are recorded separately because deletion of their details removes the lookup.
            for(int id:created){jdbc.update("DELETE invoices WHERE order_id=?",id);jdbc.update("DELETE order_details WHERE order_id=?",id);jdbc.update("DELETE order_status_history WHERE order_id=?",id);jdbc.update("DELETE orders WHERE id=?",id);}
            jdbc.update("DELETE inventory_logs WHERE variant_id=?",vid);variants.deleteById(vid);products.deleteById(pid);return null;});
    }
    List<Integer> created=new ArrayList<>();
    int place(String method){return place(method,false);}
    int place(String method,boolean pos){int id=tx(()->{
        var item=new CartItemDto(pid,vid,"Kiểm thử","39",BigDecimal.ONE,1);
        Order o;
        if(pos)o=orders.createPosOrder(List.of(item),null,employees.findByUser_Username("admin").orElseThrow(),null,method,"Quầy",BigDecimal.valueOf(200000));
        else {var request=new CheckoutRequest();request.setProvinceId(jdbc.queryForObject("SELECT TOP 1 id FROM tinh_thanh ORDER BY id",Integer.class));request.setWardId(jdbc.queryForObject("SELECT TOP 1 id FROM phuong_xa WHERE tinh_thanh_id=? ORDER BY id",Integer.class,request.getProvinceId()));request.setStreetAddress("Số 12, đường Nguyễn Huệ");request.setPaymentMethod(method);request.setReceiverName("Khách kiểm thử");o=orders.placeOrder(request,List.of(item),null,BigDecimal.ZERO);}
        return o.getId();});created.add(id);return id;}
    int stock(){return jdbc.queryForObject("SELECT quantity FROM product_variants WHERE id=?",Integer.class,vid);}
    Map<String,Object> row(int id){return jdbc.queryForMap("SELECT status,da_tru_kho,payment_status FROM orders WHERE id=?",id);}
    int logs(int id){return jdbc.queryForObject("SELECT COUNT(*) FROM inventory_logs WHERE variant_id=? AND reason LIKE '%' + (SELECT order_code FROM orders WHERE id=?) + '%'",Integer.class,vid,id);}
    @Test void codChecksThenConfirmsAndCancelsExactlyOnce(){
        int id=place("COD");assertEquals(5,stock());assertEquals(false,row(id).get("da_tru_kho"));assertEquals(0,logs(id));
        statuses.updateStatus(id,"CONFIRMED");statuses.updateStatus(id,"CONFIRMED");assertEquals(4,stock());assertEquals(true,row(id).get("da_tru_kho"));assertEquals(1,logs(id));
        statuses.updateStatus(id,"CANCELLED");statuses.updateStatus(id,"CANCELLED");assertEquals(5,stock());assertEquals(false,row(id).get("da_tru_kho"));assertEquals(2,logs(id));
        int pending=place("COD");statuses.updateStatus(pending,"CANCELLED");assertEquals(5,stock());assertEquals(0,logs(pending));
    }
    @Test void inventoryFunctionsAreIdempotentEvenWhenCalledDirectly(){
        int id=place("COD");
        tx(()->{var o=repository.findById(id).orElseThrow();inventory.truKhoChoDon(o);inventory.truKhoChoDon(o);return null;});
        assertEquals(4,stock());assertEquals(true,row(id).get("da_tru_kho"));assertEquals(1,logs(id));
        tx(()->{var o=repository.findById(id).orElseThrow();inventory.hoanKhoChoDon(o);inventory.hoanKhoChoDon(o);return null;});
        assertEquals(5,stock());assertEquals(false,row(id).get("da_tru_kho"));assertEquals(2,logs(id));
    }
    @Test void insufficientStockBlocksBothPlacementAndConfirmation(){
        int id=place("COD");jdbc.update("UPDATE product_variants SET quantity=0 WHERE id=?",vid);
        var before=row(id);assertThrows(IllegalArgumentException.class,()->statuses.updateStatus(id,"CONFIRMED"));assertEquals(before,row(id));assertEquals(0,stock());assertEquals(0,logs(id));
        long count=repository.count();assertThrows(IllegalArgumentException.class,()->place("COD"));assertEquals(count,repository.count());
        assertThrows(IllegalArgumentException.class,()->place("BANK_TRANSFER"));assertEquals(count,repository.count());
    }
    @Test void bankTransferAndPosReserveImmediatelyAndRestoreOnCancel(){
        int bank=place("BANK_TRANSFER");assertEquals(4,stock());assertEquals(1,logs(bank));statuses.updateStatus(bank,"CONFIRMED");assertEquals(4,stock());
        statuses.updateStatus(bank,"CANCELLED");assertEquals(5,stock());
        int pos=place("TRANSFER",true);assertEquals("PAID",row(pos).get("payment_status"));assertEquals(4,stock());statuses.updateStatus(pos,"CANCELLED");assertEquals(5,stock());assertEquals(2,logs(pos));
    }
    @Test void shippingFailureRestoresButDeliveredCannotBeCancelledAndStepsCannotBeSkipped(){
        int id=place("COD");assertThrows(IllegalArgumentException.class,()->statuses.updateStatus(id,"SHIPPING"));
        statuses.updateStatus(id,"CONFIRMED");assertThrows(IllegalArgumentException.class,()->statuses.updateStatus(id,"SHIPPING"));statuses.updateStatus(id,"PREPARING");statuses.updateStatus(id,"SHIPPING");
        assertThrows(IllegalArgumentException.class,()->statuses.updateStatus(id,"CANCELLED"));statuses.updateStatus(id,"DELIVERY_FAILED");assertEquals(5,stock());assertEquals(2,logs(id));
        int done=place("COD");for(String status:List.of("CONFIRMED","PREPARING","SHIPPING","DELIVERED"))statuses.updateStatus(done,status);
        assertThrows(IllegalArgumentException.class,()->statuses.updateStatus(done,"CANCELLED"));assertEquals(4,stock());assertEquals("DELIVERED",row(done).get("status"));
    }
    @Test void timeoutJobCancelsOnlyPendingUnpaidOldBankOrders(){
        int expired=place("BANK_TRANSFER"),paid=place("BANK_TRANSFER"),cod=place("COD");
        statuses.confirmPayment(paid);jdbc.update("UPDATE orders SET created_at=DATEADD(hour,-25,GETDATE()) WHERE id IN (?,?,?)",expired,paid,cod);
        expiry.cancelExpiredOrders();assertEquals("CANCELLED",row(expired).get("status"));assertEquals(4,stock());assertEquals(2,logs(expired));assertEquals("PENDING",row(paid).get("status"));assertEquals("PENDING",row(cod).get("status"));
    }
    @Test void concurrentCodConfirmationWithOneItemHasExactlyOneWinner() throws Exception {
        jdbc.update("UPDATE product_variants SET quantity=1 WHERE id=?",vid);int a=place("COD"),b=place("COD");
        var ready=new CountDownLatch(2);var go=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        try {List<Future<Boolean>> results=new ArrayList<>();for(int id:List.of(a,b))results.add(pool.submit(()->{ready.countDown();go.await();try{statuses.updateStatus(id,"CONFIRMED");return true;}catch(IllegalArgumentException ex){return false;}}));
            assertTrue(ready.await(10,TimeUnit.SECONDS));go.countDown();int won=0;for(var f:results)if(f.get(20,TimeUnit.SECONDS))won++;assertEquals(1,won);assertEquals(0,stock());assertEquals(1,logs(a)+logs(b));
            
        } finally {pool.shutdownNow();}
    }
    @Test void exceptionAfterDeductionRollsBackFlagStockStatusAndLog(){
        int id=place("COD");var before=row(id);
        assertThrows(IllegalStateException.class,()->tx(()->{statuses.updateStatus(id,"CONFIRMED");throw new IllegalStateException("Ép lỗi sau trừ kho");}));
        assertEquals(before,row(id));assertEquals(5,stock());assertEquals(0,logs(id));
        
    }
}
