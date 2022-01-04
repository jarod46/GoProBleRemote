package com.lazyjarod.goproremote;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;
import android.util.Log;

import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.UUID;

public class GoProBle {

    public BluetoothGatt goproGatt = null;
    UUID MAINSERVICE = UUID.fromString("0000fea6-0000-1000-8000-00805f9b34fb");
    UUID COMMANDCHARAC = UUID.fromString("b5f90072-aa8d-11e3-9046-0002a5d5c51b");
    UUID QUERYCHARAC = UUID.fromString("b5f90076-aa8d-11e3-9046-0002a5d5c51b");
    UUID QUERYRESPCHARAC = UUID.fromString("b5f90077-aa8d-11e3-9046-0002a5d5c51b");

    final byte VIDEO_MODE = 0;
    final byte PHOTO_MODE = 1;

    Context context;
    String deviceName;
    GoProRemoteIQ goProRemoteIQ;

    public void SetDeviceName(String name) {
        deviceName = name;
    }

    public  GoProBle(Context context, String deviceName, GoProRemoteIQ goProRemoteIQ) {
        this.context = context;
        this.goProRemoteIQ = goProRemoteIQ;
        SetDeviceName(deviceName);

    }

    void enableNotifications() {

        for (BluetoothGattService service : goproGatt.getServices())
        {
            for (BluetoothGattCharacteristic charac : service.getCharacteristics())
            {
                if (charac.getUuid().equals(QUERYRESPCHARAC))
                {
                    goproGatt.setCharacteristicNotification(charac, true);
                    BluetoothGattDescriptor descriptor = charac.getDescriptor(MainActivity.CHARACCONF);
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    goproGatt.writeDescriptor(descriptor);
                    //waitDescWrite();
                    break;
                }
            }
        }

    }

    void MonitorStatus() {

        BluetoothGattCharacteristic char1 = goproGatt.getService(MAINSERVICE).getCharacteristic(QUERYCHARAC);
        char1.setValue(new byte[] {02, 0x53, 0x26});
        boolean result = goproGatt.writeCharacteristic(char1);
        if (result) {
            waitChange();
        }

        BluetoothGattCharacteristic char2 = goproGatt.getService(MAINSERVICE).getCharacteristic(QUERYCHARAC);
        char2.setValue(new byte[] {02, 0x53, 0xA});
        boolean result2 = goproGatt.writeCharacteristic(char2);
        if (result2) {
            waitChange();
        }

        BluetoothGattCharacteristic char3 = goproGatt.getService(MAINSERVICE).getCharacteristic(QUERYCHARAC);
        char3.setValue(new byte[] {02, 0x53, 43});
        boolean result3 = goproGatt.writeCharacteristic(char3);
        if (result3) {
            waitChange();
        }

        BluetoothGattCharacteristic char4 = goproGatt.getService(MAINSERVICE).getCharacteristic(QUERYCHARAC);
        char4.setValue(new byte[] {02, 0x53, 70});
        boolean result4 = goproGatt.writeCharacteristic(char4);
        if (result4) {
            waitChange();
        }

    }

    void SetMode(byte mode) {
        for (BluetoothGattService service : goproGatt.getServices())
        {
            for (BluetoothGattCharacteristic charac : service.getCharacteristics())
            {
                if (charac.getUuid().equals(COMMANDCHARAC))
                {
                    //System.out.println("Record (" + enable + ")");
                    charac.setValue(new byte[] {05, 03, 01, mode, 01, mode});
                    goproGatt.writeCharacteristic(charac);
                    waitWrite();
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        return;
                    }
                    return;
                }
            }
        }
        Log.d("tag", "SetMode : charac not found !");

    }

    void Shutter(boolean enable) {
        for (BluetoothGattService service : goproGatt.getServices())
        {
            for (BluetoothGattCharacteristic charac : service.getCharacteristics())
            {
                if (charac.getUuid().equals(COMMANDCHARAC))
                {
                    //System.out.println("Record (" + enable + ")");
                    charac.setValue(new byte[] {3,1,1,enable ? (byte)1 : 0});
                    goproGatt.writeCharacteristic(charac);
                    waitWrite();
                    return;
                }
            }
        }
        Log.d("tag", "Record : charac not found !");

    }

    public void Sleep() {
        if (!ConnectedAndReady) {
            System.out.println("Not connected, ignore sleep...");
            return;
        }
        if (CamBusy()) {
            System.out.println("Camera busy, ignore sleep...");
            return;
        }
        for (BluetoothGattService service : goproGatt.getServices())
        {
            for (BluetoothGattCharacteristic charac : service.getCharacteristics())
            {
                if (charac.getUuid().equals(COMMANDCHARAC))
                {
                    charac.setValue(new byte[] {1,5});
                    goproGatt.writeCharacteristic(charac);
                    waitWrite();
                    return;
                }
            }
        }
        Log.d("tag", "Record : charac not found !");

    }

    public boolean settingDateTime = false;
    android.location.LocationListener locationListener = null;
    public android.location.LocationListener SetDateTime() {

        settingDateTime = true;

        android.location.LocationManager locationManager = (android.location.LocationManager)
                context.getSystemService(android.content.Context.LOCATION_SERVICE);



        locationListener = new android.location.LocationListener() {

            public void onLocationChanged(android.location.Location location) {

                String time = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS").format(location.getTime());
                Calendar calendar = new GregorianCalendar();
                calendar.setTimeInMillis(location.getTime());
                byte hour = (byte) calendar.get(Calendar.HOUR_OF_DAY);
                byte minute = (byte) calendar.get(Calendar.MINUTE);
                byte second = (byte) calendar.get(Calendar.SECOND);
                byte day = (byte)calendar.get(Calendar.DAY_OF_MONTH);
                byte month = (byte)(calendar.get(Calendar.MONTH) + 1);
                int year = calendar.get(Calendar.YEAR);
                byte year1 = (byte)(year & 0xFF);
                byte year2 = (byte)(year >> 8);


                if( location.getProvider().equals(android.location.LocationManager.GPS_PROVIDER)) {
                    android.util.Log.d("Location", "Time GPS: " + time); // This is what we want!
                    for (BluetoothGattService service : goproGatt.getServices())
                    {
                        for (BluetoothGattCharacteristic charac : service.getCharacteristics())
                        {
                            if (charac.getUuid().equals(COMMANDCHARAC))
                            {
                                System.out.println("Set date/time " + time);
                                byte[] dateArray = new byte[] {0x9, 0xD, 0x7, year2, year1, month, day, hour, minute, second};
                                charac.setValue(dateArray);
                                goproGatt.writeCharacteristic(charac);
                                locationManager.removeUpdates(locationListener);
                                settingDateTime = false;
                                return;
                            }
                        }
                    }
                }
                else {
                    android.util.Log.d("Location", "Time Device (" + location.getProvider() + "): " + time);
                }
            }

            public void onStatusChanged(String provider, int status, android.os.Bundle extras) {
            }
            public void onProviderEnabled(String provider) {
            }
            public void onProviderDisabled(String provider) {
            }
        };

        if (androidx.core.content.ContextCompat.checkSelfPermission(context,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            System.out.println("Incorrect 'uses-permission', requires 'ACCESS_FINE_LOCATION'");
            return null;
        }

        if (!Connect())
            return null;

        WaitReadyCam();
        locationManager.requestLocationUpdates(android.location.LocationManager.GPS_PROVIDER, 1000, 0, locationListener, Looper.getMainLooper());
        System.out.println("Waiting to get GPS infos...");
        return locationListener;

    }

    public boolean WaitConnectedAndReady() {
        long time = System.currentTimeMillis();
        while (!ConnectedAndReady) {
            try {
                Thread.sleep(100);
            }
            catch (Exception e) {
            }
            if (System.currentTimeMillis() - time > 10000) {
                System.out.println("GoPro connection timeout !");
                if (goproGatt != null)
                    goproGatt.close();
                return false;
            }
        }
        return true;
    }

    boolean hasShutter = false;
    public void TakePicture() {
        if (!Connect())
            return;;

        if (CamRecording()) {
            System.out.println("Cam busy (recording)");
            return;
        }
        WaitReadyCam();
        WaitBusyCam();

        byte mode = CurrentMode();
        if (mode != PHOTO_MODE) {
            SetMode(PHOTO_MODE);
        }
        long time = System.currentTimeMillis();
        hasShutter = false;
        do {
            Shutter(true);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        } while (!hasShutter);
        System.out.println("Take picture !");

    }

    public void ToggleRecord(boolean sleepOnStop){

        if (!Connect())
            return;

        if (CamRecording()) {
            Shutter(false);
            System.out.println("Record stopped !");
            WaitBusyCam();
            if (sleepOnStop) {
                System.out.println("Put cam to sleep...");
                Sleep();
            }
            return;
        }

        WaitReadyCam();
        WaitBusyCam();

        long time = System.currentTimeMillis();
        byte mode = CurrentMode();
        if (mode != VIDEO_MODE) {
            SetMode(VIDEO_MODE);
        }
        do {
            Shutter(true);
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                return;
            }
            if (System.currentTimeMillis() - time > 6000) {
                System.out.println("Start recording timeout...");
                return;
            }
        } while (!CamRecording());
        System.out.println("Record started !");

    }

    Boolean waitingWrite = false;
    Object writeLock = new Object();
    boolean waitWrite() {
        waitingWrite = true;
        synchronized (writeLock) {
            try {
                writeLock.wait(5000);
                if (waitingWrite) {
                    System.out.println("Wait write timeout...");
                    return false;
                }
            }
            catch (InterruptedException e) {
                return false;
            }
        }
        return true;
    }

    Boolean waitingChange = false;
    Object ChangeLock = new Object();
    boolean waitChange() {
        waitingChange = true;
        synchronized (ChangeLock) {
            try {
                ChangeLock.wait(5000);
                if (waitingChange) {
                    System.out.println("Wait change timeout...");
                    return false;
                }
            }
            catch (InterruptedException e) {
                return false;
            }
        }
        try {
            Thread.sleep(100);
        } catch (Exception e) {
            return true;
        }
        return true;
    }

    public void WaitReadyCam() {
        long time = System.currentTimeMillis();
        if (!CamReady()) {
            System.out.println("Wait ready cam...");
            while (!CamReady()) {
                try {
                    Thread.sleep(400);
                } catch (Exception e) {
                    return;
                }
                if (System.currentTimeMillis() - time > 6000) {
                    System.out.println("Wait timeout...");
                    return;
                }
            }
        }
    }

    public void WaitBusyCam() {
        long time = System.currentTimeMillis();
        if (CamBusy()) {
            System.out.println("Wait busy cam...");
            while (CamBusy()) {
                try {
                    Thread.sleep(400);
                } catch (Exception e) {
                    return;
                }
                if (System.currentTimeMillis() - time > 6000) {
                    System.out.println("Wait timeout...");
                    return;
                }
            }
            /*try {
                Thread.sleep(2000);
            }
            catch (Exception e) {
                return;
            }*/
        }
    }

    byte lastCamMode = 0;
    public byte CurrentMode() {
        lastCamMode = GetStatus((byte)43);
        Log.d("log", "Set last cam status (" + lastCamMode + ")");
        return  lastCamMode;
    }

    public static byte getLasBatteryPercent() {
        return  lasBatteryPercent;
    }

    static byte lasBatteryPercent = -1;
    public byte CurrentBattery() {
        lasBatteryPercent = GetStatus((byte)70);
        Log.d("log", "Set last battery percent (" + lasBatteryPercent + ")");
        return  lasBatteryPercent;
    }

    public boolean CamReady() {
        byte ready = GetStatus((byte)82);
        return ready == 1;
    }

    public boolean CamBusy() {
        byte busy = GetStatus((byte)8);
        return busy == 1 || busy == -1;
    }

    public boolean CamRecording(){
        return GetStatus((byte)10) == 1;
    }

    boolean waitForReady = false;
    Object readyLock = new Object();
    byte GetStatus(byte statusCode) {
        if (ConnectedAndReady) {
            for (BluetoothGattService service : goproGatt.getServices())
            {
                for (BluetoothGattCharacteristic charac : service.getCharacteristics())
                {
                    if (charac.getUuid().equals(QUERYCHARAC))
                    {
                        charac.setValue(new byte[] {2,0x13,statusCode});
                        waitForReady = goproGatt.writeCharacteristic(charac);
                        synchronized (readyLock) {
                            lastStatus = new byte[0];
                            try {
                                readyLock.wait(5000);
                                if (waitForReady) {
                                    System.out.println("Wait status timeout...");
                                    return -1;
                                }
                            }
                            catch (InterruptedException e) {
                                return  -1;
                            }
                        }
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (lastStatus.length > 5)
                            return lastStatus[5];
                        break;
                    }
                }
            }
        }
        return  -1;
    }

    public void Disconnect() {
        if (goproGatt != null)
            goproGatt.close();
    }

    public  Boolean ConnectedAndReady = false;
    byte[] lastStatus = new byte[0];
    public boolean Connect() {

        if (ConnectedAndReady)
            return true;
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice gopro = null;
        for (BluetoothDevice device : adapter.getBondedDevices())
        {
            if (device.getName().equals(deviceName))
            {
                gopro = device;
                break;
            }
        }
        if (gopro != null)
        {
            if (goproGatt != null) {
                try {
                    goproGatt.close();
                }
                catch (Exception e) {}
            }
            goproGatt = gopro.connectGatt(MainActivity.getMainActivity(),false, new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        System.out.println("GoPro ble connected");
                        goProRemoteIQ.sendMessage(GoProRemoteIQ.MessageType.Status, "Standby", "");
                        SharedPreferences sharedPref = MainActivity.getMainActivity().getPreferences(Context.MODE_PRIVATE);
                        boolean alertOnCamConnec = sharedPref.getBoolean("alertOnCamConnec", true);
                        if (alertOnCamConnec)
                            MainActivity.playSound(R.raw.coins497);
                        gatt.discoverServices();
                    }
                    else {
                        goProRemoteIQ.sendMessage(GoProRemoteIQ.MessageType.Status, "Disconnected", "");
                        System.out.println("GoPro ble disconnected");
                        ConnectedAndReady = false;
                    }
                    super.onConnectionStateChange(gatt, status, newState);
                }

                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    super.onServicesDiscovered(gatt, status);
                    enableNotifications();

                }


                @Override
                public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    super.onCharacteristicWrite(gatt, characteristic, status);
                    synchronized (writeLock) {
                        waitingWrite = false;
                        writeLock.notifyAll();
                    }
                }

                @Override
                public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                    super.onDescriptorWrite(gatt, descriptor, status);

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    ConnectedAndReady = true;
                    //MonitorRecording();

                }

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                    super.onCharacteristicChanged(gatt, characteristic);

                    synchronized (ChangeLock) {
                        waitingChange = false;
                        ChangeLock.notifyAll();
                    }


                    byte[] newStatus = characteristic.getValue();
                    Log.d("tag", "onCharacteristicChanged " + Arrays.toString(newStatus));


                    if (newStatus.length > 5 && newStatus[1] == -109) {
                        if (newStatus[3] == 0xA) {
                            Log.d("log", "Shutter : " + newStatus[5]);
                            if (newStatus[5] == 1) {
                                hasShutter = true;
                                if (lastCamMode == 0)
                                    goProRemoteIQ.sendMessage(GoProRemoteIQ.MessageType.Status, "Recording", "");
                            } else {
                                if (lastCamMode == 0)
                                    goProRemoteIQ.sendMessage(GoProRemoteIQ.MessageType.Status, "Standby", "");
                            }
                        }
                        if (newStatus[3] == 0x26 || (newStatus.length > 6 && newStatus[6] == 0x26)) {
                            byte picNum = newStatus[newStatus.length - 1];
                            Log.d("log", "Pictures num : " + picNum);
                            if (lastCamMode == 1)
                                goProRemoteIQ.sendMessage(GoProRemoteIQ.MessageType.Status, "Pictures", Byte.toString(picNum));
                        }
                        if (newStatus[3] == 43) {
                            if (newStatus[5] >= 0) {
                                Log.d("log", "Set last cam status (" + newStatus[5] + ")");
                                lastCamMode = newStatus[5];
                            }
                        }
                        if (newStatus[3] == 70) {
                            if (newStatus[5] >= 0) {
                                if (newStatus[5] != lasBatteryPercent) {
                                    Log.d("log", "Set battery percent (" + newStatus[5] + ")");
                                    lasBatteryPercent = newStatus[5];
                                    goProRemoteIQ.sendMessage(GoProRemoteIQ.MessageType.Battery, Byte.toString(lasBatteryPercent), "");
                                }
                            }
                        }
                        return;
                    }
                    lastStatus = newStatus;
                    //System.out.println("Status notified : " + Arrays.toString(lastStatus));
                    synchronized (readyLock) {
                        waitForReady = false;
                        readyLock.notifyAll();
                    }
                    //record();
                }


            });

            //goproGatt.connect();
            if (WaitConnectedAndReady()) {
                MonitorStatus();
                CurrentMode();
                CurrentBattery();
                return true;
            }

        }
        return  false;
    }

}
