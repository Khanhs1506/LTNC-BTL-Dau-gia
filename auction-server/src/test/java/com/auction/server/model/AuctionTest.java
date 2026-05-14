package com.auction.server.model;

import com.auction.server.exception.AuctionClosedException;
import com.auction.server.exception.InvalidBidException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Test cho class Auction (phiên đấu giá)
 *
 * Cách đọc test:
 *   @BeforeEach  -> chạy TRƯỚC mỗi test, dùng để chuẩn bị dữ liệu
 *   @Test        -> một bài kiểm tra cụ thể
 *   assertEquals(giá_mong_đợi, giá_thực_tế) -> kiểm tra 2 giá trị bằng nhau
 *   assertTrue(điều_kiện)                   -> kiểm tra điều kiện đúng
 *   assertThrowsExactly(Exception.class, ...)-> kiểm tra ĐÚNG LOẠI exception được ném
 *
 * LƯU Ý: Dùng assertThrowsExactly thay vì assertThrows
 *   vì AuctionClosedException và InvalidBidException là checked exception
 *   (extends Exception). Nếu dùng assertThrows thông thường thì nó chỉ
 *   kiểm tra "có phải Exception không" -> luôn đúng dù ném exception gì.
 *   assertThrowsExactly kiểm tra ĐÚNG CLASS được ném ra.
 */
public class AuctionTest {

    private Auction phienDauGia;
    private Item sanPham;

    @BeforeEach
    void chuanBiDuLieu() {
        sanPham = new ElectronicsItem("sp-001", "MacBook Pro", 10_000_000, 12);

        phienDauGia = new Auction(
                1,
                sanPham,
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().plusHours(1)
        );

        phienDauGia.startAuction();
    }

    // -----------------------------------------------------------------------
    // NHÓM 1: Kiểm tra trạng thái phiên đấu giá
    // -----------------------------------------------------------------------

    @Test
    void phienMoiTao_phaiBatDauVoiTrangThaiOPEN() {
        Auction phienMoi = new Auction(
                99, sanPham,
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().plusHours(1)
        );
        assertEquals(Auction.Status.OPEN, phienMoi.getStatus());
    }

    @Test
    void goiStartAuction_phaiChuyenSangRUNNING() {
        assertEquals(Auction.Status.RUNNING, phienDauGia.getStatus());
    }

    @Test
    void goiCloseAuction_phaiChuyenSangFINISHED() {
        phienDauGia.closeAuction();
        assertEquals(Auction.Status.FINISHED, phienDauGia.getStatus());
    }

    @Test
    void giaMacDinh_phaiTrungVoiGiaKhoiDiemSanPham() {
        assertEquals(10_000_000, phienDauGia.getCurrentHighestBid());
    }

    // -----------------------------------------------------------------------
    // NHÓM 2: Kiểm tra đặt giá hợp lệ
    // -----------------------------------------------------------------------

    @Test
    void datGiaHopLe_phaiTraVeTrue() throws Exception {
        boolean ketQua = phienDauGia.placeBid("alice", 12_000_000);
        assertTrue(ketQua);
    }

    @Test
    void datGiaHopLe_phaiCapNhatGiaCaoNhat() throws Exception {
        phienDauGia.placeBid("alice", 12_000_000);
        assertEquals(12_000_000, phienDauGia.getCurrentHighestBid());
    }

    @Test
    void datGiaHopLe_phaiCapNhatNguoiDanDau() throws Exception {
        phienDauGia.placeBid("alice", 12_000_000);
        assertEquals("alice", phienDauGia.getCurrentWinnerUsername());
    }

    @Test
    void datGiaNhieuLan_nguoiThangPhaiLaNguoiDatCaoNhat() throws Exception {
        phienDauGia.placeBid("alice", 12_000_000);
        phienDauGia.placeBid("bob", 15_000_000);
        assertEquals("bob", phienDauGia.getCurrentWinnerUsername());
        assertEquals(15_000_000, phienDauGia.getCurrentHighestBid());
    }

    @Test
    void datGiaHopLe_phaiLuuVaoLichSuBid() throws Exception {
        phienDauGia.placeBid("alice", 12_000_000);
        assertEquals(1, phienDauGia.getBidHistory().size());
    }

    @Test
    void datGia3Lan_lichSuPhaiCo3GiaoDich() throws Exception {
        phienDauGia.placeBid("alice", 12_000_000);
        phienDauGia.placeBid("bob",   15_000_000);
        phienDauGia.placeBid("carol", 18_000_000);
        assertEquals(3, phienDauGia.getBidHistory().size());
    }

    @Test
    void datGiaLoi_giaKhongDuocThayDoi() {
        double giaTruoc = phienDauGia.getCurrentHighestBid();

        try {
            phienDauGia.placeBid("alice", 100);
        } catch (Exception e) {
            // bỏ qua, chỉ cần kiểm tra giá không thay đổi
        }

        assertEquals(giaTruoc, phienDauGia.getCurrentHighestBid());
    }

    // -----------------------------------------------------------------------
    // NHÓM 4: Kiểm tra Anti-Sniping (gia hạn thời gian)
    // -----------------------------------------------------------------------

    @Test
    void extendEndTime_phaiGiaHanDungSoPhut() {
        LocalDateTime thoiGianKetThucCu = phienDauGia.getEndTime();
        phienDauGia.extendEndTime(2);
        assertEquals(thoiGianKetThucCu.plusMinutes(2), phienDauGia.getEndTime());
    }

    @Test
    void extendEndTime_nhieuLan_phaiCongDon() {
        LocalDateTime thoiGianKetThucCu = phienDauGia.getEndTime();
        phienDauGia.extendEndTime(1);
        phienDauGia.extendEndTime(1);
        assertEquals(thoiGianKetThucCu.plusMinutes(2), phienDauGia.getEndTime());
    }
}