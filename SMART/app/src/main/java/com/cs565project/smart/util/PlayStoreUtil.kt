package com.cs565project.smart.util

import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

object PlayStoreUtil {
    private const val NO_CATEGORY = "Unknown"
    private const val BASE_URL = "https://play.google.com/store/apps/details?id="

    /**
     * Retrieve category of given app from play store.
     */
    @Throws(IOException::class)
    fun getPlayStoreCategory(packageName: String): String {
        var stream: HttpUtil.ConnectionInputStream? = null
        return try {
            stream = HttpUtil.fetchUrl(BASE_URL + packageName)
            extractCategory(stream.stream)
        } finally {
            // Close Stream and disconnect HTTPS connection.
            if (stream != null) {
                stream.stream.close()
                stream.connection.disconnect()
            }
        }
    }

    @Throws(IOException::class)
    private fun extractCategory(stream: InputStream): String {
        val reader = InputStreamReader(stream, "UTF-8")
        val rawBuffer = CharArray(1000)
        var markerIdx = -1
        var targetString = ""
        var breakNext = false
        while (reader.read(rawBuffer) != -1) {
            if (breakNext) {
                targetString += String(rawBuffer)
                break
            }
            targetString = String(rawBuffer)
            markerIdx = targetString.indexOf("itemprop=\"genre\"")
            breakNext = markerIdx != -1
        }

        if (markerIdx == -1) {
            return NO_CATEGORY
        }

        val startIdx = targetString.indexOf('>', markerIdx) + 1
        val endIdx = targetString.indexOf('<', markerIdx)

        return if (startIdx == -1 || endIdx == -1) NO_CATEGORY
            else targetString.substring(startIdx, endIdx)
    }
}
