package com.lazyjarod.goproremote;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.VolumeProvider;
import android.media.session.MediaSession;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Button;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

public class BackgroundBle extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    String CHANNEL_ID = "gprmtdi2";

    static boolean isRunning = false;

    public static boolean isConnected(BluetoothDevice device) {
        try {
            Method m = device.getClass().getMethod("isConnected", (Class[]) null);
            return  (boolean) m.invoke(device, (Object[]) null);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static boolean multimediaRemoteStatus = false;
    static boolean di2RemoteStatus = false;
    public static void setMultimediaRemoteStatus(boolean status) {
        boolean lastRemoteStatus = getRemoteEnabled();
        multimediaRemoteStatus = status;
        if (getRemoteEnabled() != lastRemoteStatus)
            HandleRemoteStatusChange();
    }
    public static void setDi2RemoteStatus(boolean status) {
        boolean lastRemoteStatus = getRemoteEnabled();
        di2RemoteStatus = status;
        if (getRemoteEnabled() != lastRemoteStatus)
            HandleRemoteStatusChange();
    }
    public static boolean getRemoteEnabled() {
        return multimediaRemoteStatus || di2RemoteStatus;
    }
    static void HandleRemoteStatusChange() {
        if (isRunning()) {
            getInstance().goProRemoteIQ.sendMessage(GoProRemoteIQ.MessageType.Remote, getRemoteEnabled() ? "1" : "0", "");
        }
    }


    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device != null && device.getBluetoothClass().getDeviceClass() == 0x540) { // 0x540 PERIPHERAL_KEYBOARD
                if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
                    MainActivity.playSound(R.raw.confirmation);
                    setMultimediaRemoteStatus(true);
                    System.out.println("BT Keyboard connected : " + device.getName());
                }
                if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
                    MainActivity.playSound(R.raw.fail);
                    setMultimediaRemoteStatus(false);
                    System.out.println("BT Keyboard disconnected : " + device.getName());
                }
            }
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        createNotificationChannel();
        goProRemoteIQ = new GoProRemoteIQ();
        goProBle = new GoProBle(this, intent.getStringExtra("GoProName"), goProRemoteIQ);
        String di2Name = intent.getStringExtra("Di2Name");
        boolean useDi2 = di2Name.length() > 0 && !di2Name.equals("Disabled");
        if (useDi2)
            di2Fly = new Di2Fly(this, di2Name, intent.getIntExtra("dflyChan", 1), goProBle);

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        boolean connectedKeyboard = false;
        for (BluetoothDevice device : adapter.getBondedDevices())
        {
            if (device.getBluetoothClass().getDeviceClass() == 0x540 && isConnected(device)) {
                connectedKeyboard = true;
                break;
            }
        }
        setMultimediaRemoteStatus(connectedKeyboard);

        this.registerReceiver(broadcastReceiver, new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));
        this.registerReceiver(broadcastReceiver, new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("GoPro Di2 Remote")
                .setContentText("Monitor buttons...")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);

        if (useDi2)
            di2Fly.Initialize();

        goProRemoteIQ.Initialize(MainActivity.getMainActivity());

        isRunning = true;
        instance = this;

        handleUI();

        return START_NOT_STICKY;
    }

    void handleUI() {
        MainActivity mainActivity = MainActivity.getMainActivity();
        if (mainActivity != null) {
            Runnable action = () -> mainActivity.HandleService();
            mainActivity.runOnUiThread(action);
        }
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT);

        getSystemService(NotificationManager.class).createNotificationChannel(serviceChannel);
    }

    GoProBle goProBle;
    Di2Fly di2Fly;
    GoProRemoteIQ goProRemoteIQ;

    @Override
    public void onCreate() {
        super.onCreate();


        configureMediaSession();
    }

    static BackgroundBle instance = null;
    public static BackgroundBle getInstance() {
        return instance;
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        super.onDestroy();
        instance = null;
        this.unregisterReceiver(broadcastReceiver);
        if (s_mediaSession != null)
            s_mediaSession.release();
        if (goProRemoteIQ != null)
            goProRemoteIQ.Stop();
        if (goProBle.goproGatt != null)
            goProBle.goproGatt.close();
        if (di2Fly != null && di2Fly.di2Gatt != null)
            di2Fly.di2Gatt.close();

        handleUI();
    }

    public static boolean isRunning() {
        return isRunning;
    }

    Thread buttonHandleThread = null;
    boolean anotherPress = false;
    boolean longPress = false;
    long lastHandle = 0;

    private static MediaSession s_mediaSession;
    private void configureMediaSession() {
        s_mediaSession = new MediaSession(this, "MyMediaSession");

        // Overridden methods in the MediaSession.Callback class.

        s_mediaSession.setCallback(new MediaSession.Callback() {
            @Override
            public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
                //Log.d("tag", "onMediaButtonEvent called: " + mediaButtonIntent);
                /*if (lastButtonTime == 0 || System.currentTimeMillis() - lastButtonTime > 4000) {
                    goProBle.ToggleRecord(true);
                    lastButtonTime = System.currentTimeMillis();
                }*/


                KeyEvent ke = mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);


                if (ke != null && ke.getAction() == KeyEvent.ACTION_DOWN) {
                    int keyCode = ke.getKeyCode();
                    Log.d("tag", "onMediaButtonEvent Received command: " + ke);

                    anotherPress = true;
                    if (ke.getRepeatCount() > 0)
                        longPress = true;

                    if (buttonHandleThread == null || !buttonHandleThread.isAlive()) {
                        if (lastHandle == 0 || System.currentTimeMillis() - lastHandle > 5000) {
                            buttonHandleThread = new Thread(new Runnable() {
                                public void run() {

                                    anotherPress = false;
                                    longPress = false;
                                    try {
                                        Thread.sleep(500);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    if (longPress) {
                                        MainActivity.playSound(R.raw.longbeep);
                                        Log.d("log", "Long press !");
                                        if (goProBle.ConnectedAndReady)
                                            goProBle.Sleep();
                                        else
                                            goProBle.Connect();
                                    } else {
                                        if (anotherPress) {
                                            Log.d("log", "Double press !");
                                            MainActivity.playSound(R.raw.doublebeep);
                                            goProBle.TakePicture();
                                        } else {
                                            Log.d("log", "Simple press !");
                                            MainActivity.playSound(R.raw.beep);
                                            goProBle.ToggleRecord(true);
                                        }
                                    }
                                    lastHandle = System.currentTimeMillis();

                                }
                            });
                            buttonHandleThread.start();
                        }
                        else {
                            Log.d("log", "Too much button press, ignore.");
                        }
                    }
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

        /*s_mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        s_mediaSession.setPlaybackState(new PlaybackState.Builder()
                .setState(PlaybackState.STATE_PLAYING, 0, 0) //you simulate a player which plays something.
                .build());*/

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

        /*s_mediaSession.setPlaybackState(new PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY)
                .setState(PlaybackState.STATE_STOPPED, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 0)
                .build());*/

        //s_mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        s_mediaSession.setPlaybackToRemote(myVolumeProvider);
        s_mediaSession.setActive(true);




    }
}
