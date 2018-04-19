package com.cs565project.smart.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Constants for SharedPrefernce and convenience methods for getting and setting them
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class PreferencesHelper {

    private static Set<PreferencesChangedListener> mListeners =
            Collections.newSetFromMap(new WeakHashMap<PreferencesChangedListener, Boolean>());

    private PreferencesHelper() {

    }

    public static void setPreference(Context context, final String KEY, Object value){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (value instanceof Integer) {
            preferences.edit().putInt(KEY, (int) value).apply();
        } else if (value instanceof Boolean) {
            preferences.edit().putBoolean(KEY, (boolean) value).apply();
        } else if (value instanceof Long) {
            preferences.edit().putLong(KEY, (long) value).apply();
        } else if (value instanceof String) {
            preferences.edit().putString(KEY, (String) value).apply();
        } else {
            throw new IllegalArgumentException("Can't handle preference value of type " + value.getClass());
        }
        callListeners(context);
    }

    public static boolean getBoolPreference(Context context, final String KEY, boolean defaultValue){
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(KEY, defaultValue);
    }

    public static int getIntPreference(Context context, final String KEY, int defaultValue){
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(KEY, defaultValue);
    }

    public static long getLongPreference(Context context, final String KEY, long defaultValue){
        return PreferenceManager.getDefaultSharedPreferences(context).getLong(KEY, defaultValue);
    }

    public static String getStringPreference(Context context, final String KEY, String defaultValue){
        return PreferenceManager.getDefaultSharedPreferences(context).getString(KEY, defaultValue);
    }

    public static Object getPreference(Context context, final String KEY, Object defaultValue) {
        if (defaultValue instanceof Integer) {
            return getIntPreference(context, KEY, (int) defaultValue);
        } else if (defaultValue instanceof Boolean) {
            return getBoolPreference(context, KEY, (boolean) defaultValue);
        } else if (defaultValue instanceof Long) {
            return getLongPreference(context, KEY, (long) defaultValue);
        } else if (defaultValue instanceof String) {
            return getStringPreference(context, KEY, (String) defaultValue);
        } else {
            throw new IllegalArgumentException("Can't handle preference value of type " + defaultValue.getClass());
        }
    }

    public static void registerListener(PreferencesChangedListener listener) {
        mListeners.add(listener);
    }

    private static void callListeners(Context c){
        for (PreferencesChangedListener listener : mListeners) {
            listener.onPreferencesChanged();
        }
    }

    public interface PreferencesChangedListener {
        void onPreferencesChanged();
    }

    public static abstract class Preference<ValueType, ViewType extends View> {
        private final String key;
        private final ValueType defaultValue;
        private final int viewId;
        private int secondaryViewId;

        public Preference(String key, ValueType defaultValue, int viewId, int secondaryViewId) {
            this.key = key;
            this.defaultValue = defaultValue;
            this.viewId = viewId;
            this.secondaryViewId = secondaryViewId;
        }

        public Preference(String key, ValueType defaultValue, int viewId) {
            this(key, defaultValue, viewId, -1);
        }

        public int getViewId() {
            return viewId;
        }

        public abstract void setViewValueFromSavedPreference(View rootView, Context c);

        public void setSecondaryViewValue(String value, View rootView) {
            if (secondaryViewId == -1 || value == null) {
                return;
            }
            ((TextView) rootView.findViewById(secondaryViewId)).setText(value);
        }

        public abstract ValueType saveViewValue(View rootView, Context c);

        @SuppressWarnings("unchecked")
        public ValueType getPreferenceValue(Context c) {
            return (ValueType) PreferencesHelper.getPreference(c, key, defaultValue);
        }

        public void setPreferenceValue(Context c, ValueType value) {
            PreferencesHelper.setPreference(c, key, value);
        }

        public ViewType findView(View rootView) {
            return rootView.findViewById(viewId);
        }

        public String getKey() {
            return key;
        }

        public ValueType getDefaultValue() {
            return defaultValue;
        }
    }

    public static class SwitchPreference extends Preference<Boolean, Switch> {

        public SwitchPreference(String key, Boolean defaultValue, int viewId) {
            super(key, defaultValue, viewId);
        }

        @Override
        public void setViewValueFromSavedPreference(View rootView, Context c) {
            Switch s = findView(rootView);
            s.setChecked(getPreferenceValue(c));
        }

        @Override
        public Boolean saveViewValue(View rootView, Context c) {
            Switch s = findView(rootView);
            boolean curVal = s.isChecked();
            setPreferenceValue(c, curVal);
            return curVal;
        }
    }

    public static abstract class SeekbarPreference extends Preference<Integer, SeekBar> {

        public SeekbarPreference(String key, Integer defaultValue, int viewId, int secondaryViewId) {
            super(key, defaultValue, viewId, secondaryViewId);
        }

        @Override
        public void setViewValueFromSavedPreference(View rootView, Context c) {
            findView(rootView).setProgress(getPreferenceValue(c));
        }

        @Override
        public Integer saveViewValue(View rootView, Context c) {
            int val = findView(rootView).getProgress();
            setPreferenceValue(c, val);
            return val;
        }

        public void setSecondaryViewValue(View rootView, Context c) {
            super.setSecondaryViewValue(getSecondaryViewString(findView(rootView).getProgress(), c), rootView);
        }

        protected abstract String getSecondaryViewString(int val, Context c);
    }
}
