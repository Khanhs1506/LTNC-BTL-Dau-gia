# UML Diagrams — Dự án Đấu giá LTNC-BTL-Dau-gia

Bộ file **PlantUML** (`.puml`) được tạo từ mã nguồn trong `LTNC-BTL-Dau-giafinal.zip`, khớp với:

- `auction-server` — backend Socket + JDBC
- `auction-client` — JavaFX
- `auction-server/src/main/resources/schema.sql` — CSDL

## Danh sách file

| File | Loại UML | Nội dung |
|------|----------|----------|
| `01-class-domain-model.puml` | Class | Entity, User, Item, Auction, Factory |
| `02-class-server-architecture.puml` | Class | Network, Service, Repository, Observer |
| `03-class-client.puml` | Class | JavaFX controllers, ServerConnection, Form panels |
| `04-er-database.puml` | ER | 13 bảng MySQL + quan hệ |
| `05-sequence-login-register.puml` | Sequence | LOGIN, REGISTER |
| `06-sequence-place-bid.puml` | Sequence | PLACE_BID, anti-sniping, auto-bid, push |
| `07-sequence-seller-create-auction.puml` | Sequence | CREATE_ITEM + tạo auction |
| `08-component-deployment.puml` | Component / Deployment | Client ↔ Server ↔ MySQL |
| `09-use-case.puml` | Use Case | Admin, Seller, Bidder |
| `10-activity-auction-lifecycle.puml` | Activity | OPEN → RUNNING → FINISHED |
| `11-package-overview.puml` | Package | Maven modules |

## Cách xem / xuất ảnh

### 1. Trực tuyến (nhanh nhất)

1. Mở https://www.plantuml.com/plantuml/uml/
2. Copy toàn bộ nội dung một file `.puml` → Paste → Submit.

### 2. VS Code / Cursor

1. Cài extension **PlantUML** (jebbs.plantuml).
2. Mở file `.puml` → `Alt+D` preview hoặc export PNG/SVG.

### 3. IntelliJ IDEA

1. **Settings → Plugins → PlantUML integration**.
2. Chuột phải file `.puml` → **Show Diagram**.

### 4. Dòng lệnh (cần Java + Graphviz)

```bash
cd uml
java -jar plantuml.jar -tpng *.puml
```

## Giao thức mạng (tham chiếu sequence)

Client gửi: `ACTION===JSON` (một dòng).

| ACTION | Vai trò |
|--------|---------|
| LOGIN, REGISTER | Xác thực |
| GET_AUCTIONS, GET_ITEMS | Danh sách |
| PLACE_BID | Đặt giá |
| CREATE_ITEM, DELETE_ITEM | Seller |
| DEPOSIT, BID_HOLD, BID_RELEASE, PAYMENT | Ví |
| REGISTER_AUTO_BID, CANCEL_AUTO_BID | Auto-bid |
| ADD_FAVORITE, GET_FAVORITES | Yêu thích |
| SUBMIT_REPORT, RESOLVE_REPORT | Báo cáo |
| BAN_USER, GET_ADMIN_STATS | Admin |

Server **push** (không chờ request): `BID_UPDATE===...`, `TIME_EXTENDED===...`

## Design patterns trong dự án

| Pattern | Vị trí |
|---------|--------|
| Singleton | `AuctionManager`, `BiddingEngine`, `AutoBiddingService`, `ServerConnection` |
| Factory Method | `ItemFactory` / `ItemFactoryImpl` |
| Observer | `AuctionObserver` ← `ClientHandler`, `BiddingEngine` |
| DAO | `I*DAO` + `*DaoImpl` |
| DTO | `AuctionItemDTO`, `AuctionSummary` |

## Ghi chú

- File HTML có sẵn ở thư mục gốc: `uml-class-diagram.html` (xem trên trình duyệt).
- Bộ `.puml` này **chi tiết hơn** (ER, sequence, use case, deployment) phục vụ báo cáo BTL.
