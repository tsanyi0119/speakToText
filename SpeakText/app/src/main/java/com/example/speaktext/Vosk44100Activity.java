package com.example.speaktext;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;
import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;
import org.vosk.android.StorageService;

import java.io.IOException;

public class Vosk44100Activity extends Activity implements RecognitionListener {

    private static final int STATE_START = 0;
    private static final int STATE_READY = 1;
    private static final int STATE_DONE = 2;
    private static final int STATE_MIC = 3;

    private static final int DEVICE_STATE_CLOSE = 0;
    private static final int DEVICE_STATE_OPEN = 1;

    private int device_state = DEVICE_STATE_CLOSE;

    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

    private Model model;
    private SpeechService speechService;
    private TextView resultView;
    private String allResult = "";

    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_vosk);

        resultView = findViewById(R.id.result_text);
        setUiState(STATE_START);

        findViewById(R.id.recognize_mic).setOnClickListener(view -> recognizeMicrophone());

        LibVosk.setLogLevel(LogLevel.INFO);

        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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
    }

    @Override
    public void onResult(String hypothesis) {
        handler.post(() -> {
            try {
                JSONObject jsonObject = new JSONObject(hypothesis);
                String testValue = jsonObject.getString("text");
                allResult = allResult + testValue + " ";
                resultView.setText(allResult);
//                if (testValue.equals("i want to turn on the device")){
//                    resultView.setTextColor(Color.parseColor("#FF3333"));
//                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onFinalResult(String hypothesis) {
        handler.post(() -> setUiState(STATE_DONE));
    }

    @Override
    public void onPartialResult(String hypothesis) {
        handler.post(() -> {
            try {
                JSONObject jsonObject = new JSONObject(hypothesis);
                String testValue = jsonObject.getString("partial");
                resultView.setText(allResult + testValue);
                switch (device_state){
                    case DEVICE_STATE_CLOSE:
                        if (testValue.contains("turn on the device")){
                            device_state = DEVICE_STATE_OPEN;
                            resultView.setTextColor(Color.parseColor("#FF3333"));
                        }
                        break;
                    case DEVICE_STATE_OPEN:
                        if (testValue.contains("turn off the device")){
                            device_state = DEVICE_STATE_CLOSE;
                            resultView.setTextColor(Color.parseColor("#000000"));
                        }
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + testValue);

                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onError(Exception e) {
        handler.post(() -> setErrorState(e.getMessage()));
    }

    @Override
    public void onTimeout() {
        handler.post(() -> setUiState(STATE_DONE));
    }

    private void setUiState(int state) {
        handler.post(() -> {
            switch (state) {
                case STATE_START:
                    if (resultView != null) {
                        resultView.setText(R.string.preparing);
                        resultView.setMovementMethod(new ScrollingMovementMethod());
                    }
                    View recognizeMicButtonStart = findViewById(R.id.recognize_mic);
                    if (recognizeMicButtonStart != null) {
                        recognizeMicButtonStart.setEnabled(false);
                    }
                    break;
                case STATE_READY:
                    if (resultView != null) {
                        resultView.setText(R.string.ready);
                    }
                    View recognizeMicButtonReady = findViewById(R.id.recognize_mic);
                    if (recognizeMicButtonReady != null) {
                        recognizeMicButtonReady.setEnabled(true);
                        ((Button) recognizeMicButtonReady).setText(R.string.recognize_microphone);
                    }
                    break;
                case STATE_DONE:
                    View recognizeMicButtonDone = findViewById(R.id.recognize_mic);
                    if (recognizeMicButtonDone != null) {
                        ((Button) recognizeMicButtonDone).setText(R.string.recognize_microphone);
                        recognizeMicButtonDone.setEnabled(true);
                    }
                    break;
                case STATE_MIC:
                    View recognizeMicButtonMic = findViewById(R.id.recognize_mic);
                    if (recognizeMicButtonMic != null) {
                        ((Button) recognizeMicButtonMic).setText(R.string.stop_microphone);
                        if (resultView != null) {
                            resultView.setText(getString(R.string.say_something));
                        }
                        recognizeMicButtonMic.setEnabled(true);
                    }
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + state);
            }
        });
    }



    private void setErrorState(String message) {
        handler.post(() -> {
            resultView.setText(message);
            ((Button) findViewById(R.id.recognize_mic_44100)).setText(R.string.recognize_microphone);
            findViewById(R.id.recognize_mic_44100).setEnabled(false);
        });
    }

    private class RecognitionThread extends Thread {
        @Override
        public void run() {
            try {
                Recognizer rec = new Recognizer(model, 16000.0f);
                speechService = new SpeechService(rec, 16000.0f);
                speechService.startListening(Vosk44100Activity.this);
            } catch (IOException e) {
                setErrorState(e.getMessage());
            }
        }
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
            RecognitionThread recognitionThread = new RecognitionThread();
            recognitionThread.start();
        }
    }
}
