package com.example.email.controller;

import com.example.email.communication.*;
import com.example.email.model.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NewEmailController {
    @FXML
    private TextField textFieldReceivers;
    @FXML
    private TextField textFieldSubject;
    @FXML
    private TextArea textAreaMessage;
    private String sender;
    private String receivers;
    private String subject;
    private String message;
    private Email newEmail;
    private Socket socket;
    private Socket tempSocket;
    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;
    private boolean serverState = true;
    private final Alert alertError = new Alert(Alert.AlertType.ERROR);
    private final Alert alertWarning = new Alert(Alert.AlertType.WARNING);


    public void setSender(String sender) {
        this.sender = sender;
    }

    public void setReceivers(String receivers) {
        textFieldReceivers.setText(receivers);
    }

    public void setReceiversReplyAll(List<String> receivers) {
        String receiversStr = String.join(", ", receivers);
        textFieldReceivers.setText(receiversStr);
    }

    public void setSubject(String subject) {
        textFieldSubject.setText(subject);
    }

    public void setMessage(String message, String operation) {
        String newMessage = "";

        switch (operation) {
            case "forward":
                newMessage = "Email inoltrata da: " + sender + "\n\n" + message;
                break;
            case "reply":
                newMessage = message + "\n\n------------------------------------------------------\n\n";
                break;
            default:
                break;
        }
        textAreaMessage.setText(newMessage);
    }

    public void disableReceiversTextField() {
        textFieldReceivers.setDisable(true);
    }

    public void disableSubjectTextField() {
        textFieldSubject.setDisable(true);
    }

    public void disableMessageTextArea() {
        textAreaMessage.setDisable(true);
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

    public void sendEmail() {
        isServerOn();
        if (serverState) {
            try {
                openConnection();
                receivers = textFieldReceivers.getText();
                subject = textFieldSubject.getText();
                message = textAreaMessage.getText();

                if (receivers.isEmpty() || subject.isEmpty() || message.isEmpty()) {
                    showAlert(alertWarning, "Attenzione", "Uno o più campi vuoti");
                    return;
                }

                String emailID = UUID.randomUUID().toString();
                // Definisci il formato desiderato
                LocalDateTime now = LocalDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
                String formattedDateTime = now.format(formatter);

                List<String> receiversList = Arrays.asList(receivers.split(",\\s*"));
                for (String receiver : receiversList) {
                    if (!isValidEmailAddress(receiver)) {
                        showAlert(alertWarning, "Attenzione", "Formato indirizzi email destinatari non valido");
                        return;
                    }
                }
                newEmail = new Email(emailID, sender, receiversList, subject, formattedDateTime, message);

                Request sendEmailRequest = new Request("send", newEmail);
                sendRequest(sendEmailRequest);

                Response sendEmailResponse = (Response) objectInputStream.readObject();

                if (!sendEmailResponse.getResult()) {
                    showAlert(alertWarning, "Attenzione", "Uno o più destinatari non esistono");
                } else {
                    Stage stage = (Stage) textFieldReceivers.getScene().getWindow();
                    stage.close();
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                closeConnection();
            }
        } else {
            showAlert(alertError, "Errore", "Operazione fallita (server offline)");
        }
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

    private boolean isValidEmailAddress(String emailAddress) {
        String MAIL_REGEX = "[a-zA-Z0-9_.+-]+@[a-zA-Z0-9_.+-]+\\.[a-zA-Z0-9-.]+";
        Pattern pattern = Pattern.compile(MAIL_REGEX);
        Matcher matcher = pattern.matcher(emailAddress);
        return matcher.matches();
    }

    private void showAlert(Alert alert, String title, String content) {
        Platform.runLater(() -> {
            alert.setTitle(title);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }
}
