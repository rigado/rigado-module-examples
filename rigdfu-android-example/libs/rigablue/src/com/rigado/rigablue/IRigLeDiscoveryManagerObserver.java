package com.rigado.rigablue;

/**
 * Created by stutzenbergere on 11/8/14.
 */
public interface IRigLeDiscoveryManagerObserver {
    public void didDiscoverDevice(RigAvailableDeviceData device);
    public void discoveryDidTimeout();
    public void bluetoothPowerStateChanged(boolean enabled);
    public void bluetoothDoesNotSupported();
}
