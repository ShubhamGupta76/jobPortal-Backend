package com.job_Portal_Backend.job_portal_backend.session.service;

/**
 * Best-effort, dependency-free "Browser on OS" label for a User-Agent string, used only to help a
 * user recognize their own sessions in a list. Not a security control and not meant to be exact.
 */
final class UserAgentLabeler {

    private UserAgentLabeler() {
    }

    static String describe(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Unknown device";
        }
        String ua = userAgent.toLowerCase();
        return browser(ua) + " on " + os(ua);
    }

    private static String browser(String ua) {
        if (ua.contains("edg/")) return "Edge";
        if (ua.contains("opr/") || ua.contains("opera")) return "Opera";
        if (ua.contains("chrome/")) return "Chrome";
        if (ua.contains("firefox/")) return "Firefox";
        if (ua.contains("safari/") && !ua.contains("chrome/")) return "Safari";
        return "a browser";
    }

    private static String os(String ua) {
        if (ua.contains("windows")) return "Windows";
        if (ua.contains("mac os") || ua.contains("macintosh")) return "macOS";
        if (ua.contains("android")) return "Android";
        if (ua.contains("iphone") || ua.contains("ipad") || ua.contains("ios")) return "iOS";
        if (ua.contains("linux")) return "Linux";
        return "an unknown OS";
    }
}
