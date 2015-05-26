package com.rigado.rigablue;

import android.bluetooth.BluetoothDevice;

/**
 * Created by Ilya_Bogdan on 7/8/2013.
 */
public interface IRigCoreBluetoothDiscoveryObserver extends IRigCoreBluetoothCommon {

    public void didDiscoverDevice(BluetoothDevice btDevice, int rssi, byte [] scanRecord);
    public void discoveryFinishedByTimeout();
}
