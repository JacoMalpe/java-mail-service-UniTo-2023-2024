package com.example.email.controller;

import com.example.email.model.*;
import com.example.email.communication.*;
import com.example.email.ClientApp;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoginController {
    @FXML
    private TextField textFieldLogin;
    @FXML
    private Button buttonLogin;
    private User user;
    private Socket socket;
    private Socket tempSocket;
    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;
    private HomeController homeController;
    private boolean serverState = false;
    private Alert alertError = new Alert(Alert.AlertType.ERROR);
    private Alert alertWarning = new Alert(Alert.AlertType.WARNING);


    @FXML
    private void login() {
        user = new User(textFieldLogin.getText().trim());

        if (isValidEmailAddress(user.getEmailAddress())) {
            buttonLogin.setDisable(true);
            textFieldLogin.setEditable(false);
            try {
                new Thread(() -> {
                    isServerOn();
                    if (!serverState) {
                        checkConnection();
                    }
                    Platform.runLater(() -> {
                        try {
                            openConnection();

                            Request loginRequest = new Request("login", user);
                            sendRequest(loginRequest);

                            Response loginResponse = (Response) objectInputStream.readObject();

                            if (!loginResponse.getResult()) {
                                System.out.println("Error in loginResponse" + user.getEmailAddress());
                            }

                            FXMLLoader loader = new FXMLLoader(ClientApp.class.getResource("home.fxml"));
                            Parent root = loader.load();

                            homeController = loader.getController();
                            homeController.initializeUser(user);

                            Scene scene = new Scene(root, 1200, 660);
                            Stage stage = (Stage) textFieldLogin.getScene().getWindow();
                            stage.setTitle("Casella di Posta Elettronica");
                            stage.setScene(scene);
                            stage.setResizable(false);
                            stage.setOnCloseRequest(e -> homeController.disconnectUser());
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            closeConnection();
                        }
                    });
                }).start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            showAlert(alertWarning, "Attenzione", "Formato indirizzo email user non valido");
        }
    }

    private boolean isValidEmailAddress(String emailAddress) {
        String MAIL_REGEX = "[a-zA-Z0-9_.+-]+@[a-zA-Z0-9_.+-]+\\.[a-zA-Z0-9-.]+";
        Pattern pattern = Pattern.compile(MAIL_REGEX);
        Matcher matcher = pattern.matcher(emailAddress);
        return matcher.matches();
    }

    private void isServerOn() {
        try {
            tempSocket = new Socket(InetAddress.getLocalHost(), 9000);
            objectOutputStream = new ObjectOutputStream(tempSocket.getOutputStream());
            objectInputStream = new ObjectInputStream(tempSocket.getInputStream());
            Request isServerOnRequest = new Request("check");
            sendRequest(isServerOnRequest);
            Response isServerOnResponse = (Response) objectInputStream.readObject();
            if (isServerOnResponse.getResult()) {
                serverState = true;
            }
        } catch (Exception e) {
            serverState = false;
        } finally {
            try {
                if (objectOutputStream != null) {
                    objectOutputStream.close();
                }
                if (objectInputStream != null) {
                    objectInputStream.close();
                }
                if (tempSocket != null) {
                    tempSocket.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private void openConnection() {
        try {
            socket = new Socket(InetAddress.getLocalHost(), 9000);
            objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectInputStream = new ObjectInputStream(socket.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void closeConnection() {
        if (socket != null && objectOutputStream != null && objectInputStream != null) {
            try {
                objectOutputStream.close();
                objectInputStream.close();
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void checkConnection() {
        showAlert(alertError, "Errore", "Il server è offline...");
        while (!serverState) {
            try {
                isServerOn();
                Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        closeAlert(alertError);
    }

    private void sendRequest(Request request) {
        if (objectOutputStream == null) {
            return;
        }
        try {
            objectOutputStream.writeObject(request);
            objectOutputStream.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(Alert alert, String title, String content) {
        Platform.runLater(() -> {
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    private void closeAlert(Alert alert) {
        Platform.runLater(() -> {
            if (alert != null && alert.isShowing()) {
                alert.close();
            }
        });
    }
}
