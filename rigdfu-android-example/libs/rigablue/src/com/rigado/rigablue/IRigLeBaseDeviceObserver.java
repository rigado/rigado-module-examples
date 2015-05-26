package com.rigado.rigablue;

import android.bluetooth.BluetoothGattCharacteristic;

/**
 * Created by stutzenbergere on 11/8/14.
 */
public interface IRigLeBaseDeviceObserver {
    void didUpdateValue(RigLeBaseDevice device, BluetoothGattCharacteristic characteristic);
    void didUpdateNotifyState(RigLeBaseDevice device, BluetoothGattCharacteristic characteristic);
    void didWriteValue(RigLeBaseDevice device, BluetoothGattCharacteristic characteristic);
    void discoveryDidComplete(RigLeBaseDevice device);
}
