package com.rigado.rigablue;

import android.bluetooth.BluetoothDevice;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;

/**
 * Created by Ilya_Bogdan on 7/8/2013.
 */
public class RigLeDiscoveryManager implements IRigCoreBluetoothDiscoveryObserver {

    private volatile ArrayList<RigAvailableDeviceData> mDiscoveredDevices;
    private boolean mIsDiscoveryRunning;
    private IRigLeDiscoveryManagerObserver mObserver;
    private final Semaphore mLock = new Semaphore(1, true);

//    private Handler mHandler;
    private int searchTime;
//    private static final int TIME_TO_COMPARE_AVAILABLE_DEVICES = 3000;
    private List<UUID> uuidArrayList;
    private static RigLeDiscoveryManager instance = null;

    RigLeDiscoveryManager() {
//        mHandler = new Handler();
        mDiscoveredDevices = new ArrayList<RigAvailableDeviceData>();
        RigCoreBluetooth.getInstance().setDiscoveryObserver(this);
    }

    public static RigLeDiscoveryManager getInstance() {
        if(instance == null) {
            instance = new RigLeDiscoveryManager();
        }
        return instance;
    }

    public void startDiscoverDevices(RigDeviceRequest request) {
        RigLog.d("__RigLeDiscoveryManager.startDiscoverDevices__");
        if (request == null || mIsDiscoveryRunning) {
            return;
        }
        searchTime = 0;
        clearAvailableDevices();

        if (request.getTimeout() > 0) {
            //TODO: Create timer to handle timeout of discovery
            searchTime = request.getTimeout();
        }
        uuidArrayList = new ArrayList<UUID>();
        for (String uuid : request.getUuidList()) {
            uuidArrayList.add(UUID.fromString(uuid));
        }

        mIsDiscoveryRunning = true;
        mObserver = request.getObserver();

        RigCoreBluetooth.getInstance().startDiscovery(uuidArrayList.toArray(new UUID[uuidArrayList.size()]), searchTime);
    }

    public void stopDiscoveringDevices() {
        RigLog.d("__RigLeDiscoveryManager.stopDiscoveringDevices__");
        RigCoreBluetooth.getInstance().stopDiscovery();
        mIsDiscoveryRunning = false;
    }

    public ArrayList<RigAvailableDeviceData> getDiscoveredDevices() {
        /* Return a copy of the current device list */
        mLock.acquireUninterruptibly();
        ArrayList<RigAvailableDeviceData> deviceList = new ArrayList<RigAvailableDeviceData>(mDiscoveredDevices);
        mLock.release();
        return deviceList;
    }

    public void clearAvailableDevices() {
        RigLog.d("__RigLeDiscoveryManager.clearAvailableDevices__");
        mLock.acquireUninterruptibly();
        mDiscoveredDevices.clear();
        mLock.release();
    }

    public boolean removeAvailableDevice(RigAvailableDeviceData device) {
        RigLog.d("__RigLeDiscoveryManager.removeAvailableDevice__");
        boolean result;

        mLock.acquireUninterruptibly();
        result = mDiscoveredDevices.remove(device);
        mLock.release();

        return result;
    }

    public void setObserver(IRigLeDiscoveryManagerObserver observer) {
        this.mObserver = observer;
    }

    public boolean isDiscoveryRunning() {
        return mIsDiscoveryRunning;
    }

    /*private void synchronizeAvailableDevices() {
        Iterator<RigAvailableDevice> rigAvailableDeviceIterator = mDiscoveredDevices.iterator();
        while (rigAvailableDeviceIterator.hasNext()) {
            RigAvailableDevice rigAvailableDevice = rigAvailableDeviceIterator.next();

            int index = mDiscoveredDevicesNew.indexOf(rigAvailableDevice);
            if (index < 0) {
                rigAvailableDeviceIterator.remove();
            } else {
                rigAvailableDevice.setDeviceData(mDiscoveredDevicesNew.get(index));
                mDiscoveredDevicesNew.remove(index);
            }
        }

        for (RigAvailableDevice rigAvailableDevice : mDiscoveredDevicesNew) {
            mDiscoveredDevices.add(rigAvailableDevice);
        }

        if (mRigCoreBluetoothDiscoveryObserver != null) {
            mRigCoreBluetoothDiscoveryObserver.didDiscoverDevice(null);
        }
    }*/

    @Override
    public void didDiscoverDevice(BluetoothDevice btDevice, int rssi, byte [] scanRecord) {
        RigLog.d("RigLeDiscoveryManager.didDiscoverDevice");

        boolean found = false;
        //TODO: Is this a necessary check on Android?
        if (rssi > 0) {
            RigLog.d("RigLeDiscoveryManager.rssi > 0");
            /* Sometimes an invalid RSSI is provided by the OS.  Connecting to devices reported in this state will generally result
            * in an unknown connection error. */
            return;
        }

        mLock.acquireUninterruptibly();
        RigLog.d("RigLeDiscoveryManager.didDiscoverDevice:got lock");
        for(RigAvailableDeviceData device : mDiscoveredDevices) {
            if(device.getBluetoothDevice().getAddress().equals(btDevice.getAddress())) {
                found = true;
                break;
            }
        }

        if(!found) {
            RigAvailableDeviceData availableDevice = new RigAvailableDeviceData(btDevice, rssi, scanRecord, System.currentTimeMillis());
            mDiscoveredDevices.add(availableDevice);
            mLock.release();
            if (mObserver != null) {
                mObserver.didDiscoverDevice(availableDevice);
            } else {
                RigLog.d("Observer is null!");
            }
        } else {

            mLock.release();
        }
    }

    @Override
    public void discoveryFinishedByTimeout() {
        RigLog.d("RigLeDiscoveryManager.discoveryFinishedByTimeout");
        mIsDiscoveryRunning = false;
        if (mObserver != null) {
            mObserver.discoveryDidTimeout();
        }
    }

    /*@Override
    public void discoveryFinished() {
        RigLog.d("RigLeDiscoveryManager.discoveryFinished");
        mIsDiscoveryRunning = false;
        if (mObserver != null) {
            mObserver.discoveryFinished();
        }
    }*/

    @Override
    public void bluetoothPowerStateChanged(boolean enabled) {
        RigLog.d("RigLeDiscoveryManager.bluetoothPowerStateChanged");
        if (!enabled) {
            mIsDiscoveryRunning = false;
            clearAvailableDevices();
        }
        if (mObserver != null) {
            mObserver.bluetoothPowerStateChanged(enabled);
        }

    }

    @Override
    public void bluetoothDoesNotSupported() {
        RigLog.d("RigLeDiscoveryManager.bluetoothDoesNotSupported");
        mIsDiscoveryRunning = false;
        clearAvailableDevices();
        if (mObserver != null) {
            mObserver.bluetoothDoesNotSupported();
        }
    }
}
