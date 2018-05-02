package com.cs565project.smart.util

import android.content.Context
import com.cs565project.smart.R
import java.util.*

/**
 * Some utility graph methods.
 */
object GraphUtil {

    fun buildSubtitle(context: Context, items: List<String>): String {

        val sb = StringBuilder()
        when {
            items.isEmpty() -> sb.append("")
            items.size == 1 -> sb.append(items[0])
            items.size == 2 -> sb.append(items[0]).append(" ").append("and").append(" ").append(items[1])
            else -> sb.append(items[0]).append(", ").append(items[1])
                    .append(String.format(Locale.getDefault(), context.getString(R.string.n_more_apps), items.size - 2))
        }
        return sb.toString()
    }

    // TODO move more data processing in here.
}
