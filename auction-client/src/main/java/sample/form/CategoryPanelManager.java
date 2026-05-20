package sample.form;

import javafx.scene.layout.VBox;
import sample.form.panels.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Quản lý toàn bộ category panel:
 *  - Lazy init (chỉ tạo khi lần đầu dùng)
 *  - Cache trong RAM (giữ state khi switch)
 *  - Show/Hide qua setVisible + setManaged (không xóa node)
 *  - Reset toàn bộ khi đóng form
 */
public class CategoryPanelManager {

    // Container trong FXML — tất cả panel đều nằm trong này
    private final VBox container;

    // Cache: category key → panel instance
    private final Map<String, CategoryFormPanel> cache = new HashMap<>();

    // Registry: category key → factory (lazy)
    private final Map<String, Supplier<CategoryFormPanel>> registry = new HashMap<>();

    // Panel đang hiển thị hiện tại
    private CategoryFormPanel activePannel = null;
    private String            activeKey    = null;

    public CategoryPanelManager(VBox container) {
        this.container = container;
        registerAll();
    }

    /** Đăng ký tất cả category → factory */
    private void registerAll() {
        register("ArtItem",          ArtFormPanel::new);
        register("VehicleItem",      VehicleFormPanel::new);
        register("ElectronicsItem",  ElectronicsFormPanel::new);
        register("Nội thất",         FurnitureFormPanel::new);
        register("Bất động sản",     RealEstateFormPanel::new);
        register("Vé sự kiện",       EventTicketFormPanel::new);
        register("Trò chơi điện tử", GameFormPanel::new);
        register("Thể thao",         SportFormPanel::new);
        register("Sách",             BookFormPanel::new);
        register("Thời trang",       FashionFormPanel::new);
    }

    public void register(String key, Supplier<CategoryFormPanel> factory) {
        registry.put(key, factory);
    }

    /**
     * Chuyển sang category mới.
     * - Panel cũ bị hide (giữ state trong RAM).
     * - Panel mới được lazy-init (nếu chưa có) rồi show.
     */
    public void switchTo(String categoryKey) {
        if (categoryKey.equals(activeKey)) return; // không làm gì nếu same

        // Ẩn panel hiện tại
        if (activePannel != null) activePannel.hide();

        // Lấy hoặc tạo panel mới
        CategoryFormPanel next = getOrCreate(categoryKey);
        if (next == null) return;

        next.show();
        activePannel = next;
        activeKey    = categoryKey;
    }

    /** Lấy từ cache hoặc lazy-init và thêm vào container */
    private CategoryFormPanel getOrCreate(String key) {
        if (cache.containsKey(key)) return cache.get(key);

        Supplier<CategoryFormPanel> factory = registry.get(key);
        if (factory == null) return null;

        CategoryFormPanel panel = factory.get();
        cache.put(key, panel);

        // Thêm vào container 1 lần duy nhất
        container.getChildren().add(panel.getRoot());
        return panel;
    }

    /** Validate panel đang active */
    public String validate() {
        if (activePannel == null) return null;
        return activePannel.validate();
    }

    /** Fill DTO từ panel đang active */
    public void fillDTO(sample.AuctionItemDTO dto) {
        if (activePannel != null) activePannel.fillDTO(dto);
    }

    /** Reset TẤT CẢ panel (khi đóng form / reload) */
    public void resetAll() {
        cache.values().forEach(CategoryFormPanel::reset);
        if (activePannel != null) activePannel.hide();
        activePannel = null;
        activeKey    = null;
    }

    public String getActiveKey() { return activeKey; }
}