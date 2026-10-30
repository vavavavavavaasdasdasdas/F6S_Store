package com.example.demodatn.config;

import com.example.demodatn.entity.khachhang.ShippingAddress;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import javax.sql.DataSource;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SchemaCheckTest {
    @Test void missingTableNamesItsMigrationAndMissingColumnsAreAllReported() {
        var required = SchemaCheck.requiredColumns(List.of(ShippingAddress.class));
        assertEquals(Set.of("id","customer_id","ten_nguoi_nhan","so_dien_thoai","dia_chi","la_mac_dinh","thu_tu","created_at","tinh_thanh_id","phuong_xa_id","so_nha_duong","latitude","longitude","nguon_toa_do"), required.get("dia_chi_nhan_hang"));
        assertEquals(List.of("Thiếu bảng dbo.dia_chi_nhan_hang — 009_dia_chi_nhan_hang.sql"), SchemaCheck.missing(required, Map.of()));
        var missing = SchemaCheck.missing(required, Map.of("dia_chi_nhan_hang", Set.of("id")));
        assertEquals(13, missing.size());
        assertTrue(missing.stream().anyMatch(line->line.contains("tinh_thanh_id") && line.contains("015_theo_doi_giao_hang.sql")));
        assertEquals(5,missing.stream().filter(line->line.contains("016_dia_chi_phi_ship_don_online.sql")).count());
        assertEquals(7,missing.stream().filter(line->line.contains("009_dia_chi_nhan_hang.sql")).count());
    }

    @Test void completeSchemaHasNoErrorsAndUnrelatedBeansDoNotOpenAConnection() {
        var required = SchemaCheck.requiredColumns(List.of(ShippingAddress.class));
        assertTrue(SchemaCheck.missing(required, required).isEmpty());
        var provider = new StaticListableBeanFactory().getBeanProvider(DataSource.class);
        var checker = new SchemaCheck(provider);
        Object bean = new Object();
        assertSame(bean, checker.postProcessBeforeInitialization(bean, "ordinaryBean"));
    }
}
