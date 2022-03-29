package com.abdelmaksou.blind;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.abdelmaksou.blind.soundService.HomeSoundService;
import com.abdelmaksou.blind.soundService.InternetSoundService;
import com.abdelmaksou.blind.soundService.ObjectDetectionSoundService;
import com.abdelmaksou.blind.soundService.TextRecognitionSoundService;
import com.abdelmaksou.blind.soundService.UnknownErrorSoundService;
import com.abdelmaksou.blind.soundService.WaitSoundService;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.PictureResult;

import java.io.IOError;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

import darren.googlecloudtts.GoogleCloudTTS;
import darren.googlecloudtts.GoogleCloudTTSFactory;
import darren.googlecloudtts.model.VoicesList;
import darren.googlecloudtts.parameter.AudioConfig;
import darren.googlecloudtts.parameter.AudioEncoding;
import darren.googlecloudtts.parameter.VoiceSelectionParams;

import static com.otaliastudios.cameraview.CameraUtils.decodeBitmap;

public class text_recognition extends AppCompatActivity {

    CameraView cameraView;
    FrameLayout frameLayout;
    ImageView imageView;
    final Handler handler = new Handler();
    TTSWavenetGoogle tts = new TTSWavenetGoogle();
    int limit = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // runs the full explanatory audio for the first time only and runs an indicator for the rest
        if (getSharedPreferences("PREFERENCE", MODE_PRIVATE).getBoolean(text_recognition.class.getCanonicalName(), true)) {
            getSharedPreferences("PREFERENCE", MODE_PRIVATE).edit().putBoolean(text_recognition.class.getCanonicalName(),false).apply();
            // wait 850 ms
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //start service and play music
                    startService(new Intent(text_recognition.this, TextRecognitionSoundService.class));
                }
            }, 850);
        }
        else
        {
            tts.execute("text recognition");
        }
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Objects.requireNonNull(getSupportActionBar()).hide();
        setContentView(R.layout.activity_text_recognition);

        // handles the camera lifecycle
        cameraView = findViewById(R.id.camera);
        cameraView.setLifecycleOwner(this);

        // contents of the UI
        frameLayout = findViewById(R.id.textFrame);
        imageView = findViewById(R.id.text_recognition);

        // long click listeners
        frameLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                textInstructionsAgain(view);
                return true;
            }
        });
        imageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                textInstructionsAgain(view);
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
                stopService(new Intent(text_recognition.this, TextRecognitionSoundService.class));
                stopService(new Intent(text_recognition.this, WaitSoundService.class));
                startService(new Intent(text_recognition.this, InternetSoundService.class));
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
                stopService(new Intent(text_recognition.this, TextRecognitionSoundService.class));
                stopService(new Intent(text_recognition.this, WaitSoundService.class));
                startService(new Intent(text_recognition.this, InternetSoundService.class));
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    // back action
    @Override
    public void onBackPressed()
    {
        //stop service and stop music
        tts.onProgressUpdate(true);
        stopService(new Intent(text_recognition.this, TextRecognitionSoundService.class));
        stopService(new Intent(text_recognition.this, WaitSoundService.class));
        stopService(new Intent(text_recognition.this, InternetSoundService.class));
        Intent i = new Intent(text_recognition.this, Home.class);
        startActivity(i);
        finish();
        super.onBackPressed();
    }

    public void captureAndRecognise(View v)
    {
        //stop sound
        tts.onProgressUpdate(true);
        stopService(new Intent(text_recognition.this, TextRecognitionSoundService.class));
        stopService(new Intent(text_recognition.this, WaitSoundService.class));
        stopService(new Intent(text_recognition.this, InternetSoundService.class));

        cameraView.addCameraListener(new CameraListener() {
            @Override
            public void onPictureTaken(PictureResult result) {
                // set limit to zero
                limit = 0;

                // access the raw data and convert it to bitmap
                byte[] data = result.getData();
                Bitmap bitmap = decodeBitmap(data, Integer.MAX_VALUE, Integer.MAX_VALUE);

                // detect text
                FirebaseVisionImage firebaseVisionImage = FirebaseVisionImage.fromBitmap(bitmap);
                FirebaseVisionTextRecognizer firebaseVisionTextRecognizer = FirebaseVision.getInstance().getCloudTextRecognizer();
                // waiting sound
                startService(new Intent(text_recognition.this, WaitSoundService.class));
                // process text
                firebaseVisionTextRecognizer.processImage(firebaseVisionImage).addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                    @Override
                    public void onSuccess(FirebaseVisionText firebaseVisionText) {

                        // get text and display
                        List<FirebaseVisionText.TextBlock> blockList = firebaseVisionText.getTextBlocks();
                        if (blockList.size() == 0)
                        {
                            // wait 650 ms
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (limit == 0) {
                                        //Toast.makeText(text_recognition.this, "No text detected", Toast.LENGTH_LONG).show();
                                        tts.execute("No text detected");
                                        limit = -1;
                                    }
                                }
                            }, 650);
                        }
                        else
                        {
                            String text = "The detected text is.";
                            for (FirebaseVisionText.TextBlock block : firebaseVisionText.getTextBlocks())
                            {
                                text += block.getText();
                            }
                            if (limit == 0) {
                                //Toast.makeText(text_recognition.this, text, Toast.LENGTH_LONG).show();
                                tts.execute(text);
                                limit = -1;
                            }
                        }

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //start service and play music
                        stopService(new Intent(text_recognition.this, TextRecognitionSoundService.class));
                        stopService(new Intent(text_recognition.this, WaitSoundService.class));
                        startService(new Intent(text_recognition.this, InternetSoundService.class));
                    }
                });

            }
        });
        // snap picture
        cameraView.takePicture();
    }

    protected void onDestroy() {
        //stop service and stop music
        tts.onProgressUpdate(true);
        stopService(new Intent(text_recognition.this, TextRecognitionSoundService.class));
        stopService(new Intent(text_recognition.this, WaitSoundService.class));
        stopService(new Intent(text_recognition.this, InternetSoundService.class));
        super.onDestroy();
    }


    public void textInstructionsAgain(View v)
    {
        //start service and play music
        stopService(new Intent(text_recognition.this, InternetSoundService.class));
        stopService(new Intent(text_recognition.this, WaitSoundService.class));
        startService(new Intent(text_recognition.this, TextRecognitionSoundService.class));
    }

}
