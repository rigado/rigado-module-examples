package com.rigado.androidbtle;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.rigado.rigablue.IRigFirmwareUpdateManagerObserver;
import com.rigado.rigablue.IRigLeBaseDeviceObserver;
import com.rigado.rigablue.IRigLeConnectionManagerObserver;
import com.rigado.rigablue.IRigLeDiscoveryManagerObserver;
import com.rigado.rigablue.RigAvailableDeviceData;
import com.rigado.rigablue.RigDeviceRequest;
import com.rigado.rigablue.RigFirmwareUpdateManager;
import com.rigado.rigablue.RigLeBaseDevice;
import com.rigado.rigablue.RigLeConnectionManager;
import com.rigado.rigablue.RigLeDiscoveryManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class MainActivity extends ActionBarActivity
        implements IRigFirmwareUpdateManagerObserver,
        IRigLeDiscoveryManagerObserver,
        IRigLeConnectionManagerObserver,
        IRigLeBaseDeviceObserver,
        View.OnClickListener {

    // constants
    private final String TAG = getClass().getSimpleName();

    private final static int BTLE_CONNECT_TIMEOUT_MS = 10000;//10 sec
    private final static String BTLE_DEFAULT_DEVICE_NAME = "RigDfu";

    /*TODO: This threshold can be raised to force devices to be within close proximity of the Android
    device performing the update */
    private final static int RSSI_UPDATE_THRESHOLD = -128;

    //TODO: Update the following strings to match the UUIDs of your custom service
    private final static String BTLE_SERVICE_UUID = "00001530-1212-efde-1523-785feabcd123"; //RigDfu
    private static final String BTLE_CONTROL_POINT_UUID = "00001531-1212-efde-1523-785feabcd123";

    //TODO: Update this command to match the reset command for your device
    private final static byte [] bootloader_command = { 0x03, 0x56, 0x30, 0x57 };

    // members
    private RigFirmwareUpdateManager mRigFirmwareUpdateManager;
    private JsonFirmwareReader mJsonFirmwareReader;
    private ArrayList<JsonFirmwareType> mJsonFirmwareTypeList;
    private int mLastProgressIndication = -1;
    private RigAvailableDeviceData mRigAvailableDeviceData;
    private boolean mIsConnectionInProgress;
    private RigLeBaseDevice mRigLeBaseDevice;//currently connected device
    private Utilities mUtilities;

    // UI references
    private NumberPicker mFirmwarePicker;
    private ProgressBar mProgressBar;
    private TextView mTextViewStatus;
    private TextView mTextViewDeviceName;
    private TextView mTextViewManufacturerName;
    private Button mButtonDeploy;
    private boolean mIsUpdateInProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // initialization
        mUtilities = new Utilities();

        // UI references
        mFirmwarePicker = (NumberPicker) findViewById(R.id.id_picker_firmware);
        mProgressBar = (ProgressBar) findViewById(R.id.id_progress_deployment);
        mTextViewStatus = (TextView) findViewById(R.id.id_tv_status);
        mTextViewDeviceName = (TextView) findViewById(R.id.id_tv_device_name);
        mTextViewManufacturerName = (TextView) findViewById(R.id.id_tv_mfg_name);
        mButtonDeploy = (Button) findViewById(R.id.id_btn_begin_deploy);
        mIsUpdateInProgress = false;

        mButtonDeploy.setOnClickListener(this);

        // read list of available firmwares - these are listed in res/raw/firmware_descriptions.json
        /* TODO: Update raw or assets folder to contain your binary and update firmware_descriptions.json to have
           the necessary information regarding the update binary.  Note: Update binaries must be
           binary files generated using the genimage.py Python script.  See Getting Started with the
           Rigado Secure Bootloader for more details.
         */
        mJsonFirmwareReader = new JsonFirmwareReader();
        mJsonFirmwareTypeList = mJsonFirmwareReader.getFirmwareList(this);

        // show firmware in Picker
        final String[] arrayFirmwareNames = new String[mJsonFirmwareTypeList.size()];
        for(int index=0; index<mJsonFirmwareTypeList.size(); index++) {
            final JsonFirmwareType firmwareRecord = mJsonFirmwareTypeList.get(index);
            arrayFirmwareNames[index] = firmwareRecord.getFwname() + firmwareRecord.getProperties().getVersion();
        }
        mFirmwarePicker.setMinValue(0);
        mFirmwarePicker.setMaxValue(mJsonFirmwareTypeList.size()-1);
        mFirmwarePicker.setDisplayedValues(arrayFirmwareNames);
        mFirmwarePicker.post(new Runnable() {
            @Override
            public void run() {
                if (mJsonFirmwareTypeList.size() > 1) {
                    mFirmwarePicker.setValue(1);// auto select the 2nd item in the list to make the UI more identifiable to the user
                }
            }
        });

        // set up the BTLE
        RigLeConnectionManager.getInstance().setObserver(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_activity_main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.id_menu_search) {
            scanForDevicesIfAllowed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onClick(View view) {
        if (view == mButtonDeploy) {
            if (mRigLeBaseDevice == null) {
                // it's null if there's no connection so show a message to connect first
                Toast.makeText(getApplicationContext(), R.string.txt_error_device_not_connected, Toast.LENGTH_LONG).show();
            } else {
                updateFirmware();
            }
        }
    }

    private void updateFirmware() {
        // get selected firmware from picker
        final int selectedIndex = mFirmwarePicker.getValue();
        final JsonFirmwareType firmwareRecord = mJsonFirmwareTypeList.get(selectedIndex);

        // initialize FW Manager
        mRigFirmwareUpdateManager = new RigFirmwareUpdateManager();
        mRigFirmwareUpdateManager.setObserver(this);

        BluetoothGattService resetService = null;
        BluetoothGattCharacteristic resetChar = null;

        for(BluetoothGattService service : mRigLeBaseDevice.getServiceList()) {
            switch (service.getUuid().toString()) {
                case BTLE_SERVICE_UUID:
                    resetService = service;
                    break;
            }

        }

        if(resetService == null) {
            Toast.makeText(getApplicationContext(), "Bluetooth service for Reset command not found!", Toast.LENGTH_LONG).show();
            return;
        }

        resetChar = resetService.getCharacteristic(UUID.fromString(BTLE_CONTROL_POINT_UUID));
        if(resetChar == null) {
            Toast.makeText(getApplicationContext(), "Bluetooth characteristic for Reset command not found!", Toast.LENGTH_LONG).show();
            return;
        }

        //part of the firmware update process may require scanning
        if(Utilities.hasPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) && Utilities.isLocationEnabled(this)) {
            mUtilities.startFirmwareUpdate(this, mRigFirmwareUpdateManager, mRigLeBaseDevice, firmwareRecord, resetChar, bootloader_command);
            mIsUpdateInProgress = true;
        } else {
            loadPermissionsActivity();
        }

    }


    /**
     * update the text on the status field (R.id.id_tv_status)
     * @param status
     */
    private void showStatus(final String status) {
        // UI widgets must be updated from UI thread
        mTextViewStatus.post(new Runnable() {
            @Override
            public void run() {
                mTextViewStatus.setText(status);
            }
        });
    }
    private void showStatus(final int statusid) {
        // UI widgets must be updated from UI thread
        mTextViewStatus.post(new Runnable() {
            @Override
            public void run() {
                mTextViewStatus.setText(statusid);
            }
        });
    }

    public void discoverStartBTLEdevice(int timeout) {
        RigDeviceRequest request = new RigDeviceRequest(new String[] {BTLE_SERVICE_UUID}, timeout);
        request.setObserver(this);
        RigLeDiscoveryManager.getInstance().startDiscoverDevices(request);
        // NOTE: next expected callback to trigger is likely to be didDiscoverDevice()
    }

    public void discoverStopBTLEdevice() {
        RigLeDiscoveryManager.getInstance().stopDiscoveringDevices();
    }

    public void connectBTLEdevice(RigAvailableDeviceData availableDevice, int timeout) {
        RigLeConnectionManager.getInstance().connectDevice(availableDevice, timeout);
    }

    public void disconnectBTLEdevice(RigLeBaseDevice device) {
        RigLeConnectionManager.getInstance().disconnectDevice(device);
    }

    // Concrete implementations for the IRigFirmwareUpdateManagerObserver interface
    @Override
    public void updateProgress(final int progress) {

        //only update progress if there really was visible progress
        if (mLastProgressIndication != progress) {
            mLastProgressIndication = progress;

            // UI widgets must be updated from UI thread
            mProgressBar.post(new Runnable() {
                @Override
                public void run() {
                    mProgressBar.setProgress(progress);
                }
            });
        }
    }

    @Override
    public void updateStatus(final String status, int error) {
        showStatus(status);
    }

    @Override
    public void didFinishUpdate() {

        // reset the UI and state for next firmware programming
        mLastProgressIndication = -1;
        mIsUpdateInProgress = false;
        showStatus(R.string.txt_tv_status_idle);
    }

    @Override
    public void updateFailed(int error) {

    }
    // end: Concrete implementations for the IRigFirmwareUpdateManagerObserver interface


    // Concrete implementations for the IRigLeBaseDeviceObserver interface
    @Override
    public void didUpdateValue(RigLeBaseDevice device, BluetoothGattCharacteristic characteristic) {

    }

    @Override
    public void didUpdateNotifyState(RigLeBaseDevice device, BluetoothGattCharacteristic characteristic) {

    }

    @Override
    public void didWriteValue(RigLeBaseDevice device, BluetoothGattCharacteristic characteristic) {

    }

    @Override
    public void discoveryDidComplete(RigLeBaseDevice device) {
        Log.d(TAG, "discoveryDidComplete");
        // update UI with device name
        mTextViewDeviceName.post(new Runnable() {
            @Override
            public void run() {
                mTextViewDeviceName.setText(mRigLeBaseDevice.getName());
            }
        });

        //update UI with device manufacturer
        final String strMfgName = mUtilities.getManufacturerName(mRigLeBaseDevice);
        mTextViewManufacturerName.post(new Runnable() {
            @Override
            public void run() {
                mTextViewManufacturerName.setText(strMfgName);
            }
        });

        showStatus(R.string.txt_status_connected);
    }
    // end: Concrete implementations for the IRigLeBaseDeviceObserver interface


    // Concrete implementation for the IRigLeConnectionManagerObserver interface
    @Override
    public void didConnectDevice(final RigLeBaseDevice device) {
        discoverStopBTLEdevice();
        mIsConnectionInProgress = false;

        // store connected device for later
        mRigLeBaseDevice = device;
        mRigLeBaseDevice.setObserver(this);
        mRigLeBaseDevice.runDiscovery();


    }

    @Override
    public void didDisconnectDevice(BluetoothDevice btDevice) {
        if(mIsUpdateInProgress) {
            /* When update is in progress, ignore this message since the disconnect is due to
             * resetting in to the bootloader.
             */
             return;
        }
        mIsConnectionInProgress = false;
        mRigLeBaseDevice = null;

        // clear device name
        mTextViewDeviceName.post(new Runnable() {
            @Override
            public void run() {
                mTextViewDeviceName.setText(R.string.txt_default_device_name);
            }
        });

        //clear device manufacturer
        mTextViewManufacturerName.post(new Runnable() {
            @Override
            public void run() {
                mTextViewManufacturerName.setText(R.string.txt_default_mfg_name);
            }
        });

        showStatus(R.string.txt_status_disconnected);
    }

    @Override
    public void deviceConnectionDidFail(RigAvailableDeviceData device) {
        mIsConnectionInProgress = false;
        showStatus(R.string.txt_status_disconnected);
    }

    @Override
    public void deviceConnectionDidTimeout(RigAvailableDeviceData device) {
        mIsConnectionInProgress = false;
        showStatus(R.string.txt_status_timeout);
    }
    // end: Concrete implementation for the IRigLeConnectionManagerObserver interface


    // Concrete implementation for the IRigLeDiscoveryManagerObserver interface
    @Override
    public void didDiscoverDevice(RigAvailableDeviceData device) {

        if (mIsConnectionInProgress) {
            return;
        }

        // for demo purposes only, check how close the device is and only connect if it's close
        if (device.getRssi() > RSSI_UPDATE_THRESHOLD) {

            // for demo purposes, only connected to "RigDfu" device
            //if (device.getBluetoothDevice().getName().equals(BTLE_DEFAULT_DEVICE_NAME)) {

                // automatically connect
                mIsConnectionInProgress = true;

                RigLeConnectionManager.getInstance().connectDevice(device, BTLE_CONNECT_TIMEOUT_MS);
                //NOTE: next expected callback to trigger is likely to be didConnectDevice()
            //}
        }
    }


    @Override
    public void discoveryDidTimeout() {

        if (mIsConnectionInProgress) {
            Log.d(TAG, "Discover timeout occurred, but connection is in progress");
            return;
        }

        scanForDevicesIfAllowed(); //always check permissions before scanning
    }

    @Override
    public void bluetoothPowerStateChanged(boolean enabled) {
        //Nothing to do here
    }

    @Override
    public void bluetoothDoesNotSupported() {
        // you will see this if:
        // 1) Bluetooth permission is not set in manifest
        // 2) RigaBlue is not initialized in the Application class
        showStatus(R.string.txt_error_btnotsupported);
    }
    // end: Concrete implementation for the IRigLeDiscoveryManagerObserver interface

    private final static int BTLE_SEARCH_TIMEOUT_MS = 20000;//20sec

    private void scanForDevicesIfAllowed() {
        if(!Utilities.hasPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ||
                !Utilities.isLocationEnabled(this)) {
            loadPermissionsActivity();
        } else {
            discoverStartBTLEdevice(BTLE_SEARCH_TIMEOUT_MS);
        }
    }

    private void loadPermissionsActivity() {
        Intent intent = new Intent(this, PermissionsActivity.class);
        startActivity(intent);
        finish(); //clear backstack
    }
}
