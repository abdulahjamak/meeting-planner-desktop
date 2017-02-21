package meetings;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class Email {
    private Session session;
    private Transport transport;

    static Properties props = new Properties();

    static {
        try {
            props.load(Email.class.getResourceAsStream("/email.txt"));
        } catch (IOException e) {
            System.out.println("Please add the email properties file in the resource folder!");
        }
    }

    private Email() {
    }

    public static void email(Participant... participants) {
        if (participants.length == 0) return;
        Task<Boolean> task = new Task<Boolean>() {
            int counter = 0;

            @Override
            public Boolean call() {
                Email email = new Email();
                try {
                    email.connect();
                    for (Participant p : participants) {
                        Set<String> emails = new HashSet<>();
                        for (String s : p.getEmail().split(",")) {
                            emails.add(s.trim());
                        }
                        for (String address : emails) {
                            System.out.println(address);
                            if (isCancelled()) break;
                            String result = email.send(address, p.getName(), p.generate());
                            if (result.equals("Sent!")) p.setExportChecked(false);
                            counter++;
                            updateMessage(String.format("%s (%s): %s", address, p.getName(), result));
                            updateProgress(counter, participants.length);
                        }
                    }
                } catch (Exception e1) {
                    e1.printStackTrace();
                    System.out.println(e1.getMessage());
                    Platform.runLater(() -> {
                        Alert a = new Alert(Alert.AlertType.ERROR);
                        a.setHeaderText("Couldn't connect to mail server");
                        a.setContentText(e1.getMessage());
                        a.show();
                    });
                    return false;
                } finally {
                    email.close();
                }
                return true;
            }
        };
        start(task);
    }

    private static void start(Task<Boolean> task) {
        ObservableList<String> list = FXCollections.observableArrayList();
        list.add("Connecting...");
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.UTILITY);
        Button close = new Button("Cancel");
        close.setOnAction(event -> task.cancel());
        ListView<String> listView = new ListView<>(list);
        ProgressBar bar = new ProgressBar();
        bar.setPrefWidth(Double.MAX_VALUE);
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(20));
        vbox.getChildren().addAll(bar, listView, close);
        stage.setScene(new Scene(vbox, 640, 480));
        stage.setOnCloseRequest(event -> task.cancel());
        task.setOnSucceeded(e -> {
            close.setText("Close");
            close.setOnAction(e1 -> stage.close());
            stage.setOnCloseRequest(event -> stage.close());
            if (!task.getValue()) stage.close();
        });
        task.messageProperty().addListener((observable, oldValue, newValue) -> list.add(newValue));
        bar.progressProperty().bind(task.progressProperty());
        task.setOnFailed(e -> stage.close());
        stage.show();
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void connect() {

        //props.list(System.out);
        session = Session.getDefaultInstance(props);


        try {
            transport = session.getTransport("smtp");
            transport.connect(props.getProperty("mail.smtp.user"), props.getProperty("mail.smtp.password"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String send(String to, String name, Path file) {
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(props.getProperty("address")));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(Export.emailSubject.get().replaceAll("#name", name));

            BodyPart body = new MimeBodyPart();
            body.setText(Export.emailTemplate.get().replaceAll("#name", name));

            BodyPart attachment = new MimeBodyPart();
            DataSource source = new FileDataSource(file.toFile());
            attachment.setDataHandler(new DataHandler(source));
            attachment.setFileName(file.getFileName().toString());

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(body);
            multipart.addBodyPart(attachment);

            message.setContent(multipart);
            message.saveChanges();
            InternetAddress[] address = {new InternetAddress(to)};
            transport.sendMessage(message, address);
            return "Sent!";
        } catch (MessagingException e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    private void close() {
        try {
            transport.close();
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}
