package com.auction.server.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Test cho class BidTransaction (giao dịch đặt giá)
 *
 * Mỗi lần ai đó đặt giá thành công thì hệ thống tạo ra 1 BidTransaction
 * để lưu lại: ai đặt, đặt bao nhiêu, lúc nào.
 */
public class BidTransactionTest {

    @Test
    void taoGiaoDich_transactionIdKhongDuocNull() {
        BidTransaction gd = new BidTransaction(1, "alice", 5_000_000);

        // transactionId được tạo tự động bằng UUID -> không được null và không rỗng
        assertNotNull(gd.getTransactionId());
        assertFalse(gd.getTransactionId().isEmpty());
    }

    @Test
    void taoGiaoDich_moi_phaiCoIdKhacNhau() {
        // Mỗi giao dịch phải có mã riêng (UUID là duy nhất)
        BidTransaction gd1 = new BidTransaction(1, "alice", 5_000_000);
        BidTransaction gd2 = new BidTransaction(1, "alice", 5_000_000);

        assertNotEquals(gd1.getTransactionId(), gd2.getTransactionId());
    }

    @Test
    void taoGiaoDich_thongTinPhaiDuocLuuDung() {
        BidTransaction gd = new BidTransaction(42, "bob", 12_500_000);

        // Kiểm tra từng field
        assertEquals(42, gd.getAuctionId());
        assertEquals("bob", gd.getBidderUsername());
        assertEquals(12_500_000, gd.getBidAmount());
    }

    @Test
    void taoGiaoDich_timestampKhongDuocNull() {
        BidTransaction gd = new BidTransaction(1, "alice", 5_000_000);

        // Timestamp là thời điểm tạo giao dịch -> phải có
        assertNotNull(gd.getTimestamp());
    }

    @Test
    void constructorDocTuDB_luuDungTatCaField() {
        // Constructor dùng khi đọc dữ liệu từ database (id đã có sẵn)
        String idCoDinh = "abc-123";
        LocalDateTime thoiGian = LocalDateTime.of(2025, 5, 1, 10, 30, 0);

        BidTransaction gd = new BidTransaction(idCoDinh, 7, "carol", 30_000_000, thoiGian);

        assertEquals(idCoDinh, gd.getTransactionId());
        assertEquals(7, gd.getAuctionId());
        assertEquals("carol", gd.getBidderUsername());
        assertEquals(30_000_000, gd.getBidAmount());
        assertEquals(thoiGian, gd.getTimestamp());
    }

    @Test
    void getFormattedTimestamp_phaiDungDinhDangVietNam() {
        // Định dạng Việt Nam: ngày/tháng/năm giờ:phút:giây
        LocalDateTime thoiGian = LocalDateTime.of(2025, 5, 1, 10, 5, 9);
        BidTransaction gd = new BidTransaction("id-1", 1, "alice", 1_000, thoiGian);

        assertEquals("01/05/2025 10:05:09", gd.getFormattedTimestamp());
    }

    @Test
    void toString_phaiChuaTenNguoiDatGia() {
        BidTransaction gd = new BidTransaction(1, "alice", 5_000_000);

        // toString() phải chứa tên người đặt giá để dễ đọc log
        assertTrue(gd.toString().contains("alice"));
    }
}