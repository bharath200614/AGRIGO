package com.agrigo.utils;

import android.content.Context;
import android.widget.Toast;

/**
 * Utility class for displaying toast messages
 */
public class ToastUtils {

    public static void showShort(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static void showShort(Context context, int messageResId) {
        Toast.makeText(context, messageResId, Toast.LENGTH_SHORT).show();
    }

    public static void showLong(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    public static void showLong(Context context, int messageResId) {
        Toast.makeText(context, messageResId, Toast.LENGTH_LONG).show();
    }
}
