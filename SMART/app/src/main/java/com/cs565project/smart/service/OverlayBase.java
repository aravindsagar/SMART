package com.cs565project.smart.service;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

/**
 * A base class for managing overlay windows.
 */
public abstract class OverlayBase {
    protected static final String LOG_TAG = OverlayBase.class.getSimpleName();

    private WindowManager windowManager;
    private Context context;
    private View myLayout;
    private WindowManager.LayoutParams myLayoutParams;

    private boolean isAdded = false;

    public OverlayBase(Context context, WindowManager windowManager, int layoutResource) {
        this.context = context;
        this.windowManager = windowManager;

        LayoutInflater inflater = LayoutInflater.from(context);
        myLayout = inflater.inflate(layoutResource, null, false);
    }

    /**
     * Populate data, setup callbacks etc on the window.
     *
     * @param rootView View inflated from the given layoutResource.
     */
    abstract void setupLayout(View rootView);

    abstract WindowManager.LayoutParams buildLayoutParams();
    /**
     * Adds the view specified by buildLayout() to the windowManager passed during initialization
     */
    public void execute() {
        if (isAdded) return;

        myLayout.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        setupLayout(myLayout);

        if(myLayoutParams == null) {
            myLayoutParams = buildLayoutParams();
        }

        try {
            windowManager.addView(myLayout, myLayoutParams);
            isAdded = true;
            myLayout.invalidate();
        } catch (IllegalStateException e) {
            isAdded = false;
        }
    }

    /**
     * Removes the view specified by buildLayout() from the windowManager passed during initialization
     */
    public void remove() {
        if (!isAdded) return;
        try {
            windowManager.removeView(myLayout);
            isAdded = false;
        } catch (Exception e) {
            //Do nothing
        }
    }

    public boolean isVisible() {
        return isAdded;
    }

    protected int getDisplayWidth() {
        DisplayMetrics displaymetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displaymetrics);
        return displaymetrics.widthPixels;
    }

    protected int getDisplayHeight() {
        DisplayMetrics displaymetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displaymetrics);
        return displaymetrics.heightPixels;
    }

    protected View getViewRoot() {
        return myLayout;
    }

    public Context getContext() {
        return context;
    }
}