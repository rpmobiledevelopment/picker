package com.rp.picture.imageView.transition;

import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.rp.picture.imageView.animation.ViewPosition;
import com.rp.picture.imageView.animation.ViewPositionAnimator;
import com.rp.picture.imageView.animation.ViewPositionAnimator.PositionUpdateListener;
import com.rp.picture.imageView.internal.GestureDebug;
import com.rp.picture.imageView.views.interfaces.AnimatorView;

import java.util.ArrayList;
import java.util.List;

public class ViewsTransitionAnimator<ID> extends ViewsCoordinator<ID> {

    private static final Object NONE = new Object();

    private static final String TAG = ViewsTransitionAnimator.class.getSimpleName();

    private final List<PositionUpdateListener> listeners = new ArrayList<>();

    private boolean enterWithAnimation;
    private boolean isEntered;

    private boolean exitRequested;
    private boolean exitWithAnimation;

    ViewsTransitionAnimator() {
        addPositionUpdateListener((position, isLeaving) -> {
            if (position == 0f && isLeaving) {
                cleanupRequest();
            }
        });
    }

    public void enter(@NonNull ID id, boolean withAnimation) {
        if (GestureDebug.isDebugAnimator()) {
            Log.d(TAG, "Enter requested for " + id + ", with animation = " + withAnimation);
        }

        enterWithAnimation = withAnimation;
        request(id);
    }

    @SuppressWarnings({ "unchecked", "SameParameterValue" })
    public void enterSingle(boolean withAnimation) {
        // Passing 'NONE' Object instead of ID. Will fail if ID will be actually used.
        enter((ID) NONE, withAnimation);
    }

    public void exit(boolean withAnimation) {
        if (getRequestedId() == null) {
            throw new IllegalStateException("You should call enter(...) before calling exit(...)");
        }

        if (GestureDebug.isDebugAnimator()) {
            Log.d(TAG, "Exit requested from " + getRequestedId()
                    + ", with animation = " + withAnimation);
        }

        exitRequested = true;
        exitWithAnimation = withAnimation;
        exitIfRequested();
    }

    private void exitIfRequested() {
        if (exitRequested && isReady()) {
            exitRequested = false;

            if (GestureDebug.isDebugAnimator()) {
                Log.d(TAG, "Perform exit from " + getRequestedId());
            }

            getAnimatorNonNull().exit(exitWithAnimation);
        }
    }

    public boolean isLeaving() {
        return exitRequested || getRequestedId() == null
                || (isReady() && getAnimatorNonNull().isLeaving());
    }


    public void addPositionUpdateListener(@NonNull PositionUpdateListener listener) {
        listeners.add(listener);
        if (isReady()) {
            getAnimatorNonNull().addPositionUpdateListener(listener);
        }
    }

    @SuppressWarnings("unused") // Public API
    public void removePositionUpdateListener(@NonNull PositionUpdateListener listener) {
        listeners.remove(listener);
        if (isReady()) {
            getAnimatorNonNull().removePositionUpdateListener(listener);
        }
    }


    @Override
    public void setFromListener(@NonNull OnRequestViewListener<ID> listener) {
        super.setFromListener(listener);
        if (listener instanceof RequestListener) {
            ((RequestListener<ID>) listener).initAnimator(this);
        }
    }

    @Override
    public void setToListener(@NonNull OnRequestViewListener<ID> listener) {
        super.setToListener(listener);
        if (listener instanceof RequestListener) {
            ((RequestListener<ID>) listener).initAnimator(this);
        }
    }

    @Override
    protected void onFromViewChanged(@Nullable View fromView, @Nullable ViewPosition fromPos) {
        super.onFromViewChanged(fromView, fromPos);

        if (isReady()) {
            if (GestureDebug.isDebugAnimator()) {
                Log.d(TAG, "Updating 'from' view for " + getRequestedId());
            }

            if (fromView != null) {
                getAnimatorNonNull().update(fromView);
            } else if (fromPos != null) {
                getAnimatorNonNull().update(fromPos);
            } else {
                getAnimatorNonNull().updateToNone();
            }
        }
    }

    @Override
    protected void onToViewChanged(@Nullable AnimatorView old, @NonNull AnimatorView view) {
        super.onToViewChanged(old, view);

        if (isReady() && old != null) {
            // Animation is in place, we should carefully swap animators
            swapAnimator(old.getPositionAnimator(), view.getPositionAnimator());
        } else {
            if (old != null) {
                cleanupAnimator(old.getPositionAnimator());
            }
            initAnimator(view.getPositionAnimator());
        }
    }

    @Override
    protected void onViewsReady(@NonNull ID id) {
        if (!isEntered) {
            isEntered = true;

            if (GestureDebug.isDebugAnimator()) {
                Log.d(TAG, "Ready to enter for " + getRequestedId());
            }

            if (getFromView() != null) {
                getAnimatorNonNull().enter(getFromView(), enterWithAnimation);
            } else if (getFromPos() != null) {
                getAnimatorNonNull().enter(getFromPos(), enterWithAnimation);
            } else {
                getAnimatorNonNull().enter(enterWithAnimation);
            }

            exitIfRequested();
        }

        if (getFromView() instanceof ImageView && getToView() instanceof ImageView) {
            // Pre-setting 'to' image with 'from' image to prevent flickering
            ImageView from = (ImageView) getFromView();
            ImageView to = (ImageView) getToView();
            if (to.getDrawable() == null) {
                to.setImageDrawable(from.getDrawable());
            }
        }

        super.onViewsReady(id);
    }

    @Override
    protected void cleanupRequest() {
        if (getToView() != null) {
            cleanupAnimator(getToView().getPositionAnimator());
        }

        isEntered = false;
        exitRequested = false;

        super.cleanupRequest();
    }


    private void initAnimator(ViewPositionAnimator animator) {
        for (PositionUpdateListener listener : listeners) {
            animator.addPositionUpdateListener(listener);
        }
    }

    private void cleanupAnimator(ViewPositionAnimator animator) {
        for (PositionUpdateListener listener : listeners) {
            animator.removePositionUpdateListener(listener);
        }

        if (!animator.isLeaving() || animator.getPosition() != 0f) {
            if (GestureDebug.isDebugAnimator()) {
                Log.d(TAG, "Exiting from cleaned animator for " + getRequestedId());
            }

            animator.exit(false);
        }
    }

    // Replaces old animator with new one preserving state.
    private void swapAnimator(ViewPositionAnimator old, ViewPositionAnimator next) {
        final float position = old.getPosition();
        final boolean isLeaving = old.isLeaving();
        final boolean isAnimating = old.isAnimating();

        if (GestureDebug.isDebugAnimator()) {
            Log.d(TAG, "Swapping animator for " + getRequestedId());
        }

        cleanupAnimator(old);

        if (getFromView() != null) {
            next.enter(getFromView(), false);
        } else if (getFromPos() != null) {
            next.enter(getFromPos(), false);
        } else {
            next.enter(false);
        }

        initAnimator(next);

        next.setState(position, isLeaving, isAnimating);
    }

    @NonNull
    private ViewPositionAnimator getAnimatorNonNull() {
        if (getToView() == null) {
            throw new NullPointerException();
        }
        return getToView().getPositionAnimator();
    }


    public abstract static class RequestListener<ID> implements OnRequestViewListener<ID> {
        private ViewsTransitionAnimator<ID> animator;

        protected void initAnimator(ViewsTransitionAnimator<ID> animator) {
            this.animator = animator;
        }

        protected ViewsTransitionAnimator<ID> getAnimator() {
            return animator;
        }
    }

}
