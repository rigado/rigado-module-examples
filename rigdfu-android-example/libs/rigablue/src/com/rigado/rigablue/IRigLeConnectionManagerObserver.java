package com.rigado.rigablue;

import android.bluetooth.BluetoothDevice;

/**
 * Created by stutzenbergere on 11/8/14.
 */
public interface IRigLeConnectionManagerObserver {
    void didConnectDevice(RigLeBaseDevice device);
    void didDisconnectDevice(BluetoothDevice btDevice);
    void deviceConnectionDidFail(RigAvailableDeviceData device);
    void deviceConnectionDidTimeout(RigAvailableDeviceData device);
}
