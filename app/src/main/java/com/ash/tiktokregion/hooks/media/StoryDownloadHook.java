package com.ash.tiktokregion.hooks.media;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ash.tiktokregion.core.BaseHook;
import com.ash.tiktokregion.core.ModuleConfig;
import com.ash.tiktokregion.hooks.network.UrlSanitizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Handles TikTok 24h story unlocking, story aweme tracking, and share sheet "Save Story" injection.
 */
public class StoryDownloadHook implements BaseHook {

    private static final String TAG = "TikTokStoryDownload";
    private static volatile Object sCurrentAweme = null;
    private static final List<Object> sCurrentStoryList = Collections.synchronizedList(new ArrayList<>());
    private static final Set<String> sKnownStoryAids = Collections.synchronizedSet(new HashSet<>());
    private static final Set<Object> sKnownStoryAwemes = Collections.synchronizedSet(Collections.newSetFromMap(new java.util.WeakHashMap<>()));

    @Override
    public void hook(ClassLoader classLoader) {
        hookStoryTracking(classLoader);
        hookShareSheetDialog(classLoader);
    }

    private void hookStoryTracking(ClassLoader classLoader) {
        final String awemeClass = "com.ss.android.ugc.aweme.feed.model.Aweme";
        final String userStoryClass = "com.ss.android.ugc.aweme.feed.model.story.UserStory";

        XC_MethodHook trackAwemeHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (param.thisObject != null) {
                    sCurrentAweme = param.thisObject;
                    if (!isStoryAweme(param.thisObject)) {
                        synchronized (sCurrentStoryList) {
                            sCurrentStoryList.clear();
                        }
                    }
                }
            }
        };

        try {
            XposedHelpers.findAndHookMethod(awemeClass, classLoader, "getVideo", trackAwemeHook);
            XposedHelpers.findAndHookMethod(awemeClass, classLoader, "getImageInfos", trackAwemeHook);
            XposedHelpers.findAndHookMethod(awemeClass, classLoader, "getPhotoModeImageInfo", trackAwemeHook);
        } catch (Throwable ignored) {}

        try {
            XposedHelpers.findAndHookMethod(userStoryClass, classLoader, "getStories", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!ModuleConfig.downloadStory) return;
                    Object result = param.getResult();
                    if (result instanceof List) {
                        List<?> list = (List<?>) result;
                        synchronized (sCurrentStoryList) {
                            sCurrentStoryList.clear();
                            sCurrentStoryList.addAll(list);
                        }
                        for (Object item : list) {
                            if (item != null) {
                                trackStoryAweme(item);
                            }
                        }
                        long currentPos = 0;
                        try {
                            Object posObj = XposedHelpers.callMethod(param.thisObject, "getCurrentPosition");
                            if (posObj instanceof Number) {
                                currentPos = ((Number) posObj).longValue();
                            }
                        } catch (Throwable ignored) {}

                        if (currentPos >= 0 && currentPos < list.size() && list.get((int) currentPos) != null) {
                            sCurrentAweme = list.get((int) currentPos);
                        } else if (!list.isEmpty() && list.get(0) != null) {
                            sCurrentAweme = list.get(0);
                        }
                    }
                }
            });
            XposedBridge.log(TAG + ": Hooked UserStory.getStories()");
        } catch (Throwable t) {
            Log.d(TAG, "UserStory.getStories hook failed: " + t.getMessage());
        }

        try {
            XposedHelpers.findAndHookMethod(userStoryClass, classLoader, "setCurrentPosition", long.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!ModuleConfig.downloadStory || param.args == null || param.args.length == 0) return;
                    try {
                        long pos = ((Number) param.args[0]).longValue();
                        synchronized (sCurrentStoryList) {
                            if (pos >= 0 && pos < sCurrentStoryList.size()) {
                                Object current = sCurrentStoryList.get((int) pos);
                                if (current != null) {
                                    sCurrentAweme = current;
                                    trackStoryAweme(current);
                                }
                            }
                        }
                    } catch (Throwable ignored) {}
                }
            });
            XposedBridge.log(TAG + ": Hooked UserStory.setCurrentPosition(long)");
        } catch (Throwable t) {
            Log.d(TAG, "UserStory.setCurrentPosition hook fallback: " + t.getMessage());
        }

        try {
            XposedHelpers.findAndHookMethod(awemeClass, classLoader, "getUserStory", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!ModuleConfig.downloadStory) return;
                    Object userStory = param.getResult();
                    if (userStory != null) {
                        try {
                            List<?> stories = (List<?>) XposedHelpers.callMethod(userStory, "getStories");
                            if (stories != null && !stories.isEmpty()) {
                                for (Object item : stories) {
                                    if (item != null) trackStoryAweme(item);
                                }
                            }
                        } catch (Throwable ignored) {}
                    }
                }
            });
        } catch (Throwable ignored) {}
    }

    private static void trackStoryAweme(Object aweme) {
        if (aweme == null) return;
        sKnownStoryAwemes.add(aweme);
        try {
            String aid = (String) XposedHelpers.callMethod(aweme, "getAid");
            if (aid != null && !aid.isEmpty()) {
                sKnownStoryAids.add(aid);
            }
        } catch (Throwable ignored) {}
    }

    public static boolean isStoryAweme(Object aweme) {
        if (aweme == null) return false;
        if (sKnownStoryAwemes.contains(aweme)) return true;

        String aid = getAwemeAid(aweme);
        if (aid != null && !aid.isEmpty() && sKnownStoryAids.contains(aid)) return true;

        synchronized (sCurrentStoryList) {
            if (sCurrentStoryList.contains(aweme)) return true;
            if (aid != null && !aid.isEmpty()) {
                for (Object s : sCurrentStoryList) {
                    if (s != null && aid.equals(getAwemeAid(s))) return true;
                }
            }
        }

        try {
            Object typeObj = XposedHelpers.callMethod(aweme, "getAwemeType");
            if (typeObj instanceof Number) {
                int type = ((Number) typeObj).intValue();
                // 40: story video, 43: story photo, 150: story
                if (type == 40 || type == 43 || type == 150) return true;
                // Regular feed video (0), ad (2), normal photo mode album (68) are not stories
                if (type == 0 || type == 2 || type == 68) return false;
            }
        } catch (Throwable ignored) {}

        try {
            Object isStory = XposedHelpers.callMethod(aweme, "isStory");
            if (Boolean.TRUE.equals(isStory) || (isStory instanceof Number && ((Number) isStory).intValue() == 1)) {
                return true;
            }
        } catch (Throwable ignored) {}

        try {
            Object isTikTokStory = XposedHelpers.callMethod(aweme, "getIsTikTokStory");
            if (Boolean.TRUE.equals(isTikTokStory) || (isTikTokStory instanceof Number && ((Number) isTikTokStory).intValue() == 1)) {
                return true;
            }
        } catch (Throwable ignored) {}

        try {
            Object story = XposedHelpers.callMethod(aweme, "getStory");
            if (story != null) {
                Object isStoryFlag = null;
                try { isStoryFlag = XposedHelpers.callMethod(story, "isStory"); } catch (Throwable ignored) {}
                if (Boolean.TRUE.equals(isStoryFlag)) return true;
            }
        } catch (Throwable ignored) {}

        try {
            Object userStory = XposedHelpers.callMethod(aweme, "getUserStory");
            if (userStory != null) {
                List<?> stories = (List<?>) XposedHelpers.callMethod(userStory, "getStories");
                if (stories != null && !stories.isEmpty()) {
                    if (stories.contains(aweme)) return true;
                    if (aid != null && !aid.isEmpty()) {
                        for (Object s : stories) {
                            if (s != null && aid.equals(getAwemeAid(s))) return true;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        return false;
    }

    private static String getAwemeAid(Object aweme) {
        if (aweme == null) return null;
        try {
            return (String) XposedHelpers.callMethod(aweme, "getAid");
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean isStoryDetailActivity(Activity activity) {
        if (activity == null) return false;
        if (isSavedVideoList(activity)) return false;

        String activityName = activity.getClass().getName();
        if (activityName.contains("Story") || activityName.contains("story")) {
            return true;
        }

        Intent intent = activity.getIntent();
        if (intent != null) {
            if (intent.getBooleanExtra("is_story", false)
                    || intent.getBooleanExtra("is_from_story", false)
                    || intent.hasExtra("story_id")) {
                return true;
            }
            Bundle extras = intent.getExtras();
            if (extras != null) {
                for (String key : extras.keySet()) {
                    if (key.toLowerCase(Locale.US).contains("story")) {
                        try {
                            Object val = extras.get(key);
                            if (Boolean.TRUE.equals(val) || (val instanceof String && ((String) val).toLowerCase(Locale.US).contains("story"))) {
                                return true;
                            }
                        } catch (Throwable ignored) {}
                    }
                }
            }
        }
        return false;
    }

    private static boolean isSavedVideoList(Activity activity) {
        if (activity == null) return false;
        String activityName = activity.getClass().getName();
        if (activityName.contains("Favorite") || activityName.contains("Bookmark")
                || activityName.contains("Collection")) {
            return true;
        }
        Intent intent = activity.getIntent();
        if (intent != null) {
            if (intent.getBooleanExtra("is_story", false)
                    || intent.getBooleanExtra("is_from_story", false)
                    || intent.hasExtra("story_id")) {
                return false;
            }
        }
        return false;
    }

    private void hookShareSheetDialog(ClassLoader classLoader) {
        XC_MethodHook dialogHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (!ModuleConfig.downloadStory) return;
                if (param.thisObject instanceof Dialog) {
                    attachSaveButtonToShareDialog((Dialog) param.thisObject);
                }
            }
        };

        try {
            XposedHelpers.findAndHookMethod("android.app.Dialog", classLoader, "show", dialogHook);
            XposedHelpers.findAndHookMethod("android.app.Dialog", classLoader, "onWindowFocusChanged", boolean.class, dialogHook);
        } catch (Throwable ignored) {}
    }

    private void attachSaveButtonToShareDialog(final Dialog dialog) {
        if (dialog == null) return;
        String dialogClassName = dialog.getClass().getName();
        if (dialogClassName.contains("Progress") || dialogClassName.contains("Loading")
                || dialogClassName.contains("Toast") || dialogClassName.contains("Alert")) {
            return;
        }

        final Window window = dialog.getWindow();
        if (window == null) return;
        final View decor = window.getDecorView();
        if (!(decor instanceof ViewGroup)) return;

        Runnable attachTask = () -> {
            try {
                if (!dialog.isShowing()) return;
                final ViewGroup decorGroup = (ViewGroup) decor;
                final String tag = "tiktok_enhancer_share_dl_btn";
                if (decorGroup.findViewWithTag(tag) != null) return;

                TextView reportTv = findActionTextView(decorGroup);
                if (reportTv == null) return;

                Context ctx = dialog.getContext();
                Activity activity = getActivityFromContext(ctx);
                if (activity == null) activity = dialog.getOwnerActivity();

                if (isSavedVideoList(activity)) return;

                Object currentAweme = (sCurrentAweme != null) ? sCurrentAweme : WatermarkBypassHook.getCurrentAweme();
                boolean isStory = isStoryAweme(currentAweme) || isStoryDetailActivity(activity);

                // The "Save Story" button must ONLY appear on 24-hour stories!
                // Normal videos already have native clean saving enabled by WatermarkBypassHook.
                if (!isStory) {
                    return;
                }

                if (!ModuleConfig.downloadStory) {
                    return;
                }

                if (hasNativeDownloadButton(decorGroup)) return;

                String buttonText = "Save Story";

                TextView saveBtn = new TextView(ctx);
                saveBtn.setTag(tag);
                saveBtn.setText(buttonText);
                saveBtn.setTextSize(12.5f);
                saveBtn.setTextColor(Color.WHITE);
                saveBtn.setGravity(Gravity.CENTER);
                saveBtn.setPadding(dpToPx(ctx, 16), dpToPx(ctx, 6), dpToPx(ctx, 16), dpToPx(ctx, 6));
                saveBtn.setElevation(dpToPx(ctx, 6));

                GradientDrawable bg = new GradientDrawable();
                bg.setShape(GradientDrawable.RECTANGLE);
                bg.setCornerRadius(dpToPx(ctx, 17));
                bg.setColor(Color.parseColor("#E6181920"));
                bg.setStroke(dpToPx(ctx, 1), Color.parseColor("#40FFFFFF"));
                saveBtn.setBackground(bg);

                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        dpToPx(ctx, 34)
                );
                lp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                lp.bottomMargin = dpToPx(ctx, 10);

                saveBtn.setOnClickListener(v -> {
                    try { dialog.dismiss(); } catch (Throwable ignored) {}
                    downloadAweme(ctx, currentAweme);
                });

                decorGroup.addView(saveBtn, lp);
            } catch (Throwable ignored) {}
        };

        decor.post(attachTask);
        decor.postDelayed(attachTask, 250);
    }

    private static boolean hasNativeDownloadButton(View view) {
        if (view == null) return false;
        if (view instanceof TextView && view.getVisibility() == View.VISIBLE) {
            CharSequence text = ((TextView) view).getText();
            if (text != null && isDownloadActionText(text.toString())) {
                return true;
            }
        }
        CharSequence cd = view.getContentDescription();
        if (cd != null && isDownloadActionText(cd.toString())) {
            return true;
        }
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                if (hasNativeDownloadButton(vg.getChildAt(i))) return true;
            }
        }
        return false;
    }

    private static boolean isDownloadActionText(String str) {
        if (str == null) return false;
        String s = str.trim().toLowerCase(Locale.US);
        return s.equals("download") || s.equals("save video") || s.equals("unduh")
                || s.equals("herunterladen") || s.equals("télécharger") || s.equals("descargar")
                || s.equals("guardar video") || s.equals("simpan video") || s.equals("lưu video")
                || s.equals("salvar vídeo") || s.equals("salvar video") || s.equals("scarica video")
                || s.equals("video speichern") || s.equals("videoyu kaydet") || s.equals("скачать")
                || s.equals("сохранить видео") || s.equals("บันทึกวิดีโอ") || s.equals("muat turun")
                || s.contains("保存本地") || s.contains("保存视频") || s.contains("下载")
                || s.contains("動画を保存") || s.contains("동영상 저장");
    }

    private static TextView findActionTextView(View view) {
        if (view instanceof TextView) {
            CharSequence text = ((TextView) view).getText();
            if (text != null) {
                String s = text.toString().trim().toLowerCase(Locale.US);
                if (s.equals("report") || s.equals("reportar") || s.equals("signaler")
                        || s.equals("laporkan") || s.equals("melden") || s.equals("báo cáo")
                        || s.equals("segnala") || s.equals("пожаловаться") || s.equals("bildir")
                        || s.contains("举报") || s.contains("报告")
                        || s.equals("通報") || s.equals("신고") || s.equals("copy link")
                        || s.equals("salin tautan") || s.equals("copiar enlace")) {
                    return (TextView) view;
                }
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                TextView found = findActionTextView(vg.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    public static void downloadAweme(Context ctx, Object aweme) {
        if (ctx == null) return;
        Object target = (aweme != null) ? aweme : sCurrentAweme;
        if (target == null) {
            target = WatermarkBypassHook.getCurrentAweme();
        }
        if (target == null && !sCurrentStoryList.isEmpty()) {
            synchronized (sCurrentStoryList) {
                if (!sCurrentStoryList.isEmpty()) target = sCurrentStoryList.get(0);
            }
        }

        MediaDownloader.StoryMedia media = extractStoryMedia(target);
        if (media == null || media.urls.isEmpty()) {
            Toast.makeText(ctx, "Could not resolve stream URL for saving", Toast.LENGTH_SHORT).show();
            return;
        }

        if (media.isVideo) {
            String url = UrlSanitizer.cleanVideoUrl(media.urls.get(0));
            String name = "TikTok_Story_" + media.aid + ".mp4";
            MediaDownloader.enqueueDownload(ctx, url, name, media.desc, "video/mp4");
        } else {
            for (int i = 0; i < media.urls.size(); i++) {
                String url = media.urls.get(i);
                String name = "TikTok_Story_" + media.aid + "_" + (i + 1) + ".jpeg";
                MediaDownloader.enqueueDownload(ctx, url, name, media.desc, "image/jpeg");
            }
        }
    }

    private static MediaDownloader.StoryMedia extractStoryMedia(Object aweme) {
        if (aweme == null) return null;
        MediaDownloader.StoryMedia media = new MediaDownloader.StoryMedia();

        try {
            media.aid = (String) XposedHelpers.callMethod(aweme, "getAid");
        } catch (Throwable ignored) {}
        if (media.aid == null || media.aid.isEmpty()) {
            media.aid = String.valueOf(System.currentTimeMillis());
        }

        try {
            media.desc = (String) XposedHelpers.callMethod(aweme, "getDesc");
        } catch (Throwable ignored) {}

        // Video
        try {
            Object video = XposedHelpers.callMethod(aweme, "getVideo");
            if (video != null) {
                Object bestAddr = VideoQualityHook.getHighestQualityPlayAddr(video);
                if (bestAddr != null) {
                    List<String> list = MediaDownloader.getUrlsFromUrlModel(bestAddr);
                    if (!list.isEmpty()) {
                        media.isVideo = true;
                        media.urls.addAll(list);
                        return media;
                    }
                }
                String[] addrMethods = {"getPlayAddr", "getProperPlayAddr", "getPlayAddrH264", "getDownloadAddr"};
                for (String m : addrMethods) {
                    try {
                        Object urlModel = XposedHelpers.callMethod(video, m);
                        if (urlModel != null) {
                            List<String> list = MediaDownloader.getUrlsFromUrlModel(urlModel);
                            if (!list.isEmpty()) {
                                media.isVideo = true;
                                media.urls.addAll(list);
                                return media;
                            }
                        }
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {}

        // Photo Mode
        try {
            Object photoModeInfo = XposedHelpers.callMethod(aweme, "getPhotoModeImageInfo");
            if (photoModeInfo != null) {
                List<?> imageList = (List<?>) XposedHelpers.callMethod(photoModeInfo, "getImageList");
                if (imageList != null && !imageList.isEmpty()) {
                    for (Object item : imageList) {
                        if (item == null) continue;
                        String[] fields = {"displayImageNoWatermark", "targetMultiRateImageUrl", "thumbnail"};
                        for (String f : fields) {
                            try {
                                Object urlModel = XposedHelpers.getObjectField(item, f);
                                if (urlModel != null) {
                                    List<String> list = MediaDownloader.getUrlsFromUrlModel(urlModel);
                                    if (!list.isEmpty()) {
                                        media.urls.add(list.get(0));
                                        break;
                                    }
                                }
                            } catch (Throwable ignored) {}
                        }
                    }
                    if (!media.urls.isEmpty()) {
                        media.isVideo = false;
                        return media;
                    }
                }
            }
        } catch (Throwable ignored) {}

        return null;
    }

    private static Activity getActivityFromContext(Context context) {
        if (context instanceof Activity) return (Activity) context;
        if (context instanceof android.content.ContextWrapper) {
            Context base = ((android.content.ContextWrapper) context).getBaseContext();
            if (base instanceof Activity) return (Activity) base;
        }
        return null;
    }

    private static int dpToPx(Context context, float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }
}
