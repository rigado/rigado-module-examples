package com.rigado.rigablue;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;

/**
 * Created by stutzenbergere on 11/16/14.
 */
public class RigNotificationStateChangeRequest implements IRigDataRequest {

    BluetoothDevice mDevice;
    BluetoothGattCharacteristic mCharacteristic;
    boolean mEnableState;

    public RigNotificationStateChangeRequest(BluetoothDevice device, BluetoothGattCharacteristic characteristic,
                                             boolean enableState) {
        mDevice = device;
        mCharacteristic = characteristic;
        mEnableState = enableState;
    }

    public BluetoothDevice getDevice() {
        return mDevice;
    }

    public BluetoothGattCharacteristic getCharacteristic() {
        return mCharacteristic;
    }

    public boolean getEnableState() {
        return mEnableState;
    }

    @Override
    public void post(RigService service) {
        service.setCharacteristicNotification(mDevice.getAddress(),
                mCharacteristic, mEnableState);
    }
}
