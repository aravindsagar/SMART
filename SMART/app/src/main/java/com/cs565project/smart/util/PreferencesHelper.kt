package com.cs565project.smart.util

import android.content.Context
import android.preference.PreferenceManager
import android.view.View
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import java.util.*

/**
 * Constants for SharedPrefernce and convenience methods for getting and setting them
 */
object PreferencesHelper {

    private val mListeners = Collections.newSetFromMap(WeakHashMap<PreferencesChangedListener, Boolean>())

    fun setPreference(context: Context, KEY: String, value: Any) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        if (value is Int) {
            preferences.edit().putInt(KEY, value).apply()
        } else if (value is Boolean) {
            preferences.edit().putBoolean(KEY, value).apply()
        } else if (value is Long) {
            preferences.edit().putLong(KEY, value).apply()
        } else if (value is String) {
            preferences.edit().putString(KEY, value).apply()
        } else {
            throw IllegalArgumentException("Can't handle preference value of type " + value.javaClass)
        }
        callListeners(context)
    }

    fun getBoolPreference(context: Context, KEY: String, defaultValue: Boolean): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(KEY, defaultValue)
    }

    fun getIntPreference(context: Context, KEY: String, defaultValue: Int): Int {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(KEY, defaultValue)
    }

    fun getLongPreference(context: Context, KEY: String, defaultValue: Long): Long {
        return PreferenceManager.getDefaultSharedPreferences(context).getLong(KEY, defaultValue)
    }

    fun getStringPreference(context: Context, KEY: String, defaultValue: String): String? {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(KEY, defaultValue)
    }

    fun getPreference(context: Context, KEY: String, defaultValue: Any): Any {
        return if (defaultValue is Int) {
            getIntPreference(context, KEY, defaultValue)
        } else if (defaultValue is Boolean) {
            getBoolPreference(context, KEY, defaultValue)
        } else if (defaultValue is Long) {
            getLongPreference(context, KEY, defaultValue)
        } else if (defaultValue is String) {
            getStringPreference(context, KEY, defaultValue) as Any
        } else {
            throw IllegalArgumentException("Can't handle preference value of type " + defaultValue!!.javaClass)
        }
    }

    fun registerListener(listener: PreferencesChangedListener) {
        mListeners.add(listener)
    }

    private fun callListeners(c: Context) {
        for (listener in mListeners) {
            listener.onPreferencesChanged()
        }
    }

    interface PreferencesChangedListener {
        fun onPreferencesChanged()
    }

    abstract class Preference<ValueType, ViewType : View> @JvmOverloads constructor(val key: String, val defaultValue: ValueType, val viewId: Int, private val secondaryViewId: Int = -1) {

        abstract fun setViewValueFromSavedPreference(rootView: View, c: Context)

        fun setSecondaryViewValue(value: String?, rootView: View) {
            if (secondaryViewId == -1 || value == null) {
                return
            }
            (rootView.findViewById<View>(secondaryViewId) as TextView).text = value
        }

        abstract fun saveViewValue(rootView: View, c: Context): ValueType

        fun getPreferenceValue(c: Context): ValueType {
            return PreferencesHelper.getPreference(c, key, defaultValue as Any) as ValueType
        }

        fun setPreferenceValue(c: Context, value: ValueType) {
            PreferencesHelper.setPreference(c, key, value as Any)
        }

        fun findView(rootView: View): ViewType {
            return rootView.findViewById(viewId)
        }
    }

    class SwitchPreference(key: String, defaultValue: Boolean, viewId: Int) : Preference<Boolean, Switch>(key, defaultValue, viewId) {

        override fun setViewValueFromSavedPreference(rootView: View, c: Context) {
            val s = findView(rootView)
            s.isChecked = getPreferenceValue(c)
        }

        override fun saveViewValue(rootView: View, c: Context): Boolean {
            val s = findView(rootView)
            val curVal = s.isChecked
            setPreferenceValue(c, curVal)
            return curVal
        }
    }

    abstract class SeekbarPreference(key: String, defaultValue: Int, viewId: Int, secondaryViewId: Int) : Preference<Int, SeekBar>(key, defaultValue, viewId, secondaryViewId) {

        override fun setViewValueFromSavedPreference(rootView: View, c: Context) {
            findView(rootView).progress = getPreferenceValue(c)
        }

        override fun saveViewValue(rootView: View, c: Context): Int {
            val `val` = findView(rootView).progress
            setPreferenceValue(c, `val`)
            return `val`
        }

        fun setSecondaryViewValue(rootView: View, c: Context) {
            super.setSecondaryViewValue(getSecondaryViewString(findView(rootView).progress, c), rootView)
        }

        protected abstract fun getSecondaryViewString(`val`: Int, c: Context): String
    }
}
