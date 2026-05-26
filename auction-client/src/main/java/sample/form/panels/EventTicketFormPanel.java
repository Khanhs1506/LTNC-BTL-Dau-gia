package sample.form.panels;

import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import sample.AuctionItemDTO;
import sample.form.CategoryFormPanel;

public class EventTicketFormPanel extends CategoryFormPanel {

    private TextField    txtEventName, txtOrganizer, txtVenue, txtSeat, txtCode;
    private DatePicker   dpEventDate;
    private ComboBox<String> cmbTicketType;

    @Override
    protected void buildUI() {
        txtEventName  = field("VD: Concert Sơn Tùng, EURO 2028...");
        txtOrganizer  = field("VD: Live Nation, VTV...");
        txtVenue      = field("VD: SVĐ Mỹ Đình, Hà Nội...");
        txtSeat       = field("VD: Khu A, Hàng 5, Ghế 22");
        txtCode       = field("VD: EVT-2024-XXXXX");
        dpEventDate   = new DatePicker();
        dpEventDate.setPromptText("dd/MM/yyyy");
        dpEventDate.setMaxWidth(Double.MAX_VALUE);
        cmbTicketType = combo("Chọn hạng vé",
                "VIP","Hạng nhất","Hạng thường","Gia đình","Online stream");

        root.getChildren().addAll(
                labeledField("Tên sự kiện *",          txtEventName),
                row(labeledField("Đơn vị tổ chức",     txtOrganizer),
                        labeledField("Địa điểm *",          txtVenue)),
                row(labeledField("Ngày diễn ra *",      dpEventDate),
                        labeledField("Hạng vé *",           cmbTicketType)),
                row(labeledField("Thông tin ghế / Khu vực", txtSeat),
                        labeledField("Mã vé / QR",          txtCode))
        );
    }

    @Override
    public String validate() {
        String e;
        if ((e = req(txtEventName, "Tên sự kiện"))        != null) return e;
        if ((e = req(txtVenue,     "Địa điểm"))           != null) return e;
        if (dpEventDate.getValue() == null)
            return "⚠ Vui lòng chọn: Ngày diễn ra";
        if ((e = reqCombo(cmbTicketType, "Hạng vé"))      != null) return e;
        return null;
    }

    @Override
    public void fillDTO(AuctionItemDTO dto) {
        dto.description += "\nSự kiện: " + txtEventName.getText()
                + " | Địa điểm: " + txtVenue.getText()
                + " | Ghế: " + txtSeat.getText();
    }

    @Override
    public void reset() {
        txtEventName.clear(); txtOrganizer.clear();
        txtVenue.clear();     txtSeat.clear(); txtCode.clear();
        dpEventDate.setValue(null);
        cmbTicketType.setValue(null);
    }
}