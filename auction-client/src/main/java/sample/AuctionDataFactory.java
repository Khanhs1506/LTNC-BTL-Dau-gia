package sample;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AuctionDataFactory {

    private static final String[] CATEGORIES = {
            "Biển số xe", "Bất động sản", "Nghệ thuật",
            "Xe cộ", "Đồng hồ", "Trang sức", "Điện thoại",
            "Máy tính", "Nội thất", "Vé sự kiện"
    };

    // Tiêu đề mẫu theo từng category
    private static final java.util.Map<String, String[]> TITLES_BY_CAT =
            new java.util.HashMap<String, String[]>() {{
                put("Biển số xe",   new String[]{"BKS 30K", "BKS 51K", "BKS 43K", "BKS 29A", "BKS 80A"});
                put("Bất động sản", new String[]{"Biệt thự Quận 2", "Căn hộ Hà Nội", "Nhà phố Đà Nẵng", "Villa Nha Trang"});
                put("Nghệ thuật",   new String[]{"Tranh Bùi Xuân Phái", "Tượng đồng cổ", "Gốm Bát Tràng", "Tranh lụa"});
                put("Xe cộ",        new String[]{"Mercedes S500", "Porsche 911", "BMW M5", "Ferrari 488"});
                put("Đồng hồ",      new String[]{"Rolex Submariner", "Patek Philippe", "Omega Seamaster", "Audemars Piguet"});
                put("Trang sức",    new String[]{"Nhẫn kim cương", "Vòng cổ vàng 18K", "Bông tai sapphire"});
                put("Điện thoại",   new String[]{"iPhone 15 Pro Max", "Samsung S24 Ultra", "Sony Xperia 1 VI"});
                put("Máy tính",     new String[]{"MacBook Pro M3", "Dell XPS 15", "Surface Pro 9"});
                put("Nội thất",     new String[]{"Bộ sofa da thật", "Tủ gỗ sồi cổ điển", "Đèn chùm pha lê"});
                put("Vé sự kiện",   new String[]{"VIP Concert BlackPink", "F1 Grand Prix VIP", "Champions League Final"});
            }};

    public static List<HomeController.AuctionItem> generate(int count) {
        Random rng = new Random();
        List<HomeController.AuctionItem> list = new ArrayList<>();

        String[] catKeys = CATEGORIES;

        for (int i = 0; i < count; i++) {
            // Chọn category ngẫu nhiên
            String cat    = catKeys[rng.nextInt(catKeys.length)];
            String[] titlesForCat = TITLES_BY_CAT.getOrDefault(cat, new String[]{"Item"});
            String title  = titlesForCat[rng.nextInt(titlesForCat.length)]
                    + " #" + (i + 1);

            double base    = 50_000_000 + rng.nextInt(500) * 1_000_000.0;
            double highest = base * (1.0 + rng.nextDouble() * 0.8);
            int    bids    = 5 + rng.nextInt(60);
            int    daysLeft = 5 + rng.nextInt(200);

            java.time.LocalDate end = java.time.LocalDate.now().plusDays(daysLeft);
            String endDate = end.format(
                    java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));

            int h = rng.nextInt(24), m = rng.nextInt(60), s = rng.nextInt(60);
            String countdown = String.format("%02d:%02d:%02d", h, m, s);

            // ✅ 7 tham số — khớp với constructor mới
            list.add(new HomeController.AuctionItem(
                    title,
                    formatVND(base),
                    formatVND(highest),
                    countdown,
                    bids,
                    endDate,
                    cat          // ← tham số category bị thiếu trước đây
            ));
        }
        return list;
    }

    private static String formatVND(double amount) {
        return String.format("%,.0f VNĐ", amount).replace(",", ".");
    }
}