package com.rigado.androidbtle;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.Toast;


/** Send users to this activity if they have denied location permissions.
 * Scanning with BLE requires ACCESS_COARSE_LOCATION or ACCESS_FINE_LOCATION.
 * Rigablue's firmwareUpdate() method scans for the RigDfu device, so you must
 * have permissions calling update firmware. */

public class PermissionsActivity extends AppCompatActivity {
    private final String[] locationPermission = {Manifest.permission.ACCESS_COARSE_LOCATION};
    private final static int LOCATION_REQUEST_CODE = 101;
    private final static int LOCATION_RESULT_CODE = 0x0F;

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        setContentView(R.layout.activity_permissions);

        checkForPermissions();
    }

    private void checkForPermissions() {
        /**
         * Apps installed on Android devices with API 6.0+ require Location permissions and for
         * Location to be turned on to discover devices. The Rigablue library uses the scanning
         * feature after disconnecting from the device in order to locate the RigDfu bootloader.
         * Location is a dangerous permission, and users can revoke permissions or turn it off at any time.
         * Apps should always check for permissions before beginning the discovery process.
         *
         * https://developer.android.com/training/permissions/requesting.html
         *
         * https://code.google.com/p/android/issues/detail?id=189090&q=ble%20android%206.0&colspec=ID%20Type%20Status%20Owner%20Summary%20Stars
         *
         * */
        if(Utilities.hasPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            //If we already have permission, check if location is turned on
            checkLocationStatus();

        } else {
            //Else, if we do not have permission, & the user has previously denied our request,
            // show a dialog explaining why we need access to location.
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                alertUserPermissionRequestRationale();
            } else {
                //Else, request access to location
                ActivityCompat.requestPermissions(this, locationPermission, LOCATION_REQUEST_CODE);
            }

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case LOCATION_REQUEST_CODE:
                if(grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //If permission was granted, check if Location is turned on before loading UI
                    checkLocationStatus();
                } else {
                    if(ActivityCompat.shouldShowRequestPermissionRationale(PermissionsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                        //If permission was denied, explain why we need access to location
                        alertUserPermissionRequestRationale();
                    } else {
                        //The user has permanently denied access and will have to manually
                        //update their Settings to connect.
                        Toast.makeText(PermissionsActivity.this, "Please enable location services to connect.", Toast.LENGTH_LONG).show();
                    }
                }
                break;
        }
    }


    private void alertUserPermissionRequestRationale() {
        new AlertDialog.Builder(this)
                .setTitle("BLE Scanning Unavailable")
                .setMessage("Marshmallow+ requires Location services to scan for BLE devices. Please enable location to continue.")
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        //Once we have explained why we need access to Location, request it again
                        ActivityCompat.requestPermissions(PermissionsActivity.this, locationPermission, LOCATION_REQUEST_CODE);
                    }
                })
                .show();
    }

    private void checkLocationStatus() {
        LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if(manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) || manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            loadMainActivity();
        } else {
            //If location is not enabled, send the user to Settings
            Toast.makeText(this, "Please turn on location to scan for devices.", Toast.LENGTH_LONG).show();
            startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), LOCATION_RESULT_CODE);
        }
    }

    private void loadMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

}
