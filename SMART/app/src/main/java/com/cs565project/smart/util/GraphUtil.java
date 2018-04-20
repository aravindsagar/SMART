package com.cs565project.smart.util;

import android.content.Context;

import com.cs565project.smart.R;

import java.util.List;
import java.util.Locale;

/**
 * Some utility graph methods.
 */
public class GraphUtil {

    public static String buildSubtitle(Context context, List<String> items) {

        StringBuilder sb = new StringBuilder();
        if (items.size() <= 0) {
            sb.append("");
        } else if (items.size() == 1) {
            sb.append(items.get(0));
        } else if (items.size() == 2) {
            sb.append(items.get(0)).append(" ").append("and").append(" ").append(items.get(1));
        } else {
            sb.append(items.get(0)).append(", ").append(items.get(1))
                    .append(String.format(Locale.getDefault(), context.getString(R.string.n_more_apps), items.size() - 2));
        }
        return sb.toString();
    }

    // TODO move more data processing in here.
}
