package com.ash.tiktokregion;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import de.robv.android.xposed.XposedHelpers;

public class MediaDownloader {

    private static final String TAG = "TikTokDownloader";

    private static final Pattern PATTERN_WATERMARK = Pattern.compile("([&?])watermark=[^&]*");
    private static final Pattern PATTERN_IS_WATERMARK = Pattern.compile("([&?])is_watermark=[^&]*");
    private static final Pattern PATTERN_LOGO_NAME = Pattern.compile("([&?])logo_name=[^&]*");

    public static class StoryMedia {
        public boolean isVideo;
        public List<String> urls = new ArrayList<>();
        public String aid;
        public String desc;
    }

    public static void downloadAweme(Context ctx, Object aweme) {
        if (ctx == null) return;

        Object targetAweme = (aweme != null) ? aweme : WatermarkHook.sCurrentAweme;
        boolean isStory = WatermarkHook.isStoryAweme(targetAweme);

        StoryMedia media = extractStoryMedia(targetAweme);

        if (media == null && isStory) {
            synchronized (WatermarkHook.sCurrentStoryList) {
                for (Object item : WatermarkHook.sCurrentStoryList) {
                    if (item != null) {
                        media = extractStoryMedia(item);
                        if (media != null) {
                            Log.i(TAG, "Found story media from sCurrentStoryList");
                            break;
                        }
                    }
                }
            }
        }

        if (media == null && WatermarkHook.sLastPlayedPlayAddr != null) {
            List<String> liveUrls = getUrlsFromUrlModel(WatermarkHook.sLastPlayedPlayAddr);
            if (!liveUrls.isEmpty()) {
                media = new StoryMedia();
                media.isVideo = true;
                media.urls.addAll(liveUrls);
                media.aid = String.valueOf(System.currentTimeMillis());
                media.desc = isStory ? "Story Video" : "TikTok Video";
                Log.i(TAG, "Found media directly from live sLastPlayedPlayAddr: " + liveUrls.get(0));
            }
        }

        if (media == null || media.urls.isEmpty()) {
            Log.e(TAG, "Could not find media stream URL. aweme=" + (aweme != null ? aweme.getClass().getName() : "null")
                    + ", sCurrentAweme=" + (WatermarkHook.sCurrentAweme != null ? WatermarkHook.sCurrentAweme.getClass().getName() : "null")
                    + ", isStory=" + isStory);
            Toast.makeText(ctx, "Could not find video stream URL", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isChina = MainHook.isChinaPackage();
        String prefix = isChina ? (isStory ? "douyin_story_" : "douyin_") : (isStory ? "story_" : "tiktok_");
        if (media.isVideo) {
            String bestUrl = cleanDownloadUrl(media.urls.get(0));
            enqueueDownload(ctx, bestUrl, prefix + "video_" + media.aid + ".mp4", media.desc, "video/mp4");
        } else {
            int count = 0;
            for (int i = 0; i < media.urls.size(); i++) {
                String imgUrl = media.urls.get(i);
                enqueueDownload(ctx, imgUrl, prefix + "photo_" + media.aid + "_" + (i + 1) + ".jpg", media.desc, "image/jpeg");
                count++;
            }
            String msg;
            if (isChina) {
                msg = isStory ? ("正在保存 " + count + " 张日常照片...") : ("正在下载 " + count + " 张图片...");
            } else {
                msg = isStory ? ("Downloading " + count + " story photo(s)...") : ("Downloading " + count + " photo(s)...");
            }
            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
        }
    }

    public static StoryMedia extractStoryMedia(Object aweme) {
        if (aweme == null) return null;
        StoryMedia media = new StoryMedia();

        try {
            media.aid = (String) XposedHelpers.callMethod(aweme, "getAid");
        } catch (Throwable ignored) {}
        if (media.aid == null || media.aid.isEmpty()) {
            media.aid = String.valueOf(System.currentTimeMillis());
        }
        try {
            media.desc = (String) XposedHelpers.callMethod(aweme, "getDesc");
        } catch (Throwable ignored) {}

        try {
            Object video = null;
            try {
                video = XposedHelpers.getObjectField(aweme, "video");
            } catch (Throwable ignored) {}
            if (video == null) {
                try {
                    video = XposedHelpers.callMethod(aweme, "getVideo");
                } catch (Throwable ignored) {}
            }
            if (video != null) {
                WatermarkHook.cleanVideo(video);
                Object cleanPlay = WatermarkHook.getCleanPlayAddr(video);
                if (cleanPlay != null) {
                    List<String> extracted = getUrlsFromUrlModel(cleanPlay);
                    if (!extracted.isEmpty()) {
                        media.isVideo = true;
                        media.urls.addAll(extracted);
                        Log.i(TAG, "Extracted clean video URL from Video: " + extracted.get(0));
                        return media;
                    }
                }

                if (WatermarkHook.sLastPlayedPlayAddr != null) {
                    List<String> extracted = getUrlsFromUrlModel(WatermarkHook.sLastPlayedPlayAddr);
                    if (!extracted.isEmpty()) {
                        media.isVideo = true;
                        media.urls.addAll(extracted);
                        Log.i(TAG, "Extracted clean video URL from sLastPlayedPlayAddr: " + extracted.get(0));
                        return media;
                    }
                }

                String[] addrMethods = {
                        "getPlayAddr", "getProperPlayAddr", "LJIILJJIL", "LJIIIIZZ", "LJIILIIL",
                        "LJIILL", "LJIILLIIL", "getPlayAddrH264", "getH264PlayAddr", "getPlayAddrBytevc1",
                        "getDownloadNoWatermarkAddr"
                };

                for (String m : addrMethods) {
                    try {
                        Object urlModel = XposedHelpers.callMethod(video, m);
                        if (urlModel != null) {
                            WatermarkHook.cleanUrlModel(urlModel);
                            List<String> extracted = getUrlsFromUrlModel(urlModel);
                            if (!extracted.isEmpty()) {
                                media.isVideo = true;
                                media.urls.addAll(extracted);
                                Log.i(TAG, "Extracted story video URL from Video." + m + "(): " + extracted.get(0));
                                return media;
                            }
                        }
                    } catch (Throwable ignored) {}
                }

                for (String fieldName : new String[]{"a", "d", "b", "playAddr"}) {
                    try {
                        Object urlModel = XposedHelpers.getObjectField(video, fieldName);
                        if (urlModel != null) {
                            WatermarkHook.cleanUrlModel(urlModel);
                            List<String> extracted = getUrlsFromUrlModel(urlModel);
                            if (!extracted.isEmpty()) {
                                media.isVideo = true;
                                media.urls.addAll(extracted);
                                Log.i(TAG, "Extracted story video URL from Video." + fieldName + ": " + extracted.get(0));
                                return media;
                            }
                        }
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable t) {
            Log.d(TAG, "Video extraction failed: " + t.getMessage());
        }

        try {
            Object photoModeInfo = XposedHelpers.callMethod(aweme, "getPhotoModeImageInfo");
            if (photoModeInfo != null) {
                List<?> imageList = (List<?>) XposedHelpers.callMethod(photoModeInfo, "getImageList");
                if (imageList != null && !imageList.isEmpty()) {
                    for (Object item : imageList) {
                        if (item == null) continue;
                        String[] modelFields = {"displayImageNoWatermark", "targetMultiRateImageUrl", "userWatermarkImage", "ownerWatermarkImage", "thumbnail"};
                        for (String f : modelFields) {
                            try {
                                Object urlModel = XposedHelpers.getObjectField(item, f);
                                if (urlModel != null) {
                                    List<String> extracted = getUrlsFromUrlModel(urlModel);
                                    if (!extracted.isEmpty()) {
                                        media.urls.add(extracted.get(0));
                                        break;
                                    }
                                }
                            } catch (Throwable ignored) {}
                        }
                    }
                    if (!media.urls.isEmpty()) {
                        media.isVideo = false;
                        Log.i(TAG, "Extracted " + media.urls.size() + " story photos from PhotoModeImageInfo");
                        return media;
                    }
                }
            }
        } catch (Throwable t) {
            Log.d(TAG, "PhotoModeImageInfo extraction failed: " + t.getMessage());
        }

        try {
            List<?> imageInfos = (List<?>) XposedHelpers.callMethod(aweme, "getImageInfos");
            if (imageInfos != null && !imageInfos.isEmpty()) {
                for (Object img : imageInfos) {
                    if (img == null) continue;
                    String[] imgMethods = {"getLabelLarge", "getLabelThumb", "getUrlModel"};
                    for (String m : imgMethods) {
                        try {
                            Object urlModel = XposedHelpers.callMethod(img, m);
                            if (urlModel != null) {
                                List<String> extracted = getUrlsFromUrlModel(urlModel);
                                if (!extracted.isEmpty()) {
                                    media.urls.add(extracted.get(0));
                                    break;
                                }
                            }
                        } catch (Throwable ignored) {}
                    }
                }
                if (!media.urls.isEmpty()) {
                    media.isVideo = false;
                    Log.i(TAG, "Extracted " + media.urls.size() + " story photos from ImageInfos");
                    return media;
                }
            }
        } catch (Throwable t) {
            Log.d(TAG, "ImageInfos extraction failed: " + t.getMessage());
        }

        return null;
    }

    public static List<String> getUrlsFromUrlModel(Object urlModel) {
        List<String> res = new ArrayList<>();
        if (urlModel == null) return res;
        try {
            List<?> list = (List<?>) XposedHelpers.callMethod(urlModel, "getUrlList");
            if (list == null || list.isEmpty()) {
                try {
                    list = (List<?>) XposedHelpers.getObjectField(urlModel, "urlList");
                } catch (Throwable ignored) {}
            }
            if (list != null) {
                for (Object item : list) {
                    if (item instanceof String) {
                        String s = (String) item;
                        if (!s.isEmpty() && (s.startsWith("http://") || s.startsWith("https://"))) {
                            res.add(cleanDownloadUrl(s));
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        if (res.isEmpty()) {
            try {
                String uri = (String) XposedHelpers.callMethod(urlModel, "getUri");
                if (uri == null) {
                    try {
                        uri = (String) XposedHelpers.getObjectField(urlModel, "uri");
                    } catch (Throwable ignored) {}
                }
                if (uri != null && (uri.startsWith("http://") || uri.startsWith("https://"))) {
                    res.add(cleanDownloadUrl(uri));
                }
            } catch (Throwable ignored) {}
        }
        return res;
    }

    public static void enqueueDownload(Context ctx, String url, String fileName, String desc, String mimeType) {
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setTitle(fileName);
            request.setDescription(desc != null && !desc.isEmpty() ? desc : "Downloading from story");
            request.setMimeType(mimeType);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            request.addRequestHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14)");

            DownloadManager dm = (DownloadManager) ctx.getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm != null) {
                dm.enqueue(request);
                String msg;
                if (MainHook.isChinaPackage()) {
                    msg = fileName.contains("story") ? ("正在保存日常: " + fileName) : ("正在下载无水印视频: " + fileName);
                } else {
                    msg = fileName.contains("story") ? ("Saving story: " + fileName) : ("Downloading video: " + fileName);
                }
                Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
            }
        } catch (Throwable t) {
            Log.e(TAG, "enqueueDownload failed", t);
            Toast.makeText(ctx, "Download request failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public static String cleanDownloadUrl(String url) {
        if (url == null) return null;
        if (!url.contains("playwm") && !url.contains("watermark") && !url.contains("logo_name")) {
            return url;
        }
        String clean = url.replace("/playwm/", "/play/").replace("playwm", "play");
        if (clean.contains("watermark=")) {
            clean = PATTERN_WATERMARK.matcher(clean).replaceAll("$1watermark=0");
        }
        if (clean.contains("is_watermark=")) {
            clean = PATTERN_IS_WATERMARK.matcher(clean).replaceAll("$1is_watermark=0");
        }
        if (clean.contains("logo_name=")) {
            clean = PATTERN_LOGO_NAME.matcher(clean).replaceAll("");
        }
        return clean;
    }
}
