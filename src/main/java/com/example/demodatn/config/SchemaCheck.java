package com.example.demodatn.config;

import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import java.lang.reflect.Modifier;
import java.sql.SQLException;
import java.util.*;

/** Read-only preflight runs before Hibernate validation and reports all missing mappings together. */
@Component @RequiredArgsConstructor @Slf4j
public class SchemaCheck implements BeanPostProcessor {
    private final ObjectProvider<DataSource> dataSource;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String name) {
        if (!(bean instanceof LocalContainerEntityManagerFactoryBean)) return bean;
        try (var connection = dataSource.getObject().getConnection()) {
            Map<String,Set<String>> actual = new TreeMap<>();
            try (var columns = connection.getMetaData().getColumns(connection.getCatalog(), "dbo", "%", "%")) {
                while (columns.next()) actual.computeIfAbsent(columns.getString("TABLE_NAME").toLowerCase(Locale.ROOT), k -> new TreeSet<>())
                        .add(columns.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
            }
            var scanner = new ClassPathScanningCandidateComponentProvider(false);
            scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
            var types = new ArrayList<Class<?>>();
            for (var candidate : scanner.findCandidateComponents("com.example.demodatn.entity"))
                types.add(Class.forName(candidate.getBeanClassName()));
            var missing = missing(requiredColumns(types), actual);
            if (!missing.isEmpty()) {
                missing.forEach(item -> log.error("SCHEMA_CHECK: {}", item));
                throw new IllegalStateException("Schema chưa đầy đủ: " + String.join("; ", missing)
                        + ". Chạy database/migrations/chay_tat_ca.sql trên đúng database trước khi khởi động.");
            }
        } catch (SQLException | ClassNotFoundException error) {
            log.error("SCHEMA_CHECK: Không đọc được metadata schema; không thay đổi database.", error);
            throw new IllegalStateException("Không kiểm tra được schema database.", error);
        }
        return bean;
    }

    static Map<String,Set<String>> requiredColumns(Collection<Class<?>> types) {
        Map<String,Set<String>> result = new TreeMap<>();
        for (Class<?> type : types) {
            Table mapping = type.getAnnotation(Table.class);
            String table = mapping == null || mapping.name().isBlank() ? snake(type.getSimpleName()) : mapping.name();
            Set<String> columns = result.computeIfAbsent(table.toLowerCase(Locale.ROOT), k -> new TreeSet<>());
            for (Class<?> parent = type; parent != null && parent != Object.class; parent = parent.getSuperclass()) {
                for (var field : parent.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())
                            || field.isAnnotationPresent(Transient.class) || field.isAnnotationPresent(OneToMany.class)
                            || field.isAnnotationPresent(ManyToMany.class) || field.isAnnotationPresent(ElementCollection.class)) continue;
                    var column = field.getAnnotation(Column.class);
                    var join = field.getAnnotation(JoinColumn.class);
                    String name = join != null && !join.name().isBlank() ? join.name()
                            : column != null && !column.name().isBlank() ? column.name() : snake(field.getName());
                    columns.add(name.toLowerCase(Locale.ROOT));
                }
            }
        }
        return result;
    }

    static List<String> missing(Map<String,Set<String>> required, Map<String,Set<String>> actual) {
        var missing = new ArrayList<String>();
        required.forEach((table, columns) -> {
            if (!actual.containsKey(table)) missing.add("Thiếu bảng dbo." + table + " — " + migration(table, ""));
            else columns.stream().filter(column -> !actual.get(table).contains(column))
                    .forEach(column -> missing.add("Thiếu cột dbo." + table + "." + column + " — " + migration(table, column)));
        });
        return missing;
    }

    private static String migration(String table, String column) {
        if(java.util.Set.of("so_cccd","ngay_sinh","gioi_tinh","must_change_password","credential_mail_failed","credential_mail_window","credential_mail_count","credential_mail_pending_until").contains(column))return "011_can_cuoc_tai_khoan_email.sql";
        if(java.util.Set.of("phuong_xa","cau_hinh_phi_ship").contains(table) || java.util.Set.of("ma_hanh_chinh","phuong_xa_id","so_nha_duong","latitude","longitude","nguon_toa_do","distance_km","is_free_ship","shipping_policy_version","shipping_note","order_source","sales_note","ma_tinh_giao","ma_phuong_giao","ten_phuong_giao","dia_chi_giao_snapshot").contains(column))return "016_dia_chi_phi_ship_don_online.sql";
        if(java.util.Set.of("tinh_thanh","vung_giao_hang","order_status_history").contains(table) || java.util.Set.of("tinh_thanh_id","ten_tinh_thanh","du_kien_giao_tu","du_kien_giao_den","ngay_giao_thuc_te").contains(column))return "015_theo_doi_giao_hang.sql";
        if(column.equals("payment_reference"))return "014_tham_chieu_thanh_toan.sql";
        if(table.equals("mau_sac") || column.equals("mau_sac_id") || column.equals("ten_mau_sac") || column.equals("ten_de_giay") || (table.equals("product_variants") && column.equals("de_giay_id")))return "013_mau_sac_bien_the.sql";
        if(table.equals("de_giay") || column.equals("de_giay_id"))return "010_de_giay.sql";
        if (table.equals("dia_chi_nhan_hang")) return "009_dia_chi_nhan_hang.sql";
        if (table.equals("product_images")) return "004_anh_san_pham.sql";
        if (table.equals("chat_lieu") || column.equals("chat_lieu_id") || column.equals("ten_chat_lieu")) return "005_chat_lieu.sql";
        if (table.equals("orders") && column.equals("da_tru_kho")) return "006_luat_ton_kho.sql";
        return "shopgiay.sql (schema gốc)";
    }

    private static String snake(String name) {
        return name.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }
}
