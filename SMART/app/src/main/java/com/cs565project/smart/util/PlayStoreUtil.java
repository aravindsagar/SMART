package com.cs565project.smart.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import static com.cs565project.smart.util.AppInfo.NO_CATEGORY;

public class PlayStoreUtil {

    private static final String BASE_URL = "https://play.google.com/store/apps/details?id=";

    /**
     * Retrieve category of given app from play store.
     */
    public static String getPlayStoreCategory(String packageName) throws IOException {
        HttpUtil.ConnectionInputStream stream = null;
        String result = null;
        try {
            stream = HttpUtil.fetchUrl(BASE_URL + packageName);
            if (stream.getStream() != null) {
                // Converts Stream to String with max length of 500.
                result = extractCategory(stream.getStream());
            }
        } finally {
            // Close Stream and disconnect HTTPS connection.
            if (stream != null) {
                if (stream.getStream() != null) {
                    stream.getStream().close();
                }
                if (stream.getConnection() != null) {
                    stream.getConnection().disconnect();
                }
            }
        }
        return result;
    }

    private static String extractCategory(InputStream stream) throws IOException {
        Reader reader = new InputStreamReader(stream, "UTF-8");
        char[] rawBuffer = new char[1000];
        int markerIdx = -1;
        String targetString = "";
        boolean breakNext = false;
        while (reader.read(rawBuffer) != -1) {
            if (breakNext) {
                targetString += new String(rawBuffer);
                break;
            }
            targetString = new String(rawBuffer);
            markerIdx = targetString.indexOf("itemprop=\"genre\"");
            breakNext = (markerIdx != -1);
        }

        if (markerIdx == -1) {
            return NO_CATEGORY;
        }

        int startIdx = targetString.indexOf('>', markerIdx) + 1,
                endIdx = targetString.indexOf('<', markerIdx);

        if (startIdx == -1 || endIdx == -1) {
            return NO_CATEGORY;
        }

        return targetString.substring(startIdx, endIdx);
    }
}
