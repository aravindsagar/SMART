package com.cs565project.smart.recommender

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import com.cs565project.smart.R
import com.cs565project.smart.util.HttpUtil
import org.json.JSONException
import java.io.IOException
import java.util.*

/**
 * Fetches news items to be displayed on block overlay.
 */
class NewsItem(val title: String, val publisher: String, val icon: Drawable, val uri: Uri) {
    companion object {
        private val MAX_NEWS_ITEMS = 3

        private val API_KEY = "33e71b361aed407aa0e671d7e61f3020"
        private val NEWS_URL = "https://newsapi.org/v2/top-headlines?country=us&" +
                "apiKey=" + API_KEY

        /**
         * Returns a list of recommended news items. Call from a background thread.
         */
        fun getRecommendedNews(context: Context): List<NewsItem> {
            // TODO customize news according to user preferences.

            val results = ArrayList<NewsItem>()
            try {
                val newsData = HttpUtil.getJson(NEWS_URL)
                val articleArray = newsData.getJSONArray("articles")
                for (i in 0 until Math.min(MAX_NEWS_ITEMS, articleArray.length())) {
                    val article = articleArray.getJSONObject(i)
                    var stream: HttpUtil.ConnectionInputStream? = null
                    var image: Drawable?
                    try {
                        stream = HttpUtil.fetchUrl(article.getString("urlToImage"))
                        val bmp = BitmapFactory.decodeStream(stream!!.stream)
                        val scaledBmp = Bitmap.createScaledBitmap(bmp, 120, 120, false)
                        bmp.recycle()
                        image = BitmapDrawable(context.resources, scaledBmp)
                    } catch (e: IOException) {
                        e.printStackTrace()
                        image = context.getDrawable(R.drawable.ic_public_black_24dp)
                    } finally {
                        if (stream != null) {
                            if (stream.stream != null) {
                                stream.stream.close()
                            }
                            if (stream.connection != null) {
                                stream.connection.disconnect()
                            }
                        }
                    }
                    results.add(NewsItem(
                            article.getString("title"),
                            article.getJSONObject("source").getString("name"),
                            image!!,
                            Uri.parse(article.getString("url"))
                    ))
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            return results
        }
    }

}
