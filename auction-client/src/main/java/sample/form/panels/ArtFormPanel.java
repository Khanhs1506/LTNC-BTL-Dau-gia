package sample.form.panels;

import javafx.scene.control.TextField;
import sample.AuctionItemDTO;
import sample.form.CategoryFormPanel;

public class ArtFormPanel extends CategoryFormPanel {

    private TextField txtArtist, txtStyle, txtSize, txtCertificate, txtYear;

    @Override
    protected void buildUI() {
        txtArtist      = field("VD: Picasso, Nguyễn Tư Nghiêm...");
        txtStyle       = field("VD: Sơn dầu, Acrylic, Điêu khắc...");
        txtSize        = field("VD: 60×80 cm");
        txtCertificate = field("VD: Số chứng nhận, triển lãm...");
        txtYear        = field("VD: 2022");

        root.getChildren().addAll(
                row(labeledField("Nghệ sĩ / Tác giả *",     txtArtist),
                        labeledField("Chất liệu / Phong cách *", txtStyle)),
                row(labeledField("Kích thước tác phẩm",      txtSize),
                        labeledField("Năm sáng tác",              txtYear)),
                labeledField("Chứng nhận xuất xứ", txtCertificate)
        );
    }

    @Override
    public String validate() {
        String e;
        if ((e = req(txtArtist, "Nghệ sĩ / Tác giả"))          != null) return e;
        if ((e = req(txtStyle,  "Chất liệu / Phong cách"))      != null) return e;
        return null;
    }

    @Override
    public void fillDTO(AuctionItemDTO dto) {
        dto.artist      = txtArtist.getText().trim();
        dto.description = (dto.description + "\nPhong cách: " + txtStyle.getText()
                + " | Kích thước: " + txtSize.getText()
                + " | Năm: " + txtYear.getText()).strip();
        dto.certificate = txtCertificate.getText().trim();
    }

    @Override
    public void reset() {
        txtArtist.clear(); txtStyle.clear();
        txtSize.clear();   txtCertificate.clear(); txtYear.clear();
    }
}