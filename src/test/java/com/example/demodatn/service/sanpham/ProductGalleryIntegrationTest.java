package com.example.demodatn.service.sanpham;
import com.example.demodatn.dto.sanpham.AdminProductForm;
import com.example.demodatn.entity.sanpham.ProductImage;
import com.example.demodatn.exception.CatalogValidationException;
import com.example.demodatn.repository.sanpham.CategoryRepository;
import com.example.demodatn.repository.sanpham.ProductImageRepository;
import com.example.demodatn.repository.sanpham.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ProductGalleryIntegrationTest {
    @org.springframework.beans.factory.annotation.Autowired com.example.demodatn.repository.sanpham.ColorRepository phase5Colors;
    @org.springframework.beans.factory.annotation.Autowired com.example.demodatn.repository.sanpham.SoleRepository phase5Soles;

    @Autowired AdminCatalogService catalog;
    @Autowired ProductGalleryService gallery;
    @Autowired ProductImageRepository images;
    @Autowired ProductRepository products;
    @Autowired CategoryRepository categories;
    @Autowired PlatformTransactionManager manager;
    @Autowired org.springframework.web.context.WebApplicationContext context;
    @org.springframework.beans.factory.annotation.Value("${shop.upload-dir:uploads/products}") String uploadRoot;

    private AdminProductForm form(int count) throws Exception {
        AdminProductForm f=new AdminProductForm();f.setInitialColorId(phase5Colors.findAll().get(0).getId());f.setInitialSoleId(phase5Soles.findAll().get(0).getId());f.setCode("IMG-"+UUID.randomUUID());f.setName("Bộ ảnh kiểm thử");
        f.setStatus(true);f.setCategoryId(categories.findAll().get(0).getId());
        var bytes=new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",bytes);
        var files=new MockMultipartFile[count];for(int i=0;i<count;i++)files[i]=new MockMultipartFile("images","photo.png","image/png",bytes.toByteArray());
        f.setImages(files);return f;
    }
    @Test @Transactional
    void saveReordersChangesMainAndRejectsForeignIds() throws Exception {
        var f=form(4);var p=catalog.saveProduct(f);var rows=gallery.list(p.getId());assertEquals(4,rows.size());
        f.setId(p.getId());f.setImages(null);f.setRemovedImageIds(List.of(rows.get(1).getId()));
        f.setImageOrder(List.of("existing:"+rows.get(3).getId(),"existing:"+rows.get(2).getId(),"existing:"+rows.get(0).getId()));
        f.setMainImageKey("existing:"+rows.get(2).getId());catalog.saveProduct(f);
        var after=gallery.list(p.getId());assertEquals(3,after.size());assertEquals(List.of(0,1,2),after.stream().map(ProductImage::getPosition).toList());
        assertEquals(rows.get(3).getId(),after.get(0).getId());assertTrue(after.get(1).isMain());assertEquals(after.get(1).getPath(),p.getImageUrl());
        f.setRemovedImageIds(List.of(-1L));assertThrows(CatalogValidationException.class,()->catalog.saveProduct(f));
    }
    @Test @Transactional @org.springframework.security.test.context.support.WithMockUser(username="admin",roles="ADMIN")
    void populatedGalleryAndEditFormRenderWithoutRestrictedExpressions() throws Exception {
        var p=catalog.saveProduct(form(4));
        var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(context).apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity()).build();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/products/"+p.getId())).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/admin/products-crud/edit/"+p.getId())).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
    }
    @Test
    void invalidUploadsRollbackProductAndImageRows() throws Exception {
        long before=products.count(), imagesBefore=images.count();
        var zero=form(0);assertThrows(CatalogValidationException.class,()->catalog.saveProduct(zero));
        var nine=form(9);assertThrows(CatalogValidationException.class,()->catalog.saveProduct(nine));
        var fake=form(0);fake.setImages(new MockMultipartFile[]{new MockMultipartFile("images","evil.jpg","image/jpeg",new byte[]{77,90,0,1})});
        assertThrows(CatalogValidationException.class,()->catalog.saveProduct(fake));
        var large=form(0);large.setImages(new MockMultipartFile[]{new MockMultipartFile("images","big.png","image/png",new byte[6*1024*1024])});
        assertThrows(CatalogValidationException.class,()->catalog.saveProduct(large));
        assertEquals(before,products.count());assertEquals(imagesBefore,images.count());
    }
    @Test
    void deletingUnreferencedProductRemovesRowsAndFilesAfterCommit() throws Exception {
        var p=catalog.saveProduct(form(2));var paths=gallery.list(p.getId()).stream().map(ProductImage::getPath).toList();
        catalog.deleteProduct(p.getId());assertFalse(products.existsById(p.getId()));assertTrue(gallery.list(p.getId()).isEmpty());
        for(String path:paths) assertFalse(java.nio.file.Files.exists(java.nio.file.Path.of(uploadRoot,path.substring(path.lastIndexOf('/')+1))));
    }
}
