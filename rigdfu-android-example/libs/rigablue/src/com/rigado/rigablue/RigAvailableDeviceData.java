package com.rigado.rigablue;

import android.bluetooth.BluetoothDevice;

/**
 * Created by Ilya_Bogdan on 7/8/2013.
 */
public class RigAvailableDeviceData {

    private BluetoothDevice mBluetoothDevice;
    private int mRssi;
    private long mDiscoverTime;
    private byte[] mScanRecord;

    public RigAvailableDeviceData(BluetoothDevice bluetoothDevice, int rssi, byte[] scanRecord, long discoverTime) {
        this.mDiscoverTime = discoverTime;
        this.mBluetoothDevice = bluetoothDevice;
        this.mRssi = rssi;
        this.mScanRecord = scanRecord;
    }

    public int getRssi() {
        return mRssi;
    }

    public long getDiscoverTime() {
        return mDiscoverTime;
    }

    public byte[] getScanRecord() {
        return mScanRecord;
    }

    public BluetoothDevice getBluetoothDevice() {
        return mBluetoothDevice;
    }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof RigAvailableDeviceData)) {
            return false;
        }
        RigAvailableDeviceData deviceData = (RigAvailableDeviceData) o;
        return deviceData.getBluetoothDevice().getAddress().equals(mBluetoothDevice.getAddress());
    }

    @Override
    public String toString() {
        if(mBluetoothDevice == null) {
            return "";
        }
        return mBluetoothDevice.getAddress();
    }
}
