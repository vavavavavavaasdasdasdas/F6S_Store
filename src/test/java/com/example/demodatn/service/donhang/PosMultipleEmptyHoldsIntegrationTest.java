package com.example.demodatn.service.donhang;

import com.example.demodatn.repository.donhang.PosHeldOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
@WithMockUser(username = "admin", roles = "ADMIN")
class PosMultipleEmptyHoldsIntegrationTest {

    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    @Autowired PosHoldService holds;
    @Autowired PosHeldOrderRepository holdRepo;

    MockMvc mvc;

    @BeforeEach
    void setup() {
        com.example.demodatn.TestDatabaseGuard.check(jdbc);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void canCreateUpToFiveEmptyHoldsWithoutRequiringProducts() throws Exception {
        MockHttpSession session = new MockHttpSession();

        // Ban đầu chưa có hóa đơn chờ nào của admin
        List<PosHoldService.HoldSummary> initialHolds = holds.list("admin", "POS");
        int startCount = initialHolds.size();

        // 1. Nhấn thêm hóa đơn chờ lần 1 khi giỏ trống -> Tạo thành công
        mvc.perform(post("/admin/pos/holds/new-tab").session(session).with(csrf())
                        .param("paymentMethod", "CASH"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/pos"))
                .andExpect(flash().attribute("success", "Đã thêm hóa đơn chờ mới."));

        assertEquals(startCount + 1, holds.list("admin", "POS").size(), "Số lượng hóa đơn chờ phải tăng lên 1");
        assertNotNull(session.getAttribute("posLoadedHold"), "Hóa đơn mới tạo phải được active vào session");

        // 2. Nhấn tiếp lần 2 (đơn trước vẫn chưa chọn sản phẩm nào) -> Tạo thành công
        mvc.perform(post("/admin/pos/holds/new-tab").session(session).with(csrf())
                        .param("paymentMethod", "CASH"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/pos"))
                .andExpect(flash().attribute("success", "Đã thêm hóa đơn chờ mới."));

        assertEquals(startCount + 2, holds.list("admin", "POS").size(), "Số lượng hóa đơn chờ phải tăng lên 2");

        // 3. Nhấn tiếp lần 3, 4, 5 cho đến khi đủ 5 hóa đơn
        while (holds.list("admin", "POS").size() < 5) {
            mvc.perform(post("/admin/pos/holds/new-tab").session(session).with(csrf())
                            .param("paymentMethod", "CASH"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/pos"));
        }
        assertEquals(5, holds.list("admin", "POS").size(), "Đã tạo đủ tối đa 5 hóa đơn chờ");

        // 4. Nhấn lần 6 khi đã có đủ 5 hóa đơn chờ -> Bị chặn và thông báo lỗi
        mvc.perform(post("/admin/pos/holds/new-tab").session(session).with(csrf())
                        .param("paymentMethod", "CASH"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/pos"))
                .andExpect(flash().attribute("error", PosHoldService.LIMIT_MESSAGE));

        assertEquals(5, holds.list("admin", "POS").size(), "Vẫn giữ đúng tối đa 5 hóa đơn chờ");

        // 5. Kiểm tra giao diện GET /admin/pos hiển thị 5 hóa đơn chờ
        mvc.perform(get("/admin/pos").session(session))
                .andExpect(status().isOk())
                .andExpect(model().attribute("heldOrders", hasSize(5)))
                .andExpect(content().string(containsString("5/5 hóa đơn")));

        // 6. Xóa bớt 1 hóa đơn chờ -> số lượng giảm còn 4
        var currentList = holds.list("admin", "POS");
        Long toDeleteId = currentList.get(0).id();
        mvc.perform(post("/admin/pos/holds/" + toDeleteId + "/delete").session(session).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/pos"))
                .andExpect(flash().attribute("success", "Đã xóa hóa đơn chờ."));

        assertEquals(4, holds.list("admin", "POS").size(), "Sau khi xóa 1, còn lại 4 hóa đơn chờ");

        // 7. Nhấn thêm lại -> Lại thành công tạo lại hóa đơn thứ 5
        mvc.perform(post("/admin/pos/holds/new-tab").session(session).with(csrf())
                        .param("paymentMethod", "CASH"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/pos"))
                .andExpect(flash().attribute("success", "Đã thêm hóa đơn chờ mới."));

        assertEquals(5, holds.list("admin", "POS").size(), "Đã tạo lại đủ 5 hóa đơn chờ");
    }
}
