package com.lazyjarod.goproremote;


import android.app.Service;
import android.content.Intent;
import android.media.VolumeProvider;
import android.media.session.MediaSession;
import android.os.IBinder;
import androidx.annotation.Nullable;

import android.util.Log;
import android.view.KeyEvent;

public final class PlayerService extends Service {
    private static MediaSession s_mediaSession;
    static boolean ready = false;

    public static boolean isRunning() {
        return isRunning;
    }

    GoProBle goProBle;
    @Override
    public void onCreate() {
        // Instantiate new MediaSession object.
        configureMediaSession();


    }

    void handleUI() {
        MainActivity mainActivity = MainActivity.getMainActivity();
        if (mainActivity != null) {
            Runnable action = () -> mainActivity.HandleService();
            mainActivity.runOnUiThread(action);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String goproName = intent.getStringExtra("GoProName");
        Log.d("log", "Service gopro name : " + goproName);
        goProBle = new GoProBle(getApplicationContext(), goproName, new GoProRemoteIQ());
        isRunning = true;
        handleUI();
        return super.onStartCommand(intent, flags, startId);
    }

    static boolean isRunning = false;
    @Override
    public void onDestroy() {
        isRunning = false;
        if (s_mediaSession != null)
            s_mediaSession.release();

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    long lastButtonTime = 0;

    private void configureMediaSession() {
        s_mediaSession = new MediaSession(this, "MyMediaSession");

        // Overridden methods in the MediaSession.Callback class.

        s_mediaSession.setCallback(new MediaSession.Callback() {
            @Override
            public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
                Log.d("tag", "onMediaButtonEvent called: " + mediaButtonIntent);
                /*if (lastButtonTime == 0 || System.currentTimeMillis() - lastButtonTime > 4000) {
                    goProBle.ToggleRecord(true);
                    lastButtonTime = System.currentTimeMillis();
                }*/


                KeyEvent ke = mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                if (ke != null && ke.getAction() == KeyEvent.ACTION_DOWN) {
                    int keyCode = ke.getKeyCode();
                    Log.d("tag", "onMediaButtonEvent Received command: " + ke);
                }
                return super.onMediaButtonEvent(mediaButtonIntent);
            }

            @Override
            public void onSkipToNext() {
                Log.d("tag", "onSkipToNext called (media button pressed)");
                super.onSkipToNext();
            }

            @Override
            public void onSkipToPrevious() {
                Log.d("tag", "onSkipToPrevious called (media button pressed)");
                super.onSkipToPrevious();
            }

            @Override
            public void onPause() {
                Log.d("tag", "onPause called (media button pressed)");
                super.onPause();
            }

            @Override
            public void onPlay() {
                Log.d("tag", "onPlay called (media button pressed)");
                super.onPlay();
            }

            @Override
            public void onStop() {
                Log.d("tag", "onStop called (media button pressed)");
                super.onStop();
            }
        });

        VolumeProvider myVolumeProvider =
                new VolumeProvider(VolumeProvider.VOLUME_CONTROL_RELATIVE, /*max volume*/100, /*initial volume level*/50) {
            @Override
            public void onAdjustVolume(int direction) {
                Log.d("tag", "onAdjustVolume : " + direction);
                /*
                -1 -- volume down
                1 -- volume up
                0 -- volume button released
                 */
            }
        };

        //s_mediaSession.setPlaybackState(new PlaybackState.Builder()
          //      .setState(PlaybackState.STATE_PLAYING, 0, 0) //you simulate a player which plays something.
            //    .build());

        //s_mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        s_mediaSession.setActive(true);
        s_mediaSession.setPlaybackToRemote(myVolumeProvider);



    }
}