/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rigado.rigablue;

import android.bluetooth.*;
import android.content.Context;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class RigService {
    //private final static String TAG = BluetoothLeService.class.getSimpleName();

    private Context mContext;
//    private static String mPackageName;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private HashMap<String, BluetoothGatt> mBluetoothGattHashMap;
    private HashMap<String, BluetoothGattCallback> mBluetoothGattCallbackHashMap;
    private IRigCoreListener mRigCoreListener;

    public RigService(String packageName, Context context, IRigCoreListener iRigCoreListener) {
        mContext = context;
//        mPackageName = packageName;
        mRigCoreListener = iRigCoreListener;
    }

    private boolean refreshDeviceCache(BluetoothGatt gatt){
        RigLog.d("refreshDeviceCache");
        try {
            Method localMethod = gatt.getClass().getMethod("refresh", new Class[0]);
            if (localMethod != null) {
                boolean bool = ((Boolean) localMethod.invoke(gatt, new Object[0])).booleanValue();
                return bool;
            }
        }
        catch (Exception localException) {
            RigLog.e("An exception occurred while refreshing device");
        }
        return false;
    }

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        RigLog.d("initialize");
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.

        mBluetoothGattHashMap = new HashMap<String, BluetoothGatt>();
        mBluetoothGattCallbackHashMap = new HashMap<String, BluetoothGattCallback>();
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                RigLog.e("Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            RigLog.e("Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    public int getConnectionStateForDevice(BluetoothDevice device) {
        BluetoothGatt gatt = mBluetoothGattHashMap.get(device.getAddress());
        return gatt.getConnectionState(device);
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public synchronized boolean connect(final String address) {
        RigLog.d("connect");
        if (mBluetoothAdapter == null || address == null) {
            RigLog.i("BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if(device == null) {
            RigLog.e("Could not get remote device!");
            return false;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                    if (mBluetoothGattHashMap.containsKey(address)) {
                        RigLog.w("Device already connected.");
                    } else {
                        RigLog.d("Trying to create a new connection.");
                        BluetoothGattCallback callback = new RigBluetoothGattCallback(mRigCoreListener, mBluetoothGattHashMap, mBluetoothGattCallbackHashMap);
                        if(mBluetoothGattCallbackHashMap.containsKey(address)) {
                            mBluetoothGattCallbackHashMap.remove(address);
                            mBluetoothGattCallbackHashMap.put(address, callback);
                        }

                        BluetoothGatt gatt = device.connectGatt(mContext, false, callback);
                        if(gatt != null) {
                            refreshDeviceCache(gatt);
                        }

                    }
            }
        }).start();
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public synchronized void disconnect(final String address) {
        RigLog.d("disconnect");
        if (mBluetoothAdapter == null) {
            RigLog.e("BluetoothAdapter not initialized");
            return;
        }
        if (mBluetoothGattHashMap.get(address) == null) {
            RigLog.w(address + "is not connected!");
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                BluetoothGatt gatt = mBluetoothGattHashMap.get(address);
                if(gatt != null) {
                    mBluetoothGattHashMap.get(address).disconnect();
                }
            }
        }).start();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public synchronized void close() {
        RigLog.d("close");
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (BluetoothGatt bluetoothGatt : mBluetoothGattHashMap.values()) {
                    if (bluetoothGatt != null) {
                        bluetoothGatt.close();
                    }
                }
                mBluetoothGattHashMap.clear();
            }
        }).start();
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public synchronized void readCharacteristic(final String address, final BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGattHashMap.get(address) == null) {
            RigLog.e("BluetoothAdapter not initialized or device already disconnected");
            return;
        }

        if(characteristic == null) {
            RigLog.e("Invalid characteristic; Characteristic is null!");
            return;
        }

        RigLog.d("readCharacteristic - " + Arrays.toString(characteristic.getValue()));
        mBluetoothGattHashMap.get(address).readCharacteristic(characteristic);
    }

    public synchronized void writeCharacteristic(final String address, final BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGattHashMap.get(address) == null) {
            RigLog.w("BluetoothAdapter not initialized or device already disconnected");
            return;
        }

        if(characteristic == null) {
            RigLog.e("Invalid characteristic; Characteristic is null!");
            return;
        }

        RigLog.i("writeCharacteristic for " + address + " with value - " + Arrays.toString(characteristic.getValue()));

        if(!mBluetoothGattHashMap.get(address).writeCharacteristic(characteristic)) {
            RigLog.e("writeCharacteristic failed!");
        }
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification.  False otherwise.
     */
    public synchronized void setCharacteristicNotification(final String address, final BluetoothGattCharacteristic characteristic, final boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGattHashMap.get(address) == null) {
            RigLog.w("BluetoothAdapter not initialized or device already disconnected");
            return;
        }

        if(characteristic == null) {
            RigLog.e("Invalid characteristic; Characteristic is null!");
            return;
        }

        RigLog.d("setCharacteristicNotification - " + Arrays.toString(characteristic.getValue()));
        mBluetoothGattHashMap.get(address).setCharacteristicNotification(characteristic, enabled);
        for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
            if (descriptor != null) {
                RigLog.d("descriptor = " + descriptor.getUuid());
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                mBluetoothGattHashMap.get(address).writeDescriptor(descriptor);
            } else {
                RigLog.w("descriptor = null");
            }
        }
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices(final String address) {
        if (mBluetoothGattHashMap.get(address) == null) {
            return null;
        }
        return mBluetoothGattHashMap.get(address).getServices();
    }
}
