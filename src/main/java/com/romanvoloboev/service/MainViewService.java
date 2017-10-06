package com.romanvoloboev.service;

import com.darkprograms.speech.microphone.Microphone;
import com.darkprograms.speech.recognizer.GSpeechDuplex;
import com.darkprograms.speech.recognizer.GSpeechResponseListener;
import com.darkprograms.speech.recognizer.GoogleResponse;
import javafx.scene.control.TextArea;
import net.sourceforge.javaflacencoder.FLACFileWriter;
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
public class MainViewService implements GSpeechResponseListener {
    private static final Logger log = LoggerFactory.getLogger(MainViewService.class);
    private final Microphone microphone;
    private final GSpeechDuplex duplex;
    private final Environment env;
    private final String API_KEY;

    @Autowired
    public MainViewService(Environment env) {
        this.env = env;
        API_KEY = env.getProperty("google_api_key");
        log.info("API KEY: {}", API_KEY);

        microphone = new Microphone(FLACFileWriter.FLAC);
        duplex = new GSpeechDuplex(API_KEY);
        duplex.setLanguage("ru");
    }

    public String getSomeText() {
        return "HELLOO111";
    }

    public void startRecord(TextArea textArea) throws LineUnavailableException, InterruptedException {
        log.info("--- Starting Speech Recognition, Microphone State is: {}",  microphone.getState());
        log.info("--- AudioFormat: {}", microphone.getAudioFormat());

        duplex.recognize(microphone.getTargetDataLine(), microphone.getAudioFormat());

        duplex.addResponseListener(new GSpeechResponseListener() {
            String old_text = "";

            public void onResponse(GoogleResponse gr) {
                log.info("--- onResponse");
                String output = "";
                output = gr.getResponse();
                log.info("--- resp: "+gr.getResponse());
                if (gr.getResponse() == null) {
                    this.old_text = textArea.getText();
                    if (this.old_text.contains("(")) {
                        this.old_text = this.old_text.substring(0, this.old_text.indexOf('('));
                    }
                    log.info("Paragraph Line Added");
                    this.old_text = ( textArea.getText() + "\n" );
                    this.old_text = this.old_text.replace(")", "").replace("( ", "");
                    textArea.setText(this.old_text);
                    return;
                }
                if (output.contains("(")) {
                    output = output.substring(0, output.indexOf('('));
                }
                if (!gr.getOtherPossibleResponses().isEmpty()) {
                    output = output + " (" + (String) gr.getOtherPossibleResponses().get(0) + ")";
                }
                log.info(output);
                textArea.setText("");
                textArea.appendText(this.old_text);
                textArea.appendText(output);
            }
        });

    }


    public void stopRecord() {
        microphone.close();
        duplex.stopSpeechRecognition();
        log.info("Stopping Speech Recognition, Microphone State is: {}",  microphone.getState());
    }

    @Override
    public void onResponse(GoogleResponse googleResponse) {

    }
}
