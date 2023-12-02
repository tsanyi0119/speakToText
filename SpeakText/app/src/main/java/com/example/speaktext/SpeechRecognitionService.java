package com.example.speaktext;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;

public class SpeechRecognitionService extends Service {
    private SpeechRecognizer speechRecognizer;

    @Override
    public void onCreate() {
        super.onCreate();
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                // 語音辨識準備好了
            }

            @Override
            public void onBeginningOfSpeech() {
                // 開始語音輸入
            }

            @Override
            public void onRmsChanged(float rmsdB) {
                // 語音音量變化
            }

            @Override
            public void onBufferReceived(byte[] bytes) {

            }

            @Override
            public void onEndOfSpeech() {

            }

            // 其他語音辨識回調方法，根據您的需求處理它們

            @Override
            public void onError(int error) {
                // 語音辨識出錯
            }

            @Override
            public void onResults(Bundle results) {
                // 獲取語音辨識結果
                ArrayList<String> resultList = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

                // 創建一個Intent並將結果放入
                Intent broadcastIntent = new Intent("SpeechRecognitionResults");
                broadcastIntent.putStringArrayListExtra("results", resultList);

                // 發送廣播給MainActivity
                sendBroadcast(broadcastIntent);
                Log.d("SpeechRecognitionService", "onResults: Result is not null");

            }

            @Override
            public void onPartialResults(Bundle bundle) {
                ArrayList<String> partialResults = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (partialResults != null && partialResults.size() > 0) {
                    String partialResult = partialResults.get(0); // 获取逐字辨识的部分结果

                    // 创建一个Intent并将部分结果放入其中
                    Intent broadcastIntent = new Intent("PartialSpeechRecognitionResult");
                    broadcastIntent.putExtra("partialResult", partialResult);

                    // 发送广播给MainActivity
                    sendBroadcast(broadcastIntent);
                }
            }

            @Override
            public void onEvent(int i, Bundle bundle) {

            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 開始語音辨識
        if (speechRecognizer != null) {
            Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
//            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US"); // 設定語言為英文（美國）
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            // 語音辨識的完整靜默時間，以毫秒為單位
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000);
            // 辨識的最小時間，以毫秒為單位
//            recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 2000);

            speechRecognizer.startListening(recognizerIntent);
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 停止語音辨識
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }
}

