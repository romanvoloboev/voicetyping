package com.romanvoloboev.utils;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.rpc.ApiStreamObserver;
import com.google.api.gax.rpc.BidiStreamingCallable;
import com.google.auth.Credentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.speech.v1.*;
import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.ByteString;
import javafx.scene.control.TextArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ResourceUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created at 11.10.17
 *
 * @author romanvoloboev
 */
public class GoogleSpeechRecognizeService {
    private static final Logger log = LoggerFactory.getLogger(GoogleSpeechRecognizeService.class);
    private final List<StreamingRecognizeResponse> messages = new ArrayList<>();
    private ApiStreamObserver<StreamingRecognizeRequest> requestObserver;
    private final Microphone microphone;
    private int buffSize;


    private Credentials loadCredentialsFromFile() throws IOException {
        String credentialsFile = "classpath:credentials.json";
        log.info("Loading credentials from specified file: {}", credentialsFile);
        try (FileInputStream fileInputStream = new FileInputStream(ResourceUtils.getFile(credentialsFile))) {
            Credentials credentials = ServiceAccountCredentials.fromStream(fileInputStream);
            log.info("Successfully loaded from file.");
            return credentials;
        }
    }

    public GoogleSpeechRecognizeService(Microphone microphone) {
        this.microphone = microphone;
        buffSize = microphone.getTargetDataLine().getBufferSize();
    }


    public void startRecognition(TextArea textArea) {
        microphone.startRecording();
        initRecognition();
        new Thread(new Runnable() {
            byte data[] = new byte[buffSize];
            @Override
            public void run() {
                while (microphone.getState() == Microphone.State.BUSY) {
                    int bytesRead = microphone.getTargetDataLine().read(data, 0, buffSize);
                    if (bytesRead > 0) {
                        recognizeData(data, bytesRead);
                    } else {
                        System.out.println("error reading bytes");
                    }
                }
            }
        }).start();
    }

    public void stopRecognition() {
        requestObserver.onCompleted();
        microphone.stopRecording();
    }

    private void recognizeData(byte[] data, int size) {
        requestObserver.onNext(StreamingRecognizeRequest.newBuilder().setAudioContent(ByteString.copyFrom(data, 0, size)).build());
    }

    private void initRecognition() {
        try {
            SpeechSettings speechSettings = SpeechSettings.newBuilder()
                            .setCredentialsProvider(FixedCredentialsProvider.create(loadCredentialsFromFile()))
                            .build();
            SpeechClient speech = SpeechClient.create(speechSettings);


            RecognitionConfig recognitionConfig = RecognitionConfig.newBuilder()
                    .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                    .setLanguageCode("en-US")
                    .setSampleRateHertz(16000)
                    .build();

            StreamingRecognitionConfig streamingRecognitionConfig = StreamingRecognitionConfig.newBuilder()
                    .setConfig(recognitionConfig)
                    .build();

            ResponseApiStreamingObserver<StreamingRecognizeResponse> responseObserver = new ResponseApiStreamingObserver<>();
            BidiStreamingCallable<StreamingRecognizeRequest,StreamingRecognizeResponse> callable = speech.streamingRecognizeCallable();
            requestObserver = callable.bidiStreamingCall(responseObserver);

            //init request
            requestObserver.onNext(StreamingRecognizeRequest.newBuilder().setStreamingConfig(streamingRecognitionConfig).build());

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    class ResponseApiStreamingObserver<T> implements ApiStreamObserver<T> {
        private final SettableFuture<List<T>> future = SettableFuture.create();

        @Override
        public void onNext(T message) {
            System.out.println("onNext response: "+message.toString());
            messages.add((StreamingRecognizeResponse) message);
        }

        @Override
        public void onError(Throwable t) {
            future.setException(t);
        }

        @Override
        public void onCompleted() {
            System.out.println("onCompleted.");
            future.set((List<T>) messages);
        }

        // Returns the SettableFuture object to get received messages / exceptions.
        public SettableFuture<List<T>> getFuture() {
            return future;
        }
    }

}
