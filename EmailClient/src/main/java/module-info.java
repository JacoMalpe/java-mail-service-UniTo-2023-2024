module com.example.emailclient {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.example.email to javafx.fxml;
    exports com.example.email;

    opens com.example.email.communication to javafx.fxml;
    exports com.example.email.communication;

    opens com.example.email.controller to javafx.fxml;
    exports com.example.email.controller;

    opens com.example.email.model to javafx.base;
    exports com.example.email.model;
}