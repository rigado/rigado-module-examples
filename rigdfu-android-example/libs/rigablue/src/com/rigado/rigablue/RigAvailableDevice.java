package com.rigado.rigablue;

import android.bluetooth.BluetoothDevice;

/**
 * Created by Ilya_Bogdan on 7/8/2013.
 */
public class RigAvailableDevice {

    private BluetoothDevice mBluetoothDevice;
    private int mRssi;
    private long mDiscoverTime;
    private byte[] mScanRecord;

    public RigAvailableDevice(BluetoothDevice bluetoothDevice, int rssi, byte[] scanRecord, long discoverTime) {
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

    public void setDeviceData(RigAvailableDevice rigAvailableDevice) {
        mRssi = rigAvailableDevice.getRssi();
        mDiscoverTime = rigAvailableDevice.getDiscoverTime();
        mScanRecord = rigAvailableDevice.getScanRecord();
    }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof RigAvailableDevice)) {
            return false;
        }
        RigAvailableDevice rigAvailableDevice = (RigAvailableDevice) o;
        return rigAvailableDevice.getBluetoothDevice().getAddress().equals(this.getBluetoothDevice().getAddress());
    }

    @Override
    public String toString() {
        return mBluetoothDevice.getAddress();
    }
}
