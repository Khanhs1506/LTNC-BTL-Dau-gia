package sample;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import sample.model.Transaction;
import sample.model.Wallet;

import java.time.format.DateTimeFormatter;
import java.util.List;

import javafx.application.Platform;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import sample.model.Transaction;
import sample.model.Wallet;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class WalletController {

    // ── FXML ───────────────────────────────────────────────────────────
    @FXML private Label  lblBalance;
    @FXML private Label  lblTotalDeposited;
    @FXML private Label  lblTotalHeld;
    @FXML private Label  lblTotalRefunded;
    @FXML private Label  lblTotalPaid;

    // Deposit form
    @FXML private TextField  txtDepositAmount;
    @FXML private ComboBox<String> cmbPaymentMethod;
    @FXML private Button     btnDeposit;
    @FXML private Label      lblDepositStatus;
    @FXML private ProgressIndicator depositSpinner;

    // Transaction history
    @FXML private TableView<Transaction>          tblTransactions;
    @FXML private TableColumn<Transaction, String> colDate;
    @FXML private TableColumn<Transaction, String> colType;
    @FXML private TableColumn<Transaction, String> colAmount;
    @FXML private TableColumn<Transaction, String> colStatus;
    @FXML private TableColumn<Transaction, String> colNote;
    @FXML private TableColumn<Transaction, String> colBalance;

    // Filters
    @FXML private ComboBox<String>  cmbFilterType;
    @FXML private ComboBox<String>  cmbFilterStatus;
    @FXML private DatePicker        dpFrom;
    @FXML private DatePicker        dpTo;
    @FXML private Button            btnFilter;
    @FXML private Button            btnResetFilter;

    // Pagination
    @FXML private Label lblPage;
    @FXML private Button btnPrevPage;
    @FXML private Button btnNextPage;

    // ── State ──────────────────────────────────────────────────────────
    private Wallet wallet;
    private ObservableList<Transaction> txList = FXCollections.observableArrayList();
    private int currentPage = 1;
    private static final int PAGE_SIZE = 10;

    // ── Initialize ─────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        setupComboBoxes();
        setupTable();
        loadData();
    }

    private void setupComboBoxes() {
        cmbPaymentMethod.getItems().addAll(
                "💳 VNPay", "📱 MoMo", "🏦 Internet Banking",
                "💸 ZaloPay", "Thẻ ATM"
        );
        cmbPaymentMethod.getSelectionModel().selectFirst();

        cmbFilterType.getItems().addAll(
                "Tất cả", "Nạp tiền", "Đặt cọc đấu giá",
                "Hoàn tiền", "Thanh toán", "Điều chỉnh số dư"
        );
        cmbFilterType.getSelectionModel().selectFirst();

        cmbFilterStatus.getItems().addAll(
                "Tất cả", "Thành công", "Đang xử lý", "Thất bại", "Đã hủy"
        );
        cmbFilterStatus.getSelectionModel().selectFirst();
    }

    private void setupTable() {
        colDate  .setCellValueFactory(c -> new javafx.beans.property
                .SimpleStringProperty(c.getValue().getFormattedDate()));
        colNote  .setCellValueFactory(c -> new javafx.beans.property
                .SimpleStringProperty(c.getValue().getNote()));
        colAmount.setCellValueFactory(c -> new javafx.beans.property
                .SimpleStringProperty(c.getValue().getFormattedAmount()));
        colBalance.setCellValueFactory(c -> new javafx.beans.property
                .SimpleStringProperty(String.format("%,.0f VNĐ", c.getValue().getBalanceAfter())));

        // Type column — hiện badge màu
        colType.setCellValueFactory(c -> new javafx.beans.property
                .SimpleStringProperty(c.getValue().getType().label));
        colType.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Transaction tx = getTableView().getItems().get(getIndex());
                Label badge = new Label(item);
                badge.setStyle(
                        "-fx-background-color: " + tx.getType().color + "22;" +
                                "-fx-text-fill: "        + tx.getType().color + ";" +
                                "-fx-background-radius: 6; -fx-padding: 3 8;" +
                                "-fx-font-weight: bold; -fx-font-size: 11;"
                );
                setGraphic(badge);
                setText(null);
            }
        });

        // Status column — badge màu
        colStatus.setCellValueFactory(c -> new javafx.beans.property
                .SimpleStringProperty(c.getValue().getStatus().label));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Transaction tx = getTableView().getItems().get(getIndex());
                Label badge = new Label(item);
                badge.setStyle(
                        "-fx-background-color: " + tx.getStatus().color + "22;" +
                                "-fx-text-fill: "        + tx.getStatus().color + ";" +
                                "-fx-background-radius: 6; -fx-padding: 3 8;" +
                                "-fx-font-weight: bold; -fx-font-size: 11;"
                );
                setGraphic(badge);
                setText(null);
            }
        });

        // Amount: đỏ nếu trừ, xanh nếu cộng
        colAmount.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                Transaction tx = getTableView().getItems().get(getIndex());
                setText(item);
                setStyle("-fx-font-weight: bold; -fx-text-fill: " +
                        (tx.isCredit() ? "#27ae60" : "#e74c3c") + ";");
            }
        });

        tblTransactions.setItems(txList);
        tblTransactions.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Striped rows
        tblTransactions.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(Transaction item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setStyle(""); return; }
                setStyle(getIndex() % 2 == 0
                        ? "-fx-background-color: #ffffff;"
                        : "-fx-background-color: #fafafa;");
            }
        });
    }

    // ── Load data ──────────────────────────────────────────────────────
    private void loadData() {
        new Thread(() -> {
            Wallet w = WalletService.getInstance().fetchWallet();
            List<Transaction> txs = WalletService.getInstance()
                    .fetchTransactions(null, null, null, null, currentPage, PAGE_SIZE);
            List<Transaction> allTxs = WalletService.getInstance()
                    .fetchTransactions(null, null, null, null, 1, 10000);
            Platform.runLater(() -> {
                this.wallet = w;
                calculateSummary(allTxs);
                refreshSummary();
                txList.setAll(txs);
                updatePageLabel();
            });
        }, "WalletLoader").start();
    }

    // ── Refresh tóm tắt ───────────────────────────────────────────────
    private void refreshSummary() {
        if (wallet == null) return;
        lblBalance       .setText(fmt(wallet.getBalance()));
        lblTotalDeposited.setText(fmt(wallet.getTotalDeposited()));
        lblTotalHeld     .setText(fmt(wallet.getTotalHeld()));
        lblTotalRefunded .setText(fmt(wallet.getTotalRefunded()));
        lblTotalPaid     .setText(fmt(wallet.getTotalPaid()));

        // Cập nhật UserSession để navbar đồng bộ
        UserSession.getInstance().setBalance(wallet.getBalance());
        HomeController home = HomeController.getInstance();
        if (home != null) home.refreshBalanceLabel();
    }

    private String fmt(double v) { return String.format("%,.0f VNĐ", v); }

    // ── Nạp tiền ──────────────────────────────────────────────────────
    @FXML
    private void handleDeposit() {
        String amountStr = txtDepositAmount.getText().trim();
        if (amountStr.isEmpty()) {
            showDepositStatus("⚠ Vui lòng nhập số tiền", "#f39c12");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr.replace(",", "").replace(".", ""));
        } catch (NumberFormatException e) {
            showDepositStatus("⚠ Số tiền không hợp lệ", "#e74c3c");
            return;
        }

        if (amount < 10_000) {
            showDepositStatus("⚠ Tối thiểu 10.000 VNĐ", "#e74c3c");
            return;
        }

        String method = cmbPaymentMethod.getValue();

        // UI: đang xử lý
        btnDeposit.setDisable(true);
        depositSpinner.setVisible(true);
        showDepositStatus("⏳ Đang xử lý...", "#f39c12");

        new Thread(() -> {
            Transaction tx = WalletService.getInstance().deposit(amount, method);
            Platform.runLater(() -> {
                btnDeposit.setDisable(false);
                depositSpinner.setVisible(false);

                if (tx.getStatus() == Transaction.Status.SUCCESS) {
                    // Cập nhật số dư
                    if (wallet != null) {
                        wallet.setBalance(wallet.getBalance() + amount);
                        wallet.setTotalDeposited(wallet.getTotalDeposited() + amount);
                        refreshSummary();
                        txList.add(0, tx); // thêm vào đầu danh sách
                    }
                    txtDepositAmount.clear();
                    showDepositStatus("✅ Nạp tiền thành công!", "#27ae60");

                    ToastNotification.success(
                            btnDeposit.getScene().getWindow(),
                            "Nạp " + String.format("%,.0f VNĐ", amount) + " thành công!");
                } else {
                    showDepositStatus("❌ Nạp tiền thất bại: " + tx.getNote(), "#e74c3c");
                    ToastNotification.error(btnDeposit.getScene().getWindow(), "Nạp tiền thất bại!");
                }
            });
        }, "DepositThread").start();
    }

    private void showDepositStatus(String msg, String color) {
        lblDepositStatus.setText(msg);
        lblDepositStatus.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 13; -fx-font-weight: bold;");
        lblDepositStatus.setVisible(true);
    }

    // ── Filter ────────────────────────────────────────────────────────
    @FXML
    private void handleFilter() {
        currentPage = 1;
        applyFilter();
    }

    @FXML
    private void handleResetFilter() {
        cmbFilterType  .getSelectionModel().selectFirst();
        cmbFilterStatus.getSelectionModel().selectFirst();
        dpFrom.setValue(null);
        dpTo  .setValue(null);
        currentPage = 1;
        applyFilter();
    }

    private void applyFilter() {
        String typeStr   = mapTypeLabel  (cmbFilterType  .getValue());
        String statusStr = mapStatusLabel(cmbFilterStatus.getValue());
        String from = dpFrom.getValue() != null ? dpFrom.getValue().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : null;
        String to   = dpTo  .getValue() != null ? dpTo  .getValue().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : null;

        new Thread(() -> {
            List<Transaction> txs = WalletService.getInstance().fetchTransactions(typeStr, statusStr, from, to, currentPage, PAGE_SIZE);
            Platform.runLater(() -> {
                txList.setAll(txs);
                updatePageLabel();
            });
        }, "FilterThread").start();
    }

    // ── Phân trang ────────────────────────────────────────────────────
    @FXML private void handlePrevPage() { if (currentPage > 1) { currentPage--; applyFilter(); } }
    @FXML private void handleNextPage() { currentPage++; applyFilter(); }

    private void updatePageLabel() {
        lblPage.setText("Trang " + currentPage);
        btnPrevPage.setDisable(currentPage == 1);
        btnNextPage.setDisable(txList.size() < PAGE_SIZE);
    }

    // ── Helpers ────────────────────────────────────────────────────────
    private String mapTypeLabel(String label) {
        if (label == null || label.equals("Tất cả")) return null;
        for (Transaction.Type t : Transaction.Type.values())
            if (t.label.equals(label)) return t.name();
        return null;
    }

    private String mapStatusLabel(String label) {
        if (label == null || label.equals("Tất cả")) return null;
        for (Transaction.Status s : Transaction.Status.values())
            if (s.label.equals(label)) return s.name();
        return null;
    }

    // ── Public API: được gọi từ AuctionDetailController ───────────────

    /**
     * Giữ tiền đặt cọc khi Bidder vừa đặt giá thành công.
     * Gọi từ AuctionDetailController sau khi server xác nhận bid.
     */
    public static void notifyBidPlaced(String auctionId, String auctionName,
                                       double depositAmount, double bidAmount) {
        new Thread(() -> {
            Transaction tx = WalletService.getInstance()
                    .holdForBid(Integer.parseInt(auctionId), depositAmount);

            Platform.runLater(() -> {
                HomeController home = HomeController.getInstance();
                if (home == null) return;

                if (tx.getStatus() == Transaction.Status.SUCCESS) {
                    UserSession.getInstance().deductBalance(depositAmount);
                    home.refreshBalanceLabel();
                    ToastNotification.success(home.getRoot().getScene().getWindow(),
                            "🔒 Đặt cọc " + String.format("%,.0f VNĐ", depositAmount)
                                    + " cho \"" + auctionName + "\"");
                } else {
                    ToastNotification.error(
                            home.getRoot().getScene().getWindow(),
                            "❌ Không đủ số dư để đặt cọc!");
                }
            });
        }, "HoldThread").start();
    }

    /**
     * Thông báo thắng đấu giá.
     */
    public static void notifyAuctionWon(String auctionName, double amount) {
        Platform.runLater(() -> {
            HomeController home = HomeController.getInstance();
            if (home == null) return;
            ToastNotification.success(
                    home.getRoot().getScene().getWindow(),
                    "🏆 Chúc mừng! Bạn thắng đấu giá \"" + auctionName + "\"");
        });
    }

    /**
     * Thông báo thua và hoàn tiền.
     */
    public static void notifyAuctionLost(String auctionName, double refundAmount) {
        Platform.runLater(() -> {
            HomeController home = HomeController.getInstance();
            if (home == null) return;
            UserSession.getInstance().addBalance(refundAmount);
            home.refreshBalanceLabel();
            ToastNotification.info(
                    home.getRoot().getScene().getWindow(),
                    "💸 Hoàn " + String.format("%,.0f VNĐ", refundAmount)
                            + " - Thua đấu giá \"" + auctionName + "\"");
        });
    }

    // Thêm vào cuối WalletController.java
    @FXML private void handleQuickDeposit100()  { setDepositAmount(100_000);    }
    @FXML private void handleQuickDeposit500()  { setDepositAmount(500_000);    }
    @FXML private void handleQuickDeposit1M()   { setDepositAmount(1_000_000);  }
    @FXML private void handleQuickDeposit5M()   { setDepositAmount(5_000_000);  }
    @FXML
    private void handleQuickDeposit10M()  { setDepositAmount(10_000_000); }

    // ── Hàm tự động phân loại và cộng dồn 4 ô thông số ────────────────
    private void calculateSummary(List<Transaction> allTxs) {
        double deposited = 0, paid = 0, refunded = 0, hold = 0, release = 0;

        for (Transaction tx : allTxs) {
            // Chỉ tính tiền vào các giao dịch có trạng thái THÀNH CÔNG
            if (tx.getStatus() == Transaction.Status.SUCCESS) {
                String type = tx.getType().name();
                switch (type) {
                    case "DEPOSIT":     deposited += tx.getAmount(); break;
                    case "PAYMENT":     paid      += tx.getAmount(); break;
                    case "REFUND":      refunded  += tx.getAmount(); break;
                    case "BID_HOLD":    hold      += tx.getAmount(); break;
                    case "BID_RELEASE": release   += tx.getAmount(); break;
                }
            }
        }

        // Tiền đang cọc thực tế = Tổng tiền hệ thống đã giữ (Hold) - Tổng tiền hệ thống đã trả lại (Release)
        double currentHolding = hold - release;

        // Gán số liệu vừa tính được vào đối tượng wallet để refreshSummary() lấy hiển thị
        if (this.wallet != null) {
            this.wallet.setTotalDeposited(deposited);
            this.wallet.setTotalPaid(paid);
            this.wallet.setTotalRefunded(refunded);
            this.wallet.setTotalHeld(currentHolding > 0 ? currentHolding : 0);
        }
    }

    private void setDepositAmount(double amount) {
        txtDepositAmount.setText(String.format("%.0f", amount));
    }
}
