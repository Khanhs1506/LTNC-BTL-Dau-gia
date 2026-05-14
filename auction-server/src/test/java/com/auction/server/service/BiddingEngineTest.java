package com.auction.server.service;

import com.auction.server.model.Auction;
import com.auction.server.model.ElectronicsItem;
import com.auction.server.model.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Test cho BiddingEngine
 *
 * BiddingEngine là "trái tim" xử lý đấu giá:
 *   1. Nhận lệnh đặt giá -> chuyển cho Auction xử lý
 *   2. Nếu bid trong 30 giây cuối -> tự động gia hạn thêm 1 phút (Anti-Sniping)
 *   3. Thông báo cho tất cả Observer biết có bid mới (Realtime Update)
 */
public class BiddingEngineTest {

    private BiddingEngine engine;
    private AuctionManager manager;
    private Item sanPham;

    @BeforeEach
    void chuanBi() {
        engine  = BiddingEngine.getInstance();
        manager = AuctionManager.getInstance();

        // Xóa hết phiên cũ
        List<Auction> danhSachCu = manager.getAllAuctions();
        for (Auction a : danhSachCu) {
            manager.endAuction(a.getId());
        }

        sanPham = new ElectronicsItem("item-1", "TV OLED 65\"", 10_000_000, 12);

        // Tạo phiên RUNNING và add vào manager
        Auction phien = new Auction(
                1, sanPham,
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().plusHours(1)
        );
        phien.startAuction(); // OPEN -> RUNNING
        manager.addAuction(phien);
    }

    // -----------------------------------------------------------------------
    // NHÓM 1: Singleton
    // -----------------------------------------------------------------------

    @Test
    void getInstance_phaiLaCungMotInstance() {
        BiddingEngine a = BiddingEngine.getInstance();
        BiddingEngine b = BiddingEngine.getInstance();
        assertSame(a, b);
    }

    // -----------------------------------------------------------------------
    // NHÓM 2: processBid - đặt giá qua Engine
    // -----------------------------------------------------------------------

    @Test
    void processBid_hopLe_phaiTraVeTrue() throws Exception {
        boolean ketQua = engine.processBid(1, "alice", 12_000_000);
        assertTrue(ketQua);
    }

    @Test
    void processBid_hopLe_giaTrongAuctionPhaiCapNhat() throws Exception {
        engine.processBid(1, "alice", 12_000_000);

        Auction phien = manager.getAuction(1);
        assertEquals(12_000_000, phien.getCurrentHighestBid());
        assertEquals("alice", phien.getCurrentWinnerUsername());
    }

    @Test
    void processBid_auctionIdKhongTonTai_phaiNemException() {
        // ID 9999 chưa được add vào manager
        assertThrows(Exception.class, () -> {
            engine.processBid(9999, "alice", 12_000_000);
        });
    }

    @Test
    void processBid_giaThaHon_phaiNemException() {
        // 100 < 10 triệu (startingPrice) -> phải báo lỗi
        assertThrows(Exception.class, () -> {
            engine.processBid(1, "alice", 100);
        });
    }

    // -----------------------------------------------------------------------
    // NHÓM 3: Anti-Sniping (gia hạn thời gian khi bid cuối)
    // -----------------------------------------------------------------------

    @Test
    void processBid_trongThang30GiayConLai_phaiGiaHanThem1Phut() throws Exception {
        // Tạo phiên SẮP KẾT THÚC (còn 15 giây)
        Auction phienSapXong = new Auction(
                2, sanPham,
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusSeconds(15)  // còn 15 giây
        );
        phienSapXong.startAuction();
        manager.addAuction(phienSapXong);

        LocalDateTime thoiGianCu = phienSapXong.getEndTime();

        engine.processBid(2, "bob", 11_000_000); // đặt giá lúc còn 15 giây

        // endTime phải được đẩy ra sau (gia hạn thêm)
        assertTrue(phienSapXong.getEndTime().isAfter(thoiGianCu));
    }

    @Test
    void processBid_conNhieuThoiGian_khongGiaHan() throws Exception {
        // Phiên còn 1 tiếng (được tạo trong setUp)
        LocalDateTime thoiGianCu = manager.getAuction(1).getEndTime();

        engine.processBid(1, "alice", 12_000_000);

        // endTime KHÔNG được thay đổi
        assertEquals(thoiGianCu, manager.getAuction(1).getEndTime());
    }

    // -----------------------------------------------------------------------
    // NHÓM 4: Observer Pattern (Realtime Update)
    // -----------------------------------------------------------------------

    @Test
    void processBid_phaiThongBaoChoObserver() throws Exception {
        // Dùng List để lưu log thông báo
        List<String> logThongBao = new ArrayList<>();

        // Tạo observer: khi có bid mới thì ghi log
        AuctionObserver observer = (auctionId, tenNguoiDat, soTien) -> {
            logThongBao.add("Phiên " + auctionId + ": " + tenNguoiDat + " đặt " + soTien);
        };

        engine.addObserver(observer);
        engine.processBid(1, "alice", 12_000_000);
        engine.removeObserver(observer); // dọn dẹp sau test

        // Observer phải nhận được đúng 1 thông báo
        assertEquals(1, logThongBao.size());
    }

    @Test
    void addNhieuObserver_tatCaPhaiNhanDuocThongBao() throws Exception {
        List<String> log1 = new ArrayList<>();
        List<String> log2 = new ArrayList<>();

        AuctionObserver obs1 = (id, user, amount) -> log1.add("obs1 nhận");
        AuctionObserver obs2 = (id, user, amount) -> log2.add("obs2 nhận");

        engine.addObserver(obs1);
        engine.addObserver(obs2);
        engine.processBid(1, "alice", 12_000_000);
        engine.removeObserver(obs1);
        engine.removeObserver(obs2);

        // Cả 2 observer đều phải nhận được thông báo
        assertEquals(1, log1.size());
        assertEquals(1, log2.size());
    }

    @Test
    void removeObserver_khongNhanThongBaoNua() throws Exception {
        List<String> log = new ArrayList<>();
        AuctionObserver observer = (id, user, amount) -> log.add("nhận");

        engine.addObserver(observer);
        engine.removeObserver(observer); // remove trước khi bid

        engine.processBid(1, "alice", 12_000_000);

        // Đã remove -> log phải rỗng
        assertEquals(0, log.size());
    }
}