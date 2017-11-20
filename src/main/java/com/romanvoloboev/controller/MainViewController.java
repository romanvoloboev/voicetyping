package com.romanvoloboev.controller;

import com.romanvoloboev.service.MainViewService;
import de.felixroske.jfxsupport.FXMLController;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sound.sampled.LineUnavailableException;
import java.io.File;
import java.util.concurrent.Callable;

/**
 * Created at 05.10.17
 *
 * @author romanvoloboev
 */
@FXMLController
public class MainViewController {
    private static final Logger log = LoggerFactory.getLogger(MainViewController.class);
    private final MainViewService mainViewService;
//    public MenuItem open;
//    public MenuItem save;
//    public MenuItem exit;
//    public MenuItem analyze;
//    public MenuItem format;
//    public MenuItem edit;
//    public MenuItem about;


    @Autowired
    public MainViewController(MainViewService mainViewService) {
        this.mainViewService = mainViewService;
    }

    @FXML
    private TextArea textArea;

    public void openAction(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("OPen file");
        File file = fileChooser.showOpenDialog(new Stage());
        textArea.setText(file.getName());
    }

    public void exitAction(ActionEvent actionEvent) {
        Platform.exit();
        System.exit(0);
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
