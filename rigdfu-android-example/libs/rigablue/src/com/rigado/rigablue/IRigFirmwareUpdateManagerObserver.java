package com.rigado.rigablue;

/**
 * Created by stutzenbergere on 11/19/14.
 */
public interface IRigFirmwareUpdateManagerObserver {
    public void updateProgress(final int progress);
    public void updateStatus(final String status, int error);
    public void didFinishUpdate();
    public void displayUnplugMessage();
    public void dismissUnplugAlert();

}
