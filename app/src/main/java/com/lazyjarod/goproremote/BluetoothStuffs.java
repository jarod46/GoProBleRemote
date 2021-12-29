package com.lazyjarod.goproremote;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.widget.TextView;

import java.util.UUID;

public class BluetoothStuffs {

    public void Record(Context ct){
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice gopro = null;
        for (BluetoothDevice device : adapter.getBondedDevices())
        {
            if (device.getName().toLowerCase().contains("gopro"))
            {
                gopro = device;
                break;
            }
        }
        if (gopro != null)
        {
            System.out.print("test");

            BluetoothGatt gatt = gopro.connectGatt(ct,false, new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    super.onConnectionStateChange(gatt, status, newState);
                }

                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    super.onServicesDiscovered(gatt, status);
                }


            });

            //TextView label = findViewById(R.id.textView);

            boolean connected = gatt.connect();
            //label.setText("connected : " + connected);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            long time = System.currentTimeMillis();

            gatt.discoverServices();

            BluetoothGattCharacteristic command = null;

            while (command == null) {
                for (BluetoothGattService service : gatt.getServices()) {
                    for (BluetoothGattCharacteristic charac : service.getCharacteristics()) {
                        if (charac.getUuid().equals(UUID.fromString("b5f90072-aa8d-11e3-9046-0002a5d5c51b"))) {
                            command = charac;
                            break;
                        }
                    }
                }
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (System.currentTimeMillis() - 10000 > time) {
                    //label.setText("get command timeout");
                    break;
                }
                //if (command == null) {
                //   gatt.discoverServices();
                //}

            }


            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


            if (command != null)
            {
                time = System.currentTimeMillis();

                command.setValue(new byte[] {3,1,1,1});
                while (!gatt.writeCharacteristic(command)) {

                    if (System.currentTimeMillis() - 6000 > time) {
                        //label.setText("start timeout");
                        break;
                    }
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }


            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            gatt.disconnect();
        }
    }

}
