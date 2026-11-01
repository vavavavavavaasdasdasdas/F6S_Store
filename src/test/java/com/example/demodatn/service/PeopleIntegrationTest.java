package com.example.demodatn.service;
import com.example.demodatn.dto.nhanvien.EmployeeDto;
import com.example.demodatn.entity.donhang.Order;
import com.example.demodatn.entity.khachhang.Customer;
import com.example.demodatn.entity.nhanvien.Employee;
import com.example.demodatn.entity.nhanvien.User;
import com.example.demodatn.exception.PeopleValidationException;
import com.example.demodatn.repository.donhang.OrderRepository;
import com.example.demodatn.repository.khachhang.CustomerRepository;
import com.example.demodatn.repository.nhanvien.EmployeeRepository;
import com.example.demodatn.repository.nhanvien.RoleRepository;
import com.example.demodatn.repository.nhanvien.UserRepository;
import com.example.demodatn.security.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import jakarta.validation.Validator;
import java.util.UUID;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@WithMockUser(username="phase7-admin",roles="ADMIN")
class PeopleIntegrationTest {
    @Autowired PeopleService people;
    @Autowired EmployeeRepository employees;
    @Autowired CustomerRepository customers;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired OrderRepository orders;
    @Autowired PasswordEncoder encoder;
    @Autowired UserDetailsServiceImpl details;
    @Autowired AuthenticationManager authentication;
    @Autowired WebApplicationContext context;
    @Autowired Validator validator;
    @Autowired JdbcTemplate jdbc;

    private EmployeeDto employeeForm() {
        String token=UUID.randomUUID().toString().substring(0,8);
        EmployeeDto f=new EmployeeDto();f.setEmployeeCode("P7-"+token);f.setFullName("Nhân viên kiểm thử");
        f.setEmail(token+"@example.invalid");f.setPhone("09"+String.format("%08d",Math.floorMod(token.hashCode(),100000000)));
        f.setPosition("Nhân viên");f.setUsername("p7-"+token);f.setPassword("Phase7!Test");return f;
    }
    private Customer customerWithAccount() {
        EmployeeDto f=employeeForm();
        User u=new User();u.setUsername(f.getUsername());u.setPassword(encoder.encode(f.getPassword()));u.setRole(roles.findByName("ROLE_USER"));users.saveAndFlush(u);
        Customer c=new Customer();c.setCustomerCode(f.getEmployeeCode());c.setFullName("Khách Phase 7");c.setEmail(f.getEmail());c.setPhone(f.getPhone());c.setUser(u);
        return customers.saveAndFlush(c);
    }
    private Order order(Customer c,Employee e) {
        Order o=new Order();o.setCustomer(c);o.setEmployee(e);o.setOrderCode("P7-"+UUID.randomUUID());o.setOrderType("ONLINE");o.setStatus("PENDING");o.setTotalAmount(BigDecimal.TEN);o.setFinalAmount(BigDecimal.TEN);return orders.saveAndFlush(o);
    }
    @Test @Transactional
    void createEmployeeHashesPasswordAuthenticatesAndDisablesTogether() {
        var f=employeeForm();var e=people.saveEmployee(f);
        assertNotNull(e.getUser());assertEquals("ROLE_EMPLOYEE",e.getUser().getRole().getName());
        assertNotEquals(f.getPassword(),e.getUser().getPassword());assertTrue(encoder.matches(f.getPassword(),e.getUser().getPassword()));
        assertTrue(authentication.authenticate(new UsernamePasswordAuthenticationToken(f.getUsername(),f.getPassword())).isAuthenticated());
        people.employeeStatus(e.getId(),false);
        assertFalse(e.getStatus());assertFalse(e.getUser().getStatus());assertFalse(details.loadUserByUsername(f.getUsername()).isEnabled());
        assertThrows(DisabledException.class,()->authentication.authenticate(new UsernamePasswordAuthenticationToken(f.getUsername(),f.getPassword())));
        people.employeeStatus(e.getId(),true);assertTrue(details.loadUserByUsername(f.getUsername()).isEnabled());
    }
    @Test @Transactional
    void deactivatedCustomerKeepsOrdersAndCannotAuthenticate() {
        var c=customerWithAccount();var o=order(c,null);String orderCode=o.getOrderCode();
        assertTrue(people.deleteCustomer(c.getId()).contains("vô hiệu hóa"));
        assertTrue(customers.existsById(c.getId()));assertEquals(orderCode,orders.findById(o.getId()).orElseThrow().getOrderCode());
        assertEquals(BigDecimal.TEN,o.getFinalAmount());assertFalse(details.loadUserByUsername(c.getUser().getUsername()).isEnabled());
        assertEquals(1,orders.findByCustomer_IdOrderByCreatedAtDescIdDesc(c.getId(),PageRequest.of(0,10)).getTotalElements());
    }
    @Test @Transactional
    void userDetailsChecksProfileEvenIfUserFlagWasNotChanged() {
        var c=customerWithAccount();c.setStatus(false);customers.flush();
        assertTrue(c.getUser().getStatus());assertFalse(details.loadUserByUsername(c.getUser().getUsername()).isEnabled());
    }
    @Test @Transactional
    void employeeDeleteIsAtomicAndReferencedEmployeeIsOnlyDisabled() {
        var first=people.saveEmployee(employeeForm());int userId=first.getUser().getId(),employeeId=first.getId();
        people.deleteEmployee(employeeId);assertFalse(users.existsById(userId));assertFalse(employees.existsById(employeeId));
        var second=people.saveEmployee(employeeForm());var o=order(null,second);
        assertTrue(people.deleteEmployee(second.getId()).contains("vô hiệu hóa"));
        assertTrue(employees.existsById(second.getId()));assertTrue(users.existsById(second.getUser().getId()));
        assertFalse(second.getUser().getStatus());assertNotNull(orders.findById(o.getId()).orElseThrow().getEmployee());
    }
    @Test @Transactional
    void currentAccountCannotDisableOrDeleteItselfIncludingLinkedCustomer() {
        var f=employeeForm();f.setUsername("phase7-admin");var e=people.saveEmployee(f);
        assertThrows(IllegalArgumentException.class,()->people.employeeStatus(e.getId(),false));
        assertThrows(IllegalArgumentException.class,()->people.deleteEmployee(e.getId()));
        var c=customerWithAccount();c.setUser(e.getUser());customers.saveAndFlush(c);
        assertThrows(IllegalArgumentException.class,()->people.customerStatus(c.getId(),false));
        assertThrows(IllegalArgumentException.class,()->people.deleteCustomer(c.getId()));
    }
    @Test @Transactional
    void duplicateEmailPhoneAndUsernameReportSpecificFields() {
        var f=employeeForm();people.saveEmployee(f);
        var next=employeeForm();next.setEmail(f.getEmail().toUpperCase());
        assertEquals("email",assertThrows(PeopleValidationException.class,()->people.saveEmployee(next)).getField());
        next.setEmail(employeeForm().getEmail());next.setPhone("+84"+f.getPhone().substring(1));
        assertEquals("phone",assertThrows(PeopleValidationException.class,()->people.saveEmployee(next)).getField());
        next.setPhone(employeeForm().getPhone());next.setUsername(f.getUsername().toUpperCase());
        assertEquals("username",assertThrows(PeopleValidationException.class,()->people.saveEmployee(next)).getField());
        assertTrue(validator.validate(f).isEmpty());f.setPhone("invalid");f.setEmail("invalid");
        assertEquals(2,validator.validate(f).size());
    }
    @Test
    void employeeInsertFailureRollsBackTheAlreadyInsertedUser() {
        int limit=jdbc.queryForObject("SELECT CHARACTER_MAXIMUM_LENGTH FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='dbo' AND TABLE_NAME='employees' AND COLUMN_NAME='full_name'",Integer.class);
        var f=employeeForm();f.setFullName("x".repeat(limit+1));long before=users.count();
        assertThrows(org.springframework.dao.DataAccessException.class,()->people.saveEmployee(f));
        assertEquals(before,users.count());assertTrue(users.findByUsername(f.getUsername()).isEmpty());
        assertFalse(employees.existsByEmployeeCodeIgnoreCaseAndIdNot(f.getEmployeeCode(),-1));
    }
    @Test @Transactional
    void searchIsPagedAndMatchesNamePhoneOrEmail() {
        String token=UUID.randomUUID().toString().substring(0,8);
        for(int i=0;i<12;i++){var f=employeeForm();f.setFullName("P7PAGE-"+token);people.saveEmployee(f);}
        String q="P7PAGE-"+token;
        var first=employees.findByFullNameContainingIgnoreCaseOrPhoneContainingOrEmailContainingIgnoreCase(q,q,q,PageRequest.of(0,10));
        var second=employees.findByFullNameContainingIgnoreCaseOrPhoneContainingOrEmailContainingIgnoreCase(q,q,q,PageRequest.of(1,10));
        assertEquals(12,first.getTotalElements());assertEquals(10,first.getNumberOfElements());assertEquals(2,second.getNumberOfElements());
    }
    @Test @Transactional
    void existingSessionIsRejectedAfterDeactivationAndGetCannotDelete() throws Exception {
        var f=employeeForm();var e=people.saveEmployee(f);var principal=details.loadUserByUsername(f.getUsername());
        people.employeeStatus(e.getId(),false);
        var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        mvc.perform(get("/admin/employees").with(user(principal))).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login?disabled"));
        mvc.perform(get("/admin/employees/delete/"+e.getId())).andExpect(status().isMethodNotAllowed());
    }
    @Test @WithMockUser(roles="USER")
    void customerRoleCannotManagePeople() throws Exception {
        var mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        for(String kind:new String[]{"customers","employees"}) {
            mvc.perform(get("/admin/"+kind)).andExpect(status().isForbidden());
            mvc.perform(post("/admin/"+kind+"/status/1").with(csrf()).param("enabled","false")).andExpect(status().isForbidden());
            mvc.perform(post("/admin/"+kind+"/delete/1").with(csrf())).andExpect(status().isForbidden());
        }
    }
}
