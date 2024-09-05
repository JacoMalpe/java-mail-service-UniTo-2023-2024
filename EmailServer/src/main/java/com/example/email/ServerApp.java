package com.example.email;

import com.example.email.controller.ServerLogController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import java.io.IOException;

public class ServerApp extends Application {
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ServerApp.class.getResource("serverLog.fxml"));
        Parent root = fxmlLoader.load();
        ServerLogController serverLogController = fxmlLoader.getController();

        Scene scene = new Scene(root, 520, 300);
        stage.setOnCloseRequest(e -> serverLogController.stopServer());
        stage.setTitle("Server");
        stage.setScene(scene);
        stage.setResizable(false);
        double screenWidth = Screen.getPrimary().getBounds().getWidth();
        stage.setX(screenWidth - 600);
        stage.show();
    }

    public static void main(String[] args){launch();}
}
