package com.rp.picture.imageView.transition;

import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.rp.picture.imageView.animation.ViewPosition;
import com.rp.picture.imageView.internal.GestureDebug;
import com.rp.picture.imageView.views.interfaces.AnimatorView;

@SuppressWarnings("WeakerAccess") // Public API (methods)
public class ViewsCoordinator<ID> {

    private static final String TAG = ViewsCoordinator.class.getSimpleName();

    private OnRequestViewListener<ID> fromListener;
    private OnRequestViewListener<ID> toListener;
    private OnViewsReadyListener<ID> readyListener;

    private ID requestedId;
    private ID fromId;
    private ID toId;

    private View fromView;
    private ViewPosition fromPos;
    private AnimatorView toView;

    public void setFromListener(@NonNull OnRequestViewListener<ID> listener) {
        fromListener = listener;
    }

    public void setToListener(@NonNull OnRequestViewListener<ID> listener) {
        toListener = listener;
    }

    @SuppressWarnings("unused") // Public API
    public void setReadyListener(@Nullable OnViewsReadyListener<ID> listener) {
        readyListener = listener;
    }

    public void request(@NonNull ID id) {
        if (fromListener == null) {
            throw new RuntimeException("'from' listener is not set");
        }
        if (toListener == null) {
            throw new RuntimeException("'to' listener is not set");
        }

        cleanupRequest();

        if (GestureDebug.isDebugAnimator()) {
            Log.d(TAG, "Requesting " + id);
        }

        requestedId = id;
        fromListener.onRequestView(id);
        toListener.onRequestView(id);
    }

    @Nullable
    public ID getRequestedId() {
        return requestedId;
    }

    @Nullable
    public View getFromView() {
        return fromView;
    }

    @Nullable
    public ViewPosition getFromPos() {
        return fromPos;
    }

    @Nullable
    public AnimatorView getToView() {
        return toView;
    }


    public void setFromView(@NonNull ID id, @NonNull View fromView) {
        setFromInternal(id, fromView, null);
    }

    @SuppressWarnings("unused") // Public API
    public void setFromPos(@NonNull ID id, @NonNull ViewPosition fromPos) {
        setFromInternal(id, null, fromPos);
    }

    public void setFromNone(@NonNull ID id) {
        setFromInternal(id, null, null);
    }

    private void setFromInternal(@NonNull ID id, View fromView, ViewPosition fromPos) {
        if (requestedId == null || !requestedId.equals(id)) {
            return;
        }
        if (this.fromView == fromView && fromView != null) {
            return; // Already set
        }

        if (GestureDebug.isDebugAnimator()) {
            Log.d(TAG, "Setting 'from' view for " + id);
        }

        onFromViewChanged(fromView, fromPos);

        fromId = id;
        this.fromView = fromView;
        this.fromPos = fromPos;
        notifyWhenReady();
    }

    protected void onFromViewChanged(@Nullable View fromView, @Nullable ViewPosition fromPos) {
        // Can be overridden to setup views
    }


    public void setToView(@NonNull ID id, @NonNull AnimatorView toView) {
        if (requestedId == null || !requestedId.equals(id)) {
            return;
        }
        if (this.toView == toView) {
            return; // Already set
        }

        if (GestureDebug.isDebugAnimator()) {
            Log.d(TAG, "Setting 'to' view for " + id);
        }

        onToViewChanged(this.toView, toView);

        toId = id;
        this.toView = toView;
        notifyWhenReady();
    }

    protected void onToViewChanged(@Nullable AnimatorView old, @NonNull AnimatorView view) {
        // Can be overridden to setup views
    }


    public boolean isReady() {
        return requestedId != null && requestedId.equals(fromId) && requestedId.equals(toId);
    }

    private void notifyWhenReady() {
        if (isReady()) {
            onViewsReady(requestedId);
        }
    }

    protected void onViewsReady(@NonNull ID id) {
        if (readyListener != null) {
            readyListener.onViewsReady(id);
        }
    }

    protected void cleanupRequest() {
        if (requestedId == null) {
            return;
        }

        if (GestureDebug.isDebugAnimator()) {
            Log.d(TAG, "Cleaning up request " + requestedId);
        }

        fromView = null;
        fromPos = null;
        toView = null;
        requestedId = fromId = toId = null;
    }


    public interface OnRequestViewListener<ID> {
        void onRequestView(@NonNull ID id);
    }

    public interface OnViewsReadyListener<ID> {

        void onViewsReady(@NonNull ID id);
    }

}
