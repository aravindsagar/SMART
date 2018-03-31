package com.cs565project.smart.util;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import static com.cs565project.smart.util.AppInfo.NO_CATEGORY;

public class PlayStoreUtil {

    private static final String BASE_URL = "https://play.google.com/store/apps/details?id=";

    /**
     * Retrieve category of given app from play store.
     */
    public static String getPlayStoreCategory(String packageName) throws IOException {
        URL url = new URL(BASE_URL + packageName);
        InputStream stream = null;
        HttpsURLConnection connection = null;
        String result = null;
        try {
            connection = (HttpsURLConnection) url.openConnection();
            // Timeout for reading InputStream arbitrarily set to 3000ms.
            connection.setReadTimeout(3000);
            // Timeout for connection.connect() arbitrarily set to 3000ms.
            connection.setConnectTimeout(3000);
            // For this use case, set HTTP method to GET.
            connection.setRequestMethod("GET");
            // Already true by default but setting just in case; needs to be true since this request
            // is carrying an input (response) body.
            connection.setDoInput(true);
            // Open communications link (network traffic occurs here).
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpsURLConnection.HTTP_OK) {
                throw new IOException("HTTP error code: " + responseCode);
            }
            // Retrieve the response body as an InputStream.
            stream = connection.getInputStream();
            if (stream != null) {
                // Converts Stream to String with max length of 500.
                result = extractCategory(stream);
            }
        } finally {
            // Close Stream and disconnect HTTPS connection.
            if (stream != null) {
                stream.close();
            }
            if (connection != null) {
                connection.disconnect();
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

        Log.d("playStore", targetString);
        Log.d("markerIdx", markerIdx + "");
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
