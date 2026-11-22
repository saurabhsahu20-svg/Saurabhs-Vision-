package com.saurabhsvision.sbrowser;

import java.net.URLEncoder;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Pure-Java address bar resolver; rejects dangerous/non-web schemes. */
public final class AddressResolver {
    private AddressResolver() {}
    public static String resolve(String typed, String engine) {
        String text = typed == null ? "" : typed.trim();
        if (text.isEmpty()) return null;
        // Reject invisible/control characters before interpreting navigation.
        for (int i=0;i<text.length();i++) {
            char c=text.charAt(i);
            if (Character.isISOControl(c) || c=='\\') return null;
        }
        String lc = text.toLowerCase(Locale.ROOT);
        if (lc.startsWith("https://") || lc.startsWith("http://")) {
            // Merely starting with "https://" is not evidence of a valid URL.
            // Do not pass malformed input or credential-embedded URLs to WebView.
            try {
                URI uri=new URI(text);
                String host=uri.getHost();
                if (host==null || host.isEmpty() || uri.getRawUserInfo()!=null)
                    return null;
                int port=uri.getPort();
                if (port>65535) return null;
                return text;
            } catch (URISyntaxException error) {return null;}
        }
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
