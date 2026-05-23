package com.auction.server.service;

import com.auction.server.model.Auction;
import com.auction.server.model.ElectronicsItem;
import com.auction.server.model.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Test cho AuctionManager (Singleton Pattern)
 *
 * AuctionManager quản lý tất cả phiên đấu giá đang hoạt động.
 * Dùng pattern Singleton -> chỉ có 1 instance duy nhất trong hệ thống.
 *
 * assertSame(a, b) -> kiểm tra a và b là CÙNG MỘT object (cùng địa chỉ bộ nhớ)
 * assertNull(x)    -> kiểm tra x là null
 */
public class AuctionManagerTest {

    private AuctionManager manager;
    private Item sanPham;

    @BeforeEach
    void chuanBi() {
        manager = AuctionManager.getInstance();

        // Xóa hết phiên cũ từ test trước để tránh ảnh hưởng
        List<Auction> danhSachCu = manager.getAllActiveAuctions();
        for (Auction a : danhSachCu) {
            manager.endAuction(a.getId());
        }

        sanPham = new ElectronicsItem("item-1", "iPhone 15", 20_000_000, 12);
    }

    // -----------------------------------------------------------------------
    // NHÓM 1: Singleton Pattern
    // -----------------------------------------------------------------------

    @Test
    void getInstance_phaiTraVeCungMotInstance() {
        // Gọi getInstance() 2 lần -> phải là CÙNG MỘT object
        AuctionManager a = AuctionManager.getInstance();
        AuctionManager b = AuctionManager.getInstance();

        // assertSame kiểm tra a == b (cùng địa chỉ bộ nhớ)
        assertSame(a, b);
    }

    // -----------------------------------------------------------------------
    // NHÓM 2: Thêm và lấy phiên đấu giá
    // -----------------------------------------------------------------------

    @Test
    void addAuction_vaGetAuction_hoatDongDung() {
        Auction phien = new Auction(100, sanPham,
                LocalDateTime.now(), LocalDateTime.now().plusHours(1));

        manager.addAuction(phien);

        // Lấy lại bằng id -> phải tìm được
        Auction ketQua = manager.getAuction(100);
        assertNotNull(ketQua);
        assertEquals(100, ketQua.getId());
    }

    @Test
    void getAuction_idKhongTonTai_phaiTraVeNull() {
        // ID 99999 chưa được thêm vào -> phải trả về null
        assertNull(manager.getAuction(99999));
    }

    @Test
    void addAuction_null_khongNemException() {
        // Truyền null vào không được crash chương trình
        assertDoesNotThrow(() -> manager.addAuction(null));
    }

    // -----------------------------------------------------------------------
    // NHÓM 3: Lấy tất cả phiên đấu giá
    // -----------------------------------------------------------------------

    @Test
    void getAllAuctions_phaiTraVeTatCaPhienDaAdd() {
        Item sanPham2 = new ElectronicsItem("item-2", "Samsung S24", 15_000_000, 6);
        Auction phien1 = new Auction(200, sanPham, LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        Auction phien2 = new Auction(201, sanPham2, LocalDateTime.now(), LocalDateTime.now().plusHours(1));

        manager.addAuction(phien1);
        manager.addAuction(phien2);

        List<Auction> tatCa = manager.getAllActiveAuctions();

        // Danh sách phải chứa cả 2 phiên vừa thêm
        assertTrue(tatCa.stream().anyMatch(a -> a.getId() == 200));
        assertTrue(tatCa.stream().anyMatch(a -> a.getId() == 201));
    }

    // -----------------------------------------------------------------------
    // NHÓM 4: Kết thúc phiên đấu giá
    // -----------------------------------------------------------------------

    @Test
    void endAuction_phaiXoaPhienKhoiDanhSach() {
        Auction phien = new Auction(300, sanPham,
                LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        manager.addAuction(phien);

        manager.endAuction(300); // kết thúc phiên

        // Sau khi endAuction -> getAuction phải trả về null
        assertNull(manager.getAuction(300));
    }

    @Test
    void endAuction_idKhongTonTai_khongNemException() {
        // Kết thúc phiên không tồn tại -> không được crash
        assertDoesNotThrow(() -> manager.endAuction(99999));
    }
}