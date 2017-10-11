package com.romanvoloboev.service;

import com.darkprograms.speech.microphone.Microphone;
import com.darkprograms.speech.recognizer.GSpeechDuplex;
import com.darkprograms.speech.recognizer.GSpeechResponseListener;
import com.darkprograms.speech.recognizer.GoogleResponse;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.SpeechClient;
import com.romanvoloboev.controller.Trie;
import com.romanvoloboev.controller.TrieMap;
import com.romanvoloboev.utils.GoogleSpeechRecognizeService;
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

        Trie<String> stringTrie = new Trie<>();
        stringTrie.add("Hello");
        stringTrie.add("Hell");
        stringTrie.add("Help");
        stringTrie.add("мама мыла раму");
        stringTrie.add("мама мыла ирму");
        stringTrie.add("мама любила раму");

        log.info(stringTrie.toString());
        log.info("{}", stringTrie.contains("Hell"));


        TrieMap<String, String> stringTrieMap = new TrieMap<>();
        stringTrieMap.put("Hello", "---hello");
        stringTrieMap.put("Hell", "---hell");
        stringTrieMap.put("Help", "---help");

        log.info(stringTrieMap.toString());

        log.info("{}", stringTrieMap.get("Hell"));

        GoogleSpeechRecognizeService googleSpeechRecognizeService = new GoogleSpeechRecognizeService(new com.romanvoloboev.utils.Microphone());
        googleSpeechRecognizeService.startRecognition();
        googleSpeechRecognizeService.stopRecognition();



//        log.info("--- Starting Speech Recognition, Microphone State is: {}",  microphone.getState());
//        log.info("--- AudioFormat: {}", microphone.getAudioFormat());
//
//        duplex.recognize(microphone.getTargetDataLine(), microphone.getAudioFormat());
//
//        duplex.addResponseListener(new GSpeechResponseListener() {
//            String old_text = "";
//
//            public void onResponse(GoogleResponse gr) {
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
//            }
//        });

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
