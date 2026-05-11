module birdeggsolution {
    requires javafx.graphics;
    requires javafx.fxml;
    requires javafx.controls;
    requires java.sql;
    requires mysql.connector.j;
    requires com.google.gson;
    opens sample to javafx.fxml;
    exports sample;
}