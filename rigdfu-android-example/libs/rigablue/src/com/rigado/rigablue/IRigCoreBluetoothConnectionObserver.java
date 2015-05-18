package com.rigado.rigablue;

import android.bluetooth.BluetoothDevice;

/**
 * Created by Ilya_Bogdan on 7/8/2013.
 */
public interface IRigCoreBluetoothConnectionObserver extends IRigCoreBluetoothCommon{
    public void didConnectDevice(BluetoothDevice btDevice);
    public void connectionDidTimeout(BluetoothDevice btDevice);
    public void didDisconnectDevice(BluetoothDevice btDevice);
    public void didFailToConnectDevice(BluetoothDevice btDevice);
}
