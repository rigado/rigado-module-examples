package com.rigado.rigablue;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;

/**
 * Created by stutzenbergere on 11/8/14.
 */
public interface IRigCoreBluetoothDeviceObserver {
    public void didUpdateNotificationState(BluetoothDevice btDevice, BluetoothGattCharacteristic characteristic);
    public void didUpdateValue(BluetoothDevice btDevice, BluetoothGattCharacteristic characteristic);
    public void didWriteValue(BluetoothDevice btDevice, BluetoothGattCharacteristic characteristic);

}
