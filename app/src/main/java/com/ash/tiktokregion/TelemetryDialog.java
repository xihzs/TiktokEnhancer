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
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import de.robv.android.xposed.XposedHelpers;

public class TelemetryDialog {

    private static final String TAG = "TikTokTelemetryDialog";

    public static void show(Context context, Object aweme) {
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
            String countryIso = QualityAndTelemetryHook.extractCountryIso(aweme);
            String flagEmoji = QualityAndTelemetryHook.countryCodeToEmoji(countryIso);
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
            String secUid = getSafeString(author, "getSecUid", "secUid");
            String accountRegion = getSafeString(author, "getAccountRegion", "accountRegion");
            if (TextUtils.isEmpty(accountRegion)) {
                accountRegion = getSafeString(author, "getIsoCountryCode", "isoCountryCode");
            }
            String authorRegion = getSafeString(author, "getRegion", "region");
            String country = getSafeString(author, "getCountry", "country");
            String deviceRegion = getSafeString(author, "getDeviceRegion", "deviceRegion");
            String cityName = getSafeString(author, "getCityName", "cityName");
            String bioLocation = getSafeString(author, "getBioLocation", "bioLocation");

            boolean isPrivate = false;
            try {
                Boolean p1 = (Boolean) getSafeObject(author, "isPrivateAccount", "isPrivateAccount");
                if (Boolean.TRUE.equals(p1)) isPrivate = true;
                if (!isPrivate) {
                    Boolean p2 = (Boolean) getSafeObject(author, "isSecret", "secret");
                    if (Boolean.TRUE.equals(p2)) isPrivate = true;
                }
            } catch (Throwable ignored) {}

            long regTime = getSafeLong(author, "getCreateTime", "createTime");
            if (regTime == 0) regTime = getSafeLong(author, "getRegisterTime", "registerTime");
            long followers = getSafeLong(author, "getFollowerCount", "followerCount");
            long following = getSafeLong(author, "getFollowingCount", "followingCount");
            long awemeCount = getSafeLong(author, "getAwemeCount", "awemeCount");
            long privateAwemeCount = getSafeLong(author, "getPrivateAwemeCount", "privateAwemeCount");
            long totalHearts = getSafeLong(author, "getTotalFavorited", "totalFavorited");

            String insId = getSafeString(author, "getInsId", "insId");
            String twitterName = getSafeString(author, "getTwitterName", "twitterName");
            if (TextUtils.isEmpty(twitterName)) twitterName = getSafeString(author, "getTwitterId", "twitterId");
            String ytTitle = getSafeString(author, "getYoutubeChannelTitle", "youtubeChannelTitle");
            if (TextUtils.isEmpty(ytTitle)) ytTitle = getSafeString(author, "getYoutubeChannelId", "youtubeChannelId");

            String ageGroup = getSafeString(author, "getPredictedAgeGroup", "predictedAgeGroup");
            String bioEmail = getSafeString(author, "getBioEmail", "bioEmail");
            if (TextUtils.isEmpty(bioEmail)) bioEmail = getSafeString(author, "getEmail", "email");
            String bioPhone = getSafeString(author, "getBioPhone", "bioPhone");
            String bindPhone = getSafeString(author, "getBindPhone", "bindPhone");

            if (!TextUtils.isEmpty(nickname)) {
                addTelemetryRow(activity, cardUser, "Nickname", nickname, false, false, null, false);
            }
            if (!TextUtils.isEmpty(handle)) {
                addTelemetryRow(activity, cardUser, "Handle", "@" + handle, false, false, handle, false);
            }
            if (!TextUtils.isEmpty(uid)) {
                addTelemetryRow(activity, cardUser, "User ID", uid, true, true, uid, false);
            }
            if (!TextUtils.isEmpty(secUid)) {
                addTelemetryRow(activity, cardUser, "SecUID", secUid, true, true, secUid, false);
            }
            if (!TextUtils.isEmpty(accountRegion)) {
                String regEmoji = QualityAndTelemetryHook.countryCodeToEmoji(accountRegion);
                String regName = getCountryName(accountRegion);
                String regDisplay = (regEmoji != null ? regEmoji + " " : "")
                        + (!TextUtils.isEmpty(regName) ? regName + " (" + accountRegion.toUpperCase(Locale.ROOT) + ")" : accountRegion.toUpperCase(Locale.ROOT));
                addTelemetryRow(activity, cardUser, "Account Region (Locked)", regDisplay, false, false, null, false);
            }
            if (!TextUtils.isEmpty(authorRegion) && !authorRegion.equalsIgnoreCase(accountRegion)) {
                String profEmoji = QualityAndTelemetryHook.countryCodeToEmoji(authorRegion);
                String profName = getCountryName(authorRegion);
                String profDisplay = (profEmoji != null ? profEmoji + " " : "")
                        + (!TextUtils.isEmpty(profName) ? profName + " (" + authorRegion.toUpperCase(Locale.ROOT) + ")" : authorRegion.toUpperCase(Locale.ROOT));
                addTelemetryRow(activity, cardUser, "Profile Region", profDisplay, false, false, null, false);
            }
            if (!TextUtils.isEmpty(deviceRegion) && !deviceRegion.equalsIgnoreCase(accountRegion) && !deviceRegion.equalsIgnoreCase(authorRegion)) {
                String devEmoji = QualityAndTelemetryHook.countryCodeToEmoji(deviceRegion);
                String devDisplay = (devEmoji != null ? devEmoji + " " : "") + deviceRegion.toUpperCase(Locale.ROOT);
                addTelemetryRow(activity, cardUser, "Device Region", devDisplay, false, false, null, false);
            }
            if (!TextUtils.isEmpty(country) && !country.equalsIgnoreCase(accountRegion) && !country.equalsIgnoreCase(authorRegion)) {
                addTelemetryRow(activity, cardUser, "Country", country, false, false, null, false);
            }
            addTelemetryRow(activity, cardUser, "Account Privacy", isPrivate ? "Private (Restricted)" : "Public Account", false, false, null, false);
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
            if (totalHearts > 0) {
                addTelemetryRow(activity, cardUser, "Total Hearts", formatNumber(totalHearts), true, false, null, false);
            }
            if (followers > 0) {
                addTelemetryRow(activity, cardUser, "Followers", formatNumber(followers), true, false, null, false);
            }
            if (following > 0) {
                addTelemetryRow(activity, cardUser, "Following", formatNumber(following), true, false, null, false);
            }
            if (awemeCount > 0) {
                String totalVids = formatNumber(awemeCount) + (privateAwemeCount > 0 ? " (" + privateAwemeCount + " priv)" : "");
                addTelemetryRow(activity, cardUser, "Total Videos", totalVids, true, false, null, false);
            }
            if (!TextUtils.isEmpty(insId)) {
                addTelemetryRow(activity, cardUser, "Instagram", "@" + insId, false, true, insId, false);
            }
            if (!TextUtils.isEmpty(twitterName)) {
                addTelemetryRow(activity, cardUser, "Twitter / X", "@" + twitterName, false, true, twitterName, false);
            }
            if (!TextUtils.isEmpty(ytTitle)) {
                addTelemetryRow(activity, cardUser, "YouTube", ytTitle, false, true, ytTitle, false);
            }
            if (!TextUtils.isEmpty(ageGroup)) {
                addTelemetryRow(activity, cardUser, "Predicted Age", ageGroup, false, false, null, false);
            }
            if (!TextUtils.isEmpty(bioEmail)) {
                addTelemetryRow(activity, cardUser, "Contact Email", bioEmail, false, true, bioEmail, false);
            }
            if (!TextUtils.isEmpty(bioPhone)) {
                addTelemetryRow(activity, cardUser, "Contact Phone", bioPhone, false, true, bioPhone, false);
            }
            if (!TextUtils.isEmpty(bindPhone)) {
                addTelemetryRow(activity, cardUser, "Bound Phone", bindPhone, false, true, bindPhone, false);
            }
            if (regTime > 0) {
                addTelemetryRow(activity, cardUser, "Registered", formatTimestamp(regTime), true, false, null, true);
            }
            trimLastDivider(cardUser);

            LinearLayout cardVideo = createSectionCard(activity, scrollContent, "VIDEO & ENCODING");
            if (!TextUtils.isEmpty(aid)) {
                addTelemetryRow(activity, cardVideo, "Video ID (AID)", aid, true, true, aid, false);
            }
            String videoRegion = getSafeString(aweme, "getRegion", "region");
            if (!TextUtils.isEmpty(videoRegion)) {
                String vrEmoji = QualityAndTelemetryHook.countryCodeToEmoji(videoRegion);
                String vrName = getCountryName(videoRegion);
                String vrDisplay = (vrEmoji != null ? vrEmoji + " " : "")
                        + (!TextUtils.isEmpty(vrName) ? vrName + " (" + videoRegion.toUpperCase(Locale.ROOT) + ")" : videoRegion.toUpperCase(Locale.ROOT));
                addTelemetryRow(activity, cardVideo, "Video Geo-Region", vrDisplay, false, false, null, false);
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
            if (bitRates == null || bitRates.isEmpty()) {
                bitRates = getSafeList(video, "getRawBitRate", "bitRateList");
            }
            if (bitRates == null || bitRates.isEmpty()) {
                bitRates = getSafeList(video, "getDashVideoBitRate", null);
            }
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
                            String lowerGear = gear.toLowerCase(Locale.ROOT);
                            if (lowerGear.contains("2160") || lowerGear.contains("4k")) {
                                bw = 2160; bh = 3840;
                            } else if (lowerGear.contains("1440") || lowerGear.contains("2k")) {
                                bw = 1440; bh = 2560;
                            } else if (lowerGear.contains("1080")) {
                                bw = 1080; bh = 1920;
                            } else if (lowerGear.contains("720")) {
                                bw = 720; bh = 1280;
                            } else if (lowerGear.contains("540")) {
                                bw = 540; bh = 960;
                            }
                        }
                    }
                    if (bw > maxStreamWidth) {
                        maxStreamWidth = bw;
                        maxStreamHeight = bh;
                    }
                }
            }

            if (maxStreamWidth <= 1080) {
                String dModelStr = getSafeString(video, "getVideoModelStr", "dVideoModel");
                if (dModelStr != null) {
                    String dLower = dModelStr.toLowerCase(Locale.ROOT);
                    if (dLower.contains("2160p") || dLower.contains("3840x2160") || dLower.contains("2160x3840")) {
                        maxStreamWidth = 2160;
                        maxStreamHeight = 3840;
                    } else if (maxStreamWidth <= 720 && (dLower.contains("1080p") || dLower.contains("1080x1920") || dLower.contains("1920x1080"))) {
                        maxStreamWidth = 1080;
                        maxStreamHeight = 1920;
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
                if (Math.min(width, height) >= 2160 || Math.max(width, height) >= 3840) {
                    qualityLabel = " (4K UHD)";
                } else if (Math.min(width, height) >= 1080 || Math.max(width, height) >= 1920) {
                    qualityLabel = " (1080p HD)";
                } else if (Math.min(width, height) >= 720 || Math.max(width, height) >= 1280) {
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

    public static String getCountryName(String code) {
        if (TextUtils.isEmpty(code) || code.length() != 2) return "";
        try {
            Locale l = new Locale("", code.toUpperCase(Locale.ROOT));
            return l.getDisplayCountry(Locale.ENGLISH);
        } catch (Throwable ignored) {
            return "";
        }
    }

    public static String getLanguageDisplayName(String code) {
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

    public static Object getSafeObject(Object target, String getter, String field) {
        if (target == null) return null;
        try {
            return XposedHelpers.callMethod(target, getter);
        } catch (Throwable ignored) {}
        try {
            return XposedHelpers.getObjectField(target, field);
        } catch (Throwable ignored) {}
        return null;
    }

    public static String getSafeString(Object target, String getter, String field) {
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

    public static long getSafeLong(Object target, String getter, String field) {
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

    public static int getSafeInt(Object target, String getter, String field) {
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

    public static List<?> getSafeList(Object target, String getter, String field) {
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

    private static int dpToPx(Context context, float dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                context.getResources().getDisplayMetrics()
        );
    }
}
