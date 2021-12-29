package com.lazyjarod.goproremote;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;
import android.widget.Button;

import com.garmin.android.connectiq.ConnectIQ;

import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;

public class Di2Fly {

    public enum ButtonPressType {
        UNKNOWN,
        SIMPLE, // 0x10
        LONG, // 0x20
        DOUBLE, // 0x40
    }

    public BluetoothGatt di2Gatt = null;
    UUID DFLYCHARAC = UUID.fromString("00002ac2-5348-494d-414e-4f5f424c4500");

    GoProBle goProBle;
    String deviceName;
    int dflyChan;
    Context context;

    public Di2Fly(Context context, String deviceName, int dflyChan, GoProBle goProBle) {
        this.context = context;
        this.goProBle = goProBle;
        SetDeviceName(deviceName);
        this.dflyChan = dflyChan;
    }

    public void SetDeviceName(String name) {
        deviceName = name;
    }

    void monitorButtons() {

        for (BluetoothGattService service : di2Gatt.getServices())
        {
            for (BluetoothGattCharacteristic charac : service.getCharacteristics())
            {
                if (charac.getUuid().equals(DFLYCHARAC))
                {
                    di2Gatt.setCharacteristicNotification(charac, true);
                    BluetoothGattDescriptor descriptor = charac.getDescriptor(MainActivity.CHARACCONF);
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                    di2Gatt.writeDescriptor(descriptor);
                    MainActivity.playSound(R.raw.confirmation);
                    System.out.println("D-Fly monitoring enabled");
                    return;
                }
            }
        }
        System.out.println("Di2 D-Fly Buttons monitoring failed");

    }

    ButtonPressType InterpretPressType(Byte val) {
        if ((val & 0x40) != 0) {
            return ButtonPressType.DOUBLE;
        }
        else if ((val & 0x20) != 0) {
            return ButtonPressType.LONG;
        }
        else if ((val & 0x10) != 0) {
            return ButtonPressType.SIMPLE;
        }
        return ButtonPressType.UNKNOWN;
    }

    byte[] lastDfly = new byte[0];
    long lastPress = 0;
    boolean manageButton = false;
    boolean tooMuchPressDisplayed = false;
    public void Initialize(Button b) {


        if (di2Gatt != null) {
            try {
                di2Gatt.close();
                Thread.sleep(500);
            } catch (Exception e) {
            }
        }

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice di2 = null;
        lastDfly = new byte[] {20,-16,-16,-16,-16};
        for (BluetoothDevice device : adapter.getBondedDevices())
        {
            if (device.getName().equals(deviceName))
            {
                di2 = device;
                break;
            }
        }

        //Log.d("tag", "Di2 ble connecting...");

        if (di2 != null)
        {
            System.out.println("Di2 ble connecting " + deviceName + "...");

            di2Gatt = di2.connectGatt(MainActivity.getContext(),true, new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    super.onConnectionStateChange(gatt, status, newState);

                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        System.out.println("Di2 ble connected");
                        gatt.discoverServices();
                    }
                    else {
                        MainActivity.playSound(R.raw.fail);
                        System.out.println("Di2 ble disconnect");
                    }

                }

                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {



                    super.onServicesDiscovered(gatt, status);
                    System.out.println("Di2 ble services discovered");
                    monitorButtons();



                }

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

                    byte[] newDfly = characteristic.getValue();

                    if (newDfly.length < 5) {
                        manageButton = false;
                        return;
                    }

                    //System.out.println("dlfy event old val " + Arrays.toString(lastDfly));
                    //System.out.println("dlfy event new val " + Arrays.toString(newDfly));

                    if (newDfly[1] == -16 && newDfly[2] == -16 && newDfly[3] == -16 && newDfly[4] == -16) {

                        Log.d("log", "Dfly : receive init value, ignore.");
                        lastDfly = newDfly;
                        return;
                    }

                    if (manageButton) {
                        System.out.println("Already handling button...");
                        lastDfly = newDfly;
                        return;
                    }
                    manageButton = true;
                    if (lastPress > 0) {
                        long timeDiff = System.currentTimeMillis() - lastPress;
                        if (timeDiff < 5000) {
                            if (!tooMuchPressDisplayed) {
                                System.out.println("Too much button push, ignore");
                                tooMuchPressDisplayed = true;
                            }
                            manageButton = false;
                            lastDfly = newDfly;
                            return;
                        }
                        else {
                            tooMuchPressDisplayed = false;
                        }
                    }

                    super.onCharacteristicChanged(gatt, characteristic);
                    //Log.d("tag", "onCharacteristicChanged");
                    for (int i = 1; i < 5; i++) {
                        if (newDfly[i] != lastDfly[i]) {
                            ButtonPressType buttonPressType = InterpretPressType(newDfly[i]);
                            System.out.println("D-Fly button " + buttonPressType.toString().toLowerCase() + " press ! Chan" + i);
                            if (i == dflyChan) {
                                if (buttonPressType == ButtonPressType.SIMPLE) {
                                    MainActivity.playSound(R.raw.beep);
                                    goProBle.ToggleRecord(true);
                                }
                                if (buttonPressType == ButtonPressType.DOUBLE) {
                                    MainActivity.playSound(R.raw.doublebeep);
                                    goProBle.TakePicture();
                                }
                                if (buttonPressType == ButtonPressType.LONG) {
                                    MainActivity.playSound(R.raw.longbeep);
                                    if (goProBle.ConnectedAndReady)
                                        goProBle.Sleep();
                                    else
                                        goProBle.Connect();
                                }
                                lastPress = System.currentTimeMillis();
                                break;
                            }
                        }
                    }
                    lastDfly = newDfly;
                    manageButton = false;
                }


            });

            Runnable action = new Runnable() {
                @Override
                public void run() {
                    b.setEnabled(true);
                }
            };
            MainActivity activity = MainActivity.getMainActivity();
            if (activity != null) {
                activity.runOnUiThread(action);
            }


            //di2Gatt.connect();

        }
    }

}
