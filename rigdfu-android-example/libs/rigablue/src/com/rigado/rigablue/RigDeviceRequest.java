package com.rigado.rigablue;

/**
 * Created by Ilya_Bogdan on 7/8/2013.
 */
public class RigDeviceRequest {

    private String[] mUuidList;
    private int mTimeout;
    private IRigLeDiscoveryManagerObserver mObserver;

    public String[] getUuidList() {
        return mUuidList;
    }

    public int getTimeout() {
        return mTimeout;
    }

    //TODO: Make constructor take observer as well
    public RigDeviceRequest(String[] uuidList, int timeout) {
        mUuidList = uuidList;
        mTimeout = timeout;
    }

    public IRigLeDiscoveryManagerObserver getObserver() {
        return mObserver;
    }

    public void setObserver(IRigLeDiscoveryManagerObserver observer) {
        mObserver = observer;
    }
}
