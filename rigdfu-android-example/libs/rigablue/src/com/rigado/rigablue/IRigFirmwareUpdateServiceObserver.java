package com.rigado.rigablue;

/**
 * Created by stutzenbergere on 11/19/14.
 */
public interface IRigFirmwareUpdateServiceObserver {
    public void didEnableControlPointNotifications();
    public void didUpdateValueForControlPoint(byte [] value);
    public void didWriteValueForControlPoint();
    public void didConnectPeripheral();
    public void didDisconnectPeripheral();
    public void didDiscoverCharacteristicsForDFUService();
}
