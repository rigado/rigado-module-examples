package com.rigado.rigablue;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;

/**
 * Created by stutzenbergere on 11/16/14.
 */
public class RigWriteRequest implements IRigDataRequest {

    private BluetoothGattCharacteristic mCharacteristic;
    private BluetoothDevice mDevice;
    private byte [] mValue;

    public RigWriteRequest(BluetoothDevice device, BluetoothGattCharacteristic characteristic, byte [] value) {
        mDevice = device;
        mCharacteristic = characteristic;
        mValue = new byte[value.length];
        System.arraycopy(value, 0, mValue, 0, value.length);
    }

    public BluetoothDevice getDevice() {
        return mDevice;
    }

    public BluetoothGattCharacteristic getCharacteristic() {
        return mCharacteristic;
    }

    public byte [] getValue() {
        return mValue;
    }

    boolean isRequestCharacteristic(BluetoothGattCharacteristic characteristic) {
        if(characteristic == null || mCharacteristic == null) {
            return false;
        }

        if(mCharacteristic.getUuid().equals(characteristic.getUuid())) {
            return true;
        }

        return false;
    }

    boolean isRequestForDevice(BluetoothDevice device) {
        if(device == null || mDevice == null) {
            return false;
        }

        if(mDevice.getAddress().equals(device.getAddress())) {
            return true;
        }

        return false;
    }

    @Override
    public void post(RigService service) {
        if(service == null) {
            return;
        }

        mCharacteristic.setValue(mValue);
        service.writeCharacteristic(mDevice.getAddress(), mCharacteristic);
    }
}
