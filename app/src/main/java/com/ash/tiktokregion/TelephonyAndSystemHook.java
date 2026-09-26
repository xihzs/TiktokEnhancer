package com.ash.tiktokregion;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;
import android.util.Log;

import java.util.Collections;
import java.util.Locale;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class TelephonyAndSystemHook {

    private static final String TAG = "TikTokTelephonyHook";

    public static void hook(ClassLoader classLoader) {
        if (classLoader == null) return;
        hookTelephonyManager(classLoader);
        hookSubscriptionManager(classLoader);
        hookSubscriptionInfo(classLoader);
        hookSystemProperties(classLoader);
        hookLocale(classLoader);
        hookTimezone(classLoader);
        hookWifi(classLoader);
        hookByteDanceRegionEngine(classLoader);
    }

    private static void hookTelephonyManager(ClassLoader classLoader) {
        final String className = "android.telephony.TelephonyManager";

        hookMethodReturn(classLoader, className, "getSimCountryIso", new MethodReturnValue() {
            @Override
            public Object getValue() {
                return MainHook.sEnabled ? MainHook.sCountryIso.toLowerCase() : null;
            }
        });

        hookMethodReturn(classLoader, className, "getSimCountryIso", new Class<?>[]{int.class}, new MethodReturnValue() {
            @Override
            public Object getValue() {
                return MainHook.sEnabled ? MainHook.sCountryIso.toLowerCase() : null;
            }
        });

        hookMethodReturn(classLoader, className, "getNetworkCountryIso", new MethodReturnValue() {
            @Override
            public Object getValue() {
                return MainHook.sEnabled ? MainHook.sCountryIso.toLowerCase() : null;
            }
        });

        hookMethodReturn(classLoader, className, "getNetworkCountryIso", new Class<?>[]{int.class}, new MethodReturnValue() {
            @Override
            public Object getValue() {
                return MainHook.sEnabled ? MainHook.sCountryIso.toLowerCase() : null;
            }
        });

        hookMethodReturn(classLoader, className, "getSimOperator", new MethodReturnValue() {
            @Override
            public Object getValue() {
                return MainHook.sEnabled ? MainHook.sOperatorMccMnc : null;
            }
        });

        hookMethodReturn(classLoader, className, "getSimOperator", new Class<?>[]{int.class}, new MethodReturnValue() {
            @Override
            public Object getValue() {
                return MainHook.sEnabled ? MainHook.sOperatorMccMnc : null;
            }
        });

        hookMethodReturn(classLoader, className, "getNetworkOperator", new MethodReturnValue() {
            @Override
            public Object getValue() {
                return MainHook.sEnabled ? MainHook.sOperatorMccMnc : null;
            }
        });

        hookMethodReturn(classLoader, className, "getNetworkOperator", new Class<?>[]{int.class}, new MethodReturnValue() {
            @Override
            public Object getValue() {
                return MainHook.sEnabled ? MainHook.sOperatorMccMnc : null;
            }
        });

        hookMethodReturn(classLoader, className, "getSimOperatorName", new MethodReturnValue() {
            @Override
            public Object getValue() {
                return MainHook.sEnabled ? MainHook.sOperatorName : null;
            }
        });

        hookMethodReturn(classLoader, className, "getSimOperatorName", new Class<?>[]{int.class}, new MethodReturnValue() {
            @Override
            public Object getValue() {
                return MainHook.sEnabled ? MainHook.sOperatorName : null;
            }
        });

        hookMethodReturn(classLoader, className, "getNetworkOperatorName", new MethodReturnValue() {
            @Override
            public Object getValue() {
                return MainHook.sEnabled ? MainHook.sOperatorName : null;
            }
        });

        hookMethodReturn(classLoader, className, "getNetworkOperatorName", new Class<?>[]{int.class}, new MethodReturnValue() {
            @Override
            public Object getValue() {
                return MainHook.sEnabled ? MainHook.sOperatorName : null;
            }
        });

        hookMethodReturn(classLoader, className, "getSimState", new MethodReturnValue() {
            @Override
            public Object getValue() {
                return MainHook.sEnabled ? 5 : null;
            }
        });

        hookMethodReturn(classLoader, className, "getSimState", new Class<?>[]{int.class}, new MethodReturnValue() {
            @Override
            public Object getValue() {
                return MainHook.sEnabled ? 5 : null;
            }
        });

        hookMethodReturn(classLoader, className, "getPhoneType", new MethodReturnValue() {
            @Override
            public Object getValue() {
                return MainHook.sEnabled ? 1 : null;
            }
        });

        hookMethodReturn(classLoader, className, "hasIccCard", new MethodReturnValue() {
            @Override
            public Object getValue() {
                return MainHook.sEnabled ? true : null;
            }
        });

        hookMethodReturn(classLoader, className, "hasIccCard", new Class<?>[]{int.class}, new MethodReturnValue() {
            @Override
            public Object getValue() {
                return MainHook.sEnabled ? true : null;
            }
        });

        hookMethodReturn(classLoader, className, "isNetworkRoaming", new MethodReturnValue() {
            @Override
            public Object getValue() {
                return MainHook.sEnabled ? false : null;
            }
        });

        hookMethodReturn(classLoader, className, "isNetworkRoaming", new Class<?>[]{int.class}, new MethodReturnValue() {
            @Override
            public Object getValue() {
                return MainHook.sEnabled ? false : null;
            }
        });

        hookMethodReturn(classLoader, className, "getLine1Number", new MethodReturnValue() {
            @Override
            public Object getValue() {
                return MainHook.sEnabled ? "" : null;
            }
        });

        hookMethodReturn(classLoader, className, "getLine1Number", new Class<?>[]{int.class}, new MethodReturnValue() {
            @Override
            public Object getValue() {
                return MainHook.sEnabled ? "" : null;
            }
        });

        hookMethodReturn(classLoader, className, "getSubscriberId", new MethodReturnValue() {
            @Override
            public Object getValue() {
                return MainHook.sEnabled ? (MainHook.sOperatorMccMnc + "123456789") : null;
            }
        });

        hookMethodReturn(classLoader, className, "getSubscriberId", new Class<?>[]{int.class}, new MethodReturnValue() {
            @Override
            public Object getValue() {
                return MainHook.sEnabled ? (MainHook.sOperatorMccMnc + "123456789") : null;
            }
        });

        hookMethodReturn(classLoader, className, "getSimSerialNumber", new MethodReturnValue() {
            @Override
            public Object getValue() {
                return MainHook.sEnabled ? ("8901260" + MainHook.sOperatorMccMnc + "12345") : null;
            }
        });

        hookMethodReturn(classLoader, className, "getSimSerialNumber", new Class<?>[]{int.class}, new MethodReturnValue() {
            @Override
            public Object getValue() {
                return MainHook.sEnabled ? ("8901260" + MainHook.sOperatorMccMnc + "12345") : null;
            }
        });

        try {
            Class<?> tmClass = XposedHelpers.findClassIfExists(className, classLoader);
            if (tmClass != null) {
                XposedHelpers.findAndHookMethod(tmClass, "getCellLocation", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (MainHook.sEnabled) {
                            param.setResult(null);
                        }
                    }
                });

                XposedHelpers.findAndHookMethod(tmClass, "getAllCellInfo", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (MainHook.sEnabled) {
                            param.setResult(Collections.emptyList());
                        }
                    }
                });

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        XposedHelpers.findAndHookMethod(tmClass, "requestCellInfoUpdate",
                                java.util.concurrent.Executor.class,
                                android.telephony.TelephonyManager.CellInfoCallback.class,
                                new XC_MethodHook() {
                                    @Override
                                    protected void beforeHookedMethod(MethodHookParam param) {
                                        if (MainHook.sEnabled) {
                                            param.setResult(null);
                                        }
                                    }
                                });
                    } catch (Throwable ignored) {}
                }
                MainHook.log("Hooked TelephonyManager cell location/info to suppress cell tower triangulation");
            }
        } catch (Throwable ignored) {}
    }

    private static void hookSubscriptionManager(ClassLoader classLoader) {
        final String className = "android.telephony.SubscriptionManager";

        hookMethodReturn(classLoader, className, "getActiveSubscriptionInfoCount", new MethodReturnValue() {
            @Override
            public Object getValue() {
                return MainHook.sEnabled ? 1 : null;
            }
        });

        hookMethodReturn(classLoader, className, "getDefaultSubscriptionId", new MethodReturnValue() {
            @Override
            public Object getValue() {
                return MainHook.sEnabled ? 1 : null;
            }
        });

        hookMethodReturn(classLoader, className, "getDefaultDataSubscriptionId", new MethodReturnValue() {
            @Override
            public Object getValue() {
                return MainHook.sEnabled ? 1 : null;
            }
        });
    }

    private static void hookSubscriptionInfo(ClassLoader classLoader) {
        final String className = "android.telephony.SubscriptionInfo";

        hookMethodReturn(classLoader, className, "getCountryIso", new MethodReturnValue() {
            @Override
            public Object getValue() {
                return MainHook.sEnabled ? MainHook.sCountryIso.toLowerCase() : null;
            }
        });

        hookMethodReturn(classLoader, className, "getMcc", new MethodReturnValue() {
            @Override
            public Object getValue() {
                if (!MainHook.sEnabled) return null;
                try {
                    return Integer.parseInt(MainHook.sMcc);
                } catch (Throwable ignored) {
                    return 310;
                }
            }
        });

        hookMethodReturn(classLoader, className, "getMnc", new MethodReturnValue() {
            @Override
            public Object getValue() {
                if (!MainHook.sEnabled) return null;
                try {
                    return Integer.parseInt(MainHook.sMnc);
                } catch (Throwable ignored) {
                    return 260;
                }
            }
        });

        hookMethodReturn(classLoader, className, "getMccString", new MethodReturnValue() {
            @Override
            public Object getValue() {
                return MainHook.sEnabled ? MainHook.sMcc : null;
            }
        });

        hookMethodReturn(classLoader, className, "getMncString", new MethodReturnValue() {
            @Override
            public Object getValue() {
                return MainHook.sEnabled ? MainHook.sMnc : null;
            }
        });

        hookMethodReturn(classLoader, className, "getCarrierName", new MethodReturnValue() {
            @Override
            public Object getValue() {
                return MainHook.sEnabled ? MainHook.sOperatorName : null;
            }
        });

        hookMethodReturn(classLoader, className, "getDisplayName", new MethodReturnValue() {
            @Override
            public Object getValue() {
                return MainHook.sEnabled ? MainHook.sOperatorName : null;
            }
        });
    }

    private static void hookSystemProperties(ClassLoader classLoader) {
        try {
            Class<?> sysPropClass = XposedHelpers.findClassIfExists("android.os.SystemProperties", classLoader);
            if (sysPropClass == null) return;

            XC_MethodHook propHook = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!MainHook.sEnabled || param.args == null || param.args.length == 0) return;
                    String key = (String) param.args[0];
                    if (key == null || !key.startsWith("gsm.")) return;

                    if (key.startsWith("gsm.sim.operator.iso-country") || key.startsWith("gsm.operator.iso-country")) {
                        param.setResult(MainHook.sCountryIso.toLowerCase());
                    } else if (key.startsWith("gsm.sim.operator.numeric") || key.startsWith("gsm.operator.numeric")) {
                        param.setResult(MainHook.sOperatorMccMnc);
                    } else if (key.startsWith("gsm.sim.operator.alpha") || key.startsWith("gsm.operator.alpha")) {
                        param.setResult(MainHook.sOperatorName);
                    } else if (key.startsWith("gsm.sim.state")) {
                        param.setResult("LOADED");
                    }
                }
            };

            XposedHelpers.findAndHookMethod(sysPropClass, "get", String.class, propHook);
            XposedHelpers.findAndHookMethod(sysPropClass, "get", String.class, String.class, propHook);
        } catch (Throwable t) {
            Log.d(TAG, "SystemProperties hook failed: " + t.getMessage());
        }
    }

    private static void hookLocale(ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                    Locale.class,
                    "getDefault",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (MainHook.sEnabled && MainHook.sSpoofLocale) {
                                if (MainHook.sCachedSpoofedLocale != null) {
                                    param.setResult(MainHook.sCachedSpoofedLocale);
                                } else if (MainHook.sCountryIso != null && !MainHook.sCountryIso.isEmpty()) {
                                    param.setResult(new Locale(MainHook.sLocaleLang != null ? MainHook.sLocaleLang : "en", MainHook.sCountryIso.toUpperCase(Locale.ROOT)));
                                }
                            }
                        }
                    }
            );
        } catch (Throwable t) {
            Log.d(TAG, "Locale hook failed: " + t.getMessage());
        }

        try {
            XposedHelpers.findAndHookMethod(
                    Resources.class,
                    "getConfiguration",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!MainHook.sEnabled || !MainHook.sSpoofLocale || MainHook.sCountryIso == null || MainHook.sCountryIso.isEmpty()) return;
                            Object config = param.getResult();
                            if (config instanceof Configuration) {
                                Configuration cfg = (Configuration) config;
                                Locale targetLoc = MainHook.sCachedSpoofedLocale != null ? MainHook.sCachedSpoofedLocale : new Locale(MainHook.sLocaleLang != null ? MainHook.sLocaleLang : "en", MainHook.sCountryIso.toUpperCase(Locale.ROOT));
                                cfg.locale = targetLoc;
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    cfg.setLocales(new LocaleList(targetLoc));
                                }
                            }
                        }
                    }
            );
            MainHook.log("Hooked Resources.getConfiguration for system locale alignment");
        } catch (Throwable ignored) {}
    }

    private static void hookTimezone(ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                    java.util.TimeZone.class,
                    "getDefault",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!MainHook.sEnabled || MainHook.sCountryIso == null || MainHook.sCountryIso.isEmpty()) return;
                            String tzId = MainHook.getTimezoneForCountry(MainHook.sCountryIso);
                            if (tzId != null) {
                                param.setResult(java.util.TimeZone.getTimeZone(tzId));
                            }
                        }
                    }
            );
            MainHook.log("Hooked TimeZone.getDefault to align timezone with " + MainHook.sCountryIso);
        } catch (Throwable t) {
            MainHook.logD("Failed to hook TimeZone.getDefault: " + t.getMessage());
        }
    }

    private static void hookWifi(ClassLoader classLoader) {
        try {
            Class<?> wifiInfoClass = XposedHelpers.findClassIfExists("android.net.wifi.WifiInfo", classLoader);
            if (wifiInfoClass != null) {
                XposedHelpers.findAndHookMethod(wifiInfoClass, "getBSSID", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (MainHook.sEnabled) {
                            param.setResult("02:00:00:00:00:00");
                        }
                    }
                });
                XposedHelpers.findAndHookMethod(wifiInfoClass, "getSSID", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (MainHook.sEnabled) {
                            param.setResult("<unknown ssid>");
                        }
                    }
                });
                MainHook.log("Hooked WifiInfo.getBSSID and getSSID to suppress Wi-Fi router triangulation");
            }
        } catch (Throwable t) {
            MainHook.logD("Failed to hook WifiInfo: " + t.getMessage());
        }

        try {
            Class<?> wifiManagerClass = XposedHelpers.findClassIfExists("android.net.wifi.WifiManager", classLoader);
            if (wifiManagerClass != null) {
                XposedHelpers.findAndHookMethod(wifiManagerClass, "getScanResults", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (MainHook.sEnabled) {
                            param.setResult(Collections.emptyList());
                        }
                    }
                });
                MainHook.log("Hooked WifiManager.getScanResults to suppress AP beacon scanning");
            }
        } catch (Throwable t) {
            MainHook.logD("Failed to hook WifiManager: " + t.getMessage());
        }
    }

    private static void hookByteDanceRegionEngine(ClassLoader classLoader) {
        if (classLoader == null) return;
        try {
            Class<?> regionCls = XposedHelpers.findClassIfExists("X.03bD", classLoader);
            if (regionCls == null) {
                regionCls = XposedHelpers.findClassIfExists("X.C868003bD", classLoader);
            }
            if (regionCls != null) {
                hookByteDanceRegionClass(regionCls);
            }
        } catch (Throwable ignored) {}

        try {
            Class<?> mccCls = XposedHelpers.findClassIfExists("X.06yR", classLoader);
            if (mccCls == null) {
                mccCls = XposedHelpers.findClassIfExists("X.C1776406yR", classLoader);
            }
            if (mccCls != null) {
                hookByteDanceMccMncClass(mccCls);
            }
        } catch (Throwable ignored) {}
    }

    public static void hookByteDanceRegionClass(Class<?> clazz) {
        if (clazz == null) return;
        try {
            XC_MethodHook returnCountryUpper = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (MainHook.sEnabled && MainHook.sCountryIso != null && !MainHook.sCountryIso.isEmpty()) {
                        param.setResult(MainHook.sCountryIso.toUpperCase(Locale.ROOT));
                    }
                }
            };

            for (String method : new String[]{"LIZ", "LIZIZ", "LJ", "LJFF", "LIZLLL", "LIZJ"}) {
                try {
                    XposedHelpers.findAndHookMethod(clazz, method, returnCountryUpper);
                } catch (Throwable ignored) {}
            }
            MainHook.log("Hooked ByteDance region resolver: " + clazz.getName());
        } catch (Throwable t) {
            MainHook.logD("Failed hooking ByteDance region class: " + t.getMessage());
        }
    }

    public static void hookByteDanceMccMncClass(Class<?> clazz) {
        if (clazz == null) return;
        try {
            XposedHelpers.findAndHookMethod(clazz, "LIZIZ", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (MainHook.sEnabled && MainHook.sOperatorMccMnc != null && !MainHook.sOperatorMccMnc.isEmpty()) {
                        param.setResult(MainHook.sOperatorMccMnc);
                    }
                }
            });
            MainHook.log("Hooked ByteDance MCC/MNC resolver: " + clazz.getName());
        } catch (Throwable t) {
            MainHook.logD("Failed hooking ByteDance MCC/MNC class: " + t.getMessage());
        }
    }

    public interface MethodReturnValue {
        Object getValue();
    }

    public static void hookMethodReturn(ClassLoader classLoader, String className, String methodName, final MethodReturnValue callback) {
        hookMethodReturn(classLoader, className, methodName, new Class<?>[0], callback);
    }

    public static void hookMethodReturn(ClassLoader classLoader, String className, String methodName, Class<?>[] parameterTypes, final MethodReturnValue callback) {
        try {
            Class<?> clazz = XposedHelpers.findClassIfExists(className, classLoader);
            if (clazz == null) return;

            Object[] args = new Object[parameterTypes.length + 1];
            System.arraycopy(parameterTypes, 0, args, 0, parameterTypes.length);
            args[parameterTypes.length] = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (MainHook.sEnabled) {
                        Object val = callback.getValue();
                        if (val != null) {
                            param.setResult(val);
                        }
                    }
                }
            };

            XposedHelpers.findAndHookMethod(clazz, methodName, args);
        } catch (Throwable ignored) {
        }
    }
}
