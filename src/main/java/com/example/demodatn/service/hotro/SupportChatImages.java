package com.example.demodatn.service.hotro;
import com.example.demodatn.service.sanpham.ProductImageStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.*;
@Service
public class SupportChatImages {
    private final Path root;
    private final ProductImageStorage images;
    public SupportChatImages(@Value("${shop.chat.upload-dir:uploads/support-chat}") String directory,
                             @Value("${shop.upload-dir:uploads/products}") String productDirectory) {
        root=Path.of(directory).toAbsolutePath().normalize();
        if(root.startsWith(Path.of(productDirectory).toAbsolutePath().normalize())
            ||root.startsWith(Path.of("src/main/resources/static").toAbsolutePath().normalize())
            ||root.startsWith(Path.of("target/classes/static").toAbsolutePath().normalize()))
            throw new IllegalArgumentException("Thư mục ảnh chat phải nằm ngoài tài nguyên công khai.");
        images=new ProductImageStorage(root.toString(),3*1024*1024);
    }
    public String store(MultipartFile file) {
        String path=images.store(file);
        if(path==null)throw new IllegalArgumentException("Vui lòng chọn ảnh.");
        return path.substring(path.lastIndexOf('/')+1);
    }
    public Path path(String name) {
        if(name==null || !name.matches("[a-f0-9-]{36}\\.(png|jpg|webp)"))throw new IllegalArgumentException("Ảnh không hợp lệ.");
        Path file=root.resolve(name).normalize();
        if(!file.getParent().equals(root))throw new IllegalArgumentException("Ảnh không hợp lệ.");
        return file;
    }
    public void remove(String name) {if(name!=null)images.removeNewFile("/uploads/products/"+name);}
}
