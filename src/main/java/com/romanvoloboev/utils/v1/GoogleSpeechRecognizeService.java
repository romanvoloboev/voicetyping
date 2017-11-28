package com.romanvoloboev.utils.v1;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.rpc.ApiStreamObserver;
import com.google.api.gax.rpc.BidiStreamingCallable;
import com.google.auth.Credentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.speech.v1.*;
import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.*;

/**
 * Created at 11.10.17
 *
 * @author romanvoloboev
 */

@Service
public class GoogleSpeechRecognizeService {
    private static final Logger log = LoggerFactory.getLogger(GoogleSpeechRecognizeService.class);
    private static Microphone microphone;
    private static int buffSize;
    private static ExecutorService executorService = Executors.newSingleThreadExecutor();
    private static ApiStreamObserver<StreamingRecognizeRequest> requestObserver;
    private static ResponseApiStreamingObserver responseObserver;


    private static Credentials loadCredentialsFromFile() throws IOException {
        String credentialsFile = "classpath:credentials.json";
        log.info("Loading credentials from specified file: {}", credentialsFile);
        try (FileInputStream fileInputStream = new FileInputStream(ResourceUtils.getFile(credentialsFile))) {
            Credentials credentials = ServiceAccountCredentials.fromStream(fileInputStream);
            log.info("Successfully loaded from file.");
            return credentials;
        }
    }

    public GoogleSpeechRecognizeService(Microphone mic) throws InterruptedException, ExecutionException, TimeoutException {
        microphone = mic;
        buffSize = microphone.getTargetDataLine().getBufferSize();
        microphone.startRecording();

        executorService.submit(new RecognitionTask());

    }

    private static class RecognitionTask implements Callable<Void> {
        @Override
        public Void call() throws Exception {
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


    public void stopRecognition() {
        requestObserver.onCompleted();
        microphone.stopRecording();
    }

    private static void recognizeData(byte[] data, int size) {
        log.info("sending recognition request");
        requestObserver.onNext(StreamingRecognizeRequest.newBuilder().setAudioContent(ByteString.copyFrom(data, 0, size)).build());
    }


    private static void initRecognition() throws IOException {

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
        log.info("sending INIT recognition request");
        requestObserver.onNext(StreamingRecognizeRequest.newBuilder().setStreamingConfig(streamingRecognitionConfig).build());
    }

    static class ResponseApiStreamingObserver implements ApiStreamObserver<StreamingRecognizeResponse> {
        @Override
        public void onNext(StreamingRecognizeResponse message) {
            if (message.getError().getCode() == 11) {
                requestObserver.onCompleted();
                executorService.submit(new RecognitionTask());
            }

            log.info("==== raw resp: {}", message.toString());
            log.info("==== msg: {}", message.getResultsList().get(0).getAlternatives(0).getTranscript());
        }

        @Override
        public void onError(Throwable t) {
        }

        @Override
        public void onCompleted() {
            if (responseObserver != null) responseObserver = null;
            if (requestObserver != null) requestObserver = null;
        }

    }

}
