package com.rp.picture.imageView.animation;

import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;

class ViewPositionHolder implements ViewTreeObserver.OnPreDrawListener {

    private final ViewPosition pos = ViewPosition.newInstance();

    private OnViewPositionChangeListener listener;
    private View view;
    private View.OnAttachStateChangeListener attachListener;
    private boolean isPaused;

    @Override
    public boolean onPreDraw() {
        update();
        return true;
    }

    void init(@NonNull View view, @NonNull OnViewPositionChangeListener listener) {
        clear(); // Cleaning up old listeners, just in case

        this.view = view;
        this.listener = listener;

        attachListener = new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(@NonNull View view) {
                onViewAttached(view, true);
            }

            @Override
            public void onViewDetachedFromWindow(@NonNull View view) {
                onViewAttached(view, false);
            }
        };
        view.addOnAttachStateChangeListener(attachListener);

        onViewAttached(view, view.isAttachedToWindow());

        if (view.isLaidOut()) {
            update();
        }
    }

    private void onViewAttached(View view, boolean attached) {
        view.getViewTreeObserver().removeOnPreDrawListener(this);
        if (attached) {
            view.getViewTreeObserver().addOnPreDrawListener(this);
        }
    }

    void clear() {
        if (view != null) {
            view.removeOnAttachStateChangeListener(attachListener);
            onViewAttached(view, false);
        }

        pos.view.setEmpty();
        pos.viewport.setEmpty();
        pos.image.setEmpty();

        view = null;
        attachListener = null;
        listener = null;
        isPaused = false;
    }

    void pause(boolean paused) {
        if (isPaused == paused) {
            return;
        }

        isPaused = paused;
        update();
    }

    private void update() {
        if (view != null && listener != null && !isPaused) {
            boolean changed = ViewPosition.apply(pos, view);
            if (changed) {
                listener.onViewPositionChanged(pos);
            }
        }
    }

    interface OnViewPositionChangeListener {
        void onViewPositionChanged(@NonNull ViewPosition position);
    }

}
