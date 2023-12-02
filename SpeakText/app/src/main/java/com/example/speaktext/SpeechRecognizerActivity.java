package com.example.speaktext;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

public class SpeechRecognizerActivity extends AppCompatActivity implements RecognitionListener {

    private static final int REQUEST_CODE_PERMISSION = 1;
    private SpeechRecognizer speechRecognizer;
    private TextView resultTextView;
    private Button startRecognitionButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speech_recognizer);

        resultTextView = findViewById(R.id.resultTextView);
        startRecognitionButton = findViewById(R.id.startRecognitionButton);

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(this);

        startRecognitionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startSpeechRecognition();
            }
        });
    }

    private void startSpeechRecognition() {
        resultTextView.setText("辨識中...");
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
//            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

            // 將辨識語言設定為英文
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH);
            // 設定語音識別的超時時間（毫秒）
//            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000); // 3秒
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000); // 3秒
//            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000); // 1秒

            speechRecognizer.startListening(intent);
        } else {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_CODE_PERMISSION);
        }
    }

    @Override
    public void onReadyForSpeech(Bundle bundle) {
        // 語音辨識已經準備好，可以開始說話
    }

    @Override
    public void onBeginningOfSpeech() {
        // 開始說話
    }

    @Override
    public void onRmsChanged(float v) {
        // 聲音強度變化
    }

    @Override
    public void onBufferReceived(byte[] bytes) {
        // 聲音緩衝區接收到數據
    }

    @Override
    public void onEndOfSpeech() {
        // 語音輸入結束
    }

    @Override
    public void onError(int errorCode) {
        // 語音辨識錯誤處理
        Toast.makeText(this, "語音辨識錯誤，代碼：" + errorCode, Toast.LENGTH_SHORT).show();
        resultTextView.setText("辨識錯誤");
    }

    @Override
    public void onResults(Bundle results) {
        ArrayList<String> voiceResults = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (voiceResults != null && !voiceResults.isEmpty()) {
            String recognizedText = voiceResults.get(0);
            resultTextView.setText(recognizedText);
        }
    }

    @Override
    public void onPartialResults(Bundle bundle) {
        // 部分語音辨識結果
    }

    @Override
    public void onEvent(int i, Bundle bundle) {
        // 語音辨識事件
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startSpeechRecognition();
            } else {
                Toast.makeText(this, "未授予麥克風權限，無法進行語音辨識", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
