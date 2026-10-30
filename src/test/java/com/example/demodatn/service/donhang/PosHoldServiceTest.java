package com.example.demodatn.service.donhang;
import com.example.demodatn.dto.donhang.CartItemDto;
import com.example.demodatn.dto.donhang.PosCheckoutRequest;
import com.example.demodatn.entity.donhang.PosHeldOrder;
import com.example.demodatn.entity.nhanvien.User;
import com.example.demodatn.entity.sanpham.ProductVariant;
import com.example.demodatn.repository.donhang.PosHeldOrderRepository;
import com.example.demodatn.repository.khachhang.CustomerRepository;
import com.example.demodatn.repository.nhanvien.EmployeeRepository;
import com.example.demodatn.repository.nhanvien.UserRepository;
import com.example.demodatn.repository.sanpham.ProductVariantRepository;
import org.junit.jupiter.api.*;
import java.time.*;
import java.util.*;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PosHoldServiceTest {
    private final Clock clock=Clock.fixed(Instant.parse("2026-10-08T16:50:00Z"),ZoneId.of("Asia/Ho_Chi_Minh"));
    private final PosHeldOrderRepository holds=mock(PosHeldOrderRepository.class);
    private final UserRepository users=mock(UserRepository.class);
    private final ProductVariantRepository variants=mock(ProductVariantRepository.class);
    private final PricingService pricing=mock(PricingService.class);
    private final OrderService orders=mock(OrderService.class);
    private final PosHoldService service=new PosHoldService(holds,users,mock(EmployeeRepository.class),mock(CustomerRepository.class),variants,pricing,orders,mock(PosOnlineOrderService.class),clock);

    @Test void endOfDayAt2350ExpiresAtNextMidnight() {
        assertEquals(LocalDateTime.parse("2026-10-09T00:00"),PosHoldService.expiry(clock,"END_OF_DAY"));
    }
    @Test void endOfDayAt0010ExpiresAtFollowingMidnight() {
        var early=Clock.fixed(Instant.parse("2026-10-08T17:10:00Z"),clock.getZone());
        assertEquals(LocalDateTime.parse("2026-10-10T00:00"),PosHoldService.expiry(early,"END_OF_DAY"));
    }
    @Test void hours24PreservesCreationTimeAcrossMidnight() {
        assertEquals(LocalDateTime.parse("2026-10-09T23:50"),PosHoldService.expiry(clock,"HOURS_24"));
    }
    @Test void hours24At0010ExpiresAt0010NextDay() {
        var early=Clock.fixed(Instant.parse("2026-10-08T17:10:00Z"),clock.getZone());
        assertEquals(LocalDateTime.parse("2026-10-10T00:10"),PosHoldService.expiry(early,"HOURS_24"));
    }
    private List<CartItemDto> cart(){return List.of(new CartItemDto(1,2,"Shoe","40",BigDecimal.TEN,2));}
    private PosCheckoutRequest request(){var r=new PosCheckoutRequest();r.setPaymentMethod("CASH");return r;}
    private void setupSave(long count) {
        var user=new User();user.setId(7);when(users.lockByUsername("staff")).thenReturn(Optional.of(user));
        when(holds.countByStaffUser_IdAndExpiresAtAfter(7,service.now())).thenReturn(count);
        when(variants.findById(2)).thenReturn(Optional.of(new ProductVariant()));
        when(holds.saveAndFlush(any())).thenAnswer(inv->{PosHeldOrder h=inv.getArgument(0);h.setId(1L);return h;});
    }
    @Test void fifthIsAllowedWithoutCreatingAnOrder() {
        setupSave(4);assertEquals(1L,service.save("staff",cart(),request(),null));verifyNoInteractions(orders);
    }
    @Test void sixthIsRejectedAtServer() {
        setupSave(5);assertEquals(PosHoldService.LIMIT_MESSAGE,assertThrows(IllegalArgumentException.class,()->service.save("staff",cart(),request(),null)).getMessage());
        verify(holds,never()).saveAndFlush(any());verifyNoInteractions(orders);
    }
    @Test void expiredHoldsDoNotCountAgainstFive() {
        setupSave(3);service.save("staff",cart(),request(),null);
        verify(holds).countByStaffUser_IdAndExpiresAtAfter(7,service.now());
    }
    @Test void expiredOrderIsNotReturnedEvenAtExactExpiryInstant() {
        var h=new PosHeldOrder();h.setExpiresAt(service.now());
        when(holds.findByStaffUser_UsernameAndExpiresAtAfterOrderByCreatedAtAsc("staff",service.now())).thenReturn(List.of(h));
        assertTrue(service.list("staff").isEmpty());
    }
    @Test void expiredOrForeignOrderCannotBeOpenedOrPaid() {
        when(holds.lockActive(1L,"staff",service.now())).thenReturn(Optional.empty());
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.open("staff",1L));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.checkout("staff",1L,cart(),request()));verifyNoInteractions(orders);
    }
}
