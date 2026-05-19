package sample.form.panels;

import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import sample.AuctionItemDTO;
import sample.form.CategoryFormPanel;

public class GameFormPanel extends CategoryFormPanel {

    private TextField        txtTitle, txtPublisher, txtVersion, txtAccount, txtServer;
    private ComboBox<String> cmbPlatform, cmbType, cmbCondition;

    @Override
    protected void buildUI() {
        txtTitle     = field("VD: Liên Quân Mobile, FIFA 24, Elden Ring...");
        txtPublisher = field("VD: Garena, EA Sports, Valve...");
        txtVersion   = field("VD: v2.1.45, Season 12...");
        txtAccount   = field("VD: Level 100, rank Huyền thoại, 200+ tướng...");
        txtServer    = field("VD: Server 01, Asia, VN...");

        cmbPlatform = combo("Chọn nền tảng",
                "PC / Steam", "Mobile (Android/iOS)", "PlayStation",
                "Xbox", "Nintendo Switch", "Web Browser");
        cmbType = combo("Chọn loại",
                "Tài khoản game", "Item / Vật phẩm", "Kim cương / Nạp thẻ",
                "Đĩa game / Key bản quyền", "Mod / Plugin", "Khác");
        cmbCondition = combo("Tình trạng tài khoản",
                "Chưa qua sử dụng", "Đang hoạt động", "Cần xác minh", "Bị khóa tạm thời");

        root.getChildren().addAll(
                row(labeledField("Tên game *",    txtTitle),
                        labeledField("Nhà phát hành", txtPublisher)),
                row(labeledField("Nền tảng *",    cmbPlatform),
                        labeledField("Loại *",        cmbType)),
                labeledField("Thông tin tài khoản / Vật phẩm", txtAccount),
                row(labeledField("Server / Máy chủ",  txtServer),
                        labeledField("Phiên bản / Mùa",   txtVersion)),
                labeledField("Tình trạng", cmbCondition)
        );
    }

    @Override
    public String validate() {
        String e;
        if ((e = req(txtTitle, "Tên game"))           != null) return e;
        if ((e = reqCombo(cmbPlatform, "Nền tảng"))   != null) return e;
        if ((e = reqCombo(cmbType,     "Loại"))        != null) return e;
        return null;
    }

    @Override
    public void fillDTO(AuctionItemDTO dto) {
        dto.description = (dto.description
                + "\nGame: "      + txtTitle.getText()
                + " | Server: "   + txtServer.getText()
                + " | Chi tiết: " + txtAccount.getText()).strip();
    }

    @Override
    public void reset() {
        txtTitle.clear(); txtPublisher.clear(); txtVersion.clear();
        txtAccount.clear(); txtServer.clear();
        cmbPlatform.setValue(null); cmbType.setValue(null); cmbCondition.setValue(null);
    }
}
