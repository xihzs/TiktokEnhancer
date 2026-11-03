package com.ash.tiktokregion.hooks.telephony;

import com.ash.tiktokregion.core.BaseHook;
import com.ash.tiktokregion.core.ModuleConfig;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Spoofs android.os.SystemProperties to align system-level GSM and SIM properties with target region.
 */
public class SystemPropHook implements BaseHook {

    private static final String TAG = "TikTokSystemProp";

    @Override
    public void hook(ClassLoader classLoader) {
        try {
            Class<?> sysProp = XposedHelpers.findClass("android.os.SystemProperties", classLoader);

            XposedHelpers.findAndHookMethod(sysProp, "get", String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    String key = (String) param.args[0];
                    String val = ModuleConfig.getSystemProperty(key, null);
                    if (val != null) param.setResult(val);
                }
            });

            XposedHelpers.findAndHookMethod(sysProp, "get", String.class, String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    String key = (String) param.args[0];
                    String def = (String) param.args[1];
                    String val = ModuleConfig.getSystemProperty(key, def);
                    if (val != null) param.setResult(val);
                }
            });

            XposedBridge.log(TAG + ": Hooked android.os.SystemProperties.get()");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Failed hooking SystemProperties: " + t.getMessage());
        }
    }
}
