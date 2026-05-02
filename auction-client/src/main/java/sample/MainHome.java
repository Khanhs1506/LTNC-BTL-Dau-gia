//package sample;
//
//import javafx.application.Application;
//import javafx.fxml.FXMLLoader;
//import javafx.scene.Parent;
//import javafx.scene.Scene;
//import javafx.stage.Stage;
//import javafx.stage.StageStyle;
//
//public class MainHome extends Application {
//
//    @Override
//    public void start(Stage primaryStage) {
//        try {
//            FXMLLoader loader = new FXMLLoader(
//                    getClass().getResource("/sample/home_demo.fxml")
//            );
//            Parent root = loader.load();
//
//            primaryStage.initStyle(StageStyle.UNDECORATED); // Chức năng: Tràn viền, xóa bỏ tiêu đề mặc định của Windows(X, phóng to, thu nhỏ)
//            primaryStage.setScene(new Scene(root, 1200, 800));
//            primaryStage.setTitle("Tiny Hoarder's Key Market");
//            primaryStage.show();
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            e.getCause();
//        }
//    }
//
//    public static void main(String[] args) {
//        launch(args);
//    }
//}

// --> ===== KHÔNG CHẠY =====