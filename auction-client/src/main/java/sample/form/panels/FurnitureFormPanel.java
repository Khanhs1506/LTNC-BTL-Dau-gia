package sample.form.panels;

import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import sample.AuctionItemDTO;
import sample.form.CategoryFormPanel;

public class FurnitureFormPanel extends CategoryFormPanel {

    private TextField        txtName, txtMaterial, txtDimensions, txtBrand, txtAge;
    private ComboBox<String> cmbType, cmbCondition;

    @Override
    protected void buildUI() {
        txtName       = field("VD: Bộ sofa góc L, Tủ quần áo 4 cánh...");
        txtMaterial   = field("VD: Gỗ sồi, Inox 304, Vải nhung...");
        txtDimensions = field("VD: D200 × R90 × C75 cm");
        txtBrand      = field("VD: IKEA, Hòa Phát, Xuất khẩu...");
        txtAge        = field("VD: Mới mua 6 tháng, 2 năm...");

        cmbType = combo("Chọn loại nội thất",
                "Phòng khách", "Phòng ngủ", "Phòng bếp / Ăn",
                "Phòng làm việc", "Phòng tắm", "Ngoài trời", "Khác");
        cmbCondition = combo("Chọn tình trạng",
                "Mới 100%", "Như mới", "Còn tốt", "Đã qua sử dụng", "Cần sửa chữa");

        root.getChildren().addAll(
                labeledField("Tên sản phẩm *", txtName),
                row(labeledField("Loại nội thất *",     cmbType),
                        labeledField("Tình trạng *",         cmbCondition)),
                row(labeledField("Chất liệu *",          txtMaterial),
                        labeledField("Kích thước (D×R×C)",   txtDimensions)),
                row(labeledField("Thương hiệu / Xuất xứ", txtBrand),
                        labeledField("Thời gian sử dụng",    txtAge))
        );
    }

    @Override
    public String validate() {
        String e;
        if ((e = req(txtName,     "Tên sản phẩm"))  != null) return e;
        if ((e = req(txtMaterial, "Chất liệu"))     != null) return e;
        if ((e = reqCombo(cmbType,      "Loại nội thất")) != null) return e;
        if ((e = reqCombo(cmbCondition, "Tình trạng"))    != null) return e;
        return null;
    }

    @Override
    public void fillDTO(AuctionItemDTO dto) {
        dto.brand = txtBrand.getText().trim();
        dto.description = (dto.description
                + "\nChất liệu: "   + txtMaterial.getText()
                + " | Kích thước: " + txtDimensions.getText()
                + " | Tuổi thọ: "   + txtAge.getText()).strip();
    }

    @Override
    public void reset() {
        txtName.clear(); txtMaterial.clear(); txtDimensions.clear();
        txtBrand.clear(); txtAge.clear();
        cmbType.setValue(null); cmbCondition.setValue(null);
    }
}
