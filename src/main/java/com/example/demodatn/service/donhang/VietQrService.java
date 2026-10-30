package com.example.demodatn.service.donhang;

import com.example.demodatn.config.ShopBankProperties;
import com.example.demodatn.entity.donhang.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

@Service @RequiredArgsConstructor
public class VietQrService {
    private final ShopBankProperties bank;
    private static final SecureRandom RANDOM=new SecureRandom();
    private static final String ALPHABET="ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    public record PaymentQr(boolean configured,String message,String payload,String amount,String bankName,String account,String accountName,String content) {}

    public PaymentQr create(BigDecimal amount,String reference) {
        if(amount==null || amount.signum()<=0)return new PaymentQr(false,"Tổng thanh toán bằng 0, không cần chuyển khoản.",null,"0",bank.getName(),bank.getAccount(),bank.getAccountName(),cleanContent(reference));
        final String money;
        try {money=amount.toBigIntegerExact().toString();}
        catch(ArithmeticException ex){return new PaymentQr(false,"Số tiền chuyển khoản phải là số nguyên đồng.",null,null,null,null,null,null);}
        if(!configured())return new PaymentQr(false,"Chưa cấu hình tài khoản nhận tiền",null,money,null,null,null,cleanContent(reference));
        String content=cleanContent(reference);
        String payload=payload(bank.getBin().trim(),bank.getAccount().trim(),money,content);
        return new PaymentQr(true,"",payload,money,bank.getName(),bank.getAccount(),bank.getAccountName(),content);
    }
    public PaymentQr forOrder(Order order) {
        if(order==null || !"ONLINE".equals(order.getOrderType()) || !OrderStatusService.bankTransfer(order)
            || "PAID".equals(order.getPaymentStatus()) || Set.of("CANCELLED","DELIVERY_FAILED").contains(order.getStatus()))return null;
        return create(order.getFinalAmount(),order.getOrderCode());
    }
    public boolean configured() {
        return bank.getBin()!=null && bank.getBin().trim().matches("[0-9]{6}")
            && bank.getAccount()!=null && bank.getAccount().trim().matches("[A-Za-z0-9]{1,34}")
            && bank.getAccountName()!=null && !bank.getAccountName().isBlank()
            && bank.getName()!=null && !bank.getName().isBlank();
    }
    public static String newReference() {
        StringBuilder ref=new StringBuilder("F6S");for(int i=0;i<6;i++)ref.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));return ref.toString();
    }
    public static String cleanContent(String raw) {
        String clean=Normalizer.normalize(raw==null?"":raw,Normalizer.Form.NFD).replaceAll("\\p{M}+","")
            .replace('đ','d').replace('Đ','D').replaceAll("[^A-Za-z0-9]","").toUpperCase(Locale.ROOT);
        return clean.isEmpty()?"F6S":clean.substring(0,Math.min(25,clean.length()));
    }
    private static String tag(String id,String value) {
        int length=value.getBytes(StandardCharsets.UTF_8).length;
        if(length>99)throw new IllegalArgumentException("Dữ liệu QR vượt độ dài cho phép.");
        return id+String.format(Locale.ROOT,"%02d",length)+value;
    }
    public static String payload(String bin,String account,String amount,String content) {
        if(!bin.matches("[0-9]{6}") || !account.matches("[A-Za-z0-9]{1,34}") || !amount.matches("[1-9][0-9]*"))throw new IllegalArgumentException("Thông tin chuyển khoản không hợp lệ.");
        String merchant=tag("00","A000000727")+tag("01",tag("00",bin)+tag("01",account))+tag("02","QRIBFTTA");
        String body=tag("00","01")+tag("01","12")+tag("38",merchant)+tag("53","704")+tag("54",amount)+tag("58","VN")+tag("62",tag("08",cleanContent(content)))+"6304";
        return body+String.format(Locale.ROOT,"%04X",crc16(body));
    }
    public static int crc16(String text) {
        int crc=0xFFFF;
        for(byte b:text.getBytes(StandardCharsets.UTF_8)) {
            crc^=(b&0xFF)<<8;
            for(int i=0;i<8;i++)crc=((crc<<1)^((crc&0x8000)!=0?0x1021:0))&0xFFFF;
        }
        return crc;
    }
}
