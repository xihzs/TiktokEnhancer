package com.ash.tiktokregion.hooks.media;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XposedHelpers;

/**
 * Handles media extraction, file saving, and download manager enqueuing for videos and photos.
 */
public final class MediaDownloader {

    private static final String TAG = "TikTokMediaDownloader";

    private MediaDownloader() {}

    public static class StoryMedia {
        public String aid;
        public String desc;
        public boolean isVideo = true;
        public final List<String> urls = new ArrayList<>();
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
                Toast.makeText(ctx, "Saving: " + fileName, Toast.LENGTH_SHORT).show();
            }
        } catch (Throwable t) {
            Log.e(TAG, "enqueueDownload failed", t);
            Toast.makeText(ctx, "Download failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public static void copyFile(File src, File dst) throws Exception {
        if (!src.exists()) return;
        File parent = dst.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (FileInputStream inStream = new FileInputStream(src);
             FileOutputStream outStream = new FileOutputStream(dst);
             FileChannel inChannel = inStream.getChannel();
             FileChannel outChannel = outStream.getChannel()) {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        }
    }

    public static void broadcastScanFile(Context context, File file) {
        if (context == null || file == null || !file.exists()) return;
        try {
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            mediaScanIntent.setData(Uri.fromFile(file));
            context.sendBroadcast(mediaScanIntent);
        } catch (Throwable ignored) {}
    }

    public static List<String> getUrlsFromUrlModel(Object urlModel) {
        List<String> res = new ArrayList<>();
        if (urlModel == null) return res;
        try {
            List<?> list = (List<?>) XposedHelpers.callMethod(urlModel, "getUrlList");
            if (list != null) {
                for (Object item : list) {
                    if (item instanceof String) {
                        String s = (String) item;
                        if (!s.isEmpty() && (s.startsWith("http://") || s.startsWith("https://"))) {
                            res.add(s);
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        if (res.isEmpty()) {
            try {
                String uri = (String) XposedHelpers.callMethod(urlModel, "getUri");
                if (uri != null && (uri.startsWith("http://") || uri.startsWith("https://"))) {
                    res.add(uri);
                }
            } catch (Throwable ignored) {}
        }
        return res;
    }
}
