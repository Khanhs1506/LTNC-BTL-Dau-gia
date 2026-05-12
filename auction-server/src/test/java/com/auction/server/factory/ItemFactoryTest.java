package com.auction.server.factory;

import com.auction.server.model.ArtItem;
import com.auction.server.model.ElectronicsItem;
import com.auction.server.model.Item;
import com.auction.server.model.VehicleItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Test cho ItemFactory (Factory Method Pattern)
 *
 * ItemFactory dùng để TẠO ra các loại sản phẩm khác nhau
 * (Electronics, Art, Vehicle) mà không cần new trực tiếp.
 *
 * assertInstanceOf(KieuClass.class, doiTuong)
 *   -> kiểm tra doiTuong có phải kiểu KieuClass không
 */
public class ItemFactoryTest {

    private ItemFactory factory;

    @BeforeEach
    void chuanBi() {
        factory = new ItemFactoryImpl();
    }


    // Tạo sản phẩm điện tử (ElectronicsItem)

    @Test
    void taoElectronics_phaiTaoRaDungLoai() {
        //tạo electrolic
        Item sp = factory.createItem("ELECTRONICS", "e-1", "Laptop", 15_000_000, "24");

        // Kiểm tra đúng class được tạo ra
        assertInstanceOf(ElectronicsItem.class, sp);
    }

    @Test
    void taoElectronics_tenVaGiaPhaIDung() {
        Item sp = factory.createItem("ELECTRONICS", "e-1", "Laptop", 15_000_000, "24");

        assertEquals("Laptop", sp.getName());
        assertEquals(15_000_000, sp.getStartingPrice());
    }

    @Test
    void taoElectronics_soThangBaoHanhPhaiDung() {
        Item sp = factory.createItem("ELECTRONICS", "e-1", "Laptop", 15_000_000, "24");

        // Ép kiểu về ElectronicsItem để lấy warrantyMonths
        ElectronicsItem laptop = (ElectronicsItem) sp;
        assertEquals(24, laptop.getWarrantyMonths());
    }

    @Test
    void taoElectronics_khongCoBaoHanh_phaiNemException() {
        // Không truyền số tháng bảo hành -> phải báo lỗi
        assertThrows(IllegalArgumentException.class, () -> {
            factory.createItem("ELECTRONICS", "e-1", "Laptop", 15_000_000);
            // thiếu extraDetails -> lỗi
        });
    }


    //  Tạo sản phẩm nghệ thuật (ArtItem)


    @Test
    void taoArt_phaiTaoRaDungLoai() {
        Item sp = factory.createItem("ART", "a-1", "Tranh Mona Lisa", 50_000_000, "Leonardo");

        assertInstanceOf(ArtItem.class, sp);
    }

    @Test
    void taoArt_tenHoaSiPhaiDung() {
        Item sp = factory.createItem("ART", "a-1", "Tranh Mona Lisa", 50_000_000, "Leonardo");

        ArtItem tranh = (ArtItem) sp;
        assertEquals("Leonardo", tranh.getArtist());
    }

    @Test
    void taoArt_khongCoHoaSi_phaiNemException() {
        assertThrows(IllegalArgumentException.class, () -> {
            factory.createItem("ART", "a-1", "Tranh", 50_000_000);
        });
    }


    // Tạo phương tiện (VehicleItem)


    @Test
    void taoVehicle_phaiTaoRaDungLoai() {
        Item sp = factory.createItem("VEHICLE", "v-1", "Toyota Camry", 800_000_000, "Toyota", "2022");

        assertInstanceOf(VehicleItem.class, sp);
    }

    @Test
    void taoVehicle_hangXeVaNamPhaiDung() {
        Item sp = factory.createItem("VEHICLE", "v-1", "Toyota Camry", 800_000_000, "Toyota", "2022");

        VehicleItem xe = (VehicleItem) sp;
        assertEquals("Toyota", xe.getBrand());
        assertEquals(2022, xe.getYear());
    }

    @Test
    void taoVehicle_thieuNamSanXuat_phaiNemException() {
        // Chỉ truyền hãng xe, không có năm -> lỗi
        assertThrows(IllegalArgumentException.class, () -> {
            factory.createItem("VEHICLE", "v-1", "Car", 500_000_000, "Toyota");
        });
    }


    // Các trường hợp type không hợp lệ


    @Test
    void loaiKhongHoTro_phaiNemException() {
        assertThrows(IllegalArgumentException.class, () -> {
            factory.createItem("SOFA", "s-1", "Ghế sofa", 5_000_000);
        });
    }

    @Test
    void typeNull_phaiNemException() {
        assertThrows(IllegalArgumentException.class, () -> {
            factory.createItem(null, "x-1", "SanPham", 1_000_000);
        });
    }

    @Test
    void typeRong_phaiNemException() {
        assertThrows(IllegalArgumentException.class, () -> {
            factory.createItem("", "x-1", "SanPham", 1_000_000);
        });
    }

    @Test
    void typeVietThuong_vanTaoRaDuocItem() {
        // "electronics" viết thường vẫn phải hoạt động
        Item sp = factory.createItem("electronics", "e-2", "Phone", 5_000_000, "12");
        assertInstanceOf(ElectronicsItem.class, sp);
    }
}