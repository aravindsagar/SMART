package com.cs565project.smart.service;

import android.content.Context;
import android.graphics.PixelFormat;
import android.view.View;
import android.view.WindowManager;

import com.cs565project.smart.R;

class BlockOverlay extends OverlayBase {

    BlockOverlay(Context context, WindowManager windowManager) {
        super(context, windowManager, R.layout.block_overlay);
    }

    @Override
    void setupLayout(View rootView) {
        // TODO
    }

    @Override
    WindowManager.LayoutParams buildLayoutParams() {
        int overlayType = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;

        return new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT, overlayType,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
    }
}
