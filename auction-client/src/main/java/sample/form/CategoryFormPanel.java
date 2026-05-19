package sample.form;

import javafx.scene.layout.VBox;
import sample.AuctionItemDTO;

/**
 * Base class cho mọi category form panel.
 * Mỗi category extend class này và implement các method trừu tượng.
 */
public abstract class CategoryFormPanel {

    protected final VBox root = new VBox(14);

    public CategoryFormPanel() {
        root.setVisible(false);
        root.setManaged(false);
        buildUI();   // mỗi subclass tự build UI một lần duy nhất
    }

    /** Gọi 1 lần khi khởi tạo — build toàn bộ UI cho panel này */
    protected abstract void buildUI();

    /** Validate các field bắt buộc. Trả null nếu OK, message nếu lỗi */
    public abstract String validate();

    /** Ghi dữ liệu đã nhập vào DTO */
    public abstract void fillDTO(AuctionItemDTO dto);

    /** Reset toàn bộ về giá trị mặc định (gọi khi reload/close form) */
    public abstract void reset();

    // ── Show / Hide ──────────────────────────────────────────
    public void show() {
        root.setVisible(true);
        root.setManaged(true);
    }

    public void hide() {
        root.setVisible(false);
        root.setManaged(false);
    }

    public VBox getRoot() { return root; }

    // ── Helper builders dùng chung cho subclass ──────────────
    protected VBox labeledField(String label,
                                                    javafx.scene.Node control) {
        javafx.scene.control.Label lbl = new javafx.scene.control.Label(label);
        lbl.getStyleClass().add("s-label");
        VBox box = new VBox(4, lbl, control);
        VBox.setVgrow(box, javafx.scene.layout.Priority.NEVER);
        return box;
    }

    protected javafx.scene.layout.HBox row(javafx.scene.Node... nodes) {
        javafx.scene.layout.HBox hbox = new javafx.scene.layout.HBox(12, nodes);
        for (javafx.scene.Node n : nodes)
            javafx.scene.layout.HBox.setHgrow(n, javafx.scene.layout.Priority.ALWAYS);
        return hbox;
    }

    protected javafx.scene.control.TextField field(String prompt) {
        javafx.scene.control.TextField tf = new javafx.scene.control.TextField();
        tf.setPromptText(prompt);
        tf.setMaxWidth(Double.MAX_VALUE);
        tf.getStyleClass().add("s-field");
        return tf;
    }

    @SafeVarargs
    protected final <T> javafx.scene.control.ComboBox<T> combo(
            String prompt, T... items) {
        javafx.scene.control.ComboBox<T> cb = new javafx.scene.control.ComboBox<>();
        cb.setPromptText(prompt);
        cb.setMaxWidth(Double.MAX_VALUE);
        cb.setItems(javafx.collections.FXCollections.observableArrayList(items));
        cb.getStyleClass().add("s-combo");
        return cb;
    }

    protected String req(javafx.scene.control.TextField tf, String name) {
        if (tf.getText().isBlank()) return "⚠ Vui lòng điền: " + name;
        return null;
    }

    protected String reqCombo(javafx.scene.control.ComboBox<?> cb, String name) {
        if (cb.getValue() == null) return "⚠ Vui lòng chọn: " + name;
        return null;
    }
}