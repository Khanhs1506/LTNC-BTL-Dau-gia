package com.auction.server.util;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Base64;
import java.util.UUID;

public class ImageStorageUtil {
    private static final String UPLOAD_DIR = "uploads/items/";


    public static String saveBase64Image(final String base64Str) {
        if (base64Str == null || base64Str.isBlank()) {
            return "";
        }

        try {
            String cleanBase64 = base64Str;
            // Xử lý nếu chuỗi Base64 có kèm metadata prefix dạng "data:image/jpeg;base64,..."
            if (cleanBase64.contains(",")) {
                cleanBase64 = cleanBase64.split(",")[1];
            }

            // Giải mã chuỗi Base64 về mảng bytes nguyên bản của file ảnh
            final byte[] imageBytes = Base64.getDecoder().decode(cleanBase64.trim());

            // Kiểm tra và tự động tạo thư mục lưu trữ nếu chưa có
            final File dir = new File(UPLOAD_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // Sinh tên file ngẫu nhiên bằng UUID để tránh trùng lặp tên file
            final String fileName = UUID.randomUUID().toString() + ".jpg";
            final File targetFile = new File(dir, fileName);

            // Ghi mảng byte ra file vật lý
            try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                fos.write(imageBytes);
            }

            System.out.println("[Server] Đã lưu file ảnh vật lý thành công tại: " + targetFile.getAbsolutePath());

            // Trả về đường dẫn tương đối (Ví dụ: uploads/items/abc-123.jpg)
            return UPLOAD_DIR + fileName;

        } catch (Exception e) {
            System.err.println("[Server Error] Lỗi giải mã và ghi file ảnh: " + e.getMessage());
            return "";
        }
    }
}