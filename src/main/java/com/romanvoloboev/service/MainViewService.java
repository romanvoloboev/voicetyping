package com.romanvoloboev.service;



import com.romanvoloboev.utils.v1.GoogleSpeechRecognizeService;
import com.romanvoloboev.utils.v1.Microphone;
import javafx.scene.control.TextArea;
import net.sourceforge.javaflacencoder.FLACFileWriter;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import javax.sound.sampled.LineUnavailableException;
import java.io.*;
import java.nio.file.Files;

/**
 * Created at 05.10.17
 *
 * @author romanvoloboev
 */
@Service
public class MainViewService {
    private static final Logger log = LoggerFactory.getLogger(MainViewService.class);
    private final Microphone microphone;
    private final GoogleSpeechRecognizeService googleSpeechRecognizeService;


    @Autowired
    public MainViewService(GoogleSpeechRecognizeService googleSpeechRecognizeService) {
        microphone = new Microphone();
        this.googleSpeechRecognizeService = googleSpeechRecognizeService;
        //this.googleSpeechRecognizeService.startRecognition();

    }

    @PreDestroy
    private void preDestroy() {
        this.googleSpeechRecognizeService.stopRecognition();
    }


    public void getText(File file, TextArea textArea) throws IOException {
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


    public void saveTextAreaToFile(File fileWhereSave, TextArea textArea) throws IOException {
        try (BufferedWriter bf = new BufferedWriter(new FileWriter(fileWhereSave))) {
            bf.write(textArea.getText());
        }
    }


    public String getSomeText() {
        return "HELLOO111";
    }

    public void startRecord(TextArea textArea) throws LineUnavailableException, InterruptedException {

//        GoogleSpeechRecognizeService googleSpeechRecognizeService = new GoogleSpeechRecognizeService(microphone);
//        googleSpeechRecognizeService.startRecognition(textArea);
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


//    public void stopRecord() {
//        microphone.stopRecording();
//        log.info("Stopping Speech Recognition, Microphone State is: {}",  microphone.getState());
//    }

}
