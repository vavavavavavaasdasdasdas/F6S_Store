package com.example.demodatn.repository.donhang;
import com.example.demodatn.entity.donhang.Invoice;
public interface InvoiceRepository extends org.springframework.data.jpa.repository.JpaRepository<Invoice,Integer>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<Invoice> {
    java.util.Optional<Invoice> findByOrder_Id(Integer id);
    @org.springframework.data.jpa.repository.Query("select i.order.id,i.id from Invoice i where i.order.id in :ids")
    java.util.List<Object[]> orderInvoiceIds(@org.springframework.data.repository.query.Param("ids") java.util.Collection<Integer> ids);
}
