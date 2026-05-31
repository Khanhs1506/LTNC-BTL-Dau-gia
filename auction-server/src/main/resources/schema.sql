
DROP DATABASE IF EXISTS auction_system;
CREATE DATABASE auction_system
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
 
USE auction_system;
 
-- ============================================================
-- BẢNG 1: users
-- ============================================================
CREATE TABLE users
(
    id         VARCHAR(36)  NOT NULL PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    role       ENUM('ADMIN', 'SELLER', 'BIDDER') NOT NULL,
    balance    DOUBLE       NOT NULL DEFAULT 0.0,
    rating     DOUBLE       NOT NULL DEFAULT 5.0,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- BẢNG 2: Items
-- ★ Thêm cột image_url (MEDIUMTEXT - chứa ảnh base64 tối đa ~16MB)
-- ★ Thêm cột information (TEXT - mô tả chi tiết thêm)
-- ============================================================
CREATE TABLE Items
(
    id                INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name              VARCHAR(255) NOT NULL,
    item_type         ENUM('ELECTRONICS', 'ART', 'VEHICLE') NOT NULL,
    startingPrice     DOUBLE       NOT NULL,
    currentHighestBid DOUBLE       NOT NULL,
    status            ENUM('AVAILABLE', 'IN_AUCTION', 'SOLD', 'CANCELED') NOT NULL DEFAULT 'AVAILABLE',
    seller_id         VARCHAR(36)  NOT NULL,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    information       TEXT         NULL,
    image_url         MEDIUMTEXT   NULL,
    CONSTRAINT fk_item_seller FOREIGN KEY (seller_id) REFERENCES users (id) ON DELETE CASCADE
);

-- ============================================================
-- BẢNG 3: Electronics_Items
-- ============================================================
CREATE TABLE Electronics_Items
(
    item_id         INT NOT NULL PRIMARY KEY,
    warranty_months INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_electronics_item FOREIGN KEY (item_id) REFERENCES Items (id) ON DELETE CASCADE
);

-- ============================================================
-- BẢNG 4: Art_Items
-- ============================================================
CREATE TABLE Art_Items
(
    item_id     INT          NOT NULL PRIMARY KEY,
    artist_name VARCHAR(255) NOT NULL,
    CONSTRAINT fk_art_item FOREIGN KEY (item_id) REFERENCES Items (id) ON DELETE CASCADE
);

-- ============================================================
-- BẢNG 5: Vehicle_Items
-- ============================================================
CREATE TABLE Vehicle_Items
(
    item_id INT          NOT NULL PRIMARY KEY,
    brand   VARCHAR(100) NOT NULL,
    year    INT          NOT NULL,
    CONSTRAINT fk_vehicle_item FOREIGN KEY (item_id) REFERENCES Items (id) ON DELETE CASCADE
);

-- ============================================================
-- BẢNG 6: auctions
-- ★ Thêm cột bid_step (bước giá tối thiểu mỗi lần đấu)
-- ============================================================
CREATE TABLE auctions
(
    id                      INT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    item_id                 INT      NOT NULL UNIQUE,
    current_highest_bid     DOUBLE   NOT NULL,
    current_winner_username VARCHAR(50)       DEFAULT NULL,
    start_time              DATETIME NOT NULL,
    end_time                DATETIME NOT NULL,
    status                  ENUM('OPEN', 'RUNNING', 'FINISHED', 'CANCELED') NOT NULL DEFAULT 'OPEN',
    bid_step                DOUBLE   NOT NULL DEFAULT 0,
    created_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_auction_item   FOREIGN KEY (item_id) REFERENCES Items (id) ON DELETE CASCADE,
    CONSTRAINT fk_auction_winner FOREIGN KEY (current_winner_username) REFERENCES users (username) ON DELETE SET NULL
);

-- ============================================================
-- BẢNG 7: bid_transactions
-- ============================================================
CREATE TABLE bid_transactions
(
    transaction_id  VARCHAR(36) NOT NULL PRIMARY KEY,
    auction_id      INT         NOT NULL,
    bidder_username VARCHAR(50) NOT NULL,
    bid_amount      DOUBLE      NOT NULL,
    timestamp       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bid_auction FOREIGN KEY (auction_id)      REFERENCES auctions (id)      ON DELETE CASCADE,
    CONSTRAINT fk_bid_bidder  FOREIGN KEY (bidder_username) REFERENCES users (username)   ON DELETE CASCADE
);

-- ============================================================
-- BẢNG 8: favorites (yêu thích)
-- ============================================================
CREATE TABLE favorites
(
    id         INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id    VARCHAR(36) NOT NULL,
    auction_id INT         NOT NULL,
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_fav (user_id, auction_id),
    CONSTRAINT fk_fav_user    FOREIGN KEY (user_id)    REFERENCES users    (id) ON DELETE CASCADE,
    CONSTRAINT fk_fav_auction FOREIGN KEY (auction_id) REFERENCES auctions (id) ON DELETE CASCADE
);

-- ============================================================
-- BẢNG 9: auto_bids (đấu giá tự động)
-- ============================================================
CREATE TABLE auto_bids
(
    id              INT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    auction_id      INT         NOT NULL,
    bidder_username VARCHAR(50) NOT NULL,
    max_bid         DOUBLE      NOT NULL,
    increment       DOUBLE      NOT NULL,
    trigger_minutes INT         NOT NULL DEFAULT 10,
    active          TINYINT(1)  NOT NULL DEFAULT 1,
    created_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_autobid_auction FOREIGN KEY (auction_id) REFERENCES auctions (id) ON DELETE CASCADE,
    CONSTRAINT fk_autobid_user    FOREIGN KEY (bidder_username) REFERENCES users (username) ON DELETE CASCADE
);

-- ============================================================
-- BẢNG 10: wallet_transactions (lịch sử giao dịch ví)
-- ============================================================
CREATE TABLE wallet_transactions
(
    id          INT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id     VARCHAR(36) NOT NULL,
    type        ENUM('DEPOSIT','WITHDRAW','BID_HOLD','BID_RELEASE','BID_WIN') NOT NULL,
    amount      DOUBLE      NOT NULL,
    description VARCHAR(500),
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wallet_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- ============================================================
-- BẢNG 11: reports (báo cáo vi phạm)
-- ============================================================
CREATE TABLE reports
(
    id           INT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    reporter_id  VARCHAR(36) NOT NULL,
    auction_id   INT         NOT NULL,
    reason       TEXT        NOT NULL,
    status       ENUM('PENDING','RESOLVED','DISMISSED') NOT NULL DEFAULT 'PENDING',
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_report_user    FOREIGN KEY (reporter_id) REFERENCES users    (id) ON DELETE CASCADE,
    CONSTRAINT fk_report_auction FOREIGN KEY (auction_id)  REFERENCES auctions (id) ON DELETE CASCADE
);

-- ============================================================
-- BẢNG 12: seller_payments (thanh toán của seller)
-- ============================================================
CREATE TABLE seller_payments
(
    id         INT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    auction_id INT         NOT NULL UNIQUE,
    seller_id  VARCHAR(36) NOT NULL,
    amount     DOUBLE      NOT NULL,
    paid_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pay_auction FOREIGN KEY (auction_id) REFERENCES auctions (id) ON DELETE CASCADE,
    CONSTRAINT fk_pay_seller  FOREIGN KEY (seller_id)  REFERENCES users    (id) ON DELETE CASCADE
);

-- ============================================================
-- BẢNG 13: system_settings (cấu hình hệ thống)
-- ============================================================
CREATE TABLE system_settings
(
    setting_key   VARCHAR(100) NOT NULL PRIMARY KEY,
    setting_value TEXT         NOT NULL,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ============================================================
-- INDEX
-- ============================================================
CREATE INDEX idx_users_username   ON users    (username);
CREATE INDEX idx_items_seller     ON Items    (seller_id);
CREATE INDEX idx_items_type       ON Items    (item_type);
CREATE INDEX idx_auctions_status  ON auctions (status);
CREATE INDEX idx_bids_auction     ON bid_transactions (auction_id, timestamp);
 

 x