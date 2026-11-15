package com.saurabhsvision.sbrowser;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Only https navigation is accepted by the app's first-release URL resolver. */
public final class UrlTools {
    private UrlTools() { }

    public static String resolve(String raw, String engine) {
        if (raw == null) return null;
        String input = raw.trim();
        if (input.isEmpty()) return null;
        String lower = input.toLowerCase(Locale.ROOT);
        if (lower.matches("^[a-z][a-z0-9+.-]*:.*") &&
                !lower.startsWith("https://") && !lower.startsWith("http://")) return null;
        if (lower.startsWith("http://")) input = "https://" + input.substring(7);
        if (input.startsWith("https://")) return validated(input);
        if (input.matches("(?i)^[^\\s./]+(\\.[a-z0-9-]+)+(?:[:][0-9]{1,5})?(?:[/?#].*)?$")) {
            return validated("https://" + input);
        }
        String base = engine == null ? "Google" : engine;
        String root = base.equals("DuckDuckGo") ? "https://duckduckgo.com/?q=" :
            base.equals("Bing") ? "https://www.bing.com/search?q=" :
            "https://www.google.com/search?q=";
        return root + URLEncoder.encode(input, StandardCharsets.UTF_8);
    }

    public static String validated(String value) {
        try {
            URI uri = new URI(value);
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null ||
                host.isEmpty() || uri.getRawUserInfo() != null) return null;
            return uri.toASCIIString();
        } catch (Exception e) {
            return null;
        }
    }
}
