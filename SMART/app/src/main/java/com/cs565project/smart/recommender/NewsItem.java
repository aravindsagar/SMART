package com.cs565project.smart.recommender;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.cs565project.smart.util.HttpUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NewsItem {

    private static final String API_KEY = "33e71b361aed407aa0e671d7e61f3020";
    private static final String NEWS_URL = "https://newsapi.org/v2/top-headlines?country=us&" +
            "apiKey=" + API_KEY;

    private String title;
    private String publisher;
    private Bitmap icon;
    private Uri uri;

    public NewsItem(String title, String publisher, Bitmap icon, Uri uri) {
        this.title = title;
        this.publisher = publisher;
        this.icon = icon;
        this.uri = uri;
    }

    public String getTitle() {
        return title;
    }

    public String getPublisher() {
        return publisher;
    }

    public Bitmap getIcon() {
        return icon;
    }

    public Uri getUri() {
        return uri;
    }

    /**
     * Returns a list of recommended news items. Call from a background thread.
     */
    public static List<NewsItem> getRecommendedNews(Context context) {
        // TODO customize news according to user preferences.

        List<NewsItem> results = new ArrayList<>();
        try {
            JSONObject newsData = HttpUtil.getJson(NEWS_URL);
            JSONArray articleArray = newsData.getJSONArray("articles");
            for(int i = 0; i < Math.min(4, articleArray.length()); i++) {
                JSONObject article = articleArray.getJSONObject(i);
                HttpUtil.ConnectionInputStream stream = null;
                Bitmap image = null;
                try {
                    stream = HttpUtil.fetchUrl(article.getString("urlToImage"));
                    image = BitmapFactory.decodeStream(stream.getStream());
                } catch (IOException e) {
                    e.printStackTrace();
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
                results.add(new NewsItem(
                        article.getString("title"),
                        article.getJSONObject("source").getString("name"),
                        image,
                        Uri.parse(article.getString("url"))
                ));
            }
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
        return results;
    }

}
