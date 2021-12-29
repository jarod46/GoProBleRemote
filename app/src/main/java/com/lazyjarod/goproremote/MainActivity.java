package com.lazyjarod.goproremote;

import android.Manifest;
import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.bluetooth.BluetoothAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;

import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;



public class MainActivity extends AppCompatActivity {


    /*@Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        TextView label = findViewById(R.id.textView);
        label.setText("key down : " + keyCode);
        if (keyCode == 85)
            record();
        System.out.print(("testdfsf"));
        Log.d("tag", "messagetest");
        return super.onKeyDown(keyCode, event);
    }*/

    public static UUID CHARACCONF = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    static MediaPlayer mMediaPlayer;
    public static void playSound(int sound) {
        final AudioManager mAudioManager = (AudioManager)sApplication.getSystemService(AUDIO_SERVICE);
        final int originalVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (maxVolume)
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
        mMediaPlayer = MediaPlayer.create(getContext(), sound);
        mMediaPlayer.start();
        mMediaPlayer.setOnCompletionListener(mediaPlayer -> {
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0);
            mediaPlayer.release();
        });
    }


    private static Application sApplication;



    public static Context getContext() {

        return sApplication.getApplicationContext();
    }

    static boolean readyRecord = false;
    static boolean maxVolume = true;

    GoProBle goProBle;
    //Di2Fly di2Fly;
    int currentDflyChan;

    String currentCam;
    String currentDi2;
    String currentConnectIQDevice;

    Thread setTimeThread;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //Intent intent = new Intent(this, BackgroundBle.class);
        //stopService(intent);
        savedActivity = null;
    }

    static MainActivity savedActivity;
    public static MainActivity getMainActivity() {
        return savedActivity;
    }

    public  void HandleService() {


        Button b = findViewById(R.id.button);
        Button b2 = findViewById(R.id.button2);
        Spinner connectIQChoose = findViewById(R.id.connectIQDevice);
        Spinner camChoose = findViewById(R.id.camChoice);
        Spinner di2Choose = findViewById(R.id.di2Choice);
        Spinner dflyChan = findViewById(R.id.dflyChan);
        if (BackgroundBle.isRunning()) {
            b2.setEnabled(false);
            di2Choose.setEnabled(false);
            camChoose.setEnabled(false);
            dflyChan.setEnabled(false);
            connectIQChoose.setEnabled(false);
            b.setEnabled(true);
            b.setText("STOP");
        }
        else {
            b2.setEnabled(true);
            di2Choose.setEnabled(true);
            camChoose.setEnabled(true);
            dflyChan.setEnabled(true);
            connectIQChoose.setEnabled(true);
            b.setEnabled(true);
            b.setText("START");
        }
    }




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        /*Thread t = new Thread(new Runnable() {
            public void run(){
                Looper.prepare();

            }
        });
        setTimeThread.start();*/


        savedActivity = this;
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);
        sApplication = getApplication();

        SharedPreferences sharedPref = getPreferences(MODE_PRIVATE);

        EditText editText = findViewById(R.id.logsLines);
        PrintStream printStream = new PrintStream(new CustomOutputStream(this, editText));
        System.setOut(printStream);
        System.out.println("Loading...");

        GoProRemoteIQ goProRemoteIQ = new GoProRemoteIQ();
        goProRemoteIQ.Initialize(this);

        Spinner connectiqDevice = findViewById(R.id.connectIQDevice);
        ArrayAdapter<String> spinIQAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        spinIQAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinIQAdapter.add("Empty");
        connectiqDevice.setAdapter(spinIQAdapter);
        currentConnectIQDevice = sharedPref.getString("ConnectIQDevice", "");
        if (currentConnectIQDevice.length() > 0)
            Log.d("log", "Saved connect iq device : " + currentConnectIQDevice);
        connectiqDevice.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (adapterView.getSelectedItemId() != AdapterView.INVALID_ROW_ID) {
                    String lastConnectIQDevice = adapterView.getItemAtPosition(i).toString();

                    if (!lastConnectIQDevice.isEmpty()) {
                        currentConnectIQDevice = lastConnectIQDevice;
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putString("ConnectIQDevice", lastConnectIQDevice);
                        Log.d("log", "Set ConnectIQ device to : " + lastConnectIQDevice);
                        editor.apply();
                        //goProRemoteIQ.Initialize(getMainActivity());
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });


        //playSound();
        HandleService();


        String[] LOCATION_PERMS = { Manifest.permission.ACCESS_FINE_LOCATION };
        requestPermissions(LOCATION_PERMS, 1337);

        Switch enableMaxVolum = findViewById(R.id.enableMaxVolume);
        maxVolume = sharedPref.getBoolean("enableMaxVolume", true);
        enableMaxVolum.setChecked(maxVolume);
        enableMaxVolum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                maxVolume = enableMaxVolum.isChecked();
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean("enableMaxVolume", enableMaxVolum.isChecked());
                editor.apply();
            }
        });



        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        List<String> devices = new LinkedList<>();
        devices.add("Disabled");
        for (BluetoothDevice device : adapter.getBondedDevices())
        {
            devices.add(device.getName());
        }
        Spinner camChoose = findViewById(R.id.camChoice);
        ArrayAdapter<String> spinAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, devices);
        spinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        camChoose.setAdapter(spinAdapter);

        String lastCam = sharedPref.getString("lastCam", "");
        if (lastCam.length() > 0) {
            int lastCamIndex = spinAdapter.getPosition(lastCam);
            camChoose.setSelection(lastCamIndex);
        }

        camChoose.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String lastCamName = adapterView.getItemAtPosition(i).toString();
                currentCam = lastCamName;
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString("lastCam", lastCamName);
                editor.apply();
                goProBle.SetDeviceName(currentCam);
                //System.out.println("GoPro selected : " + lastCamName);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        Spinner di2Choose = findViewById(R.id.di2Choice);
        di2Choose.setAdapter(spinAdapter);



        String lastDi2 = sharedPref.getString("lastDi2", "Disabled");
        if (lastDi2.length() > 0) {
            int lastDi2Index = spinAdapter.getPosition(lastDi2);
            di2Choose.setSelection(lastDi2Index);
        }

        di2Choose.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String lastDi2Name = adapterView.getItemAtPosition(i).toString();
                currentDi2 = lastDi2Name;
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString("lastDi2", lastDi2Name);
                editor.apply();
                //di2Fly.SetDeviceName(currentDi2);
                //System.out.println("Di2 selected : " + lastDi2Name);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        Spinner dflyChan = findViewById(R.id.dflyChan);
        List<String> dflyChans = new LinkedList<>();
        dflyChans.add("Chan1");
        dflyChans.add("Chan2");
        dflyChans.add("Chan3");
        dflyChans.add("Chan4");
        ArrayAdapter<String> dflyAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, dflyChans);
        dflyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dflyChan.setAdapter(dflyAdapter);

        int lastDlfyChan = sharedPref.getInt("lastDflyChan", 1);
        dflyChan.setSelection(lastDlfyChan - 1);

        dflyChan.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                currentDflyChan = i + 1;
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putInt("lastDflyChan", currentDflyChan);
                editor.apply();
                //System.out.println("D-Fly channel selected : " + currentDflyChan);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });



        goProBle = new GoProBle(this, currentCam, new GoProRemoteIQ());
        //di2Fly = new Di2Fly(this, currentDi2, currentDflyChan, goProBle);

        Button b = findViewById(R.id.button);
        Button b2 = findViewById(R.id.button2);

        b.setOnClickListener(v -> {

            Intent intent = new Intent(this, BackgroundBle.class);

            if (BackgroundBle.isRunning()) {
                System.out.println("Stop monitoring service...");
                stopService(intent);
            }
            else {
                //di2Fly.Initialize(b);
                b.setEnabled(false);
                System.out.println("Handle D-Fly Chan" + currentDflyChan + " on " + currentDi2);
                System.out.println("Remote camera " + currentCam);
                System.out.println("Start monitoring service...");

                //Log.d("log", "Sent name : " + currentCam);
                intent.putExtra("GoProName", currentCam);
                intent.putExtra("Di2Name", currentDi2);
                intent.putExtra("dflyChan", currentDflyChan);
                startService(intent);
            }
        });


        b2.setOnClickListener(v -> {
            b.setEnabled(false);
            b2.setEnabled(false);
            Runnable action = () -> {
                b.setEnabled(true);
                b2.setEnabled(true);
            };
            //goProBle.SetDateTime();
            setTimeThread = new Thread(new Runnable() {
                public void run(){
                    Looper.prepare();
                    android.location.LocationManager locationManager = (android.location.LocationManager)getSystemService(android.content.Context.LOCATION_SERVICE);
                    if (!goProBle.Connect())
                        return;
                    android.location.LocationListener locationListener = goProBle.SetDateTime();
                    long lastTick = System.currentTimeMillis();
                    while (goProBle.settingDateTime) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (System.currentTimeMillis() - lastTick > 30000) {
                            System.out.println("GPS acquisition timeout...");
                            break;
                        }
                    }
                    if (locationListener != null)
                        locationManager.removeUpdates(locationListener);
                    goProBle.Disconnect();
                    runOnUiThread(action);
                }
            });
            setTimeThread.start();

        });



    }

}
