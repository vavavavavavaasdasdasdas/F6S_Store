package com.example.demodatn.service.donhang;

import com.example.demodatn.config.ShopBankProperties;
import com.example.demodatn.entity.donhang.Order;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.*;

class VietQrServiceTest {
    ShopBankProperties configured(){var p=new ShopBankProperties();p.setBin("970436");p.setAccount("123456789");p.setAccountName("NGUYỄN VĂN ÁNH");p.setName("Ngân hàng kiểm thử");return p;}
    @Test void crcMatchesStandardCheckVector(){assertEquals(0x29B1,VietQrService.crc16("123456789"));}
    @Test void handAssembledTlvExampleHasIndependentPythonCrc3E70(){
        String expected="00020101021238530010A0000007270123000697043601091234567890208QRIBFTTA530370454061230005802VN62120808DH00012363043E70";
        assertEquals(expected,VietQrService.payload("970436","123456789","123000","DH000123"));
    }
    @Test void contentIsAsciiAndAtMostTwentyFiveCharacters(){assertEquals("DH001NGUYENHUE",VietQrService.cleanContent("ĐH 001 / Nguyễn-Huệ!"));assertEquals(25,VietQrService.cleanContent("Á".repeat(40)).length());assertEquals("F6S",VietQrService.cleanContent("---"));}
    @Test void missingConfigurationHasNoPayloadButKeepsExactAmountForManualReceipt(){var qr=new VietQrService(new ShopBankProperties()).create(BigDecimal.valueOf(20000000),"DH001");assertFalse(qr.configured());assertNull(qr.payload());assertEquals("Chưa cấu hình tài khoản nhận tiền",qr.message());assertEquals("20000000",qr.amount());}
    @Test void largeMoneyUsesIntegerPlainStringAndNoFloatingPoint(){var qr=new VietQrService(configured()).create(new BigDecimal("9876543210123456.00"),"DH123");assertTrue(qr.configured());assertEquals("9876543210123456",qr.amount());assertTrue(qr.payload().contains("54169876543210123456"));}
    @Test void fractionalMoneyDoesNotGenerateAnIncorrectQr(){var qr=new VietQrService(configured()).create(new BigDecimal("123.50"),"DH123");assertFalse(qr.configured());assertNull(qr.payload());assertTrue(qr.message().contains("số nguyên đồng"));}
    @Test void onlineQrHiddenAfterPaymentCancellationOrFailure(){var service=new VietQrService(configured());var o=new Order();o.setOrderCode("DH000123");o.setOrderType("ONLINE");o.setPaymentMethod("BANK_TRANSFER");o.setPaymentStatus("UNPAID");o.setStatus("PENDING");o.setFinalAmount(BigDecimal.valueOf(123000));assertNotNull(service.forOrder(o));o.setPaymentStatus("PAID");assertNull(service.forOrder(o));o.setPaymentStatus("UNPAID");o.setStatus("CANCELLED");assertNull(service.forOrder(o));o.setStatus("DELIVERY_FAILED");assertNull(service.forOrder(o));}
    @Test void generatedReferencesUseSixRandomCharactersAfterPrefix(){var refs=new HashSet<String>();for(int i=0;i<1000;i++){String r=VietQrService.newReference();assertTrue(r.matches("F6S[A-Z0-9]{6}"));assertTrue(refs.add(r));}}
}
