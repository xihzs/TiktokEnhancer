package com.ash.tiktokregion.hooks.social;

import com.ash.tiktokregion.core.BaseHook;
import com.ash.tiktokregion.core.ModuleConfig;

import android.util.SparseArray;
import android.view.View;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * High-Craft Anti-Delete Hook for TikTok Direct Messages (DM).
 * <p>
 * Ensures that when another user recalls/deletes a message:
 * 1. The message renders as a real native TikTok chat bubble with original text
 *    and the deleted indicator ([Deleted] <text>).
 * 2. Proactively caches all incoming live message text across memory and disk.
 * 3. Prevents UI-thread recursion and deadlocks by avoiding method calls on hooked
 *    models (using direct field access and thread-local recursion guards).
 * 4. Intercepts X.0q1j.LIZJ (display string resolver) and X.0q1j.LIZIZ (content resolver).
 * 5. Disables the system recall notice pill (RecallMessageProtocol.isEnabled -> false).
 * 6. Persists cached message content across app restarts with debounced async I/O.
 */
public class AntiDeleteHook implements BaseHook {

    private static final String TAG = "TikTokRegion-AntiDelete";

    private static volatile boolean sEnabled = true;

    private static final int MAX_CACHE_SIZE = 3000;
    private static final Map<String, String> sCacheByUuid = Collections.synchronizedMap(
            new LinkedHashMap<String, String>(256, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                    return size() > MAX_CACHE_SIZE;
                }
            }
    );

    private static final Map<Long, String> sCacheById = Collections.synchronizedMap(
            new LinkedHashMap<Long, String>(256, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Long, String> eldest) {
                    return size() > MAX_CACHE_SIZE;
                }
            }
    );

    private static final Set<String> sRecalledUuids = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<Long> sRecalledIds = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private static final ExecutorService sDiskExecutor = Executors.newSingleThreadExecutor();
    private static final AtomicBoolean sDiskSavePending = new AtomicBoolean(false);
    private static volatile boolean sDiskCacheLoaded = false;

    private static final ThreadLocal<Boolean> sExtractingText = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return Boolean.FALSE;
        }
    };

    public static void setEnabled(boolean enabled) {
        sEnabled = enabled;
        XposedBridge.log(TAG + ": Anti-delete message status changed -> enabled=" + enabled);
    }

    public static boolean isEnabled() {
        return sEnabled;
    }

    @Override
    public void hook(ClassLoader classLoader) {
        loadDiskCache();
        hookMessageModel(classLoader);
        hookContentParser(classLoader);
        hookBubbleDataModels(classLoader);
        hookTextTranslateAssem(classLoader);
        hookRecallProtocol(classLoader);
        hookRecallUI(classLoader);
    }

    /**
     * Hooks ByteDance IM Message model (X.0BMG in v47.6.3, X.0BVS in v47.1.4).
     */
    private static void hookMessageModel(ClassLoader classLoader) {
        String[] msgClassNames = new String[]{"X.0BMG", "X.0BVS"};
        String[] statusClassNames = new String[]{"X.0CGj", "X.0CY0"};

        Class<?> statusClass = null;
        for (String scn : statusClassNames) {
            statusClass = XposedHelpers.findClassIfExists(scn, classLoader);
            if (statusClass != null) break;
        }

        for (String msgClassName : msgClassNames) {
            Class<?> bvsClass = XposedHelpers.findClassIfExists(msgClassName, classLoader);
            if (bvsClass == null) continue;

            final Class<?> resolvedStatusClass = statusClass;

            // 1. isRecalled() -> return false so TikTok treats this as an active chat bubble
            try {
                XposedHelpers.findAndHookMethod(bvsClass, "isRecalled", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (!sEnabled) return;
                        if (isMessageRecalledRaw(param.thisObject)) {
                            markAsRecalled(param.thisObject);
                            param.setResult(false);
                        }
                    }
                });
                XposedBridge.log(TAG + ": Successfully hooked " + msgClassName + ".isRecalled()");
            } catch (Throwable t) {
                XposedBridge.log(TAG + ": Failed hooking isRecalled on " + msgClassName + ": " + t.getMessage());
            }

            // 2. getMessageStatus() -> return AVAILABLE for recalled messages
            try {
                XposedHelpers.findAndHookMethod(bvsClass, "getMessageStatus", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (!sEnabled) return;
                        Object status = param.getResult();
                        if (status != null && "RECALLED".equals(status.toString())) {
                            markAsRecalled(param.thisObject);
                            try {
                                Object available = XposedHelpers.getStaticObjectField(status.getClass(), "AVAILABLE");
                                if (available != null) {
                                    param.setResult(available);
                                }
                            } catch (Throwable ignored) {}
                        } else if (isMessageRecalledRaw(param.thisObject)) {
                            try {
                                Class<?> sc = (status != null) ? status.getClass() : resolvedStatusClass;
                                if (sc != null) {
                                    Object available = XposedHelpers.getStaticObjectField(sc, "AVAILABLE");
                                    if (available != null) {
                                        param.setResult(available);
                                    }
                                }
                            } catch (Throwable ignored) {}
                        }
                    }
                });
                XposedBridge.log(TAG + ": Successfully hooked " + msgClassName + ".getMessageStatus()");
            } catch (Throwable t) {
                XposedBridge.log(TAG + ": Failed hooking getMessageStatus on " + msgClassName + ": " + t.getMessage());
            }

            // 3. setMessageStatus -> prevent setting RECALLED
            if (resolvedStatusClass != null) {
                try {
                    XposedHelpers.findAndHookMethod(bvsClass, "setMessageStatus", resolvedStatusClass, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!sEnabled) return;
                            Object status = param.args[0];
                            if (status != null && "RECALLED".equals(status.toString())) {
                                markAsRecalled(param.thisObject);
                                Object available = XposedHelpers.getStaticObjectField(status.getClass(), "AVAILABLE");
                                if (available != null) {
                                    param.args[0] = available;
                                }
                            }
                        }
                    });
                    XposedBridge.log(TAG + ": Successfully hooked " + msgClassName + ".setMessageStatus(" + resolvedStatusClass.getSimpleName() + ")");
                } catch (Throwable t) {
                    XposedBridge.log(TAG + ": Failed hooking setMessageStatus on " + msgClassName + ": " + t.getMessage());
                }
            }

            // 4. setContent(String) -> proactively cache live text & prevent wipe on recall
            try {
                XposedHelpers.findAndHookMethod(bvsClass, "setContent", String.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        String newContent = (String) param.args[0];
                        String extracted = extractTextFromRaw(newContent);
                        if (extracted != null && !extracted.isEmpty()) {
                            cacheMessage(param.thisObject, extracted);
                        }

                        if (sEnabled && (newContent == null || newContent.trim().isEmpty())) {
                            String oldContent = null;
                            try {
                                oldContent = (String) XposedHelpers.getObjectField(param.thisObject, "content");
                            } catch (Throwable ignored) {}

                            if (oldContent != null && !oldContent.trim().isEmpty()) {
                                param.args[0] = oldContent;
                            } else {
                                String cached = getCachedMessageText(param.thisObject);
                                if (cached != null && !cached.isEmpty()) {
                                    param.args[0] = "{\"text\":\"" + escapeJson(cached) + "\"}";
                                }
                            }
                        }
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            String content = (String) XposedHelpers.getObjectField(param.thisObject, "content");
                            String extracted = extractTextFromRaw(content);
                            if (extracted != null && !extracted.isEmpty()) {
                                cacheMessage(param.thisObject, extracted);
                            }
                        } catch (Throwable ignored) {}
                    }
                });
                XposedBridge.log(TAG + ": Successfully hooked " + msgClassName + ".setContent(String)");
            } catch (Throwable t) {
                XposedBridge.log(TAG + ": Failed hooking setContent on " + msgClassName + ": " + t.getMessage());
            }

            // 5. getContent() -> ensure valid JSON containing "[Deleted] original text" for recalled messages
            try {
                XposedHelpers.findAndHookMethod(bvsClass, "getContent", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        String content = (String) param.getResult();
                        if (content != null && !content.trim().isEmpty()) {
                            String extracted = extractTextFromRaw(content);
                            if (extracted != null && !extracted.isEmpty()) {
                                cacheMessage(param.thisObject, extracted);
                            }
                        } else if (sEnabled) {
                            String cached = getCachedMessageText(param.thisObject);
                            if (cached != null && !cached.isEmpty()) {
                                content = "{\"text\":\"" + escapeJson(cached) + "\"}";
                                param.setResult(content);
                            }
                        }

                        if (sEnabled && isMessageRecalledRaw(param.thisObject)) {
                            String text = extractTextFromMessage(param.thisObject);
                            if (text == null || text.trim().isEmpty()) {
                                text = "[Message deleted]";
                            }
                            String fullText = text.startsWith("[Deleted] ") ? text : ("[Deleted] " + text);
                            param.setResult("{\"text\":\"" + escapeJson(fullText) + "\"}");
                        }
                    }
                });
                XposedBridge.log(TAG + ": Successfully hooked " + msgClassName + ".getContent()");
            } catch (Throwable t) {
                XposedBridge.log(TAG + ": Failed hooking getContent on " + msgClassName + ": " + t.getMessage());
            }

            // 6. getMsgType() -> ensure recalled text messages retain msgType = 7 (TEXT)
            try {
                XposedHelpers.findAndHookMethod(bvsClass, "getMsgType", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (!sEnabled) return;
                        if (isMessageRecalledRaw(param.thisObject)) {
                            Object res = param.getResult();
                            if (res instanceof Integer) {
                                int msgType = (Integer) res;
                                if (msgType == 0 || msgType == 1 || msgType == 1031) {
                                    param.setResult(7);
                                }
                            }
                        }
                    }
                });
                XposedBridge.log(TAG + ": Successfully hooked " + msgClassName + ".getMsgType()");
            } catch (Throwable t) {
                XposedBridge.log(TAG + ": Failed hooking getMsgType on " + msgClassName + ": " + t.getMessage());
            }

            // 7. Hook setUuid and setMsgId for proactive cache tracking
            try {
                XposedHelpers.findAndHookMethod(bvsClass, "setUuid", String.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        String uuid = (String) param.args[0];
                        if (uuid == null || uuid.isEmpty()) return;
                        String text = extractTextFromMessage(param.thisObject);
                        if (text != null && !text.isEmpty()) {
                            sCacheByUuid.put(uuid, text);
                        } else {
                            try {
                                long msgId = XposedHelpers.getLongField(param.thisObject, "msgId");
                                if (msgId > 0 && sCacheById.containsKey(msgId)) {
                                    sCacheByUuid.put(uuid, sCacheById.get(msgId));
                                }
                            } catch (Throwable ignored) {}
                        }
                    }
                });
                XposedHelpers.findAndHookMethod(bvsClass, "setMsgId", long.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Long msgId = (Long) param.args[0];
                        if (msgId == null || msgId <= 0) return;
                        String text = extractTextFromMessage(param.thisObject);
                        if (text != null && !text.isEmpty()) {
                            sCacheById.put(msgId, text);
                        } else {
                            try {
                                String uuid = (String) XposedHelpers.getObjectField(param.thisObject, "uuid");
                                if (uuid != null && !uuid.isEmpty() && sCacheByUuid.containsKey(uuid)) {
                                    sCacheById.put(msgId, sCacheByUuid.get(uuid));
                                }
                            } catch (Throwable ignored) {}
                        }
                    }
                });
            } catch (Throwable ignored) {}

            // 8. Hook addExt - detect recall packets
            try {
                XposedHelpers.findAndHookMethod(bvsClass, "addExt", String.class, String.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (!sEnabled) return;
                        String key = (String) param.args[0];
                        String val = (String) param.args[1];
                        if ("s:is_recalled".equals(key) && "1".equals(val)) {
                            markAsRecalled(param.thisObject);
                        }
                    }
                });
            } catch (Throwable ignored) {}
        }
    }

    /**
     * Hooks BaseContentParserKt and protobuf bypass (X.0CGi.LJJIIJ in v47.6.3, X.0CXy.LJJIIJZLJL in v47.1.4).
     * Prevents BaseContentParserKt from bailing out when a recall packet arrives.
     */
    private static void hookContentParser(ClassLoader classLoader) {
        // 1. Hook Protobuf bypass helper:
        // - v47.6.3: X.0CGi.LJJIIJ(0BMG)
        // - v47.1.4: X.0CXy.LJJIIJZLJL(0BVS)
        String[][] bypassCandidates = new String[][]{
                {"X.0CGi", "LJJIIJ", "X.0BMG"},
                {"X.0CXy", "LJJIIJZLJL", "X.0BVS"}
        };
        for (String[] candidate : bypassCandidates) {
            try {
                Class<?> helperClass = XposedHelpers.findClassIfExists(candidate[0], classLoader);
                Class<?> msgClass = XposedHelpers.findClassIfExists(candidate[2], classLoader);
                if (helperClass != null && msgClass != null) {
                    Method m = XposedHelpers.findMethodExactIfExists(helperClass, candidate[1], msgClass);
                    if (m != null) {
                        XposedBridge.hookMethod(m, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) {
                                if (!sEnabled) return;
                                if (isMessageRecalledRaw(param.args[0])) {
                                    param.setResult(false);
                                }
                            }
                        });
                        XposedBridge.log(TAG + ": Successfully hooked " + candidate[0] + "." + candidate[1]);
                    }
                }
            } catch (Throwable t) {
                XposedBridge.log(TAG + ": Bypass hook note (" + candidate[0] + "): " + t.getMessage());
            }
        }

        // 2. Hook BaseContentParserKt.getOrParseJsonContent(MessageClass, Class)
        try {
            Class<?> parserClass = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.im.message.content.adapter.BaseContentParserKt", classLoader);
            if (parserClass != null) {
                String[] msgClassNames = new String[]{"X.0BMG", "X.0BVS"};
                for (String msgClassName : msgClassNames) {
                    Class<?> msgClass = XposedHelpers.findClassIfExists(msgClassName, classLoader);
                    if (msgClass == null) continue;

                    Method getOrParseMethod = XposedHelpers.findMethodExactIfExists(parserClass, "getOrParseJsonContent", msgClass, Class.class);
                    if (getOrParseMethod != null) {
                        XposedBridge.hookMethod(getOrParseMethod, new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                if (!sEnabled) return;
                                Object msgObj = param.args[0];
                                if (isMessageRecalledRaw(msgObj)) {
                                    String text = extractTextFromMessage(msgObj);
                                    if (text == null || text.trim().isEmpty()) {
                                        text = "[Message deleted]";
                                    }
                                    String fullText = text.startsWith("[Deleted] ") ? text : ("[Deleted] " + text);
                                    Object tc = createTextContent(fullText, classLoader);
                                    if (tc != null) {
                                        param.setResult(tc);
                                        try {
                                            XposedHelpers.callMethod(msgObj, "putLocalCache", 501, tc);
                                        } catch (Throwable ignored) {}
                                    }
                                }
                            }
                        });
                        XposedBridge.log(TAG + ": Successfully hooked BaseContentParserKt.getOrParseJsonContent(" + msgClassName + ")");
                    }
                }
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": BaseContentParserKt hook note: " + t.getMessage());
        }
    }

    /**
     * Hooks the message bubble data items:
     * - v47.6.3: X.0pjI (Receive), X.0pjJ (Send), X.0pjK (Helper), X.0pjL (Interface)
     * - v47.1.4: X.0q1h (Receive), X.0q1i (Send), X.0q1j (Helper), X.0q1k (Interface)
     */
    private static void hookBubbleDataModels(ClassLoader classLoader) {
        // 1. Hook Receive Bubble Data constructors (X.0pjI / X.0q1h)
        String[] recvBubbleClasses = new String[]{"X.0pjI", "X.0q1h"};
        for (String rbc : recvBubbleClasses) {
            Class<?> recvClass = XposedHelpers.findClassIfExists(rbc, classLoader);
            if (recvClass != null) {
                try {
                    XposedBridge.hookAllConstructors(recvClass, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!sEnabled) return;
                            populateBubbleText(param.thisObject, classLoader);
                        }
                    });
                    XposedBridge.log(TAG + ": Successfully hooked " + rbc + " constructors (Receive Bubble)");
                } catch (Throwable t) {
                    XposedBridge.log(TAG + ": Failed hooking " + rbc + ": " + t.getMessage());
                }
            }
        }

        // 2. Hook Send Bubble Data constructors (X.0pjJ / X.0q1i)
        String[] sendBubbleClasses = new String[]{"X.0pjJ", "X.0q1i"};
        for (String sbc : sendBubbleClasses) {
            Class<?> sendClass = XposedHelpers.findClassIfExists(sbc, classLoader);
            if (sendClass != null) {
                try {
                    XposedBridge.hookAllConstructors(sendClass, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!sEnabled) return;
                            populateBubbleText(param.thisObject, classLoader);
                        }
                    });
                    XposedBridge.log(TAG + ": Successfully hooked " + sbc + " constructors (Send Bubble)");
                } catch (Throwable t) {
                    XposedBridge.log(TAG + ": Failed hooking " + sbc + ": " + t.getMessage());
                }
            }
        }

        // 3. Hook Helper methods (X.0pjK / X.0q1j: LIZIZ and LIZJ)
        String[][] helperPairs = new String[][]{
                {"X.0pjK", "X.0pjL"},
                {"X.0q1j", "X.0q1k"}
        };
        for (String[] pair : helperPairs) {
            Class<?> helperClass = XposedHelpers.findClassIfExists(pair[0], classLoader);
            Class<?> interfaceClass = XposedHelpers.findClassIfExists(pair[1], classLoader);
            if (helperClass != null && interfaceClass != null) {
                try {
                    Method liziz = XposedHelpers.findMethodExactIfExists(helperClass, "LIZIZ", interfaceClass);
                    if (liziz != null) {
                        XposedBridge.hookMethod(liziz, new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                if (!sEnabled) return;
                                Object item = param.args[0];
                                Object msgObj = extractMessageFromBubbleItem(item);
                                if (msgObj != null && isMessageRecalledRaw(msgObj)) {
                                    String text = extractTextFromMessage(msgObj);
                                    if (text == null || text.trim().isEmpty()) {
                                        text = "[Message deleted]";
                                    }
                                    String fullText = text.startsWith("[Deleted] ") ? text : ("[Deleted] " + text);
                                    Object tc = createTextContent(fullText, classLoader);
                                    if (tc != null) {
                                        param.setResult(tc);
                                    }
                                }
                            }
                        });
                    }

                    Method lizj = XposedHelpers.findMethodExactIfExists(helperClass, "LIZJ", interfaceClass);
                    if (lizj != null) {
                        XposedBridge.hookMethod(lizj, new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                if (!sEnabled) return;
                                Object item = param.args[0];
                                Object msgObj = extractMessageFromBubbleItem(item);
                                if (msgObj != null) {
                                    if (isMessageRecalledRaw(msgObj)) {
                                        String text = extractTextFromMessage(msgObj);
                                        if (text == null || text.trim().isEmpty()) {
                                            text = "[Message deleted]";
                                        }
                                        String fullText = text.startsWith("[Deleted] ") ? text : ("[Deleted] " + text);
                                        param.setResult(fullText);
                                    } else {
                                        // Proactive cache: live active message text
                                        Object res = param.getResult();
                                        if (res instanceof String) {
                                            String s = (String) res;
                                            if (!s.isEmpty() && !s.startsWith("[Deleted] ")) {
                                                cacheMessage(msgObj, s);
                                            }
                                        }
                                    }
                                }
                            }
                        });
                    }
                    XposedBridge.log(TAG + ": Successfully hooked " + pair[0] + " helper methods");
                } catch (Throwable t) {
                    XposedBridge.log(TAG + ": " + pair[0] + " hook note: " + t.getMessage());
                }
            }
        }
    }

    /**
     * Hooks TextTranslateAssem layout and binding lifecycle so chat bubbles never lay out empty.
     */
    private static void hookTextTranslateAssem(ClassLoader classLoader) {
        String assemName = "com.ss.android.ugc.aweme.im.sdk.chat.ui.text.content.TextTranslateAssem";
        Class<?> assemClass = XposedHelpers.findClassIfExists(assemName, classLoader);
        if (assemClass == null) {
            XposedBridge.log(TAG + ": " + assemName + " not found, skipping assem hook");
            return;
        }

        XC_MethodHook assemBindHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (!sEnabled || param.args == null || param.args.length == 0) return;
                Object item = param.args[0];
                if (item == null) return;
                Object msgObj = extractMessageFromBubbleItem(item);
                if (msgObj == null) return;

                if (isMessageRecalledRaw(msgObj)) {
                    String text = extractTextFromMessage(msgObj);
                    if (text == null || text.trim().isEmpty()) {
                        text = "[Message deleted]";
                    }
                    String fullText = text.startsWith("[Deleted] ") ? text : ("[Deleted] " + text);
                    try {
                        XposedHelpers.setObjectField(item, "LLJJIJIL", fullText);
                    } catch (Throwable ignored) {}
                    try {
                        XposedHelpers.setObjectField(item, "LLJJJ", fullText);
                    } catch (Throwable ignored) {}

                    Object tc = createTextContent(fullText, classLoader);
                    if (tc != null) {
                        try {
                            XposedHelpers.callMethod(msgObj, "putLocalCache", 501, tc);
                        } catch (Throwable ignored) {}
                    }
                } else {
                    // Proactive cache: Capture live message text
                    try {
                        CharSequence cs = (CharSequence) XposedHelpers.callMethod(item, "LJIILL");
                        if (cs != null && cs.length() > 0) {
                            String live = cs.toString().trim();
                            if (!live.startsWith("[Deleted] ")) {
                                cacheMessage(msgObj, live);
                            }
                        }
                    } catch (Throwable ignored) {}
                }
            }
        };

        // v47.6.3: InterfaceC36440pjL methods Zp, vq, i4
        Class<?> pjLClass = XposedHelpers.findClassIfExists("X.0pjL", classLoader);
        if (pjLClass != null) {
            for (String methodName : new String[]{"Zp", "vq", "i4"}) {
                Method m = XposedHelpers.findMethodExactIfExists(assemClass, methodName, pjLClass);
                if (m != null) {
                    try {
                        XposedBridge.hookMethod(m, assemBindHook);
                        XposedBridge.log(TAG + ": Successfully hooked TextTranslateAssem." + methodName + "(0pjL)");
                    } catch (Throwable ignored) {}
                }
            }
        }

        // v47.1.4: InterfaceC32120q1k method z4
        Class<?> q1kClass = XposedHelpers.findClassIfExists("X.0q1k", classLoader);
        if (q1kClass != null) {
            Method z4Method = XposedHelpers.findMethodExactIfExists(assemClass, "z4", q1kClass);
            if (z4Method != null) {
                try {
                    XposedBridge.hookMethod(z4Method, assemBindHook);
                    XposedBridge.log(TAG + ": Successfully hooked TextTranslateAssem.z4(0q1k)");
                } catch (Throwable ignored) {}
            }
        }
    }

    /**
     * Inspects a bubble item (X.0pjI / X.0pjJ in v47.6.3, X.0q1h / X.0q1i in v47.1.4):
     * - Proactively caches live text if active.
     * - Restores "[Deleted] original text" into LLJJIJIL / LLJJJ strictly IF recalled.
     */
    private static void populateBubbleText(Object bubbleItem, ClassLoader classLoader) {
        if (bubbleItem == null) return;
        try {
            Object msgObj = extractMessageFromBubbleItem(bubbleItem);
            if (msgObj == null) return;

            CharSequence currentText = null;
            try {
                currentText = (CharSequence) XposedHelpers.getObjectField(bubbleItem, "LLJJIJIL");
            } catch (Throwable ignored) {}
            if (currentText == null) {
                try {
                    currentText = (CharSequence) XposedHelpers.getObjectField(bubbleItem, "LLJJJ");
                } catch (Throwable ignored) {}
            }

            boolean isRecalled = isMessageRecalledRaw(msgObj);

            if (isRecalled) {
                String text = extractTextFromMessage(msgObj);
                if (text == null || text.trim().isEmpty()) {
                    text = "[Message deleted]";
                }
                String fullText = text.startsWith("[Deleted] ") ? text : ("[Deleted] " + text);
                try {
                    XposedHelpers.setObjectField(bubbleItem, "LLJJIJIL", fullText);
                } catch (Throwable ignored) {}
                try {
                    XposedHelpers.setObjectField(bubbleItem, "LLJJJ", fullText);
                } catch (Throwable ignored) {}

                Object tc = createTextContent(fullText, classLoader);
                if (tc != null) {
                    try {
                        XposedHelpers.callMethod(msgObj, "putLocalCache", 501, tc);
                    } catch (Throwable ignored) {}
                }
            } else if (currentText != null && currentText.length() > 0) {
                // Proactive Live Cache: Cache live message text when it is active and on screen
                String liveStr = currentText.toString().trim();
                if (!liveStr.startsWith("[Deleted] ")) {
                    cacheMessage(msgObj, liveStr);
                }
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Error in populateBubbleText: " + t.getMessage());
        }
    }

    private static Object extractMessageFromBubbleItem(Object item) {
        if (item == null) return null;
        try {
            // Fast direct path:
            // v47.6.3: item.LL (C30370pYy) -> LIZ (C25480pGd) -> LIZ (0BMG)
            // v47.1.4: item.LL (C19510pgF) -> LIZ (C16400pbE) -> LIZ (0BVS)
            Object skeletonData = XposedHelpers.getObjectField(item, "LL");
            if (skeletonData != null) {
                Object pbE = XposedHelpers.getObjectField(skeletonData, "LIZ");
                if (pbE != null) {
                    Object msg = XposedHelpers.getObjectField(pbE, "LIZ");
                    if (msg != null) {
                        return msg;
                    }
                }
            }
        } catch (Throwable ignored) {}
        try {
            Object skeleton = XposedHelpers.callMethod(item, "LLIZLLLIL");
            if (skeleton != null) {
                Object pbE = XposedHelpers.getObjectField(skeleton, "LIZ");
                if (pbE != null) {
                    Object msg = XposedHelpers.getObjectField(pbE, "LIZ");
                    if (msg != null) return msg;
                }
            }
        } catch (Throwable ignored) {}
        try {
            return XposedHelpers.callMethod(item, "getMessage");
        } catch (Throwable ignored) {}
        return null;
    }

    /**
     * Prevents RecallMessageProtocol from claiming the message as an unsent notice.
     */
    private static void hookRecallProtocol(ClassLoader classLoader) {
        try {
            Class<?> recallProtoClass = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.im.sdk.chat.ui.viewholder.RecallMessageProtocol", classLoader);
            if (recallProtoClass != null) {
                XposedHelpers.findAndHookMethod(recallProtoClass, "isEnabled", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (sEnabled) {
                            param.setResult(false);
                        }
                    }
                });
                XposedBridge.log(TAG + ": Hooked RecallMessageProtocol.isEnabled() -> false");
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": RecallMessageProtocol.isEnabled hook note: " + t.getMessage());
        }
    }

    /**
     * Safety fallback on RecallMessageReusedUISlotAssem in case a message still enters it.
     */
    private static void hookRecallUI(ClassLoader classLoader) {
        String assemClassName = "com.ss.android.ugc.aweme.im.sdk.chat.ui.viewholder.RecallMessageReusedUISlotAssem";
        Class<?> recallAssemClass = XposedHelpers.findClassIfExists(assemClassName, classLoader);
        if (recallAssemClass == null) {
            return;
        }

        // Hook both Xp (v47.6.3) and ur (v47.1.4)
        for (Method m : recallAssemClass.getDeclaredMethods()) {
            if (("Xp".equals(m.getName()) || "ur".equals(m.getName())) && m.getParameterTypes().length == 2 && m.getParameterTypes()[1] == String.class) {
                try {
                    XposedBridge.hookMethod(m, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!sEnabled || param.args == null || param.args.length < 2) return;
                            Object msgObj = param.args[0];
                            String text = extractTextFromMessage(msgObj);
                            param.args[1] = (text != null && !text.trim().isEmpty()) ? ("[Deleted] " + text) : "[Deleted] Message deleted";
                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!sEnabled || param.args == null || param.args.length < 2) return;
                            Object msgObj = param.args[0];
                            String text = extractTextFromMessage(msgObj);
                            String displayText = (text != null && !text.trim().isEmpty()) ? ("[Deleted] " + text) : "[Deleted] Message deleted";
                            try {
                                View frView = null;
                                try {
                                    frView = (View) XposedHelpers.callMethod(param.thisObject, "gq"); // v47.6.3
                                } catch (Throwable ignored) {}
                                if (frView == null) {
                                    try {
                                        frView = (View) XposedHelpers.callMethod(param.thisObject, "Fr"); // v47.1.4
                                    } catch (Throwable ignored) {}
                                }
                                if (frView instanceof TextView) {
                                    TextView tv = (TextView) frView;
                                    tv.setSingleLine(false);
                                    tv.setMaxLines(Integer.MAX_VALUE);
                                    tv.setText(displayText);
                                }
                            } catch (Throwable ignored) {}
                            hideSecondaryNotice(param.thisObject);
                        }
                    });
                    XposedBridge.log(TAG + ": Successfully hooked RecallMessageReusedUISlotAssem." + m.getName());
                } catch (Throwable ignored) {}
            }
        }
    }

    private static void hideSecondaryNotice(Object assemObj) {
        if (assemObj == null) return;
        try {
            Object lllilz = XposedHelpers.getObjectField(assemObj, "LLLILZ");
            if (lllilz != null) {
                View subView = (View) XposedHelpers.callMethod(lllilz, "getValue");
                if (subView != null) {
                    subView.setVisibility(View.GONE);
                }
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Creates an instance of TextContent with the given text.
     */
    public static Object createTextContent(String text, ClassLoader classLoader) {
        if (text == null) text = "";
        try {
            Class<?> tcClass = XposedHelpers.findClass("com.ss.android.ugc.aweme.im.message.content.TextContent", classLoader);
            try {
                return XposedHelpers.callStaticMethod(tcClass, "obtain", text);
            } catch (Throwable ignored) {}

            Object instance = XposedHelpers.newInstance(tcClass, text, null, null, null);
            try {
                XposedHelpers.setIntField(instance, "type", 700);
            } catch (Throwable ignored) {}
            return instance;
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": createTextContent error: " + t.getMessage());
            return null;
        }
    }

    public static boolean isMessageRecalledRaw(Object msgObj) {
        if (msgObj == null) return false;
        try {
            String uuid = null;
            try {
                uuid = (String) XposedHelpers.getObjectField(msgObj, "uuid");
            } catch (Throwable ignored) {}
            if (uuid == null) {
                try {
                    uuid = (String) XposedHelpers.callMethod(msgObj, "getUuid");
                } catch (Throwable ignored) {}
            }
            if (uuid != null && sRecalledUuids.contains(uuid)) {
                return true;
            }

            long msgId = 0;
            try {
                msgId = XposedHelpers.getLongField(msgObj, "msgId");
            } catch (Throwable ignored) {}
            if (msgId <= 0) {
                try {
                    Long mid = (Long) XposedHelpers.callMethod(msgObj, "getMsgId");
                    if (mid != null) msgId = mid;
                } catch (Throwable ignored) {}
            }
            if (msgId > 0 && sRecalledIds.contains(msgId)) {
                return true;
            }

            Map<?, ?> ext = null;
            try {
                ext = (Map<?, ?>) XposedHelpers.getObjectField(msgObj, "ext");
            } catch (Throwable ignored) {}
            if (ext != null) {
                if ("1".equals(ext.get("s:is_recalled")) || "true".equals(ext.get("s:recalled"))) {
                    if (uuid != null && !uuid.isEmpty()) sRecalledUuids.add(uuid);
                    if (msgId > 0) sRecalledIds.add(msgId);
                    return true;
                }
            }

            Map<?, ?> localExt = null;
            try {
                localExt = (Map<?, ?>) XposedHelpers.getObjectField(msgObj, "localExt");
            } catch (Throwable ignored) {}
            if (localExt != null) {
                if ("1".equals(localExt.get("s:is_recalled")) || "1".equals(localExt.get("s:antidelete_recalled"))) {
                    if (uuid != null && !uuid.isEmpty()) sRecalledUuids.add(uuid);
                    if (msgId > 0) sRecalledIds.add(msgId);
                    return true;
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private static void markAsRecalled(Object msgObj) {
        if (msgObj == null) return;
        try {
            String uuid = null;
            try {
                uuid = (String) XposedHelpers.getObjectField(msgObj, "uuid");
            } catch (Throwable ignored) {}
            if (uuid == null) {
                try {
                    uuid = (String) XposedHelpers.callMethod(msgObj, "getUuid");
                } catch (Throwable ignored) {}
            }
            if (uuid != null && !uuid.isEmpty()) {
                sRecalledUuids.add(uuid);
            }

            long msgId = 0;
            try {
                msgId = XposedHelpers.getLongField(msgObj, "msgId");
            } catch (Throwable ignored) {}
            if (msgId <= 0) {
                try {
                    Long mid = (Long) XposedHelpers.callMethod(msgObj, "getMsgId");
                    if (mid != null) msgId = mid;
                } catch (Throwable ignored) {}
            }
            if (msgId > 0) {
                sRecalledIds.add(msgId);
            }

            try {
                XposedHelpers.callMethod(msgObj, "addLocalExt", "s:antidelete_recalled", "1");
            } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }

    public static String extractCleanText(String text) {
        if (text == null) return "";
        while (text.startsWith("[Deleted] ") || text.startsWith("[Deleted] ")) {
            if (text.startsWith("[Deleted] ")) {
                text = text.substring(3);
            } else {
                text = text.substring(2);
            }
        }
        return text.trim();
    }

    /**
     * Extracts text from memory cache, direct field localCache SparseArray, or raw JSON.
     * Guaranteed to never recurse or call hooked methods on msgObj.
     */
    public static String extractTextFromMessage(Object msgObj) {
        if (msgObj == null) return null;

        if (Boolean.TRUE.equals(sExtractingText.get())) {
            return getCachedMessageText(msgObj);
        }

        sExtractingText.set(Boolean.TRUE);
        try {
            // 1. In-memory cache by UUID or msgId
            String cached = getCachedMessageText(msgObj);
            if (cached != null && !cached.trim().isEmpty()) {
                return extractCleanText(cached);
            }

            // 2. Direct field inspection of localCache SparseArray (NEVER calls getLocalCache)
            try {
                SparseArray<?> localCache = (SparseArray<?>) XposedHelpers.getObjectField(msgObj, "localCache");
                if (localCache != null) {
                    Object obj501 = localCache.get(501);
                    if (obj501 != null) {
                        String text = extractTextFromBaseContent(obj501);
                        if (text != null && !text.trim().isEmpty()) {
                            String clean = extractCleanText(text);
                            cacheMessage(msgObj, clean);
                            return clean;
                        }
                    }
                    Object obj503 = localCache.get(503);
                    if (obj503 != null) {
                        String text = extractTextFromBaseContent(obj503);
                        if (text != null && !text.trim().isEmpty()) {
                            String clean = extractCleanText(text);
                            cacheMessage(msgObj, clean);
                            return clean;
                        }
                    }
                }
            } catch (Throwable ignored) {}

            // 3. Direct field inspection of 'content' String (NEVER calls getContent)
            try {
                String rawContent = (String) XposedHelpers.getObjectField(msgObj, "content");
                String text = extractTextFromRaw(rawContent);
                if (text != null && !text.trim().isEmpty()) {
                    String clean = extractCleanText(text);
                    cacheMessage(msgObj, clean);
                    return clean;
                }
            } catch (Throwable ignored) {}

            // 4. Fallback: bounds-safe byte string scanner
            try {
                Object contentPb = XposedHelpers.getObjectField(msgObj, "contentPB");
                String text = extractTextFromByteString(contentPb);
                if (text != null && !text.trim().isEmpty()) {
                    String clean = extractCleanText(text);
                    cacheMessage(msgObj, clean);
                    return clean;
                }
            } catch (Throwable ignored) {}

            return null;
        } finally {
            sExtractingText.set(Boolean.FALSE);
        }
    }

    private static String extractTextFromBaseContent(Object baseContent) {
        if (baseContent == null) return null;
        try {
            Method getTextMethod = baseContent.getClass().getMethod("getText");
            Object res = getTextMethod.invoke(baseContent);
            if (res instanceof String && !((String) res).trim().isEmpty()) {
                return (String) res;
            }
        } catch (Throwable ignored) {}
        try {
            Field textField = baseContent.getClass().getField("text");
            Object res = textField.get(baseContent);
            if (res instanceof String && !((String) res).trim().isEmpty()) {
                return (String) res;
            }
        } catch (Throwable ignored) {}
        try {
            Field textField = baseContent.getClass().getDeclaredField("text");
            textField.setAccessible(true);
            Object res = textField.get(baseContent);
            if (res instanceof String && !((String) res).trim().isEmpty()) {
                return (String) res;
            }
        } catch (Throwable ignored) {}
        return null;
    }

    public static String extractTextFromRaw(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        raw = raw.trim();
        if (raw.equals("{}") || raw.equals("{\"hack\":\"1\"}")) return null;

        try {
            JSONObject obj = new JSONObject(raw);
            if (obj.has("text")) {
                String t = obj.optString("text");
                if (t != null && !t.trim().isEmpty()) return t;
            }
            if (obj.has("pure_text")) {
                String pt = obj.optString("pure_text");
                if (pt != null && !pt.trim().isEmpty()) return pt;
            }
            if (obj.has("content")) {
                String c = obj.optString("content");
                if (c != null && !c.trim().isEmpty()) {
                    String inner = extractTextFromRaw(c);
                    return inner != null ? inner : c;
                }
            }
            if (obj.has("desc")) {
                String d = obj.optString("desc");
                if (d != null && !d.trim().isEmpty()) return d;
            }
            if (obj.has("title")) {
                String title = obj.optString("title");
                if (title != null && !title.trim().isEmpty()) return title;
            }
        } catch (Exception ignored) {
            if (!raw.startsWith("{") && !raw.startsWith("[")) {
                return raw;
            }
        }
        return null;
    }

    /**
     * Bounds-safe Protobuf wire type 2 length-delimited scanner for ByteString.
     */
    private static String extractTextFromByteString(Object byteStringObj) {
        if (byteStringObj == null) return null;
        try {
            byte[] data = (byte[]) XposedHelpers.callMethod(byteStringObj, "toByteArray");
            if (data == null || data.length < 2) return null;
            int i = 0;
            String longestReadable = null;
            while (i < data.length) {
                int tag = 0;
                int shift = 0;
                while (i < data.length) {
                    byte b = data[i++];
                    tag |= (b & 0x7f) << shift;
                    if ((b & 0x80) == 0) break;
                    shift += 7;
                }
                int wire = tag & 7;
                if (wire == 2) {
                    int length = 0;
                    shift = 0;
                    while (i < data.length) {
                        byte b = data[i++];
                        length |= (b & 0x7f) << shift;
                        if ((b & 0x80) == 0) break;
                        shift += 7;
                    }
                    if (length > 0 && length <= data.length - i) {
                        try {
                            String s = new String(data, i, length, StandardCharsets.UTF_8);
                            if (isPrintableText(s)) {
                                if (longestReadable == null || s.length() > longestReadable.length()) {
                                    longestReadable = s;
                                }
                            }
                        } catch (Throwable ignored) {}
                        i += length;
                    } else {
                        break;
                    }
                } else if (wire == 0) {
                    while (i < data.length && (data[i] & 0x80) != 0) {
                        i++;
                    }
                    if (i < data.length) {
                        i++;
                    }
                } else if (wire == 1) {
                    i += 8;
                } else if (wire == 5) {
                    i += 4;
                } else {
                    break;
                }
            }
            return longestReadable;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean isPrintableText(String s) {
        if (s == null || s.trim().isEmpty()) return false;
        int printableCount = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isLetterOrDigit(c) || Character.isWhitespace(c)
                    || "!?,.:;\"'()-_@#$%^&*+/\\".indexOf(c) >= 0) {
                printableCount++;
            }
        }
        return ((float) printableCount / s.length()) >= 0.75f;
    }

    private static void cacheMessage(Object msgObj, String text) {
        if (msgObj == null || text == null || text.trim().isEmpty()) return;
        String clean = extractCleanText(text);
        try {
            String uuid = null;
            try {
                uuid = (String) XposedHelpers.getObjectField(msgObj, "uuid");
            } catch (Throwable ignored) {}
            if (uuid == null) {
                try {
                    uuid = (String) XposedHelpers.callMethod(msgObj, "getUuid");
                } catch (Throwable ignored) {}
            }
            if (uuid != null && !uuid.isEmpty()) {
                sCacheByUuid.put(uuid, clean);
            }

            long msgId = 0;
            try {
                msgId = XposedHelpers.getLongField(msgObj, "msgId");
            } catch (Throwable ignored) {}
            if (msgId <= 0) {
                try {
                    Long mid = (Long) XposedHelpers.callMethod(msgObj, "getMsgId");
                    if (mid != null) msgId = mid;
                } catch (Throwable ignored) {}
            }
            if (msgId > 0) {
                sCacheById.put(msgId, clean);
            }

            saveDiskCacheAsync();
        } catch (Throwable ignored) {}
    }

    private static String getCachedMessageText(Object msgObj) {
        if (msgObj == null) return null;
        try {
            String uuid = null;
            try {
                uuid = (String) XposedHelpers.getObjectField(msgObj, "uuid");
            } catch (Throwable ignored) {}
            if (uuid == null) {
                try {
                    uuid = (String) XposedHelpers.callMethod(msgObj, "getUuid");
                } catch (Throwable ignored) {}
            }
            if (uuid != null && sCacheByUuid.containsKey(uuid)) {
                return sCacheByUuid.get(uuid);
            }

            long msgId = 0;
            try {
                msgId = XposedHelpers.getLongField(msgObj, "msgId");
            } catch (Throwable ignored) {}
            if (msgId <= 0) {
                try {
                    Long mid = (Long) XposedHelpers.callMethod(msgObj, "getMsgId");
                    if (mid != null) msgId = mid;
                } catch (Throwable ignored) {}
            }
            if (msgId > 0 && sCacheById.containsKey(msgId)) {
                return sCacheById.get(msgId);
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static File getDiskCacheFile() {
        File dir = new File("/data/data/com.zhiliaoapp.musically/files");
        if (!dir.exists()) {
            dir = new File("/data/user/0/com.zhiliaoapp.musically/files");
        }
        return new File(dir, "anti_delete_msg_cache.json");
    }

    private static void loadDiskCache() {
        if (sDiskCacheLoaded) return;
        sDiskExecutor.execute(() -> {
            try {
                File file = getDiskCacheFile();
                if (!file.exists()) return;
                byte[] data = new byte[(int) file.length()];
                try (FileInputStream fis = new FileInputStream(file)) {
                    int read = fis.read(data);
                    if (read > 0) {
                        String jsonStr = new String(data, 0, read, StandardCharsets.UTF_8);
                        JSONObject json = new JSONObject(jsonStr);
                        Iterator<String> keys = json.keys();
                        while (keys.hasNext()) {
                            String k = keys.next();
                            String v = json.optString(k);
                            if (v != null && !v.isEmpty()) {
                                sCacheByUuid.put(k, v);
                            }
                        }
                        sDiskCacheLoaded = true;
                        XposedBridge.log(TAG + ": Loaded " + sCacheByUuid.size() + " messages from disk cache");
                    }
                }
            } catch (Throwable t) {
                XposedBridge.log(TAG + ": Error loading disk cache: " + t.getMessage());
            }
        });
    }

    private static void saveDiskCacheAsync() {
        if (sDiskSavePending.compareAndSet(false, true)) {
            sDiskExecutor.execute(() -> {
                try {
                    Thread.sleep(1500); // 1.5s debounce
                } catch (InterruptedException ignored) {}
                sDiskSavePending.set(false);
                try {
                    File file = getDiskCacheFile();
                    File parent = file.getParentFile();
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs();
                    }
                    JSONObject json = new JSONObject();
                    synchronized (sCacheByUuid) {
                        for (Map.Entry<String, String> entry : sCacheByUuid.entrySet()) {
                            json.put(entry.getKey(), entry.getValue());
                        }
                    }
                    byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        fos.write(bytes);
                        fos.flush();
                    }
                } catch (Throwable ignored) {}
            });
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
