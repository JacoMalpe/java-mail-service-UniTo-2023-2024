package com.example.email.model;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TextArea;

public class ServerLog {
    private boolean serverState = true;
    private final SimpleStringProperty logs;
    private TextArea textArea;


    public void setTextArea(TextArea textArea) {
        this.textArea = textArea;
    }

    public boolean getServerState() {
        return serverState;
    }

    public void setServerState(boolean value) {
        this.serverState = value;
    }

    public ServerLog() {
        logs = new SimpleStringProperty("");
    }

    public SimpleStringProperty getLogsProperty() {
        return logs;
    }

    public void addActionLog(String request) {
        String currentText = logs.getValue();
        String newText = currentText + request;

        Platform.runLater(() -> {
            logs.setValue(newText);
            if (textArea != null) {
                textArea.appendText("");
                textArea.positionCaret(textArea.getLength());
            }});
    }

    @Override
    public String toString() {
        return "ServerLog{" +
                "serverState=" + serverState +
                ", logs=" + logs +
                ", textArea=" + textArea +
                '}';
    }
}
