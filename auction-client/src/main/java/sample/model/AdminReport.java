package sample.model;

public class AdminReport {

    private String id;               // ID báo cáo
    private String reporterUsername; // Người gửi báo cáo
    private String targetUsername;   // Người/phiên bị báo cáo
    private String reason;           // Lý do
    private String createdAt;        // Ngày tạo (dạng String để dễ hiển thị)
    private String status;           // "PENDING" | "RESOLVED"

    public AdminReport() {}

    public AdminReport(String id, String reporterUsername, String targetUsername,
                       String reason, String createdAt, String status) {
        this.id = id;
        this.reporterUsername = reporterUsername;
        this.targetUsername = targetUsername;
        this.reason = reason;
        this.createdAt = createdAt;
        this.status = status;
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public String getReporterUsername() {
        return reporterUsername;
    }
    public void setReporterUsername(String v) {
        this.reporterUsername = v;
    }

    public String getTargetUsername() {
        return targetUsername;
    }
    public void setTargetUsername(String v) {
        this.targetUsername = v;
    }

    public String getReason() {
        return reason;
    }
    public void setReason(String v) {
        this.reason = v;
    }

    public String getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(String v) {
        this.createdAt = v;
    }

    public String getStatus() {
        return status;
    }
    public void setStatus(String v) {
        this.status = v;
    }
}