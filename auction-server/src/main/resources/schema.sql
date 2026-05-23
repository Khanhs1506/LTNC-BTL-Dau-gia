DROP DATABASE IF EXISTS auction_system;
CREATE DATABASE auction_system
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE auction_system;

-- ============================================================
-- BẢNG 1: users
-- ============================================================
CREATE TABLE users (
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
-- ============================================================
CREATE TABLE Items (
                       id                INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
                       name              VARCHAR(255) NOT NULL,
                       item_type         ENUM('ELECTRONICS', 'ART', 'VEHICLE') NOT NULL,
                       startingPrice     DOUBLE       NOT NULL,
                       currentHighestBid DOUBLE       NOT NULL,
                       status            ENUM('AVAILABLE', 'IN_AUCTION', 'SOLD', 'CANCELED') NOT NULL DEFAULT 'AVAILABLE',
                       seller_id         VARCHAR(36)  NOT NULL,
                       created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       CONSTRAINT fk_item_seller FOREIGN KEY (seller_id) REFERENCES users(id)
                           ON DELETE CASCADE
);

-- ============================================================
-- BẢNG 3: Electronics_Items
-- ============================================================
CREATE TABLE Electronics_Items (
                                   item_id         INT NOT NULL PRIMARY KEY,
                                   warranty_months INT NOT NULL DEFAULT 0,
                                   CONSTRAINT fk_electronics_item FOREIGN KEY (item_id) REFERENCES Items(id)
                                       ON DELETE CASCADE
);

-- ============================================================
-- BẢNG 4: Art_Items
-- ============================================================
CREATE TABLE Art_Items (
                           item_id     INT          NOT NULL PRIMARY KEY,
                           artist_name VARCHAR(255) NOT NULL,
                           CONSTRAINT fk_art_item FOREIGN KEY (item_id) REFERENCES Items(id)
                               ON DELETE CASCADE
);

-- ============================================================
-- BẢNG 5: Vehicle_Items
-- ============================================================
CREATE TABLE Vehicle_Items (
                               item_id INT          NOT NULL PRIMARY KEY,
                               brand   VARCHAR(100) NOT NULL,
                               year    INT          NOT NULL,
                               CONSTRAINT fk_vehicle_item FOREIGN KEY (item_id) REFERENCES Items(id)
                                   ON DELETE CASCADE
);

-- ============================================================
-- BẢNG 6: auctions
-- ============================================================
CREATE TABLE auctions (
                          id                      INT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
                          item_id                 INT         NOT NULL UNIQUE,
                          current_highest_bid     DOUBLE      NOT NULL,
                          current_winner_username VARCHAR(50) DEFAULT NULL,
                          start_time              DATETIME    NOT NULL,
                          end_time                DATETIME    NOT NULL,
                          status                  ENUM('OPEN', 'RUNNING', 'FINISHED', 'CANCELED') NOT NULL DEFAULT 'OPEN',
                          created_at              DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          CONSTRAINT fk_auction_item   FOREIGN KEY (item_id)
                              REFERENCES Items(id) ON DELETE CASCADE,
                          CONSTRAINT fk_auction_winner FOREIGN KEY (current_winner_username)
                              REFERENCES users(username) ON DELETE SET NULL
);

-- ============================================================
-- BẢNG 7: bid_transactions
-- ============================================================
CREATE TABLE bid_transactions (
                                  transaction_id  VARCHAR(36) NOT NULL PRIMARY KEY,
                                  auction_id      INT         NOT NULL,
                                  bidder_username VARCHAR(50) NOT NULL,
                                  bid_amount      DOUBLE      NOT NULL,
                                  timestamp       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  CONSTRAINT fk_bid_auction FOREIGN KEY (auction_id)
                                      REFERENCES auctions(id) ON DELETE CASCADE,
                                  CONSTRAINT fk_bid_bidder  FOREIGN KEY (bidder_username)
                                      REFERENCES users(username) ON DELETE CASCADE
);

-- ============================================================
-- BẢNG 8: auto_bids  (MỚI — lưu đăng ký đấu giá tự động)
-- ============================================================
CREATE TABLE auto_bids (
                           id                INT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
                           auction_id        INT         NOT NULL,
                           username          VARCHAR(50) NOT NULL,
                           max_bid           DOUBLE      NOT NULL,
                           increment_amount  DOUBLE      NOT NULL,
                           registered_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           UNIQUE KEY uq_auto_bid (auction_id, username),
                           CONSTRAINT fk_auto_bid_auction FOREIGN KEY (auction_id)
                               REFERENCES auctions(id) ON DELETE CASCADE,
                           CONSTRAINT fk_auto_bid_user    FOREIGN KEY (username)
                               REFERENCES users(username) ON DELETE CASCADE
);

-- ============================================================
-- BẢNG 9: wallet_transactions
-- ============================================================
CREATE TABLE wallet_transactions (
                                     id                 VARCHAR(36)  NOT NULL PRIMARY KEY,
                                     user_id            VARCHAR(36)  NOT NULL,
                                     type               ENUM(
                           'DEPOSIT',
                           'PAYMENT',
                           'REFUND',
                           'BID_HOLD',
                           'BID_RELEASE'
                       ) NOT NULL,
                                     amount             DOUBLE       NOT NULL,
                                     balance_before     DOUBLE       NOT NULL,
                                     balance_after      DOUBLE       NOT NULL,
                                     related_auction_id INT          DEFAULT NULL,
                                     note               VARCHAR(500) DEFAULT NULL,
                                     created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                     CONSTRAINT fk_wallet_user    FOREIGN KEY (user_id)
                                         REFERENCES users(id) ON DELETE CASCADE,
                                     CONSTRAINT fk_wallet_auction FOREIGN KEY (related_auction_id)
                                         REFERENCES auctions(id) ON DELETE SET NULL
);

CREATE INDEX idx_wallet_user_id ON wallet_transactions(user_id, created_at DESC);
CREATE INDEX idx_wallet_type    ON wallet_transactions(type);
CREATE INDEX idx_wallet_auction ON wallet_transactions(related_auction_id);

-- ============================================================
-- INDEX
-- ============================================================
CREATE INDEX idx_users_username    ON users(username);
CREATE INDEX idx_items_seller      ON Items(seller_id);
CREATE INDEX idx_items_type        ON Items(item_type);
CREATE INDEX idx_auctions_status   ON auctions(status);
CREATE INDEX idx_bids_auction      ON bid_transactions(auction_id, timestamp);
CREATE INDEX idx_auto_bids_auction ON auto_bids(auction_id);

-- ============================================================
-- DỮ LIỆU MẶC ĐỊNH: Admin account
-- ============================================================
INSERT INTO users (id, username, password, role)
VALUES (UUID(), 'admin', 'admin123', 'ADMIN');