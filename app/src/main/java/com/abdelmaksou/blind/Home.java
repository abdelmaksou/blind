package com.abdelmaksou.blind;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.abdelmaksou.blind.soundService.HomeSegmentSoundService;
import com.abdelmaksou.blind.soundService.HomeSoundService;
import com.abdelmaksou.blind.soundService.InternetSoundService;
import com.abdelmaksou.blind.soundService.ObjectDetectionSoundService;
import com.abdelmaksou.blind.soundService.TextRecognitionSoundService;
import com.abdelmaksou.blind.soundService.UnknownErrorSoundService;

import org.w3c.dom.Text;

import java.util.Objects;

import darren.googlecloudtts.GoogleCloudTTS;
import darren.googlecloudtts.GoogleCloudTTSFactory;
import darren.googlecloudtts.model.VoicesList;
import darren.googlecloudtts.parameter.AudioConfig;
import darren.googlecloudtts.parameter.AudioEncoding;
import darren.googlecloudtts.parameter.VoiceSelectionParams;

public class Home extends AppCompatActivity {

    FrameLayout textFrameLayout;
    FrameLayout objectFrameLayout;
    ImageView textImageView;
    ImageView objectImageView;
    TTSWavenetGoogle tts = new TTSWavenetGoogle();
    final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // runs the full explanatory audio for the first time only and runs an indicator for the rest
        if (getSharedPreferences("PREFERENCE", MODE_PRIVATE).getBoolean(Home.class.getCanonicalName(), true)) {
            getSharedPreferences("PREFERENCE", MODE_PRIVATE).edit().putBoolean(Home.class.getCanonicalName(),false).apply();
            // wait 650 ms
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //start service and play music
                    startService(new Intent(Home.this, HomeSoundService.class));
                }
            }, 650);
        }
        else
        {
            tts.execute("home screen");
        }
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Objects.requireNonNull(getSupportActionBar()).hide();
        setContentView(R.layout.activity_home);

        // contents of the UI
        textFrameLayout = findViewById(R.id.homeTextFrame);
        objectFrameLayout = findViewById(R.id.homeObjectFrame);
        textImageView = findViewById(R.id.text_recognition_button);
        objectImageView = findViewById(R.id.object_detection_button);

        // long click listeners
        textFrameLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                homeInstructionsAgain(view);
                return true;
            }
        });
        objectFrameLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                homeInstructionsAgain(view);
                return true;
            }
        });
        textImageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                homeInstructionsAgain(view);
                return true;
            }
        });
        objectImageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                homeInstructionsAgain(view);
                return true;
            }
        });

    }

    private class TTSWavenetGoogle extends AsyncTask<String, Boolean, Void>
    {
        GoogleCloudTTS googleCloudTTS;
        Boolean started = false;
        @Override
        protected Void doInBackground(String... strings) {
            try {
                // Set the ApiKey and create GoogleCloudTTS.
                googleCloudTTS = GoogleCloudTTSFactory.create("AIzaSyAqsCeW-CAGjrlJFOWAp_2up2R4zAzs89g");

                // Load google cloud VoicesList and select the languageCode and voiceName with index (0 ~ N).
                VoicesList voicesList = googleCloudTTS.load();
                String languageCode = "en-US";
                String voiceName = "en-US-Wavenet-I";

                // Set languageCode and voiceName, Rate and pitch parameter.
                googleCloudTTS.setVoiceSelectionParams(new VoiceSelectionParams(languageCode, voiceName))
                        .setAudioConfig(new AudioConfig(AudioEncoding.MP3, 1.0f , 0.0f));

                // start speak
                googleCloudTTS.start(strings[0]);
                started = true;
            } catch (Exception e) {
                stopService(new Intent(Home.this, HomeSoundService.class));
                stopService(new Intent(Home.this, HomeSegmentSoundService.class));
                startService(new Intent(Home.this, InternetSoundService.class));
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Boolean... stop){
            try {
                if(stop[0] && started)
                {
                    started = false;
                    googleCloudTTS.stop();
                    googleCloudTTS.close();
                }
            } catch (Exception e) {
                stopService(new Intent(Home.this, HomeSoundService.class));
                stopService(new Intent(Home.this, HomeSegmentSoundService.class));
                startService(new Intent(Home.this, InternetSoundService.class));
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    protected void onDestroy() {
        //stop service and stop music
        tts.onProgressUpdate(true);
        stopService(new Intent(Home.this, HomeSoundService.class));
        stopService(new Intent(Home.this, HomeSegmentSoundService.class));
        stopService(new Intent(Home.this, InternetSoundService.class));
        super.onDestroy();
    }

    public void goTo_objectDetection(View v){
        Intent intent = new Intent(Home.this, object_detection.class);
        startActivity(intent);
        finish();
    }

    public void goTo_textRecognition(View v){
        Intent intent = new Intent(Home.this, text_recognition.class);
        startActivity(intent);
        finish();
    }

    public void homeInstructionsAgain(View v)
    {
        //start service and play music
        stopService(new Intent(Home.this, HomeSoundService.class));
        startService(new Intent(Home.this, HomeSegmentSoundService.class));
    }
}
