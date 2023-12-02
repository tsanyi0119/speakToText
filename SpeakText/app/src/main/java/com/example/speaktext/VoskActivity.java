package com.example.speaktext;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

import org.json.JSONException;
import org.json.JSONObject;
import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;
import org.vosk.android.SpeechStreamService;
import org.vosk.android.StorageService;

import java.io.IOException;
import java.io.InputStream;

public class VoskActivity extends Activity implements RecognitionListener {

    static private final int STATE_START = 0;
    static private final int STATE_READY = 1;
    static private final int STATE_DONE = 2;
    static private final int STATE_FILE = 3;
    static private final int STATE_MIC = 4;

    /* Used to handle permission request */
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

    private Model model;
    private SpeechService speechService;
    private SpeechStreamService speechStreamService;
    private TextView resultView;

    private String allResult = "";

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_vosk);

        resultView = findViewById(R.id.result_text);
        setUiState(STATE_START);

        findViewById(R.id.recognize_mic).setOnClickListener(view -> recognizeMicrophone());

        LibVosk.setLogLevel(LogLevel.INFO);

        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.RECORD_AUDIO);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
        } else {
            initModel();
        }
    }

    private void initModel() {
        StorageService.unpack(this, "model-en-us", "model",
                (model) -> {
                    this.model = model;
                    setUiState(STATE_READY);
                },
                (exception) -> setErrorState("Failed to unpack the model" + exception.getMessage()));
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Recognizer initialization is a time-consuming and it involves IO,
                // so we execute it in async task
                initModel();
            } else {
                finish();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (speechService != null) {
            speechService.stop();
            speechService.shutdown();
        }

        if (speechStreamService != null) {
            speechStreamService.stop();
        }
    }

    @Override
    public void onResult(String hypothesis) {
//        resultView.append(hypothesis + "\n");
        try {
            // 將 JSON 字串轉換為 JSONObject
            JSONObject jsonObject = new JSONObject(hypothesis);

            // 從 JSONObject 中獲取 "test" 的值
            String testValue = jsonObject.getString("text");

            // 印出擷取到的值
            System.out.println("擷取到的值: " + testValue);
            allResult = allResult + testValue + " ";
            resultView.setText(allResult);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onFinalResult(String hypothesis) {
//        resultView.setText(hypothesis + "\n");
        setUiState(STATE_DONE);
        if (speechStreamService != null) {
            speechStreamService = null;
        }
    }

    @Override
    public void onPartialResult(String hypothesis) {
//        resultView.setText(hypothesis + "\n");
        try {
            // 將 JSON 字串轉換為 JSONObject
            JSONObject jsonObject = new JSONObject(hypothesis);

            // 從 JSONObject 中獲取 "test" 的值
            String testValue = jsonObject.getString("partial");

            // 印出擷取到的值
            System.out.println("擷取到的值: " + testValue);
            resultView.setText(allResult + testValue);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onError(Exception e) {
        setErrorState(e.getMessage());
    }

    @Override
    public void onTimeout() {
        setUiState(STATE_DONE);
    }

    private void setUiState(int state) {
        switch (state) {
            case STATE_START:
                Log.e("TAG", "setUiState: " + 1);
                resultView.setText(R.string.preparing);
                resultView.setMovementMethod(new ScrollingMovementMethod());
                findViewById(R.id.recognize_mic).setEnabled(false);
                break;
            case STATE_READY:
                Log.e("TAG", "setUiState: " + 2);
                resultView.setText(R.string.ready);
                ((Button) findViewById(R.id.recognize_mic)).setText(R.string.recognize_microphone);
                findViewById(R.id.recognize_mic).setEnabled(true);
                break;
            case STATE_DONE:
                Log.e("TAG", "setUiState: " + 3);
                ((Button) findViewById(R.id.recognize_mic)).setText(R.string.recognize_microphone);
                findViewById(R.id.recognize_mic).setEnabled(true);
                break;
            case STATE_FILE:
                Log.e("TAG", "setUiState: " + 4);
                resultView.setText(getString(R.string.starting));
                findViewById(R.id.recognize_mic).setEnabled(false);
                break;
            case STATE_MIC:
                Log.e("TAG", "setUiState: " + 5);
                ((Button) findViewById(R.id.recognize_mic)).setText(R.string.stop_microphone);
                resultView.setText(getString(R.string.say_something));
                findViewById(R.id.recognize_mic).setEnabled(true);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + state);
        }
    }

    private void setErrorState(String message) {
        resultView.setText(message);
        ((Button) findViewById(R.id.recognize_mic)).setText(R.string.recognize_microphone);
        findViewById(R.id.recognize_mic).setEnabled(false);
    }

    private void recognizeMicrophone() {
        if (speechService != null) {
            setUiState(STATE_DONE);
            speechService.stop();
            speechService = null;
        } else {
            resultView.setText("");
            allResult = "";
            setUiState(STATE_MIC);
            try {
                Recognizer rec = new Recognizer(model, 16000.0f);
                speechService = new SpeechService(rec, 16000.0f);
                speechService.startListening(this);
            } catch (IOException e) {
                setErrorState(e.getMessage());
            }
        }
    }
}