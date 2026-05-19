package sample.form.panels;

import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import sample.AuctionItemDTO;
import sample.form.CategoryFormPanel;

public class RealEstateFormPanel extends CategoryFormPanel {

    private TextField        txtAddress, txtArea, txtFloor, txtLegalDoc, txtContact;
    private ComboBox<String> cmbType, cmbDirection, cmbLegalStatus;

    @Override
    protected void buildUI() {
        txtAddress  = field("VD: 123 Nguyễn Huệ, Q.1, TP.HCM...");
        txtArea     = field("VD: 75 m²");
        txtFloor    = field("VD: Tầng 5 / 12, Nhà 3 tầng...");
        txtLegalDoc = field("VD: Sổ đỏ, Sổ hồng, HĐMB...");
        txtContact  = field("VD: 0912 345 678");

        cmbType = combo("Chọn loại bất động sản",
                "Căn hộ chung cư", "Nhà phố / Nhà riêng",
                "Biệt thự", "Đất nền", "Nhà xưởng / Kho",
                "Văn phòng / Mặt bằng", "Khách sạn / Homestay");
        cmbDirection = combo("Chọn hướng",
                "Đông", "Tây", "Nam", "Bắc",
                "Đông Nam", "Đông Bắc", "Tây Nam", "Tây Bắc");
        cmbLegalStatus = combo("Tình trạng pháp lý",
                "Sổ đỏ / Sổ hồng đầy đủ", "Đang chờ sổ",
                "Hợp đồng mua bán", "Chưa có sổ", "Đang tranh chấp");

        root.getChildren().addAll(
                labeledField("Địa chỉ *",                   txtAddress),
                row(labeledField("Loại bất động sản *",     cmbType),
                        labeledField("Diện tích *",              txtArea)),
                row(labeledField("Tầng / Số tầng",          txtFloor),
                        labeledField("Hướng",                   cmbDirection)),
                row(labeledField("Tình trạng pháp lý *",    cmbLegalStatus),
                        labeledField("Giấy tờ hiện có",         txtLegalDoc)),
                labeledField("Liên hệ xem nhà", txtContact)
        );
    }

    @Override
    public String validate() {
        String e;
        if ((e = req(txtAddress, "Địa chỉ"))                      != null) return e;
        if ((e = req(txtArea,    "Diện tích"))                     != null) return e;
        if ((e = reqCombo(cmbType,        "Loại bất động sản"))    != null) return e;
        if ((e = reqCombo(cmbLegalStatus, "Tình trạng pháp lý"))   != null) return e;
        return null;
    }

    @Override
    public void fillDTO(AuctionItemDTO dto) {
        dto.description = (dto.description
                + "\nĐịa chỉ: "    + txtAddress.getText()
                + " | Diện tích: " + txtArea.getText()
                + " | Pháp lý: "   + txtLegalDoc.getText()).strip();
    }

    @Override
    public void reset() {
        txtAddress.clear(); txtArea.clear(); txtFloor.clear();
        txtLegalDoc.clear(); txtContact.clear();
        cmbType.setValue(null); cmbDirection.setValue(null); cmbLegalStatus.setValue(null);
    }
}
