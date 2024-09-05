package com.example.email;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class ClientApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ClientApp.class.getResource("login.fxml"));
        Parent root = fxmlLoader.load();

        Scene scene = new Scene(root, 600, 300);
        stage.setTitle("Login");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.setX(0);
        stage.show();
        stage.setOnCloseRequest(event -> {
            System.exit(0);
        });
    }

    public static void main(String[] args){launch();}
}
