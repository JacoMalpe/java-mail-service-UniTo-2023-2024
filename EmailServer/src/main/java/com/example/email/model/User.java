package com.example.email.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class User implements Serializable {
    private String emailAddress;
    private List<Email> emailIn = new ArrayList<>();
    private List<Email> emailOut = new ArrayList<>();


    public User(String emailAddress) {
       setEmailAddress(emailAddress);
       setEmailIn(emailIn);
       setEmailOut(emailOut);
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public List<Email> getEmailIn() {
        return emailIn;
    }

    public void setEmailIn(List<Email> emailIn) {
        this.emailIn = emailIn;
    }

    public List<Email> getEmailOut() {
        return emailOut;
    }

    public void setEmailOut(List<Email> emailOut) {
        this.emailOut = emailOut;
    }

    public void removeEmailIn(Email email) {
        emailIn.remove(email);
    }

    public void addEmailIn(List<Email> emailIn) {
        this.emailIn.addAll(emailIn);
    }

    public void removeEmailOut(Email email) {
        emailOut.remove(email);
    }

    public void addEmailOut(List<Email> emailOut) {
        this.emailOut.addAll(emailOut);
    }

    @Override
    public String toString() {
        return "User{" +
                "emailAddress='" + emailAddress + '\'' +
                ", emailIn=" + emailIn +
                ", emailOut=" + emailOut +
                '}';
    }
}
