package com.ash.tiktokregion.core;

import android.util.Log;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.result.ClassDataList;
import org.luckypray.dexkit.result.MethodDataList;

import java.io.Closeable;

/**
 * High-performance DexKit 2.3.0 bridge manager.
 * Memory-maps DEX binaries in native C++ to dynamically resolve obfuscated
 * classes and methods across TikTok updates without hardcoding fragile ProGuard names.
 */
public final class DexKitManager implements Closeable {

    private static final String TAG = "TikTokDexKit";
    private static volatile DexKitBridge sBridge = null;
    private static volatile boolean sInitialized = false;

    private DexKitManager() {}

    /**
     * Initializes the DexKit C++ engine from the target APK path.
     */
    public static synchronized void init(String apkPath) {
        if (sInitialized && sBridge != null) {
            return;
        }

        if (apkPath == null || apkPath.isEmpty()) {
            Log.w(TAG, "Empty APK path provided to DexKitManager");
            return;
        }

        try {
            System.loadLibrary("dexkit");
            sBridge = DexKitBridge.Companion.create(apkPath);
            if (sBridge != null && sBridge.isValid()) {
                sInitialized = true;
                Log.i(TAG, "DexKit 2.3.0 initialized successfully. Indexed DEX count: " + sBridge.getDexNum());
            } else {
                Log.w(TAG, "DexKitBridge creation returned invalid handle");
                sBridge = null;
                sInitialized = false;
            }
        } catch (Throwable t) {
            Log.w(TAG, "DexKit init failed (falling back to standard reflection): " + t.getMessage());
            sBridge = null;
            sInitialized = false;
        }
    }

    public static boolean isAvailable() {
        return sInitialized && sBridge != null && sBridge.isValid();
    }

    public static DexKitBridge getBridge() {
        return sBridge;
    }

    /**
     * Executes a FindMethod query safely with error boundary.
     */
    public static MethodDataList findMethod(FindMethod query) {
        if (!isAvailable()) {
            return null;
        }
        try {
            return sBridge.findMethod(query);
        } catch (Throwable t) {
            Log.w(TAG, "findMethod query failed: " + t.getMessage());
            return null;
        }
    }

    /**
     * Executes a FindClass query safely with error boundary.
     */
    public static ClassDataList findClass(FindClass query) {
        if (!isAvailable()) {
            return null;
        }
        try {
            return sBridge.findClass(query);
        } catch (Throwable t) {
            Log.w(TAG, "findClass query failed: " + t.getMessage());
            return null;
        }
    }

    @Override
    public void close() {
        release();
    }

    public static synchronized void release() {
        if (sBridge != null) {
            try {
                sBridge.close();
            } catch (Throwable ignored) {}
            sBridge = null;
            sInitialized = false;
            Log.i(TAG, "DexKitBridge released");
        }
    }
}
