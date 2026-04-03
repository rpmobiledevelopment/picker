package com.rp.picture.imageView.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.rp.picture.imageView.GestureController;
import com.rp.picture.imageView.GestureControllerForPager;
import com.rp.picture.imageView.Settings;
import com.rp.picture.imageView.State;
import com.rp.picture.imageView.animation.ViewPositionAnimator;
import com.rp.picture.imageView.internal.DebugOverlay;
import com.rp.picture.imageView.internal.GestureDebug;
import com.rp.picture.imageView.utils.ClipHelper;
import com.rp.picture.imageView.utils.CropUtils;
import com.rp.picture.imageView.views.interfaces.AnimatorView;
import com.rp.picture.imageView.views.interfaces.ClipBounds;
import com.rp.picture.imageView.views.interfaces.ClipView;
import com.rp.picture.imageView.views.interfaces.GestureView;

public class GestureImageView extends ImageView
        implements GestureView, ClipView, ClipBounds, AnimatorView {

    private GestureControllerForPager controller;
    private final ClipHelper clipViewHelper = new ClipHelper(this);
    private final ClipHelper clipBoundsHelper = new ClipHelper(this);
    private final Matrix imageMatrix = new Matrix();

    private ViewPositionAnimator positionAnimator;

    public GestureImageView(Context context) {
        this(context, null, 0);
    }

    public GestureImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GestureImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        ensureControllerCreated();
        controller.getSettings().initFromAttributes(context, attrs);
        controller.addOnStateChangeListener(new GestureController.OnStateChangeListener() {
            @Override
            public void onStateChanged(State state) {
                applyState(state);
            }

            @Override
            public void onStateReset(State oldState, State newState) {
                applyState(newState);
            }
        });

        setScaleType(ScaleType.MATRIX);
    }

    private void ensureControllerCreated() {
        if (controller == null) {
            controller = new GestureControllerForPager(this);
        }
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        clipBoundsHelper.onPreDraw(canvas);
        clipViewHelper.onPreDraw(canvas);
        super.draw(canvas);
        clipViewHelper.onPostDraw(canvas);
        clipBoundsHelper.onPostDraw(canvas);

        if (GestureDebug.isDrawDebugOverlay()) {
            DebugOverlay.drawDebug(this, canvas);
        }
    }

    @NonNull
    @Override
    public GestureControllerForPager getController() {
        return controller;
    }

    @NonNull
    @Override
    public ViewPositionAnimator getPositionAnimator() {
        if (positionAnimator == null) {
            positionAnimator = new ViewPositionAnimator(this);
        }
        return positionAnimator;
    }

    @Override
    public void clipView(@Nullable RectF rect, float rotation) {
        clipViewHelper.clipView(rect, rotation);
    }

    @Override
    public void clipBounds(@Nullable RectF rect) {
        clipBoundsHelper.clipView(rect, 0f);
    }

    @Nullable
    public Bitmap crop() {
        return CropUtils.crop(getDrawable(), controller);
    }

    @SuppressLint("ClickableViewAccessibility") // performClick() will be called by controller
    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        return controller.onTouch(this, event);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        controller.getSettings().setViewport(width - getPaddingLeft() - getPaddingRight(),
                height - getPaddingTop() - getPaddingBottom());
        controller.resetState();
    }

    @Override
    public void setImageResource(int resId) {
        setImageDrawable(getContext().getDrawable(resId));
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);

        // Method setImageDrawable can be called from super constructor,
        // so we have to ensure controller instance is created at this point.
        ensureControllerCreated();

        Settings settings = controller.getSettings();

        // Saving old image size
        float oldWidth = settings.getImageW();
        float oldHeight = settings.getImageH();

        // Setting image size
        if (drawable == null) {
            settings.setImage(0, 0);
        } else if (drawable.getIntrinsicWidth() == -1 || drawable.getIntrinsicHeight() == -1) {
            settings.setImage(settings.getMovementAreaW(), settings.getMovementAreaH());
        } else {
            settings.setImage(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        }

        // Getting new image size
        float newWidth = settings.getImageW();
        float newHeight = settings.getImageH();

        if (newWidth > 0f && newHeight > 0f && oldWidth > 0f && oldHeight > 0f) {
            float scaleFactor = Math.min(oldWidth / newWidth, oldHeight / newHeight);
            controller.getStateController().setTempZoomPatch(scaleFactor);
            controller.updateState();
            controller.getStateController().setTempZoomPatch(0f);
        } else {
            controller.resetState();
        }
    }

    protected void applyState(@NonNull State state) {
        state.get(imageMatrix);
        setImageMatrix(imageMatrix);
    }

}
