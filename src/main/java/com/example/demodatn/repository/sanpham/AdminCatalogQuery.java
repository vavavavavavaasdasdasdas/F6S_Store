package com.example.demodatn.repository.sanpham;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.domain.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Repository @RequiredArgsConstructor
public class AdminCatalogQuery {
    private final JdbcTemplate jdbc;
    private final com.example.demodatn.util.Pagination pagination;

    private static String likePattern(String raw) {
        return "%" + raw.trim().replace("!","!!").replace("%","!%").replace("_","!_").replace("[","![") + "%";
    }

    public Page<Map<String,Object>> products(String keyword, Integer category, Boolean status, String sort, Object page) {
        return products(keyword,category,status,sort,page,null,null,null,null);
    }
    public Page<Map<String,Object>> products(String keyword, Integer category, Boolean status, String sort, Object page, Integer materialId) {
        return products(keyword,category,status,sort,page,materialId,null,null,null);
    }
    public Page<Map<String,Object>> products(String keyword,Integer category,Boolean status,String sort,Object page,Integer materialId,Integer soleId) {
        return products(keyword,category,status,sort,page,materialId,soleId,null,null);
    }
    public Page<Map<String,Object>> products(String keyword,Integer category,Boolean status,String sort,Object page,Integer materialId,Integer soleId,BigDecimal minPrice,BigDecimal maxPrice) {
        return products(keyword,category,status,sort,page,materialId,soleId,minPrice,maxPrice,null);
    }
    public Page<Map<String,Object>> products(String keyword,Integer category,Boolean status,String sort,Object page,Integer materialId,Integer soleId,BigDecimal minPrice,BigDecimal maxPrice,Integer colorId) {
        String joinV = " LEFT JOIN (SELECT product_id,MIN(price) AS price,MAX(price) AS max_price,SUM(CAST(quantity AS bigint)) AS stock FROM product_variants GROUP BY product_id) v ON v.product_id=p.id";
        String where = " WHERE 1=1";
        List<Object> args = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            where += " AND (p.name LIKE ? ESCAPE '!' OR p.code LIKE ? ESCAPE '!')";
            String pattern = likePattern(keyword);
            args.add(pattern); args.add(pattern);
        }
        if (category != null) { where += " AND p.category_id=?"; args.add(category); }
        if (status != null) { where += " AND p.status=?"; args.add(status); }
        if (materialId != null || soleId != null || colorId != null) {
            where += " AND EXISTS (SELECT 1 FROM product_variants mv WHERE mv.product_id=p.id AND mv.status=1";
            if(materialId!=null){where+=" AND mv.chat_lieu_id=?";args.add(materialId);}
            if(soleId!=null){where+=" AND mv.de_giay_id=?";args.add(soleId);}
            if(colorId!=null){where+=" AND mv.mau_sac_id=?";args.add(colorId);}
            where+=")";
        }
        if (minPrice != null) { where += " AND v.price >= ?"; args.add(minPrice); }
        if (maxPrice != null) { where += " AND v.price <= ?"; args.add(maxPrice); }
        String countSql = "SELECT COUNT_BIG(*) FROM products p" + joinV + where;
        String order = switch(sort == null ? "newest" : sort) {
            case "name" -> "p.name ASC,p.id DESC"; case "code" -> "p.code ASC,p.id DESC";
            case "price_asc" -> "v.price ASC,p.id DESC"; case "price_desc" -> "v.price DESC,p.id DESC";
            case "stock" -> "COALESCE(v.stock,0) ASC,p.id DESC"; default -> "p.id DESC";
        };
        String sql = "SELECT p.*,c.name AS category_name,v.price,v.max_price,COALESCE(v.stock,0) AS stock FROM products p LEFT JOIN categories c ON c.id=p.category_id" + joinV + where + " ORDER BY " + order;
        Page<Map<String,Object>> result = pagination.sql(jdbc,sql,countSql,args,page);
        if (!result.isEmpty()) {
            var ids = result.getContent().stream().map(p -> p.get("id")).toList();
            String placeholders = String.join(",", java.util.Collections.nCopies(ids.size(),"?"));
            for(String[] attr:java.util.List.of(new String[]{"chat_lieu","chat_lieu_id","material_names"},new String[]{"mau_sac","mau_sac_id","color_names"},new String[]{"de_giay","de_giay_id","sole_names"})) {
                var rows=jdbc.queryForList("SELECT DISTINCT v.product_id,a.ten FROM product_variants v JOIN "+attr[0]+" a ON a.id=v."+attr[1]+" WHERE v.status=1 AND v.product_id IN ("+placeholders+") ORDER BY a.ten",ids.toArray());
                for(var product:result) {
                    var names=rows.stream().filter(r->r.get("product_id").equals(product.get("id"))).map(r->r.get("ten").toString()).toList();
                    product.put(attr[2],String.join(", ",names.stream().limit(3).toList())+(names.size()>3?" +"+(names.size()-3):""));
                    product.put(attr[2].replace("_names","_labels"),names.stream().limit(3).toList());
                    product.put(attr[2].replace("_names","_extra"),Math.max(0,names.size()-3));
                }
            }
        }
        return result;
    }

    public List<Map<String,Object>> variants(Integer productId, String stock, int threshold) {
        return variantsPage(productId,stock,threshold,0).getContent();
    }
    public Page<Map<String,Object>> variantsPage(Integer productId, String stock, int threshold, Object page) {
        return variantsPage(productId,stock,threshold,page,null,null,null,null,null);
    }
    public Page<Map<String,Object>> variantsPage(Integer productId, String stock, int threshold, Object page, Integer materialId) {
        return variantsPage(productId,stock,threshold,page,materialId,null,null,null,null);
    }
    public Page<Map<String,Object>> variantsPage(Integer productId, String stock, int threshold, Object page, Integer materialId, Integer sizeId, Boolean status, String keyword, Integer categoryId) {
        return variantsPage(productId,stock,threshold,page,materialId,sizeId,status,keyword,categoryId,null,null);
    }
    public Page<Map<String,Object>> variantsPage(Integer productId, String stock, int threshold, Object page, Integer materialId, Integer sizeId, Boolean status, String keyword, Integer categoryId,Integer colorId,Integer soleId) {
        String sql = "SELECT v.id,v.product_id,v.size_id,v.chat_lieu_id,p.code,p.name,s.size_value,m.ten AS material_name,cl.ten AS color_name,cl.ma_mau AS color_hex,sole.ten AS sole_name,v.price,v.quantity,v.status FROM product_variants v JOIN products p ON p.id=v.product_id JOIN sizes s ON s.id=v.size_id JOIN chat_lieu m ON m.id=v.chat_lieu_id JOIN mau_sac cl ON cl.id=v.mau_sac_id JOIN de_giay sole ON sole.id=v.de_giay_id WHERE 1=1";
        List<Object> args = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            sql += " AND (p.code LIKE ? ESCAPE '!' OR p.name LIKE ? ESCAPE '!' OR s.size_value LIKE ? ESCAPE '!' OR m.ten LIKE ? ESCAPE '!')";
            String pattern = likePattern(keyword);
            args.add(pattern); args.add(pattern); args.add(pattern); args.add(pattern);
        }
        if (categoryId != null) { sql += " AND p.category_id=?"; args.add(categoryId); }
        if (materialId != null) { sql += " AND v.chat_lieu_id=?"; args.add(materialId); }
        if(colorId!=null){sql+=" AND v.mau_sac_id=?";args.add(colorId);}
        if(soleId!=null){sql+=" AND v.de_giay_id=?";args.add(soleId);}
        if (sizeId != null) { sql += " AND v.size_id=?"; args.add(sizeId); }
        if (productId != null) { sql += " AND v.product_id=?"; args.add(productId); }
        if (status != null) { sql += " AND v.status=?"; args.add(status); }
        if ("in".equals(stock)) { sql += " AND v.quantity > ?"; args.add(threshold); }
        if ("low".equals(stock)) { sql += " AND v.quantity > 0 AND v.quantity <= ?"; args.add(threshold); }
        if ("out".equals(stock)) sql += " AND v.quantity=0";
        return pagination.sql(jdbc,sql+" ORDER BY p.id DESC,s.size_value,v.id","SELECT COUNT_BIG(*) "+sql.substring(sql.indexOf("FROM product_variants")),args,page);
    }

    public List<Map<String,Object>> history(Integer variantId){return history(variantId,0).getContent();}
    public Page<Map<String,Object>> history(Integer variantId,Object page) { return history(variantId,page,null,null,null,null,null,null); }
    public Page<Map<String,Object>> history(Integer variantId,Object page,Integer materialId) { return history(variantId,page,materialId,null,null,null,null,null); }
    public Page<Map<String,Object>> history(Integer variantId,Object page,Integer materialId,String changeType,LocalDate fromDate,LocalDate toDate,String actor,String keyword) {
        return history(variantId,page,materialId,changeType,fromDate,toDate,actor,keyword,null,null);
    }
    public Page<Map<String,Object>> history(Integer variantId,Object page,Integer materialId,String changeType,LocalDate fromDate,LocalDate toDate,String actor,String keyword,Integer colorId,Integer soleId) {
        String from=" FROM inventory_logs l JOIN product_variants v ON v.id=l.variant_id JOIN products p ON p.id=v.product_id JOIN sizes s ON s.id=v.size_id JOIN chat_lieu m ON m.id=v.chat_lieu_id JOIN mau_sac cl ON cl.id=v.mau_sac_id JOIN de_giay sole ON sole.id=v.de_giay_id WHERE 1=1 ";
        List<Object> args=new ArrayList<>();
        if(variantId!=null){from+=" AND l.variant_id=? ";args.add(variantId);}
        if(colorId!=null){from+=" AND v.mau_sac_id=? ";args.add(colorId);}
        if(soleId!=null){from+=" AND v.de_giay_id=? ";args.add(soleId);}
        if(materialId!=null){from+=" AND v.chat_lieu_id=? ";args.add(materialId);}
        if(keyword!=null && !keyword.isBlank()) {
            from+=" AND (p.code LIKE ? ESCAPE '!' OR p.name LIKE ? ESCAPE '!' OR l.reason LIKE ? ESCAPE '!') ";
            String pattern=likePattern(keyword);
            args.add(pattern);args.add(pattern);args.add(pattern);
        }
        if("INCREASE".equals(changeType)){from+=" AND l.quantity_change > 0 ";}
        else if("DECREASE".equals(changeType)){from+=" AND l.quantity_change < 0 ";}
        else if("UNCHANGED".equals(changeType)){from+=" AND l.quantity_change = 0 ";}
        if(fromDate!=null){from+=" AND l.created_at >= ? ";args.add(fromDate.atStartOfDay());}
        if(toDate!=null){from+=" AND l.created_at < ? ";args.add(toDate.plusDays(1).atStartOfDay());}
        if(actor!=null && !actor.isBlank()){from+=" AND l.actor = ? ";args.add(actor.trim());}
        return pagination.sql(jdbc,"SELECT l.*,p.code,p.name,s.size_value,m.ten AS material_name,cl.ten AS color_name,sole.ten AS sole_name"+from+" ORDER BY l.created_at DESC,l.id DESC","SELECT COUNT_BIG(*)"+from,args,page);
    }

    public List<String> distinctHistoryActors() {
        return jdbc.queryForList("SELECT DISTINCT actor FROM inventory_logs WHERE actor IS NOT NULL AND LTRIM(RTRIM(actor)) <> '' ORDER BY actor", String.class);
    }
}
