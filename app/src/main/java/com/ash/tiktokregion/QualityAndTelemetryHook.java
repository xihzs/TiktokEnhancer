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

    private static final Handler sMainHandler = new Handler(Looper.getMainLooper());

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
            XC_MethodHook bitrateSorterHook = new XC_MethodHook() {
                @Override
                @SuppressWarnings("unchecked")
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!MainHook.isForceHighQualityEnabled()) return;
                    Object result = param.getResult();
                    if (result instanceof List) {
                        List<?> list = (List<?>) result;
                        if (list.size() > 1) {
                            param.setResult(createSortedBitrateList(list));
                        }
                    }
                }
            };

            XposedHelpers.findAndHookMethod(videoClass, "getBitRate", bitrateSorterHook);
            try {
                XposedHelpers.findAndHookMethod(videoClass, "getRawBitRate", bitrateSorterHook);
            } catch (Throwable ignored) {}

            Log.i(TAG, "Hooked Video.getBitRate / getRawBitRate to safely uncap 1080p60 & ByteVC1");
        } catch (Throwable t) {
            Log.d(TAG, "Failed hooking Video quality: " + t.getMessage());
        }
    }

    public static void hookVideoUrlModelQuality(Class<?> videoUrlClass) {
        if (videoUrlClass == null) return;
        try {
            XC_MethodHook bitrateSorterHook = new XC_MethodHook() {
                @Override
                @SuppressWarnings("unchecked")
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!MainHook.isForceHighQualityEnabled()) return;
                    Object result = param.getResult();
                    if (result instanceof List) {
                        List<?> list = (List<?>) result;
                        if (list.size() > 1) {
                            param.setResult(createSortedBitrateList(list));
                        }
                    }
                }
            };
            XposedHelpers.findAndHookMethod(videoUrlClass, "getBitRate", bitrateSorterHook);
            try {
                XposedHelpers.findAndHookMethod(videoUrlClass, "getRawBitRate", bitrateSorterHook);
            } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }

    public static void hookSimVideo(Class<?> simVideoClass) {
        if (simVideoClass == null) return;
        try {
            XposedHelpers.findAndHookMethod(simVideoClass, "getBitRate", new XC_MethodHook() {
                @Override
                @SuppressWarnings("unchecked")
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!MainHook.isForceHighQualityEnabled()) return;
                    Object result = param.getResult();
                    if (result instanceof List) {
                        List<?> list = (List<?>) result;
                        if (list.size() > 1) {
                            param.setResult(createSortedBitrateList(list));
                        }
                    }
                }
            });
        } catch (Throwable ignored) {}
    }

    public static void hookSimVideoQuality(Class<?> simVideoUrlClass) {
        if (simVideoUrlClass == null) return;
        try {
            XposedHelpers.findAndHookMethod(simVideoUrlClass, "getBitRate", new XC_MethodHook() {
                @Override
                @SuppressWarnings("unchecked")
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!MainHook.isForceHighQualityEnabled()) return;
                    Object result = param.getResult();
                    if (result instanceof List) {
                        List<?> list = (List<?>) result;
                        if (list.size() > 1) {
                            param.setResult(createSortedBitrateList(list));
                        }
                    }
                }
            });
            Log.i(TAG, "Hooked SimVideoUrlModel.getBitRate() safely");
        } catch (Throwable t) {
            Log.d(TAG, "Failed hooking SimVideoUrlModel.getBitRate(): " + t.getMessage());
        }
    }

    private static List<Object> createSortedBitrateList(List<?> original) {
        if (original == null || original.size() <= 1) {
            return (List<Object>) original;
        }
        List<Object> copy = new ArrayList<>(original);
        try {
            Collections.sort(copy, new Comparator<Object>() {
                @Override
                public int compare(Object o1, Object o2) {
                    if (o1 == null && o2 == null) return 0;
                    if (o1 == null) return 1;
                    if (o2 == null) return -1;
                    int r1 = 0, r2 = 0;
                    int b1 = 0, b2 = 0;
                    try { r1 = XposedHelpers.getIntField(o1, "bitRate"); } catch (Throwable ignored) {}
                    try { r2 = XposedHelpers.getIntField(o2, "bitRate"); } catch (Throwable ignored) {}
                    try { b1 = XposedHelpers.getIntField(o1, "isBytevc1"); } catch (Throwable ignored) {}
                    try { b2 = XposedHelpers.getIntField(o2, "isBytevc1"); } catch (Throwable ignored) {}

                    int score1 = r1 + (b1 > 0 ? 500000 : 0);
                    int score2 = r2 + (b2 > 0 ? 500000 : 0);
                    return Integer.compare(score2, score1);
                }
            });
            return copy;
        } catch (Throwable t) {
            return (List<Object>) original;
        }
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
                    disableClippingAllTheWayUp(root);
                    try {
                        View container = (View) XposedHelpers.getObjectField(param.thisObject, "LLLILZJ");
                        disableClippingAllTheWayUp(container);
                    } catch (Throwable ignored) {}
                }
            });

            XposedHelpers.findAndHookMethod(feedAvatarClass, "z4", Object.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    final Object assem = param.thisObject;
                    final Object itemParams = param.args[0];
                    if (Looper.myLooper() == Looper.getMainLooper()) {
                        updateAvatarFlagBadge(assem, itemParams);
                    } else {
                        sMainHandler.post(() -> updateAvatarFlagBadge(assem, itemParams));
                    }
                }
            });

            Log.i(TAG, "Hooked FeedAvatarDefaultAssem to display creator country flag above PFP");
        } catch (Throwable t) {
            Log.d(TAG, "hookFeedAvatar failed: " + t.getMessage());
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
                Log.d(TAG, "updateAvatarFlagBadge: container is null");
                return;
            }
            final ViewGroup targetContainer = container;

            TextView flagBadge = targetContainer.findViewWithTag(FLAG_BADGE_TAG);

            if (!MainHook.isTelemetryHUDEnabled() || itemParams == null) {
                if (flagBadge != null) {
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
                if (flagBadge != null) {
                    flagBadge.setVisibility(View.GONE);
                }
                return;
            }

            String countryIso = extractCountryIso(aweme);
            if (TextUtils.isEmpty(countryIso)) {
                if (flagBadge != null) {
                    flagBadge.setVisibility(View.GONE);
                }
                return;
            }

            String flagEmoji = countryCodeToEmoji(countryIso);
            String badgeText = (flagEmoji != null ? flagEmoji + " " : "") + countryIso;
            Log.i(TAG, "Binding PFP flag badge: country=" + countryIso + " (" + badgeText + ")");

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

                flagBadge.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                    @Override
                    public void onViewAttachedToWindow(View v) {
                        disableClippingAllTheWayUp(v);
                        v.post(() -> disableClippingAllTheWayUp(v));
                    }
                    @Override
                    public void onViewDetachedFromWindow(View v) {}
                });

                disableClippingAllTheWayUp(targetContainer);
                targetContainer.addView(flagBadge, lp);
            } else {
                flagBadge.setBackground(null);
                flagBadge.setPadding(0, 0, 0, 0);
                flagBadge.setTranslationY(-dpToPx(context, 7));
                flagBadge.setShadowLayer(dpToPx(context, 2.5f), 0, dpToPx(context, 1f), Color.parseColor("#E6000000"));
                disableClippingAllTheWayUp(targetContainer);
            }

            targetContainer.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int l, int t, int r, int b, int ol, int ot, int or, int ob) {
                    disableClippingAllTheWayUp(v);
                }
            });

            targetContainer.post(() -> disableClippingAllTheWayUp(targetContainer));

            flagBadge.setText(badgeText);
            flagBadge.setVisibility(View.VISIBLE);
            flagBadge.bringToFront();
            flagBadge.setZ(100f);

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

    private static void disableClippingAllTheWayUp(View view) {
        if (view == null) return;
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            vg.setClipChildren(false);
            vg.setClipToPadding(false);
        }
        ViewParent current = view.getParent();
        int depth = 0;
        while (current != null && depth < 30) {
            if (current instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) current;
                vg.setClipChildren(false);
                vg.setClipToPadding(false);
            }
            current = current.getParent();
            depth++;
        }
    }

    private static int dpToPx(Context context, float dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                context.getResources().getDisplayMetrics()
        );
    }

    public static void showTelemetryDialog(Context context, Object aweme) {
        if (context == null || aweme == null) return;
        Activity activity = null;
        Context ctx = context;
        while (ctx instanceof ContextWrapper) {
            if (ctx instanceof Activity) {
                activity = (Activity) ctx;
                break;
            }
            ctx = ((ContextWrapper) ctx).getBaseContext();
        }
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        try {
            Dialog dialog = new Dialog(activity);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setCanceledOnTouchOutside(true);
            dialog.setCancelable(true);

            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                WindowManager.LayoutParams wlp = new WindowManager.LayoutParams();
                wlp.copyFrom(window.getAttributes());
                DisplayMetrics dm = activity.getResources().getDisplayMetrics();
                wlp.width = Math.min((int) (dm.widthPixels * 0.92f), dpToPx(activity, 390));
                wlp.height = WindowManager.LayoutParams.WRAP_CONTENT;
                wlp.gravity = Gravity.CENTER;
                window.setAttributes(wlp);
            }

            GradientDrawable rootBg = new GradientDrawable();
            rootBg.setColor(Color.parseColor("#111319"));
            rootBg.setCornerRadius(dpToPx(activity, 12));
            rootBg.setStroke(dpToPx(activity, 1), Color.parseColor("#222633"));

            LinearLayout root = new LinearLayout(activity);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setBackground(rootBg);
            root.setPadding(dpToPx(activity, 14), dpToPx(activity, 14), dpToPx(activity, 14), dpToPx(activity, 14));

            LinearLayout header = new LinearLayout(activity);
            header.setOrientation(LinearLayout.HORIZONTAL);
            header.setGravity(Gravity.CENTER_VERTICAL);
            header.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            LinearLayout headerTextCol = new LinearLayout(activity);
            headerTextCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams headerTextColLp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            headerTextCol.setLayoutParams(headerTextColLp);

            TextView tvTitle = new TextView(activity);
            tvTitle.setText("Telemetry Details");
            tvTitle.setTextColor(Color.parseColor("#FFFFFF"));
            tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
            tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
            headerTextCol.addView(tvTitle);

            Object author = getSafeObject(aweme, "getAuthor", "author");
            String handle = getSafeString(author, "getUniqueId", "uniqueId");
            String countryIso = extractCountryIso(aweme);
            String flagEmoji = countryCodeToEmoji(countryIso);
            String aid = getSafeString(aweme, "getAid", "aid");

            String countryName = getCountryName(countryIso);
            String countryPart = "";
            if (!TextUtils.isEmpty(countryIso)) {
                countryPart = " • " + (flagEmoji != null ? flagEmoji + " " : "")
                        + (!TextUtils.isEmpty(countryName) ? countryName + " (" + countryIso + ")" : countryIso);
            }
            TextView tvSub = new TextView(activity);
            String subText = (!TextUtils.isEmpty(handle) ? "@" + handle : "Unknown Creator") + countryPart;
            tvSub.setText(subText);
            tvSub.setTextColor(Color.parseColor("#8E95A5"));
            tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
            tvSub.setEllipsize(TextUtils.TruncateAt.END);
            tvSub.setSingleLine(true);
            LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            subLp.topMargin = dpToPx(activity, 2);
            tvSub.setLayoutParams(subLp);
            headerTextCol.addView(tvSub);

            header.addView(headerTextCol);

            TextView btnClose = new TextView(activity);
            btnClose.setText("✕");
            btnClose.setTextColor(Color.parseColor("#E5E7EB"));
            btnClose.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
            btnClose.setTypeface(Typeface.DEFAULT_BOLD);
            btnClose.setGravity(Gravity.CENTER);

            GradientDrawable closeBg = new GradientDrawable();
            closeBg.setShape(GradientDrawable.OVAL);
            closeBg.setColor(Color.parseColor("#1C1F28"));
            closeBg.setStroke(dpToPx(activity, 1), Color.parseColor("#2C3140"));
            btnClose.setBackground(closeBg);

            int btnSize = dpToPx(activity, 28);
            LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(btnSize, btnSize);
            btnClose.setLayoutParams(btnLp);
            btnClose.setOnClickListener(v -> dialog.dismiss());
            header.addView(btnClose);

            root.addView(header);

            View div = new View(activity);
            LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(activity, 1));
            divLp.topMargin = dpToPx(activity, 10);
            divLp.bottomMargin = dpToPx(activity, 10);
            div.setLayoutParams(divLp);
            div.setBackgroundColor(Color.parseColor("#1E222D"));
            root.addView(div);

            ScrollView scrollView = new ScrollView(activity) {
                @Override
                protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                    int maxHeight = (int) (getContext().getResources().getDisplayMetrics().heightPixels * 0.70f);
                    super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST));
                }
            };
            scrollView.setVerticalScrollBarEnabled(false);

            LinearLayout scrollContent = new LinearLayout(activity);
            scrollContent.setOrientation(LinearLayout.VERTICAL);

            LinearLayout cardUser = createSectionCard(activity, scrollContent, "CREATOR & LOCATION");
            String nickname = getSafeString(author, "getNickname", "nickname");
            String uid = getSafeString(author, "getUid", "uid");
            String accountRegion = getSafeString(author, "getAccountRegion", "accountRegion");
            String authorRegion = getSafeString(author, "getRegion", "region");
            String cityName = getSafeString(author, "getCityName", "cityName");
            String bioLocation = getSafeString(author, "getBioLocation", "bioLocation");
            long regTime = getSafeLong(author, "getCreateTime", "createTime");
            if (regTime == 0) regTime = getSafeLong(author, "getRegisterTime", "registerTime");
            long followers = getSafeLong(author, "getFollowerCount", "followerCount");
            long awemeCount = getSafeLong(author, "getAwemeCount", "awemeCount");

            if (!TextUtils.isEmpty(nickname)) {
                addTelemetryRow(activity, cardUser, "Nickname", nickname, false, false, null, false);
            }
            if (!TextUtils.isEmpty(handle)) {
                addTelemetryRow(activity, cardUser, "Handle", "@" + handle, false, false, handle, false);
            }
            if (!TextUtils.isEmpty(uid)) {
                addTelemetryRow(activity, cardUser, "User ID", uid, true, true, uid, false);
            }
            if (!TextUtils.isEmpty(accountRegion)) {
                String regEmoji = countryCodeToEmoji(accountRegion);
                String regName = getCountryName(accountRegion);
                String regDisplay = (regEmoji != null ? regEmoji + " " : "")
                        + (!TextUtils.isEmpty(regName) ? regName + " (" + accountRegion.toUpperCase(Locale.ROOT) + ")" : accountRegion.toUpperCase(Locale.ROOT));
                addTelemetryRow(activity, cardUser, "Account Region", regDisplay, false, false, null, false);
            }
            if (!TextUtils.isEmpty(authorRegion) && !authorRegion.equalsIgnoreCase(accountRegion)) {
                String profEmoji = countryCodeToEmoji(authorRegion);
                String profName = getCountryName(authorRegion);
                String profDisplay = (profEmoji != null ? profEmoji + " " : "")
                        + (!TextUtils.isEmpty(profName) ? profName + " (" + authorRegion.toUpperCase(Locale.ROOT) + ")" : authorRegion.toUpperCase(Locale.ROOT));
                addTelemetryRow(activity, cardUser, "Profile Region", profDisplay, false, false, null, false);
            }
            String authorLang = getSafeString(author, "getLanguage", "language");
            if (!TextUtils.isEmpty(authorLang)) {
                addTelemetryRow(activity, cardUser, "User Language", getLanguageDisplayName(authorLang), false, false, null, false);
            }
            if (!TextUtils.isEmpty(cityName)) {
                addTelemetryRow(activity, cardUser, "City", cityName, false, false, null, false);
            }
            if (!TextUtils.isEmpty(bioLocation)) {
                addTelemetryRow(activity, cardUser, "Bio Location", bioLocation, false, false, null, false);
            }
            if (regTime > 0) {
                addTelemetryRow(activity, cardUser, "Registered", formatTimestamp(regTime), true, false, null, false);
            }
            if (followers > 0) {
                addTelemetryRow(activity, cardUser, "Followers", formatNumber(followers), true, false, null, false);
            }
            if (awemeCount > 0) {
                addTelemetryRow(activity, cardUser, "Total Videos", formatNumber(awemeCount), true, false, null, true);
            } else {
                trimLastDivider(cardUser);
            }

            LinearLayout cardVideo = createSectionCard(activity, scrollContent, "VIDEO & ENCODING");
            if (!TextUtils.isEmpty(aid)) {
                addTelemetryRow(activity, cardVideo, "Video ID (AID)", aid, true, true, aid, false);
            }
            long videoCreated = getSafeLong(aweme, "getCreateTime", "createTime");
            if (videoCreated > 0) {
                addTelemetryRow(activity, cardVideo, "Posted Time", formatTimestamp(videoCreated), true, false, null, false);
            }

            Object video = getSafeObject(aweme, "getVideo", "video");
            int width = getSafeInt(video, "getWidth", "width");
            int height = getSafeInt(video, "getHeight", "height");
            String ratio = getSafeString(video, "getRatio", "ratio");
            int duration = getSafeInt(video, "getVideoLength", "videoLength");
            if (duration <= 0) duration = getSafeInt(video, "getDuration", "duration");

            List<?> bitRates = getSafeList(video, "getBitRate", "bitRate");
            int maxStreamWidth = 0;
            int maxStreamHeight = 0;

            if (bitRates != null && !bitRates.isEmpty()) {
                for (Object brItem : bitRates) {
                    if (brItem == null) continue;
                    int bw = getSafeInt(brItem, "getVideoWidth", "videoWidth");
                    int bh = getSafeInt(brItem, "getVideoHeight", "videoHeight");
                    if (bw <= 0 || bh <= 0) {
                        Object playAddr = getSafeObject(brItem, "getPlayAddr", "playAddr");
                        if (playAddr != null) {
                            bw = getSafeInt(playAddr, "getWidth", "width");
                            bh = getSafeInt(playAddr, "getHeight", "height");
                        }
                    }
                    if (bw <= 0 || bh <= 0) {
                        String gear = getSafeString(brItem, "getGearName", "gearName");
                        if (gear != null) {
                            if (gear.contains("2160") || gear.contains("4k") || gear.contains("4K")) {
                                bw = 2160; bh = 3840;
                            } else if (gear.contains("1080")) {
                                bw = 1080; bh = 1920;
                            } else if (gear.contains("720")) {
                                bw = 720; bh = 1280;
                            }
                        }
                    }
                    if (bw > maxStreamWidth) {
                        maxStreamWidth = bw;
                        maxStreamHeight = bh;
                    }
                }
            }

            if (maxStreamWidth <= 0) {
                Object playAddr = getSafeObject(video, "getPlayAddr", "playAddr");
                if (playAddr != null) {
                    int pw = getSafeInt(playAddr, "getWidth", "width");
                    int ph = getSafeInt(playAddr, "getHeight", "height");
                    if (pw > maxStreamWidth) {
                        maxStreamWidth = pw;
                        maxStreamHeight = ph;
                    }
                }
            }

            if (maxStreamWidth <= 0 && !TextUtils.isEmpty(ratio)) {
                if (ratio.contains("1080")) {
                    maxStreamWidth = 1080;
                    maxStreamHeight = 1920;
                } else if (ratio.contains("720")) {
                    maxStreamWidth = 720;
                    maxStreamHeight = 1280;
                } else if (ratio.contains("4k") || ratio.contains("2160")) {
                    maxStreamWidth = 2160;
                    maxStreamHeight = 3840;
                }
            }

            if (maxStreamWidth > 0 && maxStreamHeight > 0) {
                if (width > 0 && height > 0 && width > height && maxStreamWidth < maxStreamHeight) {
                    int tmp = maxStreamWidth;
                    maxStreamWidth = maxStreamHeight;
                    maxStreamHeight = tmp;
                }
                width = maxStreamWidth;
                height = maxStreamHeight;
            }

            if (width > 0 && height > 0) {
                String qualityLabel = "";
                if (Math.min(width, height) >= 2160) {
                    qualityLabel = " (4K UHD)";
                } else if (Math.min(width, height) >= 1080) {
                    qualityLabel = " (1080p HD)";
                } else if (Math.min(width, height) >= 720) {
                    qualityLabel = " (720p HD)";
                } else if (Math.min(width, height) <= 540) {
                    qualityLabel = " (540p SD)";
                }
                String resStr = width + " × " + height + qualityLabel;
                addTelemetryRow(activity, cardVideo, "Resolution", resStr, true, false, null, false);
            }
            if (duration > 0) {
                addTelemetryRow(activity, cardVideo, "Duration", formatDuration(duration), true, false, null, false);
            }

            if (bitRates != null && !bitRates.isEmpty()) {
                Object top = bitRates.get(0);
                int topBr = getSafeInt(top, "getBitRate", "bitRate");
                int topBytevc = getSafeInt(top, "isBytevc1", "isBytevc1");
                String codecStr = (topBytevc == 1) ? "ByteVC1 (H.265)" : (topBytevc == 2 ? "AV1" : "H.264 (AVC)");
                addTelemetryRow(activity, cardVideo, "Active Codec", codecStr, true, true, null, false);
                if (topBr > 0) {
                    addTelemetryRow(activity, cardVideo, "Top Bitrate", String.format(Locale.US, "%,d kbps", topBr / 1000), true, false, null, false);
                }

                StringBuilder ladders = new StringBuilder();
                int count = Math.min(bitRates.size(), 4);
                for (int i = 0; i < count; i++) {
                    Object brItem = bitRates.get(i);
                    if (brItem == null) continue;
                    String gear = getSafeString(brItem, "getGearName", "gearName");
                    int br = getSafeInt(brItem, "getBitRate", "bitRate");
                    int bvc = getSafeInt(brItem, "isBytevc1", "isBytevc1");
                    int bw = getSafeInt(brItem, "getVideoWidth", "videoWidth");
                    if (bw <= 0) {
                        Object pa = getSafeObject(brItem, "getPlayAddr", "playAddr");
                        if (pa != null) bw = getSafeInt(pa, "getWidth", "width");
                    }
                    String cName = (bvc == 1) ? "ByteVC1" : (bvc == 2 ? "AV1" : "H.264");
                    if (ladders.length() > 0) ladders.append("\n");
                    String tierName = (bw > 0) ? (bw + "p") : (gear != null ? gear : "tier");
                    ladders.append(tierName).append(" · ").append(cName);
                    if (br > 0) ladders.append(" · ").append(br / 1000).append("k");
                }
                if (ladders.length() > 0) {
                    addTelemetryRow(activity, cardVideo, "Ladder Tiers", ladders.toString(), true, false, null, true);
                } else {
                    trimLastDivider(cardVideo);
                }
            } else {
                trimLastDivider(cardVideo);
            }

            String descLang = getSafeString(aweme, "getDescLanguage", "descLanguage");
            String stickerLang = getSafeString(aweme, "getTextStickerMajorityLang", "textStickerMajorityLang");
            Object music = getSafeObject(aweme, "getMusic", "music");
            String musicTitle = getSafeString(music, "getTitle", "title");
            String musicAuthor = getSafeString(music, "getAuthorName", "authorName");
            String musicId = getSafeString(music, "getMid", "mid");

            boolean hasAudioOrLang = !TextUtils.isEmpty(descLang) || !TextUtils.isEmpty(stickerLang)
                    || !TextUtils.isEmpty(musicTitle) || !TextUtils.isEmpty(musicAuthor);

            if (hasAudioOrLang) {
                LinearLayout cardAudio = createSectionCard(activity, scrollContent, "AUDIO & LANGUAGE");
                if (!TextUtils.isEmpty(descLang)) {
                    addTelemetryRow(activity, cardAudio, "Caption Lang", getLanguageDisplayName(descLang), false, false, null, false);
                }
                if (!TextUtils.isEmpty(stickerLang)) {
                    addTelemetryRow(activity, cardAudio, "Sticker Lang", getLanguageDisplayName(stickerLang), false, false, null, false);
                }
                if (!TextUtils.isEmpty(musicTitle)) {
                    String audioStr = musicTitle + (!TextUtils.isEmpty(musicAuthor) ? " - " + musicAuthor : "");
                    addTelemetryRow(activity, cardAudio, "Sound Title", audioStr, false, false, musicTitle, false);
                }
                if (!TextUtils.isEmpty(musicId)) {
                    addTelemetryRow(activity, cardAudio, "Sound ID", musicId, true, false, musicId, true);
                } else {
                    trimLastDivider(cardAudio);
                }
            }

            Object stats = getSafeObject(aweme, "getStatistics", "statistics");
            if (stats != null) {
                long plays = getSafeLong(stats, "getPlayCount", "playCount");
                long diggs = getSafeLong(stats, "getDiggCount", "diggCount");
                long comments = getSafeLong(stats, "getCommentCount", "commentCount");
                long shares = getSafeLong(stats, "getShareCount", "shareCount");
                long collects = getSafeLong(stats, "getCollectCount", "collectCount");

                boolean hasStats = (plays > 0 || diggs > 0 || comments > 0 || shares > 0 || collects > 0);
                if (hasStats) {
                    LinearLayout cardStats = createSectionCard(activity, scrollContent, "ENGAGEMENT METRICS");
                    if (plays > 0) {
                        addTelemetryRow(activity, cardStats, "Plays", formatNumber(plays), true, false, null, false);
                    }
                    if (diggs > 0) {
                        addTelemetryRow(activity, cardStats, "Likes", formatNumber(diggs), true, false, null, false);
                    }
                    if (comments > 0) {
                        addTelemetryRow(activity, cardStats, "Comments", formatNumber(comments), true, false, null, false);
                    }
                    if (shares > 0) {
                        addTelemetryRow(activity, cardStats, "Shares", formatNumber(shares), true, false, null, false);
                    }
                    if (collects > 0) {
                        addTelemetryRow(activity, cardStats, "Saves", formatNumber(collects), true, false, null, true);
                    } else {
                        trimLastDivider(cardStats);
                    }
                }
            }

            scrollView.addView(scrollContent);
            root.addView(scrollView);

            dialog.setContentView(root);
            dialog.show();

        } catch (Throwable t) {
            Log.d(TAG, "showTelemetryDialog error: " + t.getMessage());
        }
    }

    private static LinearLayout createSectionCard(Activity activity, LinearLayout parent, String title) {
        TextView tvSection = new TextView(activity);
        tvSection.setText(title);
        tvSection.setTextColor(Color.parseColor("#7E8699"));
        tvSection.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f);
        tvSection.setTypeface(Typeface.DEFAULT_BOLD);
        tvSection.setLetterSpacing(0.08f);
        LinearLayout.LayoutParams secLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        secLp.topMargin = dpToPx(activity, 6);
        secLp.bottomMargin = dpToPx(activity, 3);
        secLp.leftMargin = dpToPx(activity, 4);
        tvSection.setLayoutParams(secLp);
        parent.addView(tvSection);

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.parseColor("#151821"));
        cardBg.setCornerRadius(dpToPx(activity, 8));
        cardBg.setStroke(dpToPx(activity, 1), Color.parseColor("#202432"));

        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(cardBg);
        card.setPadding(dpToPx(activity, 12), dpToPx(activity, 4), dpToPx(activity, 12), dpToPx(activity, 4));
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.bottomMargin = dpToPx(activity, 6);
        card.setLayoutParams(cardLp);
        parent.addView(card);
        return card;
    }

    private static void addTelemetryRow(Activity activity, LinearLayout card, String label, String value,
                                        boolean isMonospace, boolean isAccent, String copyText, boolean isLast) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        row.setLayoutParams(rowLp);
        row.setPadding(0, dpToPx(activity, 6), 0, dpToPx(activity, 6));

        TextView tvLabel = new TextView(activity);
        tvLabel.setText(label);
        tvLabel.setTextColor(Color.parseColor("#8E95A5"));
        tvLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        labelLp.rightMargin = dpToPx(activity, 12);
        tvLabel.setLayoutParams(labelLp);
        row.addView(tvLabel);

        TextView tvValue = new TextView(activity);
        tvValue.setText(value);
        tvValue.setTextColor(Color.parseColor("#FFFFFF"));
        tvValue.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        tvValue.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        if (isMonospace) {
            tvValue.setTypeface(Typeface.MONOSPACE);
        }
        LinearLayout.LayoutParams valLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        tvValue.setLayoutParams(valLp);
        row.addView(tvValue);

        if (!TextUtils.isEmpty(copyText)) {
            row.setClickable(true);
            row.setFocusable(true);
            row.setOnClickListener(v -> copyToClipboard(activity, label, copyText));
        }

        card.addView(row);

        if (!isLast) {
            View div = new View(activity);
            LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(activity, 0.6f));
            div.setLayoutParams(divLp);
            div.setBackgroundColor(Color.parseColor("#1B1F2A"));
            card.addView(div);
        }
    }

    private static void trimLastDivider(LinearLayout card) {
        if (card != null && card.getChildCount() > 0) {
            View last = card.getChildAt(card.getChildCount() - 1);
            if (last != null && !(last instanceof LinearLayout)) {
                card.removeView(last);
            }
        }
    }

    private static void copyToClipboard(Context context, String label, String text) {
        if (context == null || text == null) return;
        try {
            ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText(label, text));
                Toast.makeText(context, "Copied " + label + ": " + text, Toast.LENGTH_SHORT).show();
            }
        } catch (Throwable ignored) {}
    }

    private static String formatTimestamp(long timestamp) {
        if (timestamp <= 0) return "N/A";
        long timeMs = timestamp;
        if (timeMs < 10000000000L) {
            timeMs *= 1000L;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String dateStr = sdf.format(new Date(timeMs)) + " UTC";

        long now = System.currentTimeMillis();
        long diff = now - timeMs;
        if (diff > 0) {
            long days = diff / (1000L * 60 * 60 * 24);
            if (days >= 365) {
                long years = days / 365;
                long months = (days % 365) / 30;
                dateStr += " (" + years + "y " + (months > 0 ? months + "m " : "") + "ago)";
            } else if (days >= 30) {
                long months = days / 30;
                dateStr += " (" + months + "mo ago)";
            } else if (days >= 1) {
                dateStr += " (" + days + "d ago)";
            } else {
                long hours = diff / (1000L * 60 * 60);
                dateStr += " (" + hours + "h ago)";
            }
        }
        return dateStr;
    }

    private static String formatNumber(long count) {
        if (count < 0) return "0";
        if (count >= 1000000) {
            return String.format(Locale.US, "%.1fM (%,d)", count / 1000000.0, count);
        } else if (count >= 1000) {
            return String.format(Locale.US, "%.1fK (%,d)", count / 1000.0, count);
        }
        return String.valueOf(count);
    }

    private static String formatDuration(int durationVal) {
        if (durationVal <= 0) return "N/A";
        float exactSec;
        int totalSec;
        if (durationVal >= 1000) {
            exactSec = durationVal / 1000.0f;
            totalSec = Math.round(exactSec);
        } else {
            exactSec = (float) durationVal;
            totalSec = durationVal;
        }
        int m = totalSec / 60;
        int s = totalSec % 60;
        if (exactSec < 60) {
            return String.format(Locale.US, "%02d:%02d (%.1fs)", m, s, exactSec);
        } else {
            return String.format(Locale.US, "%02d:%02d (%ds)", m, s, totalSec);
        }
    }

    private static String getCountryName(String code) {
        if (TextUtils.isEmpty(code) || code.length() != 2) return "";
        try {
            Locale l = new Locale("", code.toUpperCase(Locale.ROOT));
            return l.getDisplayCountry(Locale.ENGLISH);
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static String getLanguageDisplayName(String code) {
        if (TextUtils.isEmpty(code)) return "";
        String clean = code.trim().toLowerCase(Locale.ROOT);
        if ("un".equals(clean) || "und".equals(clean)) {
            return "Undetermined (un)";
        }
        try {
            Locale l = Locale.forLanguageTag(clean.replace('_', '-'));
            String display = l.getDisplayLanguage(Locale.ENGLISH);
            if (!TextUtils.isEmpty(display)) {
                return display + " (" + clean + ")";
            }
        } catch (Throwable ignored) {}
        try {
            Locale l = new Locale(clean);
            String display = l.getDisplayLanguage(Locale.ENGLISH);
            if (!TextUtils.isEmpty(display)) {
                return display + " (" + clean + ")";
            }
        } catch (Throwable ignored) {}
        return clean;
    }

    private static Object getSafeObject(Object target, String getter, String field) {
        if (target == null) return null;
        try {
            return XposedHelpers.callMethod(target, getter);
        } catch (Throwable ignored) {}
        try {
            return XposedHelpers.getObjectField(target, field);
        } catch (Throwable ignored) {}
        return null;
    }

    private static String getSafeString(Object target, String getter, String field) {
        if (target == null) return null;
        try {
            Object res = XposedHelpers.callMethod(target, getter);
            if (res instanceof String) return (String) res;
        } catch (Throwable ignored) {}
        try {
            Object res = XposedHelpers.getObjectField(target, field);
            if (res instanceof String) return (String) res;
        } catch (Throwable ignored) {}
        return null;
    }

    private static long getSafeLong(Object target, String getter, String field) {
        if (target == null) return 0L;
        try {
            Object res = XposedHelpers.callMethod(target, getter);
            if (res instanceof Number) return ((Number) res).longValue();
        } catch (Throwable ignored) {}
        try {
            return XposedHelpers.getLongField(target, field);
        } catch (Throwable ignored) {}
        try {
            return XposedHelpers.getIntField(target, field);
        } catch (Throwable ignored) {}
        return 0L;
    }

    private static int getSafeInt(Object target, String getter, String field) {
        if (target == null) return 0;
        try {
            Object res = XposedHelpers.callMethod(target, getter);
            if (res instanceof Number) return ((Number) res).intValue();
        } catch (Throwable ignored) {}
        try {
            return XposedHelpers.getIntField(target, field);
        } catch (Throwable ignored) {}
        return 0;
    }

    private static List<?> getSafeList(Object target, String getter, String field) {
        if (target == null) return Collections.emptyList();
        try {
            Object res = XposedHelpers.callMethod(target, getter);
            if (res instanceof List) return (List<?>) res;
        } catch (Throwable ignored) {}
        try {
            Object res = XposedHelpers.getObjectField(target, field);
            if (res instanceof List) return (List<?>) res;
        } catch (Throwable ignored) {}
        return Collections.emptyList();
    }
}
