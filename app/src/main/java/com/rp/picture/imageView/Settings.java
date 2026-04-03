package com.rp.picture.imageView;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.Gravity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.rp.picture.R;
import com.rp.picture.imageView.internal.UnitsUtils;

@SuppressWarnings({ "WeakerAccess", "UnusedReturnValue", "SameParameterValue" }) // Public API
public class Settings {

    public static final float MAX_ZOOM = 2f;
    public static final float OVERZOOM_FACTOR = 2f;
    public static final long ANIMATIONS_DURATION = 200L;

    private int viewportW;
    private int viewportH;

    private int movementAreaW;
    private int movementAreaH;

    private boolean isMovementAreaSpecified;

    private int imageW;
    private int imageH;

    private float minZoom = 0f;

    private float maxZoom = MAX_ZOOM;

    private float doubleTapZoom = -1f;

    private float overzoomFactor = OVERZOOM_FACTOR;

    private float overscrollDistanceX;
    private float overscrollDistanceY;

    private boolean isFillViewport = false;

    private int gravity = Gravity.CENTER;

    private Fit fitMethod = Fit.INSIDE;

    private Bounds boundsType = Bounds.NORMAL;

    private boolean isPanEnabled = true;

    private boolean isFlingEnabled = true;

    private boolean isZoomEnabled = true;

    private boolean isRotationEnabled = false;

    private boolean isRestrictRotation = false;

    private boolean isDoubleTapEnabled = true;

    private ExitType exitType = ExitType.ALL;

    private int gesturesDisableCount;

    private int boundsDisableCount;

    private long animationsDuration = ANIMATIONS_DURATION;

    Settings() {
        // Package private constructor
    }

    public void initFromAttributes(@NonNull Context context, @Nullable AttributeSet attrs) {
        if (attrs == null) {
            return;
        }

        try (TypedArray arr = context.obtainStyledAttributes(attrs, R.styleable.GestureView)) {
            movementAreaW = arr.getDimensionPixelSize(
                    R.styleable.GestureView_gest_movementAreaWidth, movementAreaW);
            movementAreaH = arr.getDimensionPixelSize(
                    R.styleable.GestureView_gest_movementAreaHeight, movementAreaH);
            isMovementAreaSpecified = movementAreaW > 0 && movementAreaH > 0;

            minZoom = arr.getFloat(
                    R.styleable.GestureView_gest_minZoom, minZoom);
            maxZoom = arr.getFloat(
                    R.styleable.GestureView_gest_maxZoom, maxZoom);
            doubleTapZoom = arr.getFloat(
                    R.styleable.GestureView_gest_doubleTapZoom, doubleTapZoom);
            overzoomFactor = arr.getFloat(
                    R.styleable.GestureView_gest_overzoomFactor, overzoomFactor);
            overscrollDistanceX = arr.getDimension(
                    R.styleable.GestureView_gest_overscrollX, overscrollDistanceX);
            overscrollDistanceY = arr.getDimension(
                    R.styleable.GestureView_gest_overscrollY, overscrollDistanceY);
            isFillViewport = arr.getBoolean(
                    R.styleable.GestureView_gest_fillViewport, isFillViewport);
            gravity = arr.getInt(
                    R.styleable.GestureView_gest_gravity, gravity);

            int fitMethodPos = arr.getInteger(
                    R.styleable.GestureView_gest_fitMethod, fitMethod.ordinal());
            fitMethod = Fit.values()[fitMethodPos];

            int boundsTypePos = arr.getInteger(
                    R.styleable.GestureView_gest_boundsType, boundsType.ordinal());
            boundsType = Bounds.values()[boundsTypePos];

            isPanEnabled = arr.getBoolean(
                    R.styleable.GestureView_gest_panEnabled, isPanEnabled);
            isFlingEnabled = arr.getBoolean(
                    R.styleable.GestureView_gest_flingEnabled, isFlingEnabled);
            isZoomEnabled = arr.getBoolean(
                    R.styleable.GestureView_gest_zoomEnabled, isZoomEnabled);
            isRotationEnabled = arr.getBoolean(
                    R.styleable.GestureView_gest_rotationEnabled, isRotationEnabled);
            isRestrictRotation = arr.getBoolean(
                    R.styleable.GestureView_gest_restrictRotation, isRestrictRotation);
            isDoubleTapEnabled = arr.getBoolean(
                    R.styleable.GestureView_gest_doubleTapEnabled, isDoubleTapEnabled);
            exitType = arr.getBoolean(
                    R.styleable.GestureView_gest_exitEnabled, true) ? exitType : ExitType.NONE;
            animationsDuration = arr.getInt(
                    R.styleable.GestureView_gest_animationDuration, (int) animationsDuration);

            boolean disableGestures = arr.getBoolean(
                    R.styleable.GestureView_gest_disableGestures, false);
            if (disableGestures) {
                disableGestures();
            }

            boolean disableBounds = arr.getBoolean(
                    R.styleable.GestureView_gest_disableBounds, false);
            if (disableBounds) {
                disableBounds();
            }
        }
    }

    @NonNull
    public Settings setViewport(int width, int height) {
        viewportW = width;
        viewportH = height;
        return this;
    }
    @NonNull
    public Settings setMovementArea(int width, int height) {
        isMovementAreaSpecified = true;
        movementAreaW = width;
        movementAreaH = height;
        return this;
    }

    @NonNull
    public Settings setImage(int width, int height) {
        imageW = width;
        imageH = height;
        return this;
    }

    @NonNull
    public Settings setMinZoom(float minZoom) {
        this.minZoom = minZoom;
        return this;
    }

    @NonNull
    public Settings setMaxZoom(float maxZoom) {
        this.maxZoom = maxZoom;
        return this;
    }

    @NonNull
    public Settings setDoubleTapZoom(float doubleTapZoom) {
        this.doubleTapZoom = doubleTapZoom;
        return this;
    }

    @NonNull
    public Settings setOverzoomFactor(float factor) {
        if (factor < 1f) {
            throw new IllegalArgumentException("Overzoom factor cannot be < 1");
        }
        overzoomFactor = factor;
        return this;
    }

    @NonNull
    public Settings setOverscrollDistance(float distanceX, float distanceY) {
        if (distanceX < 0f || distanceY < 0f) {
            throw new IllegalArgumentException("Overscroll distance cannot be < 0");
        }
        overscrollDistanceX = distanceX;
        overscrollDistanceY = distanceY;
        return this;
    }

    @NonNull
    public Settings setOverscrollDistance(
            @NonNull Context context,
            float distanceXDp,
            float distanceYDp
    ) {
        return setOverscrollDistance(
                UnitsUtils.toPixels(context, distanceXDp),
                UnitsUtils.toPixels(context, distanceYDp));
    }

    @NonNull
    public Settings setFillViewport(boolean isFitViewport) {
        this.isFillViewport = isFitViewport;
        return this;
    }

    @NonNull
    public Settings setGravity(int gravity) {
        this.gravity = gravity;
        return this;
    }

    @NonNull
    public Settings setFitMethod(@NonNull Fit fitMethod) {
        this.fitMethod = fitMethod;
        return this;
    }
    @NonNull
    public Settings setBoundsType(@NonNull Bounds boundsType) {
        this.boundsType = boundsType;
        return this;
    }
    @NonNull
    public Settings setPanEnabled(boolean enabled) {
        isPanEnabled = enabled;
        return this;
    }

    @NonNull
    public Settings setFlingEnabled(boolean enabled) {
        isFlingEnabled = enabled;
        return this;
    }

    @NonNull
    public Settings setZoomEnabled(boolean enabled) {
        isZoomEnabled = enabled;
        return this;
    }

    @NonNull
    public Settings setRotationEnabled(boolean enabled) {
        isRotationEnabled = enabled;
        return this;
    }

    @NonNull
    public Settings setRestrictRotation(boolean restrict) {
        isRestrictRotation = restrict;
        return this;
    }

    @NonNull
    public Settings setDoubleTapEnabled(boolean enabled) {
        isDoubleTapEnabled = enabled;
        return this;
    }

    @SuppressWarnings("unused") // Public API
    @NonNull
    public Settings setExitEnabled(boolean enabled) {
        exitType = enabled ? ExitType.ALL : ExitType.NONE;
        return this;
    }

    @SuppressWarnings("unused") // Public API
    @NonNull
    public Settings setExitType(@NonNull ExitType type) {
        exitType = type;
        return this;
    }

    @NonNull
    public Settings disableGestures() {
        gesturesDisableCount++;
        return this;
    }

    @NonNull
    public Settings enableGestures() {
        gesturesDisableCount--;
        return this;
    }

    @NonNull
    public Settings disableBounds() {
        boundsDisableCount++;
        return this;
    }

    @NonNull
    public Settings enableBounds() {
        boundsDisableCount--;
        return this;
    }

    @NonNull
    public Settings setAnimationsDuration(long duration) {
        if (duration < 0L) {
            throw new IllegalArgumentException("Animations duration should be >= 0");
        }
        animationsDuration = duration;
        return this;
    }

    // --------------
    //  Getters
    // --------------

    public int getViewportW() {
        return viewportW;
    }

    public int getViewportH() {
        return viewportH;
    }

    public int getMovementAreaW() {
        return isMovementAreaSpecified ? movementAreaW : viewportW;
    }

    public int getMovementAreaH() {
        return isMovementAreaSpecified ? movementAreaH : viewportH;
    }

    public int getImageW() {
        return imageW;
    }

    public int getImageH() {
        return imageH;
    }

    public float getMinZoom() {
        return minZoom;
    }

    public float getMaxZoom() {
        return maxZoom;
    }

    public float getDoubleTapZoom() {
        return doubleTapZoom;
    }

    public float getOverzoomFactor() {
        return overzoomFactor;
    }

    public float getOverscrollDistanceX() {
        return overscrollDistanceX;
    }

    public float getOverscrollDistanceY() {
        return overscrollDistanceY;
    }

    public boolean isFillViewport() {
        return isFillViewport;
    }

    public int getGravity() {
        return gravity;
    }

    @NonNull
    public Fit getFitMethod() {
        return fitMethod;
    }

    @NonNull
    public Bounds getBoundsType() {
        return boundsType;
    }

    public boolean isPanEnabled() {
        return isGesturesEnabled() && isPanEnabled;
    }

    public boolean isFlingEnabled() {
        return isGesturesEnabled() && isFlingEnabled;
    }

    public boolean isZoomEnabled() {
        return isGesturesEnabled() && isZoomEnabled;
    }

    public boolean isRotationEnabled() {
        return isGesturesEnabled() && isRotationEnabled;
    }

    public boolean isRestrictRotation() {
        return isRestrictRotation;
    }

    public boolean isDoubleTapEnabled() {
        return isGesturesEnabled() && isDoubleTapEnabled;
    }

    public boolean isExitEnabled() {
        return getExitType() != ExitType.NONE;
    }

    @NonNull
    public ExitType getExitType() {
        return isGesturesEnabled() ? exitType : ExitType.NONE;
    }

    public boolean isGesturesEnabled() {
        return gesturesDisableCount <= 0;
    }

    public boolean isRestrictBounds() {
        return boundsDisableCount <= 0;
    }

    public long getAnimationsDuration() {
        return animationsDuration;
    }

    public boolean isEnabled() {
        return isGesturesEnabled()
                && (isPanEnabled || isZoomEnabled || isRotationEnabled || isDoubleTapEnabled);
    }


    @SuppressWarnings("BooleanMethodIsAlwaysInverted") // Public API
    public boolean hasImageSize() {
        return imageW != 0 && imageH != 0;
    }

    public boolean hasViewportSize() {
        return viewportW != 0 && viewportH != 0;
    }


    public enum Fit {
        HORIZONTAL,
        VERTICAL,
        INSIDE,
        OUTSIDE,
        NONE
    }

    public enum Bounds {
        NORMAL,
        INSIDE,
        OUTSIDE,
        PIVOT,
        NONE
    }

    public enum ExitType {
        ALL,
        SCROLL,
        ZOOM,
        NONE
    }

}
