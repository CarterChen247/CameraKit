package me.kazaf.camerakit.util;

import android.util.Log;

/**
 * Created by kazaf on 2018/3/30.
 */

public class CameraLogger {

    public static final boolean isLoggingEnabled = false;

    public static void log(String tag, String msg){
        if (isLoggingEnabled){
            CameraLogger.log(tag, msg);
        }
    }
}
