package com.cs565project.smart.service

import android.content.Context
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager

/**
 * A base class for managing overlay windows.
 */
abstract class OverlayBase(val context: Context, private val windowManager: WindowManager, layoutResource: Int) {
    protected val viewRoot: View
    private var myLayoutParams: WindowManager.LayoutParams? = null

    var isVisible = false
        private set

    protected val displayWidth: Int
        get() {
            val displaymetrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(displaymetrics)
            return displaymetrics.widthPixels
        }

    protected val displayHeight: Int
        get() {
            val displaymetrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(displaymetrics)
            return displaymetrics.heightPixels
        }

    init {

        val inflater = LayoutInflater.from(context)
        viewRoot = inflater.inflate(layoutResource, null, false)
    }

    /**
     * Populate data, setup callbacks etc on the window.
     *
     * @param rootView View inflated from the given layoutResource.
     */
    internal abstract fun setupLayout(rootView: View)

    internal abstract fun buildLayoutParams(): WindowManager.LayoutParams

    /**
     * Adds the view specified by buildLayout() to the windowManager passed during initialization
     */
    fun execute() {
        if (isVisible) return

        viewRoot.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        setupLayout(viewRoot)

        if (myLayoutParams == null) {
            myLayoutParams = buildLayoutParams()
        }

        try {
            windowManager.addView(viewRoot, myLayoutParams)
            isVisible = true
            viewRoot.invalidate()
        } catch (e: IllegalStateException) {
            isVisible = false
        }

    }

    /**
     * Removes the view specified by buildLayout() from the windowManager passed during initialization
     */
    open fun remove() {
        if (!isVisible) return
        try {
            windowManager.removeView(viewRoot)
            isVisible = false
        } catch (e: Exception) {
            //Do nothing
        }

    }

    companion object {
        protected val LOG_TAG = OverlayBase::class.java.simpleName
    }
}