package com.ash.tiktokregion;

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class QualityAndTelemetryHook {

    private static final String TAG = "TikTokQualityPFP";
    private static final String FLAG_BADGE_TAG = "TIKTOK_FLAG_BADGE";

    private static volatile boolean sQualityHooked = false;
    private static volatile boolean sAvatarHooked = false;

    private static final ThreadLocal<Boolean> sInQualityHook = ThreadLocal.withInitial(() -> Boolean.FALSE);

    public static void hook(ClassLoader classLoader) {
        if (classLoader == null) return;
        hookQualityUncapper(classLoader);
        hookFeedAvatar(classLoader);
    }

    public static void onClassLoaded(Class<?> clazz) {
        if (clazz == null) return;
        String name = clazz.getName();
        if ("com.ss.android.ugc.aweme.feed.model.Video".equals(name)) {
            hookVideoQuality(clazz);
        } else if ("com.ss.android.ugc.aweme.feed.model.VideoUrlModel".equals(name)) {
            hookVideoUrlModelQuality(clazz);
        } else if ("com.ss.android.ugc.playerkit.simapicommon.model.SimVideoUrlModel".equals(name)) {
            hookSimVideoQuality(clazz);
        } else if ("com.ss.android.ugc.playerkit.simapicommon.model.SimVideo".equals(name)) {
            hookSimVideo(clazz);
        }
    }

    private static void hookQualityUncapper(ClassLoader classLoader) {
        if (sQualityHooked) return;
        sQualityHooked = true;

        try {
            Class<?> videoClass = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.feed.model.Video", classLoader);
            if (videoClass != null) {
                hookVideoQuality(videoClass);
            }
        } catch (Throwable t) {
            Log.d(TAG, "hookQualityUncapper Video error: " + t.getMessage());
        }

        try {
            Class<?> videoUrlClass = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.feed.model.VideoUrlModel", classLoader);
            if (videoUrlClass != null) {
                hookVideoUrlModelQuality(videoUrlClass);
            }
        } catch (Throwable t) {
            Log.d(TAG, "hookQualityUncapper VideoUrlModel error: " + t.getMessage());
        }

        try {
            Class<?> simVideoClass = XposedHelpers.findClassIfExists("com.ss.android.ugc.playerkit.simapicommon.model.SimVideo", classLoader);
            if (simVideoClass != null) {
                hookSimVideo(simVideoClass);
            }
        } catch (Throwable t) {
            Log.d(TAG, "hookQualityUncapper SimVideo error: " + t.getMessage());
        }

        try {
            Class<?> simVideoUrlClass = XposedHelpers.findClassIfExists("com.ss.android.ugc.playerkit.simapicommon.model.SimVideoUrlModel", classLoader);
            if (simVideoUrlClass != null) {
                hookSimVideoQuality(simVideoUrlClass);
            }
        } catch (Throwable t) {
            Log.d(TAG, "hookQualityUncapper SimVideoUrlModel error: " + t.getMessage());
        }
    }

    public static void hookVideoQuality(Class<?> videoClass) {
        if (videoClass == null) return;
        try {
            XC_MethodHook setBitrateHook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (!MainHook.isForceHighQualityEnabled()) return;
                    if (param.args != null && param.args.length > 0 && param.args[0] instanceof List) {
                        List<?> list = (List<?>) param.args[0];
                        if (!list.isEmpty()) {
                            param.args[0] = createSortedBitrateList(list);
                        }
                    }
                }
            };
            try {
                XposedHelpers.findAndHookMethod(videoClass, "setBitRate", List.class, setBitrateHook);
            } catch (Throwable ignored) {}

            XC_MethodHook bitrateHook = new XC_MethodHook() {
                @Override
                @SuppressWarnings("unchecked")
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!MainHook.isForceHighQualityEnabled()) return;
                    if (sInQualityHook.get()) return;
                    sInQualityHook.set(Boolean.TRUE);
                    try {
                        Object result = param.getResult();
                        List<?> list = (result instanceof List) ? (List<?>) result : null;
                        if (list == null || list.isEmpty()) {
                            try {
                                Object raw = XposedHelpers.getObjectField(param.thisObject, "bitRateList");
                                if (raw instanceof List && !((List<?>) raw).isEmpty()) {
                                    list = (List<?>) raw;
                                }
                            } catch (Throwable ignored) {}
                        }
                        if (list != null && !list.isEmpty()) {
                            List<Object> sorted = createSortedBitrateList(list);
                            param.setResult(sorted);
                            try {
                                XposedHelpers.setObjectField(param.thisObject, "bitRateList", sorted);
                            } catch (Throwable ignored) {}
                        }
                    } finally {
                        sInQualityHook.set(Boolean.FALSE);
                    }
                }
            };

            XposedHelpers.findAndHookMethod(videoClass, "getBitRate", bitrateHook);
            try {
                XposedHelpers.findAndHookMethod(videoClass, "getRawBitRate", bitrateHook);
            } catch (Throwable ignored) {}

            XC_MethodHook playAddrHook = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!MainHook.isForceHighQualityEnabled()) return;
                    Object res = param.getResult();
                    if (res == null) return;
                    try {
                        List<?> bitRates = (List<?>) XposedHelpers.getObjectField(param.thisObject, "bitRateList");
                        if (bitRates != null && !bitRates.isEmpty()) {
                            List<Object> sorted = createSortedBitrateList(bitRates);
                            if (sorted != null && !sorted.isEmpty()) {
                                Object top = sorted.get(0);
                                Object topPlayAddr = XposedHelpers.getObjectField(top, "playAddr");
                                if (topPlayAddr != null) {
                                    Object topUrls = XposedHelpers.getObjectField(topPlayAddr, "urlList");
                                    if (topUrls instanceof List && !((List<?>) topUrls).isEmpty()) {
                                        XposedHelpers.setObjectField(res, "urlList", topUrls);
                                    }
                                    Object topUri = XposedHelpers.getObjectField(topPlayAddr, "uri");
                                    if (topUri instanceof String && !((String) topUri).isEmpty()) {
                                        XposedHelpers.setObjectField(res, "uri", topUri);
                                    }
                                }
                            }
                        }
                    } catch (Throwable ignored) {}
                }
            };
            try { XposedHelpers.findAndHookMethod(videoClass, "getPlayAddr", playAddrHook); } catch (Throwable ignored) {}
            try { XposedHelpers.findAndHookMethod(videoClass, "getPlayAddrH264", playAddrHook); } catch (Throwable ignored) {}
            try { XposedHelpers.findAndHookMethod(videoClass, "getPlayAddrBytevc1", playAddrHook); } catch (Throwable ignored) {}
            try { XposedHelpers.findAndHookMethod(videoClass, "getProperPlayAddr", playAddrHook); } catch (Throwable ignored) {}
        } catch (Throwable t) {
            Log.d(TAG, "Failed hooking Video quality: " + t.getMessage());
        }
    }

    public static void hookVideoUrlModelQuality(Class<?> videoUrlClass) {
        if (videoUrlClass == null) return;
        try {
            XC_MethodHook setBitrateHook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (!MainHook.isForceHighQualityEnabled()) return;
                    if (param.args != null && param.args.length > 0 && param.args[0] instanceof List) {
                        List<?> list = (List<?>) param.args[0];
                        if (!list.isEmpty()) {
                            param.args[0] = createSortedBitrateList(list);
                        }
                    }
                }
            };
            try {
                XposedHelpers.findAndHookMethod(videoUrlClass, "setBitRate", List.class, setBitrateHook);
            } catch (Throwable ignored) {}

            XC_MethodHook bitrateHook = new XC_MethodHook() {
                @Override
                @SuppressWarnings("unchecked")
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!MainHook.isForceHighQualityEnabled()) return;
                    if (sInQualityHook.get()) return;
                    sInQualityHook.set(Boolean.TRUE);
                    try {
                        Object result = param.getResult();
                        if (result instanceof List) {
                            List<?> list = (List<?>) result;
                            if (!list.isEmpty()) {
                                List<Object> sorted = createSortedBitrateList(list);
                                param.setResult(sorted);
                                try {
                                    XposedHelpers.setObjectField(param.thisObject, "bitRateList", sorted);
                                } catch (Throwable ignored) {}
                            }
                        }
                    } finally {
                        sInQualityHook.set(Boolean.FALSE);
                    }
                }
            };
            XposedHelpers.findAndHookMethod(videoUrlClass, "getBitRate", bitrateHook);
            try {
                XposedHelpers.findAndHookMethod(videoUrlClass, "getRawBitRate", bitrateHook);
            } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }

    public static void hookSimVideo(Class<?> simVideoClass) {
        if (simVideoClass == null) return;
        try {
            XC_MethodHook setBitrateHook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (!MainHook.isForceHighQualityEnabled()) return;
                    if (param.args != null && param.args.length > 0 && param.args[0] instanceof List) {
                        List<?> list = (List<?>) param.args[0];
                        if (!list.isEmpty()) {
                            param.args[0] = createSortedBitrateList(list);
                        }
                    }
                }
            };
            try {
                XposedHelpers.findAndHookMethod(simVideoClass, "setBitRate", List.class, setBitrateHook);
            } catch (Throwable ignored) {}

            XC_MethodHook simBitrateHook = new XC_MethodHook() {
                @Override
                @SuppressWarnings("unchecked")
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!MainHook.isForceHighQualityEnabled()) return;
                    if (sInQualityHook.get()) return;
                    sInQualityHook.set(Boolean.TRUE);
                    try {
                        Object result = param.getResult();
                        List<?> list = (result instanceof List) ? (List<?>) result : null;
                        if (list == null || list.isEmpty()) {
                            try {
                                Object raw = XposedHelpers.getObjectField(param.thisObject, "bitRate");
                                if (raw instanceof List && !((List<?>) raw).isEmpty()) {
                                    list = (List<?>) raw;
                                }
                            } catch (Throwable ignored) {}
                        }
                        if (list != null && !list.isEmpty()) {
                            List<Object> sorted = createSortedBitrateList(list);
                            param.setResult(sorted);
                            try {
                                XposedHelpers.setObjectField(param.thisObject, "bitRate", sorted);
                            } catch (Throwable ignored) {}
                        }
                    } finally {
                        sInQualityHook.set(Boolean.FALSE);
                    }
                }
            };

            XposedHelpers.findAndHookMethod(simVideoClass, "getBitRate", simBitrateHook);
            try {
                XposedHelpers.findAndHookMethod(simVideoClass, "getRawBitRate", simBitrateHook);
            } catch (Throwable ignored) {}
            try {
                XposedHelpers.findAndHookMethod(simVideoClass, "getRawBitrate", simBitrateHook);
            } catch (Throwable ignored) {}
            try {
                XposedHelpers.findAndHookMethod(simVideoClass, "getDashVideoBitRate", simBitrateHook);
            } catch (Throwable ignored) {}

            XC_MethodHook simPlayAddrHook = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!MainHook.isForceHighQualityEnabled()) return;
                    Object res = param.getResult();
                    if (res == null) return;
                    try {
                        List<?> bitRates = (List<?>) XposedHelpers.getObjectField(param.thisObject, "bitRate");
                        if (bitRates != null && !bitRates.isEmpty()) {
                            List<Object> sorted = createSortedBitrateList(bitRates);
                            if (sorted != null && !sorted.isEmpty()) {
                                Object top = sorted.get(0);
                                Object topPlayAddr = XposedHelpers.getObjectField(top, "playAddr");
                                if (topPlayAddr != null) {
                                    Object topUrls = XposedHelpers.getObjectField(topPlayAddr, "urlList");
                                    if (topUrls instanceof List && !((List<?>) topUrls).isEmpty()) {
                                        XposedHelpers.setObjectField(res, "urlList", topUrls);
                                    }
                                    Object topUri = XposedHelpers.getObjectField(topPlayAddr, "uri");
                                    if (topUri instanceof String && !((String) topUri).isEmpty()) {
                                        XposedHelpers.setObjectField(res, "uri", topUri);
                                    }
                                }
                            }
                        }
                    } catch (Throwable ignored) {}
                }
            };
            try { XposedHelpers.findAndHookMethod(simVideoClass, "getPlayAddr", simPlayAddrHook); } catch (Throwable ignored) {}
            try { XposedHelpers.findAndHookMethod(simVideoClass, "getPlayAddrH264", simPlayAddrHook); } catch (Throwable ignored) {}
            try { XposedHelpers.findAndHookMethod(simVideoClass, "getPlayAddrBytevc1", simPlayAddrHook); } catch (Throwable ignored) {}
            try { XposedHelpers.findAndHookMethod(simVideoClass, "getProperPlayAddr", simPlayAddrHook); } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }

    public static void hookSimVideoQuality(Class<?> simVideoUrlClass) {
        if (simVideoUrlClass == null) return;
        try {
            XC_MethodHook setBitrateHook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (!MainHook.isForceHighQualityEnabled()) return;
                    if (param.args != null && param.args.length > 0 && param.args[0] instanceof List) {
                        List<?> list = (List<?>) param.args[0];
                        if (!list.isEmpty()) {
                            param.args[0] = createSortedBitrateList(list);
                        }
                    }
                }
            };
            try {
                XposedHelpers.findAndHookMethod(simVideoUrlClass, "setBitRate", List.class, setBitrateHook);
            } catch (Throwable ignored) {}

            XC_MethodHook simUrlHook = new XC_MethodHook() {
                @Override
                @SuppressWarnings("unchecked")
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!MainHook.isForceHighQualityEnabled()) return;
                    if (sInQualityHook.get()) return;
                    sInQualityHook.set(Boolean.TRUE);
                    try {
                        Object result = param.getResult();
                        if (result instanceof List) {
                            List<?> list = (List<?>) result;
                            if (!list.isEmpty()) {
                                List<Object> sorted = createSortedBitrateList(list);
                                param.setResult(sorted);
                                try {
                                    XposedHelpers.setObjectField(param.thisObject, "bitRate", sorted);
                                } catch (Throwable ignored) {}
                            }
                        }
                    } finally {
                        sInQualityHook.set(Boolean.FALSE);
                    }
                }
            };
            XposedHelpers.findAndHookMethod(simVideoUrlClass, "getBitRate", simUrlHook);
            try {
                XposedHelpers.findAndHookMethod(simVideoUrlClass, "getRawBitRate", simUrlHook);
            } catch (Throwable ignored) {}
            try {
                XposedHelpers.findAndHookMethod(simVideoUrlClass, "getDashBitRate", simUrlHook);
            } catch (Throwable ignored) {}
            try {
                XposedHelpers.findAndHookMethod(simVideoUrlClass, "getAdaptive", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (!MainHook.isForceHighQualityEnabled()) return;
                        param.setResult(Boolean.FALSE);
                    }
                });
            } catch (Throwable ignored) {}
        } catch (Throwable t) {
            Log.d(TAG, "Failed hooking SimVideoUrlModel.getBitRate(): " + t.getMessage());
        }
    }

    private static class BitrateScoredItem {
        final Object item;
        final long score;

        BitrateScoredItem(Object item, long score) {
            this.item = item;
            this.score = score;
        }
    }

    private static List<Object> createSortedBitrateList(List<?> original) {
        if (original == null || original.isEmpty()) {
            return (List<Object>) original;
        }
        try {
            int n = original.size();
            List<BitrateScoredItem> scored = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                Object obj = original.get(i);
                if (obj == null) continue;
                long r = 0;
                int b = 0;
                try { r = XposedHelpers.getIntField(obj, "bitRate"); } catch (Throwable ignored) {}
                try { b = XposedHelpers.getIntField(obj, "isBytevc1"); } catch (Throwable ignored) {}
                String g = null;
                try { g = (String) XposedHelpers.getObjectField(obj, "gearName"); } catch (Throwable ignored) {}
                long extra = getGearExtraScore(g);
                long score = r + (b > 0 ? 500000L : 0L) + extra;
                scored.add(new BitrateScoredItem(obj, score));
            }
            if (scored.isEmpty()) {
                return (List<Object>) original;
            }
            Collections.sort(scored, (a, b) -> Long.compare(b.score, a.score));
            List<Object> result = new ArrayList<>(1);
            result.add(scored.get(0).item);
            return result;
        } catch (Throwable t) {
            return (List<Object>) original;
        }
    }

    private static long getGearExtraScore(String gear) {
        if (gear == null) return 0L;
        String lower = gear.toLowerCase(Locale.ROOT);
        if (lower.contains("2160") || lower.contains("4k")) {
            return 50000000L;
        } else if (lower.contains("1440") || lower.contains("2k")) {
            return 25000000L;
        } else if (lower.contains("1080")) {
            return 10000000L;
        } else if (lower.contains("720")) {
            return 2000000L;
        }
        return 0L;
    }

    private static void hookFeedAvatar(ClassLoader classLoader) {
        if (sAvatarHooked) return;
        sAvatarHooked = true;

        try {
            Class<?> feedAvatarClass = XposedHelpers.findClassIfExists(
                    "com.ss.android.ugc.aweme.feed.assem.avatar.FeedAvatarDefaultAssem", classLoader);
            if (feedAvatarClass == null) {
                Log.d(TAG, "FeedAvatarDefaultAssem not found");
                return;
            }

            XposedHelpers.findAndHookMethod(feedAvatarClass, "onViewCreated", View.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    View root = (View) param.args[0];
                    disableClippingDirect(root);
                    try {
                        View container = (View) XposedHelpers.getObjectField(param.thisObject, "LLLILZJ");
                        disableClippingDirect(container);
                    } catch (Throwable ignored) {}
                }
            });

            XposedHelpers.findAndHookMethod(feedAvatarClass, "z4", Object.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    updateAvatarFlagBadge(param.thisObject, param.args[0]);
                }
            });

            Log.i(TAG, "Hooked FeedAvatarDefaultAssem to display creator country flag above PFP");
        } catch (Throwable t) {
            Log.d(TAG, "hookFeedAvatar failed: " + t.getMessage());
        }
    }

    private static void disableClippingDirect(View view) {
        if (view == null) return;
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            if (vg.getClipChildren()) vg.setClipChildren(false);
            if (vg.getClipToPadding()) vg.setClipToPadding(false);
        }
        ViewParent parent = view.getParent();
        if (parent instanceof ViewGroup) {
            ViewGroup pvg = (ViewGroup) parent;
            if (pvg.getClipChildren()) pvg.setClipChildren(false);
            if (pvg.getClipToPadding()) pvg.setClipToPadding(false);
        }
    }

    private static void updateAvatarFlagBadge(Object assem, Object itemParams) {
        if (assem == null) return;
        try {
            View avatarView = null;
            try {
                avatarView = (View) XposedHelpers.getObjectField(assem, "LLLILZ");
            } catch (Throwable ignored) {}

            ViewGroup container = null;
            try {
                container = (ViewGroup) XposedHelpers.getObjectField(assem, "LLLILZJ");
            } catch (Throwable ignored) {}

            if (container == null && avatarView != null && avatarView.getParent() instanceof ViewGroup) {
                container = (ViewGroup) avatarView.getParent();
            }

            if (container == null) {
                try {
                    container = (ViewGroup) XposedHelpers.getObjectField(assem, "LLLLII");
                } catch (Throwable ignored) {}
            }

            if (container == null) {
                return;
            }
            final ViewGroup targetContainer = container;

            TextView flagBadge = targetContainer.findViewWithTag(FLAG_BADGE_TAG);

            if (!MainHook.isTelemetryHUDEnabled() || itemParams == null) {
                if (flagBadge != null && flagBadge.getVisibility() != View.GONE) {
                    flagBadge.setVisibility(View.GONE);
                }
                return;
            }

            Object aweme = null;
            try {
                aweme = XposedHelpers.callMethod(itemParams, "getAweme");
            } catch (Throwable t) {
                try {
                    aweme = XposedHelpers.getObjectField(itemParams, "aweme");
                } catch (Throwable ignored) {}
            }

            if (aweme == null) {
                if (flagBadge != null && flagBadge.getVisibility() != View.GONE) {
                    flagBadge.setVisibility(View.GONE);
                }
                return;
            }

            String countryIso = extractCountryIso(aweme);
            if (TextUtils.isEmpty(countryIso)) {
                if (flagBadge != null && flagBadge.getVisibility() != View.GONE) {
                    flagBadge.setVisibility(View.GONE);
                }
                return;
            }

            String flagEmoji = countryCodeToEmoji(countryIso);
            String badgeText = (flagEmoji != null ? flagEmoji + " " : "") + countryIso;

            Context context = targetContainer.getContext();
            if (flagBadge == null) {
                flagBadge = new TextView(context);
                flagBadge.setTag(FLAG_BADGE_TAG);

                flagBadge.setBackground(null);
                flagBadge.setTextColor(Color.WHITE);
                flagBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
                flagBadge.setTypeface(Typeface.DEFAULT_BOLD);
                flagBadge.setGravity(Gravity.CENTER);
                flagBadge.setIncludeFontPadding(false);
                flagBadge.setPadding(0, 0, 0, 0);

                flagBadge.setShadowLayer(dpToPx(context, 2.5f), 0, dpToPx(context, 1f), Color.parseColor("#E6000000"));

                ViewGroup.LayoutParams lp;
                if (targetContainer instanceof RelativeLayout) {
                    RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                    rlp.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
                    if (avatarView != null && avatarView.getId() > 0) {
                        rlp.addRule(RelativeLayout.ALIGN_TOP, avatarView.getId());
                    } else {
                        rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
                    }
                    lp = rlp;
                } else if (targetContainer instanceof FrameLayout) {
                    FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                    flp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
                    lp = flp;
                } else {
                    lp = new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                }

                flagBadge.setTranslationY(-dpToPx(context, 7));
                disableClippingDirect(targetContainer);
                targetContainer.addView(flagBadge, lp);
            }

            flagBadge.setText(badgeText);
            if (flagBadge.getVisibility() != View.VISIBLE) {
                flagBadge.setVisibility(View.VISIBLE);
            }

            final Object currentAweme = aweme;
            if (MainHook.isTelemetryPopupEnabled()) {
                flagBadge.setLongClickable(true);
                flagBadge.setOnLongClickListener(v -> {
                    if (!MainHook.isTelemetryPopupEnabled()) return false;
                    try {
                        v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                    } catch (Throwable ignored) {}
                    showTelemetryDialog(v.getContext(), currentAweme);
                    return true;
                });
            } else {
                flagBadge.setLongClickable(false);
                flagBadge.setOnLongClickListener(null);
            }

        } catch (Throwable t) {
            Log.d(TAG, "updateAvatarFlagBadge error: " + t.getMessage());
        }
    }

    public static String extractCountryIso(Object aweme) {
        if (aweme == null) return null;
        String region = null;
        try {
            Object author = XposedHelpers.getObjectField(aweme, "author");
            if (author == null) {
                author = XposedHelpers.callMethod(aweme, "getAuthor");
            }
            if (author != null) {
                try {
                    region = (String) XposedHelpers.callMethod(author, "getRegion");
                } catch (Throwable ignored) {}
                if (TextUtils.isEmpty(region)) {
                    try {
                        region = (String) XposedHelpers.callMethod(author, "getAccountRegion");
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {}

        if (TextUtils.isEmpty(region)) {
            try {
                region = (String) XposedHelpers.callMethod(aweme, "getRegion");
            } catch (Throwable ignored) {}
        }

        if (!TextUtils.isEmpty(region)) {
            region = region.trim().toUpperCase(Locale.ROOT);
            if (region.length() == 2) {
                return region;
            }
        }
        return null;
    }

    public static String countryCodeToEmoji(String countryCode) {
        if (countryCode == null || countryCode.length() != 2) return null;
        String upper = countryCode.toUpperCase(Locale.ROOT);
        char c1 = upper.charAt(0);
        char c2 = upper.charAt(1);
        if (c1 < 'A' || c1 > 'Z' || c2 < 'A' || c2 > 'Z') return null;
        int cp1 = 0x1F1E6 + (c1 - 'A');
        int cp2 = 0x1F1E6 + (c2 - 'A');
        return new String(Character.toChars(cp1)) + new String(Character.toChars(cp2));
    }

    private static int dpToPx(Context context, float dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                context.getResources().getDisplayMetrics()
        );
    }

    public static void showTelemetryDialog(Context context, Object aweme) {
        TelemetryDialog.show(context, aweme);
    }
}
