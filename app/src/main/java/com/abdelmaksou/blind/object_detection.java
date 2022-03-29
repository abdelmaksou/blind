package com.abdelmaksou.blind;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.abdelmaksou.blind.soundService.HomeSoundService;
import com.abdelmaksou.blind.soundService.InternetSoundService;
import com.abdelmaksou.blind.soundService.ObjectDetectionSoundService;
import com.abdelmaksou.blind.soundService.TextRecognitionSoundService;
import com.abdelmaksou.blind.soundService.UnknownErrorSoundService;
import com.abdelmaksou.blind.tfLiteClassifier.tfLiteClassifier;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.cloud.FirebaseVisionCloudDetectorOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler;
import com.google.firebase.ml.vision.objects.FirebaseVisionObject;
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetector;
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetectorOptions;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.PictureResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import darren.googlecloudtts.GoogleCloudTTS;
import darren.googlecloudtts.GoogleCloudTTSFactory;
import darren.googlecloudtts.model.VoicesList;
import darren.googlecloudtts.parameter.AudioConfig;
import darren.googlecloudtts.parameter.AudioEncoding;
import darren.googlecloudtts.parameter.VoiceSelectionParams;

import static com.otaliastudios.cameraview.CameraUtils.decodeBitmap;

public class object_detection extends AppCompatActivity {

    CameraView cameraView;
    FrameLayout frameLayout;
    ImageView imageView;
    List<String> detected = new ArrayList<String>();
    List<Float> confidence = new ArrayList<Float>();
    private com.abdelmaksou.blind.tfLiteClassifier.tfLiteClassifier tfLiteClassifier;
    final Handler handler = new Handler();
    TTSWavenetGoogle tts = new TTSWavenetGoogle();
    int limit = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // runs the full explanatory audio for the first time only and runs an indicator for the rest
        if (getSharedPreferences("PREFERENCE", MODE_PRIVATE).getBoolean(object_detection.class.getCanonicalName(), true)) {
            getSharedPreferences("PREFERENCE", MODE_PRIVATE).edit().putBoolean(object_detection.class.getCanonicalName(),false).apply();
            // wait 650 ms
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //start service and play music
                    startService(new Intent(object_detection.this, ObjectDetectionSoundService.class));
                }
            }, 650);

        }
        else
        {
            tts.execute("object detection");
        }

        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Objects.requireNonNull(getSupportActionBar()).hide();
        setContentView(R.layout.activity_object_detection);

        // handles the camera lifecycle
        cameraView = findViewById(R.id.camera);
        cameraView.setLifecycleOwner(this);

        // contents of the UI
        frameLayout = findViewById(R.id.objectFrame);
        imageView = findViewById(R.id.object_detection);

        // long click listeners
        frameLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                objectInstructionsAgain(view);
                return true;
            }
        });
        imageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                objectInstructionsAgain(view);
                return true;
            }
        });

    }

    // back action
    @Override
    public void onBackPressed()
    {
        //stop service and stop music
        tts.onProgressUpdate(true);
        stopService(new Intent(object_detection.this, ObjectDetectionSoundService.class));
        stopService(new Intent(object_detection.this, InternetSoundService.class));
        stopService(new Intent(object_detection.this, UnknownErrorSoundService.class));
        Intent i = new Intent(object_detection.this, Home.class);
        startActivity(i);
        finish();
        super.onBackPressed();
    }

    public void captureAndDetect(View v)
    {
        // set limit to zero
        limit = 0;

        //stop sound
        tts.onProgressUpdate(true);
        stopService(new Intent(object_detection.this, ObjectDetectionSoundService.class));
        stopService(new Intent(object_detection.this, UnknownErrorSoundService.class));
        stopService(new Intent(object_detection.this, InternetSoundService.class));

        cameraView.addCameraListener(new CameraListener() {
            @Override
            public void onPictureTaken(PictureResult result) {
                // access the raw data and convert it to bitmap
                byte[] data = result.getData();
                int rotationDegree = result.getRotation();
                Bitmap bitmap = decodeBitmap(data, Integer.MAX_VALUE, Integer.MAX_VALUE);

                // TF Lite image classifier based on coco dataset
                try {
                    tfLiteClassifier = new tfLiteClassifier(object_detection.this);
                    List<tfLiteClassifier.Recognition> predictions = null;
                    predictions = tfLiteClassifier.recognizeImage(bitmap, 0);

                    String predection = "";
                    int i = 0;
                    int x = predictions.size();
                    if (predictions.get(0).getConfidence() >= 0.7)
                    {
                        x = 1;
                    }
                    else if (predictions.get(0).getConfidence() >= 0.5)
                    {
                        if (predictions.get(1).getConfidence() >= 0.4) {
                            x = 2;
                        }
                        else {
                            x = 1;
                        }
                    }
                    else {
                        x = 3;
                    }
                    if (x >= 3) {
                        i = 0;
                        switch (i) {
                            case 0:
                                predection += "It seems like a " + predictions.get(i).getName() /*+ "(" + predictions.get(i).getConfidence() + ")"*/;
                                i++;
                                break;
                            case 1:
                                predection += ", " + predictions.get(i).getName() /*+ "(" + predictions.get(i).getConfidence() + ")"*/;
                                i++;
                                break;
                            case 2:
                                predection += ", or " + predictions.get(i).getName() /*+ "(" + predictions.get(i).getConfidence() + ")"*/;
                                i++;
                                break;
                            default:
                                i = -1;
                            }
                    }
                    else if (x == 2) {
                        i = 0;
                        switch (i) {
                            case 0:
                                predection += "It seems like a " + predictions.get(i).getName() /*+ "(" + predictions.get(i).getConfidence() + ")"*/;
                                i++;
                                break;
                            case 1:
                                predection += ", or " + predictions.get(i).getName() /*+ "(" + predictions.get(i).getConfidence() + ")"*/;
                                i++;
                                break;
                            default:
                                i = -1;
                        }
                    }
                    else if (x == 1) {
                        i = 0;
                        switch (i) {
                            case 0:
                                predection += "It seems like a " + predictions.get(i).getName() /*+ "(" + predictions.get(i).getConfidence() + ")"*/;
                                i++;
                                break;
                            default:
                                i = -1;
                        }
                    }
                    else {
                        predection += "No object detected";
                    }
                    if (limit == 0) {
                        //Toast.makeText(object_detection.this, predection + ", " + vx, Toast.LENGTH_LONG).show();
                        tts.execute(predection);
                        limit = -1;
                    }
                } catch (IOException e) {
                    Toast.makeText(object_detection.this, e.getMessage(), Toast.LENGTH_LONG).show();
                    stopService(new Intent(object_detection.this, ObjectDetectionSoundService.class));
                    stopService(new Intent(object_detection.this, InternetSoundService.class));
                    startService(new Intent(object_detection.this, UnknownErrorSoundService.class));
                }

                // Firebase ML kit image labeling
                /*FirebaseVisionCloudDetectorOptions options =
                        new FirebaseVisionCloudDetectorOptions.Builder()
                                .setModelType(FirebaseVisionCloudDetectorOptions.LATEST_MODEL)
                                .setMaxResults(15)
                                .build();
                FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);

                FirebaseVisionImageLabeler detector = FirebaseVision.getInstance()
                        .getCloudImageLabeler();

                Task<List<FirebaseVisionImageLabel>> results = detector.processImage(image)
                        .addOnSuccessListener(
                                new OnSuccessListener<List<FirebaseVisionImageLabel>>() {
                                    @Override
                                    public void onSuccess(List<FirebaseVisionImageLabel> labels) {
                                        String top = "";
                                        if (labels.size() == 0)
                                        {
                                            Toast.makeText(object_detection.this, "No object detected",Toast.LENGTH_SHORT).show();
                                        }
                                        else {
                                            for (FirebaseVisionImageLabel label : labels) {
                                                detected.add(label.getText());
                                                confidence.add(label.getConfidence());
                                            }
                                            // top 3
                                            for (int x = 0; x < 3; x++)
                                            {
                                                top += detected.get(confidence.indexOf(Collections.max(confidence))) + "" + Collections.max(confidence) + "\n";
                                                detected.remove(confidence.indexOf(Collections.max(confidence)));
                                                confidence.remove(confidence.indexOf(Collections.max(confidence)));
                                            }
                                            Toast.makeText(object_detection.this, top, Toast.LENGTH_LONG).show();
                                        }
                                    }
                                })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        //start service and play music
                                        stopService(new Intent(object_detection.this, ObjectDetectionSoundService.class));
                                        startService(new Intent(object_detection.this, InternetSoundService.class));
                                    }
                                });*/

                // Firebase ML kit Multiple object detection
                /*FirebaseVisionObjectDetectorOptions options =
                        new FirebaseVisionObjectDetectorOptions.Builder()
                                .setDetectorMode(FirebaseVisionObjectDetectorOptions.SINGLE_IMAGE_MODE)
                                .enableMultipleObjects()
                                .enableClassification()
                                .build();
                FirebaseVisionObjectDetector objectDetector =
                        FirebaseVision.getInstance().getOnDeviceObjectDetector(options);
                FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
                objectDetector.processImage(image)
                        .addOnSuccessListener(
                                new OnSuccessListener<List<FirebaseVisionObject>>() {
                                    @Override
                                    public void onSuccess(List<FirebaseVisionObject> detectedObjects) {
                                        String top = "";
                                        if (detectedObjects.size() == 0)
                                        {
                                            Toast.makeText(object_detection.this, "No object detected",Toast.LENGTH_SHORT).show();
                                        }
                                        else {
                                            for (FirebaseVisionObject obj : detectedObjects) {
                                                int category = obj.getClassificationCategory();
                                                Float confidence = obj.getClassificationConfidence();
                                                top += category + ", " + confidence + "\n";
                                            }

                                            // top 3
                                            for (int x = 0; x < 3; x++)
                                            {
                                                top += detected.get(confidence.indexOf(Collections.max(confidence))) + "" + Collections.max(confidence) + "\n";
                                                detected.remove(confidence.indexOf(Collections.max(confidence)));
                                                confidence.remove(confidence.indexOf(Collections.max(confidence)));
                                            }
                                            Toast.makeText(object_detection.this, top, Toast.LENGTH_LONG).show();
                                        }
                                    }
                                })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(object_detection.this, e.getMessage(), Toast.LENGTH_LONG).show();
                                        //start service and play music
                                        stopService(new Intent(object_detection.this, ObjectDetectionSoundService.class));
                                        startService(new Intent(object_detection.this, InternetSoundService.class));
                                    }
                                });*/
            }
        });
        // snap picture
        cameraView.takePicture();
    }

    private class TTSWavenetGoogle extends AsyncTask<String, Boolean, Void>
    {
        GoogleCloudTTS googleCloudTTS;
        Boolean started = false;
        @Override
        protected Void doInBackground(String... strings) {
            try {
                // Set the ApiKey and create GoogleCloudTTS.
                googleCloudTTS = GoogleCloudTTSFactory.create("API_KEY");

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
                stopService(new Intent(object_detection.this, ObjectDetectionSoundService.class));
                stopService(new Intent(object_detection.this, UnknownErrorSoundService.class));
                startService(new Intent(object_detection.this, InternetSoundService.class));
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
                stopService(new Intent(object_detection.this, ObjectDetectionSoundService.class));
                stopService(new Intent(object_detection.this, UnknownErrorSoundService.class));
                startService(new Intent(object_detection.this, InternetSoundService.class));
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
        stopService(new Intent(object_detection.this, ObjectDetectionSoundService.class));
        stopService(new Intent(object_detection.this, InternetSoundService.class));
        stopService(new Intent(object_detection.this, UnknownErrorSoundService.class));
        super.onDestroy();
    }

    public void objectInstructionsAgain(View v)
    {
        //start service and play music
        stopService(new Intent(object_detection.this, InternetSoundService.class));
        stopService(new Intent(object_detection.this, InternetSoundService.class));
        startService(new Intent(object_detection.this, ObjectDetectionSoundService.class));
    }

}
