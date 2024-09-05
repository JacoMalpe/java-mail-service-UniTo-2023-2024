package com.example.email.controller;

import com.example.email.model.*;
import com.example.email.communication.*;
import com.example.email.ClientApp;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HomeController {
    @FXML
    private Label labelEmailAddress;
    @FXML
    private Label labelServerState;
    @FXML
    private Button buttonReply;
    @FXML
    private Button buttonReplyAll;
    @FXML
    private TextField textFieldSender;
    @FXML
    private TextField textFieldReceivers;
    @FXML
    private TextField textFieldSubject;
    @FXML
    private TextArea textAreaMessage;
    @FXML
    private TableView<Email> tableIn;
    @FXML
    private TableView<Email> tableOut;
    @FXML
    private TableColumn<Email, String> columnSenderIn;
    @FXML
    private TableColumn<Email, String> columnSubjectIn;
    @FXML
    private TableColumn<Email, LocalDateTime> columnDateIn;
    @FXML
    private TableColumn<Email, String> columnReceiversOut;
    @FXML
    private TableColumn<Email, String> columnSubjectOut;
    @FXML
    private TableColumn<Email, LocalDateTime> columnDateOut;
    private User user;
    private Email selectedEmail;
    private TableView<Email> selectedTable;
    private Socket socket;
    private Socket tempSocket;
    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;
    private NewEmailController newEmailController;
    private boolean serverState = true;
    private final Alert alertError = new Alert(Alert.AlertType.ERROR);
    private final Alert alertWarning = new Alert(Alert.AlertType.WARNING);
    private final Alert alertInformation = new Alert(Alert.AlertType.INFORMATION);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private int numNewEmail = 0;


    public void initializeUser(User user) {
        this.user = user;
        labelEmailAddress.setText(user.getEmailAddress());
        getAllEmails();
        tableIn.setOnMouseClicked(this::handleTableSelection);
        tableOut.setOnMouseClicked(this::handleTableSelection);
        scheduler.scheduleAtFixedRate(this::checkConnection, 0, 3, TimeUnit.SECONDS);
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

    private void checkConnection() {
        try {
            isServerOn();
            if (serverState) {
                Platform.runLater(() -> {
                    labelServerState.setText("ON");
                    labelServerState.setStyle("-fx-text-fill: green;");
                    try {
                        updateEmail();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } else {
                Platform.runLater(() -> {
                    labelServerState.setText("OFF");
                    labelServerState.setStyle("-fx-text-fill: red;");
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getAllEmails() {
        try {
            openConnection();

            Request getAllEmailsRequest = new Request("getAll", user);
            sendRequest(getAllEmailsRequest);

            Response getAllEmailsResponse = (Response) objectInputStream.readObject();

            if (getAllEmailsResponse.getResult()) {
                user.setEmailIn(getAllEmailsResponse.getEmailIn());
                user.setEmailOut(getAllEmailsResponse.getEmailOut());
                populateTables();
            } else {
                System.out.println("Error in getAllEmailsResponse" + user.getEmailAddress());
                showAlert(alertWarning, "Attenzione", "Operazione Fallita");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeConnection();
        }
    }

    public void updateEmail() {
        try {
            openConnection();
            Request updateEmailRequest = new Request("update", user);
            sendRequest(updateEmailRequest);

            Response updateEmailResponse = (Response) objectInputStream.readObject();

            if (updateEmailResponse.getResult()) {
                user.addEmailIn(updateEmailResponse.getEmailIn());
                user.addEmailOut(updateEmailResponse.getEmailOut());
                updateTables(user.getEmailIn(), user.getEmailOut());

                int countNewEmail = updateEmailResponse.getEmailIn().size();
                if (countNewEmail != 0) {
                    alertInformation.setOnCloseRequest(event -> {
                        if (alertInformation.getResult() == ButtonType.OK) {
                            numNewEmail = 0;
                        }
                    });
                    if (!alertInformation.isShowing()) {
                        alertInformation.setHeaderText(user.getEmailAddress());
                        showAlert(alertInformation, "Aggiornamento", countNewEmail + " nuove email");
                    } else {
                        closeAlert(alertInformation);
                        alertInformation.setHeaderText(user.getEmailAddress());
                        showAlert(alertInformation, "Aggiornamento", (countNewEmail + numNewEmail) + " nuove email");
                    }
                    numNewEmail += countNewEmail;
                }
            } else {
                System.out.println("Error in updateEmailResponse" + user.getEmailAddress());
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeConnection();
        }
    }

    @FXML
    private void handleTableSelection(MouseEvent event) {
        try {
            if (event.getSource() instanceof TableView) {
                TableView<Email> selectedTable = (TableView<Email>) event.getSource();
                Email selectedEmail = selectedTable.getSelectionModel().getSelectedItem();

                if (selectedEmail != null) {
                    textFieldSender.setText(selectedEmail.getSender());
                    textFieldSender.setEditable(false);

                    String receivers = String.join(", ", selectedEmail.getReceivers());
                    textFieldReceivers.setText(receivers);
                    textFieldReceivers.setEditable(false);

                    textFieldSubject.setText(selectedEmail.getSubject());
                    textFieldSubject.setEditable(false);

                    textAreaMessage.setText(selectedEmail.getMessage());
                    textAreaMessage.setEditable(false);

                    this.selectedEmail = selectedEmail;
                    this.selectedTable = selectedTable;
                }

                if (this.selectedTable.getId().equals("tableOut")) {
                    buttonReply.setDisable(true);
                    buttonReplyAll.setDisable(true);
                } else {
                    buttonReply.setDisable(false);
                    buttonReplyAll.setDisable(false);
                }
            }
        } catch (NullPointerException e) {
            // gestita eccezione in cui si clicca sui bordi della tabella
        }
    }

    @FXML
    public void deleteEmail() {
        if (serverState) {
            if (selectedEmail != null) {
                selectedTable.getItems().remove(selectedEmail);

                if (selectedTable.getId().equals("tableOut")) {
                    user.removeEmailOut(selectedEmail);
                } else {
                    user.removeEmailIn(selectedEmail);
                }

                textFieldSender.setText("");
                textFieldReceivers.setText("");
                textFieldSubject.setText("");
                textAreaMessage.setText("");

                try {
                    openConnection();
                    Request deleteEmailRequest = new Request("delete", user, selectedEmail, selectedTable.getId());
                    sendRequest(deleteEmailRequest);

                    Response deleteEmailResponse = (Response) objectInputStream.readObject();

                    if (!deleteEmailResponse.getResult()) {
                        System.out.println("Error in deleteEmailResponse" + user.getEmailAddress());
                        showAlert(alertWarning, "Attenzione", "Operazione Fallita");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    closeConnection();
                }

                selectedEmail = null;
                selectedTable = null;
            } else {
                showAlert(alertWarning, "Attenzione", "Seleziona una Email");
            }
        } else {
            showAlert(alertError, "Errore", "Operazione Fallita (server offline)");
        }
    }

    @FXML
    public void newEmail() {
        try {
            FXMLLoader loader = new FXMLLoader(ClientApp.class.getResource("newEmail.fxml"));
            Parent root = loader.load();

            newEmailController = loader.getController();
            newEmailController.setSender(user.getEmailAddress());

            Stage nuovaMailStage = new Stage();
            nuovaMailStage.setTitle("Nuova Email");
            nuovaMailStage.initModality(Modality.APPLICATION_MODAL);
            nuovaMailStage.setResizable(false);

            Scene scene = new Scene(root);
            nuovaMailStage.setScene(scene);

            nuovaMailStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void replyEmail() {
        if (selectedEmail != null) {
            try {
                FXMLLoader loader = new FXMLLoader(ClientApp.class.getResource("newEmail.fxml"));
                Parent root = loader.load();

                newEmailController = loader.getController();
                newEmailController.setSender(user.getEmailAddress());
                newEmailController.setReceivers(textFieldSender.getText());
                newEmailController.setSubject("RE: " + textFieldSubject.getText());
                newEmailController.setMessage(textAreaMessage.getText(), "reply");

                Stage nuovaMailStage = new Stage();
                nuovaMailStage.setTitle("Rispondi Email");
                nuovaMailStage.initModality(Modality.APPLICATION_MODAL);
                nuovaMailStage.setResizable(false);

                Scene scene = new Scene(root);
                nuovaMailStage.setScene(scene);

                newEmailController.disableSubjectTextField();
                newEmailController.disableReceiversTextField();

                nuovaMailStage.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            showAlert(alertWarning, "Attenzione", "Seleziona una Email");
        }
    }

    @FXML
    public void replyAllEmail() {
        if (selectedEmail != null) {
            try {
                FXMLLoader loader = new FXMLLoader(ClientApp.class.getResource("newEmail.fxml"));
                Parent root = loader.load();

                newEmailController = loader.getController();
                newEmailController.setSender(user.getEmailAddress());

                List<String> receiversWithoutMe = new ArrayList<>(selectedEmail.getReceivers());
                receiversWithoutMe.remove(user.getEmailAddress());
                receiversWithoutMe.add(selectedEmail.getSender());
                newEmailController.setReceiversReplyAll(receiversWithoutMe);
                newEmailController.setSubject("RE: " + textFieldSubject.getText());
                newEmailController.setMessage(textAreaMessage.getText(), "reply");

                Stage nuovaMailStage = new Stage();
                nuovaMailStage.setTitle("Rispondi Email");
                nuovaMailStage.initModality(Modality.APPLICATION_MODAL);
                nuovaMailStage.setResizable(false);

                Scene scene = new Scene(root);
                nuovaMailStage.setScene(scene);

                newEmailController.disableSubjectTextField();
                newEmailController.disableReceiversTextField();

                nuovaMailStage.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            showAlert(alertWarning, "Attenzione", "Seleziona una Email");
        }
    }

    @FXML
    public void forwardEmail() {
        if (selectedEmail != null) {
            try {
                FXMLLoader loader = new FXMLLoader(ClientApp.class.getResource("newEmail.fxml"));
                Parent root = loader.load();

                newEmailController = loader.getController();
                newEmailController.setSender(user.getEmailAddress());
                newEmailController.setSubject(textFieldSubject.getText());
                newEmailController.setMessage(textAreaMessage.getText(), "forward");

                Stage nuovaMailStage = new Stage();
                nuovaMailStage.setTitle("Inoltro Email");
                nuovaMailStage.initModality(Modality.APPLICATION_MODAL);
                nuovaMailStage.setResizable(false);

                Scene scene = new Scene(root);
                nuovaMailStage.setScene(scene);

                newEmailController.disableSubjectTextField();
                newEmailController.disableMessageTextArea();

                nuovaMailStage.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            showAlert(alertWarning, "Attenzione", "Seleziona una Email");
        }
    }

    private void populateTables() {
        ObservableList<Email> observableEmailIn = FXCollections.observableArrayList(user.getEmailIn());
        ObservableList<Email> observableEmailOut = FXCollections.observableArrayList(user.getEmailOut());

        columnReceiversOut.setCellValueFactory(new PropertyValueFactory<>("receivers"));
        columnSubjectOut.setCellValueFactory(new PropertyValueFactory<>("subject"));
        columnDateOut.setCellValueFactory(new PropertyValueFactory<>("date"));

        tableOut.setItems(observableEmailOut);

        columnSenderIn.setCellValueFactory(new PropertyValueFactory<>("sender"));
        columnSubjectIn.setCellValueFactory(new PropertyValueFactory<>("subject"));
        columnDateIn.setCellValueFactory(new PropertyValueFactory<>("date"));

        tableIn.setItems(observableEmailIn);
    }

    private void updateTables(List<Email> newEmailIn, List<Email> newEmailOut) {
        ObservableList<Email> observableEmailIn = tableIn.getItems();
        ObservableList<Email> observableEmailOut = tableOut.getItems();

        for (Email newEmail : newEmailIn) {
            if (!observableEmailIn.contains(newEmail)) {
                observableEmailIn.add(newEmail);
            }
        }

        for (Email newEmail : newEmailOut) {
            if (!observableEmailOut.contains(newEmail)) {
                observableEmailOut.add(newEmail);
            }
        }

        columnSenderIn.setCellValueFactory(new PropertyValueFactory<>("sender"));
        columnSubjectIn.setCellValueFactory(new PropertyValueFactory<>("subject"));
        columnDateIn.setCellValueFactory(new PropertyValueFactory<>("date"));

        columnReceiversOut.setCellValueFactory(new PropertyValueFactory<>("receivers"));
        columnSubjectOut.setCellValueFactory(new PropertyValueFactory<>("subject"));
        columnDateOut.setCellValueFactory(new PropertyValueFactory<>("date"));
    }

    public void disconnectUser() {
        try {
            openConnection();

            Request disconnectRequest = new Request("disconnect", user);
            sendRequest(disconnectRequest);

            Response disconnectResponse = (Response) objectInputStream.readObject();

            if (!disconnectResponse.getResult()) {
                System.out.println("Error in disconnect" + user.getEmailAddress());
                showAlert(alertWarning, "Attenzione", "Operazione Fallita");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeConnection();
            System.exit(0);
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

    private void showAlert(Alert alert, String title, String content) {
        Platform.runLater(() -> {
            alert.setTitle(title);
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
