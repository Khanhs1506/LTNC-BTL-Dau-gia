package sample.form.panels;

import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import sample.AuctionItemDTO;
import sample.form.CategoryFormPanel;

public class FashionFormPanel extends CategoryFormPanel {

    private TextField        txtName, txtBrand, txtMaterial, txtColor, txtCode;
    private ComboBox<String> cmbType, cmbSize, cmbGender, cmbCondition;

    @Override
    protected void buildUI() {
        txtName     = field("VD: Áo phông basic, Túi Hermes Kelly, Giày Nike Air...");
        txtBrand    = field("VD: Gucci, Zara, Local Brand, Handmade...");
        txtMaterial = field("VD: Cotton 100%, Da thật, Lụa tơ tằm...");
        txtColor    = field("VD: Trắng, Đen, Xanh Navy...");
        txtCode     = field("VD: SKU-12345, Mã tag gốc...");

        cmbType = combo("Chọn loại",
                "Áo", "Quần", "Váy / Đầm", "Áo khoác / Blazer",
                "Đồ thể thao", "Đồ lót / Đồ ngủ",
                "Giày / Dép / Boots", "Túi xách / Balo",
                "Phụ kiện (Thắt lưng, Mũ...)", "Trang sức", "Đồng hồ", "Khác");
        cmbSize = combo("Chọn size",
                "XS", "S", "M", "L", "XL", "XXL", "XXXL",
                "Free size", "Số 35", "Số 36", "Số 37", "Số 38",
                "Số 39", "Số 40", "Số 41", "Số 42", "Khác");
        cmbGender = combo("Giới tính",
                "Nam", "Nữ", "Unisex", "Trẻ em");
        cmbCondition = combo("Chọn tình trạng",
                "Mới 100% (còn tag)", "Như mới (đã thử 1-2 lần)",
                "Còn tốt (>90%)", "Đã qua sử dụng", "Vintage / Cổ điển");

        root.getChildren().addAll(
                labeledField("Tên sản phẩm *", txtName),
                row(labeledField("Loại *",          cmbType),
                        labeledField("Giới tính *",     cmbGender)),
                row(labeledField("Thương hiệu *",   txtBrand),
                        labeledField("Size *",           cmbSize)),
                row(labeledField("Tình trạng *",    cmbCondition),
                        labeledField("Chất liệu",        txtMaterial)),
                row(labeledField("Màu sắc",          txtColor),
                        labeledField("Mã sản phẩm / SKU", txtCode))
        );
    }

    @Override
    public String validate() {
        String e;
        if ((e = req(txtName,  "Tên sản phẩm"))    != null) return e;
        if ((e = req(txtBrand, "Thương hiệu"))      != null) return e;
        if ((e = reqCombo(cmbType,      "Loại"))       != null) return e;
        if ((e = reqCombo(cmbGender,    "Giới tính"))  != null) return e;
        if ((e = reqCombo(cmbSize,      "Size"))        != null) return e;
        if ((e = reqCombo(cmbCondition, "Tình trạng")) != null) return e;
        return null;
    }

    @Override
    public void fillDTO(AuctionItemDTO dto) {
        dto.brand = txtBrand.getText().trim();
        dto.description = (dto.description
                + "\nChất liệu: " + txtMaterial.getText()
                + " | Màu: "      + txtColor.getText()
                + " | Mã: "       + txtCode.getText()).strip();
    }

    @Override
    public void reset() {
        txtName.clear(); txtBrand.clear(); txtMaterial.clear();
        txtColor.clear(); txtCode.clear();
        cmbType.setValue(null); cmbSize.setValue(null);
        cmbGender.setValue(null); cmbCondition.setValue(null);
    }
}
