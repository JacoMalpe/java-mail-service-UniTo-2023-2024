package com.example.email.communication;

import com.example.email.model.*;
import java.io.Serializable;

public class Request implements Serializable {
    private String requestType;
    private String tableType;
    private User user;
    private Email email;


    public Request(String requestType) {
        this.requestType = requestType;
    }

    public Request(String requestType, User user) {
        this.requestType = requestType;
        this.user = user;
    }

    public Request(String requestType, Email email) {
        this.requestType = requestType;
        this.email = email;
    }

    public Request(String requestType, User user, Email email, String table) {
        this.requestType = requestType;
        this.user = user;
        this.email = email;
        this.tableType = table;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public String getTableType() {
        return tableType;
    }

    public void setTableType(String tableType) {
        this.tableType = tableType;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Email getEmail() {
        return email;
    }

    public void setEmail(Email email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "Request{" +
                "requestType='" + requestType + '\'' +
                ", tableType='" + tableType + '\'' +
                ", user=" + user +
                ", email=" + email +
                '}';
    }
}
