package com.rp.picture.imageView.commons;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager2.widget.ViewPager2;

public class DepthPageTransformer implements ViewPager.PageTransformer, ViewPager2.PageTransformer {

    private static final float MIN_SCALE = 0.75f;

    @Override
    public void transformPage(@NonNull View view, float position) {
        if (0 < position && position < 1f) {
            // Fade the page out
            view.setAlpha(1f - position);

            // Counteract the default slide transition
            view.setTranslationX(-view.getWidth() * position);

            // Scale the page down (between MIN_SCALE and 1)
            float scaleFactor = 1f - (1f - MIN_SCALE) * position;
            view.setScaleX(scaleFactor);
            view.setScaleY(scaleFactor);
            view.setTranslationZ(scaleFactor - 1f);
        } else {
            view.setAlpha(1f);
            view.setTranslationX(0f);
            view.setScaleX(1f);
            view.setScaleY(1f);
            view.setTranslationZ(0f);
        }
    }

}
