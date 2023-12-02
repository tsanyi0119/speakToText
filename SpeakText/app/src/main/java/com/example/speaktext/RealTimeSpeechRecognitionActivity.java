package com.example.speaktext;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.speaktext.R;

public class RealTimeSpeechRecognitionActivity extends AppCompatActivity {

    private static final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);

    private AudioRecord audioRecord;
    private SpeechRecognizer speechRecognizer;
    private boolean isRecording = false;
    private TextView recognitionResultTextView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_real_time_speech_recognition);

        recognitionResultTextView = findViewById(R.id.recognitionResultTextView);

        // 檢查麥克風權限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
        }

        // 初始化AudioRecord
        audioRecord = new AudioRecord(AUDIO_SOURCE, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE);

        // 初始化語音辨識器
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                // 語音辨識準備就緒
            }

            @Override
            public void onBeginningOfSpeech() {
                // 開始說話
            }

            @Override
            public void onRmsChanged(float rmsdB) {
                // 聲音強度變化
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
                // 聲音緩衝區接收到數據
                // 此處不需要呼叫辨識方法
            }

            @Override
            public void onEndOfSpeech() {
                // 語音輸入結束
            }

            @Override
            public void onError(int error) {
                // 語音辨識錯誤處理
                String errorMessage = "語音辨識錯誤，代碼：" + error;
                Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResults(Bundle results) {
                // 語音辨識結果
                if (results != null) {
                    // 處理辨識結果
                    processRecognitionResults(results);
                }
            }

            @Override
            public void onPartialResults(Bundle bundle) {

            }

            @Override
            public void onEvent(int i, Bundle bundle) {

            }

            // 其他語音辨識回調方法
        });
    }

    private void processRecognitionResults(Bundle results) {
        if (results != null) {
            // 獲取語音辨識結果
            String recognizedText = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).get(0);
            recognitionResultTextView.setText(recognizedText);
        }
    }

    private void startRecording() {
        if (!isRecording) {
            audioRecord.startRecording();
            isRecording = true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    while (isRecording) {
                        int bytesRead = audioRecord.read(buffer, 0, BUFFER_SIZE);
                        if (bytesRead > 0) {
                            // 將音頻數據傳遞給語音辨識器
                            speechRecognizer.startListening(RecognizerIntent.getVoiceDetailsIntent(getApplicationContext()));
//                            speechRecognizer.cancel();
                            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                                @Override
                                public void onReadyForSpeech(Bundle params) {
                                    // 語音辨識準備就緒
                                }

                                @Override
                                public void onBeginningOfSpeech() {
                                    // 開始說話
                                }

                                @Override
                                public void onRmsChanged(float rmsdB) {
                                    // 聲音強度變化
                                }

                                @Override
                                public void onBufferReceived(byte[] buffer) {
                                    // 聲音緩衝區接收到數據
                                }

                                @Override
                                public void onEndOfSpeech() {
                                    // 語音輸入結束
                                }

                                @Override
                                public void onError(int error) {
                                    // 語音辨識錯誤處理
                                    String errorMessage = "語音辨識錯誤，代碼：" + error;
                                    Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onResults(Bundle results) {
                                    // 語音辨識結果
                                    if (results != null) {
                                        // 處理辨識結果
                                        processRecognitionResults(results);
                                    }
                                }

                                @Override
                                public void onPartialResults(Bundle partialResults) {
                                    // 部分語音辨識結果
                                }

                                @Override
                                public void onEvent(int eventType, Bundle params) {
                                    // 語音辨識事件
                                }
                            });
                        }
                    }
                    audioRecord.stop();
                    audioRecord.release();
                }
            }).start();
        }
    }

    private void stopRecording() {
        isRecording = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        startRecording();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopRecording();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRecording();
        speechRecognizer.destroy();
    }
}
