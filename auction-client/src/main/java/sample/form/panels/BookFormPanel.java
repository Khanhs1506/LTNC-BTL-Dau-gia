package sample.form.panels;

import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import sample.AuctionItemDTO;
import sample.form.CategoryFormPanel;

public class BookFormPanel extends CategoryFormPanel {

    private TextField        txtTitle, txtAuthor, txtPublisher, txtIsbn, txtEdition;
    private ComboBox<String> cmbGenre, cmbCondition, cmbLanguage;

    @Override
    protected void buildUI() {
        txtTitle     = field("VD: Đắc Nhân Tâm, Harry Potter, Sapiens...");
        txtAuthor    = field("VD: Dale Carnegie, J.K. Rowling...");
        txtPublisher = field("VD: NXB Trẻ, NXB Tổng hợp, Fahasa...");
        txtIsbn      = field("VD: 978-604-2-18301-5");
        txtEdition   = field("VD: Tái bản lần 3, Bìa cứng 2023...");

        cmbGenre = combo("Chọn thể loại",
                "Kỹ năng sống / Self-help", "Văn học trong nước",
                "Văn học nước ngoài", "Kinh tế / Kinh doanh",
                "Lịch sử / Địa lý", "Khoa học / Kỹ thuật",
                "Tâm lý học", "Thiếu nhi / Truyện tranh",
                "Giáo khoa / Tham khảo", "Tôn giáo / Triết học", "Khác");
        cmbCondition = combo("Chọn tình trạng",
                "Mới nguyên seal", "Như mới (chưa đọc)",
                "Còn tốt (ít ghi chú)", "Đã đọc (có ghi chú)",
                "Cũ / Ố vàng");
        cmbLanguage = combo("Ngôn ngữ",
                "Tiếng Việt", "Tiếng Anh", "Tiếng Trung",
                "Tiếng Nhật", "Tiếng Pháp", "Khác");

        root.getChildren().addAll(
                labeledField("Tên sách *", txtTitle),
                row(labeledField("Tác giả *",          txtAuthor),
                        labeledField("Thể loại *",          cmbGenre)),
                row(labeledField("Nhà xuất bản",        txtPublisher),
                        labeledField("Ngôn ngữ",            cmbLanguage)),
                row(labeledField("Tình trạng *",        cmbCondition),
                        labeledField("Phiên bản / Tái bản", txtEdition)),
                labeledField("ISBN / Mã sách", txtIsbn)
        );
    }

    @Override
    public String validate() {
        String e;
        if ((e = req(txtTitle,  "Tên sách"))       != null) return e;
        if ((e = req(txtAuthor, "Tác giả"))        != null) return e;
        if ((e = reqCombo(cmbGenre,     "Thể loại"))  != null) return e;
        if ((e = reqCombo(cmbCondition, "Tình trạng"))!= null) return e;
        return null;
    }

    @Override
    public void fillDTO(AuctionItemDTO dto) {
        dto.author = txtAuthor.getText().trim();
        dto.description = (dto.description
                + "\nNXB: "        + txtPublisher.getText()
                + " | Phiên bản: " + txtEdition.getText()
                + " | ISBN: "      + txtIsbn.getText()).strip();
    }

    @Override
    public void reset() {
        txtTitle.clear(); txtAuthor.clear(); txtPublisher.clear();
        txtIsbn.clear();  txtEdition.clear();
        cmbGenre.setValue(null); cmbCondition.setValue(null); cmbLanguage.setValue(null);
    }
}
