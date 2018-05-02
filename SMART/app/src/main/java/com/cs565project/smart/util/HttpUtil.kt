package com.cs565project.smart.util

import org.json.JSONException
import org.json.JSONObject

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

import javax.net.ssl.HttpsURLConnection

object HttpUtil {
    @Throws(IOException::class)
    fun fetchUrl(urlString: String): ConnectionInputStream {
        val stream: InputStream
        val connection: HttpURLConnection

        // HTTP
        val url = URL(urlString)

        connection = url.openConnection() as HttpURLConnection
        // Timeout for reading InputStream arbitrarily set to 3000ms.
        connection.readTimeout = 3000
        // Timeout for connection.connect() arbitrarily set to 3000ms.
        connection.connectTimeout = 3000
        // For this use case, set HTTP method to GET.
        connection.requestMethod = "GET"
        // Already true by default but setting just in case; needs to be true since this request
        // is carrying an input (response) body.
        connection.doInput = true
        // Open communications link (network traffic occurs here).
        connection.connect()

        val responseCode = connection.responseCode
        if (responseCode != HttpsURLConnection.HTTP_OK) {
            throw IOException("HTTP error code: $responseCode")
        }
        // Retrieve the response body as an InputStream.
        stream = connection.inputStream

        return ConnectionInputStream(connection, stream)
    }

    @Throws(JSONException::class, IOException::class)
    fun getJson(urlString: String): JSONObject {

        var stream: ConnectionInputStream? = null

        val result = try {
            stream = fetchUrl(urlString)
            val reader = BufferedReader(InputStreamReader(stream.stream, "utf-8"), 8)
            val sb = StringBuilder()
            var line: String? = reader.readLine()
            while (line != null) {
                sb.append(line).append("\n")
                line = reader.readLine()
            }
            sb.toString()
        } finally {
            if (stream != null) {
                stream.stream.close()
                stream.connection.disconnect()
            }
        }

        return JSONObject(result)

    }

    class ConnectionInputStream(val connection: HttpURLConnection, val stream: InputStream)
}
