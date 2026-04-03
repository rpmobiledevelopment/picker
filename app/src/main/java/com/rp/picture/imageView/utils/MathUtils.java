package com.rp.picture.imageView.utils;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;

import androidx.annotation.NonNull;
import androidx.annotation.Size;

import com.rp.picture.imageView.State;

public class MathUtils {

    private static final Matrix tmpMatrix = new Matrix();
    private static final Matrix tmpMatrixInverse = new Matrix();
    private static final RectF tmpRect = new RectF();

    private MathUtils() {}

    public static float restrict(float value, float minValue, float maxValue) {
        return Math.max(minValue, Math.min(value, maxValue));
    }

    public static float interpolate(float start, float end, float factor) {
        return start + (end - start) * factor;
    }

    public static void interpolate(
            @NonNull RectF out,
            @NonNull RectF start,
            @NonNull RectF end,
            float factor
    ) {
        out.left = interpolate(start.left, end.left, factor);
        out.top = interpolate(start.top, end.top, factor);
        out.right = interpolate(start.right, end.right, factor);
        out.bottom = interpolate(start.bottom, end.bottom, factor);
    }
    @SuppressWarnings("WeakerAccess") // Public API
    @Deprecated
    public static void interpolate(
            @NonNull State out,
            @NonNull State start,
            @NonNull State end,
            float factor
    ) {
        interpolate(out, start, start.getX(), start.getY(), end, end.getX(), end.getY(), factor);
    }

    public static void interpolate(
            @NonNull State out,
            @NonNull State start,
            float startPivotX,
            float startPivotY,
            @NonNull State end,
            float endPivotX,
            float endPivotY,
            float factor
    ) {
        out.set(start);

        if (!State.equals(start.getZoom(), end.getZoom())) {
            float zoom = interpolate(start.getZoom(), end.getZoom(), factor);
            out.zoomTo(zoom, startPivotX, startPivotY);
        }

        // Getting rotations
        float startRotation = start.getRotation();
        float endRotation = end.getRotation();

        float rotation = Float.NaN;

        // Choosing shortest path to interpolate
        if (Math.abs(startRotation - endRotation) <= 180f) {
            if (!State.equals(startRotation, endRotation)) {
                rotation = interpolate(startRotation, endRotation, factor);
            }
        } else {
            // Keeping rotation positive
            float startRotationPositive = startRotation < 0f ? startRotation + 360f : startRotation;
            float endRotationPositive = endRotation < 0f ? endRotation + 360f : endRotation;

            if (!State.equals(startRotationPositive, endRotationPositive)) {
                rotation = interpolate(startRotationPositive, endRotationPositive, factor);
            }
        }

        if (!Float.isNaN(rotation)) {
            out.rotateTo(rotation, startPivotX, startPivotY);
        }

        float dx = interpolate(0f, endPivotX - startPivotX, factor);
        float dy = interpolate(0f, endPivotY - startPivotY, factor);
        out.translateBy(dx, dy);
    }

    public static void computeNewPosition(
            @NonNull @Size(2) float[] point,
            @NonNull State initialState,
            @NonNull State finalState
    ) {
        initialState.get(tmpMatrix);
        tmpMatrix.invert(tmpMatrixInverse);
        tmpMatrixInverse.mapPoints(point);
        finalState.get(tmpMatrix);
        tmpMatrix.mapPoints(point);
    }

    public static void mapIntRect(@NonNull Matrix matrix, @NonNull Rect rect) {
        tmpRect.set(rect);
        matrix.mapRect(tmpRect);
        rect.set((int) tmpRect.left, (int) tmpRect.top, (int) tmpRect.right, (int) tmpRect.bottom);
    }

}
