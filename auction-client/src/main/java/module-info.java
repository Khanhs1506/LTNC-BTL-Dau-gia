module birdeggsolution {
    requires javafx.graphics;
    requires javafx.fxml;
    requires javafx.controls;
    requires java.sql;
    requires mysql.connector.j;
    opens sample to javafx.fxml;
    exports sample;
    exports sample.model;
    opens sample.model to com.google.gson, javafx.fxml;
    requires com.google.gson;
    requires java.desktop;
}

