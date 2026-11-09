package com.example.demodatn.security;

import com.example.demodatn.dto.nhanvien.EmployeeDto;
import com.example.demodatn.entity.nhanvien.Employee;
import com.example.demodatn.entity.nhanvien.User;
import com.example.demodatn.repository.nhanvien.EmployeeRepository;
import com.example.demodatn.repository.nhanvien.RoleRepository;
import com.example.demodatn.repository.nhanvien.UserRepository;
import com.example.demodatn.service.PeopleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class AdminMutualProtectionIntegrationTest {

    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    @Autowired PeopleService people;
    @Autowired UserRepository users;
    @Autowired EmployeeRepository employees;
    @Autowired RoleRepository roles;
    @Autowired PasswordEncoder encoder;

    MockMvc mvc;
    Employee admin1Employee;
    Employee admin2Employee;
    Employee regularEmployee;

    @BeforeEach
    void setup() {
        com.example.demodatn.TestDatabaseGuard.check(jdbc);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

        var adminRole = roles.findByName("ROLE_ADMIN");
        var empRole = roles.findByName("ROLE_EMPLOYEE");

        // Admin 1
        String t1 = UUID.randomUUID().toString().substring(0, 6);
        User u1 = new User();
        u1.setUsername("adm1_" + t1);
        u1.setPassword(encoder.encode("AdminPass123!"));
        u1.setFullName("Admin Một");
        u1.setEmail("adm1_" + t1 + "@test.vn");
        u1.setPhone("0981" + String.format("%06d", Math.abs(t1.hashCode() % 1000000)));
        u1.setStatus(true);
        u1.setRole(adminRole);
        u1 = users.saveAndFlush(u1);

        Employee e1 = new Employee();
        e1.setEmployeeCode("NV1-" + t1);
        e1.setFullName(u1.getFullName());
        e1.setEmail(u1.getEmail());
        e1.setPhone(u1.getPhone());
        e1.setPosition("Admin");
        e1.setStatus(true);
        e1.setUser(u1);
        admin1Employee = employees.saveAndFlush(e1);

        // Admin 2
        String t2 = UUID.randomUUID().toString().substring(0, 6);
        User u2 = new User();
        u2.setUsername("adm2_" + t2);
        u2.setPassword(encoder.encode("AdminPass123!"));
        u2.setFullName("Admin Hai");
        u2.setEmail("adm2_" + t2 + "@test.vn");
        u2.setPhone("0982" + String.format("%06d", Math.abs(t2.hashCode() % 1000000)));
        u2.setStatus(true);
        u2.setRole(adminRole);
        u2 = users.saveAndFlush(u2);

        Employee e2 = new Employee();
        e2.setEmployeeCode("NV2-" + t2);
        e2.setFullName(u2.getFullName());
        e2.setEmail(u2.getEmail());
        e2.setPhone(u2.getPhone());
        e2.setPosition("Admin");
        e2.setStatus(true);
        e2.setUser(u2);
        admin2Employee = employees.saveAndFlush(e2);

        // Regular Employee
        String t3 = UUID.randomUUID().toString().substring(0, 6);
        User u3 = new User();
        u3.setUsername("emp_" + t3);
        u3.setPassword(encoder.encode("EmpPass123!"));
        u3.setFullName("Nhân Viên Ba");
        u3.setEmail("emp_" + t3 + "@test.vn");
        u3.setPhone("0983" + String.format("%06d", Math.abs(t3.hashCode() % 1000000)));
        u3.setStatus(true);
        u3.setRole(empRole);
        u3 = users.saveAndFlush(u3);

        Employee e3 = new Employee();
        e3.setEmployeeCode("NV3-" + t3);
        e3.setFullName(u3.getFullName());
        e3.setEmail(u3.getEmail());
        e3.setPhone(u3.getPhone());
        e3.setPosition("Nhân viên");
        e3.setStatus(true);
        e3.setUser(u3);
        regularEmployee = employees.saveAndFlush(e3);
    }

    @Test
    void adminCannotEditAnotherAdmin() throws Exception {
        String admin1User = admin1Employee.getUser().getUsername();

        // 1. Admin 1 cố vào form sửa của Admin 2 -> Bị chặn, redirect về detail kèm flash error
        mvc.perform(get("/admin/employees/edit/" + admin2Employee.getId())
                        .with(user(admin1User).roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/employees/detail/" + admin2Employee.getId()))
                .andExpect(flash().attributeExists("error"));

        // 2. Admin 1 cố submit lưu sửa thông tin của Admin 2 -> Bị chặn
        mvc.perform(post("/admin/employees/save")
                        .with(user(admin1User).roles("ADMIN"))
                        .with(csrf())
                        .param("id", admin2Employee.getId().toString())
                        .param("employeeCode", admin2Employee.getEmployeeCode())
                        .param("fullName", "Tên Đã Bị Sửa Trộm")
                        .param("phone", admin2Employee.getPhone())
                        .param("email", admin2Employee.getEmail())
                        .param("position", "Nhân viên")
                        .param("username", admin2Employee.getUser().getUsername()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/employees/detail/" + admin2Employee.getId()))
                .andExpect(flash().attribute("error", "Quản trị viên không thể chỉnh sửa thông tin của quản trị viên khác."));

        // Xác nhận trong DB thông tin Admin 2 không bị thay đổi
        Employee freshAdmin2 = employees.findById(admin2Employee.getId()).orElseThrow();
        assertEquals("Admin Hai", freshAdmin2.getFullName());
        assertEquals("Admin", freshAdmin2.getPosition());

        // 3. Admin 1 cố tắt trạng thái của Admin 2 -> Bị từ chối
        mvc.perform(post("/admin/employees/status/" + admin2Employee.getId())
                        .with(user(admin1User).roles("ADMIN"))
                        .with(csrf())
                        .param("enabled", "false"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", "Không thể thay đổi trạng thái của quản trị viên khác."));

        // 4. Admin 1 cố cấp lại mật khẩu của Admin 2 -> Bị chặn
        mvc.perform(post("/admin/employees/" + admin2Employee.getId() + "/send-account")
                        .with(user(admin1User).roles("ADMIN"))
                        .with(csrf())
                        .param("confirmed", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/employees/detail/" + admin2Employee.getId()))
                .andExpect(flash().attribute("error", "Quản trị viên không thể cấp lại thông tin của quản trị viên khác."));
    }

    @Test
    void adminCanEditSelfAndRegularEmployee() throws Exception {
        String admin1User = admin1Employee.getUser().getUsername();

        // 1. Admin 1 vào form sửa của chính mình -> OK
        mvc.perform(get("/admin/employees/edit/" + admin1Employee.getId())
                        .with(user(admin1User).roles("ADMIN")))
                .andExpect(status().isOk());

        // 2. Admin 1 vào form sửa của Nhân viên cấp dưới -> OK
        mvc.perform(get("/admin/employees/edit/" + regularEmployee.getId())
                        .with(user(admin1User).roles("ADMIN")))
                .andExpect(status().isOk());

        // 3. Admin 1 xem chi tiết Admin 2: canEdit = false, isOtherAdmin = true
        mvc.perform(get("/admin/employees/detail/" + admin2Employee.getId())
                        .with(user(admin1User).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("canEdit", false))
                .andExpect(model().attribute("isOtherAdmin", true));

        // 4. Admin 1 xem chi tiết chính mình: canEdit = true, isOtherAdmin = false
        mvc.perform(get("/admin/employees/detail/" + admin1Employee.getId())
                        .with(user(admin1User).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("canEdit", true))
                .andExpect(model().attribute("isOtherAdmin", false));

        // 5. Admin 1 xem chi tiết nhân viên: canEdit = true, isOtherAdmin = false
        mvc.perform(get("/admin/employees/detail/" + regularEmployee.getId())
                        .with(user(admin1User).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("canEdit", true))
                .andExpect(model().attribute("isOtherAdmin", false));
    }
}
