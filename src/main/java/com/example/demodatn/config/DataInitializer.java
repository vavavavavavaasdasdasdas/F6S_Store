package com.example.demodatn.config;
import com.example.demodatn.entity.nhanvien.Employee;
import com.example.demodatn.entity.nhanvien.Role;
import com.example.demodatn.entity.nhanvien.User;
import com.example.demodatn.entity.sanpham.Category;
import com.example.demodatn.entity.sanpham.Material;
import com.example.demodatn.entity.sanpham.Size;
import com.example.demodatn.repository.nhanvien.EmployeeRepository;
import com.example.demodatn.repository.nhanvien.RoleRepository;
import com.example.demodatn.repository.nhanvien.UserRepository;
import com.example.demodatn.repository.sanpham.CategoryRepository;
import com.example.demodatn.repository.sanpham.MaterialRepository;
import com.example.demodatn.repository.sanpham.SizeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final CategoryRepository categoryRepository;
    private final SizeRepository sizeRepository;
    private final MaterialRepository materialRepository;
    private final com.example.demodatn.repository.sanpham.ColorRepository colors;
    private final com.example.demodatn.repository.sanpham.SoleRepository soles;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // 1. Roles
        Role adminRole = roleRepository.findByName("ROLE_ADMIN");
        if (adminRole == null) {
            Role r = new Role();
            r.setName("ROLE_ADMIN");
            adminRole = roleRepository.save(r);
        }
        Role userRole = roleRepository.findByName("ROLE_USER");
        if (userRole == null) {
            Role r = new Role();
            r.setName("ROLE_USER");
            userRole = roleRepository.save(r);
        }

        // 2. Users
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(adminRole);
            admin.setStatus(true);
            userRepository.save(admin);
            Employee employee=new Employee();
            employee.setEmployeeCode("NV-SYSTEM");employee.setFullName("Quản trị hệ thống");
            employee.setUser(admin);employee.setStatus(true);
            employeeRepository.save(employee);
        }
        // 3. Categories
        Category catNike = createCategoryIfNotFound(1, "Nike");
        Category catAdidas = createCategoryIfNotFound(2, "Adidas");
        Category catConverse = createCategoryIfNotFound(3, "Converse");

        for (String name : java.util.List.of("Da thật","Da tổng hợp","Da lộn","Vải canvas","Vải lưới","Vải dệt kim","Cao su","Khác")) {
            if (!materialRepository.existsByName(name)) { Material material = new Material(); material.setName(name); materialRepository.save(material); }
        }

        if(colors.findAll().stream().noneMatch(c->"Khác".equals(c.getName()))) {
            var color=new com.example.demodatn.entity.sanpham.Color();color.setName("Khác");colors.save(color);
        }
        if(soles.findAll().stream().noneMatch(c->"Khác".equals(c.getName()))) {
            var sole=new com.example.demodatn.entity.sanpham.Sole();sole.setName("Khác");soles.save(sole);
        }
        // 4. Sizes
        Size size39 = createSizeIfNotFound(4, "39");
        Size size40 = createSizeIfNotFound(5, "40");
        Size size41 = createSizeIfNotFound(6, "41");

    }

    private Category createCategoryIfNotFound(Integer id, String name) {
        return categoryRepository.findAll().stream().filter(c -> c.getName().equals(name)).findFirst().orElseGet(() -> {
            Category c = new Category();
            c.setName(name);
            c.setStatus(true);
            return categoryRepository.save(c);
        });
    }

    private Size createSizeIfNotFound(Integer id, String sizeValue) {
        return sizeRepository.findAll().stream().filter(s -> s.getSizeValue().equals(sizeValue)).findFirst().orElseGet(() -> {
            Size s = new Size();
            s.setSizeValue(sizeValue);
            return sizeRepository.save(s);
        });
    }

}
