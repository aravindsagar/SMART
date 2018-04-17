package com.cs565project.smart.util;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class HttpUtil {
    public static ConnectionInputStream fetchUrl(String urlString) throws IOException {
        InputStream stream;
        HttpURLConnection connection;

        // HTTP
        URL url = new URL(urlString);

        connection = (HttpURLConnection) url.openConnection();
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

        return new ConnectionInputStream(connection, stream);
    }

    public static JSONObject getJson(String urlString) throws JSONException, IOException {

        String result = "";
        JSONObject jsonObject = null;
        ConnectionInputStream stream = null;

        try{
            stream = fetchUrl(urlString);
            if (stream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream.getStream(),"utf-8"),8);
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                result = sb.toString();
            }
        } finally {
            if (stream != null) {
                if (stream.getStream() != null) {
                    stream.getStream().close();
                }
                if (stream.getConnection() != null) {
                    stream.getConnection().disconnect();
                }

            }
        }

        // Convert string to object
        if (!result.isEmpty()) {
            jsonObject = new JSONObject(result);
        }

        return jsonObject;

    }

    public static class ConnectionInputStream {
        private HttpURLConnection connection;
        private InputStream stream;

        public ConnectionInputStream(HttpURLConnection connection, InputStream stream) {
            this.connection = connection;
            this.stream = stream;
        }

        public HttpURLConnection getConnection() {
            return connection;
        }

        public InputStream getStream() {
            return stream;
        }
    }
}
