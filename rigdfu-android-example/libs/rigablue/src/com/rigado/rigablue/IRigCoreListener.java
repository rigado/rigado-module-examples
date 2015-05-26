package com.rigado.rigablue;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

/**
 * Created by Ilya_Bogdan on 9/2/2014.
 */
public interface IRigCoreListener {

    public void onActionGattReadRemoteRssi(BluetoothDevice bluetoothDevice, int rssi);
    public void onActionGattConnected(BluetoothDevice bluetoothDevice);
    public void onActionGattDisconnected(BluetoothDevice bluetoothDevice);
    public void onActionGattFail(BluetoothDevice bluetoothDevice);
    public void onActionGattServicesDiscovered(BluetoothDevice bluetoothDevice);
    public void onActionGattDataAvailable(BluetoothGattCharacteristic characteristic, BluetoothDevice bluetoothDevice);
    public void onActionGattDataNotification(BluetoothGattCharacteristic characteristic, BluetoothDevice bluetoothDevice);
    public void onActionGattDescriptorWrite(BluetoothGattDescriptor descriptor, BluetoothDevice bluetoothDevice);
    public void onActionGattCharWrite(BluetoothDevice bluetoothDevice, BluetoothGattCharacteristic characteristic);
}
