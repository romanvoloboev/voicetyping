package com.romanvoloboev.utils;

import com.google.api.gax.rpc.ApiStreamObserver;
import com.google.api.gax.rpc.BidiStreamingCallable;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1.*;
import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.auth.ClientAuthInterceptor;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * Created at 11.10.17
 *
 * @author romanvoloboev
 */
public class GoogleSpeechRecognizeService implements ApiStreamObserver<StreamingRecognizeResponse> {
    private final SettableFuture<List<StreamingRecognizeResponse>> future = SettableFuture.create();
    private final List<StreamingRecognizeResponse> messages = new ArrayList<>();
    private ApiStreamObserver<StreamingRecognizeRequest> requestObserver;
    private final Microphone microphone;
    private int buffSize;

    public GoogleSpeechRecognizeService(Microphone microphone) {
        this.microphone = microphone;
        buffSize = microphone.getTargetDataLine().getBufferSize();
        System.setProperty("GOOGLE_APPLICATION_CREDENTIALS", "classpath:resources/speech-cloud-api.json");
    }


    public void startRecognition() {
        microphone.startRecording();

        InputStream credentials = ClassLoader.getSystemResourceAsStream("speech-cloud-api.json");
        try {
            ManagedChannel managedChannel = createChannel("speech.googleapis.com", 443, credentials);
        } catch (IOException e) {
            e.printStackTrace();
        }


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
        initRecognition();
        StreamingRecognizeRequest request = StreamingRecognizeRequest.newBuilder().setAudioContent(ByteString.copyFrom(data, 0, size)).build();
        requestObserver.onNext(request);
    }

    private void initRecognition() {
        try {
            SpeechClient speech = SpeechClient.create();

            BidiStreamingCallable<StreamingRecognizeRequest,StreamingRecognizeResponse> callable = speech.streamingRecognizeCallable();

            requestObserver = callable.bidiStreamingCall(this);

            RecognitionConfig recognitionConfig = RecognitionConfig.newBuilder()
                    .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                    .setLanguageCode("en-US")
                    .setSampleRateHertz(16000)
                    .build();

            StreamingRecognitionConfig streamingRecognitionConfig = StreamingRecognitionConfig.newBuilder()
                    .setConfig(recognitionConfig)
                    .setInterimResults(true)
                    .setSingleUtterance(true)
                    .build();

            StreamingRecognizeRequest initRequest = StreamingRecognizeRequest.newBuilder().setStreamingConfig(streamingRecognitionConfig).build();
            requestObserver.onNext(initRequest);

        } catch (IOException e) {
            e.printStackTrace();
        }

        //List<StreamingRecognizeResponse> responses = this.getFuture().get();
    }

    @Override
    public void onNext(StreamingRecognizeResponse streamingRecognizeResponse) {
        System.out.println("response: "+streamingRecognizeResponse.toString());
        messages.add(streamingRecognizeResponse);
    }

    @Override
    public void onError(Throwable throwable) {
        future.setException(throwable);
    }

    @Override
    public void onCompleted() {
        System.out.println("recognize completed.");
        future.set(messages);
    }

    private static ManagedChannel createChannel(String host, int port, InputStream credentials)
            throws IOException {
        GoogleCredentials creds = GoogleCredentials.fromStream(credentials);
        creds = creds.createScoped(OAUTH2_SCOPES);
        OkHttpChannelProvider provider = new OkHttpChannelProvider();
        OkHttpChannelBuilder builder = provider.builderForAddress(host, port);
        ManagedChannel channel =  builder.intercept(new ClientAuthInterceptor(creds, Executors
                .newSingleThreadExecutor
                        ()))
                .build();

        credentials.close();
        return channel;
    }

    public SettableFuture<List<StreamingRecognizeResponse>> getFuture() {
        return future;
    }
}
