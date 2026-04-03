package com.rp.picture.imageView.utils;

import android.os.SystemClock;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

@SuppressWarnings("unused")
public class FloatScroller {

    private static final long DEFAULT_DURATION = 250L;

    private final Interpolator interpolator;

    private boolean finished = true;

    private float startValue;
    private float finalValue;

    private float currValue;

    private long startRtc;

    private long duration = DEFAULT_DURATION;

    public FloatScroller() {
        interpolator = new AccelerateDecelerateInterpolator();
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public void forceFinished() {
        finished = true;
    }

    @SuppressWarnings("WeakerAccess") // Public API
    public void abortAnimation() {
        finished = true;
        currValue = finalValue;
    }

    public void startScroll(float startValue, float finalValue) {
        finished = false;
        startRtc = SystemClock.elapsedRealtime();

        this.startValue = startValue;
        this.finalValue = finalValue;
        currValue = startValue;
    }

    public boolean computeScroll() {
        if (finished) {
            return false;
        }

        long elapsed = SystemClock.elapsedRealtime() - startRtc;
        if (elapsed >= duration) {
            finished = true;
            currValue = finalValue;
            return false;
        }

        float time = interpolator.getInterpolation((float) elapsed / duration);
        currValue = interpolate(startValue, finalValue, time);
        return true;
    }

    public boolean isFinished() {
        return finished;
    }

    public float getStart() {
        return startValue;
    }

    public float getFinal() {
        return finalValue;
    }

    public float getCurr() {
        return currValue;
    }

    private static float interpolate(float x1, float x2, float state) {
        return x1 + (x2 - x1) * state;
    }

}
