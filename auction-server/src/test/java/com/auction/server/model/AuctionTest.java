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
 *   assertThrows(Exception.class, ...)      -> kiểm tra có ném exception không
 */
public class AuctionTest {

    // Biến dùng chung cho tất cả các test
    private Auction phienDauGia;
    private Item sanPham;

    /*
     * Hàm này chạy TRƯỚC MỖI @Test
     * Tác dụng: tạo dữ liệu sạch cho từng test, tránh test này ảnh hưởng test kia
     */
    @BeforeEach
    void chuanBiDuLieu() {
        // Tạo một sản phẩm điện tử để test
        sanPham = new ElectronicsItem("sp-001", "MacBook Pro", 10_000_000, 12);

        // Tạo phiên đấu giá: bắt đầu 5 phút trước, kết thúc sau 1 giờ
        phienDauGia = new Auction(
                1,                                      // id phiên
                sanPham,                                // sản phẩm
                LocalDateTime.now().minusMinutes(5),    // thời gian bắt đầu
                LocalDateTime.now().plusHours(1)        // thời gian kết thúc
        );

        // Chuyển phiên sang trạng thái RUNNING để có thể đặt giá
        phienDauGia.startAuction();
    }

    // -----------------------------------------------------------------------
    // NHÓM 1: Kiểm tra trạng thái phiên đấu giá
    // -----------------------------------------------------------------------

    @Test
    void phienMoiTao_phaiBatDauVoiTrangThaiOPEN() {
        // Tạo một phiên mới chưa gọi startAuction()
        Auction phienMoi = new Auction(
                99, sanPham,
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().plusHours(1)
        );
        // Kiểm tra trạng thái phải là OPEN
        assertEquals(Auction.Status.OPEN, phienMoi.getStatus());
    }

    @Test
    void goiStartAuction_phaiChuyenSangRUNNING() {
        // phienDauGia đã được gọi startAuction() trong @BeforeEach
        assertEquals(Auction.Status.RUNNING, phienDauGia.getStatus());
    }

    @Test
    void goiCloseAuction_phaiChuyenSangFINISHED() {
        phienDauGia.closeAuction();
        assertEquals(Auction.Status.FINISHED, phienDauGia.getStatus());
    }

    @Test
    void giaMacDinh_phaiTrungVoiGiaKhoiDiemSanPham() {
        // Khi chưa ai đặt giá, giá cao nhất = giá khởi điểm của sản phẩm
        assertEquals(10_000_000, phienDauGia.getCurrentHighestBid());
    }

    // -----------------------------------------------------------------------
    // NHÓM 2: Kiểm tra đặt giá hợp lệ
    // -----------------------------------------------------------------------

    @Test
    void datGiaHopLe_phaiTraVeTrue() throws Exception {
        // 12 triệu > 10 triệu (giá khởi điểm) -> hợp lệ
        boolean ketQua = phienDauGia.placeBid("alice", 12_000_000);
        assertTrue(ketQua);
    }

    @Test
    void datGiaHopLe_phaiCapNhatGiaCaoNhat() throws Exception {
        phienDauGia.placeBid("alice", 12_000_000);

        // Sau khi đặt, giá cao nhất phải là 12 triệu
        assertEquals(12_000_000, phienDauGia.getCurrentHighestBid());
    }

    @Test
    void datGiaHopLe_phaiCapNhatNguoiDanDau() throws Exception {
        phienDauGia.placeBid("alice", 12_000_000);

        // Người dẫn đầu phải là "alice"
        assertEquals("alice", phienDauGia.getCurrentWinnerUsername());
    }

    @Test
    void datGiaNhieuLan_nguoiThangPhaiLaNguoiDatCaoNhat() throws Exception {
        phienDauGia.placeBid("alice", 12_000_000); // alice đặt trước
        phienDauGia.placeBid("bob", 15_000_000);   // bob đặt cao hơn

        // bob phải là người dẫn đầu
        assertEquals("bob", phienDauGia.getCurrentWinnerUsername());
        assertEquals(15_000_000, phienDauGia.getCurrentHighestBid());
    }

    @Test
    void datGiaHopLe_phaiLuuVaoLichSuBid() throws Exception {
        phienDauGia.placeBid("alice", 12_000_000);

        // Lịch sử phải có đúng 1 giao dịch
        assertEquals(1, phienDauGia.getBidHistory().size());
    }

    @Test
    void datGia3Lan_lichSuPhaiCo3GiaoDich() throws Exception {
        phienDauGia.placeBid("alice", 12_000_000);
        phienDauGia.placeBid("bob",   15_000_000);
        phienDauGia.placeBid("carol", 18_000_000);

        assertEquals(3, phienDauGia.getBidHistory().size());
    }

    // -----------------------------------------------------------------------
    // NHÓM 3: Kiểm tra xử lý lỗi (Exception)
    // -----------------------------------------------------------------------

    @Test
    void datGiaThapHon_phaiNemInvalidBidException() {
        // 5 triệu < 10 triệu (giá khởi điểm) -> phải báo lỗi
        assertThrows(InvalidBidException.class, () -> {
            phienDauGia.placeBid("alice", 5_000_000);
        });
    }

    @Test
    void datGiaBangGiaHienTai_phaiNemInvalidBidException() {
        // Đặt đúng bằng giá khởi điểm -> cũng lỗi (phải CAO HƠN, không phải bằng)
        assertThrows(InvalidBidException.class, () -> {
            phienDauGia.placeBid("alice", 10_000_000);
        });
    }

    @Test
    void datGiaKhiPhienChuaStart_phaiNemAuctionClosedException() {
        // Tạo phiên chưa gọi startAuction() -> vẫn OPEN
        Auction phienChuaStart = new Auction(
                2, sanPham,
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().plusHours(1)
        );

        assertThrows(AuctionClosedException.class, () -> {
            phienChuaStart.placeBid("alice", 12_000_000);
        });
    }

    @Test
    void datGiaKhiPhienDaDong_phaiNemAuctionClosedException() {
        phienDauGia.closeAuction(); // đóng phiên trước

        assertThrows(AuctionClosedException.class, () -> {
            phienDauGia.placeBid("alice", 12_000_000);
        });
    }

    @Test
    void datGiaLoi_giaKhongDuocThayDoi() {
        double giaTruoc = phienDauGia.getCurrentHighestBid();

        // Cố đặt giá thấp -> ném lỗi
        try {
            phienDauGia.placeBid("alice", 100);
        } catch (Exception e) {
            // bỏ qua exception, mục đích là kiểm tra giá không bị thay đổi
        }

        // Giá vẫn phải như cũ
        assertEquals(giaTruoc, phienDauGia.getCurrentHighestBid());
    }

    // -----------------------------------------------------------------------
    // NHÓM 4: Kiểm tra Anti-Sniping (gia hạn thời gian)
    // -----------------------------------------------------------------------

    @Test
    void extendEndTime_phaiGiaHanDungSoPhut() {
        LocalDateTime thoiGianKetThucCu = phienDauGia.getEndTime();

        phienDauGia.extendEndTime(2); // gia hạn thêm 2 phút

        // Thời gian kết thúc mới = cũ + 2 phút
        assertEquals(thoiGianKetThucCu.plusMinutes(2), phienDauGia.getEndTime());
    }

    @Test
    void extendEndTime_nhieuLan_phaiCongDon() {
        LocalDateTime thoiGianKetThucCu = phienDauGia.getEndTime();

        phienDauGia.extendEndTime(1);
        phienDauGia.extendEndTime(1); // gia hạn 2 lần

        // Tổng = +2 phút
        assertEquals(thoiGianKetThucCu.plusMinutes(2), phienDauGia.getEndTime());
    }
}
