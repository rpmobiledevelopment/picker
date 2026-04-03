package com.rp.picture.imageView;

import static java.lang.Float.isNaN;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.widget.OverScroller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.rp.picture.imageView.internal.AnimationEngine;
import com.rp.picture.imageView.internal.ExitController;
import com.rp.picture.imageView.internal.MovementBounds;
import com.rp.picture.imageView.internal.detectors.RotationGestureDetector;
import com.rp.picture.imageView.internal.detectors.ScaleGestureDetectorFixed;
import com.rp.picture.imageView.utils.FloatScroller;
import com.rp.picture.imageView.utils.GravityUtils;
import com.rp.picture.imageView.utils.MathUtils;

import java.util.ArrayList;
import java.util.List;


public class GestureController implements View.OnTouchListener {

    private static final float FLING_COEFFICIENT = 0.9f;

    // Temporary objects
    private static final PointF tmpPointF = new PointF();
    private static final Point tmpPoint = new Point();
    private static final RectF tmpRectF = new RectF();
    private static final float[] tmpPointArr = new float[2];

    // Control constants converted to pixels
    private final int touchSlop;
    private final int minVelocity;
    private final int maxVelocity;

    private OnGestureListener gestureListener;
    private OnStateSourceChangeListener sourceListener;
    private final List<OnStateChangeListener> stateListeners = new ArrayList<>();

    private final AnimationEngine animationEngine;

    // Various gesture detectors
    private final GestureDetector gestureDetector;
    private final ScaleGestureDetector scaleDetector;
    private final RotationGestureDetector rotateDetector;

    private boolean isInterceptTouchCalled;
    private boolean isInterceptTouchDisallowed;
    private boolean isScrollDetected;
    private boolean isScaleDetected;
    private boolean isRotationDetected;

    private float pivotX = Float.NaN;
    private float pivotY = Float.NaN;
    private float endPivotX = Float.NaN;
    private float endPivotY = Float.NaN;

    private boolean isStateChangedDuringTouch;
    private boolean isRestrictZoomRequested;
    private boolean isRestrictRotationRequested;
    private boolean isAnimatingInBounds;

    private StateSource stateSource = StateSource.NONE;

    private final OverScroller flingScroller;
    private final FloatScroller stateScroller;

    private final MovementBounds flingBounds;
    private final State stateStart = new State();
    private final State stateEnd = new State();

    private final View targetView;
    private final Settings settings;
    private final State state = new State();
    private final State prevState = new State();
    private final StateController stateController;
    private final ExitController exitController;

    public GestureController(@NonNull View view) {
        final Context context = view.getContext();

        targetView = view;
        settings = new Settings();
        stateController = new StateController(settings);

        animationEngine = new LocalAnimationEngine(view);
        InternalGesturesListener internalListener = new InternalGesturesListener();
        gestureDetector = new GestureDetector(context, internalListener);
        scaleDetector = new ScaleGestureDetectorFixed(context, internalListener);
        rotateDetector = new RotationGestureDetector(context, internalListener);

        exitController = new ExitController(view, this);

        flingScroller = new OverScroller(context);
        stateScroller = new FloatScroller();

        flingBounds = new MovementBounds(settings);

        final ViewConfiguration configuration = ViewConfiguration.get(context);
        touchSlop = configuration.getScaledTouchSlop();
        minVelocity = configuration.getScaledMinimumFlingVelocity();
        maxVelocity = configuration.getScaledMaximumFlingVelocity();
    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Public API
    public void setOnGesturesListener(@Nullable OnGestureListener listener) {
        gestureListener = listener;
    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Public API
    public void setOnStateSourceChangeListener(@Nullable OnStateSourceChangeListener listener) {
        sourceListener = listener;
    }
    public void addOnStateChangeListener(@NonNull OnStateChangeListener listener) {
        stateListeners.add(listener);
    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Public API
    public void removeOnStateChangeListener(@NonNull OnStateChangeListener listener) {
        stateListeners.remove(listener);
    }

    @NonNull
    public Settings getSettings() {
        return settings;
    }

    @NonNull
    public State getState() {
        return state;
    }

    @SuppressWarnings("WeakerAccess") // Public API
    @NonNull
    public StateController getStateController() {
        return stateController;
    }

    public void updateState() {
        // Applying zoom patch (needed in case if image size is changed)
        stateController.applyZoomPatch(state);
        stateController.applyZoomPatch(prevState);
        stateController.applyZoomPatch(stateStart);
        stateController.applyZoomPatch(stateEnd);
        exitController.applyZoomPatch();

        boolean reset = stateController.updateState(state);
        if (reset) {
            notifyStateReset();
        } else {
            notifyStateUpdated();
        }
    }

    public void resetState() {
        stopAllAnimations();
        boolean reset = stateController.resetState(state);
        if (reset) {
            notifyStateReset();
        } else {
            notifyStateUpdated();
        }
    }

    @SuppressWarnings("unused") // Public API
    public void setPivot(float pivotX, float pivotY) {
        this.pivotX = pivotX;
        this.pivotY = pivotY;
    }

    @SuppressWarnings({ "WeakerAccess", "UnusedReturnValue" }) // Public API
    public boolean animateKeepInBounds() {
        return animateStateTo(state, true);
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean animateStateTo(@Nullable State endState) {
        return animateStateTo(endState, true);
    }

    private boolean animateStateTo(@Nullable State endState, boolean keepInBounds) {
        if (endState == null) {
            return false;
        }

        stopAllAnimations();

        // Ensure we have a correct pivot point
        if (isNaN(pivotX) || isNaN(pivotY)) {
            GravityUtils.getDefaultPivot(settings, tmpPoint);
            pivotX = tmpPoint.x;
            pivotY = tmpPoint.y;
        }

        State endStateRestricted = null;
        if (keepInBounds) {
            endStateRestricted = stateController.restrictStateBoundsCopy(
                    endState, prevState, pivotX, pivotY, false, false, true);
        }
        if (endStateRestricted == null) {
            endStateRestricted = endState;
        }

        if (endStateRestricted.equals(state)) {
            return false; // Nothing to animate
        }

        isAnimatingInBounds = keepInBounds;
        stateStart.set(state);
        stateEnd.set(endStateRestricted);

        // Computing new position of pivot point for correct state interpolation
        tmpPointArr[0] = pivotX;
        tmpPointArr[1] = pivotY;
        MathUtils.computeNewPosition(tmpPointArr, stateStart, stateEnd);
        endPivotX = tmpPointArr[0];
        endPivotY = tmpPointArr[1];

        stateScroller.setDuration(settings.getAnimationsDuration());
        stateScroller.startScroll(0f, 1f);
        animationEngine.start();

        notifyStateSourceChanged();

        return true;
    }

    @SuppressWarnings("WeakerAccess") // Public API
    public boolean isAnimatingState() {
        return !stateScroller.isFinished();
    }

    @SuppressWarnings("WeakerAccess") // Public API
    public boolean isAnimatingFling() {
        return !flingScroller.isFinished();
    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Public API
    public boolean isAnimating() {
        return isAnimatingState() || isAnimatingFling();
    }

    @SuppressWarnings("WeakerAccess") // Public API
    public void stopStateAnimation() {
        if (isAnimatingState()) {
            stateScroller.forceFinished();
            onStateAnimationFinished(true);
        }
    }

    @SuppressWarnings("WeakerAccess") // Public API
    public void stopFlingAnimation() {
        if (isAnimatingFling()) {
            flingScroller.forceFinished(true);
            onFlingAnimationFinished(true);
        }
    }

    public void stopAllAnimations() {
        stopStateAnimation();
        stopFlingAnimation();
    }

    @SuppressWarnings({"WeakerAccess", "unused"}) // Public API (can be overridden)
    protected void onStateAnimationFinished(boolean forced) {
        isAnimatingInBounds = false;
        pivotX = Float.NaN;
        pivotY = Float.NaN;
        endPivotX = Float.NaN;
        endPivotY = Float.NaN;
        notifyStateSourceChanged();
    }

    @SuppressWarnings("WeakerAccess") // Public API (can be overridden)
    protected void onFlingAnimationFinished(boolean forced) {
        if (!forced) {
            animateKeepInBounds();
        }
        notifyStateSourceChanged();
    }

    @SuppressWarnings("WeakerAccess") // Public API (can be overridden)
    protected void notifyStateUpdated() {
        prevState.set(state);
        for (OnStateChangeListener listener : stateListeners) {
            listener.onStateChanged(state);
        }
    }

    @SuppressWarnings("WeakerAccess") // Public API (can be overridden)
    protected void notifyStateReset() {
        exitController.stopDetection();
        for (OnStateChangeListener listener : stateListeners) {
            listener.onStateReset(prevState, state);
        }
        notifyStateUpdated();
    }

    private void notifyStateSourceChanged() {
        StateSource type = StateSource.NONE;
        if (isAnimating()) {
            type = StateSource.ANIMATION;
        } else if (isScrollDetected || isScaleDetected || isRotationDetected) {
            type = StateSource.USER;
        }

        if (stateSource != type) {
            stateSource = type;
            if (sourceListener != null) {
                sourceListener.onStateSourceChanged(type);
            }
        }
    }


    // -------------------
    //  Gestures handling
    // -------------------

    public boolean onInterceptTouch(@NonNull View view, @NonNull MotionEvent event) {
        isInterceptTouchCalled = true;
        return onTouchInternal(view, event);
    }

    @SuppressLint("ClickableViewAccessibility") // performClick is called in gestures callbacks
    @Override
    public boolean onTouch(@NonNull View view, @NonNull MotionEvent event) {
        if (!isInterceptTouchCalled) { // Preventing duplicate events
            onTouchInternal(view, event);
        }
        isInterceptTouchCalled = false;
        return settings.isEnabled();
    }

    protected boolean onTouchInternal(@NonNull View view, @NonNull MotionEvent event) {
        MotionEvent viewportEvent = MotionEvent.obtain(event);
        viewportEvent.offsetLocation(-view.getPaddingLeft(), -view.getPaddingTop());

        gestureDetector.setIsLongpressEnabled(view.isLongClickable());

        boolean result = gestureDetector.onTouchEvent(viewportEvent);
        scaleDetector.onTouchEvent(viewportEvent);
        rotateDetector.onTouchEvent(viewportEvent);
        result = result || isScaleDetected || isRotationDetected;

        notifyStateSourceChanged();

        if (exitController.isExitDetected()) {
            if (!state.equals(prevState)) {
                notifyStateUpdated();
            }
        }

        if (isStateChangedDuringTouch) {
            isStateChangedDuringTouch = false;

            stateController.restrictStateBounds(
                    state, prevState, pivotX, pivotY, true, true, false);

            if (!state.equals(prevState)) {
                notifyStateUpdated();
            }
        }

        if (isRestrictZoomRequested || isRestrictRotationRequested) {
            isRestrictZoomRequested = false;
            isRestrictRotationRequested = false;

            if (!exitController.isExitDetected()) {
                State restrictedState = stateController.restrictStateBoundsCopy(
                        state, prevState, pivotX, pivotY, true, false, true);
                animateStateTo(restrictedState, false);
            }
        }

        if (viewportEvent.getActionMasked() == MotionEvent.ACTION_UP
                || viewportEvent.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            onUpOrCancel(viewportEvent);
            notifyStateSourceChanged();
        }

        if (!isInterceptTouchDisallowed && shouldDisallowInterceptTouch(viewportEvent)) {
            isInterceptTouchDisallowed = true;

            final ViewParent parent = view.getParent();
            if (parent != null) {
                parent.requestDisallowInterceptTouchEvent(true);
            }
        }

        viewportEvent.recycle();

        return result;
    }

    protected boolean shouldDisallowInterceptTouch(MotionEvent event) {
        if (exitController.isExitDetected()) {
            return true;
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE: {
                // If view can be panned then parent should not intercept touch events.
                // We should check it on DOWN event since parent may quickly take control over us
                // in case of a very fast MOVE action.
                stateController.getMovementArea(state, tmpRectF);
                final boolean isPannable = State.compare(tmpRectF.width(), 0f) > 0
                        || State.compare(tmpRectF.height(), 0f) > 0;

                if (settings.isPanEnabled() && (isPannable || !settings.isRestrictBounds())) {
                    return true;
                }
                break;
            }
            case MotionEvent.ACTION_POINTER_DOWN: {
                // If view can be zoomed or rotated then parent should not intercept touch events.
                return settings.isZoomEnabled() || settings.isRotationEnabled();
            }
            default:
        }

        return false;
    }

    protected boolean onDown(@NonNull MotionEvent event) {
        isInterceptTouchDisallowed = false;

        stopFlingAnimation();

        if (gestureListener != null) {
            gestureListener.onDown(event);
        }

        return false;
    }

    protected void onUpOrCancel(@NonNull MotionEvent event) {
        isScrollDetected = false;
        isScaleDetected = false;
        isRotationDetected = false;

        exitController.onUpOrCancel();

        if (!isAnimatingFling() && !isAnimatingInBounds) {
            animateKeepInBounds();
        }

        if (gestureListener != null) {
            gestureListener.onUpOrCancel(event);
        }
    }

    @SuppressWarnings("WeakerAccess") // Public API (can be overridden)
    protected boolean onSingleTapUp(@NonNull MotionEvent event) {
        // If double tap is not enabled then it should be safe to propagate click event from here
        if (!settings.isDoubleTapEnabled()) {
            targetView.performClick();
        }
        return gestureListener != null && gestureListener.onSingleTapUp(event);
    }

    @SuppressWarnings("WeakerAccess") // Public API (can be overridden)
    protected void onLongPress(@NonNull MotionEvent event) {
        if (settings.isEnabled()) {
            targetView.performLongClick();

            if (gestureListener != null) {
                gestureListener.onLongPress(event);
            }
        }
    }

    protected boolean onScroll(@NonNull MotionEvent e1, @NonNull MotionEvent e2,
            float dx, float dy) {

        if (!settings.isPanEnabled() || isAnimatingState()) {
            return false;
        }

        if (isNaN(dx) || isNaN(dy)) {
            return false; // Invalid scroll, nothing we can do
        }

        boolean scrollConsumed = exitController.onScroll(-dx, -dy);
        if (scrollConsumed) {
            return true;
        }

        if (!isScrollDetected) {
            isScrollDetected = Math.abs(e2.getX() - e1.getX()) > touchSlop
                    || Math.abs(e2.getY() - e1.getY()) > touchSlop;

            // First scroll event can stutter a bit, so we will ignore it for smoother scrolling
            if (isScrollDetected) {
                // By returning false here we give children views a chance to intercept this scroll
                return false;
            }
        }

        if (isScrollDetected) {
            state.translateBy(-dx, -dy);
            isStateChangedDuringTouch = true;
        }

        return isScrollDetected;
    }

    protected boolean onFling(@NonNull MotionEvent e1, @NonNull MotionEvent e2,
            float vx, float vy) {

        if (!settings.isPanEnabled() || !settings.isFlingEnabled() || isAnimatingState()) {
            return false;
        }

        boolean flingConsumed = exitController.onFling();
        if (flingConsumed) {
            return true;
        }

        stopFlingAnimation();

        // Fling bounds including current position
        flingBounds.set(state).extend(state.getX(), state.getY());

        flingScroller.fling(
                Math.round(state.getX()), Math.round(state.getY()),
                limitFlingVelocity(vx * FLING_COEFFICIENT),
                limitFlingVelocity(vy * FLING_COEFFICIENT),
                Integer.MIN_VALUE, Integer.MAX_VALUE,
                Integer.MIN_VALUE, Integer.MAX_VALUE);
        animationEngine.start();

        notifyStateSourceChanged();

        return true;
    }

    private int limitFlingVelocity(float velocity) {
        if (Math.abs(velocity) < minVelocity) {
            return 0;
        } else if (Math.abs(velocity) >= maxVelocity) {
            return (int) Math.signum(velocity) * maxVelocity;
        } else {
            return Math.round(velocity);
        }
    }
    @SuppressWarnings("WeakerAccess") // Public API (can be overridden)
    protected boolean onFlingScroll(int dx, int dy) {
        float prevX = state.getX();
        float prevY = state.getY();
        float toX = prevX + dx;
        float toY = prevY + dy;

        if (settings.isRestrictBounds()) {
            flingBounds.restrict(toX, toY, tmpPointF);
            toX = tmpPointF.x;
            toY = tmpPointF.y;
        }

        state.translateTo(toX, toY);
        return !State.equals(prevX, toX) || !State.equals(prevY, toY);
    }

    @SuppressWarnings("WeakerAccess") // Public API (can be overridden)
    protected boolean onSingleTapConfirmed(MotionEvent event) {
        // If double tap is enabled we should propagate click only if we aren't in a double tap now
        if (settings.isDoubleTapEnabled()) {
            targetView.performClick();
        }
        return gestureListener != null && gestureListener.onSingleTapConfirmed(event);
    }

    protected boolean onDoubleTapEvent(MotionEvent event) {
        if (!settings.isDoubleTapEnabled()) {
            return false;
        }

        if (event.getActionMasked() != MotionEvent.ACTION_UP) {
            return false;
        }

        // ScaleGestureDetector can perform zoom by "double tap & drag" since KITKAT,
        // so we should suppress our double tap in this case
        if (isScaleDetected) {
            return false;
        }

        // Let user redefine double tap
        if (gestureListener != null && gestureListener.onDoubleTap(event)) {
            return true;
        }

        animateStateTo(stateController.toggleMinMaxZoom(state, event.getX(), event.getY()));
        return true;
    }

    protected boolean onScaleBegin(ScaleGestureDetector detector) {
        isScaleDetected = settings.isZoomEnabled();
        if (isScaleDetected) {
            exitController.onScaleBegin();
        }
        return isScaleDetected;
    }

    @SuppressWarnings("WeakerAccess") // Public API (can be overridden)
    protected boolean onScale(ScaleGestureDetector detector) {
        if (!settings.isZoomEnabled() || isAnimatingState()) {
            return false; // Ignoring scroll if animation is in progress
        }

        final float scaleFactor = detector.getScaleFactor();
        if (isNaN(scaleFactor) || isNaN(detector.getFocusX()) || isNaN(detector.getFocusY())) {
            return false; // Invalid scale, nothing we can do
        }

        boolean scaleConsumed = exitController.onScale(scaleFactor);
        if (scaleConsumed) {
            return true;
        }

        pivotX = detector.getFocusX();
        pivotY = detector.getFocusY();
        state.zoomBy(scaleFactor, pivotX, pivotY);
        isStateChangedDuringTouch = true;

        return true;
    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Public API (can be overridden)
    protected void onScaleEnd(ScaleGestureDetector detector) {
        if (isScaleDetected) {
            exitController.onScaleEnd();
        }
        isScaleDetected = false;
        isRestrictZoomRequested = true;
    }

    protected boolean onRotationBegin(RotationGestureDetector detector) {
        isRotationDetected = settings.isRotationEnabled();
        if (isRotationDetected) {
            exitController.onRotationBegin();
        }
        return isRotationDetected;
    }

    @SuppressWarnings("WeakerAccess") // Public API (can be overridden)
    protected boolean onRotate(RotationGestureDetector detector) {
        if (!settings.isRotationEnabled() || isAnimatingState()) {
            return false;
        }

        boolean rotateConsumed = exitController.onRotate();
        if (rotateConsumed) {
            return true;
        }

        pivotX = detector.getFocusX();
        pivotY = detector.getFocusY();
        state.rotateBy(detector.getRotationDelta(), pivotX, pivotY);
        isStateChangedDuringTouch = true;

        return true;
    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Public API (can be overridden)
    protected void onRotationEnd(RotationGestureDetector detector) {
        if (isRotationDetected) {
            exitController.onRotationEnd();
        }
        isRotationDetected = false;
        isRestrictRotationRequested = true;
    }

    private class LocalAnimationEngine extends AnimationEngine {
        LocalAnimationEngine(@NonNull View view) {
            super(view);
        }

        @Override
        public boolean onStep() {
            boolean shouldProceed = false;

            if (isAnimatingFling()) {
                int prevX = flingScroller.getCurrX();
                int prevY = flingScroller.getCurrY();

                if (flingScroller.computeScrollOffset()) {
                    int dx = flingScroller.getCurrX() - prevX;
                    int dy = flingScroller.getCurrY() - prevY;

                    if (!onFlingScroll(dx, dy)) {
                        stopFlingAnimation();
                    }

                    shouldProceed = true;
                }

                if (!isAnimatingFling()) {
                    onFlingAnimationFinished(false);
                }
            }

            if (isAnimatingState()) {
                stateScroller.computeScroll();
                float factor = stateScroller.getCurr();

                MathUtils.interpolate(
                        state,
                        stateStart, pivotX, pivotY,
                        stateEnd, endPivotX, endPivotY,
                        factor
                );

                shouldProceed = true;

                if (!isAnimatingState()) {
                    onStateAnimationFinished(false);
                }
            }

            if (shouldProceed) {
                notifyStateUpdated();
            }

            return shouldProceed;
        }
    }

    @SuppressWarnings("unused")
    public interface OnStateChangeListener {
        void onStateChanged(State state);

        void onStateReset(State oldState, State newState);
    }

    @SuppressWarnings("WeakerAccess") // Public API
    public interface OnStateSourceChangeListener {
        void onStateSourceChanged(StateSource source);
    }
    @SuppressWarnings("WeakerAccess") // Public API
    public enum StateSource {
        NONE, USER, ANIMATION
    }

    @SuppressWarnings({ "WeakerAccess", "EmptyMethod", "SameReturnValue", "unused" }) // Public API
    public interface OnGestureListener {

        void onDown(@NonNull MotionEvent event);

        void onUpOrCancel(@NonNull MotionEvent event);

        boolean onSingleTapUp(@NonNull MotionEvent event);

        boolean onSingleTapConfirmed(@NonNull MotionEvent event);

        void onLongPress(@NonNull MotionEvent event);

        boolean onDoubleTap(@NonNull MotionEvent event);
    }

    @SuppressWarnings("WeakerAccess") // Public API
    public static class SimpleOnGestureListener implements OnGestureListener {

        @Override
        public void onDown(@NonNull MotionEvent event) {
            // no-op
        }

        @Override
        public void onUpOrCancel(@NonNull MotionEvent event) {
            // no-op
        }

        @Override
        public boolean onSingleTapUp(@NonNull MotionEvent event) {
            return false;
        }

        @Override
        public boolean onSingleTapConfirmed(@NonNull MotionEvent event) {
            return false;
        }

        @Override
        public void onLongPress(@NonNull MotionEvent event) {
            // no-op
        }

        @Override
        public boolean onDoubleTap(@NonNull MotionEvent event) {
            return false;
        }
    }

    private class InternalGesturesListener implements
            GestureDetector.OnGestureListener,
            GestureDetector.OnDoubleTapListener,
            ScaleGestureDetector.OnScaleGestureListener,
            RotationGestureDetector.OnRotationGestureListener {

        @Override
        public boolean onSingleTapConfirmed(@NonNull MotionEvent event) {
            return GestureController.this.onSingleTapConfirmed(event);
        }

        @Override
        public boolean onDoubleTap(@NonNull MotionEvent event) {
            return false;
        }

        @Override
        public boolean onDoubleTapEvent(@NonNull MotionEvent event) {
            return GestureController.this.onDoubleTapEvent(event);
        }

        @Override
        public boolean onDown(@NonNull MotionEvent event) {
            return GestureController.this.onDown(event);
        }

        @Override
        public void onShowPress(@NonNull MotionEvent event) {
            // No-op
        }

        @Override
        public boolean onSingleTapUp(@NonNull MotionEvent event) {
            return GestureController.this.onSingleTapUp(event);
        }

        @Override
        public boolean onScroll(@Nullable MotionEvent e1, @NonNull MotionEvent e2,
                float distanceX, float distanceY) {
            return e1 != null && GestureController.this.onScroll(e1, e2, distanceX, distanceY);
        }

        @Override
        public void onLongPress(@NonNull MotionEvent event) {
            GestureController.this.onLongPress(event);
        }

        @Override
        public boolean onFling(@Nullable MotionEvent e1, @NonNull MotionEvent e2,
                float velocityX, float velocityY) {
            return e1 != null && GestureController.this.onFling(e1, e2, velocityX, velocityY);
        }

        @Override
        public boolean onRotate(@NonNull RotationGestureDetector detector) {
            return GestureController.this.onRotate(detector);
        }

        @Override
        public boolean onRotationBegin(@NonNull RotationGestureDetector detector) {
            return GestureController.this.onRotationBegin(detector);
        }

        @Override
        public void onRotationEnd(@NonNull RotationGestureDetector detector) {
            GestureController.this.onRotationEnd(detector);
        }

        @Override
        public boolean onScale(@NonNull ScaleGestureDetector detector) {
            return GestureController.this.onScale(detector);
        }

        @Override
        public boolean onScaleBegin(@NonNull ScaleGestureDetector detector) {
            return GestureController.this.onScaleBegin(detector);
        }

        @Override
        public void onScaleEnd(@NonNull ScaleGestureDetector detector) {
            GestureController.this.onScaleEnd(detector);
        }

    }

}
