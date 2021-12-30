package com.lazyjarod.goproremote;

import android.content.Context;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.garmin.android.connectiq.ConnectIQ;
import com.garmin.android.connectiq.IQApp;
import com.garmin.android.connectiq.IQDevice;
import com.garmin.android.connectiq.exception.InvalidStateException;
import com.garmin.android.connectiq.exception.ServiceUnavailableException;

import java.util.LinkedList;
import java.util.List;

public class GoProRemoteIQ {

    ConnectIQ connectIQ = null;
    IQDevice iqDevice = null;
    IQApp gpriqApp = null;

    boolean ready = false;

    boolean messageConf = false;
    ConnectIQ.IQSendMessageListener iqSendMessageListener = new ConnectIQ.IQSendMessageListener() {
        @Override
        public void onMessageStatus(IQDevice iqDevice, IQApp iqApp, ConnectIQ.IQMessageStatus iqMessageStatus) {
            Log.d("log", "message status : " + iqMessageStatus);
            if (iqMessageStatus == ConnectIQ.IQMessageStatus.SUCCESS)
                messageConf = true;
        }
    };

    boolean waitMessageConf() {
        long time = System.currentTimeMillis();
        while (!messageConf) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (System.currentTimeMillis() - time > 6000)
                return false;
        }
        return  true;
    }

    public enum MessageType {
        Status,
        Battery,
        Remote,
    }

    MessageType lastMessageType = MessageType.Status;
    String lastMessage = "disconnected";
    String lastMessage2 = "";
    long lastTimeStamp = 0;
    Thread sendThread = null;
    boolean newMessage = false;
    public void sendMessage(MessageType type, String message, String message2) {
        lastTimeStamp = System.currentTimeMillis() / 1000;
        lastMessageType = type;
        lastMessage = message;
        lastMessage2 = message2;
        newMessage = true;
        Log.d("log", "Store gps message : [" + lastMessageType + ", " + lastMessage + ", " + lastMessage2 + "]");
        if (!ready)
            return;
        if (sendThread != null && sendThread.isAlive()) {
            Log.d("log", "Queue message...");
            return;
        }
        else  {
            Log.d("log", "no queue, proceed...");
        }

        sendThread = new Thread() {
            public void run() {
                try {


                    while (ready && newMessage) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Log.d("log", "Try send message to gps : [" + lastMessageType + ", " + lastMessage + ", " + lastMessage2 + "]");
                        messageConf = false;
                        List<String> messages = new LinkedList<>();
                        messages.add(lastMessageType.toString());
                        messages.add(lastMessage);
                        messages.add(lastMessage2);
                        messages.add(Long.toString(lastTimeStamp));
                        messages.add(Byte.toString(GoProBle.getLasBatteryPercent()));
                        newMessage = false;
                        connectIQ.sendMessage(iqDevice, gpriqApp, messages, iqSendMessageListener);
                        if (!waitMessageConf()) {
                            if (!newMessage) {
                                Log.d("log", "Last phone message fail, try again...");
                                newMessage = true;
                            }
                        }

                    }
                } catch (InvalidStateException e) {
                    e.printStackTrace();
                    return;
                } catch (ServiceUnavailableException e) {
                    e.printStackTrace();
                }
            }
        };
        sendThread.start();

    }

    ConnectIQ.IQApplicationInfoListener iqApplicationInfoListener = new ConnectIQ.IQApplicationInfoListener() {
        @Override
        public void onApplicationInfoReceived(IQApp iqApp) {

            Log.d("log", "Receive groproremotestatus app infos...");
            gpriqApp = iqApp;
            ready = true;
            sendMessage(lastMessageType, lastMessage, lastMessage2);
        }

        @Override
        public void onApplicationNotInstalled(String s) {
            Log.d("log", "GoProRemoteStatus data field not found !");
            gpriqApp = null;
        }
    };

    ConnectIQ.IQDeviceEventListener iqDeviceEventListener = new ConnectIQ.IQDeviceEventListener() {
        @Override
        public void onDeviceStatusChanged(IQDevice iqDevice, IQDevice.IQDeviceStatus iqDeviceStatus) {
            if (iqDeviceStatus == IQDevice.IQDeviceStatus.CONNECTED) {
                getAppInfos();
            }
            else {
                ready = false;
            }
        }
    };

    void getAppInfos() {
        try {
            Log.d("log", "get app infos...");
            connectIQ.getApplicationInfo("0fbef6a1-d2d8-4c0c-afb3-9fa58dac02c1", iqDevice, iqApplicationInfoListener);
        } catch (InvalidStateException e) {
            e.printStackTrace();
        } catch (ServiceUnavailableException e) {
            e.printStackTrace();
        }
    }

    public  void Stop() {
        try {
            connectIQ.unregisterAllForEvents();
            connectIQ.shutdown(MainActivity.getMainActivity());
        } catch (InvalidStateException e) {
            e.printStackTrace();
        }
    }

    public void Initialize(Context context) {
        connectIQ = ConnectIQ.getInstance(context, ConnectIQ.IQConnectType.WIRELESS);

        //if (connectIQ != null) {
            try {
                connectIQ.shutdown(context);
            } catch (InvalidStateException e) {
                //e.printStackTrace();
            }
        //}

        connectIQ.initialize(context, false, new ConnectIQ.ConnectIQListener() {
            @Override
            public void onSdkReady() {


                Log.d("log", "onSdkReady");

                List<IQDevice> paired = null;
                try {
                    paired = connectIQ.getKnownDevices();
                } catch (InvalidStateException e) {
                    Log.d("log", "InvalidStateException");
                    e.printStackTrace();
                } catch (ServiceUnavailableException e) {
                    Log.d("log", "ServiceUnavailableException");
                    e.printStackTrace();
                }

                if (paired != null && paired.size() > 0) {
                    Log.d("log", "knowndevices (" + paired.size() + ")");

                    MainActivity mainActivity = MainActivity.getMainActivity();
                    List<String> devicesNames = new LinkedList<>();

                    // get the status of the devices
                    for (IQDevice device : paired) {
                        devicesNames.add(device.getFriendlyName());
                        Log.d("log", "device : " + device.getFriendlyName());
                        if (device.getFriendlyName().equals(mainActivity.currentConnectIQDevice)) {
                            Log.d("log", "use this device...");
                            iqDevice = device;
                            try {
                                connectIQ.registerForDeviceEvents(iqDevice, iqDeviceEventListener);
                            } catch (InvalidStateException e) {
                                e.printStackTrace();
                            }
                        }
                    }


                    Runnable action = () -> {
                        Log.d("log", "add connect iq devices (" + devicesNames.size() + ")");
                        Spinner connectiqDevices = mainActivity.findViewById(R.id.connectIQDevice);
                        //connectiqDevices.setEnabled(true);
                        ArrayAdapter<String> spinAdapter = (ArrayAdapter<String>)connectiqDevices.getAdapter();
                        spinAdapter.clear();
                        spinAdapter.addAll(devicesNames);
                        if (devicesNames.contains(mainActivity.currentConnectIQDevice)) {
                            int devicePostion = spinAdapter.getPosition(mainActivity.currentConnectIQDevice);
                            connectiqDevices.setSelection(devicePostion);
                        }

                    };
                    mainActivity.runOnUiThread(action);


                }

            }

            @Override
            public void onInitializeError(ConnectIQ.IQSdkErrorStatus iqSdkErrorStatus) {
                Log.d("log", "onInitializeError " + iqSdkErrorStatus);

            }

            @Override
            public void onSdkShutDown() {
                Log.d("log", "onSdkShutDown");

            }
        });

    }

}
