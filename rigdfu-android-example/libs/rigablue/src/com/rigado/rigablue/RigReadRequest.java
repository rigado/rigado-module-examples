package com.rigado.rigablue;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;

/**
 * Created by stutzenbergere on 11/18/14.
 */
public class RigReadRequest implements IRigDataRequest {

    private BluetoothDevice mDevice;
    private BluetoothGattCharacteristic mCharacteristic;

    public RigReadRequest(BluetoothDevice device, BluetoothGattCharacteristic characteristic) {
        mDevice = device;
        mCharacteristic = characteristic;
    }

    @Override
    public void post(RigService service) {
        service.readCharacteristic(mDevice.getAddress(), mCharacteristic);
    }
}
