package sample;

import sample.AuctionItemDTO;
import javafx.animation.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import sample.form.CategoryPanelManager;

import javax.swing.plaf.TableHeaderUI;
import java.awt.event.ActionEvent;
import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

/**
 * Controller cho giao diện 3-bước đăng bán sản phẩm đấu giá.
 *
 * Dùng với ItemFormFactory:
 *   ItemFormFactory.openCreate(ownerStage);
 *
 * Hoặc mở trực tiếp từ SellerDashboard:
 *   FXMLLoader loader = new FXMLLoader(getClass().getResource("/sample/seller_create_auction.fxml"));
 *   ...
 *   SellerCreateAuctionController ctrl = loader.getController();
 *   AuctionItemDTO result = ctrl.getResult(); // sau showAndWait
 */
public class SellerCreateAuctionController implements Initializable {

    // ── Header / Steps ────────────────────────────────────────
    @FXML private HBox  headerBar;
    @FXML private Label step1Dot, step2Dot, step3Dot;
    @FXML private Label lblProgress;

    // ── Step containers ───────────────────────────────────────
    @FXML private ScrollPane step1, step2, step3;
    @FXML private StackPane  stepContainer;

    // ── Step 1: Sản phẩm ─────────────────────────────────────
    @FXML private ToggleButton btnTypeSoftware, btnTypePhysical, btnTypeCollect, btnTypeOther;
    @FXML private ToggleGroup  tgType;

    // Ảnh
    @FXML private StackPane  imageDropZone;
    @FXML private ImageView  imgPreview;
    @FXML private Label      lblImageIcon, lblImageStatus;
    @FXML private Button     btnPickImage, btnRemoveImage;

    // Thông tin chung
    @FXML private TextField     txtTitle;
    @FXML private ComboBox<String> cmbCategory;
    @FXML private TextField     txtBrand;
    @FXML private TextArea      txtDescription;
    @FXML private Label         lblTitleHint, lblDescCount;

    // SOFTWARE panel
    @FXML private VBox          panelSoftware;
    @FXML private ComboBox<String> cmbPlatform;
    @FXML private TextField     txtVersion;
    @FXML private Spinner<Integer> spnKeyCount;
    @FXML private TextField     txtProductKey;

    // PHYSICAL panel
    @FXML private VBox          panelPhysical;
    @FXML private ComboBox<String> cmbCondition, cmbShipping;
    @FXML private TextField     txtOrigin, txtSize;

    // COLLECTIBLE panel
    @FXML private VBox          panelCollect;
    @FXML private TextField     txtEdition, txtCertificate;

    // ── Step 2: Đấu giá ──────────────────────────────────────
    @FXML private TextField  txtStartPrice, txtStepPrice, txtDepositPrice;
    @FXML private TextField  txtBuyNow, txtReservePrice;
    @FXML private DatePicker dpStart, dpEnd;
    @FXML private TextField  txtStartHour, txtEndHour;
    @FXML private CheckBox   chkAutoExtend, chkNotifyBid, chkPublic, chkAllowBuyNowEnd;
    @FXML private Label      previewStart, previewFee, previewNet;

    // ── Step 3: Xác nhận ─────────────────────────────────────
    @FXML private ImageView imgConfirm;
    @FXML private Label     confirmTitle, confirmCategory, confirmType, confirmDesc;
    @FXML private Label     cRowStart, cRowStep, cRowBuyNow, cRowStart2, cRowEnd, cRowFee;
    @FXML private CheckBox  chkTos, chkRefund;
    @FXML private Label     lblSubmitError;

    // ── Footer ────────────────────────────────────────────────
    @FXML private Button btnBack, btnNext, btnSubmit;

    // ── State ─────────────────────────────────────────────────
    private int            currentStep = 1;
    private File           selectedImageFile;
    private AuctionItemDTO result;
    private double         dragOffX, dragOffY;

    @FXML private Button btnArt, btnVehicle, btnElectronics;
    @FXML private MenuButton btnOther;
    private String selectedCategory = "";

    // ── Init ──────────────────────────────────────────────────
    @FXML private MenuItem menuFurniture, menuRealEstate, menuEvent;
    @FXML private MenuItem menuGame, menuSport, menuBook, menuFashion;

    @FXML private VBox dynamicFormContainer;
    private CategoryPanelManager panelManager;


    @Override
    public void initialize(URL url, ResourceBundle rb) {
        panelManager = new CategoryPanelManager(dynamicFormContainer);

        // MenuItem bindings
        menuFurniture.setOnAction(e  -> switchCategory("Nội thất"));
        menuRealEstate.setOnAction(e -> switchCategory("Bất động sản"));
        menuEvent.setOnAction(e      -> switchCategory("Vé sự kiện"));
        menuGame.setOnAction(e       -> switchCategory("Trò chơi điện tử"));
        menuSport.setOnAction(e      -> switchCategory("Thể thao"));
        menuBook.setOnAction(e       -> switchCategory("Sách"));
        menuFashion.setOnAction(e    -> switchCategory("Thời trang"));

        setupCategories();
        setupDescCounter();
        setupMoneyListeners();

        // Mặc định: Tác phẩm nghệ thuật
        switchCategory("ArtItem");
        btnArt.getStyleClass().add("category-btn-active");
        showStep(1);
    }

    /** Điểm vào duy nhất để chuyển category */
    private void switchCategory(String key) {
        selectedCategory = key;
        panelManager.switchTo(key);
    }

    // Sửa lại method này nhận String thay vì ActionEvent
    private void selectOtherCategory(String categoryName) {
        selectedCategory = "Other";
        btnOther.setText("📦  " + categoryName);

        btnArt.getStyleClass().remove("category-btn-active");
        btnVehicle.getStyleClass().remove("category-btn-active");
        btnElectronics.getStyleClass().remove("category-btn-active");

        if (!btnOther.getStyleClass().contains("category-btn-active"))
            btnOther.getStyleClass().add("category-btn-active");

        updateFormByCategory();
    }

    // Xử lý 3 nút chính
    @FXML
    private void selectCategory(javafx.event.ActionEvent e) {
        Button clicked = (Button) e.getSource();
        clearCategoryStyles();
        clicked.getStyleClass().add("category-btn-active");
        btnOther.setText("📦  Khác");

        if      (clicked == btnArt)         switchCategory("ArtItem");
        else if (clicked == btnVehicle)     switchCategory("VehicleItem");
        else if (clicked == btnElectronics) switchCategory("ElectronicsItem");
    }

    private void switchCategoryFromMenu(String name) {
        clearCategoryStyles();
        btnOther.setText("📦  " + name);
        btnOther.getStyleClass().add("category-btn-active");
        switchCategory(name);
    }

    private void clearCategoryStyles() {
        btnArt.getStyleClass().remove("category-btn-active");
        btnVehicle.getStyleClass().remove("category-btn-active");
        btnElectronics.getStyleClass().remove("category-btn-active");
        btnOther.getStyleClass().remove("category-btn-active");
    }

    // Xử lý submenu "Khác"
    @FXML
    private void selectOtherCategory(ActionEvent e) {
        MenuItem item = (MenuItem) e.getSource();
        selectedCategory = "Other";
        btnOther.setText("📦 " + item.getText()); // hiển thị lựa chọn lên nút

        // Reset active các nút chính
        btnArt.getStyleClass().remove("category-btn-active");
        btnVehicle.getStyleClass().remove("category-btn-active");
        btnElectronics.getStyleClass().remove("category-btn-active");
        btnOther.getStyleClass().add("category-btn-active");

        updateFormByCategory();
    }

    private void updateFormByCategory() {
        // Ẩn/hiện các trường tùy loại sản phẩm
        // Ví dụ: ArtItem cần trường "Chất liệu", VehicleItem cần "Năm sản xuất"...
        switch (selectedCategory) {
            case "ArtItem"         -> showArtFields();
            case "VehicleItem"     -> showVehicleFields();
            case "ElectronicsItem" -> showElectronicsFields();
            default                -> showGenericFields();
        }
    }

    private void showArtFields() {
        // Ẩn hết trước
        hideAllExtraFields();
        // Hiện các field riêng của ArtItem
        // Ví dụ: field chất liệu, kích thước, năm sáng tác...
        // labelMaterial.setVisible(true);
        // fieldMaterial.setVisible(true);
        System.out.println("Showing Art fields");
    }

    private void showVehicleFields() {
        hideAllExtraFields();
        // Hiện các field riêng của VehicleItem
        // Ví dụ: năm sản xuất, hãng xe, số km...
        // labelYear.setVisible(true);
        // fieldYear.setVisible(true);
        System.out.println("Showing Vehicle fields");
    }

    private void showElectronicsFields() {
        hideAllExtraFields();
        // Hiện các field riêng của ElectronicsItem
        // Ví dụ: thương hiệu, bảo hành, tình trạng...
        // labelBrand.setVisible(true);
        // fieldBrand.setVisible(true);
        System.out.println("Showing Electronics fields");
    }

    private void showGenericFields() {
        hideAllExtraFields();
        // Hiện các field chung cho "Khác"
        System.out.println("Showing Generic fields");
    }

    private void hideAllExtraFields() {
        // Ẩn tất cả extra fields ở đây
        // Ví dụ:
        // labelMaterial.setVisible(false);
        // fieldMaterial.setVisible(false);
        // labelYear.setVisible(false);
        // fieldYear.setVisible(false);
    }

    // ── Category & combo setup ────────────────────────────────
    private void setupCategories() {
        cmbCategory.setItems(FXCollections.observableArrayList(
                "Phần mềm", "Game", "Bảo mật", "Thiết kế", "Văn phòng",
                "LEGO / Đồ chơi", "Điện tử", "Thời trang", "Sưu tầm",
                "Nghệ thuật / NFT", "Khác"));

        cmbPlatform.setItems(FXCollections.observableArrayList(
                "Windows", "macOS", "Linux", "Cross-platform",
                "Android", "iOS", "Web"));

        cmbCondition.setItems(FXCollections.observableArrayList(
                "Mới 100%", "Như mới (99%)", "Còn tốt (90%)",
                "Đã qua sử dụng", "Cần sửa chữa"));

        cmbShipping.setItems(FXCollections.observableArrayList(
                "Miễn phí vận chuyển", "Người mua trả ship",
                "Thỏa thuận", "Giao hàng tận nơi (HN/HCM)"));
    }

    // ── Đếm ký tự description ────────────────────────────────
    private void setupDescCounter() {
        txtDescription.textProperty().addListener((obs, o, nw) -> {
            int len = nw.length();
            lblDescCount.setText(len + " / 500 ký tự");
            if (len > 500) {
                txtDescription.setText(o);
                lblDescCount.setText("500 / 500 ký tự (đã đạt giới hạn)");
            }
        });
    }

    // ── Cập nhật preview giá realtime ────────────────────────
    private void setupMoneyListeners() {
        txtStartPrice.textProperty().addListener((obs, o, nw) -> updatePricePreview());
    }

    private void updatePricePreview() {
        try {
            double sp  = parseMoney(txtStartPrice.getText());
            double fee = sp * 0.05;
            double net = sp - fee;
            previewStart.setText(formatMoney(sp));
            previewFee.setText(formatMoney(fee));
            previewNet.setText(formatMoney(net));
        } catch (Exception e) {
            previewStart.setText("0 ₫");
            previewFee.setText("0 ₫");
            previewNet.setText("0 ₫");
        }
    }

    // ── Loại sản phẩm ────────────────────────────────────────
    @FXML
    private void onTypeChanged() {
        panelSoftware.setVisible(false); panelSoftware.setManaged(false);
        panelPhysical.setVisible(false); panelPhysical.setManaged(false);
        panelCollect.setVisible(false);  panelCollect.setManaged(false);

        if (btnTypeSoftware.isSelected()) {
            show(panelSoftware);
            cmbCategory.setValue("Phần mềm");
        } else if (btnTypePhysical.isSelected()) {
            show(panelPhysical);
        } else if (btnTypeCollect.isSelected()) {
            show(panelCollect);
            cmbCategory.setValue("Sưu tầm");
        }
        // OTHER → không có panel phụ
    }

    private void show(VBox panel) {
        panel.setVisible(true); panel.setManaged(true);
    }

    // ── Upload ảnh ────────────────────────────────────────────
    @FXML
    private void handlePickImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Chọn ảnh sản phẩm");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Ảnh", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"));
        File f = fc.showOpenDialog(getStage());
        if (f != null) setImage(f);
    }

    @FXML
    private void handleRemoveImage() {
        selectedImageFile = null;
        imgPreview.setVisible(false);  imgPreview.setManaged(false);
        lblImageIcon.setOpacity(0.4);
        lblImageStatus.setText("Chưa chọn ảnh");
        btnRemoveImage.setVisible(false); btnRemoveImage.setManaged(false);
    }

    @FXML
    private void handleAddExtraImage() {
        // TODO: mở file picker và thêm vào danh sách ảnh phụ
        handlePickImage();
    }

    private void setImage(File f) {
        try {
            if (f.length() > 5 * 1024 * 1024) {
                lblImageStatus.setText("⚠ File vượt 5MB, vui lòng chọn ảnh nhỏ hơn");
                return;
            }
            Image img = new Image(f.toURI().toString());
            imgPreview.setImage(img);
            imgPreview.setVisible(true); imgPreview.setManaged(true);
            lblImageIcon.setOpacity(0);
            selectedImageFile = f;
            lblImageStatus.setText("✅ " + f.getName());
            btnRemoveImage.setVisible(true); btnRemoveImage.setManaged(true);

            // Sync to confirm step
            imgConfirm.setImage(img);
        } catch (Exception e) {
            lblImageStatus.setText("⚠ Không đọc được ảnh");
        }
    }

    // ── Duration shortcuts ────────────────────────────────────
    @FXML void setDuration1d()  { setEndDays(1);  }
    @FXML void setDuration3d()  { setEndDays(3);  }
    @FXML void setDuration7d()  { setEndDays(7);  }
    @FXML void setDuration14d() { setEndDays(14); }
    @FXML void setDuration30d() { setEndDays(30); }

    private void setEndDays(int days) {
        LocalDate start = dpStart.getValue() != null ? dpStart.getValue() : LocalDate.now();
        dpStart.setValue(start);
        dpEnd.setValue(start.plusDays(days));
        if (txtStartHour.getText().isBlank()) txtStartHour.setText("08:00");
        if (txtEndHour.getText().isBlank())   txtEndHour.setText("23:59");
    }

    // ── Navigation ────────────────────────────────────────────
    @FXML
    private void handleNext() {
        if (currentStep == 1 && !validateStep1()) return;
        if (currentStep == 2 && !validateStep2()) return;
        if (currentStep == 2) populateConfirm(); // ← populate trước khi tăng
        currentStep++;
        showStep(currentStep);
    }

    @FXML
    private void handleBack() {
        currentStep--;
        showStep(currentStep);
    }

    private void showStep(int step) {
        // Hide all
        step1.setVisible(false); step1.setManaged(false);
        step2.setVisible(false); step2.setManaged(false);
        step3.setVisible(false); step3.setManaged(false);

        // Show current
        switch (step) {
            case 1 -> { step1.setVisible(true); step1.setManaged(true); }
            case 2 -> { step2.setVisible(true); step2.setManaged(true); }
            case 3 -> { step3.setVisible(true); step3.setManaged(true); }
        }

        // Step dots
        step1Dot.getStyleClass().setAll(step > 1 ? "step-done" : step == 1 ? "step-active" : "step-inactive");
        step2Dot.getStyleClass().setAll(step > 2 ? "step-done" : step == 2 ? "step-active" : "step-inactive");
        step3Dot.getStyleClass().setAll(step == 3 ? "step-active" : "step-inactive");

        // Footer
        btnBack.setVisible(step > 1); btnBack.setManaged(step > 1);
        btnNext.setVisible(step < 3); btnNext.setManaged(step < 3);
        btnSubmit.setVisible(step == 3); btnSubmit.setManaged(step == 3);
        lblProgress.setText("Bước " + step + " / 3");
    }

    // ── Validate ──────────────────────────────────────────────
    private boolean validateStep1() {
        if (selectedImageFile == null) {
            lblImageStatus.setText("⚠ Vui lòng chọn ảnh sản phẩm");
            return false;
        }
        if (txtTitle.getText().isBlank()) {
            lblTitleHint.setText("⚠ Tên sản phẩm không được trống");
            return false;
        }
        if (txtDescription.getText().isBlank()) {
            lblTitleHint.setText("⚠ Vui lòng nhập mô tả");
            return false;
        }
        // Delegate validate xuống panel đang active
        String err = panelManager.validate();
        if (err != null) { lblTitleHint.setText(err); return false; }

        lblTitleHint.setText("");
        return true;
    }

    private boolean validateStep2() {
        try {
            double sp = parseMoney(txtStartPrice.getText());
            if (sp <= 0) throw new Exception("Giá không hợp lệ");
        } catch (Exception e) {
            showError("⚠ Giá khởi điểm không hợp lệ"); return false;
        }
        try {
            double step = parseMoney(txtStepPrice.getText());
            if (step <= 0) throw new Exception();
        } catch (Exception e) {
            showError("⚠ Bước giá không hợp lệ"); return false;
        }
        if (dpEnd.getValue() == null) {
            showError("⚠ Vui lòng chọn ngày kết thúc"); return false;
        }
        if (dpEnd.getValue().isBefore(LocalDate.now())) {
            showError("⚠ Ngày kết thúc phải sau ngày hiện tại"); return false;
        }
        return true;
    }

    private void showError(String msg) {
        lblSubmitError.setText(msg);
    }

    // ── Populate confirm step ─────────────────────────────────
    private void populateConfirm() {
        confirmTitle.setText(txtTitle.getText());
        confirmCategory.setText(nvl(cmbCategory.getValue(), "—"));
        confirmType.setText(getTypeName());
        confirmDesc.setText(txtDescription.getText());

        cRowStart.setText(safeFormatMoney(txtStartPrice.getText()));
        cRowStep.setText(safeFormatMoney(txtStepPrice.getText()));
        cRowBuyNow.setText(txtBuyNow.getText().isBlank() ? "Không có" : safeFormatMoney(txtBuyNow.getText()));

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        if (dpStart.getValue() != null)
            cRowStart2.setText(dpStart.getValue().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    + " " + nvl(txtStartHour.getText(), "08:00"));
        if (dpEnd.getValue() != null)
            cRowEnd.setText(dpEnd.getValue().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    + " " + nvl(txtEndHour.getText(), "23:59"));

        try {
            double fee = parseMoney(txtStartPrice.getText()) * 0.05;
            cRowFee.setText(formatMoney(fee));
        } catch (Exception e) { cRowFee.setText("—"); }
    }

    // ── Submit ────────────────────────────────────────────────
    @FXML
    private void handleSubmit() {
        if (!chkTos.isSelected() || !chkRefund.isSelected()) {
            lblSubmitError.setText("⚠ Vui lòng đọc và tích chọn cả hai điều khoản");
            return;
        }

        btnSubmit.setDisable(true);
        btnSubmit.setText("⏳ Đang đăng...");

        try {
            result = buildDTO();

            Thread task = new Thread(() -> {
                try {
                    String res = ServerConnection.getInstance().createItem(result);
                    javafx.application.Platform.runLater(() -> {
                        if ("CREATE_ITEM_SUCCESS".equalsIgnoreCase(res)) {
                            lblSubmitError.setStyle("-fx-text-fill:#27ae60;");
                            lblSubmitError.setText("✅ Đăng bán thành công! Đang đóng...");
                            new Timeline(new KeyFrame(Duration.seconds(1.2), e -> getStage().close())).play();
                        } else {
                            lblSubmitError.setStyle("-fx-text-fill:#e74c3c;");
                            lblSubmitError.setText("⚠ Server từ chối: " + res);
                            btnSubmit.setDisable(false);
                            btnSubmit.setText("🚀 Đăng bán ngay");
                        }
                    });
                } catch (Exception e) {
                    javafx.application.Platform.runLater(() -> {
                        lblSubmitError.setStyle("-fx-text-fill:#e74c3c;");
                        lblSubmitError.setText("⚠ Lỗi kết nối: " + e.getMessage());
                        btnSubmit.setDisable(false);
                        btnSubmit.setText("🚀 Đăng bán ngay");
                    });
                }
            });
            task.setDaemon(true);
            task.start();
        } catch (Exception e) {
            lblSubmitError.setStyle("-fx-text-fill:#e74c3c;");
            lblSubmitError.setText("⚠ Lỗi dữ liệu: " + e.getMessage());
            btnSubmit.setDisable(false);
            btnSubmit.setText("🚀 Đăng bán ngay");
        }
    }

    // ── Build DTO ─────────────────────────────────────────────
    private AuctionItemDTO buildDTO() {
        AuctionItemDTO dto = new AuctionItemDTO();
        dto.title         = txtTitle.getText().trim();
        dto.description   = txtDescription.getText().trim();
        dto.category      = selectedCategory;
        dto.startingPrice = parseMoney(txtStartPrice.getText());
        dto.stepPrice     = parseMoney(txtStepPrice.getText());
        dto.buyNowPrice   = txtBuyNow.getText().isBlank() ? 0 : parseMoney(txtBuyNow.getText());
        dto.sellerUsername = UserSession.getInstance().getUsername();
        dto.imageUrl      = selectedImageFile != null ? selectedImageFile.toURI().toString() : null;
        dto.status        = "OPEN";

        // Delegate fill xuống panel đang active
        panelManager.fillDTO(dto);

        // Thời gian
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        if (dpStart.getValue() != null) {
            String s = dpStart.getValue().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    + " " + nvl(txtStartHour.getText(), "08:00");
            try { dto.startTime = LocalDateTime.parse(s, fmt); } catch (Exception ignored) {}
        }
        if (dpEnd.getValue() != null) {
            String s = dpEnd.getValue().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    + " " + nvl(txtEndHour.getText(), "23:59");
            try { dto.endTime = LocalDateTime.parse(s, fmt); } catch (Exception ignored) {}
        }
        return dto;
    }

    private int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    // ── Getter kết quả ────────────────────────────────────────
    public AuctionItemDTO getResult() { return result; }

    // ── Drag ─────────────────────────────────────────────────
    private void setupDrag() {
        headerBar.setOnMousePressed(e -> {
            dragOffX = e.getScreenX() - getStage().getX();
            dragOffY = e.getScreenY() - getStage().getY();
        });
        headerBar.setOnMouseDragged(e -> {
            getStage().setX(e.getScreenX() - dragOffX);
            getStage().setY(e.getScreenY() - dragOffY);
        });
    }

    // ── Helpers ───────────────────────────────────────────────
    private Stage getStage() {
        return (Stage) headerBar.getScene().getWindow();
    }

    private String getTypeName() {
        return switch (selectedCategory) {
            case "ArtItem"         -> "Tác phẩm nghệ thuật";
            case "VehicleItem"     -> "Phương tiện";
            case "ElectronicsItem" -> "Thiết bị điện tử";
            default -> selectedCategory.isBlank() ? "Khác" : selectedCategory;
        };
    }

    private String formatMoney(double v) {
        return String.format("%,.0f", v).replace(",", ".") + " ₫";
    }

    private String safeFormatMoney(String raw) {
        try { return formatMoney(parseMoney(raw)); }
        catch (Exception e) { return "—"; }
    }

    private double parseMoney(String s) {
        return Double.parseDouble(
                s.trim().replace(".", "").replace(",", "").replace("₫", "").replace(" ", ""));
    }

    private String nvl(String s, String fallback) {
        return (s != null && !s.isBlank()) ? s : fallback;
    }

    @FXML
    private void handleClose() {
        panelManager.resetAll(); // Reset toàn bộ khi đóng
        getStage().close();
    }
}
