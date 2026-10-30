package com.example.demodatn.config;
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
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class DataInitializerTest {
    @Test
    void twoRunsCreateOnlyFoundationOnce() throws Exception {
        RoleRepository roles = mock(RoleRepository.class);
        UserRepository users = mock(UserRepository.class);
        EmployeeRepository employees = mock(EmployeeRepository.class);
        CategoryRepository categories = mock(CategoryRepository.class);
        SizeRepository sizes = mock(SizeRepository.class);
        MaterialRepository materials = mock(MaterialRepository.class);
        Set<String> materialRows = new HashSet<>();
        when(materials.existsByName(anyString())).thenAnswer(i -> materialRows.contains(i.getArgument(0)));
        when(materials.save(any())).thenAnswer(i -> { Material m=i.getArgument(0); materialRows.add(m.getName()); return m; });
        PasswordEncoder passwords = mock(PasswordEncoder.class);
        Map<String, Role> roleRows = new HashMap<>();
        Map<String, User> userRows = new HashMap<>();
        List<Category> categoryRows = new ArrayList<>();
        List<Size> sizeRows = new ArrayList<>();
        when(roles.findByName(anyString())).thenAnswer(i -> roleRows.get(i.getArgument(0)));
        when(roles.save(any())).thenAnswer(i -> { Role r=i.getArgument(0); roleRows.put(r.getName(),r); return r; });
        when(users.findByUsername(anyString())).thenAnswer(i -> Optional.ofNullable(userRows.get(i.getArgument(0))));
        when(users.save(any())).thenAnswer(i -> { User u=i.getArgument(0); userRows.put(u.getUsername(),u); return u; });
        when(categories.findAll()).thenAnswer(i -> new ArrayList<>(categoryRows));
        when(categories.save(any())).thenAnswer(i -> { Category c=i.getArgument(0); categoryRows.add(c); return c; });
        when(sizes.findAll()).thenAnswer(i -> new ArrayList<>(sizeRows));
        when(sizes.save(any())).thenAnswer(i -> { Size s=i.getArgument(0); sizeRows.add(s); return s; });
        when(passwords.encode("admin123")).thenReturn("hashed");
        var colors=mock(com.example.demodatn.repository.sanpham.ColorRepository.class);
        var soles=mock(com.example.demodatn.repository.sanpham.SoleRepository.class);
        var colorRows=new ArrayList<com.example.demodatn.entity.sanpham.Color>();
        var soleRows=new ArrayList<com.example.demodatn.entity.sanpham.Sole>();
        when(colors.findAll()).thenAnswer(i->new ArrayList<>(colorRows));
        when(soles.findAll()).thenAnswer(i->new ArrayList<>(soleRows));
        when(colors.save(any())).thenAnswer(i->{var c=(com.example.demodatn.entity.sanpham.Color)i.getArgument(0);colorRows.add(c);return c;});
        when(soles.save(any())).thenAnswer(i->{var c=(com.example.demodatn.entity.sanpham.Sole)i.getArgument(0);soleRows.add(c);return c;});
        DataInitializer initializer = new DataInitializer(roles, users, employees, categories, sizes, materials, colors, soles, passwords);
        initializer.run();
        initializer.run();
        assertEquals(Set.of("ROLE_ADMIN", "ROLE_USER"), roleRows.keySet());
        assertEquals(Set.of("admin"), userRows.keySet());
        assertEquals(3, categoryRows.size());
        assertEquals(3, sizeRows.size());
        assertEquals(8, materialRows.size());
        assertEquals(1,colorRows.size());assertEquals(1,soleRows.size());
        verify(materials,times(8)).save(any());
        verify(employees, times(1)).save(any());
        verify(passwords, times(1)).encode("admin123");
    }
}
