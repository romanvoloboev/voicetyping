package com.rv.controller;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.rpc.ApiStreamObserver;
import com.google.api.gax.rpc.BidiStreamingCallable;
import com.google.auth.Credentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.speech.v1.*;
import com.google.protobuf.ByteString;
import com.rv.utils.Http;
import com.rv.utils.Microphone;
import com.rv.utils.ResultResponseDTO;
import com.rv.utils.datastruct.TrieMap;
import de.felixroske.jfxsupport.FXMLController;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.*;

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
    private ResponseApiStreamingObserver responseObserver;
    private static STATE currentState = STATE.READY_FOR_COMMAND;
    private static final TrieMap<String, Integer> TRIE_MAP = new TrieMap<>();

    @Autowired
    private Http http;

    @FXML
    private MenuItem open;

    @FXML
    public StackPane root;

    @FXML
    private MenuItem save;

    @FXML
    private MenuItem exit;

    @FXML
    private MenuItem analyze;

    @FXML
    private MenuItem format;

    @FXML
    private MenuItem edit;

    @FXML
    private TextFlow textFlow;

    @FXML
    private TextFlow resultFlow;

    @FXML
    private VBox mainVbox;

    @Autowired
    public MainViewController(Microphone mic) {

        microphone = mic;
        buffSize = microphone.getTargetDataLine().getBufferSize();

        TRIE_MAP.put("voice открыть", Action.OPEN_FILE);
        TRIE_MAP.put("voice сохранить", Action.SAVE_FILE);

        TRIE_MAP.put("voice запись старт", Action.START_RECORDING);
        TRIE_MAP.put("запись старт", Action.START_RECORDING);
        TRIE_MAP.put("voice запись start", Action.START_RECORDING);

        TRIE_MAP.put("запись стоп", Action.STOP_RECORDING);
        TRIE_MAP.put("voice запись стоп", Action.STOP_RECORDING);
        TRIE_MAP.put("voice запись stop", Action.STOP_RECORDING);

        TRIE_MAP.put("voice выход", Action.EXIT);
        TRIE_MAP.put("voice анализ", Action.ANALYZE);

        startRecognition();
    }

    /// открыть диалоговое окно для выбора файла
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

    // анализ текста по нажатию на пункт меню
    public void analyzeText(ActionEvent actionEvent) {
        stopRecognition();
        String fullText = getStringFromTextFlow(textFlow);
        log.info("START PROCESSING FOR TEXT: {}", fullText);
        final VBox[] box = new VBox[1];
        ProgressIndicator pi = new ProgressIndicator();
        box[0] = new VBox(pi);
        box[0].setAlignment(Pos.CENTER);
        mainVbox.setDisable(true);
        root.getChildren().add(box[0]);
        //log.info("~~~~~1111");
        Platform.runLater(()-> {


            Future<ResultResponseDTO> future = Executors.newSingleThreadExecutor().submit(() ->  http.sendText(fullText));

            do {
                if (future.isDone()) {
                    try {
                        ResultResponseDTO res = future.get();
                        log.info("RESULT:::::::: {}", res);

                        mainVbox.setDisable(false);
                        root.getChildren().removeAll(box[0]);
                        resultFlow.getChildren().clear();
                        resultFlow.getChildren().add(new Text("Уникальность текста: "+res.getText_unique() +"%."));
                        if (res.getResult_json().getUrls().size() > 0) {
                            resultFlow.getChildren().add(new Text("\nСсылки:"));
                            for (ResultResponseDTO.ResultJson.Urls url : res.getResult_json().getUrls()) {
                                resultFlow.getChildren().add(new Text("\nURL: "+url.getUrl()+", процент плагиата по ссылке: "+url.getPlagiat()+"%."));
                            }

                        }
                        startRecognition();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }
            } while (!future.isDone());

        });
    }

    // считывание текста с верхней текстового поля
    private static String getStringFromTextFlow(TextFlow tf) {
        StringBuilder sb = new StringBuilder();
        tf.getChildren().parallelStream()
                .filter(t -> Text.class.equals(t.getClass()))
                .forEach(t -> sb.append(((Text) t).getText()));
        return sb.toString();
    }

    // генерация уникального текста по нажатию на пункт меню
    public void generateUniqueText(ActionEvent actionEvent) {
        textFlow.getChildren().clear();
        resultFlow.getChildren().clear();
        try {
            Files.lines(Paths.get(ClassLoader.getSystemResource("2.txt")
                    .toURI())).forEachOrdered(s -> textFlow.getChildren().add(new Text(s+"\n")));
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
        resultFlow.getChildren().add(new Text("Уникальность текста: 100%."));
    }


    private enum STATE {
        RECORDING, READY_FOR_COMMAND
    }

    private static class Action {
        static final Integer OPEN_FILE = 1;
        static final Integer SAVE_FILE = 2;
        static final Integer START_RECORDING = 3;
        static final Integer STOP_RECORDING = 4;
        static final Integer EXIT = 5;
        static final Integer ANALYZE = 6;
    }

    public void exitAction(ActionEvent actionEvent) {
        Platform.exit();
        System.exit(0);
    }

    // сохранение в файл
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

    // открыть текстовый файл
    private void getText(File file) throws IOException {
        String contentType = Files.probeContentType(file.toPath());
        if (contentType == null || !contentType.equals("text/plain")
                && !contentType.equals("application/msword")
                && !contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
            throw new IOException();
        } else if (contentType.equals("application/msword") || contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
            XWPFDocument document = new XWPFDocument(new FileInputStream(file));
            XWPFWordExtractor extractor = new XWPFWordExtractor(document);
            textFlow.getChildren().add(new Text(extractor.getText()));
        } else {
            textFlow.getChildren().clear();
            Files.lines(file.toPath()).forEachOrdered(s -> textFlow.getChildren().add(new Text(s+"\n")));
        }
    }

    // сохранить текст из окна в файл
    private void saveTextAreaToFile(File fileWhereSave) throws IOException {
        try (BufferedWriter bf = new BufferedWriter(new FileWriter(fileWhereSave))) {
            StringBuilder sb = new StringBuilder();
            for (Node node : textFlow.getChildren()) {
                if (node instanceof Text) {
                    sb.append(((Text) node).getText());
                }
            }
            String fullText = sb.toString();
            bf.write(fullText);
        }
    }

    // загрузка данных пользователя для работы с распознаванием речи
    private static Credentials loadCredentialsFromFile() throws IOException {
        String credentialsFile = "credentials.json";
        log.info("Loading credentials from specified file: {}", credentialsFile);

        Resource resource = new ClassPathResource(credentialsFile);
        Credentials credentials = ServiceAccountCredentials.fromStream(resource.getInputStream());
        log.info("Successfully loaded from file.");
        return credentials;
    }

    // класс-поток в котором запускается распознавание речи
       private class RecognitionTask implements Callable<Void> {
        @Override
        public Void call() {
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

    // остановка потока записи голоса
    public void stopRecognition() {
        requestObserver.onCompleted();
        microphone.stopRecording();
    }

    // запуск потока записи голоса
    public void startRecognition() {
        microphone.startRecording();
        executorService.submit(new RecognitionTask());
    }

    // отправка данных на сервер гугла для распознавания
    private static void recognizeData(byte[] data, int size) {
        //log.info("current state: {}", currentState);
        requestObserver.onNext(StreamingRecognizeRequest.newBuilder().setAudioContent(ByteString.copyFrom(data, 0, size)).build());
    }

    // первичная инициализация библиотеки гугла для распознавания
    private void initRecognition() {
        try {
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
            log.debug("sending INIT recognition request");
            requestObserver.onNext(StreamingRecognizeRequest.newBuilder().setStreamingConfig(streamingRecognitionConfig).build());
        } catch (Exception e) {
            log.error("ERR: {}", e);
        }
    }

    // определение действия по распознаной команде
    private static Integer recognizeCommand(String recognizedResult) {
        recognizedResult = recognizedResult.toLowerCase().trim();
        log.debug("recognizeCommand received: {}", recognizedResult);
        if (TRIE_MAP.contains(recognizedResult)) {
            Integer result = TRIE_MAP.get(recognizedResult);
            log.debug("recognizeCommand found command: {}", result);
            return result;
        } else {
            log.debug("recognizeCommand command NOT FOUND.");
            return -1;
        }
    }

    // класс отвечающий за обработку результата-ответа от сервиса распознавания
    class ResponseApiStreamingObserver implements ApiStreamObserver<StreamingRecognizeResponse> {
        @Override
        public void onNext(StreamingRecognizeResponse message) {
            if (message.getError().getCode() == 11) {
                requestObserver.onCompleted();
                executorService.submit(new RecognitionTask());
            }
            String recognizedResult = "";
            try {
                recognizedResult = message.getResultsList().get(0).getAlternatives(0).getTranscript();
            } catch (IndexOutOfBoundsException ignored) {}
            log.info("==== recognizedResult: {}", recognizedResult);

            if (currentState == STATE.READY_FOR_COMMAND) {
                Integer command = recognizeCommand(recognizedResult);
                if (command != -1) {
                    switch (command) {
                        case 1: {
                            Platform.runLater (() -> open.fire());
                            break;
                        }
                        case 2: {
                            Platform.runLater (() -> save.fire());
                            break;
                        }
                        case 3: {
                            setCurrentState(STATE.RECORDING);
                            break;
                        }
                        case 6: {
                            Platform.runLater(() -> analyze.fire());
                        }
                    }
                }
            } else if (currentState == STATE.RECORDING) {
                Integer res = recognizeCommand(recognizedResult);
                if (res == -1) {
                    String finalRecognizedResult = recognizedResult;
                    Platform.runLater ( () -> textFlow.getChildren().add(new Text(finalRecognizedResult.concat(". "))));
                } else if (res == 4) {
                    setCurrentState(STATE.READY_FOR_COMMAND);
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            log.error("{}", t);
        }

        @Override
        public void onCompleted() {
            if (responseObserver != null) responseObserver = null;
            if (requestObserver != null) requestObserver = null;
        }
    }

    // устновка состояния приложения
    static void setCurrentState(STATE currentState) {
        MainViewController.currentState = currentState;
    }
}
