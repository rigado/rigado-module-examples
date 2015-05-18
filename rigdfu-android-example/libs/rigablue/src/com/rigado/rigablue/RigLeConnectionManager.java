package com.rigado.rigablue;

import android.bluetooth.BluetoothDevice;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

/**
 * Created by Ilya_Bogdan on 7/8/2013.
 */
public class RigLeConnectionManager implements IRigCoreBluetoothConnectionObserver {

    private int mMinimumConnectionTimeout;
    private volatile ArrayList<RigLeBaseDevice> mConnectedDevices;
//    private boolean mShouldAutoReconnect;
    private IRigLeConnectionManagerObserver mObserver;
    private RigAvailableDeviceData mConnectingDevice;
    private HashMap<BluetoothDevice, byte[]> mAdvertisingDataList;
    private final Semaphore mLock = new Semaphore(1, true);

    private static RigLeConnectionManager instance = null;

    RigLeConnectionManager() {
//        mShouldAutoReconnect = false;
        RigCoreBluetooth.getInstance().setConnectionObserver(this);
        mConnectedDevices = new ArrayList<RigLeBaseDevice>();
        mAdvertisingDataList = new HashMap<BluetoothDevice, byte[]>();
        mMinimumConnectionTimeout = 5000;
    }

    public static RigLeConnectionManager getInstance()
    {
        if(instance == null)
        {
            instance = new RigLeConnectionManager();
        }
        return instance;
    }

    public void connectDevice(RigAvailableDeviceData device, int timeout) {
        if (timeout != 0 && timeout < mMinimumConnectionTimeout) {
            timeout = mMinimumConnectionTimeout;
        }
        mConnectingDevice = device;
        long connTimeout = timeout;
        byte [] scanRecord = device.getScanRecord();
        if(scanRecord != null) {
            mAdvertisingDataList.put(device.getBluetoothDevice(), scanRecord);
        }

        RigCoreBluetooth.getInstance().connectPeripheral(device.getBluetoothDevice(), connTimeout);
    }

    public void disconnectDevice(RigLeBaseDevice device) {
        RigCoreBluetooth.getInstance().disconnectPeripheral(device.getBluetoothDevice());
    }

    public ArrayList<RigLeBaseDevice> getConnectedDevices() {
        mLock.acquireUninterruptibly();
        ArrayList<RigLeBaseDevice> deviceList = new ArrayList<RigLeBaseDevice>(mConnectedDevices);
        mLock.release();
        return deviceList;
    }

    @Override
    public void didConnectDevice(BluetoothDevice btDevice) {
        byte [] scanRecord;
        scanRecord = mAdvertisingDataList.get(btDevice);
        RigLeBaseDevice baseDevice = new RigLeBaseDevice(btDevice, RigCoreBluetooth.getInstance().getServiceList(btDevice.getAddress()), scanRecord);
        mAdvertisingDataList.remove(btDevice);
        RigAvailableDeviceData toRemove = null;


        mLock.acquireUninterruptibly();
        mConnectedDevices.add(baseDevice);
        mLock.release();

        for(RigAvailableDeviceData availDevice : RigLeDiscoveryManager.getInstance().getDiscoveredDevices())
        {
            if(availDevice.getBluetoothDevice().getAddress().equals(btDevice.getAddress())) {
                toRemove = availDevice;
                break;
            }
        }

        /* Now that a valid connection has been made, remove the device from the available list */
        if(toRemove != null) {
            RigLeDiscoveryManager.getInstance().removeAvailableDevice(toRemove);
        }

        if (mObserver != null) {
            mObserver.didConnectDevice(baseDevice);
        }
    }



    @Override
    public void didDisconnectDevice(BluetoothDevice btDevice) {
        RigLeBaseDevice toRemove = null;

        mLock.acquireUninterruptibly();
        for(RigLeBaseDevice device : mConnectedDevices) {
            if(device.getBluetoothDevice().getAddress().equals(btDevice.getAddress())) {
                toRemove = device;
                break;
            }
        }

        if(toRemove != null) {
            mConnectedDevices.remove(toRemove);
        }
        mLock.release();

        if (mObserver != null) {
            mObserver.didDisconnectDevice(btDevice);
        }
    }

    @Override
    public void connectionDidTimeout(BluetoothDevice btDevice) {
        if (mObserver != null) {
            mObserver.deviceConnectionDidTimeout(mConnectingDevice);
            if(mAdvertisingDataList.containsKey(btDevice)) {
                mAdvertisingDataList.remove(btDevice);
            }
        }
    }

    @Override
    public void didFailToConnectDevice(BluetoothDevice btDevice) {
        if (mObserver != null) {
            mObserver.deviceConnectionDidFail(mConnectingDevice);
            if(mAdvertisingDataList.containsKey(btDevice)) {
                mAdvertisingDataList.remove(btDevice);
            }
        }
    }

    public void setObserver(IRigLeConnectionManagerObserver observer) {
        this.mObserver = observer;
    }

    public IRigLeConnectionManagerObserver getObserver() { return mObserver; }

    @Override
    public void bluetoothPowerStateChanged(boolean enabled) {
        if (!enabled) {
            mConnectedDevices.clear();
        }
    }

    @Override
    public void bluetoothDoesNotSupported() {
        mConnectedDevices.clear();
    }
}
