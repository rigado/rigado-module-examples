package com.rigado.androidbtle;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;

/**
 * Reads the JSON in res/raw and returns list of firmware files
 */
public class JsonFirmwareReader {

    // members
    private Gson mGson;
    private String TAG = getClass().getSimpleName();

    // constructor
    public JsonFirmwareReader() {
        mGson = new Gson();
    }

    /**
     * read JSON firmware configuration and return it as a handy ArrayList
     * @param context Activity context
     * @return ArrayList<JsonFirmwareType> or NULL on error
     */
    public ArrayList<JsonFirmwareType> getFirmwareList(Context context) {

        ArrayList<JsonFirmwareType> returnData = null;

        try {
            InputStream is = context.getResources().openRawResource(R.raw.firmware_descriptions);
            byte[] buffer = new byte[is.available()];
            while (is.read(buffer) != -1);
            String strJson = new String(buffer);

            Type collectionType = new TypeToken<ArrayList<JsonFirmwareType>>(){}.getType();
            returnData = mGson.fromJson(strJson, collectionType);

        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }

        return returnData;
    }

}
