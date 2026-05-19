package sample.form.panels;

import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import sample.AuctionItemDTO;
import sample.form.CategoryFormPanel;

public class ElectronicsFormPanel extends CategoryFormPanel {

    private TextField        txtBrand, txtModel, txtSerial, txtWarranty, txtSpecs;
    private ComboBox<String> cmbCondition, cmbCategory;

    @Override
    protected void buildUI() {
        txtBrand    = field("VD: Samsung, Apple, Sony...");
        txtModel    = field("VD: Galaxy S24 Ultra, iPhone 15 Pro...");
        txtSerial   = field("VD: SN123456789");
        txtWarranty = field("VD: Còn 12 tháng, Hết bảo hành...");
        txtSpecs    = field("VD: RAM 8GB, SSD 256GB, 4K OLED...");

        cmbCondition = combo("Chọn tình trạng",
                "Mới 100% (Seal)", "Mới 99% (Fullbox)", "Còn tốt (90%+)",
                "Đã qua sử dụng", "Cần sửa chữa");
        cmbCategory  = combo("Chọn loại",
                "Điện thoại / Tablet", "Laptop / Máy tính",
                "Tivi / Màn hình", "Âm thanh / Loa", "Camera / Máy ảnh",
                "Đồng hồ thông minh", "Phụ kiện", "Khác");

        root.getChildren().addAll(
                row(labeledField("Thương hiệu *",   txtBrand),
                        labeledField("Model / Dòng *",  txtModel)),
                row(labeledField("Loại thiết bị *", cmbCategory),
                        labeledField("Tình trạng *",    cmbCondition)),
                labeledField("Cấu hình / Thông số kỹ thuật", txtSpecs),
                row(labeledField("Số serial / IMEI",  txtSerial),
                        labeledField("Thông tin bảo hành", txtWarranty))
        );
    }

    @Override
    public String validate() {
        String e;
        if ((e = req(txtBrand, "Thương hiệu"))              != null) return e;
        if ((e = req(txtModel, "Model"))                    != null) return e;
        if ((e = reqCombo(cmbCategory,  "Loại thiết bị"))   != null) return e;
        if ((e = reqCombo(cmbCondition, "Tình trạng"))      != null) return e;
        return null;
    }

    @Override
    public void fillDTO(AuctionItemDTO dto) {
        dto.brand = txtBrand.getText().trim();
        dto.description = (dto.description
                + "\nModel: "    + txtModel.getText()
                + " | Thông số: " + txtSpecs.getText()
                + " | Bảo hành: " + txtWarranty.getText()).strip();
    }

    @Override
    public void reset() {
        txtBrand.clear(); txtModel.clear(); txtSerial.clear();
        txtWarranty.clear(); txtSpecs.clear();
        cmbCondition.setValue(null); cmbCategory.setValue(null);
    }
}
