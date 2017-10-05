package com.romanvoloboev.controller;

import com.romanvoloboev.service.MainViewService;
import de.felixroske.jfxsupport.FXMLController;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created at 05.10.17
 *
 * @author romanvoloboev
 */
@FXMLController
public class MainViewController {

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

    public void doStartAction(Event event) {
        String text = mainViewService.getSomeText();
        textArea.setText(text);
    }
}
