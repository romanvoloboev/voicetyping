package com.romanvoloboev.service;


import com.romanvoloboev.utils.GoogleSpeechRecognizeService;
import com.romanvoloboev.utils.Microphone;
import javafx.scene.control.TextArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.sound.sampled.LineUnavailableException;

/**
 * Created at 05.10.17
 *
 * @author romanvoloboev
 */
@Service
public class MainViewService {
    private static final Logger log = LoggerFactory.getLogger(MainViewService.class);
    private final Microphone microphone;
//    private final GSpeechDuplex duplex;
    private final Environment env;

    @Autowired
    public MainViewService(Environment env) {
        this.env = env;

        microphone = new Microphone();
    }

    public String getSomeText() {
        return "HELLOO111";
    }

    public void startRecord(TextArea textArea) throws LineUnavailableException, InterruptedException {

        GoogleSpeechRecognizeService googleSpeechRecognizeService = new GoogleSpeechRecognizeService(microphone);
        googleSpeechRecognizeService.startRecognition(textArea);
        //googleSpeechRecognizeService.stopRecognition();



//            String old_text = "";
//
//
//                log.info("--- onResponse: {}", gr.toString());
//                String output = "";
//                output = gr.getResponse();
//                log.info("--- resp: "+gr.getResponse());
//                if (gr.getResponse() == null) {
//                    this.old_text = textArea.getText();
//                    if (this.old_text.contains("(")) {
//                        this.old_text = this.old_text.substring(0, this.old_text.indexOf('('));
//                    }
//                    log.info("Paragraph Line Added");
//                    this.old_text = ( textArea.getText() + "\n" );
//                    this.old_text = this.old_text.replace(")", "").replace("( ", "");
//                    textArea.setText(this.old_text);
//                    return;
//                }
//                if (output.contains("(")) {
//                    output = output.substring(0, output.indexOf('('));
//                }
//                if (!gr.getOtherPossibleResponses().isEmpty()) {
//                    output = output + " (" + (String) gr.getOtherPossibleResponses().get(0) + ")";
//                }
//                log.info(output);
//                textArea.setText("");
//                textArea.appendText(this.old_text);
//                textArea.appendText(output);


    }


    public void stopRecord() {
        microphone.stopRecording();
        log.info("Stopping Speech Recognition, Microphone State is: {}",  microphone.getState());
    }

}
