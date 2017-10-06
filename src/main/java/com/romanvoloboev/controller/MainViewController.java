package com.romanvoloboev.controller;

import com.romanvoloboev.service.MainViewService;
import de.felixroske.jfxsupport.FXMLController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sound.sampled.LineUnavailableException;
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

    @Autowired
    public MainViewController(MainViewService mainViewService) {
        this.mainViewService = mainViewService;
    }

    @FXML
    private TextArea textArea;

    @FXML
    private Button startBtn;

    @FXML
    private Button stopBtn;

    @FXML
    private Button saveBtn;

    @FXML
    private Button clearBtn;

    @FXML
    private Button exitBtn;

    public void doStartRecord(ActionEvent event) {

        log.info("start!!!");
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    mainViewService.startRecord(textArea);
                } catch (LineUnavailableException | InterruptedException e) {
                    log.error("error: ", e);
                }
            }
        };

        Thread t = new Thread(runnable);
        t.setDaemon(true);
        t.start();

        startBtn.setDisable(true);
        stopBtn.setDisable(false);
    }

    public void doStopRecord(ActionEvent actionEvent) {
        log.info("stop!!");
        mainViewService.stopRecord();
        startBtn.setDisable(false);
        stopBtn.setDisable(true);
    }
}
