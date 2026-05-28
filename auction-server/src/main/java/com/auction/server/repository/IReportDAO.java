package com.auction.server.repository;

import java.util.List;

public interface IReportDAO {

    //Lưu báo cáo mới vào DB, trả về id tạo mới hoặc -1 nếu lỗi
    int insertReport(String reporterUsername, String targetUsername, String reason);

    //Lấy tất cả báo cáo (mới nhất trước)
    List<ReportInfo> getAllReports();

    //Đánh dấu báo cáo là RESOLVED.
    boolean resolveReport(int reportId);

    //Inner DTO
    class ReportInfo {
        public int id;
        public String reporterUsername;
        public String targetUsername;
        public String reason;
        public String createdAt;   //form yyyy-MM-dd HH:mm:ss
        public String status;      // "PENDING" | "RESOLVED"
    }
}
