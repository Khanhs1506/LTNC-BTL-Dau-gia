package com.auction.server.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Test cho Bidder và Seller (kế thừa từ User)
 *
 * Kiểm tra:
 * - Tính kế thừa (Inheritance): Bidder, Seller đều là User
 * - Tính đa hình (Polymorphism): displayRoleInfo() hoạt động đúng
 * - Các chức năng riêng: balance, joinAuction, addToFavorites, ...
 */
public class UserModelTest {

    // -----------------------------------------------------------------------
    // NHÓM 1: Bidder
    // -----------------------------------------------------------------------

    @Test
    void bidder_rolePhaiLa_BIDDER() {
        Bidder bidder = new Bidder("b-1", "alice", "pass123", 5_000_000);
        assertEquals("BIDDER", bidder.getRole());
    }

    @Test
    void bidder_luuDungSoDu() {
        Bidder bidder = new Bidder("b-1", "alice", "pass123", 5_000_000);
        assertEquals(5_000_000, bidder.getBalance());
    }

    @Test
    void bidder_setBalance_capNhatDung() {
        Bidder bidder = new Bidder("b-1", "alice", "pass123", 5_000_000);
        bidder.setBalance(10_000_000);
        assertEquals(10_000_000, bidder.getBalance());
    }

    @Test
    void bidder_joinAuction_themVaoDanhSach() {
        Bidder bidder = new Bidder("b-1", "alice", "pass123", 0);
        bidder.joinAuction("item-1");

        assertTrue(bidder.getParticipatedItemIds().contains("item-1"));
    }

    @Test
    void bidder_joinAuction_khongThemTrung() {
        Bidder bidder = new Bidder("b-1", "alice", "pass123", 0);
        bidder.joinAuction("item-1");
        bidder.joinAuction("item-1"); // lần 2 cùng item

        // Danh sách chỉ có 1 phần tử, không bị trùng
        assertEquals(1, bidder.getParticipatedItemIds().size());
    }

    @Test
    void bidder_addToFavorites_themDuocVaoYeuThich() {
        Bidder bidder = new Bidder("b-1", "alice", "pass123", 0);
        bidder.addToFavorites("item-5");

        assertTrue(bidder.getFavoriteItemIds().contains("item-5"));
    }

    @Test
    void bidder_removeFromFavorites_xoaDuocKhoiYeuThich() {
        Bidder bidder = new Bidder("b-1", "alice", "pass123", 0);
        bidder.addToFavorites("item-5");
        bidder.removeFromFavorites("item-5");

        assertFalse(bidder.getFavoriteItemIds().contains("item-5"));
    }

    // -----------------------------------------------------------------------
    // NHÓM 2: Seller
    // -----------------------------------------------------------------------

    @Test
    void seller_rolePhaiLa_SELLER() {
        Seller seller = new Seller("s-1", "bob", "pass456");
        assertEquals("SELLER", seller.getRole());
    }

    @Test
    void seller_ratingMacDinh_phaiLa5() {
        // Người bán mới mặc định 5 sao
        Seller seller = new Seller("s-1", "bob", "pass456");
        assertEquals(5.0, seller.getRating());
    }

    @Test
    void seller_setRating_capNhatDung() {
        Seller seller = new Seller("s-1", "bob", "pass456");
        seller.setRating(4.2);
        assertEquals(4.2, seller.getRating());
    }

    @Test
    void seller_addItem_themSanPhamVaoDanhSach() {
        Seller seller = new Seller("s-1", "bob", "pass456");
        seller.addItem("item-10");

        assertTrue(seller.getMyItemIds().contains("item-10"));
    }

    @Test
    void seller_addItem_khongThemTrung() {
        Seller seller = new Seller("s-1", "bob", "pass456");
        seller.addItem("item-10");
        seller.addItem("item-10"); // thêm lần 2

        assertEquals(1, seller.getMyItemIds().size());
    }

    @Test
    void seller_removeItem_xoaSanPhamKhoiDanhSach() {
        Seller seller = new Seller("s-1", "bob", "pass456");
        seller.addItem("item-10");
        seller.removeItem("item-10");

        assertFalse(seller.getMyItemIds().contains("item-10"));
    }

    // -----------------------------------------------------------------------
    // NHÓM 3: OOP - Kế thừa & Đa hình
    // -----------------------------------------------------------------------

    @Test
    void bidder_phaiLaSubclassCuaUser() {
        // Kiểm tra Inheritance: Bidder kế thừa User
        Bidder bidder = new Bidder("b-1", "alice", "pass123", 0);
        assertInstanceOf(User.class, bidder);
    }

    @Test
    void seller_phaiLaSubclassCuaUser() {
        // Kiểm tra Inheritance: Seller kế thừa User
        Seller seller = new Seller("s-1", "bob", "pass456");
        assertInstanceOf(User.class, seller);
    }

    @Test
    void bidder_displayRoleInfo_khongNemException() {
        // Kiểm tra Polymorphism: override method không bị crash
        Bidder bidder = new Bidder("b-1", "alice", "pass123", 100_000);
        assertDoesNotThrow(() -> bidder.displayRoleInfo());
    }

    @Test
    void seller_displayRoleInfo_khongNemException() {
        Seller seller = new Seller("s-1", "bob", "pass456");
        assertDoesNotThrow(() -> seller.displayRoleInfo());
    }
}
