package com.ash.tiktokregion.hooks.network;

/**
 * Pure-Java, zero-JNI URL cleaner and query injector.
 * Replaces native Rust string parsing with near-zero allocation string building.
 */
public final class UrlSanitizer {

    private UrlSanitizer() {}

    /**
     * Converts watermarked CDN stream URLs (/playwm/) to clean playback streams (/play/)
     * and forces watermark=0 without corrupting existing query signatures.
     */
    public static String cleanVideoUrl(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }

        if (!url.contains("playwm") && !url.contains("watermark=")) {
            return url;
        }

        String clean = url.replace("playwm", "play");

        int qIndex = clean.indexOf('?');
        if (qIndex == -1) {
            return clean + "?watermark=0";
        }

        String base = clean.substring(0, qIndex);
        String query = clean.substring(qIndex + 1);

        String[] pairs = query.split("&");
        StringBuilder sb = new StringBuilder(clean.length() + 16);
        sb.append(base).append('?');

        boolean hasWatermark = false;
        boolean first = true;

        for (String pair : pairs) {
            if (pair.isEmpty()) continue;
            if (!first) {
                sb.append('&');
            }
            first = false;

            if (pair.startsWith("watermark=")) {
                sb.append("watermark=0");
                hasWatermark = true;
            } else {
                sb.append(pair);
            }
        }

        if (!hasWatermark) {
            if (!first) sb.append('&');
            sb.append("watermark=0");
        }

        return sb.toString();
    }

    /**
     * Rewrites TTNet / Cronet request query strings to match target region parameters.
     */
    public static String rewriteCronetUrl(String url, String targetRegion, String carrierRegion, String mccMnc) {
        if (url == null || url.isEmpty() || targetRegion == null || targetRegion.isEmpty()) {
            return url;
        }

        int qIndex = url.indexOf('?');
        String base = qIndex != -1 ? url.substring(0, qIndex) : url;
        String query = qIndex != -1 ? url.substring(qIndex + 1) : "";

        StringBuilder sb = new StringBuilder(url.length() + 128);
        sb.append(base).append('?');

        boolean seenRegion = false;
        boolean seenCarrier = false;
        boolean seenSys = false;
        boolean seenOp = false;
        boolean first = true;

        if (!query.isEmpty()) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.isEmpty()) continue;
                if (!first) sb.append('&');
                first = false;

                int eqIdx = param.indexOf('=');
                String key = eqIdx != -1 ? param.substring(0, eqIdx) : param;

                switch (key) {
                    case "region":
                        sb.append("region=").append(targetRegion);
                        seenRegion = true;
                        break;
                    case "carrier_region":
                        sb.append("carrier_region=").append(carrierRegion);
                        seenCarrier = true;
                        break;
                    case "sys_region":
                        sb.append("sys_region=").append(targetRegion);
                        seenSys = true;
                        break;
                    case "sim_operator":
                    case "carrier_mcc_mnc":
                        sb.append(key).append('=').append(mccMnc);
                        seenOp = true;
                        break;
                    default:
                        sb.append(param);
                        break;
                }
            }
        }

        if (!seenRegion) {
            if (!first) sb.append('&');
            first = false;
            sb.append("region=").append(targetRegion);
        }

        if (!seenCarrier && carrierRegion != null && !carrierRegion.isEmpty()) {
            if (!first) sb.append('&');
            first = false;
            sb.append("carrier_region=").append(carrierRegion);
        }

        if (!seenSys) {
            if (!first) sb.append('&');
            first = false;
            sb.append("sys_region=").append(targetRegion);
        }

        if (!seenOp && mccMnc != null && !mccMnc.isEmpty()) {
            if (!first) sb.append('&');
            sb.append("sim_operator=").append(mccMnc);
        }

        return sb.toString();
    }
}
