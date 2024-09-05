package com.example.email.controller;

import com.example.email.model.*;
import com.example.email.communication.*;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ServerLogController {
    @FXML
    private TextArea textAreaLog;
    private ServerSocket serverSocket;
    private ServerLog serverLog;
    private static final int NUM_THREAD = 3;
    private ExecutorService fixedThreadPool;
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = readWriteLock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = readWriteLock.writeLock();


    @FXML
    public void initialize() {
        if (serverLog != null) {
            throw new IllegalStateException("Server can only be initialized once");
        }
        serverLog = new ServerLog();
        serverLog.setTextArea(textAreaLog);
        textAreaLog.textProperty().bind(serverLog.getLogsProperty());
        textAreaLog.setWrapText(true);
        startServer();
    }

    public void startServer() {
        System.out.println("Start server");
        serverLog.setServerState(true);
        serverLog.addActionLog("Server on\n");

        try {
            serverSocket = new ServerSocket(9000);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        fixedThreadPool = Executors.newFixedThreadPool(NUM_THREAD);

        new Thread(() -> {
            while (serverLog.getServerState()) {
                try {
                    Socket requestSocket = serverSocket.accept();
                    ServerThread serverThread = new ServerThread(requestSocket);
                    fixedThreadPool.execute(serverThread);
                }
                catch (Exception e) {
                    if (!serverSocket.isClosed()) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public void stopServer() {
        System.out.println("Stop server");
        serverLog.setServerState(false);
        try {
            serverSocket.close();
            if (!fixedThreadPool.awaitTermination(1, TimeUnit.SECONDS)) {
                fixedThreadPool.shutdownNow();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class ServerThread implements Runnable {
        private Socket requestSocket;
        private ObjectInputStream objectInputStream;
        private ObjectOutputStream objectOutputStream;


        public ServerThread(Socket socket) {
            this.requestSocket = socket;
            try {
                objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                objectInputStream = new ObjectInputStream(socket.getInputStream());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                if (objectInputStream != null && !requestSocket.isClosed()) {
                    Request requestUser = (Request) objectInputStream.readObject();
                    System.out.println("Open connection");
                    if (requestUser == null) {
                        System.out.println("Client closed the connection.");
                        return;
                    }

                    Response responseServer;
                    switch (requestUser.getRequestType()) {
                        case "check" -> {
                            responseServer = new Response(true);
                            sendResponse(responseServer);
                        }
                        case "login" -> {
                            if (!isUserAlreadyLoggedIn(requestUser.getUser().getEmailAddress())) {
                                insertUserToFile(requestUser.getUser().getEmailAddress());
                            }
                            synchronized (textAreaLog) {
                                serverLog.addActionLog(requestUser.getUser().getEmailAddress() + " ha effettuato il login\n");
                            }
                            responseServer = new Response(true);
                            sendResponse(responseServer);
                        }
                        case "disconnect" -> {
                            synchronized (textAreaLog) {
                                serverLog.addActionLog(requestUser.getUser().getEmailAddress() + " si è disconnesso\n");
                            }
                            responseServer = new Response(true);
                            sendResponse(responseServer);
                        }
                        case "getAll" -> {
                            synchronized (textAreaLog) {
                                serverLog.addActionLog(requestUser.getUser().getEmailAddress() + " ha richiesto la casella di posta\n");
                            }
                            responseServer = (Response) sendAllEmails(requestUser.getUser().getEmailAddress());
                            sendResponse(responseServer);
                        }
                        case "send" -> {
                            Email email = requestUser.getEmail();

                            if(!isValidReceivers(email.getReceivers())) {
                                responseServer = new Response(false);
                                sendResponse(responseServer);
                                return;
                            }

                            String receiversStr = String.join(", ", email.getReceivers());
                            synchronized (textAreaLog) {
                                serverLog.addActionLog(email.getSender() + " ha inviato una email a " + receiversStr + "\n");
                            }
                            saveMail(email);
                            responseServer = new Response(true);
                            sendResponse(responseServer);
                        }
                        case "delete" -> {
                            String tableType = requestUser.getTableType();
                            Email email = requestUser.getEmail();
                            Path path;
                            synchronized (textAreaLog) {
                                if (tableType.equals("tableIn")) {
                                    path = Paths.get("./EmailServer/src/main/java/com/example/email/database/", requestUser.getUser().getEmailAddress(), "in", email.getId() + ".txt");
                                    serverLog.addActionLog(requestUser.getUser().getEmailAddress() + " ha eliminato una email in entrata" + "\n");
                                } else {
                                    path = Paths.get("./EmailServer/src/main/java/com/example/email/database/", requestUser.getUser().getEmailAddress(), "out", email.getId() + ".txt");
                                    serverLog.addActionLog(requestUser.getUser().getEmailAddress() + " ha eliminato una email in uscita" + "\n");
                                }
                            }
                            deleteMail(path);
                            responseServer = new Response(true);
                            sendResponse(responseServer);
                        }
                        case "update" -> {
                            synchronized (textAreaLog) {
                                serverLog.addActionLog("aggiornamento casella di posta di " + requestUser.getUser().getEmailAddress() + "\n");
                            }
                            List<Email> emailIn = requestUser.getUser().getEmailIn();
                            List<Email> emailOut = requestUser.getUser().getEmailOut();

                            responseServer = (Response) updateMail(requestUser.getUser().getEmailAddress(), emailIn, emailOut);
                            sendResponse(responseServer);
                        }
                    }
                }
                System.out.println("Close connection");
            } catch (IOException | ClassNotFoundException e) {
                // nessuna richiesta da parte dei client
            } finally {
                try {
                    if (objectInputStream != null) {
                        objectInputStream.close();
                    }
                    if (objectOutputStream != null) {
                        objectOutputStream.close();
                    }
                    if (requestSocket != null && !requestSocket.isClosed()) {
                        requestSocket.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public Response sendAllEmails(String emailAddress) {
            List<Email> emailsIn = readEmailsFromFile(Paths.get("./EmailServer/src/main/java/com/example/email/database/", emailAddress, "in"));
            List<Email> emailsOut = readEmailsFromFile(Paths.get("./EmailServer/src/main/java/com/example/email/database/", emailAddress, "out"));

            return new Response(true, emailsIn, emailsOut);
        }

        public void saveMail(Email email) {
            String emailID = email.getId();

            Path outFilePath = Paths.get("./EmailServer/src/main/java/com/example/email/database/", email.getSender(), "out", emailID + ".txt");

            writeMailToFile(email, outFilePath);

            for (String receiver : email.getReceivers()) {
                Path inFilePath = Paths.get("./EmailServer/src/main/java/com/example/email/database/", receiver, "in", emailID + ".txt");
                writeMailToFile(email, inFilePath);
            }
        }

        public static void deleteMail(Path filePath) {
            try {
                Files.delete(filePath);
            } catch (NoSuchFileException nsfe) {
                System.out.println("Il file non esiste: " + filePath);
            } catch (DirectoryNotEmptyException dnee) {
                System.out.println("Il percorso non è un file: " + filePath);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public Response updateMail(String emailAddress, List<Email> emailsIn, List<Email> emailsOut) {
            List<Email> notVisualisedIn = findNotVisualisedEmails("./EmailServer/src/main/java/com/example/email/database/" + emailAddress + "/in", emailsIn);
            List<Email> notVisualisedOut = findNotVisualisedEmails("./EmailServer/src/main/java/com/example/email/database/" + emailAddress + "/out", emailsOut);

            return new Response(true, notVisualisedIn, notVisualisedOut);
        }

        private boolean isUserAlreadyLoggedIn(String emailAddress) {
            try {
                readLock.lock();
                List<String> lines = Files.readAllLines(Paths.get("./EmailServer/src/main/java/com/example/email/database/users.txt"));
                return lines.stream().anyMatch(line -> line.trim().equals(emailAddress));
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            finally {
                readLock.unlock();
            }
        }

        private boolean isValidReceivers(List<String> receivers) {

            try {
                Path usersFilePath = Paths.get("./EmailServer/src/main/java/com/example/email/database/users.txt");

                List<String> validUsers = Files.readAllLines(usersFilePath);

                for (String receiver : receivers) {
                    if (!validUsers.contains(receiver)) {
                        return false;
                    }
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        private void insertUserToFile(String emailAddress) {
            try {
                writeLock.lock();
                Files.write(Paths.get("./EmailServer/src/main/java/com/example/email/database/users.txt"), (emailAddress + "\n").getBytes(), StandardOpenOption.APPEND);

                Path userFolderPath = Paths.get("./EmailServer/src/main/java/com/example/email/database/" + emailAddress);
                Files.createDirectories(userFolderPath);

                Path inFolderPath = userFolderPath.resolve("in");
                Files.createDirectories(inFolderPath);

                Path outFolderPath = userFolderPath.resolve("out");
                Files.createDirectories(outFolderPath);
            } catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                writeLock.unlock();
            }
        }

        private List<Email> findNotVisualisedEmails(String folderPath, List<Email> emails) {
            List<Email> notVisualisedEmails = new ArrayList<>();

            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(folderPath))) {
                for (Path filePath : directoryStream) {
                    String fileName = filePath.getFileName().toString().replace(".txt", "");

                    // Verifica se l'ID del file non è presente nella lista emails
                    if (emails.stream().noneMatch(email -> email.getId().equals(fileName))) {
                        Email email = readEmailFromFile(filePath);
                        notVisualisedEmails.add(email);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return notVisualisedEmails;
        }

        private void writeMailToFile(Email email, Path filePath) {
            try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(filePath.toFile(), true))) {
                objectOutputStream.writeObject(email);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private Email readEmailFromFile(Path filePath) {
            try (ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(filePath.toFile()))) {
                Object object = objectInputStream.readObject();
                if (object instanceof Email) {
                    return (Email) object;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        private List<Email> readEmailsFromFile(Path folderPath) {
            List<Email> emails = new ArrayList<>();
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(folderPath)) {
                for (Path filePath : directoryStream) {
                    try (ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(filePath.toFile()))) {
                        Object object = objectInputStream.readObject();
                        if (object instanceof Email email) {
                            emails.add(email);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return emails;
        }

        private void sendResponse(Response response) {
            try {
                objectOutputStream.writeObject(response);
                objectOutputStream.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
