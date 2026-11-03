package com.ash.tiktokregion.hooks.telephony;

import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import com.ash.tiktokregion.core.BaseHook;
import com.ash.tiktokregion.core.ModuleConfig;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Spoofs TelephonyManager and SubscriptionManager to bypass TikTok's SIM card region detection.
 */
public class TelephonyHook implements BaseHook {

    private static final String TAG = "TikTokTelephony";

    @Override
    public void hook(ClassLoader classLoader) {
        hookTelephonyManager();
        hookSubscriptionManager();
        hookSubscriptionInfo();
    }

    private void hookTelephonyManager() {
        hookSimCountryIso();
        hookNetworkCountryIso();
        hookOperators();
        hookSimStates();
        hookDeviceIdentifiers();
    }

    private void hookSimCountryIso() {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String val = ModuleConfig.getTelephonyString("simCountryIso", null);
                if (val != null) param.setResult(val);
            }
        };

        try {
            XposedHelpers.findAndHookMethod(TelephonyManager.class, "getSimCountryIso", hook);
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(TelephonyManager.class, "getSimCountryIso", int.class, hook);
        } catch (Throwable ignored) {}
    }

    private void hookNetworkCountryIso() {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String val = ModuleConfig.getTelephonyString("networkCountryIso", null);
                if (val != null) param.setResult(val);
            }
        };

        try {
            XposedHelpers.findAndHookMethod(TelephonyManager.class, "getNetworkCountryIso", hook);
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(TelephonyManager.class, "getNetworkCountryIso", int.class, hook);
        } catch (Throwable ignored) {}
    }

    private void hookOperators() {
        XC_MethodHook simOpHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String val = ModuleConfig.getTelephonyString("simOperator", null);
                if (val != null) param.setResult(val);
            }
        };
        try {
            XposedHelpers.findAndHookMethod(TelephonyManager.class, "getSimOperator", simOpHook);
            XposedHelpers.findAndHookMethod(TelephonyManager.class, "getSimOperator", int.class, simOpHook);
        } catch (Throwable ignored) {}

        XC_MethodHook netOpHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String val = ModuleConfig.getTelephonyString("networkOperator", null);
                if (val != null) param.setResult(val);
            }
        };
        try {
            XposedHelpers.findAndHookMethod(TelephonyManager.class, "getNetworkOperator", netOpHook);
            XposedHelpers.findAndHookMethod(TelephonyManager.class, "getNetworkOperator", int.class, netOpHook);
        } catch (Throwable ignored) {}

        XC_MethodHook simNameHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String val = ModuleConfig.getTelephonyString("simOperatorName", null);
                if (val != null) param.setResult(val);
            }
        };
        try {
            XposedHelpers.findAndHookMethod(TelephonyManager.class, "getSimOperatorName", simNameHook);
            XposedHelpers.findAndHookMethod(TelephonyManager.class, "getSimOperatorName", int.class, simNameHook);
        } catch (Throwable ignored) {}

        XC_MethodHook netNameHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String val = ModuleConfig.getTelephonyString("networkOperatorName", null);
                if (val != null) param.setResult(val);
            }
        };
        try {
            XposedHelpers.findAndHookMethod(TelephonyManager.class, "getNetworkOperatorName", netNameHook);
            XposedHelpers.findAndHookMethod(TelephonyManager.class, "getNetworkOperatorName", int.class, netNameHook);
        } catch (Throwable ignored) {}
    }

    private void hookSimStates() {
        XC_MethodHook simStateHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                int state = ModuleConfig.getTelephonyInt("simState", -1);
                if (state != -1) param.setResult(state);
            }
        };
        try {
            XposedHelpers.findAndHookMethod(TelephonyManager.class, "getSimState", simStateHook);
            XposedHelpers.findAndHookMethod(TelephonyManager.class, "getSimState", int.class, simStateHook);
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(TelephonyManager.class, "getPhoneType", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    int type = ModuleConfig.getTelephonyInt("phoneType", -1);
                    if (type != -1) param.setResult(type);
                }
            });
        } catch (Throwable ignored) {}

        XC_MethodHook iccHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                int val = ModuleConfig.getTelephonyInt("hasIccCard", -1);
                if (val != -1) param.setResult(val == 1);
            }
        };
        try {
            XposedHelpers.findAndHookMethod(TelephonyManager.class, "hasIccCard", iccHook);
            XposedHelpers.findAndHookMethod(TelephonyManager.class, "hasIccCard", int.class, iccHook);
        } catch (Throwable ignored) {}

        XC_MethodHook roamingHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                int val = ModuleConfig.getTelephonyInt("isNetworkRoaming", -1);
                if (val != -1) param.setResult(val == 1);
            }
        };
        try {
            XposedHelpers.findAndHookMethod(TelephonyManager.class, "isNetworkRoaming", roamingHook);
            XposedHelpers.findAndHookMethod(TelephonyManager.class, "isNetworkRoaming", int.class, roamingHook);
        } catch (Throwable ignored) {}
    }

    private void hookDeviceIdentifiers() {
        XC_MethodHook line1Hook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String val = ModuleConfig.getTelephonyString("line1Number", null);
                if (val != null) param.setResult(val);
            }
        };
        try {
            XposedHelpers.findAndHookMethod(TelephonyManager.class, "getLine1Number", line1Hook);
            XposedHelpers.findAndHookMethod(TelephonyManager.class, "getLine1Number", int.class, line1Hook);
        } catch (Throwable ignored) {}

        XC_MethodHook subIdHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String val = ModuleConfig.getTelephonyString("subscriberId", null);
                if (val != null) param.setResult(val);
            }
        };
        try {
            XposedHelpers.findAndHookMethod(TelephonyManager.class, "getSubscriberId", subIdHook);
            XposedHelpers.findAndHookMethod(TelephonyManager.class, "getSubscriberId", int.class, subIdHook);
        } catch (Throwable ignored) {}

        XC_MethodHook serialHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String val = ModuleConfig.getTelephonyString("simSerialNumber", null);
                if (val != null) param.setResult(val);
            }
        };
        try {
            XposedHelpers.findAndHookMethod(TelephonyManager.class, "getSimSerialNumber", serialHook);
            XposedHelpers.findAndHookMethod(TelephonyManager.class, "getSimSerialNumber", int.class, serialHook);
        } catch (Throwable ignored) {}
    }

    private void hookSubscriptionManager() {
        try {
            XposedHelpers.findAndHookMethod(SubscriptionManager.class, "getActiveSubscriptionInfoCount", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    int count = ModuleConfig.getTelephonyInt("activeSubscriptionInfoCount", -1);
                    if (count != -1) param.setResult(count);
                }
            });

            XposedHelpers.findAndHookMethod(SubscriptionManager.class, "getDefaultSubscriptionId", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    int id = ModuleConfig.getTelephonyInt("defaultSubscriptionId", -1);
                    if (id != -1) param.setResult(id);
                }
            });

            XposedHelpers.findAndHookMethod(SubscriptionManager.class, "getDefaultDataSubscriptionId", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    int id = ModuleConfig.getTelephonyInt("defaultDataSubscriptionId", -1);
                    if (id != -1) param.setResult(id);
                }
            });
        } catch (Throwable ignored) {}
    }

    private void hookSubscriptionInfo() {
        try {
            XposedHelpers.findAndHookMethod(SubscriptionInfo.class, "getCountryIso", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    String val = ModuleConfig.getTelephonyString("simCountryIso", null);
                    if (val != null) param.setResult(val);
                }
            });

            XposedHelpers.findAndHookMethod(SubscriptionInfo.class, "getMcc", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    String mccStr = ModuleConfig.getTelephonyString("mcc", null);
                    if (mccStr != null) {
                        try {
                            param.setResult(Integer.parseInt(mccStr));
                        } catch (NumberFormatException ignored) {}
                    }
                }
            });

            XposedHelpers.findAndHookMethod(SubscriptionInfo.class, "getMnc", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    String mncStr = ModuleConfig.getTelephonyString("mnc", null);
                    if (mncStr != null) {
                        try {
                            param.setResult(Integer.parseInt(mncStr));
                        } catch (NumberFormatException ignored) {}
                    }
                }
            });

            XC_MethodHook nameHook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    String val = ModuleConfig.getTelephonyString("simOperatorName", null);
                    if (val != null) param.setResult(val);
                }
            };
            XposedHelpers.findAndHookMethod(SubscriptionInfo.class, "getCarrierName", nameHook);
            XposedHelpers.findAndHookMethod(SubscriptionInfo.class, "getDisplayName", nameHook);
        } catch (Throwable ignored) {}
    }
}
