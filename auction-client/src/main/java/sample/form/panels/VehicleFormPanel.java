package sample.form.panels;

import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import sample.AuctionItemDTO;
import sample.form.CategoryFormPanel;

public class VehicleFormPanel extends CategoryFormPanel {

    private TextField   txtBrand, txtModel, txtYear, txtKm, txtOrigin;
    private ComboBox<String> cmbCondition, cmbShipping;

    @Override
    protected void buildUI() {
        txtBrand    = field("VD: Toyota, Honda, Ford...");
        txtModel    = field("VD: Camry 2.5Q, Wave RSX...");
        txtYear     = field("VD: 2020");
        txtKm       = field("VD: 45.000 km");
        txtOrigin   = field("VD: Nhật, Đức, Việt Nam...");
        cmbCondition = combo("Chọn tình trạng",
                "Mới 100%","Như mới (99%)","Còn tốt (90%)",
                "Đã qua sử dụng","Cần sửa chữa");
        cmbShipping  = combo("Chọn phương thức",
                "Tự lái về","Vận chuyển toàn quốc","Thỏa thuận");

        root.getChildren().addAll(
                row(labeledField("Hãng xe *",          txtBrand),
                        labeledField("Mẫu xe / Dòng xe *", txtModel)),
                row(labeledField("Năm sản xuất *",      txtYear),
                        labeledField("Số km đã đi *",       txtKm)),
                row(labeledField("Tình trạng *",        cmbCondition),
                        labeledField("Xuất xứ",             txtOrigin)),
                labeledField("Phương thức giao xe", cmbShipping)
        );
    }

    @Override
    public String validate() {
        String e;
        if ((e = req(txtBrand, "Hãng xe"))           != null) return e;
        if ((e = req(txtModel, "Mẫu xe"))            != null) return e;
        if ((e = req(txtYear,  "Năm sản xuất"))      != null) return e;
        if ((e = req(txtKm,    "Số km"))             != null) return e;
        if ((e = reqCombo(cmbCondition, "Tình trạng"))!= null) return e;
        return null;
    }

    @Override
    public void fillDTO(AuctionItemDTO dto) {
        dto.brand = txtBrand.getText().trim();
        try { dto.year = Integer.parseInt(txtYear.getText().trim()); }
        catch (Exception ignored) {}
    }

    @Override
    public void reset() {
        txtBrand.clear(); txtModel.clear(); txtYear.clear();
        txtKm.clear();    txtOrigin.clear();
        cmbCondition.setValue(null); cmbShipping.setValue(null);
    }
}