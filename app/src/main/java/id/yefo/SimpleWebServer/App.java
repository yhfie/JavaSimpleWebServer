/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package id.yefo.SimpleWebServer;

import java.io.IOException;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.Parent;

import javafx.stage.Stage;

import javafx.fxml.FXMLLoader;

public class App extends Application {
    public String getGreeting() {
        return "Hello World!";
    }
    
    @Override
    public void start(Stage primaryStage) throws IOException {
        // Load file view sbg view utama
        Parent root = FXMLLoader.load(getClass().getResource("/id/yefo/SimpleWebServer/MainView.fxml"));
        
        // Membuat Scene baru dengan FXML yang telah di-load
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Yefo's Simple Web Server");
        primaryStage.show();
    }

    public static void main(String[] args) {
//        System.out.println(new App().getGreeting());

        // Launch aplikasi JavaFX
        launch(args);
    }
}
