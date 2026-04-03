package com.rp.picture.imageView;

import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.rp.picture.imageView.internal.MovementBounds;
import com.rp.picture.imageView.internal.ZoomBounds;
import com.rp.picture.imageView.utils.GravityUtils;

public class StateController {

    // Temporary objects
    private static final State tmpState = new State();
    private static final Rect tmpRect = new Rect();
    private static final RectF tmpRectF = new RectF();
    private static final PointF tmpPointF = new PointF();


    private final Settings settings;
    private final ZoomBounds zoomBounds;
    private final MovementBounds movBounds;

    private boolean isResetRequired = true;

    private float zoomPatch;

    StateController(Settings settings) {
        this.settings = settings;
        this.zoomBounds = new ZoomBounds(settings);
        this.movBounds = new MovementBounds(settings);
    }

    boolean resetState(State state) {
        isResetRequired = true;
        return updateState(state);
    }

    boolean updateState(State state) {
        if (isResetRequired) {
            // Applying initial state
            state.set(0f, 0f, zoomBounds.set(state).getFitZoom(), 0f);
            GravityUtils.getImagePosition(state, settings, tmpRect);
            state.translateTo(tmpRect.left, tmpRect.top);

            isResetRequired = !settings.hasImageSize() || !settings.hasViewportSize();
            return !isResetRequired;
        } else {
            // Restricts state's translation and zoom bounds, disallowing overscroll / overzoom.
            restrictStateBounds(state, state, Float.NaN, Float.NaN, false, false, true);
            return false;
        }
    }

    public void setTempZoomPatch(float factor) {
        zoomPatch = factor;
    }

    public void applyZoomPatch(@NonNull State state) {
        if (zoomPatch > 0f) {
            state.set(state.getX(), state.getY(), state.getZoom() * zoomPatch, state.getRotation());
        }
    }

    public float applyZoomPatch(float zoom) {
        return zoomPatch > 0f ? zoomPatch * zoom : zoom;
    }

    State toggleMinMaxZoom(State state, float pivotX, float pivotY) {
        zoomBounds.set(state);
        final float minZoom = zoomBounds.getFitZoom();
        final float maxZoom = settings.getDoubleTapZoom() > 0f
                ? settings.getDoubleTapZoom() : zoomBounds.getMaxZoom();

        final float middleZoom = 0.5f * (minZoom + maxZoom);
        final float targetZoom = state.getZoom() < middleZoom ? maxZoom : minZoom;

        final State end = state.copy();
        end.zoomTo(targetZoom, pivotX, pivotY);
        return end;
    }

    @Nullable
    State restrictStateBoundsCopy(State state, State prevState, float pivotX, float pivotY,
            boolean allowOverscroll, boolean allowOverzoom, boolean restrictRotation) {
        tmpState.set(state);
        boolean changed = restrictStateBounds(tmpState, prevState, pivotX, pivotY,
                allowOverscroll, allowOverzoom, restrictRotation);
        return changed ? tmpState.copy() : null;
    }


    boolean restrictStateBounds(State state, State prevState, float pivotX, float pivotY,
            boolean allowOverscroll, boolean allowOverzoom, boolean restrictRotation) {

        if (!settings.isRestrictBounds()) {
            return false;
        }

        if (Float.isNaN(pivotX) || Float.isNaN(pivotY)) {
            pivotX = state.getX();
            pivotY = state.getY();
        }

        boolean isStateChanged = false;

        if (restrictRotation && settings.isRestrictRotation()) {
            float rotation = Math.round(state.getRotation() / 90f) * 90f;
            if (!State.equals(rotation, state.getRotation())) {
                state.rotateTo(rotation, pivotX, pivotY);
                isStateChanged = true;
            }
        }

        zoomBounds.set(state);
        final float minZoom = zoomBounds.getMinZoom();
        final float maxZoom = zoomBounds.getMaxZoom();

        final float extraZoom = allowOverzoom ? settings.getOverzoomFactor() : 1f;
        float zoom = zoomBounds.restrict(state.getZoom(), extraZoom);

        // Applying elastic overzoom
        if (prevState != null) {
            zoom = applyZoomResilience(zoom, prevState.getZoom(), minZoom, maxZoom, extraZoom);
        }

        if (!State.equals(zoom, state.getZoom())) {
            state.zoomTo(zoom, pivotX, pivotY);
            isStateChanged = true;
        }

        float extraX = allowOverscroll ? settings.getOverscrollDistanceX() : 0f;
        float extraY = allowOverscroll ? settings.getOverscrollDistanceY() : 0f;

        movBounds.set(state);
        movBounds.restrict(state.getX(), state.getY(), extraX, extraY, tmpPointF);
        float newX = tmpPointF.x;
        float newY = tmpPointF.y;

        if (zoom < minZoom && extraZoom > 1f) {
            // Decreasing overscroll if zooming less than minimum zoom
            float factor = (extraZoom * zoom / minZoom - 1f) / (extraZoom - 1f);
            factor = (float) Math.sqrt(factor);

            movBounds.restrict(newX, newY, tmpPointF);
            float strictX = tmpPointF.x;
            float strictY = tmpPointF.y;

            newX = strictX + factor * (newX - strictX);
            newY = strictY + factor * (newY - strictY);
        }

        if (prevState != null) {
            movBounds.getExternalBounds(tmpRectF);
            newX = applyTranslationResilience(newX, prevState.getX(),
                    tmpRectF.left, tmpRectF.right, extraX);
            newY = applyTranslationResilience(newY, prevState.getY(),
                    tmpRectF.top, tmpRectF.bottom, extraY);
        }

        if (!State.equals(newX, state.getX()) || !State.equals(newY, state.getY())) {
            state.translateTo(newX, newY);
            isStateChanged = true;
        }

        return isStateChanged;
    }

    private float applyZoomResilience(float zoom, float prevZoom,
            float minZoom, float maxZoom, float overzoom) {
        if (overzoom == 1f) {
            return zoom;
        }

        float minZoomOver = minZoom / overzoom;
        float maxZoomOver = maxZoom * overzoom;

        float resilience = 0f;

        if (zoom < minZoom && zoom < prevZoom) {
            resilience = (minZoom - zoom) / (minZoom - minZoomOver);
        } else if (zoom > maxZoom && zoom > prevZoom) {
            resilience = (zoom - maxZoom) / (maxZoomOver - maxZoom);
        }

        if (resilience == 0f) {
            return zoom;
        } else {
            resilience = (float) Math.sqrt(resilience);
            return zoom + resilience * (prevZoom - zoom);
        }
    }

    private float applyTranslationResilience(float value, float prevValue,
            float boundsMin, float boundsMax, float overscroll) {
        if (overscroll == 0f) {
            return value;
        }

        float resilience = 0f;

        float avg = (value + prevValue) * 0.5f;

        if (avg < boundsMin && value < prevValue) {
            resilience = (boundsMin - avg) / overscroll;
        } else if (avg > boundsMax && value > prevValue) {
            resilience = (avg - boundsMax) / overscroll;
        }

        if (resilience == 0f) {
            return value;
        } else {
            if (resilience > 1f) {
                resilience = 1f;
            }
            resilience = (float) Math.sqrt(resilience);
            return value - resilience * (value - prevValue);
        }
    }

    public float getMinZoom(@NonNull State state) {
        return zoomBounds.set(state).getMinZoom();
    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Public API
    public float getMaxZoom(@NonNull State state) {
        return zoomBounds.set(state).getMaxZoom();
    }

    public float getFitZoom(@NonNull State state) {
        return zoomBounds.set(state).getFitZoom();
    }

    public void getMovementArea(@NonNull State state, @NonNull RectF out) {
        movBounds.set(state).getExternalBounds(out);
    }

}
