package com.abdelmaksou.blind;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.abdelmaksou.blind.soundService.HomeSegmentSoundService;
import com.abdelmaksou.blind.soundService.HomeSoundService;
import com.abdelmaksou.blind.soundService.ObjectDetectionSoundService;

import org.w3c.dom.Text;

import java.util.Objects;

public class Home extends AppCompatActivity {

    FrameLayout textFrameLayout;
    FrameLayout objectFrameLayout;
    ImageView textImageView;
    ImageView objectImageView;
    final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // wait 650 ms
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //start service and play music
                startService(new Intent(Home.this, HomeSoundService.class));
            }
        }, 650);

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

    protected void onDestroy() {
        //stop service and stop music
        stopService(new Intent(Home.this, HomeSoundService.class));
        stopService(new Intent(Home.this, HomeSegmentSoundService.class));
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

