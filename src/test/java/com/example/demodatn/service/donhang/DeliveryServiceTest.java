package com.example.demodatn.service.donhang;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
class DeliveryServiceTest {
 @ParameterizedTest @CsvSource({
 "2026-10-30T10:00,2026-10-30T03:00:00Z,3,3,2026-11-02,2026-11-02,false",
 "2026-10-30T10:00,2026-10-30T03:00:00Z,5,7,2026-11-04,2026-11-06,false",
 "2026-12-29T10:00,2026-12-29T03:00:00Z,5,7,2027-01-03,2027-01-05,false",
 "2028-02-27T10:00,2028-02-27T03:00:00Z,3,3,2028-03-01,2028-03-01,false",
 "2028-02-24T10:00,2028-02-24T03:00:00Z,5,7,2028-02-29,2028-03-02,false",
 "2026-10-01T10:00,2026-10-10T03:00:00Z,3,3,2026-10-13,2026-10-13,true",
 "2026-10-01T10:00,2026-10-04T03:00:00Z,3,3,2026-10-07,2026-10-07,true",
 "2026-10-01T10:00,2026-10-03T18:00:00Z,3,3,2026-10-07,2026-10-07,true"})
 void snapshotCalendarRangeInVietnam(String placed,String confirmed,int min,int max,String from,String to,boolean late){var e=DeliveryService.calculate(LocalDateTime.parse(placed),Instant.parse(confirmed),min,max);assertEquals(LocalDate.parse(from),e.from());assertEquals(LocalDate.parse(to),e.to());assertEquals(late,e.lateConfirmation());}
 @Test void invalidConfigurationIsRejected(){for(int[] days:new int[][]{{0,3},{3,31},{7,5}})assertThrows(IllegalArgumentException.class,()->DeliveryService.calculate(LocalDateTime.now(),Instant.now(),days[0],days[1]));}
}
