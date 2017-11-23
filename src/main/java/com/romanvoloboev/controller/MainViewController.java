package com.romanvoloboev.controller;

import com.romanvoloboev.service.MainViewService;
import de.felixroske.jfxsupport.FXMLController;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;

/**
 * Created at 05.10.17
 *
 * @author romanvoloboev
 */
@FXMLController
public class MainViewController {
    private static final Logger log = LoggerFactory.getLogger(MainViewController.class);
    private final MainViewService mainViewService;

    @Autowired
    public MainViewController(MainViewService mainViewService) {
        this.mainViewService = mainViewService;
    }

    @FXML
    private TextArea textArea;

    public void openAction(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите файл");
        FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter("Текстовые файлы", "*.txt", "*.doc", "*.docx");
        fileChooser.getExtensionFilters().add(extensionFilter);
        File file = fileChooser.showOpenDialog(new Stage());
        if (file != null) {
            try {
                mainViewService.getText(file, textArea);
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
                mainViewService.saveTextAreaToFile(fileWhereSave, textArea);
            } catch (IOException e) {
                new Alert(Alert.AlertType.ERROR, "Не удалось сохранить файл").showAndWait();
            }
        }

    }



//    public void doStartRecord(ActionEvent event) {
//
//        log.info("start!!!");
//        Runnable runnable = new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    mainViewService.startRecord(textArea);
//                } catch (LineUnavailableException | InterruptedException e) {
//                    log.error("error: ", e);
//                }
//            }
//        };
//
//        Thread t = new Thread(runnable);
//        t.setDaemon(true);
//        t.start();
//
//        startBtn.setDisable(true);
//        stopBtn.setDisable(false);
//    }
//
//    public void doStopRecord(ActionEvent actionEvent) {
//        log.info("stop!!");
//        mainViewService.stopRecord();
//        startBtn.setDisable(false);
//        stopBtn.setDisable(true);
//    }
}
