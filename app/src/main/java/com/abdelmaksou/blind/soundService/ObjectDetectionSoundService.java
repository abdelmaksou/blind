package com.abdelmaksou.blind.soundService;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;

import com.abdelmaksou.blind.R;

public class ObjectDetectionSoundService extends Service {
    MediaPlayer player;

    @Override
    public IBinder onBind(Intent intent) {
            return null;
        }
    public void onCreate() {
        player = MediaPlayer.create(this, R.raw.object); //select music file
        player.setLooping(false); //set looping
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        player.start();
        return Service.START_NOT_STICKY;
    }

    public void onDestroy() {
        player.stop();
        player.release();
        stopSelf();
        super.onDestroy();
    }

}