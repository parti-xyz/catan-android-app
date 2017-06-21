package xyz.parti.catan.helper;

import android.util.Log;

import xyz.parti.catan.BuildConfig;
import xyz.parti.catan.Constants;

/**
 * Created by dalikim on 2017. 6. 19..
 */

public class CatanLog {
    public static void d(String message) {
        if(BuildConfig.DEBUG) {
            Log.d(Constants.TAG, message);
        }
    }

    public static void e(String message, Throwable e) {
        Log.e(Constants.TAG, message, e);
    }

    public static void e(Throwable e) {
        Log.e(Constants.TAG, e.getMessage(), e);
    }
}