package com.auction.server.repository;

import com.auction.server.model.ArtItem;
import com.auction.server.model.ElectronicsItem;
import com.auction.server.model.Item;
import com.auction.server.model.VehicleItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ItemDaoImpl implements IItemDAO {
    private static final int MAX_PAGE_SIZE = 200;

    private int sanitizeLimit(int limit) {
        if (limit <= 0) {
            return IItemDAO.DEFAULT_PAGE_SIZE;
        }
        return Math.min(limit, MAX_PAGE_SIZE);
    }

    private int sanitizeOffset(int offset) {
        return Math.max(offset, 0);
    }

    // Dựng object Item đúng loại từ ResultSet của bảng Items
    // Gọi sau khi đã JOIN với bảng con (Electronics_Items / Art_Items / Vehicle_Items)
    private Item buildItem(ResultSet rs) throws SQLException {
        int    id                = rs.getInt("id");
        String name              = rs.getString("name");
        String itemType          = rs.getString("item_type");
        double startingPrice     = rs.getDouble("startingPrice");
        double currentHighestBid = rs.getDouble("currentHighestBid");

        Item item;
        switch (itemType) {
            case "ELECTRONICS":
                int warrantyMonths = rs.getInt("warranty_months");
                item = new ElectronicsItem(String.valueOf(id), name, startingPrice, warrantyMonths);
                break;
            case "ART":
                String artistName = rs.getString("artist_name");
                item = new ArtItem(String.valueOf(id), name, startingPrice, artistName);
                break;
            case "VEHICLE":
                String brand = rs.getString("brand");
                int    year  = rs.getInt("year");
                item = new VehicleItem(String.valueOf(id), name, startingPrice, brand, year);
                break;
            default:
                System.err.println("[ItemDaoImpl] Loại sản phẩm không hợp lệ: " + itemType);
                return null;
        }

        item.setCurrentHighestBid(currentHighestBid);
        return item;
    }

    @Override
    public Item getItemById(int id) {
        // LEFT JOIN để lấy thông tin bảng con tương ứng trong 1 lần query
        String sql = "SELECT i.*, e.warranty_months, a.artist_name, v.brand, v.year " +
                "FROM Items i " +
                "LEFT JOIN Electronics_Items e ON i.id = e.item_id " +
                "LEFT JOIN Art_Items         a ON i.id = a.item_id " +
                "LEFT JOIN Vehicle_Items     v ON i.id = v.item_id " +
                "WHERE i.id = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return buildItem(rs);
                }
            }
        } catch (Exception e) {
            System.err.println("[ItemDaoImpl] Lỗi getItemById: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<Item> getAllItems(int limit, int offset) {
        String sql = "SELECT i.*, e.warranty_months, a.artist_name, v.brand, v.year " +
                "FROM Items i " +
                "LEFT JOIN Electronics_Items e ON i.id = e.item_id " +
                "LEFT JOIN Art_Items         a ON i.id = a.item_id " +
                "LEFT JOIN Vehicle_Items     v ON i.id = v.item_id " +
                "ORDER BY i.id DESC LIMIT ? OFFSET ?";

        List<Item> items = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, sanitizeLimit(limit));
            stmt.setInt(2, sanitizeOffset(offset));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Item item = buildItem(rs);
                    if (item != null) items.add(item);
                }
            }
        } catch (Exception e) {
            System.err.println("[ItemDaoImpl] Lỗi getAllItems: " + e.getMessage());
        }
        return items;
    }

    @Override
    public List<Item> getItemsBySellerId(String sellerId) {
        String sql = "SELECT i.*, e.warranty_months, a.artist_name, v.brand, v.year " +
                "FROM Items i " +
                "LEFT JOIN Electronics_Items e ON i.id = e.item_id " +
                "LEFT JOIN Art_Items         a ON i.id = a.item_id " +
                "LEFT JOIN Vehicle_Items     v ON i.id = v.item_id " +
                "WHERE i.seller_id = ?";

        List<Item> items = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, sellerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Item item = buildItem(rs);
                    if (item != null) items.add(item);
                }
            }
        } catch (Exception e) {
            System.err.println("[ItemDaoImpl] Lỗi getItemsBySellerId: " + e.getMessage());
        }
        return items;
    }

    @Override
    public int insertItem(Item item, String sellerId) {
        String sqlItem = "INSERT INTO Items (name, item_type, startingPrice, currentHighestBid, seller_id) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false); // Bắt đầu transaction

            try (PreparedStatement stmt = conn.prepareStatement(sqlItem, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, item.getName());
                String typeStr;
                switch (item.getType_item()) {
                    case "ArtItem":         typeStr = "ART";         break;
                    case "ElectronicsItem": typeStr = "ELECTRONICS";  break;
                    case "VehicleItem":     typeStr = "VEHICLE";      break;
                    default:                typeStr = item.getType_item().toUpperCase(); break;
                }
                stmt.setString(2, typeStr); // "Electronics Item" → "ELECTRONICS"
                stmt.setDouble(3, item.getStartingPrice());
                stmt.setDouble(4, item.getStartingPrice()); // currentHighestBid ban đầu = startingPrice
                stmt.setString(5, sellerId);
                stmt.executeUpdate();

                // Lấy id được DB sinh ra
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (!generatedKeys.next()) {
                        conn.rollback();
                        return -1;
                    }

                    int generatedId = generatedKeys.getInt(1);

                    // Insert vào bảng con tương ứng
                    insertSubTypeItem(conn, generatedId, item);

                    conn.commit();
                    System.out.println("[ItemDaoImpl] Thêm sản phẩm thành công, id = " + generatedId);
                    return generatedId;
                }

            } catch (SQLException e) {
                conn.rollback();
                System.err.println("[ItemDaoImpl] Lỗi insertItem, đã rollback: " + e.getMessage());
                return -1;
            }

        } catch (Exception e) {
            System.err.println("[ItemDaoImpl] Lỗi kết nối insertItem: " + e.getMessage());
            return -1;
        }
    }

    // Insert vào bảng con (Electronics_Items / Art_Items / Vehicle_Items)
    private void insertSubTypeItem(Connection conn, int itemId, Item item) throws SQLException {
        if (item instanceof ElectronicsItem) {
            String sql = "INSERT INTO Electronics_Items (item_id, warranty_months) VALUES (?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, itemId);
                stmt.setInt(2, ((ElectronicsItem) item).getWarrantyMonths());
                stmt.executeUpdate();
            }

        } else if (item instanceof ArtItem) {
            String sql = "INSERT INTO Art_Items (item_id, artist_name) VALUES (?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, itemId);
                stmt.setString(2, ((ArtItem) item).getArtist());
                stmt.executeUpdate();
            }

        } else if (item instanceof VehicleItem) {
            String sql = "INSERT INTO Vehicle_Items (item_id, brand, year) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, itemId);
                stmt.setString(2, ((VehicleItem) item).getBrand());
                stmt.setInt(3, ((VehicleItem) item).getYear());
                stmt.executeUpdate();
            }
        }
    }

    @Override
    public boolean deleteItem(int id) {
        // Xóa Items sẽ tự CASCADE xóa bảng con nhờ FOREIGN KEY trong schema
        String sql = "DELETE FROM Items WHERE id = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            int rows = stmt.executeUpdate();
            return rows > 0;

        } catch (Exception e) {
            System.err.println("[ItemDaoImpl] Lỗi deleteItem: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean updateCurrentHighestBid(int itemId, double newBid) {
        String sql = "UPDATE Items SET currentHighestBid = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, newBid);
            stmt.setInt(2, itemId);
            int rows = stmt.executeUpdate();
            return rows > 0;

        } catch (Exception e) {
            System.err.println("[ItemDaoImpl] Lỗi updateCurrentHighestBid: " + e.getMessage());
            return false;
        }
    }

    @Override
    public List<Item> getItemsByCategory(String category) {
        List<Item> list = new ArrayList<>();

        // Phải có LEFT JOIN như các method khác để buildItem hoạt động
        String sql = "SELECT i.*, e.warranty_months, a.artist_name, v.brand, v.year " +
                "FROM Items i " +
                "LEFT JOIN Electronics_Items e ON i.id = e.item_id " +
                "LEFT JOIN Art_Items         a ON i.id = a.item_id " +
                "LEFT JOIN Vehicle_Items     v ON i.id = v.item_id " +
                "WHERE i.item_type = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, category);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Item item = buildItem(rs); // ← Đổi mapItem thành buildItem
                    if (item != null) list.add(item);
                }
            }
        } catch (Exception e) {
            System.err.println("[ItemDaoImpl] Lỗi getItemsByCategory: " + e.getMessage());
        }
        return list;
    }
}