package com.rp.picture.imageView.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;

import com.rp.picture.imageView.GestureController;
import com.rp.picture.imageView.Settings;
import com.rp.picture.imageView.State;

public class CropUtils {

    private CropUtils() {}

    @Nullable
    public static Bitmap crop(Drawable drawable, GestureController controller) {
        if (drawable == null) {
            return null;
        }

        controller.stopAllAnimations();
        controller.updateState(); // Applying state restrictions

        final Settings settings = controller.getSettings();
        final State state = controller.getState();
        final float zoom = state.getZoom();

        // Computing crop size for base zoom level (zoom == 1)
        int width = Math.round(settings.getMovementAreaW() / zoom);
        int height = Math.round(settings.getMovementAreaH() / zoom);

        // Crop area coordinates within viewport
        Rect pos = new Rect();
        GravityUtils.getMovementAreaPosition(settings, pos);

        Matrix matrix = new Matrix();
        state.get(matrix);
        // Scaling to base zoom level (zoom == 1)
        matrix.postScale(1f / zoom, 1f / zoom, pos.left, pos.top);
        // Positioning crop area
        matrix.postTranslate(-pos.left, -pos.top);

        try {
            // Draw drawable into bitmap
            Bitmap dst = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

            Canvas canvas = new Canvas(dst);
            canvas.concat(matrix);
            drawable.draw(canvas);

            return dst;
        } catch (OutOfMemoryError e) {
            return null; // Not enough memory for cropped bitmap
        }
    }

}
