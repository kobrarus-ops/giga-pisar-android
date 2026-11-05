package ru.gigapisar.overlay

import android.content.Context
import android.graphics.Point
import android.os.Build
import android.view.WindowManager

/**
 * The size of the screen the overlays are on, in pixels. Asked from the window manager:
 * a foldable's resources may still describe the other screen after it was folded or
 * opened, and the overlays then end up off the edge.
 */
internal object ScreenSize {
    fun of(
        context: Context,
        windowManager: WindowManager,
    ): Point {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val bounds = windowManager.maximumWindowMetrics.bounds
                if (bounds.width() > 0 && bounds.height() > 0) return Point(bounds.width(), bounds.height())
            } else {
                val size = Point()
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.getRealSize(size)
                if (size.x > 0 && size.y > 0) return size
            }
        } catch (_: Exception) {
            // Fall through to the resources.
        }
        val metrics = context.resources.displayMetrics
        return Point(metrics.widthPixels, metrics.heightPixels)
    }
}
