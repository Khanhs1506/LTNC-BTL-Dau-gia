package sample;

public class AuctionItemDTO {
    public int    id;
    public String title;
    public double giaKhoiDiem;
    public double giaCaoNhat;
    public String endTime;   // "23/12/2026 23:59:59"
    public String status;    // "RUNNING" | "ENDED"

    public AuctionItemDTO(int id, String title, double giaKhoiDiem,
                          double giaCaoNhat, String endTime, String status) {
        this.id          = id;
        this.title       = title;
        this.giaKhoiDiem = giaKhoiDiem;
        this.giaCaoNhat  = giaCaoNhat;
        this.endTime     = endTime;
        this.status      = status;
    }
}