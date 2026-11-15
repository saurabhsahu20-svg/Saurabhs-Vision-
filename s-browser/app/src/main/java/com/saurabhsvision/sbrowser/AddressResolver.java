package com.saurabhsvision.sbrowser;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Pure-Java address bar resolver; rejects dangerous/non-web schemes. */
public final class AddressResolver {
    private AddressResolver() {}
    public static String resolve(String typed, String engine) {
        String text = typed == null ? "" : typed.trim();
        if (text.isEmpty()) return null;
        if (text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0) return null;
        String lc = text.toLowerCase(Locale.ROOT);
        if (lc.startsWith("https://") || lc.startsWith("http://")) return text;
        if (lc.startsWith("javascript:") || lc.startsWith("file:") ||
            lc.startsWith("data:") || lc.startsWith("content:") ||
            lc.startsWith("intent:") || lc.contains("://")) return null;
        if (text.matches("(?i)^[a-z0-9][a-z0-9.-]*\\.[a-z]{2,}(:[0-9]{2,5})?(/[^ ]*)?$"))
            return "https://" + text;
        String query = URLEncoder.encode(text, StandardCharsets.UTF_8);
        if ("duckduckgo".equalsIgnoreCase(engine)) return "https://duckduckgo.com/?q=" + query;
        if ("bing".equalsIgnoreCase(engine)) return "https://www.bing.com/search?q=" + query;
        return "https://www.google.com/search?q=" + query;
    }
}
