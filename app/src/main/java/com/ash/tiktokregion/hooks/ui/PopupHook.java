package com.ash.tiktokregion.hooks.ui;

import com.ash.tiktokregion.core.BaseHook;
import com.ash.tiktokregion.core.ModuleConfig;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Intercepts and suppresses all in-app popups, bottom sheet prompts, update modals,
 * rating requests, contact sync permission sheets, and survey dialogs across TikTok v47.6.3 and v47.1.4.
 *
 * Hooks:
 * 1. ByteDance PopLayer Framework (com.bytedance.poplayer.core.PopupManager):
 *    - LIZ (addTask) -> suppressed (no tasks queued)
 *    - LIZIZ (checkSceneCanShowDirect) -> forced false
 *    - LJIIIZ (isStopped) -> forced true (aborts all scenes)
 *    - LJIIL (restart) -> suppressed
 *    - LJIILIIL / LJIILJJIL (startShowLayer) -> suppressed
 *    - LJIIZILJ (triggerShowScene) -> suppressed
 *    - LJIJ / LJIJI / LJIJJ (triggerShowTask) -> suppressed
 *    - LJIILL (stop) -> called upon activation to flush pending/running tasks
 * 2. PopLayer Task Contracts (IPopupTask, BasePopupTask, IStandardPopupTask, X.07Q5):
 *    - canShow() -> forced false
 * 3. App Update Subsystem (com.ss.android.ugc.aweme.setting.services.AppUpdateService):
 *    - LIZIZ (checkAndShowUpdateDialog) -> suppressed
 * 4. Google Play Core In-App Update (com.google.android.play.core.appupdate.AppUpdateManager):
 *    - startUpdateFlowForResult -> suppressed
 */
public class PopupHook implements BaseHook {

    private static final String TAG = "TikTokRegion_Popup";
    private static volatile boolean sEnabled = true;
    private static Class<?> sPopupManagerClass = null;

    public static void setEnabled(boolean enabled) {
        sEnabled = enabled;
        Log.i(TAG, "Popup suppression toggle updated: enabled=" + enabled);
        if (enabled && sPopupManagerClass != null) {
            stopPopLayer();
        }
    }

    public static boolean isEnabled() {
        return sEnabled;
    }

    @Override
    public void hook(ClassLoader classLoader) {
        hookPopupManager(classLoader);
        hookPopupTaskContract(classLoader);
        hookAppUpdateService(classLoader);
        hookPlayCoreUpdate(classLoader);
    }

    private static void hookPopupManager(ClassLoader classLoader) {
        try {
            Class<?> pmClass = XposedHelpers.findClassIfExists("com.bytedance.poplayer.core.PopupManager", classLoader);
            if (pmClass == null) {
                Log.d(TAG, "PopupManager class not found in target ClassLoader");
                return;
            }
            sPopupManagerClass = pmClass;

            // 1. Hook addTask (LIZ) -> block adding any popup task to queue
            XposedBridge.hookAllMethods(pmClass, "LIZ", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (sEnabled) {
                        param.setResult(null);
                    }
                }
            });

            // 2. Hook checkSceneCanShowDirect (LIZIZ) -> return false
            XposedBridge.hookAllMethods(pmClass, "LIZIZ", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (sEnabled) {
                        param.setResult(Boolean.FALSE);
                    }
                }
            });

            // 3. Hook isStopped (LJIIIZ) -> return true to abort triggerShowScene and triggerShowTask
            XposedBridge.hookAllMethods(pmClass, "LJIIIZ", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (sEnabled) {
                        param.setResult(Boolean.TRUE);
                    }
                }
            });

            // 4. Hook restart (LJIIL) -> suppress restarting PopupManager
            XposedBridge.hookAllMethods(pmClass, "LJIIL", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (sEnabled) {
                        param.setResult(null);
                    }
                }
            });

            // 5. Hook direct / layer show dispatches (LJIILIIL, LJIILJJIL)
            XposedBridge.hookAllMethods(pmClass, "LJIILIIL", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (sEnabled) {
                        param.setResult(null);
                    }
                }
            });

            XposedBridge.hookAllMethods(pmClass, "LJIILJJIL", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (sEnabled) {
                        param.setResult(null);
                    }
                }
            });

            // 6. Hook triggerShowScene (LJIIZILJ)
            XposedBridge.hookAllMethods(pmClass, "LJIIZILJ", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (sEnabled) {
                        param.setResult(null);
                    }
                }
            });

            // 7. Hook triggerShowTask overloads (LJIJ, LJIJI, LJIJJ)
            XC_MethodHook blockTaskHook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (sEnabled) {
                        param.setResult(null);
                    }
                }
            };
            XposedBridge.hookAllMethods(pmClass, "LJIJ", blockTaskHook);
            XposedBridge.hookAllMethods(pmClass, "LJIJI", blockTaskHook);
            XposedBridge.hookAllMethods(pmClass, "LJIJJ", blockTaskHook);

            // Flush out any existing popup tasks if already initialized
            if (sEnabled) {
                stopPopLayer();
            }

            XposedBridge.log(TAG + ": Successfully installed ByteDance PopupManager hooks");
        } catch (Throwable t) {
            Log.e(TAG, "Failed hooking PopupManager", t);
        }
    }

    private static void hookPopupTaskContract(ClassLoader classLoader) {
        String[] taskClassNames = new String[]{
                "com.bytedance.poplayer.IPopupTask",
                "com.bytedance.poplayer.BasePopupTask",
                "com.bytedance.poplayer.IStandardPopupTask",
                "X.07Q5",
                "X.C07Q5"
        };
        for (String className : taskClassNames) {
            try {
                Class<?> taskClass = XposedHelpers.findClassIfExists(className, classLoader);
                if (taskClass != null) {
                    XposedBridge.hookAllMethods(taskClass, "canShow", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (sEnabled) {
                                param.setResult(Boolean.FALSE);
                            }
                        }
                    });
                    XposedBridge.log(TAG + ": Successfully hooked " + className + ".canShow() -> false");
                }
            } catch (Throwable t) {
                Log.e(TAG, "Failed hooking popup task contract (" + className + ")", t);
            }
        }
    }

    private static void hookAppUpdateService(ClassLoader classLoader) {
        try {
            Class<?> updateServiceClass = XposedHelpers.findClassIfExists(
                    "com.ss.android.ugc.aweme.setting.services.AppUpdateService", classLoader);
            if (updateServiceClass != null) {
                XposedBridge.hookAllMethods(updateServiceClass, "LIZIZ", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (sEnabled) {
                            param.setResult(null);
                        }
                    }
                });
                XposedBridge.log(TAG + ": Successfully hooked AppUpdateService.LIZIZ -> suppressed");
            }
        } catch (Throwable t) {
            Log.e(TAG, "Failed hooking AppUpdateService", t);
        }
    }

    private static void hookPlayCoreUpdate(ClassLoader classLoader) {
        try {
            Class<?> playUpdateManager = XposedHelpers.findClassIfExists(
                    "com.google.android.play.core.appupdate.AppUpdateManager", classLoader);
            if (playUpdateManager != null) {
                XposedBridge.hookAllMethods(playUpdateManager, "startUpdateFlowForResult", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (sEnabled) {
                            param.setResult(false);
                        }
                    }
                });
                XposedBridge.log(TAG + ": Successfully hooked PlayCore AppUpdateManager.startUpdateFlowForResult");
            }
        } catch (Throwable t) {
            Log.d(TAG, "PlayCore AppUpdateManager hook skipped: " + t.getMessage());
        }

        try {
            Class<?> reviewManager = XposedHelpers.findClassIfExists(
                    "com.google.android.play.core.review.ReviewManager", classLoader);
            if (reviewManager != null) {
                XposedBridge.hookAllMethods(reviewManager, "requestReviewFlow", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (sEnabled) {
                            param.setResult(null);
                        }
                    }
                });
                XposedBridge.hookAllMethods(reviewManager, "launchReviewFlow", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (sEnabled) {
                            param.setResult(null);
                        }
                    }
                });
                XposedBridge.log(TAG + ": Successfully hooked PlayCore ReviewManager");
            }
        } catch (Throwable t) {
            Log.d(TAG, "PlayCore ReviewManager hook skipped: " + t.getMessage());
        }
    }

    /**
     * Actively triggers PopupManager.LJIILL() and sets LJIIIIZZ AtomicBoolean to true
     * to immediately halt any already-running popup threads or visible overlays.
     */
    private static void stopPopLayer() {
        if (sPopupManagerClass == null) return;
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                // Set LJIIIIZZ (AtomicBoolean) = true
                Field atomicField = XposedHelpers.findFieldIfExists(sPopupManagerClass, "LJIIIIZZ");
                if (atomicField != null) {
                    Object val = atomicField.get(null);
                    if (val instanceof AtomicBoolean) {
                        ((AtomicBoolean) val).set(true);
                    }
                }

                // Call LJIILL() (stop)
                Method stopMethod = XposedHelpers.findMethodExactIfExists(sPopupManagerClass, "LJIILL");
                if (stopMethod != null) {
                    stopMethod.invoke(null);
                    Log.i(TAG, "Invoked PopupManager.LJIILL() - PopLayer stopped & tasks flushed");
                }
            } catch (Throwable t) {
                Log.d(TAG, "stopPopLayer invocation failed: " + t.getMessage());
            }
        });
    }
}
