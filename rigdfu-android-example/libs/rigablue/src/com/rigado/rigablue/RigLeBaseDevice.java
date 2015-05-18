package com.rigado.rigablue;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Created by Ilya_Bogdan on 7/8/2013.
 */
public class RigLeBaseDevice implements IRigCoreBluetoothDeviceObserver {
    private List<BluetoothGattService> mBluetoothGattServices;
    private BluetoothDevice mBluetoothDevice;
    private String mName;
    private IRigLeBaseDeviceObserver mObserver;
    private byte[] mScanRecord;
    private boolean mIsDiscoveryComplete;
    private int mSerivceIndex;
    private int mCharacteristicIndex;

    public RigLeBaseDevice(BluetoothDevice bluetoothDevice, List<BluetoothGattService> serviceList, byte[] scanRecord) {
        mBluetoothDevice = bluetoothDevice;
        mName = mBluetoothDevice.getName();
        mBluetoothGattServices = new ArrayList<BluetoothGattService>();
        if (serviceList != null) {
            UUID mGapUuid = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb");
            UUID mGattUuid = UUID.fromString("00001801-0000-1000-8000-00805f9b34fb");
            for(BluetoothGattService service : serviceList) {
                if(service.getUuid().equals(mGapUuid) ||
                   service.getUuid().equals(mGattUuid)) {
                    //Ignore GAP and GATT services for now
                    continue;
                }
                mBluetoothGattServices.add(service);
            }
        }
        mScanRecord = scanRecord;
        mIsDiscoveryComplete = false;
    }

    @Override
    public String toString() {
        return (mName + ":" + mBluetoothDevice.getAddress());
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof RigLeBaseDevice) {
            RigLeBaseDevice device = (RigLeBaseDevice)o;
            return mBluetoothDevice.equals(device.getBluetoothDevice());
        }
        return false;
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

    public byte[] getScanRecord() { return mScanRecord; }

    public boolean isDiscoveryComplete() {
        return mIsDiscoveryComplete;
    }

    public void runDiscovery() {
        RigLog.d("__runDiscovery__");
        mSerivceIndex = 0;
        mCharacteristicIndex = 0;

        didUpdateValue(null, null);
    }



    public void setObserver(IRigLeBaseDeviceObserver observer) {
        mObserver = observer;
    }

    public boolean readCharacteristic(BluetoothGattCharacteristic characteristic) {
        RigLog.d("RigLeBaseDevice.readCharacteristic");
        if((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) == 0) {
            RigLog.e("Read property not set -- ignoring read request! " + characteristic.getUuid());
            return false;
        }
        RigCoreBluetooth.getInstance().readCharacteristic(mBluetoothDevice, characteristic);
        return true;
    }

    public void writeCharacteristic(BluetoothGattCharacteristic characteristic, byte [] value) {
        RigLog.d("RigLeBaseDevice.writeCharacteristic");
        int props = characteristic.getProperties();
        if(((props & BluetoothGattCharacteristic.PROPERTY_WRITE) == 0) &&
                ((props & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) == 0)) {
            RigLog.e("Write properties not set -- ignoring write request!" + characteristic.getUuid());
            return;
        }
        RigCoreBluetooth.getInstance().writeCharacteristic(mBluetoothDevice, characteristic, value);
    }

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        RigLog.d("RigLeBaseDevice.setCharacteristicNotification");
        if((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0) {
            RigLog.e("Notify property not set -- ignoring notify request!" + characteristic.getUuid());
            return;
        }
        RigCoreBluetooth.getInstance().setCharacteristicNotification(mBluetoothDevice, characteristic, enabled);
    }

    @Override
    public void didUpdateNotificationState(BluetoothDevice btDevice, BluetoothGattCharacteristic characteristic) {
        if(mObserver != null) {
            mObserver.didUpdateNotifyState(this, characteristic);
        }
    }

    @Override
    public void didUpdateValue(BluetoothDevice btDevice, BluetoothGattCharacteristic characteristic) {
        if(mIsDiscoveryComplete) {
            if(mObserver != null) {
                mObserver.didUpdateValue(this, characteristic);
            }
        } else {
            if(characteristic != null) {
                RigLog.i("Read Value during discovery for " + characteristic.getUuid() + ": " + Arrays.toString(characteristic.getValue()));
            }

            /* Find next readable characteristic - This is a tricky bit of code here.  Because
            * a characteristic may not be readable, we need to skip over it.  However, if we
            * do find a characteristic that we can read, the read is issued and the function exits.
            * This is because we need to wait for the BT Stack to inform us the read has
            * completed prior to starting another read. The important point here is that across
            * calls to this function, the characteristic and service indexes are not reset.  This
            * keeps the code from starting the search over every time since the search is actually
            * just being paused while a read completes. */
            while(mSerivceIndex < mBluetoothGattServices.size()) {
                BluetoothGattService service = mBluetoothGattServices.get(mSerivceIndex);
                while (mCharacteristicIndex < service.getCharacteristics().size()) {
                    BluetoothGattCharacteristic c = service.getCharacteristics().get(mCharacteristicIndex);
                    mCharacteristicIndex++;
                    RigLog.d("Characteristic: " + mCharacteristicIndex);
                    if (readCharacteristic(c)) {
                        return;
                    }
                }
                mCharacteristicIndex = 0;
                RigLog.d("Characteristic: " + mCharacteristicIndex);
                mSerivceIndex++;
                RigLog.d("Service: " + mSerivceIndex);
            }

            /* At this point, all services and characteristics have been exhausted. */
            mIsDiscoveryComplete = true;
            if(mObserver != null) {
                RigLog.d("Discovery did complete");
                mObserver.discoveryDidComplete(this);
            }
        }
    }

    @Override
    public void didWriteValue(BluetoothDevice btDevice, BluetoothGattCharacteristic characteristic) {
        if(mObserver != null) {
            mObserver.didWriteValue(this, characteristic);
        }
    }
}
