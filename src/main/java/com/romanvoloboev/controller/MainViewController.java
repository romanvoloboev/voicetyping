package com.romanvoloboev.controller;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.rpc.ApiStreamObserver;
import com.google.api.gax.rpc.BidiStreamingCallable;
import com.google.auth.Credentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.speech.v1.*;
import com.google.protobuf.ByteString;
import com.romanvoloboev.utils.v1.Microphone;
import de.felixroske.jfxsupport.FXMLController;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ResourceUtils;

import java.io.*;
import java.nio.file.Files;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created at 05.10.17
 *
 * @author romanvoloboev
 */
@FXMLController
public class MainViewController  {
    private static final Logger log = LoggerFactory.getLogger(MainViewController.class);
    private static Microphone microphone;
    private static int buffSize;
    private static ExecutorService executorService = Executors.newSingleThreadExecutor();
    private static ApiStreamObserver<StreamingRecognizeRequest> requestObserver;
    private static ResponseApiStreamingObserver responseObserver;



    @FXML
    private TextArea textArea;

    @Autowired
    public MainViewController(Microphone mic) {
        microphone = mic;
        buffSize = microphone.getTargetDataLine().getBufferSize();
        startRecognition();
    }


    public void openAction(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите файл");
        FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter("Текстовые файлы", "*.txt", "*.doc", "*.docx");
        fileChooser.getExtensionFilters().add(extensionFilter);
        File file = fileChooser.showOpenDialog(new Stage());
        if (file != null) {
            try {
                getText(file);
            } catch (IOException e) {
                new Alert(Alert.AlertType.ERROR, "Неподдерживаемый формат файла.").showAndWait();
            }
        }
    }

    public void exitAction(ActionEvent actionEvent) {
        Platform.exit();
        System.exit(0);
    }

    public void saveAction(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранение в файл");
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("txt files", "*.txt");
        fileChooser.getExtensionFilters().add(extFilter);
        File fileWhereSave = fileChooser.showSaveDialog(new Stage());
        if (fileWhereSave != null) {
            try {
                saveTextAreaToFile(fileWhereSave);
            } catch (IOException e) {
                new Alert(Alert.AlertType.ERROR, "Не удалось сохранить файл").showAndWait();
            }
        }

    }




    private void getText(File file) throws IOException {
        String contentType = Files.probeContentType(file.toPath());
        if (contentType == null || !contentType.equals("text/plain")
                && !contentType.equals("application/msword")
                && !contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
            throw new IOException();
        } else if (contentType.equals("application/msword") || contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
            XWPFDocument document = new XWPFDocument(new FileInputStream(file));
            XWPFWordExtractor extractor = new XWPFWordExtractor(document);
            textArea.setText(extractor.getText());
        } else {
            textArea.setWrapText(true);
            textArea.clear();
            Files.lines(file.toPath()).forEachOrdered(s -> textArea.appendText(s+"\n"));
        }
    }


    private void saveTextAreaToFile(File fileWhereSave) throws IOException {
        try (BufferedWriter bf = new BufferedWriter(new FileWriter(fileWhereSave))) {
            bf.write(textArea.getText());
        }
    }

    private static Credentials loadCredentialsFromFile() throws IOException {
        String credentialsFile = "classpath:credentials.json";
        log.info("Loading credentials from specified file: {}", credentialsFile);
        try (FileInputStream fileInputStream = new FileInputStream(ResourceUtils.getFile(credentialsFile))) {
            Credentials credentials = ServiceAccountCredentials.fromStream(fileInputStream);
            log.info("Successfully loaded from file.");
            return credentials;
        }
    }

    private static class RecognitionTask implements Callable<Void> {
        @Override
        public Void call() throws Exception {
            byte data[] = new byte[buffSize];
            initRecognition();
            while (microphone.getState() == Microphone.State.BUSY) {
                int bytesRead = microphone.getTargetDataLine().read(data, 0, buffSize);
                if (bytesRead > 0) {
                    recognizeData(data, bytesRead);
                } else {
                    log.error("0 bytes readed");
                }
            }
            return null;
        }
    }


    public void stopRecognition() {
        requestObserver.onCompleted();
        microphone.stopRecording();
    }

    public void startRecognition() {
        microphone.startRecording();
        executorService.submit(new RecognitionTask());
    }

    private static void recognizeData(byte[] data, int size) {
        log.info("sending recognition request");
        requestObserver.onNext(StreamingRecognizeRequest.newBuilder().setAudioContent(ByteString.copyFrom(data, 0, size)).build());
    }


    private static void initRecognition() throws IOException {

        SpeechSettings speechSettings = SpeechSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(loadCredentialsFromFile()))
                .build();
        SpeechClient speech = SpeechClient.create(speechSettings);


        RecognitionConfig recognitionConfig = RecognitionConfig.newBuilder()
                .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                .setLanguageCode("ru-RU")
                .setSampleRateHertz(16000)
                .build();

        StreamingRecognitionConfig streamingRecognitionConfig = StreamingRecognitionConfig.newBuilder()
                .setConfig(recognitionConfig).setInterimResults(false)
                .build();

        responseObserver = new ResponseApiStreamingObserver();
        BidiStreamingCallable<StreamingRecognizeRequest, StreamingRecognizeResponse> callable = speech.streamingRecognizeCallable();
        requestObserver = callable.bidiStreamingCall(responseObserver);


        //init request
        log.info("sending INIT recognition request");
        requestObserver.onNext(StreamingRecognizeRequest.newBuilder().setStreamingConfig(streamingRecognitionConfig).build());
    }

    static class ResponseApiStreamingObserver implements ApiStreamObserver<StreamingRecognizeResponse> {
        @Override
        public void onNext(StreamingRecognizeResponse message) {
            if (message.getError().getCode() == 11) {
                requestObserver.onCompleted();
                executorService.submit(new RecognitionTask());
            }

            log.info("==== raw resp: {}", message.toString());
            log.info("==== msg: {}", message.getResultsList().get(0).getAlternatives(0).getTranscript());
        }

        @Override
        public void onError(Throwable t) {
        }

        @Override
        public void onCompleted() {
            if (responseObserver != null) responseObserver = null;
            if (requestObserver != null) requestObserver = null;
        }

    }
}
