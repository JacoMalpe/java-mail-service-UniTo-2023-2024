package com.example.email.communication;

import com.example.email.model.*;
import java.io.Serializable;
import java.util.List;

public class Response implements Serializable {
    private boolean result;
    private List<Email> emailIn;
    private List<Email> emailOut;


    public Response(boolean result) {
        this.result = result;
    }

    public Response(boolean result, List<Email> emailIn, List<Email> emailOut) {
        this.result = result;
        this.emailIn = emailIn;
        this.emailOut = emailOut;
    }

    public boolean getResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
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

    @Override
    public String toString() {
        return "Response{" +
                "result=" + result +
                ", emailIn=" + emailIn +
                ", emailOut=" + emailOut +
                '}';
    }
}
