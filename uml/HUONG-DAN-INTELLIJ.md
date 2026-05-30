# Sửa lỗi đỏ & xem diagram trong IntelliJ IDEA

## Bước 1 — Cài plugin (bắt buộc)

Banner xanh trong editor: **Install plantuml4idea plugin** → bấm vào.

Hoặc thủ công:

1. **File → Settings** (Ctrl+Alt+S)
2. **Plugins → Marketplace**
3. Tìm: **PlantUML integration** (tác giả: Eugene Ingosev / plantuml4idea)
4. **Install** → **Restart IDE**

## Bước 2 — Xem sơ đồ

1. Mở file `.puml` (ví dụ `01-class-domain-model.puml`)
2. Một trong các cách:
   - **Phím tắt:** `Alt` + `U` (hoặc `Ctrl` + `Alt` + `U` tùy bản IDEA)
   - **Chuột phải** file → **Show PlantUML Diagram**
   - **View → Tool Windows → PlantUML** (nếu có)
3. Cửa sổ bên phải/dưới hiện hình class diagram.

## Bước 3 — Nếu vẫn báo lỗi / không vẽ được

### Cài Graphviz (Windows)

1. Tải: https://graphviz.org/download/ (file `.exe`)
2. Cài xong, thêm vào PATH (installer thường tích sẵn).
3. IDEA: **Settings → Languages & Frameworks → PlantUML**
4. **Graphviz dot executable:** trỏ tới  
   `C:\Program Files\Graphviz\bin\dot.exe`

### Reload file sau khi sửa

- Đóng tab `.puml` → mở lại, hoặc **File → Invalidate Caches** (chỉ khi cần).

## Vì sao trước đó “đỏ lòm”?

| Nguyên nhân | Cách xử lý |
|-------------|------------|
| Chưa cài plugin | Cài **PlantUML integration** |
| `List<String>`, `Map<Integer, Auction>` trong class | PlantUML hiểu `<` `>` là mũi tên → dùng `List~String~`, `Map~Integer, Auction~` |
| `Entity<T>` | Đổi thành `interface "Entity<T>" as Entity` |
| `...` trong tên method | Đổi thành tên đầy đủ, ví dụ `createItem(...)` → `createItem(...)` đã sửa |

File `01`, `02` đã chỉnh cú pháp an toàn cho IntelliJ.

## Chạy thử không cần IDEA (online)

1. Mở https://www.plantuml.com/plantuml/uml/
2. Copy toàn bộ nội dung `01-class-domain-model.puml`
3. Paste → Submit → thấy diagram = file đúng.

## Phím tắt hữu ích (khi đã có diagram)

| Thao tác | Phím |
|----------|------|
| Zoom | Ctrl + chuột giữa |
| Export PNG | Chuột phải diagram → Export |
