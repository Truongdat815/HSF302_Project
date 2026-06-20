package com.fpt.elearning.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Chuyen URL video sang dang nhung (embed) de phat truc tiep trong trang hoc.
 */
public final class VideoUtil {

    private static final Pattern YOUTUBE = Pattern.compile(
            "(?:youtube\\.com/(?:watch\\?(?:.*&)?v=|embed/|shorts/)|youtu\\.be/)([A-Za-z0-9_-]{6,})");
    private static final Pattern VIMEO = Pattern.compile("vimeo\\.com/(?:video/)?(\\d+)");

    private VideoUtil() {
    }

    /**
     * Tra ve URL nhung (iframe) cho YouTube/Vimeo, hoac null neu khong nhan dien duoc.
     */
    public static String toEmbedUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String u = url.trim();
        Matcher yt = YOUTUBE.matcher(u);
        if (yt.find()) {
            return "https://www.youtube.com/embed/" + yt.group(1);
        }
        Matcher vm = VIMEO.matcher(u);
        if (vm.find()) {
            return "https://player.vimeo.com/video/" + vm.group(1);
        }
        return null;
    }

    /**
     * URL la file video truc tiep (mp4/webm/ogg hoac video tren Cloudinary) -> dung the <video>.
     */
    public static boolean isDirectVideo(String url) {
        if (url == null) {
            return false;
        }
        String u = url.trim().toLowerCase();
        return u.endsWith(".mp4") || u.endsWith(".webm") || u.endsWith(".ogg") || u.endsWith(".mov")
                || u.contains("/video/upload/"); // Cloudinary video
    }
}
