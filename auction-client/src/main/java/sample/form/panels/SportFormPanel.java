package sample.form.panels;

import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import sample.AuctionItemDTO;
import sample.form.CategoryFormPanel;

public class SportFormPanel extends CategoryFormPanel {

    private TextField        txtName, txtBrand, txtSize, txtAge, txtSpecs;
    private ComboBox<String> cmbSport, cmbCondition;

    @Override
    protected void buildUI() {
        txtName  = field("VD: Vợt cầu lông, Xe đạp địa hình, Tạ đôi...");
        txtBrand = field("VD: Yonex, Giant, Adidas, Nike...");
        txtSize  = field("VD: Size M, 27\", 26 inch, 80kg...");
        txtAge   = field("VD: Mới mua 3 tháng, 1 năm sử dụng...");
        txtSpecs = field("VD: Khung carbon, 21 tốc độ, lông ngỗng thật...");

        cmbSport = combo("Chọn môn thể thao",
                "Cầu lông", "Tennis", "Bóng đá", "Bóng rổ",
                "Bơi lội", "Xe đạp", "Gym / Thể hình",
                "Yoga / Pilates", "Chạy bộ", "Golf", "Khác");
        cmbCondition = combo("Chọn tình trạng",
                "Mới 100%", "Như mới (dùng < 5 lần)",
                "Còn tốt (90%+)", "Đã qua sử dụng", "Cần bảo dưỡng");

        root.getChildren().addAll(
                labeledField("Tên sản phẩm *", txtName),
                row(labeledField("Môn thể thao *",  cmbSport),
                        labeledField("Tình trạng *",     cmbCondition)),
                row(labeledField("Thương hiệu *",    txtBrand),
                        labeledField("Kích cỡ / Thông số", txtSize)),
                labeledField("Mô tả chi tiết / Đặc điểm nổi bật", txtSpecs),
                labeledField("Thời gian sử dụng", txtAge)
        );
    }

    @Override
    public String validate() {
        String e;
        if ((e = req(txtName,  "Tên sản phẩm"))       != null) return e;
        if ((e = req(txtBrand, "Thương hiệu"))         != null) return e;
        if ((e = reqCombo(cmbSport,     "Môn thể thao")) != null) return e;
        if ((e = reqCombo(cmbCondition, "Tình trạng"))   != null) return e;
        return null;
    }

    @Override
    public void fillDTO(AuctionItemDTO dto) {
        dto.brand = txtBrand.getText().trim();
        dto.description = (dto.description
                + "\nMôn: "         + cmbSport.getValue()
                + " | Kích cỡ: "    + txtSize.getText()
                + " | Đặc điểm: "   + txtSpecs.getText()).strip();
    }

    @Override
    public void reset() {
        txtName.clear(); txtBrand.clear(); txtSize.clear();
        txtAge.clear(); txtSpecs.clear();
        cmbSport.setValue(null); cmbCondition.setValue(null);
    }
}
