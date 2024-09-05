package com.example.email.model;

import java.io.Serializable;
import java.util.List;

public class Email implements Serializable {
    private String id;
    private String sender;
    private List<String> receivers;
    private String subject;
    private String date;
    private String message;


    public Email(String id, String sender, List<String> receivers, String subject, String date, String message) {
        this.id = id;
        this.sender = sender;
        this.receivers = receivers;
        this.subject = subject;
        this.date = date;
        this.message = message;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public List<String> getReceivers() {
        return receivers;
    }

    public void setReceivers(List<String> receivers) {
        this.receivers = receivers;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "Email{" +
                "'" + id + '\'' +
                "; '" + sender + '\'' +
                "; " + receivers +
                "; '" + subject + '\'' +
                "; " + date +
                "; '" + message + '\'' +
                '}';
    }
}
