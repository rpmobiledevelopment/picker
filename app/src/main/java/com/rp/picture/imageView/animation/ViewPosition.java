package com.rp.picture.imageView.animation;

import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.rp.picture.imageView.utils.MathUtils;

import java.util.regex.Pattern;


@SuppressWarnings("WeakerAccess") // Public API (fields and methods)
public class ViewPosition {

    private static final String DELIMITER = "#";
    private static final Pattern SPLIT_PATTERN = Pattern.compile(DELIMITER);

    private static final int[] tmpLocation = new int[2];
    private static final Matrix tmpMatrix = new Matrix();

    private static final Rect tmpViewRect = new Rect();

    public final Rect view;
    public final Rect viewport;
    public final Rect visible;
    public final Rect image;

    private ViewPosition() {
        this.view = new Rect();
        this.viewport = new Rect();
        this.visible = new Rect();
        this.image = new Rect();
    }

    private ViewPosition(@NonNull Rect view, @NonNull Rect viewport,
            @NonNull Rect visible, @NonNull Rect image) {
        this.view = view;
        this.viewport = viewport;
        this.visible = visible;
        this.image = image;
    }

    @SuppressWarnings("unused") // Public API
    public void set(@NonNull ViewPosition pos) {
        this.view.set(pos.view);
        this.viewport.set(pos.viewport);
        this.visible.set(pos.visible);
        this.image.set(pos.image);
    }

    private boolean init(@NonNull View targetView) {
        // If view is not attached then we can't get it's position
        if (targetView.getWindowToken() == null) {
            return false;
        }

        tmpMatrix.setScale(getTotalScaleX(targetView), getTotalScaleY(targetView), 0f, 0f);
        tmpViewRect.set(view);

        targetView.getLocationInWindow(tmpLocation);

        view.set(0, 0, targetView.getWidth(), targetView.getHeight());
        MathUtils.mapIntRect(tmpMatrix, view);
        view.offset(tmpLocation[0], tmpLocation[1]);

        viewport.set(targetView.getPaddingLeft(),
                targetView.getPaddingTop(),
                targetView.getWidth() - targetView.getPaddingRight(),
                targetView.getHeight() - targetView.getPaddingBottom());
        MathUtils.mapIntRect(tmpMatrix, viewport);
        viewport.offset(tmpLocation[0], tmpLocation[1]);

        boolean isVisible = targetView.getGlobalVisibleRect(visible);
        if (!isVisible) {
            // Assuming we are starting from center of invisible view
            visible.set(view.centerX(), view.centerY(), view.centerX() + 1, view.centerY() + 1);
        }

        if (targetView instanceof ImageView) {
            ImageView imageView = (ImageView) targetView;
            Drawable drawable = imageView.getDrawable();

            if (drawable == null) {
                image.set(viewport);
            } else {
                final int drawableWidth = drawable.getIntrinsicWidth();
                final int drawableHeight = drawable.getIntrinsicHeight();

                // Getting image position within the view
                ImageViewHelper.applyScaleType(imageView.getScaleType(),
                        drawableWidth, drawableHeight, viewport.width(), viewport.height(),
                        imageView.getImageMatrix(), tmpMatrix);

                image.set(0, 0, drawableWidth, drawableHeight);
                MathUtils.mapIntRect(tmpMatrix, image);
                image.offset(viewport.left, viewport.top); // Calculating image position on screen
            }
        } else {
            image.set(viewport);
        }

        return !tmpViewRect.equals(view);
    }

    private static float getTotalScaleX(@Nullable Object view) {
        if (view instanceof View) {
            return ((View) view).getScaleX() * getTotalScaleX(((View) view).getParent());
        } else {
            return 1f;
        }
    }

    private static float getTotalScaleY(@Nullable Object view) {
        if (view instanceof View) {
            return ((View) view).getScaleY() * getTotalScaleY(((View) view).getParent());
        } else {
            return 1f;
        }
    }


    @NonNull
    public static ViewPosition newInstance() {
        return new ViewPosition();
    }

    @NonNull
    public static ViewPosition from(@NonNull View view) {
        ViewPosition pos = new ViewPosition();
        pos.init(view);
        return pos;
    }

    public static boolean apply(@NonNull ViewPosition pos, @NonNull View view) {
        return pos.init(view);
    }

    public static void apply(@NonNull ViewPosition pos, @NonNull Point point) {
        pos.view.set(point.x, point.y, point.x + 1, point.y + 1);
        pos.viewport.set(pos.view);
        pos.visible.set(pos.view);
        pos.image.set(pos.view);
    }

    @NonNull
    public String pack() {
        String viewStr = view.flattenToString();
        String viewportStr = viewport.flattenToString();
        String visibleStr = visible.flattenToString();
        String imageStr = image.flattenToString();
        return TextUtils.join(DELIMITER, new String[] {
                viewStr, viewportStr, visibleStr, imageStr
        });
    }

    @SuppressWarnings("unused") // Public API
    @NonNull
    public static ViewPosition unpack(String str) {
        String[] parts = TextUtils.split(str, SPLIT_PATTERN);
        if (parts.length != 4) {
            throw new IllegalArgumentException("Wrong ViewPosition string: " + str);
        }

        Rect view = Rect.unflattenFromString(parts[0]);
        Rect viewport = Rect.unflattenFromString(parts[1]);
        Rect visible = Rect.unflattenFromString(parts[2]);
        Rect image = Rect.unflattenFromString(parts[3]);

        if (view == null || viewport == null || visible == null || image == null) {
            throw new IllegalArgumentException("Wrong ViewPosition string: " + str);
        }

        return new ViewPosition(view, viewport, visible, image);
    }

}
