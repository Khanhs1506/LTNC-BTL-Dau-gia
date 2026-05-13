module birdeggsolution {
    requires javafx.graphics;
    requires javafx.fxml;
    requires javafx.controls;
    requires java.sql;
    requires mysql.connector.j;
    opens sample to javafx.fxml;
    opens sample.model to com.google.gson;
    exports sample;
    requires com.google.gson;
}

