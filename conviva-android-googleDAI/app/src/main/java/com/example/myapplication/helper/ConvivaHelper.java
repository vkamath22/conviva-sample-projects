package com.example.myapplication.helper;

import android.content.Context;

import com.conviva.api.SystemSettings;
import com.conviva.sdk.ConvivaAdAnalytics;
import com.conviva.sdk.ConvivaAnalytics;
import com.conviva.sdk.ConvivaSdkConstants;
import com.conviva.sdk.ConvivaVideoAnalytics;

import java.util.HashMap;


public class ConvivaHelper {

    private static final String DEBUG_CUSTOMER_KEY = "";
    private static final String DEBUG_GATEWAY_URL = "";

    public static ConvivaVideoAnalytics sConvivaVideoAnalytics;
    public static ConvivaAdAnalytics sConvivaAdAnalytics;

    public static void init(Context _context) {

        HashMap<String, Object> settings = new HashMap<>();

        // Provide your customer key and gateway url here.
        settings.put(ConvivaSdkConstants.GATEWAY_URL, DEBUG_GATEWAY_URL);

        // In Production the log level need not be set and will be taken as NONE.
        settings.put(ConvivaSdkConstants.LOG_LEVEL, SystemSettings.LogLevel.DEBUG);

        ConvivaAnalytics.init(_context, DEBUG_CUSTOMER_KEY, settings, null);

        sConvivaVideoAnalytics = ConvivaAnalytics.buildVideoAnalytics(_context);
    }

    public static void release(){
        sConvivaAdAnalytics.release();
        sConvivaVideoAnalytics.release();
        ConvivaAnalytics.release();
    }

    public static void deinit(){
        ConvivaAnalytics.release();
    }
}
