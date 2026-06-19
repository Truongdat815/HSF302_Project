package com.fpt.elearning.util;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

/**
 * Lam sach HTML rich text tu CKEditor truoc khi luu DB (chong XSS).
 * Cho phep cac the dinh dang co ban + style mau/co chu/can le + anh.
 */
public final class HtmlSanitizer {

    private HtmlSanitizer() {
    }

    private static final Safelist SAFELIST = Safelist.relaxed()
            // Cho phep style inline (mau, co chu) ma CKEditor sinh ra
            .addAttributes(":all", "style", "class")
            // The va thuoc tinh bo sung (s/u cho gach ngang/gach chan tu Quill)
            .addTags("span", "hr", "figure", "figcaption", "s", "u")
            .addAttributes("img", "src", "alt", "width", "height", "style")
            .addAttributes("a", "href", "title", "target", "rel")
            .addProtocols("img", "src", "http", "https", "data")
            .addProtocols("a", "href", "http", "https", "mailto");

    public static String clean(String unsafeHtml) {
        if (unsafeHtml == null) {
            return null;
        }
        return Jsoup.clean(unsafeHtml, SAFELIST);
    }
}
