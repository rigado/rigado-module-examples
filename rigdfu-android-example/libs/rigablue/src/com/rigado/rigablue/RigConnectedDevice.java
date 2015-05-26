package com.rigado.rigablue;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattService;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ilya_Bogdan on 7/8/2013.
 */
public class RigConnectedDevice {
    private List<BluetoothGattService> mBluetoothGattServices;
    private BluetoothDevice mBluetoothDevice;
    private String mName;

    public RigConnectedDevice(BluetoothDevice bluetoothDevice, List<BluetoothGattService> serviceList) {
        mBluetoothDevice = bluetoothDevice;
        mName = mBluetoothDevice.getName();
        mBluetoothGattServices = new ArrayList<BluetoothGattService>();
        if (serviceList != null) {
            mBluetoothGattServices.addAll(serviceList);
        }
    }

    /**
     * @return List of discovered services.
     * @method getServiceList
     * @discussion This method provides access to the <code>BluetoothGattService</code> objects discovered during device discovery.  Characteristics can be accessed through these
     * service objects.
     */
    public List<BluetoothGattService> getServiceList() {
        return mBluetoothGattServices;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public BluetoothDevice getBluetoothDevice() {
        return mBluetoothDevice;
    }

    public void setDeviceData(RigConnectedDevice rigConnectedDevice) {
        mName = rigConnectedDevice.getName();
    }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof RigConnectedDevice)) {
            return false;
        }
        RigConnectedDevice rigConnectedDevice = (RigConnectedDevice) o;
        return rigConnectedDevice.getBluetoothDevice().getAddress().equals(this.getBluetoothDevice().getAddress());
    }
}
