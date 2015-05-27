package com.rigado.androidbtle;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import com.rigado.rigablue.RigFirmwareUpdateManager;
import com.rigado.rigablue.RigLeBaseDevice;

import java.io.InputStream;
import java.util.List;

/**
 * BTLE Sample app useful methods and functions
 */
public class Utilities {

    // Constants
    private final String TAG = getClass().getSimpleName();
    private final String UUID_DIS_SERVICE = "0000180a";//leading characters in UUID
    private final String UUID_MFG_NAME = "00002a29";//leading characters in UUID

    /**
     * attempt to obtain the manufacturer name
     * @return "[unknown]" if unable to obtain the data, otherwise String
     */
    public String getManufacturerName(RigLeBaseDevice device) {
        BluetoothGattService disService = null;
        String strMfgName = null;
        String strUnknown = "[unknown manufacturer]";

        List<BluetoothGattService> serviceList = device.getServiceList();
        for (BluetoothGattService service : serviceList) {
            if (service.getUuid().toString().startsWith(UUID_DIS_SERVICE)) {
                disService = service;
                break;
            }
        }

        if(disService == null) {
            Log.d(TAG, "Could not find Device Information Service");
        } else {
            List<BluetoothGattCharacteristic> characteristicList = disService.getCharacteristics();
            for (BluetoothGattCharacteristic characteristic : characteristicList) {
                Log.d(TAG, characteristic.getUuid().toString());
                if (characteristic.getUuid().toString().startsWith(UUID_MFG_NAME)) {
                    //it seems that android.bluetooth.BluetoothGattCharacteristic.getStringValue can crash
                    try {
                        strMfgName = characteristic.getStringValue(0);
                    }catch (Exception e) {
                        // do nothing, just don't crash.
                    }
                }
            }
        }

        return strMfgName == null ? strUnknown : strMfgName;// on null return strUnknown
    }

    public void startFirmwareUpdate(Context context, RigFirmwareUpdateManager fwManager, RigLeBaseDevice device, JsonFirmwareType firmwareRecord,
                                    BluetoothGattCharacteristic bootCharacteristic, byte [] bootCommand) {

        if (firmwareRecord != null){

            final String filename = firmwareRecord.getProperties().getFilename1();

            // ensure that the filenames contain no extension
            String strFilenameNoExt1;
            if (filename.contains(".")) {
                strFilenameNoExt1 = filename.substring(0, filename.lastIndexOf('.'));
            } else {
                strFilenameNoExt1 = filename;
            }

            final int deviceFWid = context.getResources().getIdentifier(strFilenameNoExt1, "raw", context.getPackageName());

            InputStream fwImageInputStream = (deviceFWid != 0) ? context.getResources().openRawResource(deviceFWid) : null;

            fwManager.updateFirmware(device, fwImageInputStream, bootCharacteristic, bootCommand);
        } else {
            Log.e(TAG, "Firmware filenames are unknown - were the JSON values read correctly?");
        }
    }
}
